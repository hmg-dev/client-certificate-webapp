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
package wtf.hmg.pki.csc.service;

import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Path;

public interface CertificateService {
    Path cloneCertificateRepository() throws GitAPIException;

    void commitAndPushChanges(String operatingUser, String message) throws IOException, GitAPIException;

    void cleanupWorkingFiles() throws IOException;

    void decryptWorkingFiles(String password) throws IOException;

    void encryptWorkingFiles(String password) throws IOException;

    Path copyUserCSRToRepository(String userName, String csrFileName) throws IOException;

    void copyCertificateToUserDirectory(String userName, Path certFile) throws IOException;
}
