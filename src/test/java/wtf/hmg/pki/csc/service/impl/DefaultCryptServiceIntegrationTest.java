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

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;
import wtf.hmg.pki.csc.config.AppConfig;
import wtf.hmg.pki.csc.service.FilesService;
import wtf.hmg.pki.csc.util.SupportUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultCryptServiceIntegrationTest {

    private DefaultCryptService sut;

    private String dummySalt = "CEED";
    private AppConfig appConfig;
    private FilesService filesService;

    private static Path tempStoragePath;

    @BeforeClass
    public static void init() throws IOException {
        tempStoragePath = Files.createTempDirectory("csc-enc");
    }

    @Before
    public void setUp() {
        appConfig = new AppConfig();
        appConfig.setCryptSalt(dummySalt);
        appConfig.setStoragePath(tempStoragePath);

        filesService = new WrapperFilesService();

        sut = new DefaultCryptService();
        sut.setAppConfig(appConfig);
        sut.setFilesService(filesService);
        sut.setSupportUtils(new SupportUtils());
    }

    @Test
    public void testDecryptRealFile() throws URISyntaxException, IOException {
        Path dummyReferenceFile = Paths.get(ClassLoader.getSystemResource("dummy.decrypted.txt").toURI());
        Path dummyEncFile = Paths.get(ClassLoader.getSystemResource("dummy.encrypted.txt").toURI());
        Path tempTarget = tempStoragePath.resolve(dummyEncFile.getFileName());

        Files.copy(dummyEncFile, tempTarget);
        assertTrue(Files.exists(tempTarget));

        sut.decryptFile(tempTarget, "NARF");

        assertTrue(Files.exists(tempTarget));
        assertEquals(new String(Files.readAllBytes(tempTarget)), new String(Files.readAllBytes(dummyReferenceFile)));
    }

    @Test
    public void testEncryptRealFile() throws URISyntaxException, IOException {
        Path dummyReferenceFile = Paths.get(ClassLoader.getSystemResource("dummy.encrypted.txt").toURI());
        Path dummyEncFile = Paths.get(ClassLoader.getSystemResource("dummy.decrypted.txt").toURI());
        Path tempTarget = tempStoragePath.resolve(dummyEncFile.getFileName());

        Files.copy(dummyEncFile, tempTarget);
        assertTrue(Files.exists(tempTarget));

        sut.encryptFile(tempTarget, "NARF");

        assertTrue(Files.exists(tempTarget));
        assertEquals(new String(Files.readAllBytes(tempTarget)), new String(Files.readAllBytes(dummyReferenceFile)));
    }

    @AfterClass
    public static void tearDown() throws IOException {
        FileSystemUtils.deleteRecursively(tempStoragePath);
    }
}
