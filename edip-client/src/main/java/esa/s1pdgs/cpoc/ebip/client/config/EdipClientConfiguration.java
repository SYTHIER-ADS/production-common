package esa.s1pdgs.cpoc.ebip.client.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import esa.s1pdgs.cpoc.ebip.client.EdipClientFactory;
import esa.s1pdgs.cpoc.ebip.client.apacheftp.ApacheFtpEdipClientFactory;

@Configuration
public class EdipClientConfiguration {
	
	private final EdipClientConfigurationProperties config;
	
	@Autowired
	public EdipClientConfiguration(final EdipClientConfigurationProperties config) {
		this.config = config;
	}
	
	@Bean
	public EdipClientFactory factory() {
		return new ApacheFtpEdipClientFactory(config);
	}

}
