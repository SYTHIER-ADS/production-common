package standalone.prip.obs;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.errors.obs.ObsException;
import esa.s1pdgs.cpoc.obs_sdk.ObsClient;
import esa.s1pdgs.cpoc.obs_sdk.ObsDownloadObject;
import esa.s1pdgs.cpoc.obs_sdk.ObsObject;
import esa.s1pdgs.cpoc.obs_sdk.ObsServiceException;
import esa.s1pdgs.cpoc.obs_sdk.ObsUploadObject;
import esa.s1pdgs.cpoc.obs_sdk.SdkClientException;

// dummy impl - doin nothin
public class FakeObsClient implements ObsClient {	
	@Override
	public boolean exists(ObsObject object) throws SdkClientException, ObsServiceException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean prefixExists(ObsObject object) throws SdkClientException, ObsServiceException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<File> download(final List<ObsDownloadObject> objects) throws AbstractCodedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void upload(final List<ObsUploadObject> objects) throws AbstractCodedException {
		// TODO Auto-generated method stub
	}

	@Override
	public List<ObsObject> getObsObjectsOfFamilyWithinTimeFrame(ProductFamily obsFamily,
			Date timeFrameBegin, Date timeFrameEnd) throws SdkClientException, ObsServiceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void move(ObsObject from, ProductFamily to) throws ObsException {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<String, ObsObject> listInterval(ProductFamily family, Date intervalStart, Date intervalEnd)
			throws SdkClientException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, InputStream> getAllAsInputStream(ProductFamily family, String keyPrefix)
			throws SdkClientException {
		return null;
	}
	
	@Override
	public void validate(ObsObject object) throws ObsServiceException {
	    // TODO Auto-generated method stub

	}

	@Override
	public long size(ObsObject object) throws ObsException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getChecksum(ObsObject object) throws ObsException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public URL createTemporaryDownloadUrl(ObsObject object, long expirationTimeInSeconds)
			throws ObsException, ObsServiceException {
		URL url = null;
		try {
			url = new URL("http://www.example.org");
		} catch (MalformedURLException e) {
		}
		return url;
	}	

}
