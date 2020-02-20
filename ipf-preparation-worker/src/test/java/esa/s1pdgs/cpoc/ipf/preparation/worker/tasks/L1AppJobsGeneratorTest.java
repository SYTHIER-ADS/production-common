package esa.s1pdgs.cpoc.ipf.preparation.worker.tasks;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import esa.s1pdgs.cpoc.appcatalog.AppDataJob;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobGenerationState;
import esa.s1pdgs.cpoc.appcatalog.client.job.AppCatalogJobClient;
import esa.s1pdgs.cpoc.common.ApplicationLevel;
import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.errors.InternalErrorException;
import esa.s1pdgs.cpoc.common.errors.processing.IpfPrepWorkerInputsMissingException;
import esa.s1pdgs.cpoc.common.errors.processing.IpfPrepWorkerMetadataException;
import esa.s1pdgs.cpoc.common.errors.processing.MetadataQueryException;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.AiopProperties;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.AppConfig;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.IpfPreparationWorkerSettings;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.IpfPreparationWorkerSettings.WaitTempo;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.ProcessConfiguration;
import esa.s1pdgs.cpoc.ipf.preparation.worker.config.ProcessSettings;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.JobGeneration;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.joborder.AbstractJobOrderConf;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.joborder.JobOrder;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.joborder.JobOrderProcParam;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.joborder.L0JobOrderConf;
import esa.s1pdgs.cpoc.ipf.preparation.worker.model.tasktable.TaskTable;
import esa.s1pdgs.cpoc.ipf.preparation.worker.service.XmlConverter;
import esa.s1pdgs.cpoc.metadata.client.MetadataClient;
import esa.s1pdgs.cpoc.metadata.client.SearchMetadataQuery;
import esa.s1pdgs.cpoc.metadata.model.L0AcnMetadata;
import esa.s1pdgs.cpoc.metadata.model.L0SliceMetadata;
import esa.s1pdgs.cpoc.metadata.model.SearchMetadata;
import esa.s1pdgs.cpoc.mqi.client.MqiClient;
import esa.s1pdgs.cpoc.mqi.model.queue.IpfExecutionJob;

public class L1AppJobsGeneratorTest {

    /**
     * For testing exceptions
     */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private XmlConverter xmlConverter;

    @Mock
    private MetadataClient metadataClient;

    @Mock
    private ProcessSettings processSettings;

    @Mock
    private IpfPreparationWorkerSettings ipfPreparationWorkerSettings;

    @Mock
    private AiopProperties aiopProperties;
    
    @Mock
    private MqiClient mqiClient;

    @Mock
    private ProcessConfiguration processConfiguration;
    
    private TaskTable expectedTaskTable;

    private LevelProductsJobsGenerator generator;

    @Mock
    private AppCatalogJobClient appDataPService;

    private JobGeneration jobA;

    private AppDataJob appDataJob;

    private AppDataJob appDataJobComplete;

    private IpfExecutionJob publishedJob;

    /**
     * Test set up
     * 
     * @throws Exception
     */
    @Before
    public void init() throws Exception {

        // Retrieve task table from the XML converter
        expectedTaskTable = TestGenericUtils.buildTaskTableIW();

        // Mockito
        MockitoAnnotations.initMocks(this);
        this.mockProcessSettings();
        this.mockJobGeneratorSettings();
        this.mockXmlConverter();
        this.mockMetadataService();
        this.mockKafkaSender();
        this.mockAppDataService();

        final JobsGeneratorFactory factory =
                new JobsGeneratorFactory(processSettings, ipfPreparationWorkerSettings,
                		aiopProperties, xmlConverter, metadataClient, processConfiguration, mqiClient);
        generator = (LevelProductsJobsGenerator) factory.createJobGeneratorForL0Slice(
                new File(
                        "./test/data/generic_config/task_tables/IW_RAW__0_GRDH_1.xml"),
                appDataPService);

        appDataJob = TestL1Utils.buildJobGeneration(false);
        appDataJobComplete = TestL1Utils.buildJobGeneration(true);
        jobA = new JobGeneration(appDataJob, "IW_RAW__0_GRDH_1.xml");
    }

    private void mockProcessSettings() {
        Mockito.doAnswer(i -> {
            final Map<String, String> r = new HashMap<String, String>(2);
            return r;
        }).when(processSettings).getParams();
        Mockito.doAnswer(i -> {
            return ApplicationLevel.L1;
        }).when(processSettings).getLevel();
        doReturn("hostname").when(processSettings).getHostname();
        Mockito.doAnswer(i -> {
            final Map<String, String> r = new HashMap<String, String>(5);
            r.put("SM_RAW__0S", "^S1[A-B]_S[1-6]_RAW__0S.*$");
            r.put("AN_RAW__0S", "^S1[A-B]_N[1-6]_RAW__0S.*$");
            r.put("ZS_RAW__0S", "^S1[A-B]_N[1-6]_RAW__0S.*$");
            r.put("REP_L0PSA_", "^S1[A|B|_]_OPER_REP_ACQ.*$");
            r.put("REP_EFEP_", "^S1[A|B|_]_OPER_REP_PASS.*.EOF$");
            r.put("SM_GRDH_1S", "^S1[A-B]_S[1-6]_GRDH_1S.*$");
            r.put("SM_GRDH_1A", "^S1[A-B]_S[1-6]_GRDH_1A.*$");
            return r;
        }).when(processSettings).getOutputregexps();
    }

    private void mockJobGeneratorSettings() {
        Mockito.doAnswer(i -> {
            final Map<String, ProductFamily> r = new HashMap<>();
            r.put("IW_GRDH_1S", ProductFamily.L1_SLICE);
            r.put("IW_GRDH_1A", ProductFamily.L1_ACN);
            r.put("IW_RAW__0S", ProductFamily.L0_SLICE);
            r.put("IW_RAW__0A", ProductFamily.L0_ACN);
            r.put("IW_RAW__0C", ProductFamily.L0_ACN);
            r.put("IW_RAW__0N", ProductFamily.L0_ACN);
            return r;
        }).when(ipfPreparationWorkerSettings).getInputfamilies();
        Mockito.doAnswer(i -> {
            final Map<String, ProductFamily> r = new HashMap<>();
            r.put("IW_GRDH_1S", ProductFamily.L1_SLICE);
            r.put("IW_GRDH_1A", ProductFamily.L1_ACN);
            r.put("IW_RAW__0S", ProductFamily.L0_SLICE);
            r.put("IW_RAW__0A", ProductFamily.L0_ACN);
            r.put("IW_RAW__0C", ProductFamily.L0_ACN);
            r.put("IW_RAW__0N", ProductFamily.L0_ACN);
            return r;
        }).when(ipfPreparationWorkerSettings).getOutputfamilies();
        Mockito.doAnswer(i -> {
            return ProductFamily.AUXILIARY_FILE.name();
        }).when(ipfPreparationWorkerSettings).getDefaultfamily();
        Mockito.doAnswer(i -> {
            return 2;
        }).when(ipfPreparationWorkerSettings).getMaxnumberofjobs();
        Mockito.doAnswer(i -> {
            return new WaitTempo(2000, 3);
        }).when(ipfPreparationWorkerSettings).getWaitprimarycheck();
        Mockito.doAnswer(i -> {
            return new WaitTempo(10000, 3);
        }).when(ipfPreparationWorkerSettings).getWaitmetadatainput();
        Mockito.doAnswer(i -> {
            final Map<String, Float> r = new HashMap<>();
            r.put("IW", 7.7F);
            r.put("EW", 8.2F);
            return r;
        }).when(ipfPreparationWorkerSettings).getTypeOverlap();
        Mockito.doAnswer(i -> {
            final Map<String, Float> r = new HashMap<>();
            r.put("IW", 60F);
            r.put("EW", 21F);
            return r;
        }).when(ipfPreparationWorkerSettings).getTypeSliceLength();
    }

    private void mockXmlConverter() {
        try {
            Mockito.when(
                    xmlConverter.convertFromXMLToObject(Mockito.anyString()))
                    .thenReturn(expectedTaskTable);
            Mockito.doAnswer(i -> {
                final AnnotationConfigApplicationContext ctx =
                        new AnnotationConfigApplicationContext();
                ctx.register(AppConfig.class);
                ctx.refresh();
                final XmlConverter xmlConverter = ctx.getBean(XmlConverter.class);
                final String r = xmlConverter
                        .convertFromObjectToXMLString(i.getArgument(0));
                ctx.close();
                return r;
            }).when(xmlConverter).convertFromObjectToXMLString(Mockito.any());
        } catch (IOException | JAXBException e1) {
            fail("BuildTaskTableException raised: " + e1.getMessage());
        }
    }

    private void mockMetadataService() {
        try {
            Mockito.doAnswer(i -> {
                return null;
            }).when(this.metadataClient).getEdrsSession(Mockito.anyString(),
                    Mockito.anyString());
            Mockito.doAnswer(i -> {
                return new L0SliceMetadata(
                        "S1A_IW_RAW__0SDV_20171213T121623_20171213T121656_019684_021735_C6DB.SAFE",
                        "IW_RAW__0S",
                        "S1A_IW_RAW__0SDV_20171213T121623_20171213T121656_019684_021735_C6DB.SAFE",
                        "2017-12-13T12:16:23.224083Z",
                        "2017-12-13T12:16:56.224083Z",
                        "S1",
                        "A",
                        "WILE",
                        6, 3, "021735");
            }).when(this.metadataClient).getL0Slice(Mockito.anyString());
            Mockito.doAnswer(i -> {
                return new L0AcnMetadata(
                        "S1A_IW_RAW__0ADV_20171213T121123_20171213T121947_019684_021735_51B1.SAFE",
                        "IW_RAW__0A",
                        "S1A_IW_RAW__0ADV_20171213T121123_20171213T121947_019684_021735_51B1.SAFE",
                        "2017-12-13T12:16:23.224083Z",
                        "2017-12-13T12:16:56.224083Z",
                        "S1",
                        "A",
                        "WILE", 6, 10, "021735");
            }).when(this.metadataClient).getFirstACN(Mockito.anyString(),
                    Mockito.anyString());
            Mockito.doAnswer(i -> {
                final SearchMetadataQuery query = i.getArgument(0);
                if ("IW_RAW__0S".equalsIgnoreCase(query.getProductType())) {
                    return Arrays.asList(new SearchMetadata(
                            "S1A_IW_RAW__0SDV_20171213T121623_20171213T121656_019684_021735_C6DB.SAFE",
                            "IW_RAW__0S",
                            "S1A_IW_RAW__0SDV_20171213T121623_20171213T121656_019684_021735_C6DB.SAFE",
                            "2017-12-13T12:16:23.123456Z", "2017-12-13T12:16:56.123456Z",
                            "S1",
                            "A",
                            "WILE"));
                } else if ("IW_RAW__0A"
                        .equalsIgnoreCase(query.getProductType())) {
                    return Arrays.asList(new SearchMetadata(
                            "S1A_IW_RAW__0ADV_20171213T121123_20171213T121947_019684_021735_51B1.SAFE",
                            "IW_RAW__0A",
                            "S1A_IW_RAW__0ADV_20171213T121123_20171213T121947_019684_021735_51B1.SAFE",
                            "2017-12-13T12:11:23.123456Z", "2017-12-13T12:19:47.123456Z",
                            "S1",
                            "A",
                            "WILE"));
                } else if ("IW_RAW__0C"
                        .equalsIgnoreCase(query.getProductType())) {
                    return Arrays.asList(new SearchMetadata(
                            "S1A_IW_RAW__0CDV_20171213T121123_20171213T121947_019684_021735_E131.SAFE",
                            "IW_RAW__0C",
                            "S1A_IW_RAW__0CDV_20171213T121123_20171213T121947_019684_021735_E131.SAFE",
                            "2017-12-13T12:11:23.123456Z", "2017-12-13T12:19:47.123456Z",
                            "S1",
                            "A",
                            "WILE"));
                } else if ("IW_RAW__0N"
                        .equalsIgnoreCase(query.getProductType())) {
                    return Arrays.asList(new SearchMetadata(
                            "S1A_IW_RAW__0NDV_20171213T121123_20171213T121947_019684_021735_87D4.SAFE",
                            "IW_RAW__0N",
                            "S1A_IW_RAW__0NDV_20171213T121123_20171213T121947_019684_021735_87D4.SAFE",
                            "2017-12-13T12:11:23.123456Z", "2017-12-13T12:19:47.123456Z",
                            "S1",
                            "A",
                            "WILE"));
                } else if ("AUX_CAL".equalsIgnoreCase(query.getProductType())) {
                    return Arrays.asList(new SearchMetadata(
                            "S1A_AUX_CAL_V20171017T080000_G20171013T101200.SAFE",
                            "AUX_CAL",
                            "S1A_AUX_CAL_V20171017T080000_G20171013T101200.SAFE",
                            "2017-10-17T08:00:00.123456Z", "9999-12-31T23:59:59.123456Z",
                            "S1",
                            "A",
                            "WILE"));
                } else if ("AUX_INS".equalsIgnoreCase(query.getProductType())) {
                    return Arrays.asList(new SearchMetadata(
                            "S1A_AUX_INS_V20171017T080000_G20171013T101216.SAFE",
                            "AUX_INS",
                            "S1A_AUX_INS_V20171017T080000_G20171013T101216.SAFE",
                            "2017-10-17T08:00:00.123456Z", "9999-12-31T23:59:59.123456Z",
                            "S1",
                            "A",
                            "WILE"));
                } else if ("AUX_PP1".equalsIgnoreCase(query.getProductType())) {
                    return Arrays.asList(new SearchMetadata(
                            "S1A_AUX_PP1_V20171017T080000_G20171013T101236.SAFE",
                            "AUX_PP1",
                            "S1A_AUX_PP1_V20171017T080000_G20171013T101236.SAFE",
                            "2017-10-17T08:00:00.123456Z", "9999-12-31T23:59:59.123456Z",
                            "S1",
                            "A",
                            "WILE"));
                } else if ("AUX_RESORB"
                        .equalsIgnoreCase(query.getProductType())) {
                    return Arrays.asList(new SearchMetadata(
                            "S1A_OPER_AUX_RESORB_OPOD_20171213T143838_V20171213T102737_20171213T134507.EOF",
                            "AUX_OBMEMC",
                            "S1A_OPER_AUX_RESORB_OPOD_20171213T143838_V20171213T102737_20171213T134507.EOF",
                            "2017-12-13T10:27:37.123456Z", "2017-12-13T13:45:07.123456Z",
                            "S1",
                            "A",
                            "WILE"));
                }
                return Collections.emptyList();
            }).when(this.metadataClient).search(Mockito.any(), Mockito.any(),
                    Mockito.any(), Mockito.anyString(), Mockito.anyInt(),
                    Mockito.anyString(), Mockito.anyString());
        } catch (final MetadataQueryException e) {
            fail(e.getMessage());
        }
    }

    private void mockKafkaSender() throws AbstractCodedException {
        Mockito.doAnswer(i -> {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(new File("./tmp/inputMessageL1.json"),
                    i.getArgument(0));
            mapper.writeValue(new File("./tmp/jobDtoL1.json"),
                    i.getArgument(1));
            publishedJob = i.getArgument(1);
            return null;
        }).when(mqiClient).publish(Mockito.any(), Mockito.any());
    }

    private void mockAppDataService()
            throws InternalErrorException, AbstractCodedException {
        doReturn(Arrays.asList(TestL1Utils.buildJobGeneration(false)))
                .when(appDataPService)
                .findNByPodAndGenerationTaskTableWithNotSentGeneration(
                        Mockito.anyString(), Mockito.anyString());
        final AppDataJob<?> primaryCheckAppJob =
                TestL1Utils.buildJobGeneration(true);
        primaryCheckAppJob.getGenerations().get(0)
                .setState(AppDataJobGenerationState.PRIMARY_CHECK);
        final AppDataJob<?> readyAppJob =
                TestL1Utils.buildJobGeneration(true);
        readyAppJob.getGenerations().get(0)
                .setState(AppDataJobGenerationState.READY);
        final AppDataJob<?> sentAppJob =
                TestL1Utils.buildJobGeneration(true);
        sentAppJob.getGenerations().get(0)
                .setState(AppDataJobGenerationState.SENT);
        doReturn(TestL1Utils.buildJobGeneration(true)).when(appDataPService)
                .patchJob(Mockito.eq(123L), Mockito.any(), Mockito.anyBoolean(),
                        Mockito.anyBoolean(), Mockito.anyBoolean());

        doReturn(primaryCheckAppJob).when(appDataPService).patchTaskTableOfJob(
                Mockito.eq(123L), Mockito.eq("IW_RAW__0_GRDH_1.xml"),
                Mockito.eq(AppDataJobGenerationState.PRIMARY_CHECK));
        doReturn(readyAppJob).when(appDataPService).patchTaskTableOfJob(
                Mockito.eq(123L), Mockito.eq("IW_RAW__0_GRDH_1.xml"),
                Mockito.eq(AppDataJobGenerationState.READY));
        doReturn(sentAppJob).when(appDataPService).patchTaskTableOfJob(
                Mockito.eq(123L), Mockito.eq("IW_RAW__0_GRDH_1.xml"),
                Mockito.eq(AppDataJobGenerationState.SENT));
    }

    @Test
    public void testPresearchWhenSliceMissing()
            throws IpfPrepWorkerInputsMissingException, MetadataQueryException {
        doThrow(new MetadataQueryException("test ex"))
                .when(this.metadataClient).getL0Slice(Mockito.anyString());

        thrown.expect(IpfPrepWorkerInputsMissingException.class);
        thrown.expectMessage("Missing inputs");
        thrown.expect(hasProperty("missingMetadata",
                hasKey(appDataJob.getProduct().getProductName())));
        thrown.expect(hasProperty("missingMetadata",
                hasValue(containsString("lice: test ex"))));
        generator.preSearch(jobA);
    }

    @Test
    public void testPresearchWhenAcnMissing()
            throws IpfPrepWorkerInputsMissingException, MetadataQueryException {
        doThrow(new MetadataQueryException("test ex"))
                .when(this.metadataClient)
                .getFirstACN(Mockito.anyString(), Mockito.anyString());

        thrown.expect(IpfPrepWorkerInputsMissingException.class);
        thrown.expectMessage("Missing inputs");
        thrown.expect(hasProperty("missingMetadata",
                hasKey(appDataJob.getProduct().getProductName())));
        thrown.expect(hasProperty("missingMetadata",
                hasValue(containsString("CNs: test ex"))));
        generator.preSearch(jobA);
    }

    @Test
    public void testPresearch()
            throws IpfPrepWorkerInputsMissingException, IpfPrepWorkerMetadataException {
        generator.preSearch(jobA);

        assertEquals(appDataJobComplete.getProduct().getInsConfId(),
                jobA.getAppDataJob().getProduct().getInsConfId());
        assertEquals(appDataJobComplete.getProduct().getNumberSlice(),
                jobA.getAppDataJob().getProduct().getNumberSlice());
        assertEquals(appDataJobComplete.getProduct().getDataTakeId(),
                jobA.getAppDataJob().getProduct().getDataTakeId());
        assertEquals(appDataJobComplete.getProduct().getProductType(),
                jobA.getAppDataJob().getProduct().getProductType());
        assertEquals(appDataJobComplete.getProduct().getTotalNbOfSlice(),
                jobA.getAppDataJob().getProduct().getTotalNbOfSlice());
        assertEquals(appDataJobComplete.getProduct().getSegmentStartDate(),
                jobA.getAppDataJob().getProduct().getSegmentStartDate());
        assertEquals(appDataJobComplete.getProduct().getSegmentStopDate(),
                jobA.getAppDataJob().getProduct().getSegmentStopDate());
    }

    @Test
    public void testUpdateProcParam() {
        final AbstractJobOrderConf conf = new L0JobOrderConf();
        conf.setProcessorName("AIO_PROCESSOR");
        final JobOrderProcParam procParam1 =
                new JobOrderProcParam("Processing_Mode", "FAST24");
        final JobOrderProcParam procParam2 =
                new JobOrderProcParam("PT_Assembly", "no");
        final JobOrderProcParam procParam3 = new JobOrderProcParam("Timeout", "360");
        conf.addProcParam(procParam1);
        conf.addProcParam(procParam2);
        conf.addProcParam(procParam3);

        final JobOrder jobOrder = new JobOrder();
        jobOrder.setConf(conf);

        generator.updateProcParam(jobOrder, "PT_Assembly", "yes");
        assertTrue(jobOrder.getConf().getNbProcParams() == 3);
        assertEquals("FAST24",
                jobOrder.getConf().getProcParams().get(0).getValue());
        assertEquals("yes",
                jobOrder.getConf().getProcParams().get(1).getValue());
        assertEquals("360",
                jobOrder.getConf().getProcParams().get(2).getValue());

        generator.updateProcParam(jobOrder, "Mission_Id", "S1");
        assertTrue(jobOrder.getConf().getNbProcParams() == 4);
        assertEquals("FAST24",
                jobOrder.getConf().getProcParams().get(0).getValue());
        assertEquals("yes",
                jobOrder.getConf().getProcParams().get(1).getValue());
        assertEquals("360",
                jobOrder.getConf().getProcParams().get(2).getValue());
        assertEquals("Mission_Id",
                jobOrder.getConf().getProcParams().get(3).getName());
        assertEquals("S1",
                jobOrder.getConf().getProcParams().get(3).getValue());
    }

    @Test
    public void testCustomeJobOrder() {
        final AbstractJobOrderConf conf = new L0JobOrderConf();
        conf.setProcessorName("AIO_PROCESSOR");
        final JobOrderProcParam procParam1 =
                new JobOrderProcParam("Mission_Id", "S1B");
        final JobOrderProcParam procParam2 =
                new JobOrderProcParam("PT_Assembly", "no");
        final JobOrderProcParam procParam3 = new JobOrderProcParam("Timeout", "360");
        conf.addProcParam(procParam1);
        conf.addProcParam(procParam2);
        conf.addProcParam(procParam3);

        final JobOrder jobOrder = new JobOrder();
        jobOrder.setConf(conf);

        final JobGeneration job =
                new JobGeneration(appDataJobComplete, "IW_RAW__0_GRDH_1.xml");
        job.setJobOrder(jobOrder);

        generator.customJobOrder(job);

        assertEquals("20171213_121623224083",
                job.getJobOrder().getConf().getSensingTime().getStart());
        assertEquals("20171213_121656224083",
                job.getJobOrder().getConf().getSensingTime().getStop());
        assertEquals(8, job.getJobOrder().getConf().getNbProcParams());
        for (final JobOrderProcParam param : job.getJobOrder().getConf()
                .getProcParams()) {
            switch (param.getName()) {
                case "Mission_Id":
                    assertEquals("S1A", param.getValue());
                    break;
                case "PT_Assembly":
                    assertEquals("no", param.getValue());
                    break;
                case "Timeout":
                    assertEquals("360", param.getValue());
                    break;
                case "Slice_Number":
                    assertEquals("3", param.getValue());
                    break;
                case "Total_Number_Of_Slices":
                    assertEquals("10", param.getValue());
                    break;
                case "Slice_Overlap":
                    assertEquals("7.7", param.getValue());
                    break;
                case "Slice_Length":
                    assertEquals("60.0", param.getValue());
                    break;
                case "Slicing_Flag":
                    assertEquals("TRUE", param.getValue());
                    break;
            }
        }
    }

    // FIXME enable test
//    @Test
//    public void testRun() {
//        try {
//
//            generator.run();
//
//            Mockito.verify(JobsSender).sendJob(Mockito.any(), Mockito.any());
//
//            assertEquals(ProductFamily.L1_JOB, publishedJob.getFamily());
//            assertEquals("NRT", publishedJob.getProductProcessMode());
//            assertEquals(
//                    "S1A_IW_RAW__0SDV_20171213T142312_20171213T142344_019685_02173E_07F5.SAFE",
//                    publishedJob.getProductIdentifier());
//            assertEquals(expectedTaskTable.getPools().size(),
//                    publishedJob.getPools().size());
//            for (int i = 0; i < expectedTaskTable.getPools().size(); i++) {
//                assertEquals(
//                        expectedTaskTable.getPools().get(i).getTasks().size(),
//                        publishedJob.getPools().get(i).getTasks().size());
//                for (int j = 0; j < expectedTaskTable.getPools().get(i)
//                        .getTasks().size(); j++) {
//                    assertEquals(
//                            expectedTaskTable.getPools().get(i).getTasks()
//                                    .get(j).getFileName(),
//                            publishedJob.getPools().get(i).getTasks().get(j)
//                                    .getBinaryPath());
//                }
//            }
//
//            // TODO to improve to check dto ok
//        } catch (Exception e) {
//        	e.printStackTrace();
//            fail(e.getMessage());
//        }
//    }
}
