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

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@ComponentScan({"wtf.hmg.pki"})
@EnableConfigurationProperties
public class WebclientSpringConfig implements WebMvcConfigurer {

    @Autowired
    private AppConfig appConfig;
    
    @Value("${spring.mail.host}")
    private String mailServerHost;
    
    @Value("${spring.mail.port}")
    private Integer mailServerPort;
    
    @Value("${spring.mail.username}")
    private String mailServerUsername;
    
    @Value("${spring.mail.password}")
    private String mailServerPassword;
    
    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
    private String mailServerStartTls;
    
    @Value("${spring.mail.properties.mail.debug}")
    private String mailDebug;
    
    
    @Bean
    public Java8TimeDialect java8TimeDialect() {
        return new Java8TimeDialect();
    }

    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver slr = new SessionLocaleResolver();
        slr.setDefaultLocale(Locale.GERMAN);
        return slr;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        lci.setParamName("lang");
        return lci;
    }
    
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailServerHost);
        mailSender.setPort(mailServerPort);
        
        mailSender.setUsername(mailServerUsername);
        mailSender.setPassword(mailServerPassword);
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", mailServerStartTls);
        props.put("mail.debug", mailDebug);
        
        return mailSender;
    }
    
    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(5);
    }
    
    
    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    @Override
    public void addViewControllers(final ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/error").setViewName("error");
    }

    @Bean
    public SshSessionFactory sshSessionFactory() {
        return new SshdSessionFactory() {
            protected List<Path> getDefaultIdentities(@NonNull File sshDir) {
                return Collections.singletonList(appConfig.getGitUserKeyfile());
            }
        };
    }

    @Bean
    public TransportConfigCallback gitTransportConfigCallback() {
        return new TransportConfigCallback() {
            @Override
            public void configure(final Transport transport) {
                SshTransport sshTransport = ( SshTransport )transport;
                sshTransport.setSshSessionFactory( sshSessionFactory() );
            }
        };
    }
    
    public void setMailServerHost(final String mailServerHost) {
        this.mailServerHost = mailServerHost;
    }
    
    public void setMailServerPort(final Integer mailServerPort) {
        this.mailServerPort = mailServerPort;
    }
    
    public void setMailServerUsername(final String mailServerUsername) {
        this.mailServerUsername = mailServerUsername;
    }
    
    public void setMailServerPassword(final String mailServerPassword) {
        this.mailServerPassword = mailServerPassword;
    }
    
    public void setMailServerStartTls(final String mailServerStartTls) {
        this.mailServerStartTls = mailServerStartTls;
    }
    
    public void setMailDebug(final String mailDebug) {
        this.mailDebug = mailDebug;
    }
}
