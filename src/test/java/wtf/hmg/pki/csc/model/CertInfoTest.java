package wtf.hmg.pki.csc.model;

import org.junit.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.Temporal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class CertInfoTest {
	
	@Test
	public void testBuildModel() {
		String userName = "Brain";
		String certFileName = "user.crt.pem";
		Path certFile = mock(Path.class);
		Path certFileNamePath = mock(Path.class);
		Temporal lastModified = Instant.now();
		boolean renewalReq = true;
		boolean renewed = true;
		
		given(certFile.getFileName()).willReturn(certFileNamePath);
		given(certFileNamePath.toString()).willReturn(certFileName);
		
		CertInfo.Builder b = new CertInfo.Builder();
		b.userName(userName)
				.certFile(certFile)
				.lastModified(lastModified)
				.renewalRequested(renewalReq)
				.renewed(renewed);
		
		CertInfo result = b.build();
		assertNotNull(result);
		assertEquals(userName, result.getUserName());
		assertEquals(certFile, result.getCertFile());
		assertEquals(lastModified, result.getLastModified());
		assertEquals(renewalReq, result.isRenewalRequested());
		assertEquals(certFileName, result.getCertFileName());
		assertEquals(renewed, result.isRenewed());
	}
	
}
