/**
 * 
 */
package esa.s1pdgs.cpoc.mdcatalog.extraction;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.mdcatalog.es.EsServices;
import esa.s1pdgs.cpoc.mdcatalog.extraction.model.ConfigFileDescriptor;
import esa.s1pdgs.cpoc.mdcatalog.extraction.obs.ObsService;
import esa.s1pdgs.cpoc.mdcatalog.status.AppStatus;
import esa.s1pdgs.cpoc.mqi.client.GenericMqiService;
import esa.s1pdgs.cpoc.mqi.model.queue.AuxiliaryFileDto;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;

/**
 * KAFKA consumer. Consume on a topic defined in configuration file
 * 
 * @author Olivier Bex-Chauvet
 */
@Controller
public class AuxiliaryFilesExtractor
        extends GenericExtractor<AuxiliaryFileDto> {

    /**
     * Logger
     */
    private static final Logger LOGGER =
            LogManager.getLogger(AuxiliaryFilesExtractor.class);

    /**
     * Pattern for configuration files to extract data
     */
    private final static String PATTERN_CONFIG =
            "^([0-9a-z][0-9a-z]){1}([0-9a-z]){1}(_(OPER|TEST))?_(AUX_OBMEMC|AUX_PP1|AUX_CAL|AUX_INS|AUX_RESORB|MPL_ORBPRE|MPL_ORBSCT)_\\w{1,}\\.(XML|EOF|SAFE)(/.*)?$";

    /**
     * Amazon S3 service for configuration files
     */
    private final ObsService obsService;

    /**
     * Manifest filename
     */
    private final String manifestFilename;

    /**
     * 
     */
    private final String fileManifestExt;

    @Autowired
    public AuxiliaryFilesExtractor(final EsServices esServices,
            final ObsService obsService,
            @Qualifier("mqiServiceForAuxiliaryFiles") final GenericMqiService<AuxiliaryFileDto> mqiService,
            final AppStatus appStatus,
            final MetadataExtractorConfig extractorConfig,
            @Value("${file.product-categories.auxiliary-files.local-directory}") final String localDirectory,
            @Value("${file.manifest-filename}") final String manifestFilename,
            @Value("${file.file-with-manifest-ext}") final String fileManifestExt) {
        super(esServices, mqiService, appStatus, localDirectory,
                extractorConfig, PATTERN_CONFIG, ProductCategory.AUXILIARY_FILES);
        this.obsService = obsService;
        this.manifestFilename = manifestFilename;
        this.fileManifestExt = fileManifestExt;
    }

    /**
     * Consume a message from the AUXILIARY_FILES product category and extract
     * metadata
     * 
     * @see GenericExtractor#genericExtract()
     */
    @Scheduled(fixedDelayString = "${file.product-categories.auxiliary-files.fixed-delay-ms}", initialDelayString = "${file.product-categories.auxiliary-files.init-delay-poll-ms}")
    public void extract() {
        super.genericExtract();
    }

    /**
     * @see GenericExtractor#extractMetadata(GenericMessageDto)
     */
    @Override
    protected JSONObject extractMetadata(
            final GenericMessageDto<AuxiliaryFileDto> message)
            throws AbstractCodedException {
        // Upload file
        String keyObs = getKeyObs(message);
        LOGGER.info(
                "[MONITOR] [step 1] [AUXILIARY_FILES] [productName {}] Downloading file {}",
                extractProductNameFromDto(message.getBody()), keyObs);
        File metadataFile = obsService.downloadFile(
                ProductFamily.AUXILIARY_FILE, keyObs, this.localDirectory);

        // Extract description from pattern
        LOGGER.info(
                "[MONITOR] [step 2] [AUXILIARY_FILES] [productName {}] Extracting from filename",
                extractProductNameFromDto(message.getBody()));
        ConfigFileDescriptor configFileDesc =
                fileDescriptorBuilder.buildConfigFileDescriptor(metadataFile);

        // Build metadata from file and extracted
        LOGGER.info(
                "[MONITOR] [step 3] [AUXILIARY_FILES] [productName {}] Extracting from file",
                extractProductNameFromDto(message.getBody()));
        return mdBuilder.buildConfigFileMetadata(configFileDesc, metadataFile);
    }

    /**
     * Get the OBS key of the file used for extracting metadata for this product
     * 
     * @param message
     * @return
     */
    private String getKeyObs(final GenericMessageDto<AuxiliaryFileDto> message) {
        String keyObs = message.getBody().getKeyObjectStorage();
        if (keyObs.toLowerCase().endsWith(fileManifestExt.toLowerCase())) {
            keyObs += "/" + manifestFilename;
        }
        return keyObs;
    }

    /**
     * @see GenericExtractor#extractProductNameFromDto(Object)
     */
    @Override
    protected String extractProductNameFromDto(final AuxiliaryFileDto dto) {
        return dto.getProductName();
    }

    /**
     * @see GenericExtractor#cleanProcessing(GenericMessageDto)
     */
    @Override
    protected void cleanProcessing(
            final GenericMessageDto<AuxiliaryFileDto> message) {
        // TODO Auto-generated method stub
        File metadataFile = new File(localDirectory + getKeyObs(message));
        if (metadataFile.exists()) {
            File parent = metadataFile.getParentFile();
            metadataFile.delete();
            // Remove upper directory if needed
            if (!localDirectory.endsWith(parent.getName() + "/")) {
                parent.delete();
            }
        }
    }

}
