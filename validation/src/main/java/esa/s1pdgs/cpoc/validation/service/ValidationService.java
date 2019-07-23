package esa.s1pdgs.cpoc.validation.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.processing.MetadataQueryException;
import esa.s1pdgs.cpoc.common.utils.DateUtils;
import esa.s1pdgs.cpoc.metadata.model.SearchMetadata;
import esa.s1pdgs.cpoc.obs_sdk.ObsClient;
import esa.s1pdgs.cpoc.obs_sdk.ObsObject;
import esa.s1pdgs.cpoc.obs_sdk.SdkClientException;
import esa.s1pdgs.cpoc.validation.service.metadata.MetadataService;

@Service
public class ValidationService {
	private static final Logger LOGGER = LogManager.getLogger(ValidationService.class);

	private final MetadataService metadataService;

	private final ObsClient obsClient;

	@Autowired
	public ValidationService(MetadataService metadataService, ObsClient obsClient) {
		this.metadataService = metadataService;
		this.obsClient = obsClient;
	}

	public boolean checkConsistencyForFamilyAndTimeFrame(ProductFamily family, String intervalStart, String intervalEnd)
			throws MetadataQueryException, SdkClientException {
		LOGGER.info("Validating for inconsistancy between time interval from {} and {}", intervalStart, intervalEnd);

		boolean consistent = true;

		List<SearchMetadata> metadataResults = null;
		try {
			metadataResults = metadataService.query(family, null, intervalStart, intervalEnd);
			if (metadataResults == null) {
				// set to empty list
				metadataResults = new ArrayList<>();
			}

			LOGGER.info("Metadata query for family '{}' returned {} results", family, metadataResults.size());

		} catch (MetadataQueryException ex) {
			String errorMessage = String.format("[ValidationTask] [subTask retrieveMetadata] [STOP KO] %s [code %d] %s",
					"LOG_ERROR", ex.getCode().getCode(), ex.getLogMessage());
			LOGGER.error(errorMessage);
			throw ex;
		}

		Map<String, ObsObject> obsResults = null;
		try {

			LocalDateTime localDateTimeStart = LocalDateTime.parse(intervalStart, DateUtils.METADATA_DATE_FORMATTER);
			LocalDateTime localDateTimeEnd = LocalDateTime.parse(intervalEnd, DateUtils.METADATA_DATE_FORMATTER);

			Date startDate = Date.from(localDateTimeStart.atZone(ZoneId.of("UTC")).toInstant());
			Date endDate = Date.from(localDateTimeEnd.atZone(ZoneId.of("UTC")).toInstant());

			obsResults = obsClient.listInterval(family, startDate, endDate);
			LOGGER.info("OBS query for family '{}' returned {} results", family, obsResults.size());
		} catch (SdkClientException | DateTimeParseException ex) {
			String errorMessage = String.format("[ValidationTask] [subTask retrieveObs] [STOP KO] %s %s", "LOG_ERROR",
					ex.getMessage());
			LOGGER.error(errorMessage);
			throw ex;
		}

		for (SearchMetadata smd : metadataResults) {
			if (obsResults.get(smd.getKeyObjectStorage()) == null) {
				// Metadata does exist, but no product in OBS
				consistent = false;
				LOGGER.info("Product {} does exist in metadata, but not in OBS", smd.getKeyObjectStorage());
			} else {
				// Metadata and product exists
				LOGGER.debug("Product {} does exist in metadata and OBS", smd.getKeyObjectStorage());
				obsResults.remove(smd.getKeyObjectStorage());
			}
		}

		if (obsResults.size() > 0) {
			LOGGER.info("Found {} products that exist in OBS, but not in metdata", obsResults.size());
			for (ObsObject product : obsResults.values()) {
				consistent = false;
				LOGGER.info("Product {} does exist in OBS, but not in metadata", product.getKey());
			}
		}

		return consistent;

	}

}
