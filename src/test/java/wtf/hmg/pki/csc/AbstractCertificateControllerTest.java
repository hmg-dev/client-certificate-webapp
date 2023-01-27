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
import org.mockito.Mock;
import org.springframework.security.oauth2.core.user.OAuth2User;
import wtf.hmg.pki.csc.service.CertificateService;
import wtf.hmg.pki.csc.util.AuditLog;

import java.io.IOException;
import java.nio.file.Path;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AbstractCertificateControllerTest {
	@Mock
	protected AuditLog auditLog;
	@Mock
	protected CertificateService certificateService;
	@Mock
	protected OAuth2User user;
	@Mock
	protected Path csrRepoFile;
	@Mock
	protected Path certFile;
	
	protected String cryptPassword = "NARF";
	protected String adminName = "Pinky";
	
	protected void verifyPrepareWorkspace() throws IOException, GitAPIException {
		verify(certificateService, times(1)).cleanupWorkingFiles();
		verify(certificateService, times(1)).cloneCertificateRepository();
		verify(certificateService, times(1)).decryptWorkingFiles(cryptPassword);
	}
}
