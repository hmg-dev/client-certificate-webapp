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

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;
import org.springframework.util.FileSystemUtils;
import wtf.hmg.pki.csc.config.AppConfig;
import wtf.hmg.pki.csc.model.SharedApp;
import wtf.hmg.pki.csc.service.FilesService;
import wtf.hmg.pki.csc.service.SharedAppService;
import wtf.hmg.pki.csc.util.SupportUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class DefaultSharedAppServiceTest {
	
	private DefaultSharedAppService sut;
	
	private String dummyName = "Test-Application";
	private String dummyPassword = "Test123456App42";
	private static Path tempStoragePath;
	
	@Mock
	private Path dummyStoragePath;
	@Mock
	private Path dummyScriptsPath;
	@Mock
	private Path dummyAppsPath;
	@Mock
	private SupportUtils supportUtils;
	@Mock
	private AppConfig appConfig;
	@Mock
	private FilesService filesService;
	private FilesService realFilesService;
	
	@BeforeClass
	public static void init() throws IOException, URISyntaxException {
		tempStoragePath = Files.createTempDirectory("csca");
		TestPathHelper.initDummyAppsStructure(tempStoragePath);
	}
	
	@AfterClass
	public static void tearDown() throws IOException {
		FileSystemUtils.deleteRecursively(tempStoragePath);
	}
	
	@Before
	public void setup() {
		realFilesService = new WrapperFilesService();
		
		sut = new DefaultSharedAppService();
		sut.setSupportUtils(supportUtils);
		sut.setAppConfig(appConfig);
		sut.setFilesService(filesService);
		
		given(appConfig.getStoragePath()).willReturn(dummyStoragePath);
		given(appConfig.getScriptsPath()).willReturn(dummyScriptsPath);
		given(dummyStoragePath.resolve(SharedAppService.SHARED_APPS_FOLDER)).willReturn(dummyAppsPath);
	}
	
	@Test
	public void testCreateAppKey() throws IOException {
		String expectedDescription = "Create application key";
		String expectedWorkDir = "/data/projects/csc/applications";
		String expectedScript = "/opt/scripts/gen-app-key.sh";
		Path createKeyScript = mock(Path.class);
		
		given(supportUtils.generatePassword(anyInt())).willReturn(dummyPassword);
		given(dummyAppsPath.toString()).willReturn(expectedWorkDir);
		given(dummyScriptsPath.resolve("gen-app-key.sh")).willReturn(createKeyScript);
		given(createKeyScript.toString()).willReturn(expectedScript);
		
		String result = sut.createAppKey(dummyName);
		assertNotNull(result);
		assertEquals(dummyPassword, result);
		
		verify(supportUtils, times(1)).generatePassword(32);
		verify(appConfig, times(1)).getStoragePath();
		verify(appConfig, times(1)).getScriptsPath();
		verify(dummyScriptsPath, times(1)).resolve("gen-app-key.sh");
		verify(dummyStoragePath, times(1)).resolve(SharedAppService.SHARED_APPS_FOLDER);
		verify(supportUtils, times(1)).runCommandLine(expectedDescription, expectedScript, expectedWorkDir, dummyName, dummyPassword);
	}
	
	@Test
	public void testCreateCSR() throws IOException {
		String expectedDescription = "Create application CSR";
		String expectedWorkDir = "/data/projects/csc/applications";
		String expectedScript = "/opt/scripts/gen-app-csr.sh";
		Path createKeyScript = mock(Path.class);
		
		given(dummyAppsPath.toString()).willReturn(expectedWorkDir);
		given(dummyScriptsPath.resolve("gen-app-csr.sh")).willReturn(createKeyScript);
		given(createKeyScript.toString()).willReturn(expectedScript);
		
		sut.createCSR(dummyName, dummyPassword);
		
		verify(appConfig, times(1)).getStoragePath();
		verify(appConfig, times(1)).getScriptsPath();
		verify(dummyScriptsPath, times(1)).resolve("gen-app-csr.sh");
		verify(dummyStoragePath, times(1)).resolve(SharedAppService.SHARED_APPS_FOLDER);
		verify(supportUtils, times(1)).runCommandLine(expectedDescription, expectedScript, expectedWorkDir, dummyName, dummyPassword);
	}
	
	@Test
	public void testFindAppCertForFilename_whenCertFileNotExisting() {
		given(appConfig.getStoragePath()).willReturn(tempStoragePath);
		
		String appName = "app-nocert";
		String fileName = "app-nocert.crt.pem";
		
		given(supportUtils.normalizeFileName(fileName)).willReturn(fileName);
		given(supportUtils.normalizeFileName(appName)).willReturn(appName);
		
		sut.setFilesService(realFilesService);
		Path result = sut.findAppFileForFilename(appName, fileName);
		
		assertNull(result);
		verify(supportUtils, times(1)).normalizeFileName(fileName);
		verify(supportUtils, times(1)).normalizeFileName(appName);
	}
	
	@Test
	public void testFindAppCertForFilename() {
		given(appConfig.getStoragePath()).willReturn(tempStoragePath);
		
		String appName = "app-withcert";
		String fileName = "app-withcert.crt.pem";
		Path expectedResult = tempStoragePath.resolve(SharedAppService.SHARED_APPS_FOLDER).resolve(appName).resolve(fileName);
		
		given(supportUtils.normalizeFileName(fileName)).willReturn(fileName);
		given(supportUtils.normalizeFileName(appName)).willReturn(appName);
		
		sut.setFilesService(realFilesService);
		Path result = sut.findAppFileForFilename(appName, fileName);
		assertNotNull(result);
		assertEquals(expectedResult, result);
		
		verify(supportUtils, times(1)).normalizeFileName(fileName);
		verify(supportUtils, times(1)).normalizeFileName(appName);
	}
	
	@Test
	public void testFindSharedApps_forError() throws IOException {
		doThrow(new IOException("TEST")).when(filesService).list(any());
		
		List<SharedApp> result = sut.findSharedApps();
		assertNotNull(result);
		assertTrue(result.isEmpty());
		
		verify(appConfig, times(1)).getStoragePath();
		verify(filesService, times(1)).list(dummyAppsPath);
	}
	
	@Test
	public void testDeleteAppCertificate() throws IOException {
		String appName = "app-withcert";
		String fileName = "app-withcert.crt.pem";
		Path dummyAppPath = mock(Path.class);
		Path expectedPath = mock(Path.class);
		
		given(supportUtils.normalizeFileName(fileName)).willReturn(fileName);
		given(appConfig.getStoragePath()).willReturn(dummyStoragePath);
		given(dummyStoragePath.resolve(SharedAppService.SHARED_APPS_FOLDER)).willReturn(dummyAppsPath);
		given(dummyAppsPath.resolve(appName)).willReturn(dummyAppPath);
		given(dummyAppPath.resolve(fileName)).willReturn(expectedPath);
		given(supportUtils.normalizeFileName(appName)).willReturn(appName);
		
		sut.deleteAppFile(appName, fileName);
		
		verify(appConfig, times(1)).getStoragePath();
		verify(filesService, times(1)).deleteIfExists(expectedPath);
		verify(supportUtils, times(1)).normalizeFileName(fileName);
		verify(supportUtils, times(1)).normalizeFileName(appName);
	}
	
	@Test
	public void testAppFileAsResource_forNoSuchFile() throws IOException {
		String dummyFilename = "app-test.crt.pem";
		String appName = "app-test";
		
		given(appConfig.getStoragePath()).willReturn(tempStoragePath);
		given(supportUtils.normalizeFileName(dummyFilename)).willReturn(dummyFilename);
		given(supportUtils.normalizeFileName(appName)).willReturn(appName);
		
		Resource resource = sut.appFileAsResource(appName, dummyFilename);
		assertNull(resource);
		
		verify(supportUtils, times(1)).normalizeFileName(dummyFilename);
		verify(supportUtils, times(1)).normalizeFileName(appName);
	}
	
	@Test
	public void testAppFileAsResource() throws IOException, URISyntaxException {
		String dummyFilename = "app-withcert.crt.pem";
		String appName = "app-withcert";
		Path dummyCertFile = Paths.get(ClassLoader.getSystemResource("dummy.crt.pem").toURI());
		
		given(appConfig.getStoragePath()).willReturn(tempStoragePath);
		given(supportUtils.normalizeFileName(dummyFilename)).willReturn(dummyFilename);
		given(supportUtils.normalizeFileName(appName)).willReturn(appName);
		
		Resource resource = sut.appFileAsResource(appName, dummyFilename);
		assertNotNull(resource);
		assertEquals(dummyFilename, resource.getFilename());
		assertTrue(resource.isFile());
		assertTrue(resource.exists());
		assertNotNull(resource.getFile());
		assertEquals(new String(Files.readAllBytes(dummyCertFile)), new String(Files.readAllBytes(resource.getFile().toPath())));
		
		verify(supportUtils, times(1)).normalizeFileName(dummyFilename);
		verify(supportUtils, times(1)).normalizeFileName(appName);
		verify(appConfig, times(1)).getStoragePath();
	}
	
	@Test
	public void testRequestRenewalForCert_existingRequest() throws IOException {
		String appName = "app-test";
		String dummyFilename = "app-test.crt.pem";
		String expectedFilename = dummyFilename + ".reqrenew";
		Path expectedPath = tempStoragePath.resolve(SharedAppService.SHARED_APPS_FOLDER).resolve(appName).resolve(expectedFilename);
		
		given(filesService.isRegularFile(expectedPath)).willReturn(true);
		given(appConfig.getStoragePath()).willReturn(tempStoragePath);
		given(supportUtils.normalizeFileName(expectedFilename)).willReturn(expectedFilename);
		given(supportUtils.normalizeFileName(appName)).willReturn(appName);
		
		sut.requestRenewalForCert(appName, dummyFilename);
		
		verify(filesService, times(1)).isRegularFile(expectedPath);
		verify(filesService, times(1)).setLastModifiedTime(eq(expectedPath), any(FileTime.class));
		verify(filesService, never()).createFile(expectedPath);
		verify(appConfig, times(1)).getStoragePath();
		verify(supportUtils, times(2)).normalizeFileName(anyString());
	}
	
	@Test
	public void testRequestRenewalForCert_newRequest() throws IOException {
		String appName = "app-test";
		String dummyFilename = "app-test.crt.pem";
		String expectedFilename = dummyFilename + ".reqrenew";
		Path expectedPath = tempStoragePath.resolve(SharedAppService.SHARED_APPS_FOLDER).resolve(appName).resolve(expectedFilename);
		
		given(filesService.isRegularFile(expectedPath)).willReturn(false);
		given(appConfig.getStoragePath()).willReturn(tempStoragePath);
		given(supportUtils.normalizeFileName(expectedFilename)).willReturn(expectedFilename);
		given(supportUtils.normalizeFileName(appName)).willReturn(appName);
		
		sut.requestRenewalForCert(appName, dummyFilename);
		
		verify(filesService, times(1)).isRegularFile(expectedPath);
		verify(filesService, never()).setLastModifiedTime(eq(expectedPath), any(FileTime.class));
		verify(filesService, times(1)).createFile(expectedPath);
		verify(appConfig, times(1)).getStoragePath();
		verify(supportUtils, times(2)).normalizeFileName(anyString());
	}
	
	@Test
	public void testFindSharedApps() {
		given(appConfig.getStoragePath()).willReturn(tempStoragePath);
		
		sut.setFilesService(realFilesService);
		sut.setSupportUtils(new SupportUtils());
		
		List<SharedApp> result = sut.findSharedApps();
		assertNotNull(result);
		assertEquals(4, result.size());
		assertSharedAppResult(result);
		
		verify(appConfig, times(1)).getStoragePath();
	}
	
	private void assertSharedAppResult(final List<SharedApp> result) {
		assertNotNull(result.get(0));
		assertNotNull(result.get(1));
		assertNotNull(result.get(2));
		assertNotNull(result.get(3));
		
		SharedApp nocert = result.get(0);
		SharedApp onlykey = result.get(1);
		SharedApp withcert = result.get(2);
		SharedApp withcertreneq = result.get(3);
		
		assertSharedAppNoCert(nocert);
		assertSharedAppOnlyKey(onlykey);
		assertSharedAppWithCert(withcert);
		assertSharedAppWithCertRenewRequest(withcertreneq);
	}
	
	private void assertSharedAppNoCert(final SharedApp nocert) {
		assertEquals("app-nocert", nocert.getName());
		assertNull(nocert.getCertFile());
		assertNotNull(nocert.getCsrFile());
		assertNotNull(nocert.getKeyFile());
		assertFalse(nocert.isRenewalRequested());
		assertNotNull(nocert.getKeyLastModified());
		assertNotNull(nocert.getCsrLastModified());
		assertNull(nocert.getCertLastModified());
		assertNull(nocert.getCertValidTo());
	}
	
	private void assertSharedAppOnlyKey(final SharedApp onlykey) {
		assertEquals("app-onlykey", onlykey.getName());
		assertNull(onlykey.getCertFile());
		assertNull(onlykey.getCsrFile());
		assertNotNull(onlykey.getKeyFile());
		assertFalse(onlykey.isRenewalRequested());
		assertNotNull(onlykey.getKeyLastModified());
		assertNull(onlykey.getCsrLastModified());
		assertNull(onlykey.getCertLastModified());
		assertNull(onlykey.getCertValidTo());
	}
	
	private void assertSharedAppWithCert(final SharedApp withcert) {
		assertEquals("app-withcert", withcert.getName());
		assertNotNull(withcert.getCertFile());
		assertNotNull(withcert.getCsrFile());
		assertNotNull(withcert.getKeyFile());
		assertFalse(withcert.isRenewalRequested());
		assertNotNull(withcert.getKeyLastModified());
		assertNotNull(withcert.getCsrLastModified());
		assertNotNull(withcert.getCertLastModified());
		assertNotNull(withcert.getCertValidTo());
	}
	
	private void assertSharedAppWithCertRenewRequest(final SharedApp withcertreneq) {
		assertEquals("app-withcert-reneq", withcertreneq.getName());
		assertNotNull(withcertreneq.getCertFile());
		assertNotNull(withcertreneq.getCsrFile());
		assertNotNull(withcertreneq.getKeyFile());
		assertTrue(withcertreneq.isRenewalRequested());
		assertNotNull(withcertreneq.getKeyLastModified());
		assertNotNull(withcertreneq.getCsrLastModified());
		assertNotNull(withcertreneq.getCertLastModified());
		assertNotNull(withcertreneq.getCertValidTo());
	}
}
