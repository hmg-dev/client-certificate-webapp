package wtf.hmg.pki.csc.model;

import org.junit.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.Temporal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class CertInfoTest {
	
	@Test
	public void testBuildModel() {
		String userName = "Brain";
		Path certFile = mock(Path.class);
		Temporal lastModified = Instant.now();
		
		CertInfo.Builder b = new CertInfo.Builder();
		b.userName(userName)
				.certFile(certFile)
				.lastModified(lastModified);
		
		CertInfo result = b.build();
		assertNotNull(result);
		assertEquals(userName, result.getUserName());
		assertEquals(certFile, result.getCertFile());
		assertEquals(lastModified, result.getLastModified());
	}
	
}
