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

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import wtf.hmg.pki.csc.config.AppConfig;
import wtf.hmg.pki.csc.service.CryptService;
import wtf.hmg.pki.csc.service.FilesService;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCertificateServiceTest {
    private DefaultCertificateService sut;

    private AppConfig appConfig;

    @Mock
    private FilesService filesService;
    @Mock
    private CloneCommand cloneCommand;
    @Mock
    private TransportConfigCallback transportConfigCallback;
    @Mock
    private CryptService cryptService;
    @Mock
    private Git gitRepo;

    private Path dummyStoragePath = Paths.get("/data/projects/csc");
    private String repoUrl = "ssh://git@git.company.repo:6666/prj/client-side-certs.git";

    @Before
    public void setUp() {
        appConfig = new AppConfig();
        appConfig.setStoragePath(dummyStoragePath);
        appConfig.setCertRepo(repoUrl);

        sut = new DefaultCertificateService() {
            protected CloneCommand cloneCommand() {
                return cloneCommand;
            }
            protected Git openRepository(final Path repo) throws IOException {
                return gitRepo;
            }
        };
        sut.setAppConfig(appConfig);
        sut.setTransportConfigCallback(transportConfigCallback);
        sut.setFilesService(filesService);
        sut.setCryptService(cryptService);
    }

    @Test
    public void testCloneCertificateRepository() throws GitAPIException {
        Path expectedPath = dummyStoragePath.resolve("cert-repo");

        given(cloneCommand.setURI(anyString())).willReturn(cloneCommand);
        given(cloneCommand.setDirectory(any(File.class))).willReturn(cloneCommand);
        given(cloneCommand.setTransportConfigCallback(any(TransportConfigCallback.class))).willReturn(cloneCommand);

        Path repoPath = sut.cloneCertificateRepository();
        assertNotNull(repoPath);
        assertEquals(expectedPath, repoPath);

        verify(cloneCommand, times(1)).setURI(repoUrl);
        verify(cloneCommand, times(1)).setDirectory(expectedPath.toFile());
        verify(cloneCommand, times(1)).setTransportConfigCallback(transportConfigCallback);
        verify(cloneCommand, times(1)).call();
    }

    @Test
    public void testCleanupWorkingFilesForNothingToCleanup() throws IOException {
        Path expectedPath = dummyStoragePath.resolve("cert-repo");
        given(filesService.exists(expectedPath)).willReturn(false);

        sut.cleanupWorkingFiles();

        verify(filesService, times(1)).exists(expectedPath);
        verify(filesService, never()).deleteRecursively(any(Path.class));
    }

    @Test
    public void testCleanupWorkingFilesForExistingFiles() throws IOException {
        Path expectedPath = dummyStoragePath.resolve("cert-repo");
        given(filesService.exists(expectedPath)).willReturn(true);

        sut.cleanupWorkingFiles();

        verify(filesService, times(1)).exists(expectedPath);
        verify(filesService, times(1)).deleteRecursively(expectedPath);
    }

    @Test
    public void testDecryptWorkingFiles() throws IOException {
        String dummyPassword = "NARF";
        Path expectedPath = dummyStoragePath.resolve("cert-repo");
        int expectedDepth = 4;
        List<Path> dummyFiles = Arrays.asList(expectedPath.resolve("ca.cert.pem"),
                expectedPath.resolve("intermediate/cert/intermediate.crt.pem"));

        given(filesService.find(eq(expectedPath), eq(expectedDepth), any())).willReturn(dummyFiles.stream());

        sut.decryptWorkingFiles(dummyPassword);

        verify(filesService, times(1)).find(eq(expectedPath), eq(expectedDepth), any());
        verify(cryptService, times(dummyFiles.size())).decryptFile(any(Path.class), eq(dummyPassword));
    }

    @Test
    public void testEncryptWorkingFiles() throws IOException {
        String dummyPassword = "NARF";
        Path expectedPath = dummyStoragePath.resolve("cert-repo");
        int expectedDepth = 4;
        List<Path> dummyFiles = Arrays.asList(expectedPath.resolve("ca.cert.pem"),
                expectedPath.resolve("intermediate/cert/intermediate.crt.pem"));

        given(filesService.find(eq(expectedPath), eq(expectedDepth), any())).willReturn(dummyFiles.stream());

        sut.encryptWorkingFiles(dummyPassword);

        verify(filesService, times(1)).find(eq(expectedPath), eq(expectedDepth), any());
        verify(cryptService, times(dummyFiles.size())).encryptFile(any(Path.class), eq(dummyPassword));
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void testCopyCSRToRepoForExistingFile() throws IOException {
        String userName = "user1";
        String csrFileName = "user1.csr.pem";
        Path expectedSource = dummyStoragePath.resolve("users").resolve(userName).resolve(csrFileName);
        Path expectedPath = dummyStoragePath.resolve("cert-repo/intermediate/csr").resolve(csrFileName);

        doThrow(new FileAlreadyExistsException("TEST")).when(filesService).copy(expectedSource, expectedPath, StandardCopyOption.REPLACE_EXISTING);
        sut.copyUserCSRToRepository(userName, csrFileName);
    }

    @Test
    public void testCopyCSRToRepo() throws IOException {
        String userName = "user1";
        String csrFileName = "user1.csr.pem";
        Path expectedSource = dummyStoragePath.resolve("users").resolve(userName).resolve(csrFileName);
        Path expectedPath = dummyStoragePath.resolve("cert-repo/intermediate/csr").resolve(csrFileName);

        Path csrInRepo = sut.copyUserCSRToRepository(userName, csrFileName);
        assertNotNull(csrInRepo);
        assertEquals(expectedPath, csrInRepo);

        verify(filesService, times(1)).copy(expectedSource, expectedPath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    public void testCopyCertificateToUserDirectory() throws IOException {
        String userName = "user1";
        Path certFile = dummyStoragePath.resolve("cert-repo/intermediate/certs/user1.crt.pem");
        Path expectedTarget = dummyStoragePath.resolve("users").resolve(userName).resolve("certs").resolve(certFile.getFileName().toString());

        sut.copyCertificateToUserDirectory(userName, certFile);

        verify(filesService, times(1)).createDirectories(expectedTarget.getParent());
        verify(filesService, times(1)).copy(certFile, expectedTarget, StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    public void testCommitAndPushChanges() throws IOException, GitAPIException {
        String adminUser = "Pinky";
        AddCommand addCommand = mock(AddCommand.class);
        CommitCommand commitCommand = mock(CommitCommand.class);
        PushCommand pushCommand = mock(PushCommand.class);
        String operationMessage = "Signed User-Certificate";
        String expectedMessage = operationMessage + " - done by: " + adminUser;

        given(gitRepo.add()).willReturn(addCommand);
        given(gitRepo.commit()).willReturn(commitCommand);
        given(gitRepo.push()).willReturn(pushCommand);
        given(addCommand.addFilepattern(anyString())).willReturn(addCommand);
        given(commitCommand.setMessage(anyString())).willReturn(commitCommand);
        given(pushCommand.setTransportConfigCallback(transportConfigCallback)).willReturn(pushCommand);

        sut.commitAndPushChanges(adminUser, operationMessage);

        verify(gitRepo, times(1)).add();
        verify(addCommand, times(1)).addFilepattern(".");
        verify(addCommand, times(1)).call();

        verify(gitRepo, times(1)).commit();
        verify(commitCommand, times(1)).setMessage(expectedMessage);
        verify(commitCommand, times(1)).call();

        verify(gitRepo, times(1)).push();
        verify(pushCommand, times(1)).setTransportConfigCallback(transportConfigCallback);
        verify(pushCommand, times(1)).call();
    }
}
