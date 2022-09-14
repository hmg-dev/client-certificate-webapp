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

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SupportUtilsTest {
	
	private SupportUtils sut;
	
	@Mock
	private ProcessBuilder processBuilder;
	@Mock
	private Process dummyProcess;
	
	@Before
	public void init() throws IOException {
		sut = new SupportUtils(){
			protected ProcessBuilder processBuilder() {
				return processBuilder;
			}
		};
		
		given(processBuilder.start()).willReturn(dummyProcess);
	}
	
	@Test
	public void testIsAdmin() {
		OAuth2User user = mock(OAuth2User.class);
		GrantedAuthority authority1 = mock(GrantedAuthority.class);
		GrantedAuthority authority2 = mock(GrantedAuthority.class);
		Collection<GrantedAuthority> authorities = Arrays.asList(authority1, authority2);
		Collection<GrantedAuthority> userAuthorities = Arrays.asList(authority1);
		
		willReturn(authorities).given(user).getAuthorities();
		given(authority1.getAuthority()).willReturn("ROLE_company.domain");
		given(authority2.getAuthority()).willReturn(SupportUtils.ADMIN_GROUP);
		
		assertTrue(sut.isAdmin(user));
		
		willReturn(userAuthorities).given(user).getAuthorities();
		assertFalse(sut.isAdmin(user));
		
		verify(user, atLeastOnce()).getAuthorities();
	}
	
	@Test
	public void testIsSharedAppAdmin() {
		OAuth2User user = mock(OAuth2User.class);
		GrantedAuthority authority1 = mock(GrantedAuthority.class);
		GrantedAuthority authority2 = mock(GrantedAuthority.class);
		Collection<GrantedAuthority> authorities = Arrays.asList(authority1, authority2);
		Collection<GrantedAuthority> userAuthorities = Arrays.asList(authority1);
		
		willReturn(authorities).given(user).getAuthorities();
		given(authority1.getAuthority()).willReturn("ROLE_company.domain");
		given(authority2.getAuthority()).willReturn(SupportUtils.SHARED_APP_ADMIN_GROUP);
		
		assertTrue(sut.isSharedAppAdmin(user));
		
		willReturn(userAuthorities).given(user).getAuthorities();
		assertFalse(sut.isSharedAppAdmin(user));
		
		verify(user, atLeastOnce()).getAuthorities();
	}
	
	@Test
	public void testGeneratePassword() {
		int expectedLength = 32;
		
		String result = sut.generatePassword(expectedLength);
		assertPasswordCharacteristics(result, expectedLength);
		
		String result2 = sut.generatePassword(expectedLength);
		assertPasswordCharacteristics(result2, expectedLength);
		assertNotEquals(result, result2);
	}
	
	private void assertPasswordCharacteristics(final String pw, final int expectedLength) {
		assertNotNull(pw);
		assertEquals(expectedLength, pw.length());
		assertTrue(StringUtils.isAlphanumeric(pw));
	}
	
	@Test
	public void testRunCommandLine() throws IOException, InterruptedException {
		String dummyDescription = "Test Run";
		String[] dummyCommand = new String[] { "/usr/bin/exa" , "-alFh" };
		
		Process result = sut.runCommandLine(dummyDescription, dummyCommand);
		assertNotNull(result);
		assertEquals(dummyProcess, result);
		
		verify(processBuilder, times(1)).command(dummyCommand);
		verify(processBuilder, times(1)).start();
		verify(dummyProcess, times(1)).waitFor();
	}
	
	@Test
	public void testRunCommandLine_forExecutionError() throws InterruptedException, IOException {
		String dummyDescription = "Test Run";
		String[] dummyCommand = new String[] { "/usr/bin/exa" , "-alFh" };
		doThrow(new InterruptedException("TEST")).when(dummyProcess).waitFor();
		
		try {
			sut.runCommandLine(dummyDescription, dummyCommand);
			fail();
		} catch (IllegalStateException e) {
			assertNotNull(e.getMessage());
		}
		
		verify(processBuilder, times(1)).start();
		verify(dummyProcess, times(1)).waitFor();
	}
	
	@Test
	public void testRunCommandLine_forErrorResult() throws InterruptedException, IOException {
		String dummyDescription = "Test Run";
		String[] dummyCommand = new String[] { "/usr/bin/exa" , "-alFh" };
		InputStream errorStream = mock(InputStream.class);
		
		given(dummyProcess.waitFor()).willReturn(1);
		given(dummyProcess.getErrorStream()).willReturn(errorStream);
		
		try {
			sut.runCommandLine(dummyDescription, dummyCommand);
			fail();
		} catch (IllegalStateException e) {
			assertNotNull(e.getMessage());
		}
		
		verify(processBuilder, times(1)).start();
		verify(dummyProcess, times(1)).waitFor();
		verify(dummyProcess, times(1)).getErrorStream();
	}
	
	@Test
	public void testNormalizeFileName() {
		assertEquals("", sut.normalizeFileName(""));
		assertEquals("NARF", sut.normalizeFileName("NARF"));
		assertEquals("NARF", sut.normalizeFileName("/NARF"));
		assertEquals("NARF", sut.normalizeFileName("Zort/NARF"));
		assertEquals("NARF.pem", sut.normalizeFileName("Zort/NARF.pem"));
		assertEquals("NARF.pem", sut.normalizeFileName("../../Zort/NARF.pem"));
	}
	
	@Test
	public void testDetermineLastModified() throws URISyntaxException {
		Path dummyCSR = Paths.get(ClassLoader.getSystemResource("dummy.csr.pem").toURI());
		Temporal result = sut.determineLastModified(dummyCSR);
		
		assertNotNull(result);
	}
	
	@Test(expected = IllegalStateException.class)
	public void testDetermineLastModifiedForInvalidPath() {
		sut.determineLastModified(Paths.get("/invalid"));
	}
}
