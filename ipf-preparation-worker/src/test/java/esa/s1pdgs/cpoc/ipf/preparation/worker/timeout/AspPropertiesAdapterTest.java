package esa.s1pdgs.cpoc.ipf.preparation.worker.timeout;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

import esa.s1pdgs.cpoc.appcatalog.AppDataJob;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobProduct;
import esa.s1pdgs.cpoc.common.utils.DateUtils;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.AspProperties;
import esa.s1pdgs.cpoc.ipf.preparation.worker.type.segment.AspPropertiesAdapter;
import esa.s1pdgs.cpoc.ipf.preparation.worker.type.segment.L0SegmentProduct;
import esa.s1pdgs.cpoc.mqi.model.queue.CatalogEvent;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;

public class AspPropertiesAdapterTest {

	public final static String METADATA_DATE_FORMAT = DateUtils.METADATA_DATE_FORMAT;
	public final static DateTimeFormatter METADATA_DATE_FORMATTER = DateUtils.METADATA_DATE_FORMATTER;

	public final static int WAITING_TIME_HOURS_MINIMAL_FAST = 3;
	public final static int WAITING_TIME_HOURS_NOMINAL_FAST = 20;
	public final static int WAITING_TIME_HOURS_MINIMAL_NRT_PT = 1;
	public final static int WAITING_TIME_HOURS_NOMINAL_NRT_PT = 2;

	// --------------------------------------------------------------------------

	@Test
	public final void testTimeout_IfDisabled_ShallReturnFalse() throws ParseException {
		final AspPropertiesAdapter aspPropertiesAdapter = AspPropertiesAdapter.of(this.createAspProperties(true));

		final String sensingEndTime = "2020-01-22T10:00:00.000000Z";
		final String jobCreationTime = "2020-01-28T10:00:00.000000Z";

		final AppDataJob job = this.newJobWithMetadata(jobCreationTime, sensingEndTime, "FAST24");

		assertFalse(aspPropertiesAdapter.isTimeoutReached(job, sensingEndTime), "Expected timeout to be disabled!");
	}

	@Test
	public final void testTimeout_IfEnabled_ShallReturnTrue() throws ParseException {
		final AspPropertiesAdapter aspPropertiesAdapter = AspPropertiesAdapter.of(this.createAspProperties(false));
		final AspPropertiesAdapter aspPropertiesAdapter2 = AspPropertiesAdapter.of(this.createAspProperties());

		final String sensingEndTime = "2020-01-22T10:00:00.000000Z";
		final String jobCreationTime = "2020-01-26T10:00:00.000000Z";

		final AppDataJob job = this.newJobWithMetadata(jobCreationTime, sensingEndTime, "FAST24");

		assertTrue(aspPropertiesAdapter.isTimeoutReached(job, sensingEndTime),
				"Expected timeout to be enabled and reached!");
		assertTrue(aspPropertiesAdapter2.isTimeoutReached(job, sensingEndTime),
				"Expected timeout to be enabled and reached!");
	}

	@Test
	public final void testTimeout_IfTimeoutReached_ShallReturnTrue() throws ParseException {
		final AspPropertiesAdapter aspPropertiesAdapter = AspPropertiesAdapter.of(this.createAspProperties(false));

		final LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
		final String sensingEndTime = now.minusHours(WAITING_TIME_HOURS_NOMINAL_FAST + 1)
				.format(METADATA_DATE_FORMATTER);
		final String jobCreationTime = now.minusHours(WAITING_TIME_HOURS_NOMINAL_FAST - 1)
				.format(METADATA_DATE_FORMATTER);

		final AppDataJob job = this.newJobWithMetadata(jobCreationTime, sensingEndTime, "FAST24");

		assertTrue(aspPropertiesAdapter.isTimeoutReached(job, sensingEndTime), "Expected timeout to be reached!");
	}

	@Test
	public final void testTimeout_IfTimeoutNotReached_ShallReturnFalse() throws ParseException {
		final AspPropertiesAdapter aspPropertiesAdapter = AspPropertiesAdapter.of(this.createAspProperties(false));

		final LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
		final String sensingEndTime = now
				.minusHours(WAITING_TIME_HOURS_NOMINAL_FAST - WAITING_TIME_HOURS_MINIMAL_FAST - 1)
				.format(METADATA_DATE_FORMATTER);
		final String jobCreationTime = now
				.minusHours(WAITING_TIME_HOURS_NOMINAL_FAST - WAITING_TIME_HOURS_MINIMAL_FAST - 2)
				.format(METADATA_DATE_FORMATTER);

		final AppDataJob job = this.newJobWithMetadata(jobCreationTime, sensingEndTime, "FAST24");

		assertFalse(aspPropertiesAdapter.isTimeoutReached(job, sensingEndTime),
				"Expected timeout to not be reached!");
	}

	// --------------------------------------------------------------------------

	private final AppDataJob newJobWithMetadata(final String jobCreationDate, final String sensingStopTime,
			final String processMode) throws ParseException {
		final AppDataJob job = new AppDataJob();
		final CatalogEvent event = new CatalogEvent();
		event.setMetadata(Collections.emptyMap());

		final GenericMessageDto<CatalogEvent> mess = new GenericMessageDto<CatalogEvent>(1, "topic", event);
		job.getMessages().add(mess);
		job.setStopTime(sensingStopTime);
		job.setCreationDate(new SimpleDateFormat(METADATA_DATE_FORMAT).parse(jobCreationDate)); // TODO: check timezone

		final AppDataJobProduct appDataJobProduct = new AppDataJobProduct();
		appDataJobProduct.getMetadata().put("processMode", processMode);
		appDataJobProduct.getMetadata().put("productName", "product123");
		job.setProduct(appDataJobProduct);

		return job;
	}

	private AspProperties createAspProperties(boolean disableTimeout) {
		final AspProperties aspProperties = new AspProperties();

		aspProperties.setDisableTimeout(disableTimeout);
		aspProperties.setWaitingTimeHoursMinimalFast(WAITING_TIME_HOURS_MINIMAL_FAST);
		aspProperties.setWaitingTimeHoursNominalFast(WAITING_TIME_HOURS_NOMINAL_FAST);
		aspProperties.setWaitingTimeHoursMinimalNrtPt(WAITING_TIME_HOURS_MINIMAL_NRT_PT);
		aspProperties.setWaitingTimeHoursNominalNrtPt(WAITING_TIME_HOURS_NOMINAL_NRT_PT);

		return aspProperties;
	}

	private AspProperties createAspProperties() {
		final AspProperties aspProperties = new AspProperties();

		aspProperties.setWaitingTimeHoursMinimalFast(WAITING_TIME_HOURS_MINIMAL_FAST);
		aspProperties.setWaitingTimeHoursNominalFast(WAITING_TIME_HOURS_NOMINAL_FAST);
		aspProperties.setWaitingTimeHoursMinimalNrtPt(WAITING_TIME_HOURS_MINIMAL_NRT_PT);
		aspProperties.setWaitingTimeHoursNominalNrtPt(WAITING_TIME_HOURS_NOMINAL_NRT_PT);

		return aspProperties;
	}

}
