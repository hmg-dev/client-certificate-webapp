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
package wtf.hmg.pki.csc.model;

import org.junit.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.Temporal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class CSRTest {

    @Test
    public void testBuildModel() {
        String userName = "Pinky";
        Path csrFile = mock(Path.class);
        Temporal lastModified = Instant.now();
        String csrInfo = "CN=CSR Info";

        CSR.Builder b = new CSR.Builder();
        b.userName(userName)
            .csrFile(csrFile)
            .lastModified(lastModified)
            .csrInfo(csrInfo);

        CSR result = b.build();
        assertNotNull(result);
        assertEquals(userName, result.getUserName());
        assertEquals(csrFile, result.getCsrFile());
        assertEquals(lastModified, result.getLastModified());
        assertEquals(csrInfo, result.getCsrInfo());
    }
}
