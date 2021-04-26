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
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import wtf.hmg.pki.csc.config.AppConfig;
import wtf.hmg.pki.csc.model.CertInfo;
import wtf.hmg.pki.csc.service.FilesService;
import wtf.hmg.pki.csc.service.UserDataService;
import wtf.hmg.pki.csc.util.CscUtils;
import wtf.hmg.pki.csc.util.SupportUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DefaultUserDataService implements UserDataService {

    private static final String USERS_SUBDIR = "users";
    private static final String ACCEPTED_CSR_SUBDIR = "accepted";
    private static final String REJECTED_CSR_SUBDIR = "rejected";
    private static final String CERTS_SUBDIR = "certs";
    public static final String CA_CERT_FILE = "intermediate.cert.pem";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private AppConfig appConfig;
    @Autowired
    private FilesService filesService;
    @Autowired
    private SupportUtils supportUtils;

    @Override
    public List<String> findCertificateRequestsForUser(final String userName) {
        Path userPath = appConfig.getStoragePath().resolve(USERS_SUBDIR).resolve(userName);
        return findUserFilesInternal(userPath, userName);
    }

    @Override
    public List<String> findAcceptedCertificateRequestsForUser(final String userName) {
        Path userPath = appConfig.getStoragePath().resolve(USERS_SUBDIR).resolve(userName).resolve(ACCEPTED_CSR_SUBDIR);
        return findUserFilesInternal(userPath, userName);
    }

    @Override
    public List<String> findRejectedCertificateRequestsForUser(final String userName) {
        Path userPath = appConfig.getStoragePath().resolve(USERS_SUBDIR).resolve(userName).resolve(REJECTED_CSR_SUBDIR);
        return findUserFilesInternal(userPath, userName);
    }

    @Override
    public List<CertInfo> findCertificatesForUser(final String userName) {
        Path userPath = appConfig.getStoragePath().resolve(USERS_SUBDIR).resolve(userName).resolve(CERTS_SUBDIR);
        try(Stream<Path> certs = Files.find(userPath, 1, CscUtils::isValidCSRFile)) {
            return certs.map(path -> pathToUserCertInfo(path, userName)).collect(Collectors.toList());
        } catch (IOException e) {
            log.info("Unable to find CertFiles for user: {}", e.getMessage());
            log.debug("Unable to find CertFiles for user: {}", userName, e);  // http://slf4j.org/faq.html#paramException
        }
        return Collections.emptyList();
    }
    
    private CertInfo pathToUserCertInfo(final Path certFile, final String userName) {
        Path renewRequest = certFile.getParent().resolve(certFile.getFileName().toString() + ".reqrenew");
        CertInfo.Builder b = new CertInfo.Builder();
        b.userName(userName)
                .certFile(certFile)
                .renewalRequested(Files.isRegularFile(renewRequest));
        try {
            b.renewed(isCertRenewed(certFile));
        } catch (IOException e) {
            log.warn("Unable to determine renewed state", e);
        }
        return b.build();
    }
    
    private List<String> findUserFilesInternal(final Path userPath, final String userName) {
        try(Stream<Path> files = Files.find(userPath, 1, CscUtils::isValidCSRFile)) {
            return files.map(path -> path.getFileName().toString()).collect(Collectors.toList());
        } catch (IOException e) {
            log.info("Unable to find CSRFiles for user: {}", e.getMessage());
            log.debug("Unable to find CSRFiles for user: {}", userName, e);
        }

        return Collections.emptyList();
    }

    @Override
    public Resource userCertificateFileAsResource(final String userName, final String filename) throws IOException {
        Path userPath = appConfig.getStoragePath().resolve(USERS_SUBDIR).resolve(userName).resolve(CERTS_SUBDIR).resolve(supportUtils.normalizeFileName(filename));
        if(Files.isRegularFile(userPath)) {
            return new UrlResource(userPath.toUri());
        }
        return null;
    }

    @Override
    public Resource caCertificateAsResource() throws IOException {
        Path caPath = appConfig.getStoragePath().resolve(CA_CERT_FILE);
        return new UrlResource(caPath.toUri());
    }

    @Override
    public Resource certRevocationListAsResource() throws IOException {
        return new UrlResource(appConfig.getCertRevocationListPath().toUri());
    }

    @Override
    public void saveUploadedCSR(final String userName, final MultipartFile csrFile) throws IOException {
        if(!CscUtils.validateCSRString(new String(csrFile.getBytes()))) {
            throw new IOException("Invalid CSR File!");
        }
        Path csrPath = findAndValidateTargetCSRPath(userName, csrFile.getOriginalFilename());
        csrFile.transferTo(csrPath);
    }

    @Override
    public void saveUploadedCSR(final String userName, final String fileName, final String fileData) throws IOException {
        Path csrPath = findAndValidateTargetCSRPath(userName, fileName);
        Files.write(csrPath, fileData.getBytes());
    }

    private Path findAndValidateTargetCSRPath(final String userName, final String fileName) throws IOException {
        Path userPath = findAndEnsureUserDirectory(userName);
        Path csrPath = userPath.resolve(supportUtils.normalizeFileName(fileName));
        if(Files.exists(csrPath)) {
            throw new IOException("Request-File already exists: " + csrPath.getFileName());
        }

        return csrPath;
    }
    
    private Path findAndEnsureUserDirectory(final String userName) throws IOException {
        Path userPath = appConfig.getStoragePath().resolve(USERS_SUBDIR).resolve(userName);
        if(!Files.exists(userPath)) {
            Files.createDirectory(userPath);
        }
        return userPath;
    }
    
    @Override
    public boolean isCertRenewed(final Path cert) throws IOException {
        String basename = StringUtils.substringBefore(cert.getFileName().toString(), ".crt");
        Path userdir = cert.getParent().getParent();
        Path renewed = userdir.resolve("accepted").resolve(basename + ".csr.pem.renewed");
        Path renewReq = cert.getParent().resolve(cert.getFileName().toString() + ".reqrenew");
        
        if(!filesService.isRegularFile(renewed)) {
            return false;
        }
        if(!filesService.isRegularFile(renewReq)) {
            return true;
        }
    
        FileTime renewTime = filesService.getLastModifiedTime(renewed);
        FileTime requestTime = filesService.getLastModifiedTime(renewReq);
        
        return renewTime.compareTo(requestTime) > 0;
    }
    
    @Override
    public void requestRenewalForCert(final String userName, final String certFileName) throws IOException {
        Path userDir = findAndEnsureUserDirectory(userName);
        Path reqFile = userDir.resolve("certs").resolve(certFileName + ".reqrenew");
        
        if(filesService.isRegularFile(reqFile)) {
            filesService.setLastModifiedTime(reqFile, FileTime.fromMillis(System.currentTimeMillis()));
        } else {
            filesService.createFile(reqFile);
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
