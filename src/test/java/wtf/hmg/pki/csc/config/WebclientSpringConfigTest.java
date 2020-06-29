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
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import static org.junit.Assert.*;

public class WebclientSpringConfigTest {

    private WebclientSpringConfig sut;

    @Before
    public void setUp() {
        sut = new WebclientSpringConfig();
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

}
