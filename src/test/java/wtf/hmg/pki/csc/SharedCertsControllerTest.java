/*
 Copyright (C) 2021, Martin Drößler <m.droessler@handelsblattgroup.com>
 Copyright (C) 2021, Handelsblatt GmbH

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
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import wtf.hmg.pki.csc.service.CryptService;
import wtf.hmg.pki.csc.service.SharedAppService;
import wtf.hmg.pki.csc.util.SupportUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SharedCertsControllerTest extends AbstractCertificateControllerTest {
	
	private SharedCertsController sut;
	
	@Mock
	private OAuth2AuthenticationToken auth;
	@Mock
	private Model model;
	@Mock
	private SupportUtils supportUtils;
	@Mock
	private Locale dummyLocale;
	@Mock
	private RedirectAttributes redirectAttributes;
	@Mock
	private MessageSource messageSource;
	@Mock
	private SharedAppService sharedAppService;
	@Mock
	private ReentrantLock lock;
	@Mock
	private CryptService cryptService;
	
	private String dummyAppName = "user@Name";
	private String dummyCSRFileName = "csrFileName.csr.pem";
	private String dummyCRTFileName = "csrFileName.crt.pem";
	private String dummyServerKeyPassword = "ZORT";
	
	@Before
	public void setup() {
		sut = new SharedCertsController();
		sut.setSupportUtils(supportUtils);
		sut.setMessageSource(messageSource);
		sut.setSharedAppService(sharedAppService);
		sut.setCertificateService(certificateService);
		sut.setCryptService(cryptService);
		SharedCertsController.setLock(lock);
		
		given(auth.getPrincipal()).willReturn(user);
	}
	
	@Test
	public void testSharedCertsPage() {
		given(supportUtils.isAdmin(user)).willReturn(true);
		
		String result = sut.sharedCertsPage(model, auth);
		assertEquals("sharedCertsPage", result);
		
		verify(model, times(1)).addAttribute(eq("sharedApps"), anyList());
		verify(supportUtils, atLeastOnce()).isAdmin(user);
		verify(sharedAppService, times(1)).findSharedApps();
	}
	
	@Test
	public void testCreateSharedApp_forInvalidAppName() {
		testCreateSharedApp_forInvalidAppNameInternal("THE App!");
		testCreateSharedApp_forInvalidAppNameInternal("THE App");
		testCreateSharedApp_forInvalidAppNameInternal("THE-App?");
	}
	
	private void testCreateSharedApp_forInvalidAppNameInternal(final String dummyAppName) {
		String dummyErrorMessage = "TEST - Invalid AppName";
		given(messageSource.getMessage(eq("shared.certs.appname.invalid"), any(), eq(dummyLocale))).willReturn(dummyErrorMessage);
		
		String result = sut.createSharedApp(dummyAppName, null, null, dummyLocale, redirectAttributes);
		assertEquals("redirect:/shared-certs", result);
		
		verify(messageSource, atLeastOnce()).getMessage(eq("shared.certs.appname.invalid"), any(), any(Locale.class));
		verify(redirectAttributes, atLeastOnce()).addFlashAttribute("errorMessage", dummyErrorMessage);
		verifyNoInteractions(sharedAppService);
	}
	
	@Test
	public void testCreateSharedApp_forIOError() throws IOException {
		String dummyAppName = "THE-App";
		String dummyErrorMessage = "TEST - Error";
		
		given(messageSource.getMessage(eq("shared.certs.creation.failed"), any(), eq(dummyLocale))).willReturn(dummyErrorMessage);
		doThrow(new IOException("TEST")).when(sharedAppService).createAppKey(anyString());
		
		String result = sut.createSharedApp(dummyAppName, null, null, dummyLocale, redirectAttributes);
		assertEquals("redirect:/shared-certs", result);
		
		verify(messageSource, atLeastOnce()).getMessage(eq("shared.certs.creation.failed"), any(), any(Locale.class));
		verify(redirectAttributes, atLeastOnce()).addFlashAttribute("errorMessage", dummyErrorMessage);
		verify(sharedAppService, times(1)).createAppKey(dummyAppName);
		verify(sharedAppService, never()).createCSR(anyString(), anyString());
		verify(sharedAppService, never()).createAppDetails(anyString(), anyString(), anyString());
	}
	
	@Test
	public void testCreateSharedApp_forInvalidContactMail() throws IOException {
		String dummyAppName = "THE-App";
		String dummyErrorMessage = "TEST - Error";
		String dummyTeamContact = "team@contact.invalid";
		
		given(messageSource.getMessage(eq("shared.certs.contact.invalid"), any(), eq(dummyLocale))).willReturn(dummyErrorMessage);
		
		String result = sut.createSharedApp(dummyAppName, null, dummyTeamContact, dummyLocale, redirectAttributes);
		assertEquals("redirect:/shared-certs", result);
		
		verify(messageSource, atLeastOnce()).getMessage(eq("shared.certs.contact.invalid"), any(), any(Locale.class));
		verify(redirectAttributes, atLeastOnce()).addFlashAttribute("errorMessage", dummyErrorMessage);
		verify(sharedAppService, times(1)).isValidEMail(dummyTeamContact);
		verify(sharedAppService, never()).createAppKey(dummyAppName);
		verify(sharedAppService, never()).createCSR(anyString(), anyString());
		verify(sharedAppService, never()).createAppDetails(anyString(), anyString(), anyString());
	}
	
	@Test
	public void testCreateSharedApp() throws IOException {
		String dummyAppName = "THE-App";
		String dummyTeamName = "Team-Name";
		String dummyTeamContact = "team@contact.local";
		String dummyPassword = "Test-Password-123";
		String dummySuccessMessage = "Test success";
		
		given(sharedAppService.createAppKey(dummyAppName)).willReturn(dummyPassword);
		given(sharedAppService.isValidEMail(dummyTeamContact)).willReturn(true);
		given(messageSource.getMessage(eq("shared.certs.creation.success"), any(), eq(dummyLocale))).willReturn(dummySuccessMessage);
		
		String result = sut.createSharedApp(dummyAppName, dummyTeamName, dummyTeamContact, dummyLocale, redirectAttributes);
		assertEquals("redirect:/shared-certs", result);
		
		verify(messageSource, atLeastOnce()).getMessage(eq("shared.certs.creation.success"), any(), any(Locale.class));
		verify(redirectAttributes, atLeastOnce()).addFlashAttribute("message", dummySuccessMessage);
		verify(sharedAppService, times(1)).isValidEMail(dummyTeamContact);
		verify(sharedAppService, times(1)).createAppKey(dummyAppName);
		verify(sharedAppService, times(1)).createCSR(dummyAppName, dummyPassword);
		verify(sharedAppService, times(1)).createAppDetails(dummyAppName, dummyTeamName, dummyTeamContact);
	}
	
	@Test
	public void testSignAppCSR_forAlreadyLocked() {
		given(lock.tryLock()).willReturn(false);
		
		String result = sut.signAppCSR(dummyAppName, dummyCSRFileName, cryptPassword, dummyServerKeyPassword, redirectAttributes, auth);
		assertEquals("redirect:/shared-certs", result);
		
		verifyLockedBehaviour();
	}
	
	@Test
	public void testSignAppCSR_forError() throws IOException, GitAPIException {
		given(lock.tryLock()).willReturn(true);
		doThrow(new TransportException("TEST")).when(certificateService).cloneCertificateRepository();
		
		String result = sut.signAppCSR(dummyAppName, dummyCSRFileName, cryptPassword, dummyServerKeyPassword, redirectAttributes, auth);
		assertEquals("redirect:/shared-certs", result);
		
		verifyErrorBehaviour();
		verify(certificateService, times(1)).cleanupWorkingFiles();
		verify(certificateService, times(1)).cloneCertificateRepository();
	}
	
	@Test
	public void testSignAppCSR() throws IOException, GitAPIException {
		given(lock.tryLock()).willReturn(true);
		given(user.getName()).willReturn(adminName);
		given(certificateService.copyAppCSRToRepository(dummyAppName, dummyCSRFileName)).willReturn(csrRepoFile);
		given(cryptService.signCertificateRequest(csrRepoFile, dummyServerKeyPassword)).willReturn(certFile);
		
		String result = sut.signAppCSR(dummyAppName, dummyCSRFileName, cryptPassword, dummyServerKeyPassword, redirectAttributes, auth);
		assertEquals("redirect:/shared-certs", result);
		
		verify(auth, atLeastOnce()).getPrincipal();
		verify(user, times(1)).getName();
		verify(lock, times(1)).tryLock();
		verify(lock, times(1)).unlock();
		verifyPrepareWorkspace();
		verify(certificateService, times(1)).copyAppCSRToRepository(dummyAppName, dummyCSRFileName);
		verify(cryptService, times(1)).signCertificateRequest(csrRepoFile, dummyServerKeyPassword);
		verify(certificateService, times(1)).copyCertificateToAppDirectory(dummyAppName, certFile);
		verify(certificateService, times(1)).encryptWorkingFiles(cryptPassword);
		verify(certificateService, times(1)).commitAndPushChanges(adminName, "Signed App-Certificate");
		
		verify(redirectAttributes, never()).addFlashAttribute(eq("errorMessage"), anyString());
		verify(redirectAttributes, times(1)).addFlashAttribute(eq("message"), anyString());
	}
	
	@Test
	public void testDownloadAppFile_forIOError() throws IOException {
		String dummyFileName = "NARF";
		doThrow(new IOException("TEST")).when(sharedAppService).appFileAsResource(anyString(), anyString());
		
		ResponseEntity<Resource> result = sut.downloadAppFile(dummyAppName, dummyFileName);
		
		assertNotNull(result);
		assertEquals(400, result.getStatusCodeValue());
		verify(sharedAppService, times(1)).appFileAsResource(dummyAppName, dummyFileName);
	}
	
	@Test
	public void testDownloadAppFile_forNoSuchFile() throws IOException {
		String dummyFileName = "NARF";
		given(sharedAppService.appFileAsResource(anyString(), anyString())).willReturn(null);
		
		ResponseEntity<Resource> result = sut.downloadAppFile(dummyAppName, dummyFileName);
		
		assertNotNull(result);
		assertEquals(404, result.getStatusCodeValue());
		verify(sharedAppService, times(1)).appFileAsResource(dummyAppName, dummyFileName);
	}
	
	@Test
	public void testDownloadAppFile() throws IOException {
		String dummyFileName = "NARF";
		Resource dummyResource = mock(Resource.class);
		
		given(sharedAppService.appFileAsResource(anyString(), anyString())).willReturn(dummyResource);
		
		ResponseEntity<Resource> result = sut.downloadAppFile(dummyAppName, dummyFileName);
		
		assertNotNull(result);
		assertEquals(200, result.getStatusCodeValue());
		assertNotNull(result.getHeaders());
		assertNotNull(result.getHeaders().getContentType());
		assertEquals("application/x-pem-file", result.getHeaders().getContentType().toString());
		assertEquals(dummyResource, result.getBody());
		assertNotNull(result.getHeaders().getContentDisposition());
		assertEquals(dummyFileName, result.getHeaders().getContentDisposition().getFilename());
		verify(sharedAppService, times(1)).appFileAsResource(dummyAppName, dummyFileName);
	}
	
	@Test
	public void testRequestRenew_invalidFileName() throws IOException {
		String errorMessage = "Dummy Error Message invalid file!";
		given(messageSource.getMessage(eq("shared.certs.request.renew.cert.invalid"), any(), eq(dummyLocale))).willReturn(errorMessage);
		
		String result = sut.requestAppRenew("test-app", "INVALID", dummyLocale, redirectAttributes);
		assertNotNull(result);
		assertEquals("redirect:/shared-certs", result);
		
		verify(redirectAttributes, atLeastOnce()).addFlashAttribute("errorMessage", errorMessage);
		verify(sharedAppService, never()).requestRenewalForCert(anyString(), anyString());
	}
	
	@Test
	public void testRequestRenew_exceptionOccurred() throws IOException {
		String errorMessage = "Dummy Error Message Exception occurred!";
		given(messageSource.getMessage(eq("shared.certs.request.renew.error"), any(), eq(dummyLocale))).willReturn(errorMessage);
		doThrow(new IOException("TEST ERROR")).when(sharedAppService).requestRenewalForCert(anyString(), anyString());
		
		String result = sut.requestAppRenew("test-app", "test-app.crt.pem", dummyLocale, redirectAttributes);
		assertNotNull(result);
		assertEquals("redirect:/shared-certs", result);
		
		verify(redirectAttributes, atLeastOnce()).addFlashAttribute("errorMessage", errorMessage);
		verify(sharedAppService, times(1)).requestRenewalForCert(anyString(), anyString());
	}
	
	@Test
	public void testRequestRenew() throws IOException {
		String message = "Dummy Success Message!";
		given(messageSource.getMessage(eq("shared.certs.request.renew.success"), any(), eq(dummyLocale))).willReturn(message);
		
		String result = sut.requestAppRenew("test-app", "test-app.crt.pem", dummyLocale, redirectAttributes);
		assertNotNull(result);
		assertEquals("redirect:/shared-certs", result);
		
		verify(redirectAttributes, atLeastOnce()).addFlashAttribute("message", message);
		verify(redirectAttributes, never()).addFlashAttribute(eq("errorMessage"), anyString());
		verify(sharedAppService, times(1)).requestRenewalForCert(anyString(), anyString());
	}
	
	@Test
	public void testRenewAppCert_forAlreadyLocked() {
		given(lock.tryLock()).willReturn(false);
		
		String result = sut.renewAppCert(dummyAppName, dummyCRTFileName, cryptPassword, dummyServerKeyPassword, redirectAttributes, auth);
		assertEquals("redirect:/shared-certs", result);
		
		verifyLockedBehaviour();
	}
	
	@Test
	public void testRenewAppCert_forError() throws GitAPIException {
		given(lock.tryLock()).willReturn(true);
		doThrow(new TransportException("TEST")).when(certificateService).cloneCertificateRepository();
		
		String result = sut.renewAppCert(dummyAppName, dummyCRTFileName, cryptPassword, dummyServerKeyPassword, redirectAttributes, auth);
		assertEquals("redirect:/shared-certs", result);
		
		verifyErrorBehaviour();
		verify(certificateService, times(1)).cloneCertificateRepository();
	}
	
	@Test
	public void testRenewAppCert() throws IOException, GitAPIException {
		Path renewedCertFile = mock(Path.class);
		given(lock.tryLock()).willReturn(true);
		given(user.getName()).willReturn(adminName);
		given(sharedAppService.findAppFileForFilename(dummyAppName, dummyCRTFileName)).willReturn(certFile);
		given(sharedAppService.findAppFileForFilename(dummyAppName, dummyCSRFileName)).willReturn(csrRepoFile);
		given(cryptService.signCertificateRequest(csrRepoFile, dummyServerKeyPassword)).willReturn(renewedCertFile);
		
		String result = sut.renewAppCert(dummyAppName, dummyCRTFileName, cryptPassword, dummyServerKeyPassword, redirectAttributes, auth);
		assertEquals("redirect:/shared-certs", result);
		
		verify(auth, atLeastOnce()).getPrincipal();
		verify(user, times(1)).getName();
		verify(lock, times(1)).tryLock();
		verify(lock, times(1)).unlock();
		verifyPrepareWorkspace();
		verify(sharedAppService, times(1)).findAppFileForFilename(dummyAppName, dummyCRTFileName);
		verify(cryptService, times(1)).revokeCertificate(certFile, dummyServerKeyPassword);
		verify(sharedAppService, times(1)).deleteAppFile(dummyAppName, dummyCRTFileName);
		
		verify(sharedAppService, times(1)).findAppFileForFilename(dummyAppName, dummyCSRFileName);
		verify(cryptService, times(1)).signCertificateRequest(csrRepoFile, dummyServerKeyPassword);
		verify(certificateService, times(1)).copyCertificateToAppDirectory(dummyAppName, renewedCertFile);
		verify(sharedAppService, times(1)).deleteAppFile(dummyAppName, dummyCRTFileName + ".reqrenew");
		
		verify(certificateService, times(1)).encryptWorkingFiles(cryptPassword);
		verify(certificateService, times(1)).commitAndPushChanges(adminName, "Renewed App-Certificate");
		verify(redirectAttributes, times(1)).addFlashAttribute(eq("message"), anyString());
	}
	
	@Test
	public void testRevokeAppCert_forAlreadyLocked() {
		given(lock.tryLock()).willReturn(false);
		
		String result = sut.revokeAppCert(dummyAppName, dummyCRTFileName, cryptPassword, dummyServerKeyPassword, redirectAttributes, auth);
		assertEquals("redirect:/shared-certs", result);
		
		verifyLockedBehaviour();
	}
	
	@Test
	public void testRevokeAppCert_forError() throws GitAPIException {
		given(lock.tryLock()).willReturn(true);
		doThrow(new TransportException("TEST")).when(certificateService).cloneCertificateRepository();
		
		String result = sut.revokeAppCert(dummyAppName, dummyCRTFileName, cryptPassword, dummyServerKeyPassword, redirectAttributes, auth);
		assertEquals("redirect:/shared-certs", result);
		
		verifyErrorBehaviour();
		verify(certificateService, times(1)).cloneCertificateRepository();
	}
	
	@Test
	public void testRevokeAppCert() throws IOException, GitAPIException {
		given(lock.tryLock()).willReturn(true);
		given(user.getName()).willReturn(adminName);
		given(sharedAppService.findAppFileForFilename(dummyAppName, dummyCRTFileName)).willReturn(certFile);
		
		String result = sut.revokeAppCert(dummyAppName, dummyCRTFileName, cryptPassword, dummyServerKeyPassword, redirectAttributes, auth);
		assertEquals("redirect:/shared-certs", result);
		
		verify(auth, atLeastOnce()).getPrincipal();
		verify(user, times(1)).getName();
		verify(lock, times(1)).tryLock();
		verify(lock, times(1)).unlock();
		verifyPrepareWorkspace();
		verify(sharedAppService, times(1)).findAppFileForFilename(dummyAppName, dummyCRTFileName);
		verify(cryptService, times(1)).revokeCertificate(certFile, dummyServerKeyPassword);
		verify(sharedAppService, times(1)).deleteAppFile(dummyAppName, dummyCRTFileName);
		verify(certificateService, times(1)).encryptWorkingFiles(cryptPassword);
		verify(certificateService, times(1)).commitAndPushChanges(adminName, "Revoked App-Certificate");
		verify(redirectAttributes, times(1)).addFlashAttribute(eq("message"), anyString());
	}
	
	private void verifyLockedBehaviour() {
		verifyNoInteractions(certificateService);
		verify(lock, times(1)).tryLock();
		verify(lock, never()).unlock();
		verify(redirectAttributes, times(1)).addFlashAttribute(eq("errorMessage"), anyString());
		verify(redirectAttributes, never()).addFlashAttribute(eq("message"), anyString());
	}
	
	private void verifyErrorBehaviour() {
		verify(lock, times(1)).tryLock();
		verify(lock, times(1)).unlock();
		verify(redirectAttributes, times(1)).addFlashAttribute(eq("errorMessage"), anyString());
	}
}
