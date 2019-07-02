package esa.s1pdgs.cpoc.mdcatalog.extraction;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import esa.s1pdgs.cpoc.common.EdrsSessionFileType;
import esa.s1pdgs.cpoc.common.FileExtension;
import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.errors.processing.MetadataExtractionException;
import esa.s1pdgs.cpoc.errorrepo.ErrorRepoAppender;
import esa.s1pdgs.cpoc.mdcatalog.ProcessConfiguration;
import esa.s1pdgs.cpoc.mdcatalog.es.EsServices;
import esa.s1pdgs.cpoc.mdcatalog.extraction.model.EdrsSessionFileDescriptor;
import esa.s1pdgs.cpoc.mdcatalog.extraction.obs.ObsService;
import esa.s1pdgs.cpoc.mdcatalog.status.AppStatus;
import esa.s1pdgs.cpoc.mqi.client.GenericMqiService;
import esa.s1pdgs.cpoc.mqi.model.queue.EdrsSessionDto;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;
import esa.s1pdgs.cpoc.report.LoggerReporting;

public class EdrsSessionsExtractorTest {

    /**
     * Elasticsearch services
     */
    @Mock
    protected EsServices esServices;

    /**
     * Elasticsearch services
     */
    @Mock
    protected ObsService obsService;

    /**
     * MQI service
     */
    @Mock
    private GenericMqiService<EdrsSessionDto> mqiService;

    /**
     * 
     */
    @Mock
    protected MetadataExtractorConfig extractorConfig;

    /**
     * Application status
     */
    @Mock
    protected AppStatus appStatus;

    /**
     * Extractor
     */
    protected EdrsSessionsExtractor extractor;

    /**
     * Job to process
     */
    private GenericMessageDto<EdrsSessionDto> inputMessage;

    /**
     * Job to process
     */
    private GenericMessageDto<EdrsSessionDto> inputMessageXml;

    private final ErrorRepoAppender errorAppender = ErrorRepoAppender.NULL;
    
    private final ProcessConfiguration config = new ProcessConfiguration();
    
    /**
     * Initialization
     * 
     * @throws AbstractCodedException
     */
    @Before
    public void init() throws AbstractCodedException {
        MockitoAnnotations.initMocks(this);

        doNothing().when(appStatus).setError(Mockito.any(), Mockito.anyString());
        doReturn(true).when(mqiService).ack(Mockito.any());

        inputMessage = new GenericMessageDto<EdrsSessionDto>(123, "",
                new EdrsSessionDto("S1A/123/ch01/D_123_ch01_D.RAW", 1,
                        EdrsSessionFileType.RAW, "S1", "A"));

        inputMessageXml = new GenericMessageDto<EdrsSessionDto>(123, "",
                new EdrsSessionDto("S1B/123/ch02/D_123_ch03_D.XML", 2,
                        EdrsSessionFileType.SESSION, "S1", "B"));

        extractor = new EdrsSessionsExtractor(esServices, mqiService, appStatus,
                (new File("./test/workDir/")).getAbsolutePath(),errorAppender, config,
                extractorConfig);
    }

    @Test
    public void testExtractProductName() {
        assertEquals("S1A/123/ch01/D_123_ch01_D.RAW",
                extractor.extractProductNameFromDto(inputMessage.getBody()));
        assertEquals("S1B/123/ch02/D_123_ch03_D.XML",
                extractor.extractProductNameFromDto(inputMessageXml.getBody()));
    }

    @Test
    public void testExtractMetadata() throws MetadataExtractionException, AbstractCodedException {
        EdrsSessionFileDescriptor expectedDescriptor =
                new EdrsSessionFileDescriptor();
        
        expectedDescriptor.setFilename("D_123_ch01_D.RAW");
        expectedDescriptor.setRelativePath("S1A/123/ch01/D_123_ch01_D.RAW");
        expectedDescriptor.setProductName("D_123_ch01_D.RAW");
        expectedDescriptor.setExtension(FileExtension.RAW);
        expectedDescriptor.setEdrsSessionFileType(EdrsSessionFileType.RAW);
        expectedDescriptor.setMissionId("S1");
        expectedDescriptor.setSatelliteId("A");
        expectedDescriptor.setChannel(1);
        expectedDescriptor.setSessionIdentifier("123");
        expectedDescriptor.setKeyObjectStorage("S1A/123/ch01/D_123_ch01_D.RAW");
        expectedDescriptor.setProductFamily(ProductFamily.EDRS_SESSION);
        
        final LoggerReporting.Factory reportingFactory = new LoggerReporting.Factory(
        		LogManager.getLogger(GenericExtractorTest.class), "TestMetadataExtraction")
        		.product(ProductFamily.EDRS_SESSION.toString(), "D_123_ch01_D.RAW");
        
        JSONObject expected = extractor.mdBuilder
                .buildEdrsSessionFileMetadata(expectedDescriptor);
        JSONObject result = extractor.extractMetadata(reportingFactory, inputMessage);
        for (String key: expected.keySet()) {
            if (!"insertionTime".equals(key)) {
                assertEquals(expected.get(key), result.get(key));
            }
        }

    }

}
