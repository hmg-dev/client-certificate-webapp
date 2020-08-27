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

import java.nio.file.Path;
import java.time.temporal.Temporal;

public class CSR {

    private final String userName;
    private final Path csrFile;
    private final Temporal lastModified;
    private final Temporal lastRenewed;
    private final String csrInfo;

    private CSR(final Builder b) {
        userName = b.userName;
        csrFile = b.csrFile;
        lastModified = b.lastModified;
        lastRenewed = b.lastRenewed;
        csrInfo = b.csrInfo;
    }

    public String getUserName() {
        return userName;
    }

    public Path getCsrFile() {
        return csrFile;
    }

    public String getCsrInfo() {
        return csrInfo;
    }

    public Temporal getLastModified() {
        return lastModified;
    }
    
    public Temporal getLastRenewed() {
        return lastRenewed;
    }
    
    public static class Builder {
        private String userName;
        private Path csrFile;
        private Temporal lastModified;
        private Temporal lastRenewed;
        private String csrInfo;

        public CSR build() {
            return new CSR(this);
        }

        public Builder userName(final String userName) {
            this.userName = userName;
            return this;
        }

        public Builder csrFile(final Path csrFile) {
            this.csrFile = csrFile;
            return this;
        }

        public Builder csrInfo(final String csrInfo) {
            this.csrInfo = csrInfo;
            return this;
        }

        public Builder lastModified(final Temporal lastModified) {
            this.lastModified = lastModified;
            return this;
        }
    
        public Builder lastRenewed(final Temporal lastRenewed) {
            this.lastRenewed = lastRenewed;
            return this;
        }
    }
}
