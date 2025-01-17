package esa.s1pdgs.cpoc.ipf.execution.worker.config;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Check the application properties
 * 
 * @author Viveris Technologies
 */
@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
public class DevPropertiesTest {

    /**
     * Properties to test
     */
    @Autowired
    private DevProperties properties;

    /**
     * Test the properties
     */
    @Test
    public void testLoadProperties() {
        assertEquals(3, properties.getStepsActivation().size());
        assertEquals(true, properties.getStepsActivation().get("download"));
        assertEquals(true, properties.getStepsActivation().get("upload"));
        assertEquals(true, properties.getStepsActivation().get("erasing"));
    }

}
