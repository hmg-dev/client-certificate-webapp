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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wtf.hmg.pki.csc.config.AppConfig;
import wtf.hmg.pki.csc.model.CSR;
import wtf.hmg.pki.csc.model.CertInfo;
import wtf.hmg.pki.csc.service.FilesService;
import wtf.hmg.pki.csc.util.CscUtils;
import wtf.hmg.pki.csc.util.SupportUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.temporal.Temporal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DefaultAdminDataService implements wtf.hmg.pki.csc.service.AdminDataService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private FilesService filesService;
    @Autowired
    private SupportUtils supportUtils;

    @Override
    public List<CSR> findPendingCertificateRequests() {
        try {
            return filesService.find(appConfig.getStoragePath().resolve("users"), 2, CscUtils::isValidCSRFile)
                    .map(this::pathToCSR)
                    .collect(Collectors.toList());
        } catch (IOException|IllegalStateException e) {
            log.warn("Unable to find pending csr", e);
        }

        return Collections.emptyList();
    }

    @Override
    public List<CSR> findSignedCertificateRequests() {
        try {
            return filesService.find(appConfig.getStoragePath().resolve("users"), 3, CscUtils::isSignedCSRFile)
                    .map(this::pathToSignedCSR)
                    .collect(Collectors.toList());
        } catch (IOException|IllegalStateException e) {
            log.warn("Unable to find pending csr", e);
        }
        return Collections.emptyList();
    }
    
    @Override
    public List<CertInfo> findRevokedCertificates() {
        try {
            return filesService.find(appConfig.getStoragePath().resolve("users"), 3, CscUtils::isRevokedCertFile)
                    .map(this::pathToCertInfo)
                    .collect(Collectors.toList());
        } catch (IOException|IllegalStateException e) {
            log.warn("Unable to find pending csr", e);
        }
        
        return Collections.emptyList();
    }

    private CSR pathToCSR(final Path path) {
        return pathToCSRAndUser(path, path.getParent());
    }

    private CSR pathToSignedCSR(final Path path) {
        return pathToCSRAndUser(path, path.getParent().getParent());
    }

    private CSR pathToCSRAndUser(final Path path, final Path userFolder) {
        String baseName = StringUtils.substringBefore(path.getFileName().toString(), ".csr");
        Path certFile = userFolder.resolve("certs").resolve(baseName + ".crt.pem.reqrenew");
        
        CSR.Builder b = new CSR.Builder();
        b.csrFile(path);
        b.csrInfo(CscUtils.extractCSRInfo(path));
        b.userName(userFolder.getFileName().toString());
        b.lastModified(supportUtils.determineLastModified(path));
        b.lastRenewed(determineLastRenewed(path));
        b.renewalRequested(Files.isRegularFile(certFile));

        return b.build();
    }
    
    private CertInfo pathToCertInfo(final Path path) {
        return pathToCertInfoAndUser(path, path.getParent().getParent());
    }
    
    private CertInfo pathToCertInfoAndUser(final Path path, final Path userFolder) {
        CertInfo.Builder b = new CertInfo.Builder();
        b.certFile(path);
        b.userName(userFolder.getFileName().toString());
        b.lastModified(supportUtils.determineLastModified(path));
        
        return b.build();
    }
    
    private Temporal determineLastRenewed(final Path path) {
        Path renewPath = path.getParent().resolve(path.getFileName().toString() + ".renewed");
        if(Files.isRegularFile(renewPath)) {
            return supportUtils.determineLastModified(renewPath);
        }
        
        return null;
    }
    
    @Override
    public Path findAcceptedCSR(final String userName, final String fileName) {
        return appConfig.getStoragePath().resolve("users").resolve(userName).resolve("accepted").resolve(fileName);
    }
    
    @Override
    public Path findUserCertForRequest(final String userName, final String fileName) {
        String baseName = StringUtils.substringBefore(fileName, ".csr");
        Path certFile = appConfig.getStoragePath().resolve("users").resolve(userName).resolve("certs").resolve(baseName + ".crt.pem");

        return filesService.exists(certFile) ? certFile : null;
    }
    
    @Override
    public void flagRevokedUserCert(final String userName, final String fileName) throws IOException {
        Path certFile = findUserCertForRequest(userName, fileName);
        Path certRevokedPath = appConfig.getStoragePath().resolve("users").resolve(userName).resolve("revoked").resolve(certFile.getFileName());
    
        filesService.createDirectories(certRevokedPath.getParent());
        Path target = filesService.move(certFile, certRevokedPath, StandardCopyOption.REPLACE_EXISTING);
        filesService.setLastModifiedTime(target, FileTime.fromMillis(System.currentTimeMillis()));
    }
    
    @Override
    public void flagRevokedUserCertAndCSR(final String userName, final String fileName) throws IOException {
        Path source = appConfig.getStoragePath().resolve("users").resolve(userName).resolve("accepted").resolve(fileName);
        Path target = appConfig.getStoragePath().resolve("users").resolve(userName).resolve("rejected").resolve(fileName);

        filesService.createDirectories(target.getParent());
        filesService.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        
        flagRevokedUserCert(userName, fileName);
    }

    @Override
    public void rejectUserCSR(final String userName, final String fileName) throws IOException {
        Path source = appConfig.getStoragePath().resolve("users").resolve(userName).resolve(fileName);
        Path target = appConfig.getStoragePath().resolve("users").resolve(userName).resolve("rejected").resolve(fileName);

        filesService.createDirectories(target.getParent());
        filesService.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void acceptUserCSR(final String userName, final String fileName) throws IOException {
        Path source = appConfig.getStoragePath().resolve("users").resolve(userName).resolve(fileName);
        Path target = appConfig.getStoragePath().resolve("users").resolve(userName).resolve("accepted").resolve(fileName);

        filesService.createDirectories(target.getParent());
        filesService.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
    
    @Override
    public void flagCSRasRenewed(final Path csrFile) throws IOException {
        Path renewPath = csrFile.getParent().resolve(csrFile.getFileName().toString() + ".renewed");
        if(Files.isRegularFile(renewPath)) {
            filesService.setLastModifiedTime(renewPath, FileTime.fromMillis(System.currentTimeMillis()));
        } else {
            filesService.createFile(renewPath);
        }
    
        String baseName = StringUtils.substringBefore(csrFile.getFileName().toString(), ".csr");
        Path certFile = csrFile.getParent().getParent().resolve("certs").resolve(baseName + ".crt.pem.reqrenew");
        if(Files.isRegularFile(certFile)) {
            filesService.deleteRecursively(certFile);
        }
    }
    
    public void setAppConfig(final AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public void setFilesService(final FilesService filesService) {
        this.filesService = filesService;
    }
    
    public void setSupportUtils(final SupportUtils supportUtils) {
        this.supportUtils = supportUtils;
    }
}
