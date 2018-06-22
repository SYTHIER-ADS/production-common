package fr.viveris.s1pdgs.archives.controller.dto;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.viveris.s1pdgs.archives.model.ProductFamily;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class ReportDtoTest {

	/**
	 * Test toString
	 */
	//String productName, String productType, String keyObjectStorage, String validityStart,String validityStop
	@Test
	public void testToString() {
		ReportDto obj = new ReportDto("name", "content", ProductFamily.BLANK);
		
		String str = obj.toString();
		assertTrue(str.contains("productName=name"));
		assertTrue(str.contains("content=content"));
		assertTrue(str.contains("familyName=BLANK"));
	}
	
	/**
	 * Check equals and hascode methods
	 */
	@Test
	public void testEquals() {
		EqualsVerifier.forClass(ReportDto.class).usingGetClass().suppress(Warning.NONFINAL_FIELDS).verify();
	}

}
