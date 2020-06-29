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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import wtf.hmg.pki.csc.model.CSR;
import wtf.hmg.pki.csc.service.AdminDataService;
import wtf.hmg.pki.csc.service.CertificateService;
import wtf.hmg.pki.csc.service.CryptService;
import wtf.hmg.pki.csc.util.CscUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Controller
public class AdminUIController {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private AdminDataService adminDataService;
    @Autowired
    private CertificateService certificateService;
    @Autowired
    private CryptService cryptService;

    private static ReentrantLock lock = new ReentrantLock();

    @GetMapping("/admin")
    @PreAuthorize("hasRole('DevOps')")
    public String adminPage(final Model model, final OAuth2AuthenticationToken auth) {
        List<CSR> pendingRequests = adminDataService.findPendingCertificateRequests();
        List<CSR> signedRequests = adminDataService.findSignedCertificateRequests();

        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("signedRequests", signedRequests);
        model.addAttribute("user", auth.getPrincipal());

        return "adminPage";
    }

    @PostMapping("/rejectCSR")
    @PreAuthorize("hasRole('DevOps')")
    public String rejectUserCSR(@RequestParam("userName") final String userName,
                                @RequestParam("fileName") final String fileName,
                                final RedirectAttributes redirectAttributes) {
        try {
            adminDataService.rejectUserCSR(CscUtils.normalizeUserName(userName), fileName);
            redirectAttributes.addFlashAttribute("message", "CSR successfully rejected!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to execute reject-operation! Error was: " + e.getMessage());
        }

        return "redirect:/admin";
    }

    @PostMapping("/signCSR")
    @PreAuthorize("hasRole('DevOps')")
    public String signCSR(@RequestParam("userName") final String userName,
                          @RequestParam("fileName") final String fileName,
                          @RequestParam("cryptPassword") final String cryptPassword,
                          @RequestParam("keyPassword") final String keyPassword,
                          final RedirectAttributes redirectAttributes, final OAuth2AuthenticationToken auth) {
        String operatingUser = auth.getPrincipal().getName();
        if(!lock.tryLock()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Concurrent access prevented! Try again in a minute.");
            return "redirect:/admin";
        }
        try {
            signCertificateRequest(CscUtils.normalizeUserName(userName), fileName, cryptPassword, keyPassword, operatingUser);
            redirectAttributes.addFlashAttribute("message", "Certificate has been signed successfully!");
        } catch (GitAPIException|IOException|IllegalStateException e) {
            log.error("Unable to sign CSR!", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Something went horribly wrong! Unable to execute sign-operation! Error was: " + e.getMessage());
        } finally {
            lock.unlock();
        }

        return "redirect:/admin";
    }

    private void signCertificateRequest(final String userName, final String fileName, final String cryptPassword,
                                        final String keyPassword, final String operatingUser) throws IOException, GitAPIException {
        prepareWorkspace(cryptPassword);
        Path csrFile = certificateService.copyUserCSRToRepository(userName,fileName);
        Path certFile = cryptService.signCertificateRequest(csrFile, keyPassword);
        certificateService.copyCertificateToUserDirectory(userName, certFile);
        certificateService.encryptWorkingFiles(cryptPassword);
        adminDataService.acceptUserCSR(userName, fileName);
        certificateService.commitAndPushChanges(operatingUser, "Signed User-Certificate");
    }

    @PostMapping("/revokeCert")
    @PreAuthorize("hasRole('DevOps')")
    public String revokeCert(final String userName, final String fileName, final String cryptPassword,
                             final String keyPassword, final RedirectAttributes redirectAttributes, final OAuth2AuthenticationToken auth) {
        String operatingUser = auth.getPrincipal().getName();
        if(!lock.tryLock()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Concurrent access prevented! Try again in a minute.");
            return "redirect:/admin";
        }

        try {
            prepareWorkspace(cryptPassword);
            Path cert = findUserCertForRequest(CscUtils.normalizeUserName(userName), fileName);
            cryptService.revokeCertificate(cert, keyPassword); // re-generate CRL?
            adminDataService.flagRevokedUserCertAndCSR(CscUtils.normalizeUserName(userName), fileName);
            certificateService.encryptWorkingFiles(cryptPassword);
            certificateService.commitAndPushChanges(operatingUser, "Revoked User-Certificate");
            redirectAttributes.addFlashAttribute("message", "Certificate has been successfully revoked!");
        } catch (GitAPIException | IOException | IllegalStateException e) {
            log.error("Unable to revoke CSR!", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Something went horribly wrong! Unable to execute REVOKE-operation! Error was: " + e.getMessage());
        } finally {
            lock.unlock();
        }

        return "redirect:/admin";
    }

    private Path findUserCertForRequest(final String userName, final String fileName) {
        Path cert = adminDataService.findUserCertForRequest(userName, fileName);
        if(cert == null) {
            throw new IllegalStateException("Inconsistent Userdata: no corresponding certificate file for CSR found!");
        }
        return cert;
    }

    private void prepareWorkspace(final String cryptPassword) throws IOException, GitAPIException {
        certificateService.cleanupWorkingFiles();
        certificateService.cloneCertificateRepository();
        certificateService.decryptWorkingFiles(cryptPassword);
    }

    public void setAdminDataService(final AdminDataService adminDataService) {
        this.adminDataService = adminDataService;
    }

    public void setCertificateService(final CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    protected static void setLock(final ReentrantLock lock) {
        AdminUIController.lock = lock;
    }

    public void setCryptService(final CryptService cryptService) {
        this.cryptService = cryptService;
    }

}
