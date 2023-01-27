/*
 Copyright (C) 2023, Martin Drößler <m.droessler@handelsblattgroup.com>
 Copyright (C) 2023, Handelsblatt GmbH

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuditLog {
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	
	protected void setLogger(final Logger logger) {
		this.logger = logger;
	}
	
	public void logSignedCSR(final String operatingUser, final String certName, final String certUser) {
		logger.info("Operator '{}' SIGNED CSR '{}' of user '{}'", operatingUser, certName, certUser);
	}
	
	public void logRejectedCSR(final String operatingUser, final String certName, final String certUser) {
		logger.info("Operator '{}' REJECTED CSR '{}' of user '{}'", operatingUser, certName, certUser);
	}
	
	public void logRevokedCert(final String operatingUser, final String certName, final String certUser) {
		logger.info("Operator '{}' REVOKED Cert '{}' of user '{}'", operatingUser, certName, certUser);
	}
	
	public void logRenewedCert(final String operatingUser, final String certName, final String certUser) {
		logger.info("Operator '{}' RENEWED Cert '{}' of user '{}'", operatingUser, certName, certUser);
	}
	
	public void logCreatedSharedApp(final String operatingUser, final String appName, final String teamName) {
		logger.info("SHARED App Operator '{}' CREATED APP/CSR '{}' for team '{}'", operatingUser, appName, teamName);
	}
	
	public void logSignedSharedAppCSR(final String operatingUser, final String appName) {
		logger.info("SHARED APP Operator '{}' SIGNED APP-CSR '{}'", operatingUser, appName);
	}
	
	public void logRequestedSharedAppRenewal(final String operatingUser, final String appName) {
		logger.info("SHARED APP Operator '{}' REQUESTED RENEWAL for APP '{}'", operatingUser, appName);
	}
	
	public void logRenewedSharedAppCert(final String operatingUser, final String appName) {
		logger.info("SHARED APP Operator '{}' RENEWED Cert for APP '{}'", operatingUser, appName);
	}
	
	public void logRevokedSharedAppCert(final String operatingUser, final String appName) {
		logger.info("SHARED APP Operator '{}' REVOKED Cert for APP '{}'", operatingUser, appName);
	}
}
