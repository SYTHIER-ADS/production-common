package esa.s1pdgs.cpoc.mqi.model.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import esa.s1pdgs.cpoc.mqi.model.queue.AuxiliaryFileDto;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Test the AuxiliaryFilesMessageDto
 * 
 * @author Viveris Technologies
 */
public class AuxiliaryFilesMessageDtoTest {

    /**
     * Test getters, setters and constructors
     */
    @Test
    public void testGettersSettersConstructors() {
        AuxiliaryFileDto body = new AuxiliaryFileDto("product-name", "key-obs");
        AuxiliaryFilesMessageDto dto =
                new AuxiliaryFilesMessageDto(123, "input-key", body);
        assertEquals(123, dto.getIdentifier());
        assertEquals(body, dto.getBody());
        assertEquals("input-key", dto.getInputKey());

        dto = new AuxiliaryFilesMessageDto();
        dto.setIdentifier(321);
        dto.setBody(body);
        dto.setInputKey("othey-input");
        assertEquals(321, dto.getIdentifier());
        assertEquals(body, dto.getBody());
        assertEquals("othey-input", dto.getInputKey());
    }

    /**
     * Test the toString function
     */
    @Test
    public void testToString() {
        AuxiliaryFileDto body = new AuxiliaryFileDto("product-name", "key-obs");
        AuxiliaryFilesMessageDto dto =
                new AuxiliaryFilesMessageDto(123, "input-key", body);
        String str = dto.toString();
        assertTrue("toString should contain the identifier",
                str.contains("identifier: 123"));
        assertTrue("toString should contain the body",
                str.contains("body: " + body.toString()));
        assertTrue("toString should contain the input key",
                str.contains("inputKey: input-key"));
    }

    /**
     * Check equals and hashcode methods
     */
    @Test
    public void checkEquals() {
        EqualsVerifier.forClass(AuxiliaryFilesMessageDto.class).usingGetClass()
                .suppress(Warning.NONFINAL_FIELDS).verify();
    }

}
