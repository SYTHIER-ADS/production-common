package esa.s1pdgs.cpoc.prip.frontend.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.core.uri.UriResourceEntitySetImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.RecoverableDataAccessException;

import esa.s1pdgs.cpoc.common.CommonConfigurationProperties;
import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.obs.ObsException;
import esa.s1pdgs.cpoc.obs_sdk.ObsClient;
import esa.s1pdgs.cpoc.obs_sdk.ObsServiceException;
import esa.s1pdgs.cpoc.prip.frontend.service.processor.ProductEntityProcessor;
import esa.s1pdgs.cpoc.prip.metadata.PripMetadataRepository;
import esa.s1pdgs.cpoc.prip.model.PripMetadata;

public class TestProductEntityProcessor {
	
	ProductEntityProcessor uut;

	@Mock
	CommonConfigurationProperties commonConfigurationProperties;
	
	@Mock
	PripMetadataRepository pripMetadataRepositoryMock;

	@Mock
	OData odataMock;
	
	@Mock
	UriInfo uriInfoMock;
	
	@Mock
	EdmEntitySet edmEntitySetMock;
	
	@Mock
	ODataRequest odataRequestMock;
	
	@Mock
	UriResourceEntitySet uriResourceEntitySetMock;
	
	@Mock
	UriParameter uriParameterMock;
	
	@Mock
	ODataSerializer odataSerializerMock;
	
	@Mock
	SerializerResult serializerResultMock;
	
	@Mock
	ObsClient obsClientMock; 
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		final int downloadUrlExpirationTimeInSeconds = 0;
		uut = new ProductEntityProcessor(commonConfigurationProperties, pripMetadataRepositoryMock, obsClientMock, downloadUrlExpirationTimeInSeconds, "username");
		uut.init(odataMock, null);
	}

	@Test
	public void testReadEntity_OnExistentProduct_ShallReturnStatusOk() throws ODataApplicationException, ODataLibraryException, IOException {
		final String entitySetName = "Products";
		final String uuid = "00000000-0000-0000-0000-000000000001";
		final String baseUri = "http://example.org";
		final String odataPath = "/" + entitySetName + "(" + uuid + ")";

		final PripMetadata pripMetadata = new PripMetadata();
		pripMetadata.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
		
		doReturn(pripMetadata).when(pripMetadataRepositoryMock).findById(Mockito.eq(uuid));

		doReturn(baseUri).when(odataRequestMock).getRawBaseUri();
		doReturn(odataPath).when(odataRequestMock).getRawODataPath();
		doReturn(baseUri + odataPath).when(odataRequestMock).getRawRequestUri();

		doReturn(Arrays.asList(uriResourceEntitySetMock)).when(uriInfoMock).getUriResourceParts();

		doReturn(edmEntitySetMock).when(uriResourceEntitySetMock).getEntitySet();
		doReturn(Arrays.asList(uriParameterMock)).when(uriResourceEntitySetMock).getKeyPredicates();
		
		doReturn("Id").when(uriParameterMock).getName();
		doReturn("'" + uuid + "'").when(uriParameterMock).getText();
		
		doReturn(entitySetName).when(edmEntitySetMock).getName();
		
		doReturn(odataSerializerMock).when(odataMock).createSerializer(Mockito.any());
		
		doReturn(serializerResultMock).when(odataSerializerMock).entity(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		
		doReturn(new ByteArrayInputStream("expected result".getBytes())).when(serializerResultMock).getContent();
		
		final ODataResponse odataResponse = new ODataResponse();
		uut.readEntity(odataRequestMock, odataResponse, uriInfoMock, ContentType.JSON_FULL_METADATA);
		
		Mockito.verify(pripMetadataRepositoryMock, times(1)).findById(Mockito.eq(uuid));
		assertEquals(HttpStatusCode.OK.getStatusCode(), odataResponse.getStatusCode());
		assertEquals("expected result", IOUtils.toString(odataResponse.getContent(), StandardCharsets.UTF_8));
	}

	@Test
	public void testReadEntity_OnNonExistentProduct_ShallReturnStatusNotFound() throws ODataApplicationException, ODataLibraryException {
		final String entity = "Products";
		final String uuid = "00000000-0000-0000-0000-000000000002";
		final String baseUri = "http://example.org";
		final String odataPath = "/" + entity + "(" + uuid + ")";

		doReturn(null).when(pripMetadataRepositoryMock).findById(Mockito.eq(uuid));

		doReturn(baseUri).when(odataRequestMock).getRawBaseUri();
		doReturn(odataPath).when(odataRequestMock).getRawODataPath();
		doReturn(baseUri + odataPath).when(odataRequestMock).getRawRequestUri();

		final UriResourceEntitySetImpl uriResourceEntitySet = new UriResourceEntitySetImpl(edmEntitySetMock);			
		uriResourceEntitySet.setKeyPredicates(Arrays.asList(uriParameterMock));
		
		doReturn("Id").when(uriParameterMock).getName();
		doReturn("'" + uuid + "'").when(uriParameterMock).getText();
		
		doReturn(Arrays.asList(uriResourceEntitySetMock)).when(uriInfoMock).getUriResourceParts();

		doReturn(edmEntitySetMock).when(uriResourceEntitySetMock).getEntitySet();
		doReturn(Arrays.asList(uriParameterMock)).when(uriResourceEntitySetMock).getKeyPredicates();
		
		doReturn(entity).when(edmEntitySetMock).getName();
				
		final ODataResponse odataResponse = new ODataResponse();
		uut.readEntity(odataRequestMock, odataResponse, uriInfoMock, ContentType.JSON_FULL_METADATA);
		
		Mockito.verify(pripMetadataRepositoryMock, times(1)).findById(Mockito.eq(uuid));
		assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), odataResponse.getStatusCode());
	}
	
	@Test
	public void testReadEntity_OnRecoverableDataAccessException_ShallReturnStatusServiceUnavailabl()
			throws ODataApplicationException, ODataLibraryException, IOException {
		final String entitySetName = "Products";
		final String uuid = "00000000-0000-0000-0000-000000000001";
		final String baseUri = "http://example.org";
		final String odataPath = "/" + entitySetName + "(" + uuid + ")";

		final PripMetadata pripMetadata = new PripMetadata();
		pripMetadata.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
		
		doThrow(RecoverableDataAccessException.class).when(pripMetadataRepositoryMock).findById(Mockito.eq(uuid));

		doReturn(baseUri).when(odataRequestMock).getRawBaseUri();
		doReturn(odataPath).when(odataRequestMock).getRawODataPath();
		doReturn(baseUri + odataPath).when(odataRequestMock).getRawRequestUri();

		doReturn(Arrays.asList(uriResourceEntitySetMock)).when(uriInfoMock).getUriResourceParts();

		doReturn(edmEntitySetMock).when(uriResourceEntitySetMock).getEntitySet();
		doReturn(Arrays.asList(uriParameterMock)).when(uriResourceEntitySetMock).getKeyPredicates();
		
		doReturn("Id").when(uriParameterMock).getName();
		doReturn("'" + uuid + "'").when(uriParameterMock).getText();
		
		doReturn(entitySetName).when(edmEntitySetMock).getName();
		
		final ODataResponse odataResponse = new ODataResponse();
		try {
			uut.readEntity(odataRequestMock, odataResponse, uriInfoMock, ContentType.JSON_FULL_METADATA);
			fail("Required exception wasn't thrown");
		} catch (ODataApplicationException e) {
			assertEquals(HttpStatusCode.SERVICE_UNAVAILABLE.getStatusCode(), e.getStatusCode());
		}
		
		Mockito.verify(pripMetadataRepositoryMock, times(1)).findById(Mockito.eq(uuid));
	}
	
	@Test
	public void testReadMediaEntity_OnExistentProduct_ShallReturnStatusTemoraryRedirect()
			throws ODataApplicationException, ODataLibraryException, IOException, ObsException, ObsServiceException {
		final String entitySetName = "Products";
		final String uuid = "00000000-0000-0000-0000-000000000001";
		final String baseUri = "http://example.org";
		final String odataPath = "/" + entitySetName + "(" + uuid + ")";

		final PripMetadata pripMetadata = new PripMetadata();
		pripMetadata.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
		pripMetadata.setProductFamily(ProductFamily.BLANK);
		pripMetadata.setObsKey("S1foo/bar");
		
		doReturn(pripMetadata).when(pripMetadataRepositoryMock).findById(Mockito.eq(uuid));

		doReturn(baseUri).when(odataRequestMock).getRawBaseUri();
		doReturn(odataPath).when(odataRequestMock).getRawODataPath();
		doReturn(baseUri + odataPath).when(odataRequestMock).getRawRequestUri();

		doReturn(Arrays.asList(uriResourceEntitySetMock)).when(uriInfoMock).getUriResourceParts();

		doReturn(edmEntitySetMock).when(uriResourceEntitySetMock).getEntitySet();
		doReturn(Arrays.asList(uriParameterMock)).when(uriResourceEntitySetMock).getKeyPredicates();
		
		doReturn("Id").when(uriParameterMock).getName();
		doReturn("'" + uuid + "'").when(uriParameterMock).getText();
		
		doReturn(entitySetName).when(edmEntitySetMock).getName();
			
		doReturn(new URL("http://www.example.org")).when(obsClientMock).createTemporaryDownloadUrl(Mockito.any(), Mockito.anyLong());
		
		final ODataResponse odataResponse = new ODataResponse();
		uut.readMediaEntity(odataRequestMock, odataResponse, uriInfoMock, ContentType.JSON_FULL_METADATA);
		
		Mockito.verify(pripMetadataRepositoryMock, times(1)).findById(Mockito.eq(uuid));
		assertEquals(HttpStatusCode.TEMPORARY_REDIRECT.getStatusCode(), odataResponse.getStatusCode());
	}
	
	@Test
	public void testReadMediaEntity_OnNonExistentProduct_ShallReturnStatusNotFound()
			throws ODataApplicationException, ODataLibraryException, IOException, ObsException, ObsServiceException {
		final String entitySetName = "Products";
		final String uuid = "00000000-0000-0000-0000-000000000001";
		final String baseUri = "http://example.org";
		final String odataPath = "/" + entitySetName + "(" + uuid + ")";

		doReturn(null).when(pripMetadataRepositoryMock).findById(Mockito.eq(uuid));

		doReturn(baseUri).when(odataRequestMock).getRawBaseUri();
		doReturn(odataPath).when(odataRequestMock).getRawODataPath();
		doReturn(baseUri + odataPath).when(odataRequestMock).getRawRequestUri();

		doReturn(Arrays.asList(uriResourceEntitySetMock)).when(uriInfoMock).getUriResourceParts();

		doReturn(edmEntitySetMock).when(uriResourceEntitySetMock).getEntitySet();
		doReturn(Arrays.asList(uriParameterMock)).when(uriResourceEntitySetMock).getKeyPredicates();
		
		doReturn("Id").when(uriParameterMock).getName();
		doReturn("'" + uuid + "'").when(uriParameterMock).getText();
		
		doReturn(entitySetName).when(edmEntitySetMock).getName();
			
		final ODataResponse odataResponse = new ODataResponse();
		uut.readMediaEntity(odataRequestMock, odataResponse, uriInfoMock, ContentType.JSON_FULL_METADATA);
		
		Mockito.verify(pripMetadataRepositoryMock, times(1)).findById(Mockito.eq(uuid));
		assertEquals(HttpStatusCode.NOT_FOUND.getStatusCode(), odataResponse.getStatusCode());
	}
}
