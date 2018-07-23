package fr.viveris.s1pdgs.jobgenerator.service.mqi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.mqi.client.ErrorService;
import esa.s1pdgs.cpoc.mqi.client.GenericMqiService;
import esa.s1pdgs.cpoc.mqi.model.queue.LevelJobDto;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericPublicationMessageDto;

/**
 * Service for publishing in KAFKA topics
 * 
 * @author Viveris Technologies
 */
@Service
public class OutputProcuderFactory {

    /**
     * Logger
     */
    protected static final Logger LOGGER = LogManager.getLogger(OutputProcuderFactory.class);

    /**
     * MQI client for LEVEL_JOBS
     */
    private final GenericMqiService<LevelJobDto> senderJobs;

    /**
     * MQI client for errors
     */
    private final ErrorService senderErrors;

    /**
     * Constructor
     * 
     * @param senderProducts
     * @param senderReports
     */
    @Autowired
    public OutputProcuderFactory(
            @Qualifier("mqiServiceForLevelJobs") final GenericMqiService<LevelJobDto> senderJobs,
            @Qualifier("mqiServiceForErrors") final ErrorService senderErrors) {
        this.senderJobs = senderJobs;
        this.senderErrors = senderErrors;
    }

    /**
     * Send an output in right topic according its family
     * 
     * @param msg
     * @throws AbstractCodedException
     */
    public void sendJob(GenericMessageDto<?> genericMessageDto, LevelJobDto dto)
            throws AbstractCodedException {
        senderJobs.publish(new GenericPublicationMessageDto<LevelJobDto>(
                genericMessageDto.getIdentifier(), dto.getFamily(), dto));
    }
    
    /**
     * Publish a error
     * @param message
     */
    public void sendError(final String message) {
        try {
            senderErrors.publish(message);
        } catch (AbstractCodedException e) {
            LOGGER.error(e.getLogMessage());
        }
    }
}
