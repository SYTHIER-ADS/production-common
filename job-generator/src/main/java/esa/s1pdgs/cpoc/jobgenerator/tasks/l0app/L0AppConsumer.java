package esa.s1pdgs.cpoc.jobgenerator.tasks.l0app;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;

import esa.s1pdgs.cpoc.appcatalog.client.job.AppCatalogJobClient;
import esa.s1pdgs.cpoc.appcatalog.server.job.db.AppDataJob;
import esa.s1pdgs.cpoc.appcatalog.server.job.db.AppDataJobFile;
import esa.s1pdgs.cpoc.appcatalog.server.job.db.AppDataJobProduct;
import esa.s1pdgs.cpoc.appcatalog.server.job.db.AppDataJobState;
import esa.s1pdgs.cpoc.common.EdrsSessionFileType;
import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.errors.InvalidFormatProduct;
import esa.s1pdgs.cpoc.errorrepo.ErrorRepoAppender;
import esa.s1pdgs.cpoc.errorrepo.model.rest.FailedProcessingDto;
import esa.s1pdgs.cpoc.jobgenerator.config.ProcessSettings;
import esa.s1pdgs.cpoc.jobgenerator.status.AppStatus;
import esa.s1pdgs.cpoc.jobgenerator.tasks.AbstractGenericConsumer;
import esa.s1pdgs.cpoc.jobgenerator.tasks.AbstractJobsDispatcher;
import esa.s1pdgs.cpoc.metadata.client.MetadataClient;
import esa.s1pdgs.cpoc.metadata.model.EdrsSessionMetadata;
import esa.s1pdgs.cpoc.mqi.client.GenericMqiClient;
import esa.s1pdgs.cpoc.mqi.client.StatusService;
import esa.s1pdgs.cpoc.mqi.model.queue.EdrsSessionDto;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;
import esa.s1pdgs.cpoc.report.FilenameReportingInput;
import esa.s1pdgs.cpoc.report.LoggerReporting;
import esa.s1pdgs.cpoc.report.Reporting;
import esa.s1pdgs.cpoc.report.ReportingMessage;

public class L0AppConsumer extends AbstractGenericConsumer<EdrsSessionDto> {
    
    /**
     * 
     */
    private String taskForFunctionalLog;
    
    private final MetadataClient metadataClient;

    public L0AppConsumer(
            final AbstractJobsDispatcher<EdrsSessionDto> jobDispatcher,
            final ProcessSettings processSettings,
            final GenericMqiClient mqiService,
            final StatusService mqiStatusService,
            final AppCatalogJobClient appDataService,
            final ErrorRepoAppender errorRepoAppender,
            final AppStatus appStatus,
            final MetadataClient metadataClient) {
        super(jobDispatcher, processSettings, mqiService, mqiStatusService,
                appDataService, appStatus, errorRepoAppender, ProductCategory.EDRS_SESSIONS);
        this.metadataClient = metadataClient; 
    }

	@Scheduled(fixedDelayString = "${process.fixed-delay-ms}", initialDelayString = "${process.initial-delay-ms}")
    public void consumeMessages() {    	
        final Reporting.Factory reportingFactory = new LoggerReporting.Factory("L0JobGeneration");   
        
        // First, consume message
        GenericMessageDto<EdrsSessionDto> mqiMessage = readMessage();
        if (mqiMessage == null || mqiMessage.getBody() == null) {
            LOGGER.trace("[MONITOR] [step 0] No message received: continue");
            return;
        }

        // Second process message
        EdrsSessionDto leveldto = mqiMessage.getBody();
        String productName = leveldto.getKeyObjectStorage();

        if (leveldto.getProductType() == EdrsSessionFileType.SESSION) {

            int step = 0;
            boolean ackOk = false;
            String errorMessage = "";
            // Note: the report log of consume and global log is raised during
            // building job to get the session identifier which is the real
            // product name
            appStatus.setProcessing(mqiMessage.getIdentifier());            
    	 	
            final Reporting reporting = reportingFactory.newReporting(0);
            FailedProcessingDto failedProc = new FailedProcessingDto();
            
            try {

                // Create the EdrsSessionFile object from the consumed message
                step = 1;
                LOGGER.info(
                        "[MONITOR] [step {}] [productName {}] Building product",
                        step, leveldto.getKeyObjectStorage());
                if (leveldto.getChannelId() != 1
                        && leveldto.getChannelId() != 2) {
                    throw new InvalidFormatProduct("Invalid channel identifier "
                            + leveldto.getChannelId());
                }
                reporting.begin(
                		new FilenameReportingInput(Collections.singletonList(mqiMessage.getBody().getKeyObjectStorage())),
                		new ReportingMessage("Start job generation using  {}", mqiMessage.getBody().getKeyObjectStorage())
                );
                
                AppDataJob<EdrsSessionDto> appDataJob = buildJob(mqiMessage);
                
                LOGGER.debug ("== appDataJob(1) {}",appDataJob.toString());
                productName = appDataJob.getProduct().getProductName();

                // Dispatch
                step++;
                if (appDataJob.getMessages().size() == 2) {
                    LOGGER.info(
                            "[MONITOR] [step 2] [productName {}] Dispatching product",
                            productName);
                    if (appDataJob.getState() == AppDataJobState.WAITING) {
                        appDataJob.setState(AppDataJobState.DISPATCHING);
                        appDataJob = appDataService.patchJob(
                                appDataJob.getIdentifier(), appDataJob, false,
                                false, false);
                        LOGGER.debug ("== appDataJob(2) {}",appDataJob.toString());
                    }
                    jobsDispatcher.dispatch(appDataJob);
                }
                // Ack
                step++;
                ackOk = true;
            } catch (AbstractCodedException ace) {
                ackOk = false;
                errorMessage = String.format(
                        "[MONITOR] [step %d] [productName %s] [code %d] %s",
                        step, productName, ace.getCode().getCode(),
                        ace.getLogMessage());
                
                reporting.error(new ReportingMessage("[code {}] {}", ace.getCode().getCode(), ace.getLogMessage()));
                
                failedProc = new FailedProcessingDto(processSettings.getHostname(),new Date(),errorMessage, mqiMessage);
            }  
            
            // Ack and check if application shall stopped
            ackProcessing(mqiMessage, failedProc, ackOk, productName, errorMessage);

            step = 0;
            LOGGER.info("[MONITOR] [step 0] [productName {}] End", step,
                    leveldto.getKeyObjectStorage());
                       
            reporting.end(new ReportingMessage("End job generation using {}", mqiMessage.getBody().getKeyObjectStorage()));
        }

    }

    protected AppDataJob<EdrsSessionDto> buildJob(GenericMessageDto<EdrsSessionDto> mqiMessage)
            throws AbstractCodedException {
    	
    	// Check if a job is already created for message identifier
        List<AppDataJob> existingJobs = appDataService
                .findByMessagesIdentifier(mqiMessage.getIdentifier());

        if (CollectionUtils.isEmpty(existingJobs)) {
        	final EdrsSessionDto sessionDto = mqiMessage.getBody();
        	final String productType = sessionDto.getProductType().name();
        	final String productName = new File(sessionDto.getProductName()).getName();
        	LOGGER.debug("Querying metadata for product {} of type {}", productName, productType); 
        	final EdrsSessionMetadata edrsSessionMetadata = metadataClient.getEdrsSession(productType, productName);
           	LOGGER.debug ("Got result {}", edrsSessionMetadata); 
        	
            // Search if session is already in progress
            List<AppDataJob<EdrsSessionDto>> existingJobsForSession =
                    appDataService.findByProductSessionId(sessionDto.getSessionId()).stream()
                    .map(s -> (AppDataJob<EdrsSessionDto>) s).collect(Collectors.toList());

            if (CollectionUtils.isEmpty(existingJobsForSession)) {
            	LOGGER.debug ("== creating jobDTO from {}",mqiMessage ); 
                // Create the JOB
                AppDataJob<EdrsSessionDto> jobDto = new AppDataJob<>();
                // General details
                jobDto.setLevel(processSettings.getLevel());
                jobDto.setPod(processSettings.getHostname());
                // Messages
                jobDto.getMessages().add(mqiMessage);
                // Product
                AppDataJobProduct productDto = new AppDataJobProduct();
                productDto.setProductType(productType);
                productDto.setSessionId(sessionDto.getSessionId());
                productDto.setMissionId(edrsSessionMetadata.getMissionId());
                productDto.setStationCode(sessionDto.getStationCode());
                productDto.setProductName(sessionDto.getSessionId());
                productDto.setSatelliteId(sessionDto.getSatelliteId());
                productDto.setStartTime(edrsSessionMetadata.getStartTime());
                productDto.setStopTime(edrsSessionMetadata.getStopTime());

                if (sessionDto.getChannelId() == 1) {
                    LOGGER.debug ("== ch1 ");    
                    productDto.setRaws1(edrsSessionMetadata.getRawNames().stream().map(
                            s -> new AppDataJobFile(s))
                    		.collect(Collectors.toList()));
                } else {
                	LOGGER.debug ("== ch2 ");
                    productDto.setRaws2(edrsSessionMetadata.getRawNames().stream().map(
                            s -> new AppDataJobFile(s))
                            .collect(Collectors.toList()));
                }

                jobDto.setProduct(productDto);
               
                LOGGER.debug ("== jobDTO {}",jobDto.toString());
                AppDataJob<EdrsSessionDto> newJobDto = appDataService.newJob(jobDto);
                LOGGER.debug ("== newJobDto {}",newJobDto.toString());
                return newJobDto;
            } else {

                // Update pod if needed
                boolean update = false;
                boolean updateMessage = false;
                boolean updateProduct = false;
                AppDataJob<EdrsSessionDto> jobDto = (AppDataJob<EdrsSessionDto>) existingJobsForSession.get(0);
                LOGGER.debug ("== existingJobsForSession.get(0) jobDto {}", jobDto.toString());
                
                if (!jobDto.getPod().equals(processSettings.getHostname())) {
                    jobDto.setPod(processSettings.getHostname());
                    update = true;
                }
                
                final List<GenericMessageDto<EdrsSessionDto>> mess = jobDto.getMessages().stream()
                		.map(s -> (GenericMessageDto<EdrsSessionDto>)s)
                		.collect(Collectors.toList());
                
                LOGGER.debug ("== existing message {}", jobDto.getMessages().toString());
                
				final GenericMessageDto<EdrsSessionDto> firstMess = (GenericMessageDto<EdrsSessionDto>) jobDto.getMessages().get(0);
                
				LOGGER.debug ("== firstMessage {}",firstMess.toString());
                // Updates messages if needed
                final EdrsSessionDto dto = firstMess.getBody();
                
                if (jobDto.getMessages().size() == 1 && dto.getChannelId() != mqiMessage.getBody().getChannelId()) {
                	LOGGER.debug ("== existing message {}",jobDto.getMessages());
                	
                    jobDto.getMessages().add(mqiMessage);
                    if (mqiMessage.getBody().getChannelId() == 1) {
                        jobDto.getProduct()
                                .setRaws1(edrsSessionMetadata.getRawNames().stream()
                                        .map(s -> new AppDataJobFile(s))
                                        .collect(Collectors.toList()));
                        
                        LOGGER.debug ("== channel1 ");    
                        
                    } else {
                        jobDto.getProduct()
                                .setRaws2(edrsSessionMetadata.getRawNames().stream()
                                        .map(s -> new AppDataJobFile(s))
                                        .collect(Collectors.toList()));
                        LOGGER.debug ("== channel2 ");
                    }
                    update = true;
                    updateMessage = true;
                    updateProduct = true;
                }
                // Update
                if (update) {
                    jobDto = appDataService.patchJob(jobDto.getIdentifier(),
                            jobDto, updateMessage, updateProduct, false);
                    LOGGER.debug ("== updated(1) jobDto {}", jobDto.toString());
                }
                // Return object
                return jobDto;

            }

        } else {
            // Update pod if needed
            boolean update = false;
            boolean updateMessage = false;
            boolean updateProduct = false;
            AppDataJob jobDto = existingJobs.get(0);
            LOGGER.debug ("== existingJobs.get(0) jobDto {}", jobDto.toString());

            if (!jobDto.getPod().equals(processSettings.getHostname())) {
                jobDto.setPod(processSettings.getHostname());
                update = true;
            }
            // Update
            if (update) {
                jobDto = appDataService.patchJob(jobDto.getIdentifier(), jobDto,
                        updateMessage, updateProduct, false);
                LOGGER.debug ("== updated(2) jobDto {}", jobDto.toString());
            }
            // Return object
            return jobDto;
        }

    }

    @Override
    protected String getTaskForFunctionalLog() {
    	return this.taskForFunctionalLog;
    }
    
    @Override
    public void setTaskForFunctionalLog(String taskForFunctionalLog) {
    	this.taskForFunctionalLog = taskForFunctionalLog; 
    }
}
