package fr.viveris.s1pdgs.mdcatalog.model;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class L0OutputFileDescriptorTest {

	@Test
    public void equalsConfigFileDecriptor() {
		EqualsVerifier.forClass(L0OutputFileDescriptor.class).usingGetClass().suppress(Warning.NONFINAL_FIELDS).verify();
    }

}
