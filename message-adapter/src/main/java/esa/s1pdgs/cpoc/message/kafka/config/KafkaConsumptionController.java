package esa.s1pdgs.cpoc.message.kafka.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.RoundRobinAssignor;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;

import esa.s1pdgs.cpoc.message.Consumption;
import esa.s1pdgs.cpoc.message.MessageConsumer;
import esa.s1pdgs.cpoc.message.MessageConsumerFactory;
import esa.s1pdgs.cpoc.message.kafka.KafkaAcknowledgement;
import esa.s1pdgs.cpoc.message.kafka.KafkaConsumption;
import esa.s1pdgs.cpoc.message.kafka.KafkaMessage;
import esa.s1pdgs.cpoc.mqi.model.queue.AbstractMessage;

@Component
public class KafkaConsumptionController {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaConsumptionController.class);

    private final MessageConsumerFactory<AbstractMessage> consumerFactory;
    private final KafkaProperties kafkaProperties;
    private final Optional<ConsumerRebalanceListener> rebalanceListener;

    private final Map<String, ConcurrentMessageListenerContainer<String, AbstractMessage>> containers = new HashMap<>();

    @Autowired
    public KafkaConsumptionController(MessageConsumerFactory<AbstractMessage> consumerFactory, KafkaProperties kafkaProperties, Optional<ConsumerRebalanceListener> rebalanceListener) {
        this.consumerFactory = consumerFactory;
        this.kafkaProperties = kafkaProperties;
        this.rebalanceListener = rebalanceListener;
    }

    @PostConstruct
    public void initAndStartContainers() {
        List<MessageConsumer<AbstractMessage>> messageConsumers = consumerFactory.createConsumers();

        for (MessageConsumer<AbstractMessage> messageConsumer : messageConsumers) {
            containers.put(messageConsumer.topic(), containerFor(messageConsumer));
        }

        containers.forEach((topic, container) -> {
            LOG.info("Starting consumer on topic {}", topic);
            container.start();

        });
    }

    private ConcurrentMessageListenerContainer<String, AbstractMessage> containerFor(MessageConsumer<AbstractMessage> messageConsumer) {

        final String topic = messageConsumer.topic();

        KafkaConsumption consumption = new KafkaConsumption();
        final ConcurrentMessageListenerContainer<String, AbstractMessage> container = new ConcurrentMessageListenerContainer<>(
                consumerFactory(topic, messageConsumer.messageType()),
                containerProperties(topic, listenerFor(messageConsumer, consumption))
        );
        consumption.setKafkaContainer(container);

        return container;
    }

    private MessageListener<String, AbstractMessage> listenerFor(MessageConsumer<AbstractMessage> messageConsumer, Consumption consumption) {

        return (AcknowledgingMessageListener<String, AbstractMessage>) (data, acknowledgment)
                -> messageConsumer.onMessage(new KafkaMessage<>(data.value(), data), new KafkaAcknowledgement(acknowledgment), consumption);
    }

    private <T> ConsumerFactory<String, T> consumerFactory(final String topic, final Class<T> dtoClass) {
        final JsonDeserializer<T> jsonDeserializer = new JsonDeserializer<>(dtoClass);
        jsonDeserializer.addTrustedPackages("*");
        final ErrorHandlingDeserializer<T> errorHandlingDeserializer = new ErrorHandlingDeserializer<>(jsonDeserializer);

        errorHandlingDeserializer.setFailedDeserializationFunction((failedDeserializationInfo) -> {
            LOG.error(
                    "Error on deserializing element from queue '{}'. Expected json of class {} but was: {}",
                    topic,
                    dtoClass.getName(),
                    new String(failedDeserializationInfo.getData())
            );
            return null;
        });
        return new DefaultKafkaConsumerFactory<>(
                consumerConfig(clientIdForTopic(topic)),
                new StringDeserializer(),
                errorHandlingDeserializer
        );
    }

    private Map<String, Object> consumerConfig(final String consumerId) {
        final Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getConsumer().getGroupId());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, kafkaProperties.getConsumer().getMaxPollIntervalMs());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, kafkaProperties.getConsumer().getMaxPollRecords());
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, kafkaProperties.getConsumer().getSessionTimeoutMs());
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, kafkaProperties.getConsumer().getHeartbeatIntvMs());
        props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, Collections.singletonList(RoundRobinAssignor.class));
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, consumerId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaProperties.getConsumer().getAutoOffsetReset());
        return props;
    }

    // use unique clientId to circumvent 'instance already exists' problem
    private String clientIdForTopic(final String topic) {
        return kafkaProperties.getClientId() + "-" +
                kafkaProperties.getHostname() + "-" +
                topic;
    }

    private ContainerProperties containerProperties(final String topic, final MessageListener<String, AbstractMessage> messageListener) {
        final ContainerProperties containerProp = new ContainerProperties(topic);
        containerProp.setMessageListener(messageListener);
        containerProp.setPollTimeout(kafkaProperties.getListener().getPollTimeoutMs());
        containerProp.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        rebalanceListener.ifPresent(containerProp::setConsumerRebalanceListener);

        return containerProp;
    }

    @PreDestroy
    public void stopContainers() {
        containers.forEach((topic, container) -> {
            LOG.info("Stopping consumer on topic {}", topic);
            container.stop();
        });
    }

}
