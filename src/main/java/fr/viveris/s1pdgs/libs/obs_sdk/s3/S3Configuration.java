package fr.viveris.s1pdgs.libs.obs_sdk.s3;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.retry.PredefinedBackoffStrategies;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import fr.viveris.s1pdgs.libs.obs_sdk.ObsFamily;
import fr.viveris.s1pdgs.libs.obs_sdk.ObsServiceException;
import fr.viveris.s1pdgs.libs.obs_sdk.s3.retry.SDKCustomDefaultRetryCondition;

/**
 * The amazon S3 keys
 * 
 * @author Viveris Technologies
 */
public class S3Configuration {

    /**
     * Configuration file name
     */
    public static final String CONFIG_FILE = "obs-aws-s3.properties";

    /**
     * Configuration
     */
    private final PropertiesConfiguration configuration;

    /**
     * A unique string that identified the user <code>user.id</code>
     */
    public static final String USER_ID = "user.id";

    /**
     * <code>user.secret</code>
     */
    public static final String USER_SECRET = "user.secret";

    /**
     * <code>endpoint</code>
     */
    public static final String ENDPOINT = "endpoint";

    /**
     * <code>endpoint.region</code>
     */
    public static final String ENDPOINT_REGION = "endpoint.region";

    /**
     * Name of the bucket dedicated to the family
     * {@link ObsFamily#AUXILIARY_FILE}
     */
    public static final String BCK_AUX_FILES = "bucket.auxiliary-files";

    /**
     * Name of the bucket dedicated to the family {@link ObsFamily#EDRS_SESSION}
     */
    public static final String BCK_EDRS_SESSIONS = "bucket.edrs-sessions";

    /**
     * Name of the bucket dedicated to the family {@link ObsFamily#L0_PRODUCT}
     */
    public static final String BCK_L0_PRODUCTS = "bucket.l0-products";

    /**
     * Name of the bucket dedicated to the family {@link ObsFamily#L0_ACN}
     */
    public static final String BCK_L0_ACNS = "bucket.l0-acns";

    /**
     * Name of the bucket dedicated to the family {@link ObsFamily#L1_PRODUCT}
     */
    public static final String BCK_L1_PRODUCTS = "bucket.l1-products";

    /**
     * Name of the bucket dedicated to the family {@link ObsFamily#L1_ACN}
     */
    public static final String BCK_L1_ACNS = "bucket.l1-acns";

    /**
     * Timeout in second for shutdown a thread
     */
    public static final String TM_S_SHUTDOWN = "timeout-s.shutdown";

    /**
     * Timeout in second of a download execution
     */
    public static final String TM_S_DOWN_EXEC = "timeout-s.down-exec";

    /**
     * Timeout in second of a upload execution
     */
    public static final String TM_S_UP_EXEC = "timeout-s.up-exec";

    /**
     * @throws ConfigurationException
     */
    public S3Configuration() throws ObsServiceException {
        try {
            configuration = new PropertiesConfiguration(CONFIG_FILE);
            configuration
                    .setReloadingStrategy(new FileChangedReloadingStrategy());
        } catch (ConfigurationException confEx) {
            throw new ObsServiceException(
                    "Properties extraction fails: " + confEx.getMessage(),
                    confEx);
        }
    }

    /**
     * @return
     */
    public PropertiesConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Get a configured value in int format
     * 
     * @param key
     * @return
     * @throws ObsServiceException
     */
    public int getIntOfConfiguration(final String key)
            throws ObsServiceException {
        try {
            return configuration.getInt(key);
        } catch (ConversionException convE) {
            throw new ObsServiceException("Cannot get configuration value of "
                    + key + ": " + convE.getMessage(), convE);
        }
    }

    /**
     * Get the name of the bucket to use according the OBS family
     * 
     * @param family
     * @return
     * @throws ObsServiceException
     */
    public String getBucketForFamily(final ObsFamily family)
            throws ObsServiceException {
        String bucket;
        switch (family) {
            case AUXILIARY_FILE:
                bucket = configuration.getString(BCK_AUX_FILES);
                break;
            case EDRS_SESSION:
                bucket = configuration.getString(BCK_EDRS_SESSIONS);
                break;
            case L0_ACN:
                bucket = configuration.getString(BCK_L0_ACNS);
                break;
            case L0_PRODUCT:
                bucket = configuration.getString(BCK_L0_PRODUCTS);
                break;
            case L1_ACN:
                bucket = configuration.getString(BCK_L1_ACNS);
                break;
            case L1_PRODUCT:
                bucket = configuration.getString(BCK_L1_PRODUCTS);
                break;
            default:
                throw new ObsServiceException(
                        "Invalid object storage family " + family);
        }
        return bucket;
    }

    /**
     * Build the default Amazon s3 client
     * 
     * @return
     */
    public AmazonS3 defaultS3Client() {
        // Credentials
        BasicAWSCredentials awsCreds =
                new BasicAWSCredentials(configuration.getString(USER_ID),
                        configuration.getString(USER_SECRET));
        
        // Client configuration (protocol and retry policy)
        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTP);
        RetryPolicy retryPolicy = new RetryPolicy(
                new SDKCustomDefaultRetryCondition(10),
                new PredefinedBackoffStrategies.SDKDefaultBackoffStrategy(100,
                        500, 20000),
                3, true);
        clientConfig.setRetryPolicy(retryPolicy);
        
        // Amazon s3 client
        return AmazonS3ClientBuilder.standard()
                .withClientConfiguration(clientConfig)
                .withEndpointConfiguration(new EndpointConfiguration(
                        configuration.getString(ENDPOINT),
                        configuration.getString(ENDPOINT_REGION)))
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
    }
}
