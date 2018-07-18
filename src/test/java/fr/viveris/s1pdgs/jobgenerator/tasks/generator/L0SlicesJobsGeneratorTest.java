package fr.viveris.s1pdgs.jobgenerator.tasks.generator;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;

import java.io.File;
import java.io.IOException;
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

import fr.viveris.s1pdgs.jobgenerator.config.AppConfig;
import fr.viveris.s1pdgs.jobgenerator.config.JobGeneratorSettings;
import fr.viveris.s1pdgs.jobgenerator.config.JobGeneratorSettings.WaitTempo;
import fr.viveris.s1pdgs.jobgenerator.config.ProcessSettings;
import fr.viveris.s1pdgs.jobgenerator.controller.JobsProducer;
import fr.viveris.s1pdgs.jobgenerator.exception.AbstractCodedException;
import fr.viveris.s1pdgs.jobgenerator.exception.InputsMissingException;
import fr.viveris.s1pdgs.jobgenerator.exception.MetadataException;
import fr.viveris.s1pdgs.jobgenerator.model.Job;
import fr.viveris.s1pdgs.jobgenerator.model.ProcessLevel;
import esa.s1pdgs.cpoc.common.ProductFamily;
import fr.viveris.s1pdgs.jobgenerator.model.ResumeDetails;
import fr.viveris.s1pdgs.jobgenerator.model.joborder.AbstractJobOrderConf;
import fr.viveris.s1pdgs.jobgenerator.model.joborder.JobOrder;
import fr.viveris.s1pdgs.jobgenerator.model.joborder.JobOrderProcParam;
import fr.viveris.s1pdgs.jobgenerator.model.joborder.L0JobOrderConf;
import fr.viveris.s1pdgs.jobgenerator.model.metadata.L0AcnMetadata;
import fr.viveris.s1pdgs.jobgenerator.model.metadata.L0SliceMetadata;
import fr.viveris.s1pdgs.jobgenerator.model.metadata.SearchMetadata;
import fr.viveris.s1pdgs.jobgenerator.model.metadata.SearchMetadataQuery;
import fr.viveris.s1pdgs.jobgenerator.model.product.L0Slice;
import fr.viveris.s1pdgs.jobgenerator.model.product.L0SliceProduct;
import fr.viveris.s1pdgs.jobgenerator.model.tasktable.TaskTable;
import fr.viveris.s1pdgs.jobgenerator.service.XmlConverter;
import fr.viveris.s1pdgs.jobgenerator.service.metadata.MetadataService;
import fr.viveris.s1pdgs.jobgenerator.utils.DateUtils;
import fr.viveris.s1pdgs.jobgenerator.utils.TestGenericUtils;

public class L0SlicesJobsGeneratorTest {

	/**
	 * For testing exceptions
	 */
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	private XmlConverter xmlConverter;

	@Mock
	private MetadataService metadataService;

	@Mock
	private ProcessSettings processSettings;

	@Mock
	private JobGeneratorSettings jobGeneratorSettings;

	@Mock
	private JobsProducer kafkaJobsSender;

	private TaskTable expectedTaskTable;

	private L0SlicesJobsGenerator generator;

	private Job<L0Slice> jobA;

	/**
	 * Test set up
	 * 
	 * @throws Exception
	 */
	@Before
	public void init() throws Exception {

		// Retrieve task table from the XML converter
		expectedTaskTable = TestGenericUtils.buildTaskTableIW();

		// Mcokito
		MockitoAnnotations.initMocks(this);
		this.mockProcessSettings();
		this.mockJobGeneratorSettings();
		this.mockXmlConverter();
		this.mockMetadataService();
		this.mockKafkaSender();

		JobsGeneratorFactory factory = new JobsGeneratorFactory(processSettings, jobGeneratorSettings, xmlConverter,
				metadataService, kafkaJobsSender);
		generator = (L0SlicesJobsGenerator) factory
				.createJobGeneratorForL0Slice(new File("./test/data/generic_config/task_tables/IW_RAW__0_GRDH_1.xml"));

		L0Slice sliceA = new L0Slice("IW");
		L0SliceProduct productA = new L0SliceProduct(
				"S1A_IW_RAW__0SDV_20171213T142312_20171213T142344_019685_02173E_07F5.SAFE", "A", "S1",
				DateUtils.convertDateIso("20171213T142312"), DateUtils.convertDateIso("20171213T142312"), sliceA);
		jobA = new Job<>(productA, new ResumeDetails("topic", "dto"));
	}

	private void mockProcessSettings() {
		Mockito.doAnswer(i -> {
			Map<String, String> r = new HashMap<String, String>(2);
			return r;
		}).when(processSettings).getParams();
		Mockito.doAnswer(i -> {
			return ProcessLevel.L1;
		}).when(processSettings).getLevel();
		Mockito.doAnswer(i -> {
			Map<String, String> r = new HashMap<String, String>(5);
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
			Map<String, ProductFamily> r = new HashMap<>();
			r.put("IW_GRDH_1S", ProductFamily.L1_PRODUCT);
			r.put("IW_GRDH_1A", ProductFamily.L1_ACN);
			r.put("IW_RAW__0S", ProductFamily.L0_PRODUCT);
			r.put("IW_RAW__0A", ProductFamily.L0_ACN);
			r.put("IW_RAW__0C", ProductFamily.L0_ACN);
			r.put("IW_RAW__0N", ProductFamily.L0_ACN);
			return r;
		}).when(jobGeneratorSettings).getOutputfamilies();
		Mockito.doAnswer(i -> {
			return ProductFamily.AUXILIARY_FILE.name();
		}).when(jobGeneratorSettings).getDefaultfamily();
		Mockito.doAnswer(i -> {
			return 2;
		}).when(jobGeneratorSettings).getMaxnumberofjobs();
		Mockito.doAnswer(i -> {
			return new WaitTempo(2000, 3);
		}).when(jobGeneratorSettings).getWaitprimarycheck();
		Mockito.doAnswer(i -> {
			return new WaitTempo(10000, 3);
		}).when(jobGeneratorSettings).getWaitmetadatainput();
		Mockito.doAnswer(i -> {
			Map<String, Float> r = new HashMap<>();
			r.put("IW", 7.7F);
			r.put("EW", 8.2F);
			return r;
		}).when(jobGeneratorSettings).getTypeOverlap();
		Mockito.doAnswer(i -> {
			Map<String, Float> r = new HashMap<>();
			r.put("IW", 60F);
			r.put("EW", 21F);
			return r;
		}).when(jobGeneratorSettings).getTypeSliceLength();
	}

	private void mockXmlConverter() {
		try {
			Mockito.when(xmlConverter.convertFromXMLToObject(Mockito.anyString())).thenReturn(expectedTaskTable);
			Mockito.doAnswer(i -> {
				AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
				ctx.register(AppConfig.class);
				ctx.refresh();
				XmlConverter xmlConverter = ctx.getBean(XmlConverter.class);
				String r = xmlConverter.convertFromObjectToXMLString(i.getArgument(0));
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
			}).when(this.metadataService).getEdrsSession(Mockito.anyString(), Mockito.anyString());
			Mockito.doAnswer(i -> {
				return new L0SliceMetadata("S1A_IW_RAW__0SDV_20171213T121623_20171213T121656_019684_021735_C6DB.SAFE",
						"IW_RAW__0S", "S1A_IW_RAW__0SDV_20171213T121623_20171213T121656_019684_021735_C6DB.SAFE",
						"2017-12-13T12:16:23.224083Z", "2017-12-13T12:16:56.224083Z", 6, 3, "021735");
			}).when(this.metadataService).getSlice(Mockito.anyString(), Mockito.anyString());
			Mockito.doAnswer(i -> {
				return new L0AcnMetadata("S1A_IW_RAW__0ADV_20171213T121123_20171213T121947_019684_021735_51B1.SAFE",
						"IW_RAW__0A", "S1A_IW_RAW__0ADV_20171213T121123_20171213T121947_019684_021735_51B1.SAFE",
						"2017-12-13T12:16:23.224083Z", "2017-12-13T12:16:56.224083Z", 6, 10, "021735");
			}).when(this.metadataService).getFirstACN(Mockito.anyString(), Mockito.anyString());
			Mockito.doAnswer(i -> {
				SearchMetadataQuery query = i.getArgument(0);
				if ("IW_RAW__0S".equalsIgnoreCase(query.getProductType())) {
					return new SearchMetadata(
							"S1A_IW_RAW__0SDV_20171213T121623_20171213T121656_019684_021735_C6DB.SAFE", "IW_RAW__0S",
							"S1A_IW_RAW__0SDV_20171213T121623_20171213T121656_019684_021735_C6DB.SAFE",
							"2017-12-13T12:16:23", "2017-12-13T12:16:56");
				} else if ("IW_RAW__0A".equalsIgnoreCase(query.getProductType())) {
					return new SearchMetadata(
							"S1A_IW_RAW__0ADV_20171213T121123_20171213T121947_019684_021735_51B1.SAFE", "IW_RAW__0A",
							"S1A_IW_RAW__0ADV_20171213T121123_20171213T121947_019684_021735_51B1.SAFE",
							"2017-12-13T12:11:23", "2017-12-13T12:19:47");
				} else if ("IW_RAW__0C".equalsIgnoreCase(query.getProductType())) {
					return new SearchMetadata(
							"S1A_IW_RAW__0CDV_20171213T121123_20171213T121947_019684_021735_E131.SAFE", "IW_RAW__0C",
							"S1A_IW_RAW__0CDV_20171213T121123_20171213T121947_019684_021735_E131.SAFE",
							"2017-12-13T12:11:23", "2017-12-13T12:19:47");
				} else if ("IW_RAW__0N".equalsIgnoreCase(query.getProductType())) {
					return new SearchMetadata(
							"S1A_IW_RAW__0NDV_20171213T121123_20171213T121947_019684_021735_87D4.SAFE", "IW_RAW__0N",
							"S1A_IW_RAW__0NDV_20171213T121123_20171213T121947_019684_021735_87D4.SAFE",
							"2017-12-13T12:11:23", "2017-12-13T12:19:47");
				} else if ("AUX_CAL".equalsIgnoreCase(query.getProductType())) {
					return new SearchMetadata("S1A_AUX_CAL_V20171017T080000_G20171013T101200.SAFE", "AUX_CAL",
							"S1A_AUX_CAL_V20171017T080000_G20171013T101200.SAFE", "2017-10-17T08:00:00",
							"9999-12-31T23:59:59");
				} else if ("AUX_INS".equalsIgnoreCase(query.getProductType())) {
					return new SearchMetadata("S1A_AUX_INS_V20171017T080000_G20171013T101216.SAFE", "AUX_INS",
							"S1A_AUX_INS_V20171017T080000_G20171013T101216.SAFE", "2017-10-17T08:00:00",
							"9999-12-31T23:59:59");
				} else if ("AUX_PP1".equalsIgnoreCase(query.getProductType())) {
					return new SearchMetadata("S1A_AUX_PP1_V20171017T080000_G20171013T101236.SAFE", "AUX_PP1",
							"S1A_AUX_PP1_V20171017T080000_G20171013T101236.SAFE", "2017-10-17T08:00:00",
							"9999-12-31T23:59:59");
				} else if ("AUX_RESORB".equalsIgnoreCase(query.getProductType())) {
					return new SearchMetadata(
							"S1A_OPER_AUX_RESORB_OPOD_20171213T143838_V20171213T102737_20171213T134507.EOF",
							"AUX_OBMEMC",
							"S1A_OPER_AUX_RESORB_OPOD_20171213T143838_V20171213T102737_20171213T134507.EOF",
							"2017-12-13T10:27:37", "2017-12-13T13:45:07");
				}
				return null;
			}).when(this.metadataService).search(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString(),
					Mockito.anyInt());
		} catch (MetadataException e) {
			fail(e.getMessage());
		}
	}

	private void mockKafkaSender() throws AbstractCodedException {
		Mockito.doAnswer(i -> {
			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(new File("./tmp/jobDtoL1.json"), i.getArgument(0));
			return null;
		}).when(this.kafkaJobsSender).send(Mockito.any());
	}

	@Test
	public void testPresearchWhenSliceMissing() throws InputsMissingException, MetadataException {
		doThrow(new MetadataException("test ex")).when(this.metadataService).getSlice(Mockito.anyString(),
				Mockito.anyString());

		thrown.expect(InputsMissingException.class);
		thrown.expectMessage("Missing inputs");
		thrown.expect(hasProperty("missingMetadata", hasKey(jobA.getProduct().getIdentifier())));
		thrown.expect(hasProperty("missingMetadata", hasValue(containsString("lice: test ex"))));
		generator.preSearch(jobA);
	}

	@Test
	public void testPresearchWhenAcnMissing() throws InputsMissingException, MetadataException {
		doThrow(new MetadataException("test ex")).when(this.metadataService).getFirstACN(Mockito.anyString(),
				Mockito.anyString());

		thrown.expect(InputsMissingException.class);
		thrown.expectMessage("Missing inputs");
		thrown.expect(hasProperty("missingMetadata", hasKey(jobA.getProduct().getIdentifier())));
		thrown.expect(hasProperty("missingMetadata", hasValue(containsString("CNs: test ex"))));
		generator.preSearch(jobA);
	}

	@Test
	public void testUpdateProcParam() {
		AbstractJobOrderConf conf = new L0JobOrderConf();
		conf.setProcessorName("AIO_PROCESSOR");
		JobOrderProcParam procParam1 = new JobOrderProcParam("Processing_Mode", "FAST24");
		JobOrderProcParam procParam2 = new JobOrderProcParam("PT_Assembly", "no");
		JobOrderProcParam procParam3 = new JobOrderProcParam("Timeout", "360");
		conf.addProcParam(procParam1);
		conf.addProcParam(procParam2);
		conf.addProcParam(procParam3);

		JobOrder jobOrder = new JobOrder();
		jobOrder.setConf(conf);

		generator.updateProcParam(jobOrder, "PT_Assembly", "yes");
		assertTrue(jobOrder.getConf().getNbProcParams() == 3);
		assertEquals("FAST24", jobOrder.getConf().getProcParams().get(0).getValue());
		assertEquals("yes", jobOrder.getConf().getProcParams().get(1).getValue());
		assertEquals("360", jobOrder.getConf().getProcParams().get(2).getValue());

		generator.updateProcParam(jobOrder, "Mission_Id", "S1");
		assertTrue(jobOrder.getConf().getNbProcParams() == 4);
		assertEquals("FAST24", jobOrder.getConf().getProcParams().get(0).getValue());
		assertEquals("yes", jobOrder.getConf().getProcParams().get(1).getValue());
		assertEquals("360", jobOrder.getConf().getProcParams().get(2).getValue());
		assertEquals("Mission_Id", jobOrder.getConf().getProcParams().get(3).getName());
		assertEquals("S1", jobOrder.getConf().getProcParams().get(3).getValue());
	}

	@Test
	public void testRun() {
		try {

			generator.addJob(jobA);
			generator.run();

			Mockito.verify(kafkaJobsSender).send(Mockito.any());

			// As job is not removed from cached if no another run is launch we can it
			assertFalse(generator.cachedJobs.containsKey("SESSION_1"));
			// TODO to improve to check dto ok (do manually by reading the file
			// ./tmp/jobDto.txt)
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
}
