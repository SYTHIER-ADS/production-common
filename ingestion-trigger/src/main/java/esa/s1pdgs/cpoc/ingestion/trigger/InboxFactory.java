package esa.s1pdgs.cpoc.ingestion.trigger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import esa.s1pdgs.cpoc.ingestion.trigger.config.InboxConfiguration;
import esa.s1pdgs.cpoc.ingestion.trigger.config.ProcessConfiguration;
import esa.s1pdgs.cpoc.ingestion.trigger.filter.BlacklistRegexRelativePathInboxFilter;
import esa.s1pdgs.cpoc.ingestion.trigger.filter.InboxFilter;
import esa.s1pdgs.cpoc.ingestion.trigger.filter.WhitelistRegexRelativePathInboxFilter;
import esa.s1pdgs.cpoc.ingestion.trigger.kafka.producer.KafkaSubmissionClient;
import esa.s1pdgs.cpoc.mqi.model.queue.IngestionJob;

@Component
public class InboxFactory {

	private static final Logger LOG = LoggerFactory.getLogger(InboxFactory.class);

	private final KafkaTemplate<String, IngestionJob> kafkaTemplate;
	private final InboxAdapterFactory inboxAdapterFactory;
	private final IngestionTriggerServiceTransactional inboxPollingServiceTransactional;
	private final String hostname;

	@Autowired
	public InboxFactory(final KafkaTemplate<String, IngestionJob> kafkaTemplate,
			final IngestionTriggerServiceTransactional inboxPollingServiceTransactional,
			final InboxAdapterFactory inboxAdapterFactory,
			final ProcessConfiguration processConfiguration) {
		this.kafkaTemplate = kafkaTemplate;
		this.inboxPollingServiceTransactional = inboxPollingServiceTransactional;
		this.inboxAdapterFactory = inboxAdapterFactory;
		this.hostname = processConfiguration.getHostname();
	}

	public Inbox newInbox(InboxConfiguration config) throws IOException {

		createDirectory(config);

		List<InboxFilter> filter = new ArrayList<>();
		filter.add(new BlacklistRegexRelativePathInboxFilter(Pattern.compile(config.getIgnoreRegex())));
		filter.add(new WhitelistRegexRelativePathInboxFilter(Pattern.compile(config.getMatchRegex())));
		return new Inbox(inboxAdapterFactory.newInboxAdapter(config.getDirectory()), filter,
				inboxPollingServiceTransactional, new KafkaSubmissionClient(kafkaTemplate, config.getTopic() + "-" + hostname), hostname
		);
	}

	private void createDirectory(InboxConfiguration config) throws IOException {

		Path inboxPath = Paths.get(config.getDirectory());
		try {
			Files.createDirectories(inboxPath);
		} catch (IOException e) {
			LOG.error(String.format("could not create inbox directory %s", inboxPath), e.getMessage());
			throw (e);
		}
	}

}
