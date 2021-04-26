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

public class SharedApp {
	
	private final String name;
	private final Temporal certLastModified;
	private final Temporal keyLastModified;
	private final Temporal csrLastModified;
	private final Path certFile;
	private final Path keyFile;
	private final Path csrFile;
	private final boolean renewalRequested;
	private final String csrInfo;
	
	private SharedApp(final Builder b) {
		name = b.name;
		certLastModified = b.certLastModified;
		keyLastModified = b.keyLastModified;
		csrLastModified = b.csrLastModified;
		certFile = b.certFile;
		keyFile = b.keyFile;
		csrFile = b.csrFile;
		renewalRequested = b.renewalRequested;
		csrInfo = b.csrInfo;
	}
	
	public String getName() {
		return name;
	}
	
	public Temporal getCertLastModified() {
		return certLastModified;
	}
	
	public Temporal getKeyLastModified() {
		return keyLastModified;
	}
	
	public Temporal getCsrLastModified() {
		return csrLastModified;
	}
	
	public Path getCertFile() {
		return certFile;
	}
	
	public Path getKeyFile() {
		return keyFile;
	}
	
	public Path getCsrFile() {
		return csrFile;
	}
	
	public boolean isRenewalRequested() {
		return renewalRequested;
	}
	
	public String getCsrInfo() {
		return csrInfo;
	}
	
	public static class Builder {
		private String name;
		private Temporal certLastModified;
		private Temporal keyLastModified;
		private Temporal csrLastModified;
		private Path certFile;
		private Path keyFile;
		private Path csrFile;
		private boolean renewalRequested;
		private String csrInfo;
		
		public SharedApp build() {
			return new SharedApp(this);
		}
		
		public Builder setName(final String name) {
			this.name = name;
			return this;
		}
		
		public Builder setCertLastModified(final Temporal certLastModified) {
			this.certLastModified = certLastModified;
			return this;
		}
		
		public Builder setKeyLastModified(final Temporal keyLastModified) {
			this.keyLastModified = keyLastModified;
			return this;
		}
		
		public Builder setCsrLastModified(final Temporal csrLastModified) {
			this.csrLastModified = csrLastModified;
			return this;
		}
		
		public Builder setCertFile(final Path certFile) {
			this.certFile = certFile;
			return this;
		}
		
		public Builder setKeyFile(final Path keyFile) {
			this.keyFile = keyFile;
			return this;
		}
		
		public Builder setCsrFile(final Path csrFile) {
			this.csrFile = csrFile;
			return this;
		}
		
		public Builder setRenewalRequested(final boolean renewalRequested) {
			this.renewalRequested = renewalRequested;
			return this;
		}
		
		public Builder setCsrInfo(final String csrInfo) {
			this.csrInfo = csrInfo;
			return this;
		}
	}
	
}
