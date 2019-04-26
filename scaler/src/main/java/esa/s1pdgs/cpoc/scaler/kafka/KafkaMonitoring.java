package esa.s1pdgs.cpoc.scaler.kafka;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import esa.s1pdgs.cpoc.scaler.kafka.model.ConsumerGroupsDescription;
import esa.s1pdgs.cpoc.scaler.kafka.model.KafkaPerGroupMonitor;
import esa.s1pdgs.cpoc.scaler.kafka.model.KafkaPerGroupPerTopicMonitor;
import esa.s1pdgs.cpoc.scaler.kafka.model.SpdgsTopic;
import esa.s1pdgs.cpoc.scaler.kafka.services.KafkaService;

/**
 * Service to monitor KAFKA
 * 
 * @author Cyrielle Gailliard
 *
 */
@Service
public class KafkaMonitoring {

	/**
	 * Kafka properties
	 */
	private final KafkaMonitoringProperties kafkaProperties;

	/**
	 * KAFKA service
	 */
	private final KafkaService kafkaService;

	/**
	 * Constructor
	 * 
	 * @param kafkaService
	 */
	@Autowired
	public KafkaMonitoring(final KafkaMonitoringProperties kafkaProperties, final KafkaService kafkaService) {
		this.kafkaProperties = kafkaProperties;
		this.kafkaService = kafkaService;
	}

	public KafkaPerGroupMonitor monitorL1Jobs() {
        String groupId = kafkaProperties.getGroupIdPerTopic().get(SpdgsTopic.L1_JOBS);
	    KafkaPerGroupMonitor globalMonitor = new KafkaPerGroupMonitor(new Date(), groupId);
		for (String topicName : kafkaProperties.getTopics().get(SpdgsTopic.L1_JOBS)) {
		    globalMonitor.addMonitor(this.monitorGroupPerTopic(groupId, topicName));
		}
		return globalMonitor;
	}

	private KafkaPerGroupPerTopicMonitor monitorGroupPerTopic(String groupId, String topicName) {
		ConsumerGroupsDescription desc = this.kafkaService.describeConsumerGroup(groupId, topicName);
		KafkaPerGroupPerTopicMonitor monitor = new KafkaPerGroupPerTopicMonitor(new Date(), groupId, topicName);
		if (!CollectionUtils.isEmpty(desc.getDescPerPartition())) {
			monitor.setNbPartitions(desc.getDescPerPartition().size());
			desc.getDescPerPartition().forEach((k, v) -> {
				monitor.getLagPerPartition().put(Integer.valueOf(v.getId()), Long.valueOf(v.getLag()));
			});
		}
		if (!CollectionUtils.isEmpty(desc.getDescPerConsumer())) {
			monitor.setNbConsumers(desc.getDescPerConsumer().size());
			desc.getDescPerConsumer().forEach((k, v) -> {
				monitor.getLagPerConsumers().put(v.getConsumerId(), Long.valueOf(v.getTotalLag()));
			});
		}
		return monitor;
	}
}
