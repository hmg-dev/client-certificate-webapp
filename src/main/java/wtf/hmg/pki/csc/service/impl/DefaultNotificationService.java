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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import wtf.hmg.pki.csc.config.AppConfig;
import wtf.hmg.pki.csc.service.NotificationService;

import java.util.concurrent.ExecutorService;

@Service
public class DefaultNotificationService implements NotificationService {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private AppConfig appConfig;
	@Autowired
	private JavaMailSender javaMailSender;
	@Autowired
	private ExecutorService executorService;
	
	@Override
	public void sendNotificationAsync(final String subject, final String text) {
		executorService.execute(() -> {
			try {
				sendNotification(subject, text);
			} catch (Exception e) {
				log.error("Sending async notification failed!", e);
			}
		});
	}
	
	@Override
	public void sendNotification(final String subject, final String text) {
		if(!appConfig.isNotificationsEnabled()) {
			log.debug("Notifications are disabled.");
			return;
		}
		
		SimpleMailMessage msg = new SimpleMailMessage();
		msg.setFrom(appConfig.getNotificationSender());
		msg.setTo(appConfig.getNotificationRecipients().toArray(new String[]{}));
		msg.setSubject(subject);
		msg.setText(text);
		
		log.info("Sending notification...");
		javaMailSender.send(msg);
	}
	
	public void setAppConfig(final AppConfig appConfig) {
		this.appConfig = appConfig;
	}
	
	public void setJavaMailSender(final JavaMailSender javaMailSender) {
		this.javaMailSender = javaMailSender;
	}
	
	public void setExecutorService(final ExecutorService executorService) {
		this.executorService = executorService;
	}
}
