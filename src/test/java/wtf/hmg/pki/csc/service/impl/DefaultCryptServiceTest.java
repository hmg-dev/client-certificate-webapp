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

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import wtf.hmg.pki.csc.config.AppConfig;
import wtf.hmg.pki.csc.service.FilesService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCryptServiceTest {

    private DefaultCryptService sut;

    private AppConfig appConfig;
    private Path dummyStoragePath = Paths.get("/data/projects/csc");
    private String dummySalt = "CEED";

    @Mock
    private ProcessBuilder processBuilder;
    @Mock
    private FilesService filesService;
    @Mock
    private Process dummyProcess;

    @Before
    public void setUp() throws IOException {
        appConfig = new AppConfig();
        appConfig.setCryptSalt(dummySalt);
        appConfig.setStoragePath(dummyStoragePath);

        sut = new DefaultCryptService() {
            protected ProcessBuilder processBuilder() {
                return processBuilder;
            }
        };
        sut.setAppConfig(appConfig);
        sut.setFilesService(filesService);

        given(processBuilder.start()).willReturn(dummyProcess);
    }

    @Test
    public void testDecryptFileForExecutionError() throws IOException, InterruptedException {
        Path input = dummyStoragePath.resolve("cert-repo/enc.pem");
        String dummyPassword = "NARF";

        doThrow(new InterruptedException("TEST")).when(dummyProcess).waitFor();

        try {
            sut.decryptFile(input, dummyPassword);
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
            assertNotNull(e.getCause());
        }

        verify(processBuilder, times(1)).start();
        verify(dummyProcess, times(1)).waitFor();
        verifyNoInteractions(filesService);
    }

    @Test
    public void testDecryptFileForErrorResult() throws IOException, InterruptedException {
        Path input = dummyStoragePath.resolve("cert-repo/enc.pem");
        String dummyPassword = "NARF";
        InputStream errorStream = mock(InputStream.class);

        given(dummyProcess.waitFor()).willReturn(1);
        given(dummyProcess.getErrorStream()).willReturn(errorStream);

        try {
            sut.decryptFile(input, dummyPassword);
            fail();
        } catch (IllegalStateException e) {
            assertNotNull(e);
            assertNull(e.getCause());
        }

        verify(processBuilder, times(1)).start();
        verify(dummyProcess, times(1)).waitFor();
        verify(dummyProcess, times(1)).getErrorStream();
        verifyNoInteractions(filesService);
    }

    @Test
    public void testDecryptFile() throws IOException, InterruptedException {
        Path input = dummyStoragePath.resolve("cert-repo/enc.pem");
        Path expectedOutput = dummyStoragePath.resolve("cert-repo/enc.pem.tmp");
        String dummyPassword = "NARF";

        sut.decryptFile(input, dummyPassword);

        verify(processBuilder, times(1))
                .command("openssl", "enc", "-d", "-aes256", "-a", "-S", dummySalt, "-pbkdf2",
                        "-iter", "20000", "-pass", "pass:"+dummyPassword, "-in", input.toString(),
                        "-out", input.toString() + ".tmp");
        verify(processBuilder, times(1)).start();
        verify(dummyProcess, times(1)).waitFor();
        verify(filesService, times(1)).move(expectedOutput, input, StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    public void testEncryptFile() throws IOException, InterruptedException {
        Path input = dummyStoragePath.resolve("cert-repo/enc.pem");
        Path expectedOutput = dummyStoragePath.resolve("cert-repo/enc.pem.tmp");
        String dummyPassword = "NARF";

        sut.encryptFile(input, dummyPassword);

        verify(processBuilder, times(1))
                .command("openssl", "enc", "-aes256", "-a", "-S", dummySalt, "-pbkdf2",
                        "-iter", "20000", "-pass", "pass:"+dummyPassword, "-in", input.toString(),
                        "-out", input.toString() + ".tmp");
        verify(processBuilder, times(1)).start();
        verify(dummyProcess, times(1)).waitFor();
        verify(filesService, times(1)).move(expectedOutput, input, StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    public void testSignCertificateRequest() throws IOException {
        Path script = dummyStoragePath.resolve("cert-repo/sign-csr.sh");
        Path input = dummyStoragePath.resolve("cert-repo/intermediate/csr/user1.csr.pem");
        String keyPassword = "NARF";
        String certLocation = "intermediate/certs/user1.crt.pem";
        Path expectedResult = dummyStoragePath.resolve("cert-repo").resolve(certLocation);

        String dummySignScriptOutput = "The matching entry has the following details\n" +
                "Type          :Valid\n" +
                "Expires on    :220218141316Z\n" +
                "Serial Number :1001\n" +
                "File name     :unknown\n" +
                "Subject Name  :/C=DE/ST=NRW/O=Test Company GmbH/OU=DPD/CN=PINKY/emailAddress=pinky.brain@valid.email\n" +
                "Certificate is in: \n" +
                certLocation;

        given(dummyProcess.getInputStream()).willReturn(IOUtils.toInputStream(dummySignScriptOutput, Charset.defaultCharset()));

        Path result = sut.signCertificateRequest(input, keyPassword);
        assertNotNull(result);
        assertEquals(expectedResult, result);

        verify(processBuilder, times(1)).command(script.toString(),
                script.getParent().toString(), input.toString(), keyPassword);
        verify(dummyProcess, times(1)).getInputStream();
    }

    @Test
    public void testRevokeCertificate() throws IOException, InterruptedException {
        Path script = dummyStoragePath.resolve("cert-repo/revoke-cert.sh");
        Path input = dummyStoragePath.resolve("users/user1/certs/user1.crt.pem");
        String keyPassword = "NARF";

        sut.revokeCertificate(input, keyPassword);

        verify(processBuilder, times(1)).command(script.toString(),
                script.getParent().toString(), input.toString(), keyPassword);
        verify(processBuilder, times(1)).start();
        verify(dummyProcess, times(1)).waitFor();
    }

}
