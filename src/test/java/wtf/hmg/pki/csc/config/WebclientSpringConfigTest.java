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
package wtf.hmg.pki.csc.config;

import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;

import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class WebclientSpringConfigTest {

    private WebclientSpringConfig sut;
    
    private String mailServerHost = "dummyHost";
    private Integer mailServerPort = 42;
    private String mailServerUsername = "dummyUser";
    private String mailServerPassword = "dummyPassword";
    private String mailServerStartTls = "true";
    private String mailDebug = "false";
    
    @Before
    public void setUp() {
        sut = new WebclientSpringConfig();
        sut.setMailServerHost(mailServerHost);
        sut.setMailServerPort(mailServerPort);
        sut.setMailServerUsername(mailServerUsername);
        sut.setMailServerPassword(mailServerPassword);
        sut.setMailServerStartTls(mailServerStartTls);
        sut.setMailDebug(mailDebug);
    }

    @Test
    public void testJava8TimeDialect() {
        Java8TimeDialect result = sut.java8TimeDialect();
        assertNotNull(result);
    }

    @Test
    public void testAddInterceptors() {
        InterceptorRegistry dummyRegistry = mock(InterceptorRegistry.class);
        sut.addInterceptors(dummyRegistry);
        verify(dummyRegistry, times(1)).addInterceptor(any(LocaleChangeInterceptor.class));
    }
    
    @Test
    public void testAddViewControllers() {
        ViewControllerRegistration dummyRegistration = mock(ViewControllerRegistration.class);
        ViewControllerRegistry dummyRegistry = mock(ViewControllerRegistry.class);
        given(dummyRegistry.addViewController(anyString())).willReturn(dummyRegistration);
        
        sut.addViewControllers(dummyRegistry);
        
        verify(dummyRegistry, times(2)).addViewController(anyString());
        verify(dummyRegistry, times(1)).addViewController("/login");
        verify(dummyRegistry, times(1)).addViewController("/error");
        verify(dummyRegistration, times(2)).setViewName(anyString());
        verify(dummyRegistration, times(1)).setViewName("login");
        verify(dummyRegistration, times(1)).setViewName("error");
    }
    
    @Test
    public void testLocaleResolver() {
        LocaleResolver resolver = sut.localeResolver();
        assertNotNull(resolver);
    }

    @Test
    public void testLocaleChangeInterceptor() {
        LocaleChangeInterceptor interceptor = sut.localeChangeInterceptor();

        assertNotNull(interceptor);
        assertEquals("lang", interceptor.getParamName());
    }

    @Test
    public void testSshSessionFactory() {
        SshSessionFactory factory = sut.sshSessionFactory();
        assertNotNull(factory);
    }

    @Test
    public void testGitTransportConfigCallback() {
        TransportConfigCallback callback = sut.gitTransportConfigCallback();
        assertNotNull(callback);
    }
    
    @Test
    public void testJavaMailSender() {
        JavaMailSenderImpl result = (JavaMailSenderImpl) sut.javaMailSender();
        
        assertEquals(mailServerHost, result.getHost());
        assertEquals((int)mailServerPort, result.getPort());
        assertEquals(mailServerUsername, result.getUsername());
        assertEquals(mailServerPassword, result.getPassword());
        assertNotNull(result.getJavaMailProperties());
        assertEquals(mailServerStartTls, result.getJavaMailProperties().getProperty("mail.smtp.starttls.enable"));
        assertEquals(mailDebug, result.getJavaMailProperties().getProperty("mail.debug"));
    }
    
    @Test
    public void testExecutorService() {
        ExecutorService result = sut.executorService();
        assertNotNull(result);
        assertFalse(result.isShutdown());
    }
}
