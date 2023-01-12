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
package wtf.hmg.pki.csc.service.impl;

import wtf.hmg.pki.csc.service.SharedAppService;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestPathHelper {

    public static void initDummyFileStructure(final Path dummyStoragePath) throws IOException {
        Path users = dummyStoragePath.resolve("users");
        Path user1 = users.resolve("user1");

        Path user1ac = user1.resolve("accepted");
        Path user1rc = user1.resolve("rejected");
        Path user1certs = user1.resolve("certs");
        Path user1revoked = user1.resolve("revoked");
        Path caCert = dummyStoragePath.resolve("intermediate.cert.pem");

        Files.createDirectory(users);
        createUserDirectories(users, "user0", "user1", "user2", "user3");
        Files.createDirectory(user1ac);
        Files.createDirectory(user1rc);
        Files.createDirectory(user1certs);
        Files.createDirectory(user1revoked);
        Files.write(user1.resolve("user1.csr.pem"), "DUMMY-CSR".getBytes());
        Files.write(user1ac.resolve("user1-ac.csr.pem"), "DUMMY-CSR Accepted".getBytes());
        Files.write(user1ac.resolve("user1-ac.csr.pem.renewed"), "1".getBytes());
        Files.write(user1rc.resolve("user1-rc.csr.pem"), "DUMMY-CSR Rejected".getBytes());
        Files.write(user1certs.resolve("user1.crt.pem"), "DUMMY-CERT".getBytes());
        Files.write(user1certs.resolve("user1.crt.pem.reqrenew"), "renewal-request".getBytes());
        Files.write(user1certs.resolve("user1-ac.crt.pem.reqrenew"), "renewal-request".getBytes());
        Files.write(user1revoked.resolve("user1.crt.pem"), "DUMMY-CERT".getBytes());
        Files.write(caCert, "DUMMY CA-CERT".getBytes());

        Files.write(users.resolve("user2/user2.crs.pem"), "user2-DUMMY-CRS".getBytes());
        Files.write(users.resolve("user3/user2.crs.pem"), "user3-DUMMY-CRS".getBytes());
    }

    private static void createUserDirectories(final Path parentDir, final String... user) throws IOException {
        for(String u : user) {
            Path userDir = parentDir.resolve(u);
            Files.createDirectory(userDir);
        }
    }
    
    public static void initDummyAppsStructure(final Path dummyStoragePath) throws IOException, URISyntaxException {
        Path apps = dummyStoragePath.resolve(SharedAppService.SHARED_APPS_FOLDER);
        Path appNoCert = apps.resolve("app-nocert");
        Path appOnlyKey = apps.resolve("app-onlykey");
        Path appWithCert = apps.resolve("app-withcert");
        Path appWithCertRenewReq = apps.resolve("app-withcert-reneq");
        Path appWithTeamDetails = apps.resolve("app-withteam");
        Path dummyCertFile = Paths.get(ClassLoader.getSystemResource("dummy.crt.pem").toURI());
        Path dummyTeamProperties = Paths.get(ClassLoader.getSystemResource("dummy.appdetails.properties").toURI());
    
        Files.createDirectory(apps);
        Files.createDirectory(appNoCert);
        Files.createDirectory(appOnlyKey);
        Files.createDirectory(appWithCert);
        Files.createDirectory(appWithCertRenewReq);
        Files.createDirectory(appWithTeamDetails);
    
        Files.copy(dummyCertFile, appWithCert.resolve("app-withcert.crt.pem"));
        Files.copy(dummyCertFile, appWithCertRenewReq.resolve("app-withcert-reneq.crt.pem"));
        Files.copy(dummyTeamProperties, appWithTeamDetails.resolve("appdetails.properties"));
        
        Files.write(appNoCert.resolve("app-nocert.key.pem"), "DUMMY-KEY".getBytes());
        Files.write(appNoCert.resolve("app-nocert.csr.pem"), "DUMMY-CSR".getBytes());
        Files.write(appOnlyKey.resolve("app-onlykey.key.pem"), "DUMMY-KEY".getBytes());
        Files.write(appWithCert.resolve("app-withcert.key.pem"), "DUMMY-KEY".getBytes());
        Files.write(appWithCert.resolve("app-withcert.csr.pem"), "DUMMY-CSR".getBytes());
        //Files.write(appWithCert.resolve("app-withcert.crt.pem"), "DUMMY-CRT".getBytes());
        Files.write(appWithCertRenewReq.resolve("app-withcert-reneq.key.pem"), "DUMMY-KEY".getBytes());
        Files.write(appWithCertRenewReq.resolve("app-withcert-reneq.csr.pem"), "DUMMY-CSR".getBytes());
        //Files.write(appWithCertRenewReq.resolve("app-withcert-reneq.crt.pem"), "DUMMY-CRT".getBytes());
        Files.write(appWithCertRenewReq.resolve("app-withcert-reneq.crt.pem.reqrenew"), "Renew-Request".getBytes());
    }
}
