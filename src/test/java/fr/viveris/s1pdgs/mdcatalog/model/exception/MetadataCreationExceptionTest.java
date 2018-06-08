package fr.viveris.s1pdgs.mdcatalog.model.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.viveris.s1pdgs.mdcatalog.model.exception.AbstractCodedException.ErrorCode;

/**
 * Test the exception MetadataCreationException
 */
public class MetadataCreationExceptionTest {

	/**
	 * Test getters and constructors
	 */
	@Test
	public void testGettersConstructors() {
		MetadataCreationException exception = new MetadataCreationException("product-name",
				"test-result", "test-status");
		
		assertEquals(ErrorCode.ES_CREATION_ERROR, exception.getCode());
		assertEquals("product-name", exception.getProductName());
		assertEquals("test-result", exception.getResult());
		assertEquals("test-status", exception.getStatus());
	}

	/**
	 * Test get log message
	 */
	@Test
	public void testLogMessage() {
		MetadataCreationException exception = new MetadataCreationException("product-name",
				"test-result", "test-status");
		
		String log = exception.getLogMessage();
		assertTrue(log.contains("[result test-result]"));
		assertTrue(log.contains("[status test-status]"));
		assertTrue(log.contains("[msg"));
	}

}
