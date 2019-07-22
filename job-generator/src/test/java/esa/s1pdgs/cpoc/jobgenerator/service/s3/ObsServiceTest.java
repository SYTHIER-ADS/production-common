package esa.s1pdgs.cpoc.jobgenerator.service.s3;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.obs.ObsException;
import esa.s1pdgs.cpoc.common.errors.obs.ObsUnknownObject;
import esa.s1pdgs.cpoc.obs_sdk.ObsClient;
import esa.s1pdgs.cpoc.obs_sdk.ObsDownloadObject;
import esa.s1pdgs.cpoc.obs_sdk.ObsFamily;
import esa.s1pdgs.cpoc.obs_sdk.ObsObject;
import esa.s1pdgs.cpoc.obs_sdk.ObsService;
import esa.s1pdgs.cpoc.obs_sdk.ObsServiceException;
import esa.s1pdgs.cpoc.obs_sdk.ObsUploadObject;
import esa.s1pdgs.cpoc.obs_sdk.SdkClientException;

/**
 * Test the ObsService
 * 
 * @author Viveris Technologies
 *
 */
public class ObsServiceTest {

	/**
	 * Mock OBS client
	 */
	@Mock
	private ObsClient client;

	/**
	 * Service to test
	 */
	private ObsService service;

	/**
	 * To check the raised custom exceptions
	 */
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	/**
	 * Initialization
	 * 
	 * @throws SdkClientException
	 * @throws ObsServiceException
	 */
	@Before
	public void init() throws ObsServiceException, SdkClientException {
		MockitoAnnotations.initMocks(this);

		doThrow(new ObsServiceException("error 1 message")).when(client)
				.doesObjectExist(Mockito.eq(new ObsObject("error-key", ObsFamily.AUXILIARY_FILE)));
		doThrow(new SdkClientException("error 2 message")).when(client)
				.doesObjectExist(Mockito.eq(new ObsObject("error-key", ObsFamily.EDRS_SESSION)));
		doReturn(true).when(client).doesObjectExist(Mockito.eq(new ObsObject("test-key", ObsFamily.AUXILIARY_FILE)));
		doReturn(false).when(client).doesObjectExist(Mockito.eq(new ObsObject("test-key", ObsFamily.EDRS_SESSION)));

		doThrow(new ObsServiceException("error 1 message")).when(client).uploadObject(
				Mockito.eq(new ObsUploadObject("error-key", ObsFamily.AUXILIARY_FILE, new File("pom.xml"))));
		doThrow(new SdkClientException("error 2 message")).when(client).uploadObject(
				Mockito.eq(new ObsUploadObject("error-key", ObsFamily.EDRS_SESSION, new File("pom.xml"))));
		doReturn(2).when(client).uploadObject(
				Mockito.eq(new ObsUploadObject("test-key", ObsFamily.AUXILIARY_FILE, new File("pom.xml"))));
		doReturn(1).when(client)
				.uploadObject(Mockito.eq(new ObsUploadObject("test-key", ObsFamily.EDRS_SESSION, new File("pom.xml"))));

		doThrow(new ObsServiceException("error 1 message")).when(client)
				.downloadObject(Mockito.eq(new ObsDownloadObject("error-key", ObsFamily.AUXILIARY_FILE, "test/")));
		doThrow(new SdkClientException("error 2 message")).when(client)
				.downloadObject(Mockito.eq(new ObsDownloadObject("error-key", ObsFamily.EDRS_SESSION, "test/")));
		doReturn(0).when(client)
				.downloadObject(Mockito.eq(new ObsDownloadObject("test-key", ObsFamily.AUXILIARY_FILE, "test/")));
		doReturn(1).when(client)
				.downloadObject(Mockito.eq(new ObsDownloadObject("test-key", ObsFamily.EDRS_SESSION, "test/")));
		doReturn(2).when(client)
				.downloadObject(Mockito.eq(new ObsDownloadObject("test-key/key2", ObsFamily.EDRS_SESSION, "test/")));

		service = new ObsService(client);
	}

	/**
	 * Test getObsFamily
	 */
	@Test
	public void testGetObsFamily() {
		assertEquals(ObsFamily.AUXILIARY_FILE, service.getObsFamily(ProductFamily.AUXILIARY_FILE));
		assertEquals(ObsFamily.EDRS_SESSION, service.getObsFamily(ProductFamily.EDRS_SESSION));
		assertEquals(ObsFamily.UNKNOWN, service.getObsFamily(ProductFamily.BLANK));
		assertEquals(ObsFamily.L0_ACN, service.getObsFamily(ProductFamily.L0_ACN));
		assertEquals(ObsFamily.L0_SLICE, service.getObsFamily(ProductFamily.L0_SLICE));
        assertEquals(ObsFamily.L0_SEGMENT, service.getObsFamily(ProductFamily.L0_SEGMENT));
		assertEquals(ObsFamily.UNKNOWN, service.getObsFamily(ProductFamily.L0_REPORT));
		assertEquals(ObsFamily.L1_ACN, service.getObsFamily(ProductFamily.L1_ACN));
		assertEquals(ObsFamily.L1_SLICE, service.getObsFamily(ProductFamily.L1_SLICE));
		assertEquals(ObsFamily.UNKNOWN, service.getObsFamily(ProductFamily.L1_REPORT));
		assertEquals(ObsFamily.UNKNOWN, service.getObsFamily(ProductFamily.JOB_ORDER));
	}

	/**
	 * Test exist when client raise ObsServiceException
	 * 
	 * @throws ObjectStorageException
	 */
	@Test
	public void testExistWhenException() throws ObsException {
		thrown.expect(ObsException.class);
		thrown.expect(hasProperty("key", is("error-key")));
		thrown.expect(hasProperty("family", is(ProductFamily.AUXILIARY_FILE)));
		thrown.expectMessage("error 1 message");
		thrown.expectCause(isA(ObsServiceException.class));

		service.exist(ProductFamily.AUXILIARY_FILE, "error-key");
	}

	/**
	 * Test exist when client raise ObsServiceException
	 * 
	 * @throws ObjectStorageException
	 */
	@Test
	public void testExistWhenException2() throws ObsException {
		thrown.expect(ObsException.class);
		thrown.expect(hasProperty("key", is("error-key")));
		thrown.expect(hasProperty("family", is(ProductFamily.EDRS_SESSION)));
		thrown.expectMessage("error 2 message");
		thrown.expectCause(isA(SdkClientException.class));

		service.exist(ProductFamily.EDRS_SESSION, "error-key");
	}

	/**
	 * Test nominal case of exists
	 * 
	 * @throws ObjectStorageException
	 * @throws ObsServiceException
	 * @throws SdkClientException
	 */
	@Test
	public void testNominalExist() throws ObsException, ObsServiceException, SdkClientException {
		boolean ret = service.exist(ProductFamily.AUXILIARY_FILE, "test-key");
		assertTrue(ret);
		verify(client, times(1)).doesObjectExist(Mockito.eq(new ObsObject("test-key", ObsFamily.AUXILIARY_FILE)));

		ret = service.exist(ProductFamily.EDRS_SESSION, "test-key");
		assertFalse(ret);
		verify(client, times(1)).doesObjectExist(Mockito.eq(new ObsObject("test-key", ObsFamily.EDRS_SESSION)));
	}

	/**
	 * Test exist when client raise ObsServiceException
	 * 
	 * @throws ObjectStorageException
	 */
	@Test
	public void testUploadFileWhenException() throws ObsException {
		thrown.expect(ObsException.class);
		thrown.expect(hasProperty("key", is("error-key")));
		thrown.expect(hasProperty("family", is(ProductFamily.AUXILIARY_FILE)));
		thrown.expectMessage("error 1 message");
		thrown.expectCause(isA(ObsServiceException.class));

		service.uploadFile(ProductFamily.AUXILIARY_FILE, "error-key", new File("pom.xml"));
	}

	/**
	 * Test exist when client raise ObsServiceException
	 * 
	 * @throws ObjectStorageException
	 */
	@Test
	public void testUploadFileWhenException2() throws ObsException {
		thrown.expect(ObsException.class);
		thrown.expect(hasProperty("key", is("error-key")));
		thrown.expect(hasProperty("family", is(ProductFamily.EDRS_SESSION)));
		thrown.expectMessage("error 2 message");
		thrown.expectCause(isA(SdkClientException.class));

		service.uploadFile(ProductFamily.EDRS_SESSION, "error-key", new File("pom.xml"));
	}

	/**
	 * Test nominal case of exists
	 * 
	 * @throws ObjectStorageException
	 * @throws ObsServiceException
	 * @throws SdkClientException
	 */
	@Test
	public void testNominalUpload() throws ObsException, ObsServiceException, SdkClientException {
		service.uploadFile(ProductFamily.AUXILIARY_FILE, "test-key", new File("pom.xml"));
		verify(client, times(1)).uploadObject(
				Mockito.eq(new ObsUploadObject("test-key", ObsFamily.AUXILIARY_FILE, new File("pom.xml"))));

		service.uploadFile(ProductFamily.EDRS_SESSION, "test-key", new File("pom.xml"));
		verify(client, times(1))
				.uploadObject(Mockito.eq(new ObsUploadObject("test-key", ObsFamily.EDRS_SESSION, new File("pom.xml"))));
	}

	/**
	 * Test downloadFile when client raise ObsServiceException
	 * 
	 * @throws ObjectStorageException
	 * @throws ObsUnknownObjectException
	 */
	@Test
	public void testDownloadFileWhenException() throws ObsException, ObsUnknownObject {
		thrown.expect(ObsException.class);
		thrown.expect(hasProperty("key", is("error-key")));
		thrown.expect(hasProperty("family", is(ProductFamily.AUXILIARY_FILE)));
		thrown.expectMessage("error 1 message");
		thrown.expectCause(isA(ObsServiceException.class));

		service.downloadFile(ProductFamily.AUXILIARY_FILE, "error-key", "test/");
	}

	/**
	 * Test downloadFile when client raise ObsServiceException
	 * 
	 * @throws ObjectStorageException
	 * @throws ObsUnknownObjectException
	 */
	@Test
	public void testDownloadFileWhenException2() throws ObsException, ObsUnknownObject {
		thrown.expect(ObsException.class);
		thrown.expect(hasProperty("key", is("error-key")));
		thrown.expect(hasProperty("family", is(ProductFamily.EDRS_SESSION)));
		thrown.expectMessage("error 2 message");
		thrown.expectCause(isA(SdkClientException.class));

		service.downloadFile(ProductFamily.EDRS_SESSION, "error-key", "test/");
	}

	/**
	 * Test downloadFile when client raise ObsServiceException
	 * 
	 * @throws ObjectStorageException
	 * @throws ObsUnknownObjectException
	 */
	@Test
	public void testDownloadFileWhenUnknown() throws ObsException, ObsUnknownObject {
		thrown.expect(ObsUnknownObject.class);
		thrown.expect(hasProperty("key", is("test-key")));
		thrown.expect(hasProperty("family", is(ProductFamily.AUXILIARY_FILE)));

		service.downloadFile(ProductFamily.AUXILIARY_FILE, "test-key", "test/");
	}

	/**
	 * Test nominal case of downloadFile
	 * 
	 * @throws ObjectStorageException
	 * @throws ObsServiceException
	 * @throws SdkClientException
	 * @throws ObsUnknownObjectException
	 */
	@Test
	public void testNominalDownload()
			throws ObsException, ObsServiceException, SdkClientException, ObsUnknownObject {
		File upload1 = service.downloadFile(ProductFamily.EDRS_SESSION, "test-key", "test/");
		verify(client, times(1))
				.downloadObject(Mockito.eq(new ObsDownloadObject("test-key", ObsFamily.EDRS_SESSION, "test/")));
		assertEquals("test-key", upload1.getName());
		assertEquals("test", upload1.getParentFile().getName());

		File upload2 = service.downloadFile(ProductFamily.EDRS_SESSION, "test-key/key2", "test/");
		verify(client, times(1))
				.downloadObject(Mockito.eq(new ObsDownloadObject("test-key/key2", ObsFamily.EDRS_SESSION, "test/")));
		assertEquals("key2", upload2.getName());
		assertEquals("test", upload2.getParentFile().getName());
	}
}
