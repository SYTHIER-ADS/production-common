package esa.s1pdgs.cpoc.wrapper.status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.wrapper.status.AppStatus.WrapperStatus;
import esa.s1pdgs.cpoc.wrapper.status.dto.WrapperStatusDto;

/**
 * @author Olivier Bex-Chauvet
 */
@RestController
@RequestMapping(path = "/wrapper")
public class WrapperRestController {

    /**
     * Logger
     */
    protected static final Logger LOGGER =
            LogManager.getLogger(WrapperRestController.class);

    /**
     * Application status
     */
    private final AppStatus appStatus;

    /**
     * Constructor
     * 
     * @param appStatus
     */
    @Autowired
    public WrapperRestController(final AppStatus appStatus) {
        this.appStatus = appStatus;
    }

    /**
     * Get application status
     * 
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, path = "/status")
    public ResponseEntity<WrapperStatusDto> getStatusRest() {
        WrapperStatus currentStatus = appStatus.getStatus();
        long currentTimestamp = System.currentTimeMillis();
        long timeSinceLastChange =
                currentTimestamp - currentStatus.getDateLastChangeMs();
        WrapperStatusDto wrapperStatus =
                new WrapperStatusDto(currentStatus.getState(),
                        timeSinceLastChange, currentStatus.getErrorCounter());
        if (currentStatus.isFatalError()) {
            return new ResponseEntity<WrapperStatusDto>(wrapperStatus,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<WrapperStatusDto>(wrapperStatus,
                HttpStatus.OK);
    }

    /**
     * Stop application
     * 
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, path = "/stop")
    public ResponseEntity<String> postStop() {
        LOGGER.info(
                "[MONITOR] L1 Wrapper is scheduled to stop after the end of current process");
        appStatus.setStopping();
        return new ResponseEntity<String>(
                "L1 Wrapper is scheduled to stop after the end of current process",
                HttpStatus.OK);
    }

    /**
     * Stop application
     * 
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, path = "/{category}/process/{messageId}")
    public ResponseEntity<Boolean> isProcessing(
            @PathVariable(name = "category") final String category,
            @PathVariable(name = "messageId") final long messageId) {
        boolean ret = false;
        if (ProductCategory.LEVEL_JOBS.name().toLowerCase().equals(category)) {
            if (appStatus.getProcessingMsgId() == messageId) {
                ret = true;
            }
        } else {
            LOGGER.warn(
                    "[category {}] [messageId {}] Ask for message processing on a not manageable category",
                    category, messageId);
        }
        return new ResponseEntity<Boolean>(ret, HttpStatus.OK);
    }

}
