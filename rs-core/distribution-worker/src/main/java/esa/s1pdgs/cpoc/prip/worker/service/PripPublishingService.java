package esa.s1pdgs.cpoc.prip.worker.service;

import static esa.s1pdgs.cpoc.mqi.model.queue.util.CompressionEventUtil.removeZipSuffix;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.obs.ObsException;
import esa.s1pdgs.cpoc.common.errors.processing.MetadataQueryException;
import esa.s1pdgs.cpoc.common.utils.DateUtils;
import esa.s1pdgs.cpoc.common.utils.LogUtils;
import esa.s1pdgs.cpoc.common.utils.Retries;
import esa.s1pdgs.cpoc.metadata.client.MetadataClient;
import esa.s1pdgs.cpoc.metadata.model.MissionId;
import esa.s1pdgs.cpoc.metadata.model.SearchMetadata;
import esa.s1pdgs.cpoc.mqi.model.queue.CompressionEvent;
import esa.s1pdgs.cpoc.obs_sdk.ObsClient;
import esa.s1pdgs.cpoc.obs_sdk.ObsObject;
import esa.s1pdgs.cpoc.prip.metadata.PripMetadataRepository;
import esa.s1pdgs.cpoc.prip.model.Checksum;
import esa.s1pdgs.cpoc.prip.model.GeoShapeLineString;
import esa.s1pdgs.cpoc.prip.model.GeoShapePolygon;
import esa.s1pdgs.cpoc.prip.model.PripGeoCoordinate;
import esa.s1pdgs.cpoc.prip.model.PripMetadata;
import esa.s1pdgs.cpoc.prip.worker.configuration.PripWorkerConfigurationProperties;
import esa.s1pdgs.cpoc.prip.worker.mapping.MdcToPripMapper;
import esa.s1pdgs.cpoc.prip.worker.report.PripReportingInput;
import esa.s1pdgs.cpoc.prip.worker.report.PripReportingOutput;
import esa.s1pdgs.cpoc.report.Reporting;
import esa.s1pdgs.cpoc.report.ReportingInput;
import esa.s1pdgs.cpoc.report.ReportingMessage;
import esa.s1pdgs.cpoc.report.ReportingUtils;

@Service
public class PripPublishingService implements Consumer<CompressionEvent> {

	private static final Logger LOGGER = LogManager.getLogger(PripPublishingService.class);
	
	private static final String PATTERN_STR = "^S1[AB]_OPER_AUX_QCSTDB_.*$";
	private static final Pattern PATTERN_NO_MDC = Pattern.compile(PATTERN_STR, Pattern.CASE_INSENSITIVE);

	private final ObsClient obsClient;
	private final MetadataClient metadataClient;
	private final PripMetadataRepository pripMetadataRepo;
	private final PripWorkerConfigurationProperties props;
	private final MdcToPripMapper mdcToPripMapper;
	
	@Autowired
	public PripPublishingService(
			final ObsClient obsClient,
			final MetadataClient metadataClient,
			final PripMetadataRepository pripMetadataRepo,
			final PripWorkerConfigurationProperties props
	) {
		this.obsClient = obsClient;
		this.metadataClient = metadataClient;
		this.pripMetadataRepo = pripMetadataRepo;
		this.props = props;
		mdcToPripMapper = new MdcToPripMapper(props.getProductTypeRegexp(), props.getMetadataMapping());
	}
	
	
	@Override
	public void accept(CompressionEvent compressionEvent) {
	
		LOGGER.debug("starting saving PRIP metadata, got message: {}", compressionEvent);


		final Reporting reporting = ReportingUtils
				.newReportingBuilder(MissionId.fromFileName(compressionEvent.getKeyObjectStorage()))
				.predecessor(compressionEvent.getUid()).newReporting("PripWorker");

		final String name = removeZipSuffix(compressionEvent.getKeyObjectStorage());

		final ReportingInput in = PripReportingInput.newInstance(name, compressionEvent.getProductFamily());
		reporting.begin(in, new ReportingMessage("Publishing file %s in PRIP", name));
		
		try {
			createAndSave(compressionEvent);
		} catch (MetadataQueryException | InterruptedException | PripPublishingException e) {
			final String errorMessage = String.format("Error on publishing file %s in PRIP: %s", name,
					LogUtils.toString(e));
			reporting.error(new ReportingMessage(errorMessage));
			LOGGER.error(errorMessage);
//					errorAppender.send(
//							new FailedProcessingDto(props.getHostname(), new Date(), errorMessage, message));
		}
	
		reporting.end(PripReportingOutput.newInstance(new Date()), new ReportingMessage("Finished publishing file %s in PRIP", name));
	}

	private final void createAndSave(final CompressionEvent compressionEvent) throws MetadataQueryException, InterruptedException, PripPublishingException {
		
		if (pripMetadataAlreadyExists(compressionEvent.getKeyObjectStorage())) {
			throw new PripPublishingException(String.format("PRiP metadata for file %s already exists!", removeZipSuffix(compressionEvent.getKeyObjectStorage())));
		}
		
		final LocalDateTime creationDate = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

		PripMetadata pripMetadata = new PripMetadata();		
		pripMetadata.setId(UUID.randomUUID());
		pripMetadata.setObsKey(compressionEvent.getKeyObjectStorage());
		pripMetadata.setName(compressionEvent.getKeyObjectStorage());
		pripMetadata.setProductFamily(compressionEvent.getProductFamily());
		pripMetadata.setContentType(PripMetadata.DEFAULT_CONTENTTYPE);
		pripMetadata.setContentLength(
				getContentLength(compressionEvent.getProductFamily(), compressionEvent.getKeyObjectStorage()));
		pripMetadata.setCreationDate(creationDate);
		pripMetadata.setEvictionDate(creationDate.plusDays(PripMetadata.DEFAULT_EVICTION_DAYS));
		pripMetadata
				.setChecksums(getChecksums(compressionEvent.getProductFamily(), compressionEvent.getKeyObjectStorage()));
		
		if(mdcQuery(compressionEvent.getKeyObjectStorage())) {
		
			final SearchMetadata searchMetadata = queryMetadata(
					compressionEvent.getProductFamily(),
					compressionEvent.getKeyObjectStorage()
			);
			
			// ValidityStart: mandatory field, only optional when plan and report
			if (! ProductFamily.PLAN_AND_REPORT_ZIP.equals(compressionEvent.getProductFamily()) || Strings.isNotEmpty(searchMetadata.getValidityStart())) {
				pripMetadata.setContentDateStart(DateUtils.parse(searchMetadata.getValidityStart()).truncatedTo(ChronoUnit.MILLIS));
			}
			
			// ValidityStop: mandatory field, only optional when plan and report
			if (! ProductFamily.PLAN_AND_REPORT_ZIP.equals(compressionEvent.getProductFamily()) || Strings.isNotEmpty(searchMetadata.getValidityStop())) {
				pripMetadata.setContentDateEnd(DateUtils.parse(searchMetadata.getValidityStop()).truncatedTo(ChronoUnit.MILLIS));
			}
					
			Map<String, Object> pripAttributes = mdcToPripMapper.map(compressionEvent.getKeyObjectStorage(),
					searchMetadata.getProductType(), searchMetadata.getAdditionalProperties());		
			pripMetadata.setAttributes(pripAttributes);
			
			final List<PripGeoCoordinate> coordinates = new ArrayList<>();
			if (null != searchMetadata.getFootprint()) {
				LOGGER.debug("Footprint given with {} coordinates", searchMetadata.getFootprint().size());
				for (final List<Double> p : searchMetadata.getFootprint()) {
					coordinates.add(new PripGeoCoordinate(p.get(0), p.get(1)));
				}
			}
			if (!coordinates.isEmpty()) {
				// Differentiate polygon and linestring!
				String footprintIsLineStringCondition = props.getMetadataMapping().getFootprintIsLineStringRegexp();
				boolean isLineString = pripMetadata.getName().matches(footprintIsLineStringCondition);
				LOGGER.debug("Product '{}' matching '{}': {}", pripMetadata.getName(), footprintIsLineStringCondition, isLineString);
				if (isLineString) {
					LOGGER.debug("Assuming that footprint with {} points is of type 'linestring'", coordinates.size());
					pripMetadata.setFootprint(new GeoShapeLineString(coordinates));
				} else if (coordinates.size() >= 4) {
					pripMetadata.setFootprint(new GeoShapePolygon(coordinates));
				} else {
					LOGGER.warn("No valid footprint of type 'polygon' (must be >= 4 points) -> Footprint ignored!");
				}
			}		
		}
		Retries.performWithRetries(() -> {
			pripMetadataRepo.save(pripMetadata);
			return null;
		}, "saving prip metadata of " + compressionEvent.getKeyObjectStorage(), props.getMetadataInsertionRetriesNumber(),
				props.getMetadataInsertionRetriesIntervalMs());

		LOGGER.debug("end of saving PRIP metadata: {}", pripMetadata);
	}
	
	private boolean mdcQuery(String key) {
		final Matcher m = PATTERN_NO_MDC.matcher(key);

		if (m.matches()) {
			return false;
		} else {
			return true;
		}
	}
	
	private boolean pripMetadataAlreadyExists(final String key) throws InterruptedException {
	 if(Retries.performWithRetries(() -> {
		 return pripMetadataRepo.findByName(key);
		 },
			 "PRIP metadata query for " + key,
			 props.getMetadataUnavailableRetriesNumber(),
			 props.getMetadataUnavailableRetriesIntervalMs()) == null) {
		 return false;
	 } else {
		 return true;
	 }
	}

	private SearchMetadata queryMetadata(final ProductFamily productFamily, final String keyObjectStorage) throws MetadataQueryException, InterruptedException {
		return Retries.performWithRetries(() -> {
				return metadataClient.queryByFamilyAndProductName(
						removeZipSuffix(productFamily.name()),
						removeZipSuffix(keyObjectStorage)
				);
			}, 
			"metadata query for " + keyObjectStorage, 
			props.getMetadataUnavailableRetriesNumber(), 
			props.getMetadataUnavailableRetriesIntervalMs()
	    );

	}
	
	private long getContentLength(final ProductFamily family, final String key) {
		long contentLength = 0;
		try {
			contentLength = obsClient.size(new ObsObject(family, key));

		} catch (final ObsException e) {
			LOGGER.warn(String.format("could not determine content length of %s", key), e);
		}
		return contentLength;
	}

	private List<Checksum> getChecksums(final ProductFamily family, final String key) {
		final Checksum checksum = new Checksum();
		checksum.setAlgorithm(Checksum.DEFAULT_ALGORITHM);
		checksum.setValue("");
		try {
			final ObsObject obsObject = new ObsObject(family, key);
			final String value = obsClient.getChecksum(obsObject);
			checksum.setValue(value);
			
			final Instant checksumDate = obsClient.getChecksumDate(obsObject);
			if (null == checksumDate ) {
				LOGGER.warn(String.format("could not determine checksum date for %s", key));
			} else {
				checksum.setDate(LocalDateTime.ofInstant(checksumDate, ZoneOffset.UTC));
			}
		} catch (final ObsException e) {
			LOGGER.warn(String.format("could not determine checksum of %s", key), e);
		}
		return Arrays.asList(checksum);
	}
	
}
