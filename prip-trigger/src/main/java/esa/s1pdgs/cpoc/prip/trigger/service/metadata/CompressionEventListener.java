package esa.s1pdgs.cpoc.prip.trigger.service.metadata;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.mqi.client.GenericMqiClient;
import esa.s1pdgs.cpoc.mqi.client.MqiConsumer;
import esa.s1pdgs.cpoc.mqi.client.MqiListener;
import esa.s1pdgs.cpoc.mqi.model.queue.CompressionEvent;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;

@Service
public class CompressionEventListener implements MqiListener<CompressionEvent> {
	
	private static final Logger LOGGER = LogManager.getLogger(CompressionEventListener.class);
	
	private final GenericMqiClient mqiClient;
	
	private final long pollingIntervalMs;

	private final long pollingInitialDelayMs;

	@Autowired
	public CompressionEventListener(final GenericMqiClient mqiClient,
			@Value("${prip-trigger.compression-event-listener.polling-interval-ms}") final long pollingIntervalMs,
			@Value("${prip-trigger.compression-event-listener.polling-initial-delay-ms}") final long pollingInitialDelayMs) {
		this.mqiClient = mqiClient;
		this.pollingIntervalMs = pollingIntervalMs;
		this.pollingInitialDelayMs = pollingInitialDelayMs;
	}
	
	@PostConstruct
	public void initService() {
		if (pollingIntervalMs > 0) {
			final ExecutorService service = Executors.newFixedThreadPool(1);
			service.execute(new MqiConsumer<CompressionEvent>(mqiClient, ProductCategory.COMPRESSED_PRODUCTS, this,
					pollingIntervalMs, pollingInitialDelayMs, esa.s1pdgs.cpoc.appstatus.AppStatus.NULL));
		}
	}
	
	@Override
	public void onMessage(GenericMessageDto<CompressionEvent> message) {
		
	}
}
