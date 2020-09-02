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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import wtf.hmg.pki.csc.model.CertInfo;
import wtf.hmg.pki.csc.service.UserDataService;
import wtf.hmg.pki.csc.util.CscUtils;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Controller
public class UIController {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ADMIN_GROUP = "ROLE_DevOps";

    @Autowired
    private UserDataService userDataService;
    @Autowired
    private MessageSource messageSource;

    @GetMapping("/")
    public String indexPage(final Model model, final OAuth2AuthenticationToken auth) {
        OAuth2User user = auth.getPrincipal();
        String uid = determineUID(user);
        List<String> userCSRList = userDataService.findCertificateRequestsForUser(uid);
        List<String> userAcceptedCSRList = userDataService.findAcceptedCertificateRequestsForUser(uid);
        List<String> userRejectedCSRList = userDataService.findRejectedCertificateRequestsForUser(uid);
        List<CertInfo> userCertificates = userDataService.findCertificatesForUser(uid);

        boolean isAdmin = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch(a -> StringUtils.equals(ADMIN_GROUP, a));

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("user", user);
        model.addAttribute("userCSRList", userCSRList);
        model.addAttribute("userAcceptedCSRList", userAcceptedCSRList);
        model.addAttribute("userRejectedCSRList", userRejectedCSRList);
        model.addAttribute("userCertificates", userCertificates);

        return "indexPage";
    }
    
    @PostMapping("/requestRenew")
    public String requestRenew(@RequestParam final String fileName, final Locale locale,
                               final RedirectAttributes redirectAttributes, final OAuth2AuthenticationToken auth) {
        if(fileName == null || !CscUtils.isValidCSRFileName(fileName)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("user.request.renew.cert.invalid", new Object[]{fileName}, locale));
            return "redirect:/";
        }
    
        String uid = determineUID(auth);
        try {
            userDataService.requestRenewalForCert(uid, fileName);
            redirectAttributes.addFlashAttribute("message",
                    messageSource.getMessage("user.request.renew.success", null, locale));
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("user.request.renew.error", new Object[]{e.getMessage()}, locale));
        }
        
        return "redirect:/";
    }
    
    @GetMapping("/certs/{fileName:.+}")
    @ResponseBody
    public ResponseEntity<Resource> downloadCert(@PathVariable final String fileName, final OAuth2AuthenticationToken auth) {
        String uid = determineUID(auth);
        Resource resource;

        try {
            resource = userDataService.userCertificateFileAsResource(uid, fileName);
        } catch (IOException e) {
            log.info("Unable to read certificate-file for user. ", e);
            return ResponseEntity.badRequest().build();
        }

        if(resource == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/x-pem-file"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @GetMapping("/intermediate-ca.cert.pem")
    @ResponseBody
    public ResponseEntity<Resource> downloadCaCert() {
        Resource resource;
        try {
            resource = userDataService.caCertificateAsResource();
        } catch (IOException e) {
            log.error("Unable to serve ca-certificate!", e);
            return ResponseEntity.status(500).build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/x-pem-file"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"intermediate-ca.cert.pem\"")
                .body(resource);
    }

    @GetMapping("/list.crl")
    @ResponseBody
    public ResponseEntity<Resource> downloadRevocationList() {
        Resource resource;

        try {
            resource = userDataService.certRevocationListAsResource();
        } catch (IOException e) {
            log.error("Unable to serve revocation list!", e);
            return ResponseEntity.status(500).build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/x-pem-file"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"list.crl\"")
                .body(resource);
    }

    @PostMapping("/csrFile")
    public String csrFile(@RequestParam("file") final MultipartFile file, final Locale locale,
                          final RedirectAttributes redirectAttributes, final OAuth2AuthenticationToken auth) {
        String fileName = file.getOriginalFilename();
        if(fileName == null || !CscUtils.isValidCSRFileName(fileName)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("user.request.file.invalid", new Object[]{fileName}, locale));
            return "redirect:/";
        }

        log.info("Received csr-file: {}", fileName);

        String uid = determineUID(auth);
        try {
            userDataService.saveUploadedCSR(uid, file);
            redirectAttributes.addFlashAttribute("message",
                    messageSource.getMessage("user.request.file.success", null, locale));
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("user.request.file.error", new Object[]{e.getMessage()}, locale));
        }

        return "redirect:/";
    }

    @PostMapping("/csrText")
    public String csrText(@RequestParam("csrText") final String csrText, final Locale locale,
                          final RedirectAttributes redirectAttributes, final OAuth2AuthenticationToken auth) {
        if(!CscUtils.validateCSRString(csrText)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("user.request.text.invalid", null, locale));
            return "redirect:/";
        }

        log.info("Received csr-text");

        String uid = determineUID(auth);
        String fileName = uid + ".csr.pem";
        try {
            userDataService.saveUploadedCSR(uid, fileName, csrText);
            redirectAttributes.addFlashAttribute("message",
                    messageSource.getMessage("user.request.text.success", null, locale));
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("user.request.text.error", new Object[]{e.getMessage()}, locale));
        }

        return "redirect:/";
    }

    private String determineUID(final OAuth2AuthenticationToken auth) {
        OAuth2User user = auth.getPrincipal();
        return determineUID(user);
    }

    private String determineUID(final OAuth2User user) {
        return CscUtils.normalizeUserName(StringUtils.lowerCase(user.getAttribute("unique_name")));
    }

    public void setUserDataService(final UserDataService userDataService) {
        this.userDataService = userDataService;
    }

    public void setMessageSource(final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

}
