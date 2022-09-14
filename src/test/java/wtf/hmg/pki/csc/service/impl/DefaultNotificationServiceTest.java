/*
 Copyright (C) 2020, Martin Drößler <m.droessler@handelsblattgroup.com>
 Copyright (C) 2020, Handelsblatt GmbH

 This file is part of pki-web / client-certificate-webapp

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package wtf.hmg.pki.csc.service.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import wtf.hmg.pki.csc.config.AppConfig;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoInteractions;

@RunWith(MockitoJUnitRunner.class)
public class DefaultNotificationServiceTest {
	
	private DefaultNotificationService sut;
	
	@Mock
	private AppConfig appConfig;
	@Mock
	private JavaMailSender javaMailSender;
	@Mock
	private ExecutorService executorService;
	@Captor
	private ArgumentCaptor<SimpleMailMessage> mailMsgCaptor;
	@Captor
	private ArgumentCaptor<Runnable> executorCaptor;
	
	private String dummySubject = "NARF";
	private String dummyText = "ZORT";
	
	@Before
	public void init() {
		sut = new DefaultNotificationService();
		sut.setAppConfig(appConfig);
		sut.setJavaMailSender(javaMailSender);
		sut.setExecutorService(executorService);
	}
	
	@Test
	public void testSendNotification_forNotificationDisabled() {
		given(appConfig.isNotificationsEnabled()).willReturn(false);
		
		sut.sendNotification(dummySubject, dummyText);
		
		verify(appConfig, times(1)).isNotificationsEnabled();
		verifyNoMoreInteractions(appConfig);
		verifyNoInteractions(javaMailSender);
	}
	
	@Test
	public void testSendNotification() {
		String dummySender = "pinky@brain.narf";
		List<String> dummyRecipients = Collections.singletonList("brain@acme.zort");
		
		given(appConfig.isNotificationsEnabled()).willReturn(true);
		given(appConfig.getNotificationSender()).willReturn(dummySender);
		given(appConfig.getNotificationRecipients()).willReturn(dummyRecipients);
		
		sut.sendNotification(dummySubject, dummyText);
		
		verify(javaMailSender, times(1)).send(mailMsgCaptor.capture());
		verify(appConfig, times(1)).getNotificationSender();
		verify(appConfig, times(1)).getNotificationRecipients();
		verify(appConfig, times(1)).isNotificationsEnabled();
		
		SimpleMailMessage result = mailMsgCaptor.getValue();
		assertNotNull(result);
		assertEquals(dummySender, result.getFrom());
		assertArrayEquals(dummyRecipients.toArray(new String[]{}), result.getTo());
		assertEquals(dummySubject, result.getSubject());
		assertEquals(dummyText, result.getText());
	}
	
	@Test
	public void testSendAdminNotificationAsync() {
		sut.sendNotificationAsync(dummySubject, dummyText);
		
		verify(executorService, times(1)).execute(executorCaptor.capture());
		
		Runnable job = executorCaptor.getValue();
		assertNotNull(job);
	}
	
}
