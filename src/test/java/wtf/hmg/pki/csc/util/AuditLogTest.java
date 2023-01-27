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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuditLogTest {
	
	private AuditLog sut;
	
	@Mock
	private Logger auditLogger;
	
	private final String dummyOperatingUser = "Test0r";
	private final String dummyCertName = "user1.crt.pem";
	private final String dummyCertUser = "user1";
	private final String dummyAppName = "app1";
	private final String dummyTeamName = "Team NARF";
	
	@Before
	public void init() {
		sut = new AuditLog();
		sut.setLogger(auditLogger);
	}
	
	@Test
	public void testLogSignedCSR() {
		sut.logSignedCSR(dummyOperatingUser, dummyCertName, dummyCertUser);
		verify(auditLogger, times(1)).info("Operator '{}' SIGNED CSR '{}' of user '{}'", dummyOperatingUser, dummyCertName, dummyCertUser);
	}
	
	@Test
	public void testLogRejectedCSR() {
		sut.logRejectedCSR(dummyOperatingUser, dummyCertName, dummyCertUser);
		verify(auditLogger, times(1)).info("Operator '{}' REJECTED CSR '{}' of user '{}'", dummyOperatingUser, dummyCertName, dummyCertUser);
	}
	
	@Test
	public void testLogRevokedCert() {
		sut.logRevokedCert(dummyOperatingUser, dummyCertName, dummyCertUser);
		verify(auditLogger, times(1)).info("Operator '{}' REVOKED Cert '{}' of user '{}'", dummyOperatingUser, dummyCertName, dummyCertUser);
	}
	
	@Test
	public void testLogRenewedCert() {
		sut.logRenewedCert(dummyOperatingUser, dummyCertName, dummyCertUser);
		verify(auditLogger, times(1)).info("Operator '{}' RENEWED Cert '{}' of user '{}'", dummyOperatingUser, dummyCertName, dummyCertUser);
	}
	
	@Test
	public void testLogCreatedSharedApp() {
		sut.logCreatedSharedApp(dummyOperatingUser, dummyAppName, dummyTeamName);
		verify(auditLogger, times(1)).info("SHARED App Operator '{}' CREATED APP/CSR '{}' for team '{}'", dummyOperatingUser, dummyAppName, dummyTeamName);
	}
	
	@Test
	public void testLogSignedSharedAppCSR() {
		sut.logSignedSharedAppCSR(dummyOperatingUser, dummyAppName);
		verify(auditLogger, times(1)).info("SHARED APP Operator '{}' SIGNED APP-CSR '{}'", dummyOperatingUser, dummyAppName);
	}
	
	@Test
	public void testLogRequestedSharedAppRenewal() {
		sut.logRequestedSharedAppRenewal(dummyOperatingUser, dummyAppName);
		verify(auditLogger, times(1)).info("SHARED APP Operator '{}' REQUESTED RENEWAL for APP '{}'", dummyOperatingUser, dummyAppName);
	}
	
	@Test
	public void testLogRenewedSharedAppCert() {
		sut.logRenewedSharedAppCert(dummyOperatingUser, dummyAppName);
		verify(auditLogger, times(1)).info("SHARED APP Operator '{}' RENEWED Cert for APP '{}'", dummyOperatingUser, dummyAppName);
	}
	
	@Test
	public void testLogRevokedSharedAppCert() {
		sut.logRevokedSharedAppCert(dummyOperatingUser, dummyAppName);
		verify(auditLogger, times(1)).info("SHARED APP Operator '{}' REVOKED Cert for APP '{}'", dummyOperatingUser, dummyAppName);
	}
}
