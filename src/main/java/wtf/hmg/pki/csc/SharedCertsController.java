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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import wtf.hmg.pki.csc.service.CryptService;
import wtf.hmg.pki.csc.service.SharedAppService;
import wtf.hmg.pki.csc.util.CscUtils;
import wtf.hmg.pki.csc.util.SupportUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

@Controller
public class SharedCertsController extends AbstractCertificateController {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private SupportUtils supportUtils;
	@Autowired
	private MessageSource messageSource;
	@Autowired
	private SharedAppService sharedAppService;
	@Autowired
	private CryptService cryptService;
	
	private static ReentrantLock lock = new ReentrantLock();
	
	@GetMapping("/shared-certs")
	@PreAuthorize("hasRole('PKI-Shared-App')")
	public String sharedCertsPage(final Model model, final OAuth2AuthenticationToken auth) {
		model.addAttribute("sharedApps", sharedAppService.findSharedApps());
		model.addAttribute("isAdmin", supportUtils.isAdmin(auth.getPrincipal()));
		return "sharedCertsPage";
	}
	
	@PostMapping("/createSharedApp")
	@PreAuthorize("hasRole('PKI-Shared-App')")
	public String createSharedApp(@RequestParam final String appName, @RequestParam final String teamName, @RequestParam final String teamContact, 
								  final Locale locale, final RedirectAttributes redirectAttributes) {
		if(!validateAppName(appName)) {
			redirectAttributes.addFlashAttribute("errorMessage",
					messageSource.getMessage("shared.certs.appname.invalid", null, locale));
			return "redirect:/shared-certs";
		}
		if(StringUtils.isNotBlank(teamContact) && !sharedAppService.isValidEMail(teamContact)) {
			redirectAttributes.addFlashAttribute("errorMessage",
					messageSource.getMessage("shared.certs.contact.invalid", null, locale));
			return "redirect:/shared-certs";
		}
		
		String password;
		try {
			password = sharedAppService.createAppKey(appName);
			sharedAppService.createCSR(appName, password);
			sharedAppService.createAppDetails(appName, teamName, teamContact);
		} catch (IOException | IllegalStateException e) {
			redirectAttributes.addFlashAttribute("errorMessage",
					messageSource.getMessage("shared.certs.creation.failed", new Object[]{e.getMessage()}, locale));
			return "redirect:/shared-certs";
		}
		
		redirectAttributes.addFlashAttribute("message",
				messageSource.getMessage("shared.certs.creation.success", new Object[]{password}, locale));
		
		return "redirect:/shared-certs";
	}
	
	@PostMapping("/signAppCSR")
	@PreAuthorize("hasRole('DevOps')")
	public String signAppCSR(final String appName, final String csrFileName, final String cryptPassword, final String serverKeyPassword,
							 final RedirectAttributes redirectAttributes, final OAuth2AuthenticationToken auth) {
		if(!lock.tryLock()) {
			redirectAttributes.addFlashAttribute("errorMessage", "Concurrent access prevented! Try again in a minute.");
			return "redirect:/shared-certs";
		}
		
		String operatingUser = auth.getPrincipal().getName();
		try {
			prepareWorkspace(cryptPassword);
			Path csrFile = certificateService.copyAppCSRToRepository(appName, csrFileName);
			Path certFile = cryptService.signCertificateRequest(csrFile, serverKeyPassword);
			certificateService.copyCertificateToAppDirectory(appName, certFile);
			finishWorkspace(cryptPassword, operatingUser, "Signed App-Certificate");
			redirectAttributes.addFlashAttribute("message", "Certificate has been signed successfully!");
		} catch (GitAPIException|IOException|IllegalStateException e) {
			log.error("Unable to sign App-CSR!", e);
			redirectAttributes.addFlashAttribute("errorMessage", 
					"Something went horribly wrong! Unable to execute sign-operation! Error was: " + e.getMessage());
		} finally {
			lock.unlock();
		}
		
		return "redirect:/shared-certs";
	}
	
	@GetMapping("/apps/{appName}/{fileName:.+}")
	@ResponseBody
	@PreAuthorize("hasRole('PKI-Shared-App')")
	public ResponseEntity<Resource> downloadAppFile(@PathVariable final String appName,
													@PathVariable final String fileName) {
		Resource resource;
		try {
			resource = sharedAppService.appFileAsResource(appName, fileName);
		} catch (IOException e) {
			log.info("Unable to read app-file. ", e);
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
	
	@PostMapping("/requestAppRenew")
	@PreAuthorize("hasRole('PKI-Shared-App')")
	public String requestAppRenew(@RequestParam final String appName, @RequestParam final String fileName,
								  final Locale locale, final RedirectAttributes redirectAttributes) {
		if(fileName == null || !CscUtils.isValidCSRFileName(fileName)) {
			redirectAttributes.addFlashAttribute("errorMessage",
					messageSource.getMessage("shared.certs.request.renew.cert.invalid", new Object[]{fileName}, locale));
			return "redirect:/shared-certs";
		}
		
		try {
			sharedAppService.requestRenewalForCert(appName, fileName);
			redirectAttributes.addFlashAttribute("message",
					messageSource.getMessage("shared.certs.request.renew.success", null, locale));
		} catch (IOException e) {
			log.info("Failed User-Interaction 'requestRenew': ", e);
			redirectAttributes.addFlashAttribute("errorMessage",
					messageSource.getMessage("shared.certs.request.renew.error", new Object[]{e.getMessage()}, locale));
		}
		
		return "redirect:/shared-certs";
	}
	
	@PostMapping("/renewAppCert")
	@PreAuthorize("hasRole('DevOps')")
	public String renewAppCert(final String appName, final String crtFileName, final String cryptPassword, final String serverKeyPassword,
								final RedirectAttributes redirectAttributes, final OAuth2AuthenticationToken auth) {
		if(!lock.tryLock()) {
			redirectAttributes.addFlashAttribute("errorMessage", "Concurrent access prevented! Try again in a minute.");
			return "redirect:/shared-certs";
		}
		
		String operatingUser = auth.getPrincipal().getName();
		try {
			prepareWorkspace(cryptPassword);
			
			Path cert = findAppFileForFilename(appName, crtFileName);
			cryptService.revokeCertificate(cert, serverKeyPassword);
			sharedAppService.deleteAppFile(appName, crtFileName);
			
			Path csr = findAppFileForFilename(appName, StringUtils.replace(crtFileName, "crt.pem", "csr.pem"));
			Path certFile = cryptService.signCertificateRequest(csr, serverKeyPassword);
			certificateService.copyCertificateToAppDirectory(appName, certFile);
			sharedAppService.deleteAppFile(appName, crtFileName + ".reqrenew");
			
			finishWorkspace(cryptPassword, operatingUser, "Renewed App-Certificate");
			redirectAttributes.addFlashAttribute("message", "Certificate has been successfully renewed!");
		} catch (GitAPIException|IOException|IllegalStateException e) {
			log.error("Unable to sign App-CSR!", e);
			redirectAttributes.addFlashAttribute("errorMessage",
					"Something went horribly wrong! Unable to execute renew-operation! Error was: " + e.getMessage());
		} finally {
			lock.unlock();
		}
		
		return "redirect:/shared-certs";
	}
	
	@PostMapping("/revokeAppCert")
	@PreAuthorize("hasRole('DevOps')")
	public String revokeAppCert(final String appName, final String crtFileName, final String cryptPassword, final String serverKeyPassword,
								final RedirectAttributes redirectAttributes, final OAuth2AuthenticationToken auth) {
		if(!lock.tryLock()) {
			redirectAttributes.addFlashAttribute("errorMessage", "Concurrent access prevented! Try again in a minute.");
			return "redirect:/shared-certs";
		}
		
		String operatingUser = auth.getPrincipal().getName();
		try {
			prepareWorkspace(cryptPassword);
			Path cert = findAppFileForFilename(appName, crtFileName);
			cryptService.revokeCertificate(cert, serverKeyPassword);
			sharedAppService.deleteAppFile(appName, crtFileName);
			// TODO support deleting the whole app
			finishWorkspace(cryptPassword, operatingUser, "Revoked App-Certificate");
			redirectAttributes.addFlashAttribute("message", "Certificate has been successfully revoked!");
		} catch (GitAPIException|IOException|IllegalStateException e) {
			log.error("Unable to sign App-CSR!", e);
			redirectAttributes.addFlashAttribute("errorMessage",
					"Something went horribly wrong! Unable to execute revoke-operation! Error was: " + e.getMessage());
		} finally {
			lock.unlock();
		}
		
		return "redirect:/shared-certs";
	}
	
	private Path findAppFileForFilename(final String appName, final String fileName) {
		Path cert = sharedAppService.findAppFileForFilename(appName, fileName);
		if(cert == null) {
			throw new IllegalStateException("Inconsistent App-data: no corresponding certificate file for CSR found!");
		}
		return cert;
	}
	
	private boolean validateAppName(final String appName) {
		return appName != null && appName.matches("[A-Za-z0-9_-]+");
	}
	
	protected static void setLock(final ReentrantLock lock) {
		SharedCertsController.lock = lock;
	}
	
	public void setSupportUtils(final SupportUtils supportUtils) {
		this.supportUtils = supportUtils;
	}
	
	public void setMessageSource(final MessageSource messageSource) {
		this.messageSource = messageSource;
	}
	
	public void setSharedAppService(final SharedAppService sharedAppService) {
		this.sharedAppService = sharedAppService;
	}
	
	public void setCryptService(final CryptService cryptService) {
		this.cryptService = cryptService;
	}
}
