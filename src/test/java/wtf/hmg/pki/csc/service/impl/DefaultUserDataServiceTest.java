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
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import wtf.hmg.pki.csc.config.AppConfig;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DefaultUserDataServiceTest {

    private DefaultUserDataService sut;

    private static Path dummyStoragePath;
    private Path dummyCRLPath;
    private AppConfig appConfig;

    @BeforeClass
    public static void init() throws IOException {
        dummyStoragePath = Files.createTempDirectory("csc");
        TestPathHelper.initDummyFileStructure(dummyStoragePath);
    }

    @Before
    public void setUp() throws URISyntaxException {
        dummyCRLPath = Paths.get(ClassLoader.getSystemResource("dummy.revocation.list").toURI());

        appConfig = new AppConfig();
        appConfig.setStoragePath(dummyStoragePath);
        appConfig.setCertRevocationListPath(dummyCRLPath);

        sut = new DefaultUserDataService();
        sut.setAppConfig(appConfig);
    }

    @Test
    public void testFindCertificateRequestsForUserWithoutAnyFiles() {
        List<String> requests = sut.findCertificateRequestsForUser("user0");
        assertNotNull(requests);
        assertTrue(requests.isEmpty());
    }

    @Test
    public void testFindCertificateRequestsForInvalidUser() {
        List<String> requests = sut.findCertificateRequestsForUser("userInvalid");
        assertNotNull(requests);
        assertTrue(requests.isEmpty());
    }

    @Test
    public void testFindCertificateRequestsForUserWithExistingRequest() {
        List<String> requests = sut.findCertificateRequestsForUser("user1");
        assertNotNull(requests);
        assertFalse(requests.isEmpty());
        assertEquals(1, requests.size());
        assertNotNull(requests.get(0));
        assertEquals("user1.csr.pem", requests.get(0));
    }

    @Test
    public void testFindAcceptedCertificateRequestsForUserWithoutAnyFiles() {
        List<String> requests = sut.findAcceptedCertificateRequestsForUser("user0");
        assertNotNull(requests);
        assertTrue(requests.isEmpty());
    }

    @Test
    public void testFindAcceptedCertificateRequestsForInvalidUser() {
        List<String> requests = sut.findAcceptedCertificateRequestsForUser("userINvalid");
        assertNotNull(requests);
        assertTrue(requests.isEmpty());
    }

    @Test
    public void testFindAcceptedCertificateRequestsForUserWithExistingRequest() {
        List<String> requests = sut.findAcceptedCertificateRequestsForUser("user1");
        assertNotNull(requests);
        assertFalse(requests.isEmpty());
        assertEquals(1, requests.size());
        assertNotNull(requests.get(0));
        assertEquals("user1-ac.csr.pem", requests.get(0));
    }

    @Test
    public void testFindRejectedCertificateRequestsForUserWithoutAnyFiles() {
        List<String> requests = sut.findRejectedCertificateRequestsForUser("user0");
        assertNotNull(requests);
        assertTrue(requests.isEmpty());
    }

    @Test
    public void testFindRejectedCertificateRequestsForInvalidUser() {
        List<String> requests = sut.findRejectedCertificateRequestsForUser("userINvalid");
        assertNotNull(requests);
        assertTrue(requests.isEmpty());
    }

    @Test
    public void testFindRejectedCertificateRequestsForUserWithExistingRequest() {
        List<String> requests = sut.findRejectedCertificateRequestsForUser("user1");
        assertNotNull(requests);
        assertFalse(requests.isEmpty());
        assertEquals(1, requests.size());
        assertNotNull(requests.get(0));
        assertEquals("user1-rc.csr.pem", requests.get(0));
    }

    @Test
    public void testFindCertificatesForUserWithoutAnyFiles() {
        List<String> requests = sut.findCertificatesForUser("user0");
        assertNotNull(requests);
        assertTrue(requests.isEmpty());
    }

    @Test
    public void testFindCertificatesForInvalidUser() {
        List<String> requests = sut.findCertificatesForUser("userINVALID");
        assertNotNull(requests);
        assertTrue(requests.isEmpty());
    }

    @Test
    public void testFindCertificatesForUserWithExistingFiles() {
        List<String> requests = sut.findCertificatesForUser("user1");
        assertNotNull(requests);
        assertFalse(requests.isEmpty());
        assertEquals(1, requests.size());
        assertNotNull(requests.get(0));
        assertEquals("user1.crt.pem", requests.get(0));
    }

    @Test
    public void testUserCertificateFileAsResourceForNoSuchFile() throws IOException {
        String dummyFilename = "user0.crt.pem";
        String userName = "user0";

        Resource resource = sut.userCertificateFileAsResource(userName, dummyFilename);
        assertNull(resource);
    }

    @Test
    public void testUserCertificateFileAsResourceForExploitFile() throws IOException {
        String dummyFilename = "../../user1/certs/user1-cert.crt.pem";
        String userName = "user0";

        Resource resource = sut.userCertificateFileAsResource(userName, dummyFilename);
        assertNull(resource);
    }

    @Test
    public void testUserCertificateFileAsResource() throws IOException {
        String dummyFilename = "user1.crt.pem";
        String userName = "user1";

        Resource resource = sut.userCertificateFileAsResource(userName, dummyFilename);
        assertNotNull(resource);
        assertEquals(dummyFilename, resource.getFilename());
        assertTrue(resource.isFile());
        assertTrue(resource.exists());
        assertNotNull(resource.getFile());
        assertEquals("DUMMY-CERT", new String(Files.readAllBytes(resource.getFile().toPath())));
    }

    @Test
    public void testCaCertificateAsResource() throws IOException {
        String expectedFilename = "intermediate.cert.pem";

        Resource resource = sut.caCertificateAsResource();
        assertNotNull(resource);
        assertEquals(expectedFilename, resource.getFilename());
        assertTrue(resource.isFile());
        assertTrue(resource.exists());
        assertNotNull(resource.getFile());
        assertEquals("DUMMY CA-CERT", new String(Files.readAllBytes(resource.getFile().toPath())));
    }

    @Test
    public void testCertRevocationListAsResource() throws IOException {
        Resource resource = sut.certRevocationListAsResource();
        assertNotNull(resource);
        assertEquals("dummy.revocation.list", resource.getFilename());
        assertTrue(resource.isFile());
        assertTrue(resource.exists());
        assertNotNull(resource.getFile());
        assertEquals("Dummy Certificate Revocation List", new String(Files.readAllBytes(resource.getFile().toPath())));
    }

    @Test(expected = IOException.class)
    public void testSaveUplaodedCSRForExistingRequest() throws IOException, URISyntaxException {
        MultipartFile file = mock(MultipartFile.class);
        Path dummyCSR = Paths.get(ClassLoader.getSystemResource("dummy.csr.pem").toURI());

        given(file.getOriginalFilename()).willReturn("user1.csr.pem");
        given(file.getBytes()).willReturn(Files.readAllBytes(dummyCSR));

        sut.saveUploadedCSR("user1", file);
        fail("Expected Exception for existing request!");
    }

    @Test(expected = IOException.class)
    public void testSaveUplaodedCSRForInvalidRequest() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        given(file.getBytes()).willReturn("INVALID CSR".getBytes());

        sut.saveUploadedCSR("user1", file);
        fail("Expected Exception for existing request!");
    }

    @Test
    public void testSaveUplaodedCSR() throws IOException, URISyntaxException {
        MultipartFile file = mock(MultipartFile.class);
        String userID = "user42";
        String fileName = userID + ".csr.pem";
        Path dummyCSR = Paths.get(ClassLoader.getSystemResource("dummy.csr.pem").toURI());

        Path userDir = dummyStoragePath.resolve("users").resolve(userID);
        Path csrPath = userDir.resolve(fileName);

        given(file.getBytes()).willReturn(Files.readAllBytes(dummyCSR));
        given(file.getOriginalFilename()).willReturn(fileName);

        sut.saveUploadedCSR(userID, file);

        assertTrue(Files.isDirectory(userDir));
        verify(file, atLeastOnce()).getOriginalFilename();
        verify(file, times(1)).transferTo(csrPath);
    }

    @Test(expected = IOException.class)
    public void testSaveUplaodedCSRForExistingTextRequest() throws IOException {
        String userID = "user1";
        String dummyCSRData = "NARF";
        String dummyCSRFileName = userID + ".csr.pem";

        sut.saveUploadedCSR(userID, dummyCSRFileName, dummyCSRData);
        fail("Expected Exception for existing request!");
    }

    @Test
    public void testSaveUploadedCSRForTextDataNoExploit() throws IOException {
        String userID = "user36";
        String dummyCSRData = "NARF";
        String dummyCSRFileName = userID + "-nox.csr.pem";
        String exploitFilename = "../" + dummyCSRFileName;

        Path userDir = dummyStoragePath.resolve("users").resolve(userID);
        Path csrPath = userDir.resolve(dummyCSRFileName);
        Path exploitPath = dummyStoragePath.resolve("users").resolve(dummyCSRFileName);

        sut.saveUploadedCSR(userID, exploitFilename, dummyCSRData);

        assertTrue(Files.isDirectory(userDir));
        assertTrue(Files.isRegularFile(csrPath));
        assertFalse(Files.exists(exploitPath));
    }

    @Test
    public void testSaveUploadedCSRForTextData() throws IOException {
        String userID = "user36";
        String dummyCSRData = "NARF";
        String dummyCSRFileName = userID + ".csr.pem";

        Path userDir = dummyStoragePath.resolve("users").resolve(userID);
        Path csrPath = userDir.resolve(dummyCSRFileName);

        sut.saveUploadedCSR(userID, dummyCSRFileName, dummyCSRData);

        assertTrue(Files.isDirectory(userDir));
        assertTrue(Files.isRegularFile(csrPath));

        List<String> csrFile = Files.readAllLines(csrPath);
        assertNotNull(csrFile);
        assertEquals(1, csrFile.size());
        assertEquals(dummyCSRData, csrFile.get(0));
    }

    @AfterClass
    public static void tearDown() throws IOException {
        FileSystemUtils.deleteRecursively(dummyStoragePath);
    }

}
