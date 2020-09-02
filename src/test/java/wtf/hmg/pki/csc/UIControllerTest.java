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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import wtf.hmg.pki.csc.service.UserDataService;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UIControllerTest {

    private UIController sut;

    @Mock
    private OAuth2AuthenticationToken auth;
    @Mock
    private OAuth2User user;
    @Mock
    private Model model;
    @Mock
    private MultipartFile dummyFile;
    @Mock
    private RedirectAttributes redirectAttributes;
    @Mock
    private UserDataService userDataService;
    @Mock
    private MessageSource messageSource;
    @Mock
    private Locale dummyLocale;

    private String dummyUID = "T.User@company.domain";
    private String expectedUID = "t.user_company.domain";

    @Before
    public void setUp() {
        sut = new UIController();
        sut.setUserDataService(userDataService);
        sut.setMessageSource(messageSource);

        given(auth.getPrincipal()).willReturn(user);
        given(user.getAttribute("unique_name")).willReturn(dummyUID);
    }

    @Test
    public void testIndexPage() {
        GrantedAuthority authority1 = mock(GrantedAuthority.class);
        GrantedAuthority authority2 = mock(GrantedAuthority.class);
        Collection<GrantedAuthority> authorities = Arrays.asList(authority1, authority2);

        willReturn(authorities).given(user).getAuthorities();
        given(authority1.getAuthority()).willReturn("ROLE_company.domain");
        given(authority2.getAuthority()).willReturn("ROLE_DevOps");

        String result = sut.indexPage(model, auth);

        assertNotNull(result);
        assertEquals("indexPage", result);

        verify(auth, atLeastOnce()).getPrincipal();
        verify(model, times(1)).addAttribute("user", user);
        verify(user, atLeastOnce()).getAttribute("unique_name");
        verify(user, atLeastOnce()).getAuthorities();

        verify(userDataService, times(1)).findCertificateRequestsForUser(expectedUID);
        verify(userDataService, times(1)).findAcceptedCertificateRequestsForUser(expectedUID);
        verify(userDataService, times(1)).findRejectedCertificateRequestsForUser(expectedUID);
        verify(userDataService, times(1)).findCertificateRequestsForUser(expectedUID);
        verify(model, times(1)).addAttribute(eq("userCSRList"), anyList());
        verify(model, times(1)).addAttribute(eq("userAcceptedCSRList"), anyList());
        verify(model, times(1)).addAttribute(eq("userRejectedCSRList"), anyList());
        verify(model, times(1)).addAttribute(eq("userCertificates"), anyList());
        verify(model, times(1)).addAttribute("isAdmin", true);
    }

    @Test
    public void testDownloadCertForIOError() throws IOException {
        String dummyFileName = "NARF";
        doThrow(new IOException("TEST")).when(userDataService).userCertificateFileAsResource(anyString(), anyString());

        ResponseEntity<Resource> result = sut.downloadCert(dummyFileName, auth);

        assertNotNull(result);
        assertEquals(400, result.getStatusCodeValue());

        verify(auth, atLeastOnce()).getPrincipal();
        verify(user, atLeastOnce()).getAttribute("unique_name");
        verify(userDataService, times(1)).userCertificateFileAsResource(expectedUID, dummyFileName);
    }

    @Test
    public void testDownloadCertForNoSuchFile() throws IOException {
        String dummyFileName = "NARF";
        given(userDataService.userCertificateFileAsResource(anyString(), anyString())).willReturn(null);

        ResponseEntity<Resource> result = sut.downloadCert(dummyFileName, auth);

        assertNotNull(result);
        assertEquals(404, result.getStatusCodeValue());

        verify(auth, atLeastOnce()).getPrincipal();
        verify(user, atLeastOnce()).getAttribute("unique_name");
        verify(userDataService, times(1)).userCertificateFileAsResource(expectedUID, dummyFileName);
    }

    @Test
    public void testDownloadCert() throws IOException {
        String dummyFileName = "NARF";
        Resource dummyResource = mock(Resource.class);

        given(userDataService.userCertificateFileAsResource(anyString(), anyString())).willReturn(dummyResource);

        ResponseEntity<Resource> result = sut.downloadCert(dummyFileName, auth);

        assertNotNull(result);
        assertEquals(200, result.getStatusCodeValue());
        assertNotNull(result.getHeaders());
        assertNotNull(result.getHeaders().getContentType());
        assertEquals("application/x-pem-file", result.getHeaders().getContentType().toString());
        assertEquals(dummyResource, result.getBody());
        assertNotNull(result.getHeaders().getContentDisposition());
        assertEquals(dummyFileName, result.getHeaders().getContentDisposition().getFilename());

        verify(auth, atLeastOnce()).getPrincipal();
        verify(user, atLeastOnce()).getAttribute("unique_name");
        verify(userDataService, times(1)).userCertificateFileAsResource(expectedUID, dummyFileName);
    }

    @Test
    public void testDownloadCaCertificateForIOError() throws IOException {
        doThrow(new IOException("TEST")).when(userDataService).caCertificateAsResource();

        ResponseEntity<Resource> result = sut.downloadCaCert();
        assertNotNull(result);
        assertEquals(500, result.getStatusCodeValue());

        verify(userDataService, times(1)).caCertificateAsResource();
    }

    @Test
    public void testDownloadCaCertificate() throws IOException {
        String expectedFileName = "intermediate-ca.cert.pem";
        Resource dummyResource = mock(Resource.class);

        given(userDataService.caCertificateAsResource()).willReturn(dummyResource);

        ResponseEntity<Resource> result = sut.downloadCaCert();
        assertNotNull(result);
        assertEquals(200, result.getStatusCodeValue());
        assertNotNull(result.getHeaders());
        assertNotNull(result.getHeaders().getContentType());
        assertEquals("application/x-pem-file", result.getHeaders().getContentType().toString());
        assertEquals(dummyResource, result.getBody());
        assertNotNull(result.getHeaders().getContentDisposition());
        assertEquals(expectedFileName, result.getHeaders().getContentDisposition().getFilename());

        verify(userDataService, times(1)).caCertificateAsResource();
    }

    @Test
    public void testDownloadRevocationListForIOError() throws IOException {
        doThrow(new IOException("TEST")).when(userDataService).certRevocationListAsResource();

        ResponseEntity<Resource> result = sut.downloadRevocationList();
        assertNotNull(result);
        assertEquals(500, result.getStatusCodeValue());

        verify(userDataService, times(1)).certRevocationListAsResource();
    }

    @Test
    public void testDownloadRevocationList() throws IOException {
        String expectedFileName = "list.crl";
        Resource dummyResource = mock(Resource.class);

        given(userDataService.certRevocationListAsResource()).willReturn(dummyResource);

        ResponseEntity<Resource> result = sut.downloadRevocationList();
        assertNotNull(result);
        assertEquals(200, result.getStatusCodeValue());
        assertNotNull(result.getHeaders());
        assertNotNull(result.getHeaders().getContentType());
        assertEquals("application/x-pem-file", result.getHeaders().getContentType().toString());
        assertEquals(dummyResource, result.getBody());
        assertNotNull(result.getHeaders().getContentDisposition());
        assertEquals(expectedFileName, result.getHeaders().getContentDisposition().getFilename());

        verify(userDataService, times(1)).certRevocationListAsResource();
    }

    @Test
    public void testCsrFileForIOError() throws IOException {
        String errorMessage = "Dummy Error Message ...in da house, yo!";
        given(dummyFile.getOriginalFilename()).willReturn("user.csr.pem");
        given(messageSource.getMessage(eq("user.request.file.error"), any(), eq(dummyLocale))).willReturn(errorMessage);
        doThrow(new IOException("TEST!")).when(userDataService).saveUploadedCSR(any(), any(MultipartFile.class));

        String result = sut.csrFile(dummyFile, dummyLocale, redirectAttributes, auth);
        assertNotNull(result);
        assertEquals("redirect:/", result);

        verify(auth, atLeastOnce()).getPrincipal();
        verify(user, atLeastOnce()).getAttribute("unique_name");
        verify(userDataService, times(1)).saveUploadedCSR(any(), any(MultipartFile.class));
        verify(messageSource, times(1)).getMessage(eq("user.request.file.error"), any(), eq(dummyLocale));
        verify(redirectAttributes, atLeastOnce()).addFlashAttribute("errorMessage", errorMessage);
    }

    @Test
    public void testCsrFileForNullFilename() throws IOException {
        testCsrFileForInvalidFilenameInternal(null);
    }

    @Test
    public void testCsrFileForInvalidFilename() throws IOException {
        testCsrFileForInvalidFilenameInternal("user.request");
    }

    private void testCsrFileForInvalidFilenameInternal(final String filename) throws IOException {
        String errorMessage = "Dummy Error Message invalid file!";
        given(messageSource.getMessage(eq("user.request.file.invalid"), any(), eq(dummyLocale))).willReturn(errorMessage);
        given(dummyFile.getOriginalFilename()).willReturn(filename);

        String result = sut.csrFile(dummyFile, dummyLocale, redirectAttributes, auth);
        assertNotNull(result);
        assertEquals("redirect:/", result);

        verify(auth, never()).getPrincipal();
        verify(user, never()).getAttribute("unique_name");
        verify(userDataService, never()).saveUploadedCSR(any(), any(MultipartFile.class));
        verify(messageSource, times(1)).getMessage(eq("user.request.file.invalid"), any(), eq(dummyLocale));
        verify(redirectAttributes, atLeastOnce()).addFlashAttribute("errorMessage", errorMessage);
    }

    @Test
    public void testCsrFile() throws IOException {
        String successMessage = "It works!";
        given(messageSource.getMessage(eq("user.request.file.success"), any(), eq(dummyLocale))).willReturn(successMessage);
        given(dummyFile.getOriginalFilename()).willReturn("user.csr.pem");

        String result = sut.csrFile(dummyFile, dummyLocale, redirectAttributes, auth);
        assertNotNull(result);
        assertEquals("redirect:/", result);

        verify(auth, atLeastOnce()).getPrincipal();
        verify(user, atLeastOnce()).getAttribute("unique_name");
        verify(userDataService, times(1)).saveUploadedCSR(any(), any(MultipartFile.class));
        verify(messageSource, times(1)).getMessage(eq("user.request.file.success"), any(), eq(dummyLocale));
        verify(redirectAttributes, atLeastOnce()).addFlashAttribute("message", successMessage);
    }

    @Test
    public void testCsrTextForInvalidCSR() throws IOException {
        String dummyCsr = "NARF";
        String errorMessage = "Dummy Error Message invalid file!";

        given(messageSource.getMessage(eq("user.request.text.invalid"), any(), eq(dummyLocale))).willReturn(errorMessage);

        String result = sut.csrText(dummyCsr, dummyLocale, redirectAttributes, auth);
        assertNotNull(result);
        assertEquals("redirect:/", result);

        verify(auth, never()).getPrincipal();
        verify(user, never()).getAttribute("unique_name");
        verify(userDataService, never()).saveUploadedCSR(anyString(), anyString(), anyString());
        verify(messageSource, times(1)).getMessage(eq("user.request.text.invalid"), any(), eq(dummyLocale));
        verify(redirectAttributes, atLeastOnce()).addFlashAttribute("errorMessage", errorMessage);
    }

    @Test
    public void testCsrTextForIOError() throws IOException, URISyntaxException {
        String dummyCsr = readValidDummyCSR();
        String errorMessage = "Dummy Error Message: SNAFU!";
        given(messageSource.getMessage(eq("user.request.text.error"), any(), eq(dummyLocale))).willReturn(errorMessage);
        doThrow(new IOException("TEST!")).when(userDataService).saveUploadedCSR(any(), anyString(), anyString());

        String result = sut.csrText(dummyCsr, dummyLocale, redirectAttributes, auth);
        assertNotNull(result);
        assertEquals("redirect:/", result);

        verify(auth, atLeastOnce()).getPrincipal();
        verify(user, atLeastOnce()).getAttribute("unique_name");
        verify(userDataService, times(1)).saveUploadedCSR(any(), anyString(), anyString());
        verify(messageSource, times(1)).getMessage(eq("user.request.text.error"), any(), eq(dummyLocale));
        verify(redirectAttributes, atLeastOnce()).addFlashAttribute("errorMessage", errorMessage);
    }

    @Test
    public void testCsrText() throws IOException, URISyntaxException {
        String successMessage = "Aaaaaaand ...its gone";
        String dummyCsr = readValidDummyCSR();
        String expectedFileName = expectedUID + ".csr.pem";

        given(messageSource.getMessage(eq("user.request.text.success"), any(), eq(dummyLocale))).willReturn(successMessage);

        String result = sut.csrText(dummyCsr, dummyLocale, redirectAttributes, auth);
        assertNotNull(result);
        assertEquals("redirect:/", result);

        verify(auth, atLeastOnce()).getPrincipal();
        verify(user, atLeastOnce()).getAttribute("unique_name");
        verify(userDataService, times(1)).saveUploadedCSR(expectedUID, expectedFileName, dummyCsr);
        verify(messageSource, times(1)).getMessage(eq("user.request.text.success"), any(), eq(dummyLocale));
        verify(redirectAttributes, atLeastOnce()).addFlashAttribute("message", successMessage);
    }

    private String readValidDummyCSR() throws URISyntaxException, IOException {
        Path dummyCSR = Paths.get(ClassLoader.getSystemResource("dummy.csr.pem").toURI());
        return new String(Files.readAllBytes(dummyCSR));
    }
    
    @Test
    public void testRequestRenew_invalidFileName() throws IOException {
        String errorMessage = "Dummy Error Message invalid file!";
        given(messageSource.getMessage(eq("user.request.renew.cert.invalid"), any(), eq(dummyLocale))).willReturn(errorMessage);
        
    	String result = sut.requestRenew("INVALID", dummyLocale, redirectAttributes, auth);
        assertNotNull(result);
        assertEquals("redirect:/", result);
    
        verify(auth, never()).getPrincipal();
        verify(user, never()).getAttribute("unique_name");
        verify(redirectAttributes, atLeastOnce()).addFlashAttribute("errorMessage", errorMessage);
        verify(userDataService, never()).requestRenewalForCert(anyString(), anyString());
    }
    
    @Test
    public void testRequestRenew_exceptionOccurred() throws IOException {
        String errorMessage = "Dummy Error Message Exception occurred!";
        given(messageSource.getMessage(eq("user.request.renew.error"), any(), eq(dummyLocale))).willReturn(errorMessage);
        doThrow(new IOException("TEST ERROR")).when(userDataService).requestRenewalForCert(anyString(), anyString());
        
        String result = sut.requestRenew("user1.crt.pem", dummyLocale, redirectAttributes, auth);
        assertNotNull(result);
        assertEquals("redirect:/", result);
        
        verify(auth, atLeastOnce()).getPrincipal();
        verify(user, atLeastOnce()).getAttribute("unique_name");
        verify(redirectAttributes, atLeastOnce()).addFlashAttribute("errorMessage", errorMessage);
        verify(userDataService, times(1)).requestRenewalForCert(anyString(), anyString());
    }
    
    @Test
    public void testRequestRenew() throws IOException {
        String successMessage = "Dummy Success - renew requested!";
        given(messageSource.getMessage(eq("user.request.renew.success"), any(), eq(dummyLocale))).willReturn(successMessage);
        
        String result = sut.requestRenew("user1.crt.pem", dummyLocale, redirectAttributes, auth);
        assertNotNull(result);
        assertEquals("redirect:/", result);
        
        verify(auth, atLeastOnce()).getPrincipal();
        verify(user, atLeastOnce()).getAttribute("unique_name");
        verify(redirectAttributes, atLeastOnce()).addFlashAttribute("message", successMessage);
        verify(redirectAttributes, never()).addFlashAttribute(eq("errorMessage"), anyString());
        verify(userDataService, times(1)).requestRenewalForCert(anyString(), anyString());
    }
}
