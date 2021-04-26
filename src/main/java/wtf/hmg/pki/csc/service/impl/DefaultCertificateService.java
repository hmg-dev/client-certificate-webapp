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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wtf.hmg.pki.csc.config.AppConfig;
import wtf.hmg.pki.csc.service.CertificateService;
import wtf.hmg.pki.csc.service.CryptService;
import wtf.hmg.pki.csc.service.FilesService;
import wtf.hmg.pki.csc.service.SharedAppService;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DefaultCertificateService implements CertificateService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private TransportConfigCallback transportConfigCallback;
    @Autowired
    private FilesService filesService;
    @Autowired
    private CryptService cryptService;

    @Override
    public Path cloneCertificateRepository() throws GitAPIException {
        Path targetPath = certRepo();

        log.debug("Clone from {} to {} using key {}", appConfig.getCertRepo(), targetPath, appConfig.getGitUserKeyfile());

        cloneCommand().setURI(appConfig.getCertRepo())
            .setDirectory(targetPath.toFile())
            .setTransportConfigCallback(transportConfigCallback)
        .call();

        return targetPath;
    }

    protected CloneCommand cloneCommand() {
        return Git.cloneRepository();
    }

    @Override
    public void commitAndPushChanges(final String operatingUser, final String message) throws IOException, GitAPIException {
        Git gitRepo = openRepository(certRepo());

        gitRepo.add().addFilepattern(".").call();
        gitRepo.commit().setMessage(message + " - done by: " + operatingUser).call();
        gitRepo.push().setTransportConfigCallback(transportConfigCallback).call();
    }

    protected Git openRepository(final Path repo) throws IOException {
        return Git.open(repo.toFile());
    }

    @Override
    public void cleanupWorkingFiles() throws IOException {
        Path certRepoPath = certRepo();
        if(filesService.exists(certRepoPath)) {
            filesService.deleteRecursively(certRepoPath);
        }
    }

    @Override
    public void decryptWorkingFiles(final String password) throws IOException {
        List<Path> files = filesService.find(certRepo(), 4, this::validFileForEncryption).collect(Collectors.toList());

        for(Path p : files) {
            cryptService.decryptFile(p, password);
        }
    }

    @Override
    public void encryptWorkingFiles(final String password) throws IOException {
        List<Path> files = filesService.find(certRepo(), 4, this::validFileForEncryption).collect(Collectors.toList());

        for(Path p : files) {
            cryptService.encryptFile(p, password);
        }
    }

    private boolean validFileForEncryption(final Path path, final BasicFileAttributes attr) {
        return attr.isRegularFile() && StringUtils.endsWithAny(path.getFileName().toString(), "pem", "csr", "pfx", "crt");
    }

    @Override
    public Path copyUserCSRToRepository(final String userName, final String csrFileName) throws IOException {
        Path sourcePath = appConfig.getStoragePath().resolve("users").resolve(userName).resolve(csrFileName);
        Path targetPath = certRepo().resolve("intermediate/csr").resolve(csrFileName);

        filesService.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

        return targetPath;
    }
    
    @Override
    public Path copyAppCSRToRepository(final String appName, final String csrFileName) throws IOException {
        Path sourcePath = appConfig.getStoragePath().resolve(SharedAppService.SHARED_APPS_FOLDER).resolve(appName).resolve(csrFileName);
        Path targetPath = certRepo().resolve("intermediate/csr").resolve(csrFileName);
    
        filesService.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    
        return targetPath;
    }

    @Override
    public void copyCertificateToUserDirectory(final String userName, final Path certFile) throws IOException {
        Path targetPath = appConfig.getStoragePath().resolve("users").resolve(userName).resolve("certs").resolve(certFile.getFileName().toString());

        filesService.createDirectories(targetPath.getParent());
        filesService.copy(certFile, targetPath, StandardCopyOption.REPLACE_EXISTING); // FIXME: to replace or not to replace!?
    }
    
    @Override
    public void copyCertificateToAppDirectory(final String appName, final Path certFile) throws IOException {
        Path targetPath = appConfig.getStoragePath().resolve(SharedAppService.SHARED_APPS_FOLDER).resolve(appName).resolve(certFile.getFileName().toString());
        filesService.copy(certFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private Path certRepo() {
        return appConfig.getStoragePath().resolve("cert-repo");
    }

    public void setAppConfig(final AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public void setTransportConfigCallback(final TransportConfigCallback transportConfigCallback) {
        this.transportConfigCallback = transportConfigCallback;
    }

    public void setFilesService(final FilesService filesService) {
        this.filesService = filesService;
    }

    public void setCryptService(final CryptService cryptService) {
        this.cryptService = cryptService;
    }
    
}
