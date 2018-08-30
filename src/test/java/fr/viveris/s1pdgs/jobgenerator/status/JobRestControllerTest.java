package fr.viveris.s1pdgs.jobgenerator.status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import esa.s1pdgs.cpoc.common.AppState;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.mqi.client.StatusService;
import fr.viveris.s1pdgs.jobgenerator.status.AppStatus.JobStatus;
import fr.viveris.s1pdgs.jobgenerator.status.dto.JobStatusDto;
import fr.viveris.s1pdgs.jobgenerator.status.RestControllerTest;


public class JobRestControllerTest extends RestControllerTest {

    /**
     * Application status
     */
    @Mock
    protected AppStatus appStatus;

    /**
     * MQI service for stopping the MQI
     */
    @Mock
    private StatusService mqiStatusService;

    /**
     * Controller to test
     */
    private JobRestController controller;

    /**
     * Initialization
     * 
     * @throws IOException
     * @throws AbstractCodedException
     */
    @Before
    public void init() throws IOException, AbstractCodedException {
        MockitoAnnotations.initMocks(this);

        doNothing().when(mqiStatusService).stop();

        controller = new JobRestController(appStatus);

        initMockMvc(this.controller);
    }

    /**
     * Test stop application
     * 
     * @throws Exception
     */
    @Test
    public void testUrlStopApp() throws Exception {
        doNothing().when(appStatus).setStopping();
        request(post("/app/stop"))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        verify(appStatus, times(1)).setStopping();
    }

    /**
     * Test get status when fatal error
     * 
     * @throws Exception
     */
    @Test
    public void testUrlStatusWhenFatalError() throws Exception {
        JobStatus status = (new AppStatus(3, 30, mqiStatusService)).getStatus();
        status.setFatalError();
        doReturn(status).when(appStatus).getStatus();

        request(get("/app/status"))
                .andExpect(
                        MockMvcResultMatchers.status().isInternalServerError())
                .andReturn();

    }

    /**
     * Test get status when fatal error
     * 
     * @throws Exception
     */
    @Test
    public void testStatusWhenFatalError() throws Exception {
        JobStatus status = (new AppStatus(3, 30, mqiStatusService)).getStatus();
        status.setFatalError();
        doReturn(status).when(appStatus).getStatus();

        long diffTmBefore =
                System.currentTimeMillis() - status.getDateLastChangeMs();
        ResponseEntity<JobStatusDto> result = controller.getStatusRest();

        assertNotNull(result.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());

        long diffTmAfter =
                System.currentTimeMillis() - status.getDateLastChangeMs();
        verify(appStatus, times(1)).getStatus();
        assertEquals(AppState.FATALERROR, result.getBody().getStatus());
        assertEquals(0, result.getBody().getErrorCounter());
        assertTrue(result.getBody().getTimeSinceLastChange() >= diffTmBefore);
        assertTrue(diffTmAfter >= result.getBody().getTimeSinceLastChange());

    }

    /**
     * Test get status when fatal error
     * 
     * @throws Exception
     */
    @Test
    public void testUrlStatusWhenError() throws Exception {
        JobStatus status = (new AppStatus(3, 30, mqiStatusService)).getStatus();
        status.setErrorCounterProcessing(3);
        status.setErrorCounterProcessing(3);
        doReturn(status).when(appStatus).getStatus();

        request(get("/app/status"))
                .andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

    }

    /**
     * Test get status when error
     * 
     * @throws Exception
     */
    @Test
    public void testStatusWhenError() throws Exception {
        JobStatus status = (new AppStatus(3, 30, mqiStatusService)).getStatus();
        status.setErrorCounterProcessing(3);
        status.setErrorCounterProcessing(3);
        doReturn(status).when(appStatus).getStatus();

        long diffTmBefore =
                System.currentTimeMillis() - status.getDateLastChangeMs();
        ResponseEntity<JobStatusDto> result = controller.getStatusRest();

        assertNotNull(result.getBody());
        assertEquals(HttpStatus.OK, result.getStatusCode());

        long diffTmAfter =
                System.currentTimeMillis() - status.getDateLastChangeMs();
        verify(appStatus, times(1)).getStatus();
        assertEquals(AppState.ERROR, result.getBody().getStatus());
        assertEquals(2, result.getBody().getErrorCounter());
        assertTrue(result.getBody().getTimeSinceLastChange() >= diffTmBefore);
        assertTrue(diffTmAfter >= result.getBody().getTimeSinceLastChange());
    }

    /**
     * Test get status when fatal error
     * 
     * @throws Exception
     */
    @Test
    public void testIsProcessing() throws Exception {
        doReturn(1234L).when(appStatus).getProcessingMsgId();
        request(get("/app/edrs_sessions/process/1234"))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content().string("true"));
        request(get("/app/level_products/process/1234"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("true"));
        request(get("/app/level_jobs/process/1234"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("false"));
        request(get("/app/level_products/process/1235"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("false"));
        doReturn(0L).when(appStatus).getProcessingMsgId();
        request(get("/app/level_products/process/1235"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("false"));

    }

}
