package esa.s1pdgs.cpoc.ipf.preparation.worker.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import esa.s1pdgs.cpoc.appcatalog.client.job.AppCatalogJobClient;
import esa.s1pdgs.cpoc.mqi.model.queue.CatalogEvent;

/**
 * General application configuration
 * @author Cyrielle Gailliard
 *
 */
@Configuration
public class AppConfig {	
	private final AppCatalogConfigurationProperties properties;
	private final RestTemplate restTemplate;
		
	@Autowired
	public AppConfig(
			final AppCatalogConfigurationProperties properties,
			final RestTemplateBuilder restTemplateBuilder
	) {
		this.properties = properties;
		this.restTemplate = restTemplateBuilder
				.setConnectTimeout(properties.getTmConnectMs())
				.build();
	}
	
	@Bean
	public AppCatalogJobClient<CatalogEvent> appCatClient() {
		return new AppCatalogJobClient<>(
				restTemplate, 
				properties.getHostUri(), 
				properties.getMaxRetries(), 
				properties.getTempoRetryMs()
		);
	}


}