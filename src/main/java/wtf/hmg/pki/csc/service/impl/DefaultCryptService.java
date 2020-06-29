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
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import wtf.hmg.pki.csc.config.AppConfig;
import wtf.hmg.pki.csc.service.FilesService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class DefaultCryptService implements wtf.hmg.pki.csc.service.CryptService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private FilesService filesService;

    @Override
    public void decryptFile(final Path file, final String password) throws IOException {
        Path output = file.getParent().resolve(file.getFileName().toString() + ".tmp");
        runCommandLine( "Decrypting file",
                "openssl", "enc", "-d", "-aes256", "-a", "-S", appConfig.getCryptSalt(), "-pbkdf2",
                "-iter", "20000", "-pass", "pass:"+password, "-in", file.toString(), "-out", output.toString());

        filesService.move(output, file, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void encryptFile(final Path file, final String password) throws IOException {
        Path output = file.getParent().resolve(file.getFileName().toString() + ".tmp");
        runCommandLine( "Encrypting file",
                "openssl", "enc", "-aes256", "-a", "-S", appConfig.getCryptSalt(), "-pbkdf2",
                "-iter", "20000", "-pass", "pass:"+password, "-in", file.toString(), "-out", output.toString());

        filesService.move(output, file, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public Path signCertificateRequest(final Path csrFile, final String keyPassword) throws IOException {
        Path repo = appConfig.getStoragePath().resolve("cert-repo");
        Process p = runCommandLine("Sign CSR",
                repo.resolve("sign-csr.sh").toString(), repo.toString(), csrFile.toString(), keyPassword);

        List<String> lines = IOUtils.readLines(p.getInputStream(), Charset.defaultCharset());
        if(CollectionUtils.isEmpty(lines)) {
            throw new IllegalStateException("Unexpected Script-Output!");
        }

        return repo.resolve(lines.get(lines.size()-1));
    }

    @Override
    public void revokeCertificate(final Path cert, final String keyPassword) throws IOException {
        Path repo = appConfig.getStoragePath().resolve("cert-repo");
        runCommandLine("Revoke Certificate",
                repo.resolve("revoke-cert.sh").toString(), repo.toString(), cert.toString(), keyPassword);
    }

    private Process runCommandLine(final String description, final String... cmd) throws IOException {
        ProcessBuilder pb = processBuilder();
        pb.command(cmd);

        Process p = pb.start();
        int result;
        try {
            result = p.waitFor();
        } catch (InterruptedException e) {
            throw new IllegalStateException(description + " failed!", e);
        }

        if(result > 0) {
            logStreamError(p.getErrorStream());
            throw new IllegalStateException(description + " failed! Return-Code was: " + result);
        }

        return p;
    }

    private void logStreamError(final InputStream ios) {
        try {
            log.error(IOUtils.toString(ios, Charset.defaultCharset()));
        } catch (IOException e) {
            log.debug("Cannot read error-stream");
        }
    }

    protected ProcessBuilder processBuilder() {
        return new ProcessBuilder();
    }

    public void setAppConfig(final AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public void setFilesService(final FilesService filesService) {
        this.filesService = filesService;
    }

}
