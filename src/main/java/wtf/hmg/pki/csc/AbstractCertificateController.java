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
import org.springframework.beans.factory.annotation.Autowired;
import wtf.hmg.pki.csc.service.CertificateService;
import wtf.hmg.pki.csc.util.AuditLog;

import java.io.IOException;

public abstract class AbstractCertificateController {
	@Autowired
	protected CertificateService certificateService;
	@Autowired
	protected AuditLog auditLog;
	
	protected void prepareWorkspace(final String cryptPassword) throws IOException, GitAPIException {
		certificateService.cleanupWorkingFiles();
		certificateService.cloneCertificateRepository();
		certificateService.decryptWorkingFiles(cryptPassword);
	}
	
	protected void finishWorkspace(final String cryptPassword, final String operatingUser, final String operation) throws IOException, GitAPIException {
		certificateService.encryptWorkingFiles(cryptPassword);
		certificateService.commitAndPushChanges(operatingUser, operation);
	}
	
	public void setCertificateService(final CertificateService certificateService) {
		this.certificateService = certificateService;
	}
	
	public void setAuditLog(final AuditLog auditLog) {
		this.auditLog = auditLog;
	}
}
