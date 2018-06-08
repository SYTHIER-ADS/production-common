package fr.viveris.s1pdgs.mdcatalog.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.viveris.s1pdgs.mdcatalog.controllers.rest.SearchMetadataController;
import fr.viveris.s1pdgs.mdcatalog.controllers.rest.dto.SearchMetadataDto;
import fr.viveris.s1pdgs.mdcatalog.model.exception.MetadataNotPresentException;
import fr.viveris.s1pdgs.mdcatalog.model.metadata.SearchMetadata;
import fr.viveris.s1pdgs.mdcatalog.services.es.EsServices;
import fr.viveris.s1pdgs.test.RestControllerTest;

public class SearchMetadataControllerTest extends RestControllerTest {

	@Mock
	private EsServices esServices;

	private SearchMetadataController controller;

	@Before
	public void init() throws IOException {
		MockitoAnnotations.initMocks(this);

		this.controller = new SearchMetadataController(esServices);
		this.initMockMvc(this.controller);
	}

	private void mockSearchMetadata(SearchMetadata response) throws Exception {
		doReturn(response).when(esServices).lastValCover(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class), Mockito.anyInt());
	}

	private void mockSearchMetadataNotPresentException() throws Exception {
		doThrow(new MetadataNotPresentException("name")).when(esServices).lastValCover(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class), Mockito.anyInt());
	}
	
	private void mockSearchMetadataException() throws Exception {
		doThrow(new Exception()).when(esServices).lastValCover(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class), Mockito.anyInt());
	}
	
	
	@Test
	public void testSearchMetadata() throws Exception {
		SearchMetadataDto expectedResult = new SearchMetadataDto("name", "type", "kobs", "startDate", "stopDate");
		SearchMetadata response = new SearchMetadata();
		response.setProductName("name");
		response.setProductType("type");
		response.setKeyObjectStorage("kobs");
		response.setValidityStart("startDate");
		response.setValidityStop("stopDate");
		System.out.println(expectedResult);
		System.out.println(response);
		this.mockSearchMetadata(response);
		MvcResult result = request(get("/metadata/search?productType=type&mode=LatestValCover&satellite=satellite&t0=2017-12-08T12:45:23&t1=2017-12-08T13:02:19")).andExpect(MockMvcResultMatchers.status().isOk())
				.andReturn();
		assertEquals("Result is not returning the HTTP OK Status code", 200, result.getResponse().getStatus());
		assertEquals("Result is different from expected result", expectedResult.toString(), result.getResponse().getContentAsString());
	}
	
	@Test
	public void testSearchMetadataIsNULL() throws Exception {
		this.mockSearchMetadata(null);
		MvcResult result = request(get("/metadata/search?productType=type&mode=LatestValCover&satellite=satellite&t0=2017-12-08T12:45:23&t1=2017-12-08T13:02:19")).andExpect(MockMvcResultMatchers.status().isOk())
				.andReturn();
		assertEquals("Result is not returning the HTTP OK Status code", 200, result.getResponse().getStatus());
		assertEquals("Result is different from expected result", 0, result.getResponse().getContentLength());
	}

	@Test
	public void testSearchMetadataIsNotPresentException() throws Exception {
		this.mockSearchMetadataNotPresentException();
		MvcResult result = request(get("/metadata/search?productType=type&mode=LatestValCover&satellite=satellite&t0=2017-12-08T12:45:23&t1=2017-12-08T13:02:19")).andExpect(MockMvcResultMatchers.status().is4xxClientError())
				.andReturn();
		assertEquals("Result is not returning the HTTP NOT FOUND Status code", 400, result.getResponse().getStatus());
	}
	
	@Test
	public void testSearchMetadataBadMode() throws Exception {
		MvcResult result = request(get("/metadata/search?productType=type&mode=BADMODE&satellite=satellite&t0=startDate&t1=stopDate")).andExpect(MockMvcResultMatchers.status().is4xxClientError())
				.andReturn();
		assertEquals("Result is not returning the HTTP NOT FOUND Status code", 400, result.getResponse().getStatus());
	}
		
	@Test
	public void testSearchMetadataException() throws Exception {
		this.mockSearchMetadataException();
		MvcResult result = request(get("/metadata/search?productType=type&mode=LatestValCover&satellite=satellite&t0=startDate&t1=stopDate")).andExpect(MockMvcResultMatchers.status().is5xxServerError())
				.andReturn();
		assertEquals("Result is not returning the HTTP NOT FOUND Status code", 500, result.getResponse().getStatus());
	}
	
}
