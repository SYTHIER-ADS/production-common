package fr.viveris.s1pdgs.jobgenerator.controller;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import fr.viveris.s1pdgs.jobgenerator.config.L0SlicePatternSettings;
import fr.viveris.s1pdgs.jobgenerator.controller.dto.L0SliceDto;
import fr.viveris.s1pdgs.jobgenerator.exception.AbstractCodedException;
import fr.viveris.s1pdgs.jobgenerator.exception.InvalidFormatProduct;
import fr.viveris.s1pdgs.jobgenerator.exception.MaxNumberCachedJobsReachException;
import fr.viveris.s1pdgs.jobgenerator.exception.MissingRoutingEntryException;
import fr.viveris.s1pdgs.jobgenerator.model.Job;
import fr.viveris.s1pdgs.jobgenerator.model.ResumeDetails;
import fr.viveris.s1pdgs.jobgenerator.model.product.L0Slice;
import fr.viveris.s1pdgs.jobgenerator.model.product.L0SliceProduct;
import fr.viveris.s1pdgs.jobgenerator.tasks.dispatcher.L0SliceJobsDispatcher;
import fr.viveris.s1pdgs.jobgenerator.utils.DateUtils;

/**
 * KAFKA consumer for EDRS session files. Once the 2 files of the same session
 * are received, the consumer sends the session to the job dispatcher
 * 
 * @author Cyrielle Gailliard
 *
 */
@Component
@ConditionalOnProperty(prefix = "kafka.enable-consumer", name = "l0-slices")
public class L0SlicesConsumer {

	/**
	 * Format of dates used in filename of the products
	 */
	protected static final String DATE_FORMAT = "yyyyMMdd'T'HHmmss";

	/**
	 * Logger
	 */
	private static final Logger LOGGER = LogManager.getLogger(L0SlicesConsumer.class);

	/**
	 * Dispatcher of l0 slices
	 */
	private final L0SliceJobsDispatcher jobsDispatcher;

	/**
	 * Settings used to extract information from L0 product name
	 */
	private final L0SlicePatternSettings patternSettings;

	/**
	 * Pattern built from the regular expression given in configuration
	 */
	private final Pattern l0SLicesPattern;

	/**
	 * Name of the topic
	 */
	private final String topicName;

	/**
	 * Constructor
	 * 
	 * @param jobsDispatcher
	 * @param edrsSessionFileService
	 */
	@Autowired
	public L0SlicesConsumer(final L0SliceJobsDispatcher jobsDispatcher, final L0SlicePatternSettings patternSettings,
			@Value("${kafka.topics.l0-slices}") final String topicName) {
		this.jobsDispatcher = jobsDispatcher;
		this.patternSettings = patternSettings;
		this.l0SLicesPattern = Pattern.compile(this.patternSettings.getRegexp(), Pattern.CASE_INSENSITIVE);
		this.topicName = topicName;
	}

	/**
	 * Message listener container. Read a message.<br/>
	 * <ul>
	 * <li>If we receive the 2nd channel of a cached session, we send the session to
	 * the dispatcher</li>
	 * <li>If we receive a session not cached, we put the session in the cache</li>
	 * <li>Else we ignore the message</li>
	 * </ul>
	 * Furthermore, as soon as we receive a message, we clean the cache and remove
	 * sessions cached for too long
	 * 
	 * @param payload
	 */
	@KafkaListener(topics = "${kafka.topics.l0-slices}", groupId = "${kafka.group-id}", containerFactory = "l0SlicesKafkaListenerContainerFactory")
	public void receive(L0SliceDto dto) {

		LOGGER.info("[MONITOR] [step 0] [productName {}] Starting job generation", dto.getProductName());
		int step = 1;

		try {

			LOGGER.info("[MONITOR] [step 1] [productName {}] Building product", dto.getProductName());
			Matcher m = l0SLicesPattern.matcher(dto.getProductName());
			if (!m.matches()) {
				throw new InvalidFormatProduct(
						"Don't match with regular expression " + this.patternSettings.getRegexp());
			}
			String satelliteId = m.group(this.patternSettings.getMGroupSatId());
			String missionId = m.group(this.patternSettings.getMGroupMissionId());
			String acquisition = m.group(this.patternSettings.getMGroupAcquisition());
			String startTime = m.group(this.patternSettings.getMGroupStartTime());
			String stopTime = m.group(this.patternSettings.getMGroupStopTime());
			Date dateStart = DateUtils.convertWithSimpleDateFormat(startTime, DATE_FORMAT);
			Date dateStop = DateUtils.convertWithSimpleDateFormat(stopTime, DATE_FORMAT);

			// Initialize the JOB
			L0Slice slice = new L0Slice(acquisition);
			L0SliceProduct product = new L0SliceProduct(dto.getProductName(), satelliteId, missionId, dateStart,
					dateStop, slice);
			Job<L0Slice> job = new Job<>(product, new ResumeDetails(topicName, dto));

			// Dispatch job
			step++;
			LOGGER.info("[MONITOR] [step 2] [productName {}] Dispatching product", dto.getProductName());
			this.jobsDispatcher.dispatch(job);

		} catch (MaxNumberCachedJobsReachException | MissingRoutingEntryException mnce) {
			LOGGER.error("[MONITOR] [step {}] [productName {}] [resuming {}] [code {}] {} ", step,
					dto.getKeyObjectStorage(), new ResumeDetails(topicName, dto), mnce.getCode().getCode(), mnce.getLogMessage());
		} catch (AbstractCodedException e) {
			LOGGER.error("[MONITOR] [step {}] [productName {}] [code {}] {} ", step, dto.getProductName(),
					e.getCode().getCode(), e.getLogMessage());
		}

		LOGGER.info("[MONITOR] [step 0] [productName {}] End", dto.getProductName());
	}

}
