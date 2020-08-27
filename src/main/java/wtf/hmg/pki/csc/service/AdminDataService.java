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

import wtf.hmg.pki.csc.model.CSR;
import wtf.hmg.pki.csc.model.CertInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface AdminDataService {
    List<CSR> findPendingCertificateRequests();

    List<CSR> findSignedCertificateRequests();
	
	List<CertInfo> findRevokedCertificates();
	
	Path findAcceptedCSR(String userName, String fileName);
	
	Path findUserCertForRequest(String userName, String fileName);
	
	void flagRevokedUserCert(String userName, String fileName) throws IOException;
	
	void flagRevokedUserCertAndCSR(String userName, String fileName) throws IOException;

    void rejectUserCSR(String userName, String fileName) throws IOException;

    void acceptUserCSR(String userName, String fileName) throws IOException;
	
	void flagCSRasRenewed(Path csrFile) throws IOException;
}
