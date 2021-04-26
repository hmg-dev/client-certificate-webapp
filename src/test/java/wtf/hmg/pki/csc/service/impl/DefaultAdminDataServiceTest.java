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

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.FileSystemUtils;
import wtf.hmg.pki.csc.config.AppConfig;
import wtf.hmg.pki.csc.model.CSR;
import wtf.hmg.pki.csc.model.CertInfo;
import wtf.hmg.pki.csc.service.FilesService;
import wtf.hmg.pki.csc.util.CscUtilsTest;
import wtf.hmg.pki.csc.util.SupportUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.function.BiPredicate;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DefaultAdminDataServiceTest {

    private DefaultAdminDataService sut;

    private static Path dummyStoragePath;
    private AppConfig appConfig;

    @Mock
    private FilesService filesService;
    private FilesService realFilesService;
    
    @Mock
    private BasicFileAttributes attributes;
    @Mock
    private BasicFileAttributes invalidAttributes;
    
    @Captor
    private ArgumentCaptor<BiPredicate<Path, BasicFileAttributes>> matcherCaptor;

    @BeforeClass
    public static void init() throws IOException {
        dummyStoragePath = Files.createTempDirectory("csc");
        TestPathHelper.initDummyFileStructure(dummyStoragePath);
    }

    @Before
    public void setUp() {
        appConfig = new AppConfig();
        appConfig.setStoragePath(dummyStoragePath);
        realFilesService = new WrapperFilesService();

        sut = new DefaultAdminDataService();
        sut.setAppConfig(appConfig);
        sut.setSupportUtils(new SupportUtils());
    
        given(attributes.isRegularFile()).willReturn(true);
        given(invalidAttributes.isRegularFile()).willReturn(false);
    }

    @Test
    public void testFindSignedCertificateRequestsForIOError() throws IOException {
        doThrow(new IOException("TEST")).when(filesService).find(any(Path.class), anyInt(), any());

        sut.setFilesService(filesService);
        List<CSR> result = sut.findSignedCertificateRequests();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(filesService, times(1)).find(any(Path.class), eq(3), matcherCaptor.capture());

        BiPredicate<Path, BasicFileAttributes> fileMatcher = matcherCaptor.getValue();
        assertNotNull(fileMatcher);
        testIsSignedCSRFile(fileMatcher);
    }

    /**
     * Copy of {@link CscUtilsTest#testIsSignedCSRFile()}
     * Because we can't check the type of a lambda-parameter - only its behaviour!
     *
     * @param fileMatcher
     */
    private void testIsSignedCSRFile(final BiPredicate<Path, BasicFileAttributes> fileMatcher) {
        assertFalse(fileMatcher.test(Paths.get("/tmp/request.csr"), attributes));
        assertTrue(fileMatcher.test(Paths.get("/tmp/accepted/request.csr"), attributes));
        assertFalse(fileMatcher.test(Paths.get("/tmp/request.pem"), attributes));
        assertTrue(fileMatcher.test(Paths.get("/tmp/accepted/request.pem"), attributes));
        assertFalse(fileMatcher.test(Paths.get("/tmp/request.csr"), invalidAttributes));
        assertFalse(fileMatcher.test(Paths.get("/tmp/request.invalid"), attributes));
    }

    @Test
    public void testFindSignedCertificateRequests() {
        int expectedCSRamount = 1;

        sut.setFilesService(realFilesService);
        List<CSR> result = sut.findSignedCertificateRequests();
        assertNotNull(result);
        assertEquals(expectedCSRamount, result.size());

        for(int i=0; i<expectedCSRamount; i++) {
            assertNotNull(result.get(i));
            assertNotNull(result.get(i).getUserName());
            assertNotEquals("accepted", result.get(i).getUserName());
            assertNotNull(result.get(i).getCsrFile());
            assertNotNull(result.get(i).getLastModified());
            assertNotNull(result.get(i).getLastRenewed());
            assertTrue(Files.exists(result.get(i).getCsrFile()));
            assertTrue(result.get(i).isRenewalRequested());
        }
    }
    
    
    @Test
    public void testFindRevokedCertificatesForIOError() throws IOException {
        doThrow(new IOException("TEST")).when(filesService).find(any(Path.class), anyInt(), any());
        
        sut.setFilesService(filesService);
        List<CertInfo> result = sut.findRevokedCertificates();
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(filesService, times(1)).find(any(Path.class), eq(3), matcherCaptor.capture());
        
        BiPredicate<Path, BasicFileAttributes> fileMatcher = matcherCaptor.getValue();
        assertNotNull(fileMatcher);
        testIsRevokedCertFile(fileMatcher);
    }
    
    private void testIsRevokedCertFile(final BiPredicate<Path, BasicFileAttributes> fileMatcher) {
        assertTrue(fileMatcher.test(Paths.get("/tmp/revoked/cert.crt.pem"), attributes));
        assertTrue(fileMatcher.test(Paths.get("/tmp/revoked/cert.crt"), attributes));
        
        assertFalse(fileMatcher.test(Paths.get("/tmp/request.csr"), attributes));
        assertFalse(fileMatcher.test(Paths.get("/tmp/revoked/cert.crt.pem"), invalidAttributes));
        assertFalse(fileMatcher.test(Paths.get("/tmp/revoked/request.csr"), attributes));
    }
    
    @Test
    public void testFindRevokedCertificates() {
        int expectedCertAmount = 1;
        
        sut.setFilesService(realFilesService);
        List<CertInfo> result = sut.findRevokedCertificates();
        assertNotNull(result);
        assertEquals(expectedCertAmount, result.size());
        
        for(int i=0; i<expectedCertAmount; i++) {
            assertNotNull(result.get(i));
            assertNotNull(result.get(i).getUserName());
            assertNotEquals("revoked", result.get(i).getUserName());
            assertNotNull(result.get(i).getCertFile());
            assertNotNull(result.get(i).getLastModified());
            assertTrue(Files.exists(result.get(i).getCertFile()));
        }
    }
    
    @Test
    public void testFindPendingCertificateRequestsForIOError() throws IOException {
        doThrow(new IOException("TEST")).when(filesService).find(any(Path.class), anyInt(), any());

        sut.setFilesService(filesService);
        List<CSR> result = sut.findPendingCertificateRequests();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(filesService, times(1)).find(any(Path.class), eq(2), matcherCaptor.capture());

        BiPredicate<Path, BasicFileAttributes> fileMatcher = matcherCaptor.getValue();
        assertNotNull(fileMatcher);
        testIsValidCSRFile(fileMatcher);
    }

    /**
     * Copy of {@link CscUtilsTest#testIsValidCSRFile()}
     * Because we can't check the type of a lambda-parameter - only its behaviour!
     *
     * @param fileMatcher
     */
    private void testIsValidCSRFile(final BiPredicate<Path, BasicFileAttributes> fileMatcher) {
        BasicFileAttributes attributes = mock(BasicFileAttributes.class);
        BasicFileAttributes invalidAttributes = mock(BasicFileAttributes.class);

        given(attributes.isRegularFile()).willReturn(true);
        given(invalidAttributes.isRegularFile()).willReturn(false);

        assertTrue(fileMatcher.test(Paths.get("/tmp/request.csr"), attributes));
        assertTrue(fileMatcher.test(Paths.get("/tmp/request.csr.pem"), attributes));
        assertTrue(fileMatcher.test(Paths.get("/tmp/request.pem"), attributes));
        assertFalse(fileMatcher.test(Paths.get("/tmp/request.csr"), invalidAttributes));
        assertFalse(fileMatcher.test(Paths.get("/tmp/request.invalid"), attributes));
    }

    @Test
    public void testFindPendingCertificateRequests() {
        int expectedCSRamount = 3;

        sut.setFilesService(realFilesService);
        List<CSR> result = sut.findPendingCertificateRequests();
        assertNotNull(result);
        assertEquals(expectedCSRamount, result.size());

        for(int i=0; i<expectedCSRamount; i++) {
            assertNotNull(result.get(i));
            assertNotNull(result.get(i).getUserName());
            assertNotNull(result.get(i).getCsrFile());
            assertNotNull(result.get(i).getLastModified());
            assertTrue(Files.exists(result.get(i).getCsrFile()));
        }
    }

    @Test(expected = IOException.class)
    public void testRejectUserCSRForIOError() throws IOException {
        doThrow(new IOException("TEST")).when(filesService).move(any(Path.class), any(Path.class), any());

        sut.setFilesService(filesService);
        sut.rejectUserCSR("username", "filename");
    }

    @Test
    public void testRejectUserCSR() throws IOException {
        String userName = "user1";
        String fileName = "user1.csr.pem";

        Path expectedSource = dummyStoragePath.resolve("users").resolve(userName).resolve(fileName);
        Path expectedTarget = dummyStoragePath.resolve("users").resolve(userName).resolve("rejected").resolve(fileName);
        CopyOption[] expectedOptions = new CopyOption[] { StandardCopyOption.REPLACE_EXISTING };

        sut.setFilesService(filesService);
        sut.rejectUserCSR(userName, fileName);

        verify(filesService, times(1)).createDirectories(expectedTarget.getParent());
        verify(filesService, times(1)).move(expectedSource, expectedTarget, expectedOptions);
    }

    @Test(expected = IOException.class)
    public void testAcceptUserCSRForIOError() throws IOException {
        doThrow(new IOException("TEST")).when(filesService).move(any(Path.class), any(Path.class), any());

        sut.setFilesService(filesService);
        sut.acceptUserCSR("username", "filename");
    }

    @Test
    public void testAcceptUserCSR() throws IOException {
        String userName = "user1";
        String fileName = "user1.csr.pem";

        Path expectedSource = dummyStoragePath.resolve("users").resolve(userName).resolve(fileName);
        Path expectedTarget = dummyStoragePath.resolve("users").resolve(userName).resolve("accepted").resolve(fileName);
        CopyOption[] expectedOptions = new CopyOption[] { StandardCopyOption.REPLACE_EXISTING };

        sut.setFilesService(filesService);
        sut.acceptUserCSR(userName, fileName);

        verify(filesService, times(1)).createDirectories(expectedTarget.getParent());
        verify(filesService, times(1)).move(expectedSource, expectedTarget, expectedOptions);
    }

    @Test
    public void testFindUserCertForRequestWhenCertFileNotExisting() {
        String userName = "user2";
        String fileName = "user2.csr.pem";

        sut.setFilesService(realFilesService);
        Path result = sut.findUserCertForRequest(userName, fileName);

        assertNull(result);
    }

    @Test
    public void testFindUserCertForRequest() {
        String userName = "user1";
        String fileName = "user1.csr.pem";
        Path expectedResult = dummyStoragePath.resolve("users").resolve(userName).resolve("certs/user1.crt.pem");

        sut.setFilesService(realFilesService);
        Path result = sut.findUserCertForRequest(userName, fileName);
        assertNotNull(result);
        assertEquals(expectedResult, result);
    }

    @Test
    public void testFindAcceptedCSR() {
        String userName = "user1";
        String fileName = "user1.csr.pem";
        Path expectedResult = dummyStoragePath.resolve("users").resolve(userName).resolve("accepted/user1.csr.pem");
    
        sut.setFilesService(realFilesService);
        Path result = sut.findAcceptedCSR(userName, fileName);
        assertNotNull(result);
        assertEquals(expectedResult, result);
    }
    
    @Test(expected = NullPointerException.class)
    public void testFlagRevokedUserCert_NoCertFile() throws IOException {
        given(filesService.exists(any(Path.class))).willReturn(false);
    
        sut.setFilesService(filesService);
        sut.flagRevokedUserCert("username", "filename");
    }
    
    @Test
    public void testFlagRevokedUserCert() throws IOException {
        String userName = "user1";
        String fileName = "user1.csr.pem";
        Path certFile = dummyStoragePath.resolve("users").resolve(userName).resolve("certs/user1.crt.pem");
        Path revokedUserPath = dummyStoragePath.resolve("users").resolve(userName).resolve("revoked");
        Path expectedCertTarget = revokedUserPath.resolve("user1.crt.pem");
        CopyOption[] expectedOptions = new CopyOption[] { StandardCopyOption.REPLACE_EXISTING };
        
        given(filesService.exists(certFile)).willReturn(true);
        given(filesService.move(certFile, expectedCertTarget, expectedOptions)).willReturn(expectedCertTarget);
        
        sut.setFilesService(filesService);
        sut.flagRevokedUserCert(userName, fileName);
        
        verify(filesService, times(1)).exists(certFile);
        verify(filesService, times(1)).createDirectories(revokedUserPath);
        verify(filesService, times(1)).move(certFile, expectedCertTarget, expectedOptions);
        verify(filesService, times(1)).setLastModifiedTime(eq(expectedCertTarget), any(FileTime.class));
    }
    
    @Test(expected = IOException.class)
    public void testFlagRevokedUserCertAndCSRForIOError() throws IOException {
        String userName = "user1";
        String fileName = "user1.csr.pem";
        doThrow(new IOException("TEST")).when(filesService).move(any(Path.class), any(Path.class), any());

        sut.setFilesService(filesService);
        sut.flagRevokedUserCertAndCSR(userName, fileName);
    }

    @Test(expected = NullPointerException.class)
    public void testFlagRevokedUserCertAndCSRForNoCertfile() throws IOException {
        given(filesService.exists(any(Path.class))).willReturn(false);

        sut.setFilesService(filesService);
        sut.flagRevokedUserCertAndCSR("username", "filename");
    }

    @Test
    public void testFlagRevokedUserCertAndCSR() throws IOException {
        String userName = "user1";
        String fileName = "user1.csr.pem";
        Path certFile = dummyStoragePath.resolve("users").resolve(userName).resolve("certs/user1.crt.pem");
        Path revokedUserPath = dummyStoragePath.resolve("users").resolve(userName).resolve("revoked");
        Path expectedCertTarget = revokedUserPath.resolve("user1.crt.pem");
        Path expectedCSRSource = dummyStoragePath.resolve("users").resolve(userName).resolve("accepted").resolve(fileName);
        Path expectedCSRTarget = dummyStoragePath.resolve("users").resolve(userName).resolve("rejected").resolve(fileName);
        CopyOption[] expectedOptions = new CopyOption[] { StandardCopyOption.REPLACE_EXISTING };

        given(filesService.exists(certFile)).willReturn(true);
        given(filesService.move(certFile, expectedCertTarget, expectedOptions)).willReturn(expectedCertTarget);

        sut.setFilesService(filesService);
        sut.flagRevokedUserCertAndCSR(userName, fileName);

        verify(filesService, times(1)).exists(certFile);
        verify(filesService, times(1)).createDirectories(expectedCSRTarget.getParent());
        verify(filesService, times(1)).move(expectedCSRSource, expectedCSRTarget, expectedOptions);
        verify(filesService, times(1)).createDirectories(revokedUserPath);
        verify(filesService, times(1)).move(certFile, expectedCertTarget, expectedOptions);
        verify(filesService, times(1)).setLastModifiedTime(eq(expectedCertTarget), any(FileTime.class));
    }

    @Test
    public void testFlagCSRasRenewed_existingFlag() throws IOException {
        String userName = "user1";
        Path csrFile = dummyStoragePath.resolve("users").resolve(userName).resolve("accepted/user1-ac.csr.pem");
        Path expectedRenewFile = dummyStoragePath.resolve("users").resolve(userName).resolve("accepted/user1-ac.csr.pem.renewed");
        Path expectedRequestFile = dummyStoragePath.resolve("users").resolve(userName).resolve("certs/user1-ac.crt.pem.reqrenew");
    
        sut.setFilesService(filesService);
        sut.flagCSRasRenewed(csrFile);
        
        verify(filesService, times(1)).setLastModifiedTime(eq(expectedRenewFile), any(FileTime.class));
        verify(filesService, times(1)).deleteRecursively(expectedRequestFile);
    }
    
    @Test
    public void testFlagCSRasRenewed_noFlag() throws IOException {
        String userName = "user2";
        Path csrFile = dummyStoragePath.resolve("users").resolve(userName).resolve("accepted/user2-ac.csr.pem");
        Path expectedRenewFile = dummyStoragePath.resolve("users").resolve(userName).resolve("accepted/user2-ac.csr.pem.renewed");
        
        sut.setFilesService(filesService);
        sut.flagCSRasRenewed(csrFile);
        
        verify(filesService, times(1)).createFile(expectedRenewFile);
        verify(filesService, never()).deleteRecursively(any(Path.class));
    }
    
    @AfterClass
    public static void tearDown() throws IOException {
        FileSystemUtils.deleteRecursively(dummyStoragePath);
    }
}
