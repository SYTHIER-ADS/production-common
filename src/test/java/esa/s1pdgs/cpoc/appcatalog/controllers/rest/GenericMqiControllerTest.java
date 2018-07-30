/**
 * 
 */
package esa.s1pdgs.cpoc.appcatalog.controllers.rest;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import esa.s1pdgs.cpoc.appcatalog.RestControllerTest;
import esa.s1pdgs.cpoc.appcatalog.model.MqiMessage;
import esa.s1pdgs.cpoc.appcatalog.rest.MqiGenericReadMessageDto;
import esa.s1pdgs.cpoc.appcatalog.rest.MqiSendMessageDto;
import esa.s1pdgs.cpoc.appcatalog.rest.MqiStateMessageEnum;
import esa.s1pdgs.cpoc.appcatalog.services.mongodb.MqiMessageService;
import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.mqi.model.queue.AuxiliaryFileDto;
import esa.s1pdgs.cpoc.mqi.model.rest.Ack;

/**
 * Test class for MqiAuxiliaryFile rest controller
 *
 * @author Viveris Technologies
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
public class GenericMqiControllerTest extends RestControllerTest {

    @Mock
    private MqiMessageService mongoDBServices;

    @Value("${mqi.max-retries}")
    private int maxRetries;

    private MqiAuxiliaryFileController controller;

    @Before
    public void init() throws IOException {
        MockitoAnnotations.initMocks(this);

        this.controller =
                new MqiAuxiliaryFileController(mongoDBServices, maxRetries);
        this.initMockMvc(this.controller);
    }

    private void mockSearchByTopicPartitionOffsetGroup(
            List<MqiMessage> message) {
        doReturn(message).when(mongoDBServices)
                .searchByTopicPartitionOffsetGroup(Mockito.anyString(),
                        Mockito.anyInt(), Mockito.anyLong(),
                        Mockito.anyString());
    }

    private void mockSearchByPodStateCategory(List<MqiMessage> message) {
        doReturn(message).when(mongoDBServices).searchByPodStateCategory(
                Mockito.anyString(), Mockito.any(ProductCategory.class),
                Mockito.any());
    }

    private void mockSearchByTopicPartitionGroup(List<MqiMessage> message) {
        doReturn(message).when(mongoDBServices).searchByTopicPartitionGroup(
                Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(),
                Mockito.any());
    }

    private void mockSearchByID(List<MqiMessage> message) {
        doReturn(message).when(mongoDBServices).searchByID(Mockito.anyLong());
    }

    private static String convertObjectToJsonString(Object dto)
            throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.writeValueAsString(dto);
    }

    @Test
    public void testReadMessageMqiMessageDontExists() throws Exception {
        doNothing().when(mongoDBServices)
                .insertMqiMessage(Mockito.any(MqiMessage.class));
        this.mockSearchByTopicPartitionOffsetGroup(new ArrayList<MqiMessage>());
        MqiGenericReadMessageDto<AuxiliaryFileDto> body =
                new MqiGenericReadMessageDto<AuxiliaryFileDto>("group",
                        "readingPod", false, null);
        request(post("/mqi/auxiliary_files/topic/1/5/read")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(convertObjectToJsonString(body)))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(content().contentType(
                                MediaType.APPLICATION_JSON_UTF8_VALUE));
        verify(mongoDBServices, times(1)).searchByTopicPartitionOffsetGroup(
                Mockito.anyString(), Mockito.anyInt(), Mockito.anyLong(),
                Mockito.anyString());
        verify(mongoDBServices, times(1))
                .insertMqiMessage(Mockito.any(MqiMessage.class));
    }

    @Test
    public void testReadMessageMqiMessageStateACK() throws Exception {
        MqiMessage message = new MqiMessage(ProductCategory.AUXILIARY_FILES,
                "topic", 1, 5, "group", MqiStateMessageEnum.ACK_OK,
                "readingPod", null, "sendingPod", null, null, 0, null);
        List<MqiMessage> response = new ArrayList<MqiMessage>();
        response.add(message);
        this.mockSearchByTopicPartitionOffsetGroup(response);
        MqiGenericReadMessageDto<AuxiliaryFileDto> body =
                new MqiGenericReadMessageDto<AuxiliaryFileDto>("group",
                        "readingPod", false, null);
        request(post("/mqi/auxiliary_files/topic/1/5/read")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(convertObjectToJsonString(body)))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(content().contentType(
                                MediaType.APPLICATION_JSON_UTF8_VALUE));
        verify(mongoDBServices, times(1)).searchByTopicPartitionOffsetGroup(
                Mockito.anyString(), Mockito.anyInt(), Mockito.anyLong(),
                Mockito.anyString());
        verify(mongoDBServices, never())
                .insertMqiMessage(Mockito.any(MqiMessage.class));
    }

    @Test
    public void testReadMessageMqiMessageForce() throws Exception {
        doNothing().when(mongoDBServices).updateByID(Mockito.anyLong(),
                Mockito.any());
        MqiMessage message = new MqiMessage(ProductCategory.AUXILIARY_FILES,
                "topic", 1, 5, "group", MqiStateMessageEnum.READ, "readingPod",
                null, "sendingPod", null, null, 0, null);
        List<MqiMessage> response = new ArrayList<MqiMessage>();
        response.add(message);
        this.mockSearchByTopicPartitionOffsetGroup(response);
        MqiGenericReadMessageDto<AuxiliaryFileDto> body =
                new MqiGenericReadMessageDto<AuxiliaryFileDto>("group",
                        "readingPod", true, null);
        request(post("/mqi/auxiliary_files/topic/1/5/read")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(convertObjectToJsonString(body)))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(content().contentType(
                                MediaType.APPLICATION_JSON_UTF8_VALUE));
        verify(mongoDBServices, times(1)).searchByTopicPartitionOffsetGroup(
                Mockito.anyString(), Mockito.anyInt(), Mockito.anyLong(),
                Mockito.anyString());
        verify(mongoDBServices, never())
                .insertMqiMessage(Mockito.any(MqiMessage.class));
        verify(mongoDBServices, times(1)).updateByID(Mockito.anyLong(),
                Mockito.any());
    }

    @Test
    public void testReadMessageMqiMessageForceRetryMax() throws Exception {
        doNothing().when(mongoDBServices).updateByID(Mockito.anyLong(),
                Mockito.any());
        MqiMessage message = new MqiMessage(ProductCategory.AUXILIARY_FILES,
                "topic", 1, 5, "group", MqiStateMessageEnum.READ, "readingPod",
                null, "sendingPod", null, null, 2, null);
        List<MqiMessage> response = new ArrayList<MqiMessage>();
        response.add(message);
        this.mockSearchByTopicPartitionOffsetGroup(response);
        MqiGenericReadMessageDto<AuxiliaryFileDto> body =
                new MqiGenericReadMessageDto<AuxiliaryFileDto>("group",
                        "readingPod", true, null);
        request(post("/mqi/auxiliary_files/topic/1/5/read")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(convertObjectToJsonString(body)))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(content().contentType(
                                MediaType.APPLICATION_JSON_UTF8_VALUE));
        verify(mongoDBServices, times(1)).searchByTopicPartitionOffsetGroup(
                Mockito.anyString(), Mockito.anyInt(), Mockito.anyLong(),
                Mockito.anyString());
        verify(mongoDBServices, never())
                .insertMqiMessage(Mockito.any(MqiMessage.class));
        verify(mongoDBServices, times(1)).updateByID(Mockito.anyLong(),
                Mockito.any());
    }

    @Test
    public void testReadMessageMqiMessageNoForceRead() throws Exception {
        doNothing().when(mongoDBServices).updateByID(Mockito.anyLong(),
                Mockito.any());
        MqiMessage message = new MqiMessage(ProductCategory.AUXILIARY_FILES,
                "topic", 1, 5, "group", MqiStateMessageEnum.READ, "readingPod",
                null, "sendingPod", null, null, 2, null);
        List<MqiMessage> response = new ArrayList<MqiMessage>();
        response.add(message);
        this.mockSearchByTopicPartitionOffsetGroup(response);
        MqiGenericReadMessageDto<AuxiliaryFileDto> body =
                new MqiGenericReadMessageDto<AuxiliaryFileDto>("group",
                        "readingPod", false, null);
        request(post("/mqi/auxiliary_files/topic/1/5/read")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(convertObjectToJsonString(body)))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(content().contentType(
                                MediaType.APPLICATION_JSON_UTF8_VALUE));
        verify(mongoDBServices, times(1)).searchByTopicPartitionOffsetGroup(
                Mockito.anyString(), Mockito.anyInt(), Mockito.anyLong(),
                Mockito.anyString());
        verify(mongoDBServices, never())
                .insertMqiMessage(Mockito.any(MqiMessage.class));
        verify(mongoDBServices, times(1)).updateByID(Mockito.anyLong(),
                Mockito.any());
    }

    @Test
    public void testReadMessageMqiMessageNoForceSend() throws Exception {
        doNothing().when(mongoDBServices).updateByID(Mockito.anyLong(),
                Mockito.any());
        MqiMessage message = new MqiMessage(ProductCategory.AUXILIARY_FILES,
                "topic", 1, 5, "group", MqiStateMessageEnum.SEND, "readingPod",
                null, "sendingPod", null, null, 2, null);
        List<MqiMessage> response = new ArrayList<MqiMessage>();
        response.add(message);
        this.mockSearchByTopicPartitionOffsetGroup(response);
        MqiGenericReadMessageDto<AuxiliaryFileDto> body =
                new MqiGenericReadMessageDto<AuxiliaryFileDto>("group",
                        "readingPod", false, null);
        request(post("/mqi/auxiliary_files/topic/1/5/read")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(convertObjectToJsonString(body)))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(content().contentType(
                                MediaType.APPLICATION_JSON_UTF8_VALUE));
        verify(mongoDBServices, times(1)).searchByTopicPartitionOffsetGroup(
                Mockito.anyString(), Mockito.anyInt(), Mockito.anyLong(),
                Mockito.anyString());
        verify(mongoDBServices, never())
                .insertMqiMessage(Mockito.any(MqiMessage.class));
        verify(mongoDBServices, times(1)).updateByID(Mockito.anyLong(),
                Mockito.any());
    }

    @Test
    public void testReadMessageWhenexception() throws Exception {
        doThrow(RuntimeException.class).when(mongoDBServices)
                .searchByTopicPartitionOffsetGroup(Mockito.anyString(),
                        Mockito.anyInt(), Mockito.anyLong(),
                        Mockito.anyString());
        MqiGenericReadMessageDto<AuxiliaryFileDto> body =
                new MqiGenericReadMessageDto<AuxiliaryFileDto>("group",
                        "readingPod", false, null);
        request(post("/mqi/auxiliary_files/topic/1/5/read")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(convertObjectToJsonString(body))).andExpect(
                        MockMvcResultMatchers.status().isInternalServerError());
    }

    @Test
    public void testNextMessageMqiMessageDontExists() throws Exception {
        this.mockSearchByPodStateCategory(new ArrayList<MqiMessage>());
        request(get("/mqi/auxiliary_files/next").param("pod", "readingPod"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content()
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        verify(mongoDBServices, times(1)).searchByPodStateCategory(
                Mockito.anyString(), Mockito.any(ProductCategory.class),
                Mockito.any());
    }

    @Test
    public void testNextMessageMqiMessage() throws Exception {
        List<MqiMessage> response = new ArrayList<MqiMessage>();
        response.add(new MqiMessage(ProductCategory.AUXILIARY_FILES, "topic", 1,
                5, "group", MqiStateMessageEnum.READ, "readingPod", null,
                "sendingPod", null, null, 2, null));
        response.add(new MqiMessage(ProductCategory.AUXILIARY_FILES, "topic", 1,
                8, "group", MqiStateMessageEnum.READ, "readingPod", null,
                "sendingPod", null, null, 2, null));
        response.add(new MqiMessage(ProductCategory.AUXILIARY_FILES, "topic", 1,
                18, "group", MqiStateMessageEnum.READ, "readingPod", null,
                "sendingPod", null, null, 2, null));
        this.mockSearchByPodStateCategory(response);
        request(get("/mqi/auxiliary_files/next").param("pod", "readingPod"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content()
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        verify(mongoDBServices, times(1)).searchByPodStateCategory(
                Mockito.anyString(), Mockito.any(ProductCategory.class),
                Mockito.any());
    }

    @Test
    public void testNextMessageWhenException() throws Exception {
        doThrow(RuntimeException.class).when(mongoDBServices)
                .searchByPodStateCategory(Mockito.anyString(), Mockito.any(),
                        Mockito.any());
        request(get("/mqi/auxiliary_files/next").param("pod", "readingPod"))
                .andExpect(
                        MockMvcResultMatchers.status().isInternalServerError());
    }

    @Test
    public void testSendMessageMqiMessageDontExists() throws Exception {
        this.mockSearchByID(new ArrayList<MqiMessage>());
        MqiSendMessageDto body = new MqiSendMessageDto("pod", false);
        request(post("/mqi/auxiliary_files/1/send")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(convertObjectToJsonString(body))).andExpect(
                        MockMvcResultMatchers.status().is4xxClientError());
        verify(mongoDBServices, times(1)).searchByID(Mockito.anyLong());
    }

    @Test
    public void testSendMessageMqiMessageACKOK() throws Exception {
        List<MqiMessage> response = new ArrayList<MqiMessage>();
        response.add(new MqiMessage(ProductCategory.AUXILIARY_FILES, "topic", 1,
                5, "group", MqiStateMessageEnum.ACK_OK, "readingPod", null,
                "sendingPod", null, null, 2, null));
        this.mockSearchByID(response);
        MqiSendMessageDto body = new MqiSendMessageDto("pod", false);
        request(post("/mqi/auxiliary_files/1/send")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(convertObjectToJsonString(body)))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(content().contentType(
                                MediaType.APPLICATION_JSON_UTF8_VALUE));
        verify(mongoDBServices, times(1)).searchByID(Mockito.anyLong());
    }

    @Test
    public void testSendMessageMqiMessageREAD() throws Exception {
        List<MqiMessage> response = new ArrayList<MqiMessage>();
        response.add(new MqiMessage(ProductCategory.AUXILIARY_FILES, "topic", 1,
                5, "group", MqiStateMessageEnum.READ, "readingPod", null,
                "sendingPod", null, null, 0, null));
        this.mockSearchByID(response);
        MqiSendMessageDto body = new MqiSendMessageDto("pod", false);
        request(post("/mqi/auxiliary_files/1/send")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(convertObjectToJsonString(body)))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(content().contentType(
                                MediaType.APPLICATION_JSON_UTF8_VALUE));
        verify(mongoDBServices, times(1)).searchByID(Mockito.anyLong());
        verify(mongoDBServices, times(1)).updateByID(Mockito.anyLong(),
                Mockito.any());
    }

    @Test
    public void testSendMessageMqiMessageRetry() throws Exception {
        List<MqiMessage> response = new ArrayList<MqiMessage>();
        response.add(new MqiMessage(ProductCategory.AUXILIARY_FILES, "topic", 1,
                5, "group", MqiStateMessageEnum.SEND, "readingPod", null,
                "sendingPod", null, null, 0, null));
        this.mockSearchByID(response);
        MqiSendMessageDto body = new MqiSendMessageDto("pod", false);
        request(post("/mqi/auxiliary_files/1/send")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(convertObjectToJsonString(body)))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(content().contentType(
                                MediaType.APPLICATION_JSON_UTF8_VALUE));
        verify(mongoDBServices, times(1)).searchByID(Mockito.anyLong());
        verify(mongoDBServices, times(1)).updateByID(Mockito.anyLong(),
                Mockito.any());
    }

    @Test
    public void testSendMessageMqiMessageRetryMax() throws Exception {
        List<MqiMessage> response = new ArrayList<MqiMessage>();
        response.add(new MqiMessage(ProductCategory.AUXILIARY_FILES, "topic", 1,
                5, "group", MqiStateMessageEnum.SEND, "readingPod", null,
                "sendingPod", null, null, 2, null));
        this.mockSearchByID(response);
        MqiSendMessageDto body = new MqiSendMessageDto("pod", false);
        request(post("/mqi/auxiliary_files/1/send")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(convertObjectToJsonString(body)))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(content().contentType(
                                MediaType.APPLICATION_JSON_UTF8_VALUE));
        verify(mongoDBServices, times(1)).searchByID(Mockito.anyLong());
        verify(mongoDBServices, times(1)).updateByID(Mockito.anyLong(),
                Mockito.any());
    }

    @Test
    public void testSendMessagWhenException() throws Exception {
        doThrow(RuntimeException.class).when(mongoDBServices)
                .searchByID(Mockito.anyLong());
        MqiSendMessageDto body = new MqiSendMessageDto("pod", false);
        request(post("/mqi/auxiliary_files/1/send")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(convertObjectToJsonString(body))).andExpect(
                        MockMvcResultMatchers.status().isInternalServerError());
    }

    @Test
    public void testAckMessageMqiMessageOK() throws Exception {
        doNothing().when(mongoDBServices).updateByID(Mockito.anyLong(),
                Mockito.any());
        MqiMessage message = new MqiMessage(ProductCategory.AUXILIARY_FILES,
                "topic", 1, 5, "group", MqiStateMessageEnum.READ, "readingPod",
                null, "sendingPod", null, null, 2, null);
        List<MqiMessage> response = new ArrayList<MqiMessage>();
        response.add(message);
        this.mockSearchByID(response);
        request(post("/mqi/auxiliary_files/1/ack")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(convertObjectToJsonString(Ack.OK)))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(content().contentType(
                                MediaType.APPLICATION_JSON_UTF8_VALUE));
        verify(mongoDBServices, times(1)).searchByID(Mockito.anyLong());
        verify(mongoDBServices, never())
                .insertMqiMessage(Mockito.any(MqiMessage.class));
        verify(mongoDBServices, times(1)).updateByID(Mockito.anyLong(),
                Mockito.any());
    }

    @Test
    public void testAckMessageMqiMessageError() throws Exception {
        doNothing().when(mongoDBServices).updateByID(Mockito.anyLong(),
                Mockito.any());
        MqiMessage message = new MqiMessage(ProductCategory.AUXILIARY_FILES,
                "topic", 1, 5, "group", MqiStateMessageEnum.ACK_KO,
                "readingPod", null, "sendingPod", null, null, 2, null);
        List<MqiMessage> response = new ArrayList<MqiMessage>();
        response.add(message);
        this.mockSearchByID(response);
        request(post("/mqi/auxiliary_files/1/ack")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(convertObjectToJsonString(Ack.ERROR)))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(content().contentType(
                                MediaType.APPLICATION_JSON_UTF8_VALUE));
        verify(mongoDBServices, times(1)).searchByID(Mockito.anyLong());
        verify(mongoDBServices, never())
                .insertMqiMessage(Mockito.any(MqiMessage.class));
        verify(mongoDBServices, times(1)).updateByID(Mockito.anyLong(),
                Mockito.any());
    }

    @Test
    public void testAckMessageMqiMessageWARN() throws Exception {
        doNothing().when(mongoDBServices).updateByID(Mockito.anyLong(),
                Mockito.any());
        MqiMessage message = new MqiMessage(ProductCategory.AUXILIARY_FILES,
                "topic", 1, 5, "group", MqiStateMessageEnum.ACK_WARN,
                "readingPod", null, "sendingPod", null, null, 2, null);
        List<MqiMessage> response = new ArrayList<MqiMessage>();
        response.add(message);
        this.mockSearchByID(response);
        request(post("/mqi/auxiliary_files/1/ack")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(convertObjectToJsonString(Ack.WARN)))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(content().contentType(
                                MediaType.APPLICATION_JSON_UTF8_VALUE));
        verify(mongoDBServices, times(1)).searchByID(Mockito.anyLong());
        verify(mongoDBServices, never())
                .insertMqiMessage(Mockito.any(MqiMessage.class));
        verify(mongoDBServices, times(1)).updateByID(Mockito.anyLong(),
                Mockito.any());
    }

    @Test
    public void testAckMessageWhenException() throws Exception {
        doThrow(RuntimeException.class).when(mongoDBServices)
                .updateByID(Mockito.anyLong(), Mockito.any());
        request(post("/mqi/auxiliary_files/1/ack")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(convertObjectToJsonString(Ack.WARN))).andExpect(
                        MockMvcResultMatchers.status().isInternalServerError());
    }

    @Test
    public void testEarliestOffsetMessageMqiMessageDontExists()
            throws Exception {
        this.mockSearchByTopicPartitionGroup(new ArrayList<MqiMessage>());
        request(get("/mqi/auxiliary_files/topic/1/earliestOffset")
                .param("group", "group"))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(content().contentType(
                                MediaType.APPLICATION_JSON_UTF8_VALUE));
        verify(mongoDBServices, times(1)).searchByTopicPartitionGroup(
                Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(),
                Mockito.any());
    }

    @Test
    public void testEarliestOffsetMessageMqiMessage() throws Exception {
        List<MqiMessage> response = new ArrayList<MqiMessage>();
        response.add(new MqiMessage(ProductCategory.AUXILIARY_FILES, "topic", 1,
                5, "group", MqiStateMessageEnum.READ, "readingPod", null,
                "sendingPod", null, null, 2, null));
        this.mockSearchByTopicPartitionGroup(response);
        request(get("/mqi/auxiliary_files/topic/1/earliestOffset")
                .param("group", "group"))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(content().contentType(
                                MediaType.APPLICATION_JSON_UTF8_VALUE));
        verify(mongoDBServices, times(1)).searchByTopicPartitionGroup(
                Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(),
                Mockito.any());
    }

    @Test
    public void testEarliestOffsetWhenException() throws Exception {
        doThrow(RuntimeException.class).when(mongoDBServices)
                .searchByTopicPartitionGroup(Mockito.anyString(),
                        Mockito.anyInt(), Mockito.anyString(), Mockito.any());
        request(get("/mqi/auxiliary_files/topic/1/earliestOffset")
                .param("group", "group")).andExpect(
                        MockMvcResultMatchers.status().isInternalServerError());
        verify(mongoDBServices, times(1)).searchByTopicPartitionGroup(
                Mockito.eq("topic"), Mockito.eq(1), Mockito.eq("group"),
                Mockito.any());
    }

    @Test
    public void testGetNbMessages() throws Exception {
        doReturn(2).when(mongoDBServices)
                .countReadingMessages(Mockito.anyString(), Mockito.anyString());

        request(get("/mqi/auxiliary_files/topic/nbReading?pod=pod-name"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(content().string("2"));

        verify(mongoDBServices, times(1)).countReadingMessages(
                Mockito.eq("pod-name"), Mockito.eq("topic"));
    }

    @Test
    public void testGetNbMessagesWhenException() throws Exception {
        doThrow(RuntimeException.class).when(mongoDBServices)
                .countReadingMessages(Mockito.anyString(), Mockito.anyString());

        request(get("/mqi/auxiliary_files/topic/nbReading?pod=pod-name"))
                .andExpect(
                        MockMvcResultMatchers.status().isInternalServerError());
        verify(mongoDBServices, times(1)).countReadingMessages(
                Mockito.eq("pod-name"), Mockito.eq("topic"));
    }
    
    @Test
    public void testGetWhenException() throws Exception {
        doThrow(RuntimeException.class).when(mongoDBServices).searchByID(Mockito.anyLong());

        request(get("/mqi/auxiliary_files/1234"))
                .andExpect(
                        MockMvcResultMatchers.status().isInternalServerError());
        verify(mongoDBServices, times(1)).searchByID(
                Mockito.eq(1234L));
    }
    
    @Test
    public void testGetWhenNoMEssage() throws Exception {
        doReturn(new ArrayList<>()).when(mongoDBServices).searchByID(Mockito.anyLong());

        request(get("/mqi/auxiliary_files/1234"))
                .andExpect(
                        MockMvcResultMatchers.status().isNotFound());
        verify(mongoDBServices, times(1)).searchByID(
                Mockito.eq(1234L));
    }
    
    @Test
    public void testGetWhenMEssages() throws Exception {
        MqiMessage message1 = new MqiMessage(ProductCategory.AUXILIARY_FILES,
                "topic", 1, 5, "group", MqiStateMessageEnum.ACK_WARN,
                "readingPod", null, "sendingPod", null, null, 2, null);
        MqiMessage message2 = new MqiMessage(ProductCategory.AUXILIARY_FILES,
                "topic", 1, 6, "group", MqiStateMessageEnum.ACK_WARN,
                "readingPod", null, "sendingPod", null, null, 2, null);
        doReturn(Arrays.asList(message1, message2)).when(mongoDBServices).searchByID(Mockito.anyLong());

        request(get("/mqi/auxiliary_files/1234"))
                .andExpect(
                        MockMvcResultMatchers.status().isOk());
        verify(mongoDBServices, times(1)).searchByID(
                Mockito.eq(1234L));
    }

}
