package esa.s1pdgs.cpoc.jobgenerator.tasks.l0segmentapp;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import esa.s1pdgs.cpoc.appcatalog.client.job.AppCatalogJobClient;
import esa.s1pdgs.cpoc.appcatalog.common.rest.model.job.AppDataJobDto;
import esa.s1pdgs.cpoc.appcatalog.common.rest.model.job.AppDataJobDtoState;
import esa.s1pdgs.cpoc.appcatalog.common.rest.model.job.AppDataJobProductDto;
import esa.s1pdgs.cpoc.common.MessageState;
import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.errors.InvalidFormatProduct;
import esa.s1pdgs.cpoc.errorrepo.ErrorRepoAppender;
import esa.s1pdgs.cpoc.errorrepo.model.rest.FailedProcessingDto;
import esa.s1pdgs.cpoc.jobgenerator.config.L0SegmentAppProperties;
import esa.s1pdgs.cpoc.jobgenerator.config.ProcessSettings;
import esa.s1pdgs.cpoc.jobgenerator.status.AppStatus;
import esa.s1pdgs.cpoc.jobgenerator.tasks.AbstractGenericConsumer;
import esa.s1pdgs.cpoc.jobgenerator.tasks.AbstractJobsDispatcher;
import esa.s1pdgs.cpoc.mqi.client.GenericMqiClient;
import esa.s1pdgs.cpoc.mqi.client.StatusService;
import esa.s1pdgs.cpoc.mqi.model.queue.ProductDto;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;
import esa.s1pdgs.cpoc.report.LoggerReporting;
import esa.s1pdgs.cpoc.report.Reporting;

@Component
@ConditionalOnProperty(name = "process.level", havingValue = "L0_SEGMENT")

public class L0SegmentAppConsumer
        extends AbstractGenericConsumer<ProductDto> {

    /**
     * Pattern built from the regular expression given in configuration
     */
    private final Pattern pattern;
    private final Map<String, Integer> patternGroups;

    /**
     * Constructor
     * 
     * @param jobsDispatcher
     * @param patternSettings
     * @param processSettings
     * @param mqiService
     * @param appDataService
     * @param appStatus
     */
    @Autowired
    public L0SegmentAppConsumer(
            final AbstractJobsDispatcher<ProductDto> jobsDispatcher,
            final L0SegmentAppProperties appProperties,
            final ProcessSettings processSettings,
            final GenericMqiClient mqiService,
            final StatusService mqiStatusService,
            final AppCatalogJobClient appDataService,
            final ErrorRepoAppender errorRepoAppender,
            final AppStatus appStatus) {
        super(jobsDispatcher, processSettings, mqiService, mqiStatusService,
                appDataService, appStatus, errorRepoAppender, ProductCategory.LEVEL_SEGMENTS);
        this.pattern = Pattern.compile(appProperties.getNameRegexpPattern(),
                Pattern.CASE_INSENSITIVE);
        this.patternGroups = appProperties.getNameRegexpGroups();
    }

    /**
     * Periodic function for processing messages
     */
    @Scheduled(fixedDelayString = "${process.fixed-delay-ms}", initialDelayString = "${process.initial-delay-ms}")
    public void consumeMessages() {    	
    	final Reporting.Factory reportingFactory = new LoggerReporting.Factory(LOGGER, "L0_SEGMENTJobGeneration"); 
    	
        // First, consume message
        GenericMessageDto<ProductDto> mqiMessage = readMessage();
        if (mqiMessage == null || mqiMessage.getBody() == null) {
            LOGGER.trace("[MONITOR] [step 0] No message received: continue");
            return;
        }
        final Reporting reporting = reportingFactory.newReporting(0);
        
        // process message
        appStatus.setProcessing(mqiMessage.getIdentifier());
        int step = 1;
        boolean ackOk = false;
        String errorMessage = "";
        String productName = mqiMessage.getBody().getProductName();
        
        final FailedProcessingDto<GenericMessageDto<ProductDto>> failedProc =  
        		new FailedProcessingDto<GenericMessageDto<ProductDto>>();
        
        
        // Note: the report log of consume and global log is raised during
        // building job to get the datatake identifier which is the real
        // product name

        try {

            // Check if a job is already created for message identifier
            LOGGER.info(
                    "[MONITOR] [step 1] [productName {}] Creating/updating job",
                    productName);
            reporting.reportStart("Start job generation using " + mqiMessage.getBody().getProductName());
            AppDataJobDto appDataJob = buildJob(mqiMessage);
            productName = appDataJob.getProduct().getProductName();

            // Dispatch job
            step++;
            LOGGER.info(
                    "[MONITOR] [step 2] [productName {}] Dispatching product",
                    productName);
            if (appDataJob.getState() == AppDataJobDtoState.WAITING
                    || appDataJob
                            .getState() == AppDataJobDtoState.DISPATCHING) {
                appDataJob.setState(AppDataJobDtoState.DISPATCHING);
                appDataJob = appDataService.patchJob(appDataJob.getIdentifier(),
                        appDataJob, false, false, false);
                jobsDispatcher.dispatch(appDataJob);
            } else {
                LOGGER.info(
                        "[MONITOR] [step 2] [productName {}] Job for datatake already dispatched",
                        productName);
            }

            // Ack
            step++;
            ackOk = true;

        } catch (AbstractCodedException ace) {
            ackOk = false;
            errorMessage = String.format(
                    "[MONITOR] [step %d] [productName %s] [code %d] %s", step,
                    productName, ace.getCode().getCode(),
                    ace.getLogMessage());
            reporting.reportError("[code {}] {}", ace.getCode().getCode(), ace.getLogMessage());

            failedProc.processingType(mqiMessage.getInputKey())
      			.topic(mqiMessage.getInputKey())
	    		.processingStatus(MessageState.READ)
	    		.productCategory(ProductCategory.LEVEL_SEGMENTS)
	    		.failedPod(processSettings.getHostname())
	            .failureDate(new Date())
	    		.failureMessage(errorMessage)
	    		.processingDetails(mqiMessage);
        }

        // Ack and check if application shall stopped
        ackProcessing(mqiMessage, failedProc, ackOk, productName, errorMessage);

        LOGGER.info("[MONITOR] [step 0] [productName {}] End",
                productName);
        
        reporting.reportStop("End job generation using " + mqiMessage.getBody().getProductName());
    }

    protected AppDataJobDto buildJob(
            GenericMessageDto<ProductDto> mqiMessage)
            throws AbstractCodedException {
        ProductDto leveldto = mqiMessage.getBody();

        // Check if a job is already created for message identifier
        List<AppDataJobDto> existingJobs = appDataService
                .findByMessagesIdentifier(mqiMessage.getIdentifier());

        if (CollectionUtils.isEmpty(existingJobs)) {

            // Extract information from name
            Matcher m = pattern.matcher(leveldto.getProductName());
            if (!m.matches()) {
                throw new InvalidFormatProduct(
                        "Don't match with regular expression "
                                + this.pattern.pattern());
            }
            String satelliteId = m.group(this.patternGroups.get("satelliteId"));
            String missionId = m.group(this.patternGroups.get("missionId"));
            String acquisition = m.group(this.patternGroups.get("acquisition"));
            String datatakeID = m.group(this.patternGroups.get("datatakeId"));

            // Search job for given datatake id
            List<AppDataJobDto> existingJobsForDatatake =
                    appDataService.findByProductDataTakeId(datatakeID);

            if (CollectionUtils.isEmpty(existingJobsForDatatake)) {

                // Create the JOB
                AppDataJobDto jobDto = new AppDataJobDto();
                // General details
                jobDto.setLevel(processSettings.getLevel());
                jobDto.setPod(processSettings.getHostname());
                // Messages
                jobDto.getMessages().add(mqiMessage);
                // Product
                AppDataJobProductDto productDto = new AppDataJobProductDto();
                productDto.setAcquisition(acquisition);
                productDto.setMissionId(missionId);
                productDto.setDataTakeId(datatakeID);
                productDto.setProductName("l0_segments_for_" + datatakeID);
                productDto.setProcessMode(leveldto.getMode());
                productDto.setSatelliteId(satelliteId);
                jobDto.setProduct(productDto);

                return appDataService.newJob(jobDto);
            } else {
                AppDataJobDto jobDto = existingJobsForDatatake.get(0);

                if (!jobDto.getPod().equals(processSettings.getHostname())) {
                    jobDto.setPod(processSettings.getHostname());
                }
                jobDto.getMessages().add(mqiMessage);
                return appDataService.patchJob(jobDto.getIdentifier(), jobDto,
                        true, false, false);

            }

        } else {
            // Update pod if needed
            AppDataJobDto jobDto = existingJobs.get(0);

            if (!jobDto.getPod().equals(processSettings.getHostname())) {
                jobDto.setPod(processSettings.getHostname());
                jobDto = appDataService.patchJob(jobDto.getIdentifier(), jobDto,
                        false, false, false);
            }
            // Job already exists
            return jobDto;
        }
    }

    @Override
    protected String getTaskForFunctionalLog() {
        return "L0_SEGMENTJobGeneration";
    }
}
