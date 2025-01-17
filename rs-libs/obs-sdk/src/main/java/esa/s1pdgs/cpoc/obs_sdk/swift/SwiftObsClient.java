package esa.s1pdgs.cpoc.obs_sdk.swift;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.client.factory.AccountFactory;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.StoredObject;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.obs.ObsException;
import esa.s1pdgs.cpoc.metadata.model.MissionId;
import esa.s1pdgs.cpoc.obs_sdk.AbstractObsClient;
import esa.s1pdgs.cpoc.obs_sdk.FileObsUploadObject;
import esa.s1pdgs.cpoc.obs_sdk.Md5;
import esa.s1pdgs.cpoc.obs_sdk.ObsClient;
import esa.s1pdgs.cpoc.obs_sdk.ObsConfigurationProperties;
import esa.s1pdgs.cpoc.obs_sdk.ObsDownloadObject;
import esa.s1pdgs.cpoc.obs_sdk.ObsObject;
import esa.s1pdgs.cpoc.obs_sdk.ObsObjectMetadata;
import esa.s1pdgs.cpoc.obs_sdk.ObsServiceException;
import esa.s1pdgs.cpoc.obs_sdk.SdkClientException;
import esa.s1pdgs.cpoc.obs_sdk.StreamObsUploadObject;
import esa.s1pdgs.cpoc.obs_sdk.ValidArgumentAssertion;
import esa.s1pdgs.cpoc.obs_sdk.report.ReportingProductFactory;
import esa.s1pdgs.cpoc.report.Reporting;
import esa.s1pdgs.cpoc.report.ReportingMessage;
import esa.s1pdgs.cpoc.report.ReportingUtils;

public class SwiftObsClient extends AbstractObsClient {
	public static final class Factory implements ObsClient.Factory {

		@Override
		public final ObsClient newObsClient(final ObsConfigurationProperties config, final ReportingProductFactory factory) {
	    	final AccountConfig accConf = new AccountConfig();
	    	accConf.setUsername(config.getUserId());
	        accConf.setPassword(config.getUserSecret());
	        accConf.setAuthUrl(config.getEndpoint());
	        accConf.setAuthenticationMethod(config.getAuthMethod());
	        
	        // either tenant id or tenant name must be supplied
	        if (!config.getTenantId().equals(ObsConfigurationProperties.UNDEFINED)) {
	        	accConf.setTenantId(config.getTenantId());
	        }	        
	        if (!config.getTenantName().equals(ObsConfigurationProperties.UNDEFINED)) {
	        	accConf.setTenantName(config.getTenantName());
	        }	        
	        if (!config.getEndpointRegion().equals(ObsConfigurationProperties.UNDEFINED)) {
	        	accConf.setPreferredRegion(config.getEndpointRegion());
	        }
	        
	        // set proxy if defined in environmental
	        final String proxyConfig = System.getenv("https_proxy");
	        
			if (proxyConfig != null && !proxyConfig.equals("")) {
				final String removedProtocol = proxyConfig
						.replaceAll(Pattern.quote("http://"), "")
						.replaceAll(Pattern.quote("https://"), "")
						.replaceAll(Pattern.quote("/"), ""); // remove trailing slash

				final String host = removedProtocol.substring(0, removedProtocol.indexOf(':'));
				final int port = Integer.parseInt(removedProtocol.substring(removedProtocol.indexOf(':') + 1));
				accConf.setProxyHost(host);
		        accConf.setProxyPort(port);
		        accConf.setUseProxy(true);
			}
			
//			TODO: Translate the following S3 Retry Policy setup code to some JOSS equivalent 
//			RetryPolicy retryPolicy = new RetryPolicy(
//	                new SDKCustomDefaultRetryCondition(
//	                        configuration.getInt(RETRY_POLICY_MAX_RETRIES)),
//	                new PredefinedBackoffStrategies.SDKDefaultBackoffStrategy(
//	                        configuration.getInt(RETRY_POLICY_BASE_DELAY_MS),
//	                        configuration
//	                                .getInt(RETRY_POLICY_THROTTLED_BASE_DELAY_MS),
//	                        configuration.getInt(RETRY_POLICY_MAX_BACKOFF_MS)),
//	                configuration.getInt(RETRY_POLICY_MAX_RETRIES), true);
//	        client.setRetryPolicy(retryPolicy);
			
			final Account account = new AccountFactory(accConf).createAccount();
			
			if (null != account.getPreferredRegion() && !"".equals(account.getPreferredRegion())) {
				account.getAccess().setPreferredRegion(account.getPreferredRegion());
			}			
			final SwiftObsServices services = new SwiftObsServices(
					account,
					config.getMaxRetries(),
					config.getBackoffThrottledBaseDelay()
			);
			return new SwiftObsClient(config, services, factory);
		}		
	}
	
	public static final String BACKEND_NAME = "swift";

    protected final SwiftObsServices swiftObsServices;
     
	SwiftObsClient(final ObsConfigurationProperties configuration, final SwiftObsServices swiftObsServices, final ReportingProductFactory factory) {
		super(configuration, factory);
		this.swiftObsServices = swiftObsServices;
	}

	public boolean containerExists(final ProductFamily family) throws ObsServiceException {
		return swiftObsServices.containerExist(getBucketFor(family));
	}
	
	public int numberOfObjects(final ProductFamily family, final String prefixKey) throws SwiftSdkClientException, ObsServiceException {
		return swiftObsServices.getNbObjects(getBucketFor(family), prefixKey);
	}
    
	@Override
	public boolean exists(final ObsObject object) throws SdkClientException, ObsServiceException {
		ValidArgumentAssertion.assertValidArgument(object);
		return swiftObsServices.exist(getBucketFor(object.getFamily()), object.getKey());
	}

	@Override
	public boolean prefixExists(final ObsObject object) throws SdkClientException, ObsServiceException {
		ValidArgumentAssertion.assertValidArgument(object);
		return swiftObsServices.getNbObjects(
                getBucketFor(object.getFamily()),
                object.getKey()) > 0;
	}

	@Override
	public List<File> downloadObject(final ObsDownloadObject object) throws SdkClientException, ObsServiceException {
		return swiftObsServices.downloadObjectsWithPrefix(
                getBucketFor(object.getFamily()),
                object.getKey(), object.getTargetDir(),
                object.isIgnoreFolders());
	}

	@Override
	public void uploadObject(final FileObsUploadObject object) throws SdkClientException, ObsServiceException, ObsException {
		final List<Md5.Entry> fileList = new ArrayList<>();
        if (object.getFile().isDirectory()) {
        	fileList.addAll(swiftObsServices.uploadDirectory(
                    getBucketFor(object.getFamily()),
                    object.getKey(), object.getFile()));
        } else {
        	fileList.add(swiftObsServices.uploadFile(getBucketFor(object.getFamily()), object.getKey(), object.getFile()));
        }
        uploadMd5Sum(object, fileList);
	}

	/**
	 * @param object
	 * @throws ObsServiceException
	 * @throws SwiftSdkClientException
	 *
	 *  Note: The stream is not closed here. It should be closed after upload.
	 *
	 */
	@Override
	protected Md5.Entry uploadObject(final StreamObsUploadObject object) throws ObsServiceException, SwiftSdkClientException {
		return swiftObsServices.uploadStream(getBucketFor(object.getFamily()), object.getKey(), object.getInput(), object.getContentLength());
	}

	@Override
	public final void uploadMd5Sum(final ObsObject object, final List<Md5.Entry> fileList) throws ObsServiceException {
		File file;
		try {
			file = File.createTempFile(object.getKey(), Md5.MD5SUM_SUFFIX);
			try(PrintWriter writer = new PrintWriter(file)) {
				for (final Md5.Entry fileInfo : fileList) {
					writer.println(fileInfo);
				}
			}
		} catch (final IOException e) {
			throw new SwiftObsServiceException(getBucketFor(object.getFamily()), object.getKey(), "Could not store md5sum temp file", e);
		}
		try {
			swiftObsServices.uploadFile(getBucketFor(object.getFamily()), Md5.md5KeyFor(object), file);
		} catch (final SwiftSdkClientException e1) {
			throw new ObsServiceException(
					String.format("Could not upload md5 %s: %s", file, e1.getMessage()), 
					e1
			);
		}
		
		try {
			Files.delete(file.toPath());
		} catch(final IOException e) {
			file.deleteOnExit();
		}
	}	
	
	@Override
	public void move(final ObsObject from, final ProductFamily to) throws ObsException, ObsServiceException {
		ValidArgumentAssertion.assertValidArgument(from);
		ValidArgumentAssertion.assertValidArgument(to);
		swiftObsServices.move(from.getKey(), getBucketFor(from.getFamily()), getBucketFor(to));
	}

	public void createContainer(final ProductFamily family) throws SwiftSdkClientException, ObsServiceException {
		swiftObsServices.createContainer(getBucketFor(family));
	}
	
	public void deleteObject(final ProductFamily family, final String key) throws SwiftSdkClientException, ObsServiceException {
		swiftObsServices.delete(getBucketFor(family), key);
	}

	@Override
	public List<ObsObject> getObsObjectsOfFamilyWithinTimeFrame(final ProductFamily family, final Date timeFrameBegin, final Date timeFrameEnd)
			throws SdkClientException, ObsServiceException {
		ValidArgumentAssertion.assertValidArgument(family);
		ValidArgumentAssertion.assertValidArgument(timeFrameBegin);
		ValidArgumentAssertion.assertValidArgument(timeFrameEnd);

		final long methodStartTime = System.currentTimeMillis();
		final List<ObsObject> objectsOfTimeFrame = new ArrayList<>();
		final String container = getBucketFor(family);
		Collection<StoredObject> objListing = swiftObsServices.listObjectsFromContainer(container);
		boolean possiblyTruncated;
		String marker = "";
		do {
			if (objListing == null || objListing.size() == 0) {
				break;
			}
			
			for (final StoredObject o : objListing) {
				marker = o.getName();
				final Date lastModified = o.getLastModifiedAsDate();
				if (lastModified.after(timeFrameBegin) && lastModified.before(timeFrameEnd)) {
					final ObsObject obsObj = new ObsObject(family, o.getName());
					objectsOfTimeFrame.add(obsObj);
				}
			}

			possiblyTruncated = objListing.size() == swiftObsServices.MAX_RESULTS_PER_LIST;
			if (possiblyTruncated) {
				objListing = swiftObsServices.listNextBatchOfObjectsFromContainer(container, marker);
			}

		} while (possiblyTruncated);

		final float methodDuration = (System.currentTimeMillis() - methodStartTime) / 1000f;
		LOGGER.debug(String.format("Time for OBS listing objects from bucket %s within time frame: %.2fs", container,
				methodDuration));

		return objectsOfTimeFrame;
	}
	
	@Override
	public List<String> list(ProductFamily family, String keyPrefix) throws SdkClientException {
		throw new UnsupportedOperationException("list");
	}

	@Override
	public InputStream getAsStream(ProductFamily family, String key) throws SdkClientException {
		throw new UnsupportedOperationException("getAsStream");
	}

	@Override
	protected Map<String,String> collectETags(final ObsObject object) throws ObsException {
		try {
			return swiftObsServices.collectETags(getBucketFor(object.getFamily()), object.getKey());
		} catch (SwiftSdkClientException | ObsServiceException e) {
			throw new ObsException(object.getFamily(), object.getKey(), e);
		}
	}

	@Override
	public long size(final ObsObject object) throws ObsException {
		ValidArgumentAssertion.assertValidArgument(object);
		try {
			final String bucketName = getBucketFor(object.getFamily());
			/*
			 * This method is supposed to return the size of exactly one object. If more than
			 * one is returned the object is not unique and very likely not the full name of it or
			 * a directory. We are not supporting this and thus operations fails
			 */
			if (swiftObsServices.getNbObjects(bucketName, object.getKey()) != 1) {
				throw new IllegalArgumentException(String.format(
						"Unable to determinate size of object '%s' (family:%s) (is a directory or not exist?)",
						object.getKey(), object.getFamily()));
			}
			
			// return the size of the object
			return swiftObsServices.size(bucketName, object.getKey());						
		} catch (final SdkClientException ex) {
			throw new ObsException(object.getFamily(), object.getKey(), ex);
		}
	}

	@Override
	public void setExpirationTime(ObsObject object, Instant expirationTime) throws ObsServiceException {
		throw new UnsupportedOperationException("setExpirationTime not implemented yet for swift");
	}

	@Override
	public ObsObjectMetadata getMetadata(ObsObject object) {
		throw new UnsupportedOperationException("getMetadata not implemented yet for swift");
	}

	@Override
	public URL createTemporaryDownloadUrl(final ObsObject object, final long expirationTimeInSeconds) throws ObsException {
		ValidArgumentAssertion.assertValidArgument(object);
		URL url;
		final Reporting reporting = ReportingUtils.newReportingBuilder(MissionId.UNDEFINED).newReporting("ObsCreateTemporaryDownloadUrl");
		reporting.begin(new ReportingMessage(size(object), "Start creating temporary download URL for username '{}' for product '{}'", "anonymous", object.getKey()));
		try {
			url = swiftObsServices.createTemporaryDownloadUrl(getBucketFor(object.getFamily()), object.getKey(), expirationTimeInSeconds);
			reporting.end(new ReportingMessage(size(object), "End creating temporary download URL for username '{}' for product '{}'", "anonymous", object.getKey()));
		} catch (final SdkClientException ex) {
			reporting.error(new ReportingMessage(size(object), "Error on creating temporary download URL for username '{}' for product '{}'", "anonymous", object.getKey()));				
			throw new ObsException(object.getFamily(), object.getKey(), ex);
		}		
     	return url;
	}

	@Override
	public void delete(ObsObject object) throws ObsException, ObsServiceException {
		throw new UnsupportedOperationException("delete not yet implemented for swift");
	}
	
	@Override
	public String getAbsoluteStoragePath(ProductFamily family, String keyObs) {
		throw new UnsupportedOperationException("getAbsoluteStoragePath not yet implemented for swift");
	}
}
