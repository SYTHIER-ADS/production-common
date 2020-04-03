package esa.s1pdgs.cpoc.xbip.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.junit4.SpringRunner;

import esa.s1pdgs.cpoc.xbip.client.config.XbipClientConfiguration;
import esa.s1pdgs.cpoc.xbip.client.config.XbipClientConfigurationProperties;

@RunWith(SpringRunner.class)
@EnableConfigurationProperties(value = XbipClientConfigurationProperties.class)
public class ITXbipClient {
	
	@Autowired
	private XbipClientConfigurationProperties config;
	
	@Test
	public final void testConfigProperties() {
		assertNotNull(config);
		assertEquals(1, config.getHostConfigs().size());
		System.out.println(config);
	}
	
	@Test
	public final void testFoo() throws Exception {
		final XbipClientConfiguration c = new XbipClientConfiguration(config);		
		final XbipClientFactory factory = c.xbipClientFactory();		
		final XbipClient uut = factory.newXbipClient(new URI("https://cgs01.sentinel1.eo.esa.int/NOMINAL/"));
		
		uut.list(XbipEntryFilter.ALLOW_ALL).stream()
			.forEach(e -> System.out.println(e));
	}
}
