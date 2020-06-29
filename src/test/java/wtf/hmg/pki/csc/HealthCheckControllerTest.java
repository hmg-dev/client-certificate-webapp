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
package wtf.hmg.pki.csc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import wtf.hmg.pki.csc.config.AppConfig;
import wtf.hmg.pki.csc.service.FilesService;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HealthCheckControllerTest {

    private HealthCheckController sut;

    @Mock
    private FilesService filesService;
    @Mock
    private AppConfig appConfig;

    private Path storagePath = Paths.get("/data/projects/csc");

    @Before
    public void setUp() {
        sut = new HealthCheckController();
        sut.setAppConfig(appConfig);
        sut.setFilesService(filesService);

        given(appConfig.getStoragePath()).willReturn(storagePath);
    }

    @Test
    public void testCheck() {
        Path expectedPath = storagePath.resolve("users");

        given(filesService.exists(expectedPath)).willReturn(true);

        String result = sut.check();
        assertNotNull(result);
        assertEquals("check_ok", result);

        verify(appConfig, times(1)).getStoragePath();
        verify(filesService, times(1)).exists(expectedPath);
    }

    @Test
    public void testCheckNotOk() {
        Path expectedPath = storagePath.resolve("users");

        given(filesService.exists(expectedPath)).willReturn(false);

        String result = sut.check();
        assertNotNull(result);
        assertEquals("check_notok", result);

        verify(appConfig, times(1)).getStoragePath();
        verify(filesService, times(1)).exists(expectedPath);
    }

}
