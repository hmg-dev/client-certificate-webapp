/*
 Copyright (C) 2021, Martin Drößler <m.droessler@handelsblattgroup.com>
 Copyright (C) 2021, Handelsblatt GmbH

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import wtf.hmg.pki.csc.config.AppConfig;
import wtf.hmg.pki.csc.model.SharedApp;
import wtf.hmg.pki.csc.service.FilesService;
import wtf.hmg.pki.csc.service.SharedAppService;
import wtf.hmg.pki.csc.util.CscUtils;
import wtf.hmg.pki.csc.util.SupportUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DefaultSharedAppService implements SharedAppService {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private static final int PASSWORD_LENGTH = 32;
	
	@Autowired
	private SupportUtils supportUtils;
	@Autowired
	private AppConfig appConfig;
	@Autowired
	private FilesService filesService;
	
	@Override
	public String createAppKey(final String appName) throws IOException {
		Path appsFolder = appConfig.getStoragePath().resolve(SHARED_APPS_FOLDER);
		Path createKeyScript = appConfig.getScriptsPath().resolve("gen-app-key.sh");
		String password = supportUtils.generatePassword(PASSWORD_LENGTH);
		
		supportUtils.runCommandLine("Create application key", createKeyScript.toString(), 
				appsFolder.toString(), appName, password);
		
		return password;
	}
	
	@Override
	public void createCSR(final String appName, final String password) throws IOException {
		Path appsFolder = appConfig.getStoragePath().resolve(SHARED_APPS_FOLDER);
		Path createKeyScript = appConfig.getScriptsPath().resolve("gen-app-csr.sh");
		
		supportUtils.runCommandLine("Create application CSR", createKeyScript.toString(),
				appsFolder.toString(), appName, password);
	}
	
	@Override
	public Path findAppFileForFilename(final String appName, final String fileName) {
		Path appFile = appConfig.getStoragePath().resolve(SHARED_APPS_FOLDER)
				.resolve(supportUtils.normalizeFileName(appName)).resolve(supportUtils.normalizeFileName(fileName));
		return filesService.exists(appFile) ? appFile : null;
	}
	
	@Override
	public boolean deleteAppFile(final String appName, final String fileName) throws IOException {
		Path appFile = appConfig.getStoragePath().resolve(SHARED_APPS_FOLDER)
				.resolve(supportUtils.normalizeFileName(appName)).resolve(supportUtils.normalizeFileName(fileName));
		return filesService.deleteIfExists(appFile);
	}
	
	@Override
	public Resource appFileAsResource(final String appName, final String filename) throws IOException {
		Path appFilePath = appConfig.getStoragePath().resolve(SHARED_APPS_FOLDER)
				.resolve(supportUtils.normalizeFileName(appName)).resolve(supportUtils.normalizeFileName(filename));
		if(Files.isRegularFile(appFilePath)) {
			return new UrlResource(appFilePath.toUri());
		}
		return null;
	}
	
	@Override
	public void requestRenewalForCert(final String appName, final String certFileName) throws IOException {
		Path reqFile = appConfig.getStoragePath().resolve(SHARED_APPS_FOLDER)
				.resolve(supportUtils.normalizeFileName(appName))
				.resolve(supportUtils.normalizeFileName(certFileName + ".reqrenew"));
		
		if(filesService.isRegularFile(reqFile)) {
			filesService.setLastModifiedTime(reqFile, FileTime.fromMillis(System.currentTimeMillis()));
		} else {
			filesService.createFile(reqFile);
		}
	}
	
	@Override
	public List<SharedApp> findSharedApps() {
		try {
			return findSharedAppsInternal();
		} catch (IOException|IllegalStateException e) {
			log.error("Unable to find shared apps", e);
		}
		
		return Collections.emptyList();
	}
	
	private List<SharedApp> findSharedAppsInternal() throws IOException {
		Path appsFolder = appConfig.getStoragePath().resolve(SHARED_APPS_FOLDER);
		
		return filesService.list(appsFolder).sorted().filter(Files::isDirectory)
				.map(this::pathToSharedApp).collect(Collectors.toList());
	}
	
	private SharedApp pathToSharedApp(final Path p) {
		Path filename = p.getFileName();
		Path cert = resolveIfExists(p, filename.toString() + ".crt.pem");
		Path csr = resolveIfExists(p, filename.toString() + ".csr.pem");
		Path key = resolveIfExists(p, filename.toString() + ".key.pem");
		Path reqrenew = resolveIfExists(p, filename.toString() + ".crt.pem.reqrenew");
		
		SharedApp.Builder b = new SharedApp.Builder();
		b.setName(filename.toString());
		b.setCertFile(cert);
		b.setCsrFile(csr);
		b.setKeyFile(key);
		
		if(cert != null) {
			b.setCertLastModified(supportUtils.determineLastModified(cert));
			b.setCertValidTo(CscUtils.extractCertValidToDate(cert));
		}
		if(csr != null) {
			b.setCsrLastModified(supportUtils.determineLastModified(csr));
			b.setCsrInfo(CscUtils.extractCSRInfo(csr));
		}
		if(key != null) {
			b.setKeyLastModified(supportUtils.determineLastModified(key));
		}
		if(reqrenew != null) {
			b.setRenewalRequested(true);
		}
		
		return b.build();
	}
	
	private Path resolveIfExists(final Path p, final String child) {
		Path c = p.resolve(child);
		return filesService.exists(c) ? c : null;
	}
	
	public void setSupportUtils(final SupportUtils supportUtils) {
		this.supportUtils = supportUtils;
	}
	
	public void setAppConfig(final AppConfig appConfig) {
		this.appConfig = appConfig;
	}
	
	public void setFilesService(final FilesService filesService) {
		this.filesService = filesService;
	}
	
}
