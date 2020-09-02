package wtf.hmg.pki.csc.model;

import java.nio.file.Path;
import java.time.temporal.Temporal;

public class CertInfo {
	
	private final String userName;
	private final Path certFile;
	private final Temporal lastModified;
	private final boolean renewalRequested;
	private final boolean renewed;
	
	private CertInfo(final Builder b) {
		userName = b.userName;
		certFile = b.certFile;
		lastModified = b.lastModified;
		renewalRequested = b.renewalRequested;
		renewed = b.renewed;
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
	
	public boolean isRenewalRequested() {
		return renewalRequested;
	}
	
	public boolean isRenewed() {
		return renewed;
	}
	
	public String getCertFileName() {
		return certFile.getFileName().toString();
	}
	
	public static class Builder {
		private String userName;
		private Path certFile;
		private Temporal lastModified;
		private boolean renewalRequested;
		private boolean renewed;
		
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
		
		public Builder renewalRequested(final boolean renewalRequested) {
			this.renewalRequested = renewalRequested;
			return this;
		}
		
		public Builder renewed(final boolean renewed) {
			this.renewed = renewed;
			return this;
		}
	}
}
