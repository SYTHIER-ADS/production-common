package fr.viveris.s1pdgs.level0.wrapper.services.kafka;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import fr.viveris.s1pdgs.level0.wrapper.controller.dto.L1AcnDto;
import fr.viveris.s1pdgs.level0.wrapper.model.exception.KafkaSendException;

/**
 * 
 * @author Olivier Bex-Chauvet
 *
 */
@Service
public class L1ACNsProducer {

	/**
	 * Logger
	 */
	private static final Logger LOGGER = LogManager.getLogger(L1ACNsProducer.class);

	/**
	 * KAFKA template for topic "session"
	 */
	@Autowired
	private KafkaTemplate<String, L1AcnDto> kafkaAcnTemplate;

	/**
	 * Name of the topic "session"
	 */
	@Value("${kafka.topic.l1-acns}")
	private String kafkaTopic;

	/**
	 * Send a message to a topic and wait until one is published
	 * 
	 * @param descriptor
	 */
	public void send(L1AcnDto descriptor) throws KafkaSendException {
		try {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("[send] Send L1_ACN = {}", descriptor);
			}
			kafkaAcnTemplate.send(kafkaTopic, descriptor).get();
		} catch (CancellationException | InterruptedException | ExecutionException e) {

			throw new KafkaSendException(kafkaTopic, descriptor.getProductName(), e.getMessage(), e);
		}
	}
}
