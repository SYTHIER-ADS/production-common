package esa.s1pdgs.cpoc.jobgenerator.tasks.l0segmentapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import esa.s1pdgs.cpoc.appcatalog.client.job.AppCatalogJobClient;
import esa.s1pdgs.cpoc.appcatalog.common.rest.model.job.AppDataJobDto;
import esa.s1pdgs.cpoc.appcatalog.common.rest.model.job.AppDataJobGenerationDtoState;
import esa.s1pdgs.cpoc.common.ApplicationLevel;
import esa.s1pdgs.cpoc.common.ApplicationMode;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.errors.InternalErrorException;
import esa.s1pdgs.cpoc.common.errors.processing.JobGenMissingRoutingEntryException;
import esa.s1pdgs.cpoc.jobgenerator.config.JobGeneratorSettings;
import esa.s1pdgs.cpoc.jobgenerator.config.ProcessSettings;
import esa.s1pdgs.cpoc.jobgenerator.tasks.JobsGeneratorFactory;
import esa.s1pdgs.cpoc.jobgenerator.utils.TestL0SegmentUtils;
import esa.s1pdgs.cpoc.mqi.model.queue.ProductDto;

/**
 * Test the class JobDispatcher
 * 
 * @author Cyrielle Gailliard
 */
public class L0SegmentAppJobDispatcherTest {

    /**
     * Job generator factory
     */
    @Mock
    private JobsGeneratorFactory jobsGeneratorFactory;

    /**
     * Job generator settings
     */
    @Mock
    private JobGeneratorSettings jobGeneratorSettings;

    @Mock
    private ProcessSettings processSettings;

    /**
     * Job generator task scheduler
     */
    @Mock
    private ThreadPoolTaskScheduler jobGenerationTaskScheduler;

    @Mock
    private L0SegmentAppJobDispatcher mockGenerator;

    @Mock
    private AppCatalogJobClient appDataService;

    /**
     * Test set up
     * 
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // Mock process settings
        this.mockJobGeneratorSettings();
        this.mockProcessSettings();

        // Mock app catalog service
        this.mockAppDataService();

        // Mcok
        doAnswer(i -> {
            return mockGenerator;
        }).when(jobsGeneratorFactory).createJobGeneratorForEdrsSession(any(),
                any());
        doAnswer(i -> {
            return null;
        }).when(jobGenerationTaskScheduler).scheduleAtFixedRate(any(), any());
    }

    /**
     * Construct a dispatcher
     * 
     * @return
     */
    private L0SegmentAppJobDispatcher createSessionDispatcher() {
        return new L0SegmentAppJobDispatcher(jobGeneratorSettings,
                processSettings, jobsGeneratorFactory,
                jobGenerationTaskScheduler, appDataService);
    }

    /**
     * Mock the JobGeneratorSettings
     */
    private void mockJobGeneratorSettings() {
        // Mock the job generator settings
        doAnswer(i -> {
            return "./test/data/l0_segment_config/task_tables/";
        }).when(jobGeneratorSettings).getDiroftasktables();
        doAnswer(i -> {
            return 4;
        }).when(jobGeneratorSettings).getMaxnboftasktable();
        doAnswer(i -> {
            return 2000;
        }).when(jobGeneratorSettings).getJobgenfixedrate();
    }

    private void mockProcessSettings() {
        Mockito.doAnswer(i -> {
            Map<String, String> r = new HashMap<String, String>(2);
            return r;
        }).when(processSettings).getParams();
        Mockito.doAnswer(i -> {
            Map<String, String> r = new HashMap<String, String>(5);
            r.put("SM_RAW__0S", "^S1[A-B]_S[1-6]_RAW__0S.*$");
            r.put("AN_RAW__0S", "^S1[A-B]_N[1-6]_RAW__0S.*$");
            r.put("ZS_RAW__0S", "^S1[A-B]_N[1-6]_RAW__0S.*$");
            r.put("REP_L0PSA_", "^S1[A|B|_]_OPER_REP_ACQ.*$");
            r.put("REP_EFEP_", "^S1[A|B|_]_OPER_REP_PASS.*.EOF$");
            return r;
        }).when(processSettings).getOutputregexps();
        Mockito.doAnswer(i -> {
            return ApplicationLevel.L0;
        }).when(processSettings).getLevel();
        Mockito.doAnswer(i -> {
            return "hostname";
        }).when(processSettings).getHostname();
        Mockito.doAnswer(i -> {
            return ApplicationMode.TEST;
        }).when(processSettings).getMode();
    }

    private void mockAppDataService()
            throws InternalErrorException, AbstractCodedException {
        doReturn(Arrays.asList(TestL0SegmentUtils.buildAppData()))
                .when(appDataService)
                .findNByPodAndGenerationTaskTableWithNotSentGeneration(
                        Mockito.anyString(), Mockito.anyString());
        AppDataJobDto<ProductDto> primaryCheckAppJob =
                TestL0SegmentUtils.buildAppData();
        primaryCheckAppJob.getGenerations().get(0)
                .setState(AppDataJobGenerationDtoState.PRIMARY_CHECK);
        AppDataJobDto<ProductDto> readyAppJob =
                TestL0SegmentUtils.buildAppData();
        readyAppJob.getGenerations().get(0)
                .setState(AppDataJobGenerationDtoState.READY);
        AppDataJobDto<ProductDto> sentAppJob =
                TestL0SegmentUtils.buildAppData();
        sentAppJob.getGenerations().get(0)
                .setState(AppDataJobGenerationDtoState.SENT);
        doReturn(TestL0SegmentUtils.buildAppData()).when(appDataService)
                .patchJob(Mockito.eq(123L), Mockito.any(), Mockito.anyBoolean(),
                        Mockito.anyBoolean(), Mockito.anyBoolean());
        doReturn(primaryCheckAppJob).when(appDataService).patchTaskTableOfJob(
                Mockito.eq(123L), Mockito.eq("TaskTable.L0ASP.xml"),
                Mockito.eq(AppDataJobGenerationDtoState.PRIMARY_CHECK));
        doReturn(readyAppJob).when(appDataService).patchTaskTableOfJob(
                Mockito.eq(123L), Mockito.eq("TaskTable.L0ASP.xml"),
                Mockito.eq(AppDataJobGenerationDtoState.READY));
        doReturn(sentAppJob).when(appDataService).patchTaskTableOfJob(
                Mockito.eq(123L), Mockito.eq("TaskTable.L0ASP.xml"),
                Mockito.eq(AppDataJobGenerationDtoState.SENT));
    }

    @Test
    public void testCreate() {
        File taskTable1 = new File(
                "./test/data/l0_segment/config/task_tables/TaskTable.L0ASP.xml");

        // Initialize
        L0SegmentAppJobDispatcher dispatcher = this.createSessionDispatcher();
        try {
            dispatcher.createJobGenerator(taskTable1);
            verify(jobsGeneratorFactory, times(1))
                    .createJobGeneratorForL0Segment(any(), any());
            verify(jobsGeneratorFactory, times(1))
                    .createJobGeneratorForL0Segment(eq(taskTable1), any());
        } catch (AbstractCodedException e) {
            fail("Invalid raised exception: " + e.getMessage());
        }
    }

    /**
     * Test the initialize function
     */
    @Test
    public void testInitialize() {
        File taskTable1 = new File(
                "./test/data/l0_segment_config/task_tables/TaskTable.L0ASP.xml");

        // Intitialize
        L0SegmentAppJobDispatcher dispatcher = this.createSessionDispatcher();
        try {
            dispatcher.initialize();
            verify(jobGenerationTaskScheduler, times(1))
                    .scheduleWithFixedDelay(any(), anyLong());
            verify(jobGenerationTaskScheduler, times(1))
                    .scheduleWithFixedDelay(any(), eq(2000L));
            verify(jobsGeneratorFactory, times(1))
                    .createJobGeneratorForL0Segment(any(), any());
            verify(jobsGeneratorFactory, times(1))
                    .createJobGeneratorForL0Segment(eq(taskTable1), any());

            assertTrue(dispatcher.getGenerators().size() == 1);
            assertTrue(dispatcher.getGenerators()
                    .containsKey(taskTable1.getName()));
        } catch (AbstractCodedException e) {
            fail("Invalid raised exception: " + e.getMessage());
        }
    }

    /**
     * Test dispatch
     * @throws JobGenMissingRoutingEntryException 
     */
    @Test
    public void testGetTaskTable() throws JobGenMissingRoutingEntryException {

        AppDataJobDto<ProductDto> appData =
                TestL0SegmentUtils.buildAppData();

        // Init dispatcher
        L0SegmentAppJobDispatcher dispatcher = this.createSessionDispatcher();
        try {
            dispatcher.initialize();
        } catch (AbstractCodedException e) {
            fail("Invalid raised exception: " + e.getMessage());
        }

        // Dispatch
        assertEquals(1, dispatcher.getTaskTables(appData).size());
    }
}