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
package wtf.hmg.pki.csc.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.temporal.Temporal;

@Service
public class SupportUtils {
	
	protected static final String ADMIN_GROUP = "ROLE_DevOps";
	protected static final String SHARED_APP_ADMIN_GROUP = "ROLE_PKI-Shared-App";
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public boolean isAdmin(final OAuth2User user) {
		return user.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority).anyMatch(a -> StringUtils.equals(ADMIN_GROUP, a));
	}
	
	public boolean isSharedAppAdmin(final OAuth2User user) {
		return user.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority).anyMatch(a -> StringUtils.equals(SHARED_APP_ADMIN_GROUP, a));
	}
	
	public String generatePassword(final int length) {
		return RandomStringUtils.randomAlphanumeric(length);
	}
	
	public Process runCommandLine(final String description, final String... cmd) throws IOException {
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
	
	protected ProcessBuilder processBuilder() {
		return new ProcessBuilder();
	}
	
	private void logStreamError(final InputStream ios) {
		try {
			log.error(IOUtils.toString(ios, Charset.defaultCharset()));
		} catch (IOException e) {
			log.debug("Cannot read error-stream");
		}
	}
	
	public Temporal determineLastModified(final Path path) {
		try {
			return Files.getLastModifiedTime(path).toInstant();
		} catch (IOException e) {
			throw new IllegalStateException("Unable to determine lastModifiedDate of: " + path, e);
		}
	}
	
	public String normalizeFileName(final String fileName) {
		if(StringUtils.contains(fileName, "/")) {
			return StringUtils.substringAfterLast(fileName, "/");
		}
		return fileName;
	}
}
