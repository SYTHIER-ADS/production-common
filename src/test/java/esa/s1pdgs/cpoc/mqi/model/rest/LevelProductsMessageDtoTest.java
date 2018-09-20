package esa.s1pdgs.cpoc.mqi.model.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.mqi.model.queue.LevelProductDto;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Test the LevelProductsMessageDto
 * 
 * @author Viveris Technologies
 */
public class LevelProductsMessageDtoTest {

    /**
     * Test getters, setters and constructors
     */
    @Test
    public void testGettersSettersConstructors() {
        LevelProductDto body = new LevelProductDto("product-name", "key-obs",
                ProductFamily.L0_SLICE, "NRT");
        LevelProductsMessageDto dto =
                new LevelProductsMessageDto(123, "input-key", body);
        assertEquals(123, dto.getIdentifier());
        assertEquals(body, dto.getBody());
        assertEquals("input-key", dto.getInputKey());

        dto = new LevelProductsMessageDto();
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
        LevelProductDto body = new LevelProductDto("product-name", "key-obs",
                ProductFamily.L0_SLICE, "NRT");
        LevelProductsMessageDto dto =
                new LevelProductsMessageDto(123, "input-key", body);
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
        EqualsVerifier.forClass(LevelProductsMessageDto.class).usingGetClass()
                .suppress(Warning.NONFINAL_FIELDS).verify();
    }

}
