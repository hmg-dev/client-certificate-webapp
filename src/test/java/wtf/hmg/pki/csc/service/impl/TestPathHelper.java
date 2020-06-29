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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestPathHelper {

    public static void initDummyFileStructure(final Path dummyStoragePath) throws IOException {
        Path users = dummyStoragePath.resolve("users");
        Path user1 = users.resolve("user1");

        Path user1ac = user1.resolve("accepted");
        Path user1rc = user1.resolve("rejected");
        Path user1certs = user1.resolve("certs");
        Path caCert = dummyStoragePath.resolve("intermediate.cert.pem");

        Files.createDirectory(users);
        createUserDirectories(users, "user0", "user1", "user2", "user3");
        Files.createDirectory(user1ac);
        Files.createDirectory(user1rc);
        Files.createDirectory(user1certs);
        Files.write(user1.resolve("user1.csr.pem"), "DUMMY-CSR".getBytes());
        Files.write(user1ac.resolve("user1-ac.csr.pem"), "DUMMY-CSR Accepted".getBytes());
        Files.write(user1rc.resolve("user1-rc.csr.pem"), "DUMMY-CSR Rejected".getBytes());
        Files.write(user1certs.resolve("user1.crt.pem"), "DUMMY-CERT".getBytes());
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
}
