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
package wtf.hmg.pki.csc.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.Temporal;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class CscUtilsTest {

    @Mock
    private BasicFileAttributes attributes;
    @Mock
    private BasicFileAttributes invalidAttributes;
    
    @Before
    public void init() {
        given(attributes.isRegularFile()).willReturn(true);
        given(invalidAttributes.isRegularFile()).willReturn(false);
    }
    
    
    @Test
    public void testIsValidCSRFileName() {
        assertTrue(CscUtils.isValidCSRFileName("/tmp/request.csr"));
        assertTrue(CscUtils.isValidCSRFileName("request.csr"));
        assertTrue(CscUtils.isValidCSRFileName("/tmp/request.csr.pem"));
        assertTrue(CscUtils.isValidCSRFileName("/tmp/request.pem"));
        
        assertFalse(CscUtils.isValidCSRFileName("/tmp/request.invalid"));
        assertFalse(CscUtils.isValidCSRFileName("user.request"));
    }

    @Test
    public void testIsValidCSRFile() {
        assertTrue(CscUtils.isValidCSRFile(Paths.get("/tmp/request.csr"), attributes));
        assertTrue(CscUtils.isValidCSRFile(Paths.get("/tmp/request.csr.pem"), attributes));
        assertTrue(CscUtils.isValidCSRFile(Paths.get("/tmp/request.pem"), attributes));
        
        assertFalse(CscUtils.isValidCSRFile(Paths.get("/tmp/request.csr"), invalidAttributes));
        assertFalse(CscUtils.isValidCSRFile(Paths.get("/tmp/request.invalid"), attributes));
    }

    @Test
    public void testIsSignedCSRFile() {
        assertTrue(CscUtils.isSignedCSRFile(Paths.get("/tmp/accepted/request.csr"), attributes));
        assertTrue(CscUtils.isSignedCSRFile(Paths.get("/tmp/accepted/request.pem"), attributes));
        
        assertFalse(CscUtils.isSignedCSRFile(Paths.get("/tmp/request.csr"), attributes));
        assertFalse(CscUtils.isSignedCSRFile(Paths.get("/tmp/request.pem"), attributes));
        assertFalse(CscUtils.isSignedCSRFile(Paths.get("/tmp/request.csr"), invalidAttributes));
        assertFalse(CscUtils.isSignedCSRFile(Paths.get("/tmp/request.invalid"), attributes));
    }

    @Test
    public void testIsRevokedCertFile() {
        assertTrue(CscUtils.isRevokedCertFile(Paths.get("/tmp/revoked/cert.crt.pem"), attributes));
        assertTrue(CscUtils.isRevokedCertFile(Paths.get("/tmp/revoked/cert.crt"), attributes));
        
        assertFalse(CscUtils.isRevokedCertFile(Paths.get("/tmp/request.csr"), attributes));
        assertFalse(CscUtils.isRevokedCertFile(Paths.get("/tmp/revoked/cert.crt.pem"), invalidAttributes));
        assertFalse(CscUtils.isRevokedCertFile(Paths.get("/tmp/revoked/request.csr"), attributes));
    }
    
    @Test
    public void testValidateCSR() throws URISyntaxException {
        Path dummyCSR = Paths.get(ClassLoader.getSystemResource("dummy.csr.pem").toURI());
        Path dummyInvalidCSR = Paths.get(ClassLoader.getSystemResource("logback-spring.xml").toURI());

        assertTrue(Files.exists(dummyCSR));
        assertTrue(Files.isRegularFile(dummyCSR));
        assertTrue(Files.isRegularFile(dummyInvalidCSR));

        assertTrue(CscUtils.validateCSR(dummyCSR));
        assertFalse(CscUtils.validateCSR(dummyInvalidCSR));
    }

    @Test
    public void testValidateCSRString() throws URISyntaxException, IOException {
        Path dummyCSR = Paths.get(ClassLoader.getSystemResource("dummy.csr.pem").toURI());
        Path dummyInvalidCSR = Paths.get(ClassLoader.getSystemResource("logback-spring.xml").toURI());

        assertTrue(CscUtils.validateCSRString(new String(Files.readAllBytes(dummyCSR))));
        assertFalse(CscUtils.validateCSR(dummyInvalidCSR));
    }
    
    @Test
    public void testValidateCSRForInvalidFile() {
        Path dummyInvalidCSR = Paths.get("/invalid");
        boolean result = CscUtils.validateCSR(dummyInvalidCSR);
        
        assertFalse(result);
    }
    
    @Test
    public void testextractCSRInfoForInvalidCsr() throws URISyntaxException {
        Path dummyInvalidCSR = Paths.get(ClassLoader.getSystemResource("logback-spring.xml").toURI());
        String info = CscUtils.extractCSRInfo(dummyInvalidCSR);

        assertNull(info);
    }

    @Test
    public void testextractCSRInfo() throws URISyntaxException {
        Path dummyCSR = Paths.get(ClassLoader.getSystemResource("dummy.csr.pem").toURI());
        String expectedResult = "C=DE,ST=NRW,L=Paradise City,O=Überflieger Company,OU=Test,CN=Postal Dude,E=postal.dude@invalid.email";

        String info = CscUtils.extractCSRInfo(dummyCSR);

        assertNotNull(info);
        assertEquals(expectedResult, info);
    }

    @Test
    public void testNormalizeUserName() {
        assertEquals("testuser", CscUtils.normalizeUserName("testuser"));
        assertEquals("test.user", CscUtils.normalizeUserName("test.user"));
        assertEquals("test.user_hmg.wtf", CscUtils.normalizeUserName("test.user@hmg.wtf"));
    }
    
    @Test
    public void testExtractCertValidToDateForInvalidCert() throws URISyntaxException {
        Path dummyInvalidCert = Paths.get(ClassLoader.getSystemResource("logback-spring.xml").toURI());
        Temporal result = CscUtils.extractCertValidToDate(dummyInvalidCert);
        assertNull(result);
    }
    
    @Test
    public void testExtractCertValidToDate() throws URISyntaxException {
        Temporal expectedResult = Instant.parse("2022-03-05T09:09:53.00Z");
        Path dummyCert = Paths.get(ClassLoader.getSystemResource("dummy.crt.pem").toURI());
        Temporal result = CscUtils.extractCertValidToDate(dummyCert);
        
        assertNotNull(result);
        assertEquals(expectedResult, result);
    }
}
