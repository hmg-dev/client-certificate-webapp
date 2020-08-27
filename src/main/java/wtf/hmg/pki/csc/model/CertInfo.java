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

public class CertInfo {
	
	private final String userName;
	private final Path certFile;
	private final Temporal lastModified;
	
	private CertInfo(final Builder b) {
		userName = b.userName;
		certFile = b.certFile;
		lastModified = b.lastModified;
	}
	
	public String getUserName() {
		return userName;
	}
	
	public Path getCertFile() {
		return certFile;
	}
	
	public Temporal getLastModified() {
		return lastModified;
	}
	
	public static class Builder {
		private String userName;
		private Path certFile;
		private Temporal lastModified;
		private String csrInfo;
		
		public CertInfo build() {
			return new CertInfo(this);
		}
		
		public Builder userName(final String userName) {
			this.userName = userName;
			return this;
		}
		
		public Builder certFile(final Path certFile) {
			this.certFile = certFile;
			return this;
		}

		public Builder lastModified(final Temporal lastModified) {
			this.lastModified = lastModified;
			return this;
		}
	}
}
