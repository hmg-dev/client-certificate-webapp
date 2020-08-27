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
