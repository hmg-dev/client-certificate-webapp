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
package wtf.hmg.pki.csc;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import wtf.hmg.pki.csc.model.CSR;
import wtf.hmg.pki.csc.service.AdminDataService;
import wtf.hmg.pki.csc.service.CertificateService;
import wtf.hmg.pki.csc.service.CryptService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AdminUIControllerTest {

    private AdminUIController sut;

    @Mock
    private AdminDataService adminDataService;
    @Mock
    private CertificateService certificateService;
    @Mock
    private CryptService cryptService;
    @Mock
    private RedirectAttributes redirectAttributes;
    @Mock
    private OAuth2AuthenticationToken auth;
    @Mock
    private OAuth2User user;
    @Mock
    private Model model;
    @Mock
    private ReentrantLock lock;

    @Before
    public void setUp() {
        sut = new AdminUIController();
        sut.setAdminDataService(adminDataService);
        sut.setCertificateService(certificateService);
        sut.setCryptService(cryptService);
        AdminUIController.setLock(lock);

        given(auth.getPrincipal()).willReturn(user);
    }

    @Test
    public void testAdminPage() {
        List<CSR> dummyPendingRequests = Collections.emptyList();
        List<CSR> dummySignedRequests = Collections.emptyList();

        given(adminDataService.findPendingCertificateRequests()).willReturn(dummyPendingRequests);
        given(adminDataService.findSignedCertificateRequests()).willReturn(dummySignedRequests);

        String result = sut.adminPage(model, auth);

        assertNotNull(result);
        assertEquals("adminPage", result);

        verify(auth, atLeastOnce()).getPrincipal();
        verify(model, times(1)).addAttribute("user", user);
        verify(model, times(1)).addAttribute("pendingRequests", dummyPendingRequests);
        verify(model, times(1)).addAttribute("signedRequests", dummySignedRequests);
        verify(adminDataService, times(1)).findPendingCertificateRequests();
        verify(adminDataService, times(1)).findSignedCertificateRequests();
    }

    @Test
    public void testRejectUserCSRForError() throws IOException {
        String userName = "userName";
        String fileName = "fileName";

        doThrow(new IOException("TEST")).when(adminDataService).rejectUserCSR(anyString(), anyString());

        String result = sut.rejectUserCSR(userName, fileName, redirectAttributes);

        assertNotNull(result);
        assertEquals("redirect:/admin", result);

        verify(adminDataService, times(1)).rejectUserCSR(userName, fileName);
        verify(redirectAttributes, times(1)).addFlashAttribute(eq("errorMessage"), anyString());
    }

    @Test
    public void testRejectUserCSR() throws IOException {
        String userName = "userName";
        String fileName = "fileName";
        String result = sut.rejectUserCSR(userName, fileName, redirectAttributes);

        assertNotNull(result);
        assertEquals("redirect:/admin", result);

        verify(adminDataService, times(1)).rejectUserCSR(userName, fileName);
        verify(redirectAttributes, never()).addFlashAttribute(eq("errorMessage"), anyString());
        verify(redirectAttributes, times(1)).addFlashAttribute(eq("message"), anyString());
    }

    @Test
    public void testSignCSRForError() throws GitAPIException, IOException {
        String userName = "userName";
        String fileName = "fileName";
        String cryptPassword = "NARF";
        String keyPassword = "ZORT";

        given(lock.tryLock()).willReturn(true);
        doThrow(new TransportException("TEST")).when(certificateService).cloneCertificateRepository();

        String result = sut.signCSR(userName, fileName, cryptPassword, keyPassword, redirectAttributes, auth);
        assertNotNull(result);
        assertEquals("redirect:/admin", result);

        verify(certificateService, times(1)).cleanupWorkingFiles();
        verify(certificateService, times(1)).cloneCertificateRepository();
        verify(redirectAttributes, times(1)).addFlashAttribute(eq("errorMessage"), anyString());
    }

    @Test
    public void testSignCSRForRuntimeError() throws GitAPIException, IOException {
        String userName = "userName";
        String fileName = "fileName";
        String cryptPassword = "NARF";
        String keyPassword = "ZORT";

        given(lock.tryLock()).willReturn(true);
        doThrow(new IllegalStateException("TEST")).when(certificateService).decryptWorkingFiles(anyString());

        String result = sut.signCSR(userName, fileName, cryptPassword, keyPassword, redirectAttributes, auth);
        assertNotNull(result);
        assertEquals("redirect:/admin", result);

        verify(certificateService, times(1)).cleanupWorkingFiles();
        verify(certificateService, times(1)).cloneCertificateRepository();
        verify(redirectAttributes, times(1)).addFlashAttribute(eq("errorMessage"), anyString());
    }

    @Test
    public void testSignCSRForAlreadyLocked() throws GitAPIException, IOException {
        given(lock.tryLock()).willReturn(false);

        String result = sut.signCSR("userName", "fileName", "cryptPassword",
                "keyPassword", redirectAttributes, auth);
        assertNotNull(result);
        assertEquals("redirect:/admin", result);

        verifyNoInteractions(certificateService, adminDataService, cryptService);
        verify(lock, times(1)).tryLock();
        verify(lock, never()).unlock();

        verify(redirectAttributes, times(1)).addFlashAttribute(eq("errorMessage"), anyString());
        verify(redirectAttributes, never()).addFlashAttribute(eq("message"), anyString());
    }

    @Test
    public void testSignCSR() throws GitAPIException, IOException {
        String userName = "user@Name";
        String expectedUsername = "user_Name";
        String fileName = "fileName";
        String cryptPassword = "NARF";
        String keyPassword = "ZORT";
        Path csrRepoFile = mock(Path.class);
        Path certFile = mock(Path.class);
        String adminName = "Pinky";

        given(lock.tryLock()).willReturn(true);
        given(certificateService.copyUserCSRToRepository(expectedUsername, fileName)).willReturn(csrRepoFile);
        given(cryptService.signCertificateRequest(csrRepoFile, keyPassword)).willReturn(certFile);
        given(user.getName()).willReturn(adminName);

        String result = sut.signCSR(userName, fileName, cryptPassword, keyPassword, redirectAttributes, auth);
        assertNotNull(result);
        assertEquals("redirect:/admin", result);

        verify(lock, times(1)).tryLock();
        verify(certificateService, times(1)).cleanupWorkingFiles();
        verify(certificateService, times(1)).cloneCertificateRepository();
        verify(certificateService, times(1)).decryptWorkingFiles(cryptPassword);
        verify(certificateService, times(1)).copyUserCSRToRepository(expectedUsername, fileName);
        verify(cryptService, times(1)).signCertificateRequest(csrRepoFile, keyPassword);
        verify(certificateService, times(1)).copyCertificateToUserDirectory(expectedUsername, certFile);
        verify(certificateService, times(1)).encryptWorkingFiles(cryptPassword);
        verify(adminDataService, times(1)).acceptUserCSR(expectedUsername, fileName);
        verify(auth, atLeastOnce()).getPrincipal();
        verify(user, times(1)).getName();
        verify(certificateService, times(1)).commitAndPushChanges(adminName, "Signed User-Certificate");
        verify(lock, times(1)).unlock();

        verify(redirectAttributes, never()).addFlashAttribute(eq("errorMessage"), anyString());
        verify(redirectAttributes, times(1)).addFlashAttribute(eq("message"), anyString());
    }

    @Test
    public void testRevokeCertForIOError() throws IOException, GitAPIException {
        given(lock.tryLock()).willReturn(true);
        doThrow(new IOException("TEST")).when(certificateService).decryptWorkingFiles(anyString());

        String result = sut.revokeCert("username", "filename", "cryptPassword", "keyPassword", redirectAttributes, auth);
        assertNotNull(result);
        assertEquals("redirect:/admin", result);

        verify(certificateService, times(1)).cleanupWorkingFiles();
        verify(certificateService, times(1)).cloneCertificateRepository();
        verify(redirectAttributes, times(1)).addFlashAttribute(eq("errorMessage"), anyString());
    }

    @Test
    public void testRevokeCertWhenWorkspaceLocked() {
        given(lock.tryLock()).willReturn(false);

        String result = sut.revokeCert("username", "filename", "cryptPassword", "keyPassword", redirectAttributes, auth);
        assertNotNull(result);
        assertEquals("redirect:/admin", result);

        verifyNoInteractions(certificateService, adminDataService, cryptService);
        verify(lock, times(1)).tryLock();
        verify(lock, never()).unlock();

        verify(redirectAttributes, times(1)).addFlashAttribute(eq("errorMessage"), anyString());
        verify(redirectAttributes, never()).addFlashAttribute(eq("message"), anyString());
    }

    @Test
    public void testRevokeCertWhithoutCorrespondingCertificate() throws IOException, GitAPIException {
        given(lock.tryLock()).willReturn(true);
        given(adminDataService.findUserCertForRequest(anyString(), anyString())).willReturn(null);

        String result = sut.revokeCert("userName", "fileName", "cryptPassword", "keyPassword", redirectAttributes, auth);
        assertNotNull(result);
        assertEquals("redirect:/admin", result);

        verify(lock, times(1)).unlock();
        verify(certificateService, times(1)).cleanupWorkingFiles();
        verify(certificateService, times(1)).cloneCertificateRepository();
        verify(certificateService, times(1)).decryptWorkingFiles(anyString());
        verify(adminDataService, times(1)).findUserCertForRequest(anyString(), anyString());
        verify(cryptService, never()).revokeCertificate(any(Path.class), anyString());

        verify(redirectAttributes, times(1)).addFlashAttribute(eq("errorMessage"), anyString());
        verify(redirectAttributes, never()).addFlashAttribute(eq("message"), anyString());
    }

    @Test
    public void testRevokeCert() throws IOException, GitAPIException {
        String userName = "user@Name";
        String expectedUsername = "user_Name";
        String fileName = "fileName";
        String cryptPassword = "NARF";
        String keyPassword = "ZORT";
        Path certFile = mock(Path.class);
        String adminName = "Pinky";

        given(lock.tryLock()).willReturn(true);
        given(user.getName()).willReturn(adminName);
        given(adminDataService.findUserCertForRequest(expectedUsername, fileName)).willReturn(certFile);

        String result = sut.revokeCert(userName, fileName, cryptPassword, keyPassword, redirectAttributes, auth);
        assertNotNull(result);
        assertEquals("redirect:/admin", result);

        verify(lock, times(1)).tryLock();
        verify(lock, times(1)).unlock();
        verify(certificateService, times(1)).cleanupWorkingFiles();
        verify(certificateService, times(1)).cloneCertificateRepository();
        verify(certificateService, times(1)).decryptWorkingFiles(cryptPassword);
        verify(adminDataService, times(1)).findUserCertForRequest(expectedUsername, fileName);
        verify(cryptService, times(1)).revokeCertificate(certFile, keyPassword);
        verify(adminDataService, times(1)).flagRevokedUserCertAndCSR(expectedUsername, fileName);
        verify(certificateService, times(1)).encryptWorkingFiles(cryptPassword);
        verify(certificateService, times(1)).commitAndPushChanges(adminName, "Revoked User-Certificate");

        verify(redirectAttributes, never()).addFlashAttribute(eq("errorMessage"), anyString());
        verify(redirectAttributes, times(1)).addFlashAttribute(eq("message"), anyString());
    }

}
