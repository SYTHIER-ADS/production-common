package fr.viveris.s1pdgs.level0.wrapper.services.kafka;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import fr.viveris.s1pdgs.level0.wrapper.controller.dto.L0AcnDto;

/**
 * 
 * @author Olivier Bex-Chauvet
 *
 */
@Service
public class L0ACNsProducer {

	/**
	 * Logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(L0ACNsProducer.class);

	/**
	 * KAFKA template for topic "session"
	 */
	@Autowired
	private KafkaTemplate<String, L0AcnDto> kafkaAcnTemplate;

	/**
	 * Name of the topic "session"
	 */
	@Value("${kafka.topic.l0-acns}")
	private String kafkaTopic;

	/**
	 * Send a message to a topic and wait until one is published
	 * 
	 * @param descriptor
	 */
	public void send(L0AcnDto descriptor) throws KafkaException {
		try {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("[send] Send L0_ACN = {}", descriptor);
			}
			kafkaAcnTemplate.send(kafkaTopic, descriptor).get();
		} catch (CancellationException | InterruptedException | ExecutionException e) {
			throw new KafkaException(
					String.format("Error during sending %S in queue system", descriptor.getProductName()), e);
		}
	}
}
