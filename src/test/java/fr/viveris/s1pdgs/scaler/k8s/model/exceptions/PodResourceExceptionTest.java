package fr.viveris.s1pdgs.scaler.k8s.model.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import fr.viveris.s1pdgs.scaler.AbstractCodedException.ErrorCode;

public class PodResourceExceptionTest {

	@Test
	public void testPodResourceException() {
		PodResourceException e1 = new PodResourceException("message");
		assertEquals(ErrorCode.K8S_NO_TEMPLATE_POD, e1.getCode());
		assertEquals("message", e1.getMessage());
		assertNull(e1.getCause());
		
		PodResourceException e2 = new PodResourceException("message", new Throwable("throw"));

		assertEquals(ErrorCode.K8S_NO_TEMPLATE_POD, e2.getCode());
		assertEquals("message", e2.getMessage());
		assertEquals("throw", e2.getCause().getMessage());
	}

}
