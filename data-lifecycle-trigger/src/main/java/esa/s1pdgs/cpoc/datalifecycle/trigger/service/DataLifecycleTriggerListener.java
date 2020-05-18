package esa.s1pdgs.cpoc.datalifecycle.trigger.service;

import java.time.Period;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.utils.LogUtils;
import esa.s1pdgs.cpoc.datalifecycle.trigger.config.DataLifecycleTriggerConfigurationProperties.RetentionPolicy;
import esa.s1pdgs.cpoc.datalifecycle.trigger.config.ProcessConfiguration;
import esa.s1pdgs.cpoc.errorrepo.ErrorRepoAppender;
import esa.s1pdgs.cpoc.errorrepo.model.rest.FailedProcessingDto;
import esa.s1pdgs.cpoc.mqi.client.MqiClient;
import esa.s1pdgs.cpoc.mqi.client.MqiListener;
import esa.s1pdgs.cpoc.mqi.model.queue.AbstractMessage;
import esa.s1pdgs.cpoc.mqi.model.queue.EvictionManagementJob;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericPublicationMessageDto;
import esa.s1pdgs.cpoc.report.Reporting;
import esa.s1pdgs.cpoc.report.ReportingMessage;
import esa.s1pdgs.cpoc.report.ReportingUtils;

public class DataLifecycleTriggerListener<E extends AbstractMessage> implements MqiListener<E> {

	private static final Logger LOG = LogManager.getLogger(DataLifecycleTriggerListener.class);

	private final MqiClient mqiClient;
	private final ErrorRepoAppender errorRepoAppender;
	private final ProcessConfiguration processConfig;
	private final List<RetentionPolicy> retentionPolicies;

	public DataLifecycleTriggerListener(final MqiClient mqiClient, final ErrorRepoAppender errorRepoAppender,
			final ProcessConfiguration processConfig, final List<RetentionPolicy> retentionPolicies) {

		this.mqiClient = mqiClient;
		this.errorRepoAppender = errorRepoAppender;
		this.processConfig = processConfig;
		this.retentionPolicies = retentionPolicies;
	}

	@Override
	public void onMessage(GenericMessageDto<E> inputMessage) throws Exception {

		final E inputEvent = inputMessage.getBody();

		final Reporting reporting = ReportingUtils.newReportingBuilder().predecessor(inputEvent.getUid())
				.newReporting("DataLifecycleTrigger");

		reporting.begin(
				ReportingUtils.newFilenameReportingInputFor(inputEvent.getProductFamily(),
						inputEvent.getKeyObjectStorage()),
				new ReportingMessage("Handling event for %s", inputEvent.getKeyObjectStorage()));

		try {
			final EvictionManagementJob evictionManagementJob = toEvictionManagementJob(inputEvent, retentionPolicies,
					reporting.getUid());

			final GenericPublicationMessageDto<EvictionManagementJob> outputMessage = new GenericPublicationMessageDto<EvictionManagementJob>(
					inputMessage.getId(), inputEvent.getProductFamily(), evictionManagementJob);

			mqiClient.publish(outputMessage, ProductCategory.EVICTION_MANAGEMENT_JOBS);
			reporting.end(new ReportingMessage("End handling event for %s", inputEvent.getKeyObjectStorage()));
		} catch (Exception e) {
			reporting.error(new ReportingMessage("Error on handling event for %s: %s", inputEvent.getKeyObjectStorage(),
					LogUtils.toString(e)));
			throw e;
		}

	}

	EvictionManagementJob toEvictionManagementJob(final E inputEvent, final List<RetentionPolicy> retentionPolicies,
			final UUID reportingUid) {
		final EvictionManagementJob evictionManagementJob = new EvictionManagementJob();

		final Date evictionDate = calculateEvictionDate(retentionPolicies, inputEvent.getCreationDate(),
				inputEvent.getProductFamily(), inputEvent.getKeyObjectStorage());
		evictionManagementJob.setProductFamily(inputEvent.getProductFamily());
		evictionManagementJob.setKeyObjectStorage(inputEvent.getKeyObjectStorage());
		evictionManagementJob.setEvictionDate(evictionDate);
		evictionManagementJob.setUid(reportingUid);
		if (evictionDate == null) {
			evictionManagementJob.setUnlimited(true);
		} else {
			evictionManagementJob.setUnlimited(false);
		}
		return evictionManagementJob;
	}

	Date calculateEvictionDate(List<RetentionPolicy> retentionPolicies, Date creationDate, ProductFamily productFamily,
			String obsKey) {

		String fileName = obsKey.contains("/") ? obsKey.substring(obsKey.lastIndexOf("/") + 1) : obsKey;

		for (RetentionPolicy r : retentionPolicies) {

			if (r.getProductFamily().equals(productFamily.name()) && Pattern.matches(r.getFilePattern(), fileName)) {
				if (r.getRetentionTimeDays() > 0) {
					LOG.info("retention time is {} days for file: {}", r.getRetentionTimeDays(), fileName);
					return Date.from(creationDate.toInstant().plus(Period.ofDays(r.getRetentionTimeDays())));
				} else {
					LOG.info("retention time is unlimited for file: {}", fileName);
					return null;
				}
			}
		}
		LOG.warn("no retention time found for file: {}", fileName);
		return null;
	}

	@Override
	public final void onTerminalError(final GenericMessageDto<E> message, final Exception error) {
		LOG.error(error);
		errorRepoAppender
				.send(new FailedProcessingDto(processConfig.getHostname(), new Date(), error.getMessage(), message));
	}

}
