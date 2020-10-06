package esa.s1pdgs.cpoc.ipf.preparation.worker.type.pdu.generator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import esa.s1pdgs.cpoc.appcatalog.AppDataJob;
import esa.s1pdgs.cpoc.common.errors.processing.MetadataQueryException;
import esa.s1pdgs.cpoc.common.utils.DateUtils;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.PDUSettings.PDUTypeSettings;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.ProcessSettings;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.TimeInterval;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.pdu.PDUReferencePoint;
import esa.s1pdgs.cpoc.metadata.client.MetadataClient;
import esa.s1pdgs.cpoc.metadata.model.S3Metadata;
import esa.s1pdgs.cpoc.mqi.model.queue.IpfPreparationJob;

public class PDUStripeGenerator extends AbstractPDUGenerator implements PDUGenerator {
	private static final Logger LOGGER = LogManager.getLogger(PDUStripeGenerator.class);

	private final PDUTypeSettings settings;
	private final ProcessSettings processSettings;
	private final MetadataClient mdClient;

	public PDUStripeGenerator(final ProcessSettings processSettings, final PDUTypeSettings settings,
			final MetadataClient mdClient) {
		this.settings = settings;
		this.processSettings = processSettings;
		this.mdClient = mdClient;
	}

	@Override
	public List<AppDataJob> generateAppDataJobs(IpfPreparationJob job) throws MetadataQueryException {
		if (settings.getReference() == PDUReferencePoint.ORBIT) {
			final S3Metadata metadata = getMetadataForJobProduct(this.mdClient, job);

			// Check if this product is the first of its orbit
			if (checkIfFirstInOrbit(metadata, this.mdClient, job)) {
				// Product is first of orbit, generate PDU-Jobs
				LOGGER.debug("Product is first in orbit - generate PDUs with type STRIPE (Reference: Orbit)");
				final S3Metadata firstOfLastOrbit = mdClient.getFirstProductForOrbit(job.getProductFamily(),
						job.getEventMessage().getBody().getProductType(), metadata.getSatelliteId(),
						Long.parseLong(metadata.getAbsoluteStartOrbit()) - 1);

				String startTime = metadata.getAnxTime();
				if (firstOfLastOrbit != null) {
					startTime = firstOfLastOrbit.getAnx1Time();
				}

				List<TimeInterval> timeIntervals = generateTimeIntervals(startTime, metadata.getAnx1Time(),
						settings.getLengthInS());

				// Add offset to all time intervals start and stop times
				if (settings.getOffsetInS() > 0) {
					long offsetInNanos = (long) (settings.getOffsetInS() * 1000000000L);
					for (TimeInterval interval : timeIntervals) {
						interval.setStart(interval.getStart().plusNanos(offsetInNanos));
						interval.setStop(interval.getStop().plusNanos(offsetInNanos));
					}
				}

				return createJobsFromTimeIntervals(timeIntervals, job);
			}

			LOGGER.debug("Product is not first in orbit - skip PDU generation");
			return Collections.emptyList();
		} else if (settings.getReference() == PDUReferencePoint.DUMP) {
			S3Metadata metadata = getMetadataForJobProduct(mdClient, job);

			List<TimeInterval> intervals = findTimeIntervalsForMetadata(metadata, settings.getLengthInS());
			
			return createJobsFromTimeIntervals(intervals, job);
		}

		LOGGER.warn("Invalid reference point for pdu type STRIPE");
		return Collections.emptyList();
	}

	/**
	 * Create time intervals for PDU, so that the validityTime is inside the PDU
	 * intervals
	 */
	private List<TimeInterval> findTimeIntervalsForMetadata(final S3Metadata metadata, final double length) {
		List<TimeInterval> intervals = new ArrayList<>();

		LocalDateTime intervalStart = DateUtils.parse(metadata.getDumpStart());
		final LocalDateTime finalStop = DateUtils.parse(metadata.getValidityStop());

		final TimeInterval validityInterval = new TimeInterval(DateUtils.parse(metadata.getValidityStart()), finalStop);

		// Create next possible PDU interval and check if product is inside that
		// interval, if it is, add interval to list
		while (intervalStart.isBefore(finalStop)) {
			long lengthInNanos = (long) (length * 1000000000L);
			final LocalDateTime nextStop = intervalStart.plusNanos(lengthInNanos);
			final TimeInterval interval = new TimeInterval(intervalStart, nextStop);

			if (validityInterval.intersects(interval)) {
				intervals.add(interval);
			}
			intervalStart = nextStop;
		}

		return intervals;
	}

	/**
	 * Create a list of AppDataJobs from the given list of time intervals
	 */
	private List<AppDataJob> createJobsFromTimeIntervals(final List<TimeInterval> intervals,
			IpfPreparationJob preparationJob) {
		List<AppDataJob> jobs = new ArrayList<>();

		for (TimeInterval interval : intervals) {
			LOGGER.debug("Create AppDataJob for PDU time interval: [{}; {}]",
					DateUtils.formatToMetadataDateTimeFormat(interval.getStart()),
					DateUtils.formatToMetadataDateTimeFormat(interval.getStop()));
			AppDataJob appDataJob = AppDataJob.fromPreparationJob(preparationJob);
			appDataJob.setStartTime(DateUtils.formatToMetadataDateTimeFormat(interval.getStart()));
			appDataJob.setStopTime(DateUtils.formatToMetadataDateTimeFormat(interval.getStop()));

			if (processSettings.getProcessingGroup() != null) {
				appDataJob.setProcessingGroup(processSettings.getProcessingGroup());
			}

			jobs.add(appDataJob);
		}

		return jobs;
	}
}
