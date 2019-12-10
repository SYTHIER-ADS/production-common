package esa.s1pdgs.cpoc.ipf.preparation.trigger.tasks;

import java.util.List;

import org.springframework.util.CollectionUtils;

import esa.s1pdgs.cpoc.appcatalog.AppDataJob;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobProduct;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobState;
import esa.s1pdgs.cpoc.appcatalog.client.job.AppCatalogJobClient;
import esa.s1pdgs.cpoc.appstatus.AppStatus;
import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.errorrepo.ErrorRepoAppender;
import esa.s1pdgs.cpoc.ipf.preparation.trigger.config.ProcessSettings;
import esa.s1pdgs.cpoc.metadata.client.MetadataClient;
import esa.s1pdgs.cpoc.mqi.client.GenericMqiClient;
import esa.s1pdgs.cpoc.mqi.client.StatusService;
import esa.s1pdgs.cpoc.mqi.model.queue.CatalogEvent;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;

public final class L0SliceConsumer extends AbstractGenericConsumer<CatalogEvent> {  
	public L0SliceConsumer(
			final ProcessSettings processSettings, 
			final GenericMqiClient mqiClient,
			final StatusService mqiStatusService, 
			final AppCatalogJobClient<CatalogEvent> appDataService,
			final ErrorRepoAppender errorRepoAppender, 
			final AppStatus appStatus,
			final MetadataClient metadataClient
	) {
		super(
				processSettings, 
				mqiClient, 
				mqiStatusService, 
				appDataService, 
				appStatus, 
				errorRepoAppender,
				ProductCategory.EDRS_SESSIONS,
				metadataClient
		);
	}
	
    @Override
	protected final AppDataJob<CatalogEvent> dispatch(final GenericMessageDto<CatalogEvent> mqiMessage)
			throws AbstractCodedException {
        final AppDataJob<CatalogEvent> appDataJob = buildJob(mqiMessage);
        final String productName = appDataJob.getProduct().getProductName();
        LOGGER.info("Dispatching product {}", productName);

        if (appDataJob.getState() == AppDataJobState.WAITING) {
            appDataJob.setState(AppDataJobState.DISPATCHING);
            return appDataService.patchJob(appDataJob.getId(), appDataJob, false, false, false);
        }
        return appDataJob;        
	}

	private final AppDataJob<CatalogEvent> buildJob(final GenericMessageDto<CatalogEvent> mqiMessage)
            throws AbstractCodedException {
        final CatalogEvent event = mqiMessage.getBody();

        // Check if a job is already created for message identifier
        final List<AppDataJob<CatalogEvent>> existingJobs = appDataService
                .findByMessagesId(mqiMessage.getId());

        if (CollectionUtils.isEmpty(existingJobs)) {
        	final CatalogEventAdapter eventAdapter = new CatalogEventAdapter(event);
            
            // Create the JOB
            final AppDataJob<CatalogEvent> jobDto = new AppDataJob<>();
            // General details
            jobDto.setLevel(processSettings.getLevel());
            jobDto.setPod(processSettings.getHostname());
            // Messages
            jobDto.getMessages().add(mqiMessage);
            // Product
            final AppDataJobProduct productDto = new AppDataJobProduct();
            productDto.setAcquisition(eventAdapter.swathType());
            productDto.setMissionId(eventAdapter.missionId());
            productDto.setProductName(event.getKeyObjectStorage());
            productDto.setProcessMode(eventAdapter.processMode());
            productDto.setSatelliteId(eventAdapter.satelliteId());
            productDto.setStartTime(eventAdapter.startTime());
            productDto.setStopTime(eventAdapter.stopTime());
            productDto.setStationCode(eventAdapter.stationCode());   
           	productDto.setPolarisation(eventAdapter.polarisation()); 
            jobDto.setProduct(productDto);

            return appDataService.newJob(jobDto);

        } else {
            // Update pod if needed
			AppDataJob<CatalogEvent> jobDto = existingJobs.get(0);

            if (!jobDto.getPod().equals(processSettings.getHostname())) {
                jobDto.setPod(processSettings.getHostname());
                jobDto = appDataService.patchJob(jobDto.getId(), jobDto, false, false, false);
            }
            // Job already exists
            return jobDto;
        }
    }
}
