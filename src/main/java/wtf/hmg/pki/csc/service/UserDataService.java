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

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface UserDataService {

    List<String> findCertificateRequestsForUser(String userName);

    List<String> findAcceptedCertificateRequestsForUser(String userName);

    List<String> findRejectedCertificateRequestsForUser(String userName);

    List<String> findCertificatesForUser(String userName);

    Resource userCertificateFileAsResource(String userName, String filename) throws IOException;

    Resource caCertificateAsResource() throws IOException;

    Resource certRevocationListAsResource() throws IOException;

    void saveUploadedCSR(String userName, MultipartFile csrFile) throws IOException;

    void saveUploadedCSR(String userName, String fileName, String fileData) throws IOException;
}
