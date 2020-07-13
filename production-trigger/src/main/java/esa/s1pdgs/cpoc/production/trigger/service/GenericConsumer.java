package esa.s1pdgs.cpoc.production.trigger.service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import esa.s1pdgs.cpoc.appcatalog.AppDataJob;
import esa.s1pdgs.cpoc.appstatus.AppStatus;
import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.utils.LogUtils;
import esa.s1pdgs.cpoc.errorrepo.ErrorRepoAppender;
import esa.s1pdgs.cpoc.errorrepo.model.rest.FailedProcessingDto;
import esa.s1pdgs.cpoc.metadata.client.MetadataClient;
import esa.s1pdgs.cpoc.mqi.client.GenericMqiClient;
import esa.s1pdgs.cpoc.mqi.client.MessageFilter;
import esa.s1pdgs.cpoc.mqi.client.MqiConsumer;
import esa.s1pdgs.cpoc.mqi.client.MqiListener;
import esa.s1pdgs.cpoc.mqi.model.queue.CatalogEvent;
import esa.s1pdgs.cpoc.mqi.model.queue.IpfPreparationJob;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericPublicationMessageDto;
import esa.s1pdgs.cpoc.production.trigger.config.ProcessSettings;
import esa.s1pdgs.cpoc.production.trigger.consumption.ProductTypeConsumptionHandler;
import esa.s1pdgs.cpoc.production.trigger.report.DispatchReportInput;
import esa.s1pdgs.cpoc.production.trigger.report.SeaCoverageCheckReportingOutput;
import esa.s1pdgs.cpoc.report.Reporting;
import esa.s1pdgs.cpoc.report.ReportingFactory;
import esa.s1pdgs.cpoc.report.ReportingMessage;
import esa.s1pdgs.cpoc.report.ReportingUtils;

public class GenericConsumer implements MqiListener<CatalogEvent> {
    static final Logger LOGGER = LogManager.getLogger(GenericConsumer.class);
    
    private final ProcessSettings processSettings;
    private final GenericMqiClient mqiClient;
    private final List<MessageFilter> messageFilter;
    private final AppStatus appStatus;    
    private final ErrorRepoAppender errorRepoAppender;    
    private final Pattern seaCoverageCheckPattern;    
    private final MetadataClient metadataClient;
    private final ProductTypeConsumptionHandler consumptionHandler;
    
    public GenericConsumer(
            final ProcessSettings processSettings,
            final GenericMqiClient mqiService,
            final List<MessageFilter> messageFilter,
            final AppStatus appStatus,
            final ErrorRepoAppender errorRepoAppender,
            final MetadataClient metadataClient,
            final ProductTypeConsumptionHandler consumptionHandler
    ) {
        this.processSettings = processSettings;
        this.mqiClient = mqiService;
        this.messageFilter = messageFilter;
        this.appStatus = appStatus;
        this.errorRepoAppender = errorRepoAppender;
		this.seaCoverageCheckPattern = Pattern.compile(processSettings.getSeaCoverageCheckPattern());
		this.metadataClient = metadataClient;
		this.consumptionHandler = consumptionHandler;
    }

	@PostConstruct
	public void initService() {
		appStatus.setWaiting();
		if (processSettings.getFixedDelayMs() > 0) {
			final ExecutorService service = Executors.newFixedThreadPool(1);
			service.execute(new MqiConsumer<CatalogEvent>(
	    			mqiClient, 
	    			ProductCategory.CATALOG_EVENT, 
	    			this,
	    			messageFilter,
	    			processSettings.getFixedDelayMs(),
					processSettings.getInitialDelayMs(), 
					appStatus
			));
		}
	} 

    @Override
    public final void onMessage(final GenericMessageDto<CatalogEvent> mqiMessage) throws Exception {
        final CatalogEvent event = mqiMessage.getBody();
        final String productName = event.getProductName();
        
        final Reporting reporting = ReportingUtils.newReportingBuilder()
        		.predecessor(event.getUid())
        		.newReporting("ProductionTrigger");
        
        LOGGER.debug("Handling consumption of product {}", productName);
        try {
            reporting.begin(
            		ReportingUtils.newFilenameReportingInputFor(event.getProductFamily(), productName),
            		new ReportingMessage("Received CatalogEvent for %s", productName)
            );  
            if (!isOverLand(event, reporting)) {                      
                final AppDataJob job = new AppDataJob();
                job.setLevel(processSettings.getLevel());
                job.setPod(processSettings.getHostname());
                job.getMessages().add(mqiMessage);
                job.setProduct(consumptionHandler.newProductFor(mqiMessage));
                LOGGER.info("Dispatching {} product {}", consumptionHandler.type(), productName);
                dispatch(mqiMessage, job, productName, consumptionHandler.type(), reporting);
                reporting.end(new ReportingMessage("IpfPreparationJob for product %s created", productName));  
            }
            else {
            	LOGGER.debug("CatalogEvent for {} is ignored", productName);        
                reporting.end(new ReportingMessage("Product %s is not over sea, skipping", productName)); 
            }
        } catch (final Exception e) {        	
        	reporting.error(new ReportingMessage("Error on handling CatalogEvent: %s", LogUtils.toString(e)));
        	throw e;
        }
        LOGGER.debug("Done handling consumption of product {}", productName);
    }       

	@Override
	public final void onTerminalError(final GenericMessageDto<CatalogEvent> message, final Exception error) {
        LOGGER.error(error);
        errorRepoAppender.send(
        	new FailedProcessingDto(processSettings.getHostname(), new Date(), error.getMessage(), message)
        );
	}
	
	private final void dispatch(
			final GenericMessageDto<CatalogEvent> mqiMessage,
			final AppDataJob appDataJob,
			final String productName,
			final String type,
			final ReportingFactory reportingFactory
	) throws AbstractCodedException {
        final CatalogEvent event = mqiMessage.getBody();
		
        final Reporting reporting = reportingFactory.newReporting("Dispatch");
        
        reporting.begin(
        		DispatchReportInput.newInstance(appDataJob.getId(), productName, type),
        		new ReportingMessage("Dispatching AppDataJob %s for %s %s", appDataJob.getId(), type, productName)
        );     
        try {           
        	final IpfPreparationJob job = new IpfPreparationJob();
        	job.setProductFamily(event.getProductFamily());
        	job.setAppDataJob(appDataJob);
        	job.setUid(reporting.getUid());
        	
        	final GenericPublicationMessageDto<IpfPreparationJob> messageDto = new GenericPublicationMessageDto<IpfPreparationJob>(
        			mqiMessage.getId(), 
        			event.getProductFamily(), 
        			job
        	);
        	messageDto.setInputKey(mqiMessage.getInputKey());
        	messageDto.setOutputKey(event.getProductFamily().name());
        	mqiClient.publish(messageDto, ProductCategory.PREPARATION_JOBS);  
			reporting.end(
					new ReportingMessage("AppDataJob %s for %s %s dispatched", appDataJob.getId(), type, productName)
			);
		} catch (final AbstractCodedException e) {
			reporting.error(new ReportingMessage(
					"Error on dispatching AppDataJob %s for %s %s: %s", 
					appDataJob.getId(), 
					type, 
					productName,
					LogUtils.toString(e)
			));
			throw e;
		}
	}	

	private final boolean isOverLand(final CatalogEvent event, final ReportingFactory reporting) throws Exception {
        final String productName = event.getProductName();
        final ProductFamily family = event.getProductFamily();
		
        // S1PRO-483: check for matching products if they are over sea. If not, simply skip the
        // production
		final Reporting seaReport = reporting.newReporting("SeaCoverageCheck");
        try {
			if (seaCoverageCheckPattern.matcher(productName).matches()) {   
				seaReport.begin(
						ReportingUtils.newFilenameReportingInputFor(family, productName),
						new ReportingMessage("Checking sea coverage")
				);	
				if (metadataClient.getSeaCoverage(family, productName) <= processSettings.getMinSeaCoveragePercentage()) {
					seaReport.end(
							new SeaCoverageCheckReportingOutput(false), 
							new ReportingMessage("Product %s is not over sea", productName)
					);
					LOGGER.warn("Skipping job generation for product {} because it is not over sea", productName);
			        return true;
			    }
				else {
					seaReport.end(
							new SeaCoverageCheckReportingOutput(true), 
							new ReportingMessage("Product %s is over sea", productName)
					);
				}
			}
		} catch (final Exception e) {
			seaReport.error(new ReportingMessage("SeaCoverage check failed: %s", LogUtils.toString(e)));
			throw e;			
		}        
        return false;
	}    
}
