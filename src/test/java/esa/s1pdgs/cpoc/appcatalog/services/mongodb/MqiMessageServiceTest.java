/**
 * 
 */
package esa.s1pdgs.cpoc.appcatalog.services.mongodb;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import esa.s1pdgs.cpoc.appcatalog.model.MqiMessage;
import esa.s1pdgs.cpoc.appcatalog.rest.MqiStateMessageEnum;
import esa.s1pdgs.cpoc.common.ProductCategory;

/**
 * Test class for MongoDB services class
 *
 * @author Viveris Technologies
 */
public class MqiMessageServiceTest {

    @Mock
    private MqiMessageDao mongoDBDAO;

    @Mock
    private SequenceDao sequenceDao;

    @InjectMocks
    private MqiMessageService mongoDBServices;

    /**
     * Initialization
     */
    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSearchByTopicPartitionOffsetGroup() {
        List<MqiMessage> response = new ArrayList<>();
        response.add(new MqiMessage(ProductCategory.AUXILIARY_FILES, "topic", 1,
                5, "group", MqiStateMessageEnum.READ, "readingPod", null,
                "sendingPod", null, null, 0, null));
        doReturn(response).when(mongoDBDAO).searchByTopicPartitionOffsetGroup(
                Mockito.anyString(), Mockito.anyInt(), Mockito.anyLong(),
                Mockito.anyString());

        List<MqiMessage> result = mongoDBServices
                .searchByTopicPartitionOffsetGroup("topic", 1, 5, "group");

        MqiMessage expectedResult =
                new MqiMessage(ProductCategory.AUXILIARY_FILES, "topic", 1, 5,
                        "group", MqiStateMessageEnum.READ, "readingPod", null,
                        "sendingPod", null, null, 0, null);

        assertEquals(expectedResult, result.get(0));
        verify(mongoDBDAO, times(1)).searchByTopicPartitionOffsetGroup(
                Mockito.eq("topic"), Mockito.eq(1), Mockito.eq(5L),
                Mockito.eq("group"));

    }

    @Test
    public void testSearchByTopicPartitionGroup() {
        List<MqiMessage> response = new ArrayList<>();
        response.add(new MqiMessage(ProductCategory.AUXILIARY_FILES, "topic", 1,
                5, "group", MqiStateMessageEnum.READ, "readingPod", null,
                "sendingPod", null, null, 0, null));
        response.add(new MqiMessage(ProductCategory.AUXILIARY_FILES, "topic", 1,
                8, "group", MqiStateMessageEnum.READ, "readingPod", null,
                "sendingPod", null, null, 0, null));
        response.add(new MqiMessage(ProductCategory.AUXILIARY_FILES, "topic", 1,
                18, "group", MqiStateMessageEnum.READ, "readingPod", null,
                "sendingPod", null, null, 0, null));
        doReturn(response).when(mongoDBDAO).searchByTopicPartitionGroup(
                Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(),
                Mockito.any());

        Set<MqiStateMessageEnum> ackStates = new HashSet<>();
        ackStates.add(MqiStateMessageEnum.ACK_KO);
        ackStates.add(MqiStateMessageEnum.ACK_OK);
        ackStates.add(MqiStateMessageEnum.ACK_WARN);
        List<MqiMessage> result = mongoDBServices
                .searchByTopicPartitionGroup("topic", 1, "group", ackStates);

        List<MqiMessage> expectedResult = new ArrayList<>();
        expectedResult.add(new MqiMessage(ProductCategory.AUXILIARY_FILES,
                "topic", 1, 5, "group", MqiStateMessageEnum.READ, "readingPod",
                null, "sendingPod", null, null, 0, null));
        expectedResult.add(new MqiMessage(ProductCategory.AUXILIARY_FILES,
                "topic", 1, 8, "group", MqiStateMessageEnum.READ, "readingPod",
                null, "sendingPod", null, null, 0, null));
        expectedResult.add(new MqiMessage(ProductCategory.AUXILIARY_FILES,
                "topic", 1, 18, "group", MqiStateMessageEnum.READ, "readingPod",
                null, "sendingPod", null, null, 0, null));

        assertEquals(expectedResult, result);
        verify(mongoDBDAO, times(1)).searchByTopicPartitionGroup(
                Mockito.eq("topic"), Mockito.eq(1), Mockito.eq("group"),
                Mockito.eq(ackStates));

    }

    @Test
    public void testSearchByPodStateCategory() {
        List<MqiMessage> response = new ArrayList<>();
        response.add(new MqiMessage(ProductCategory.AUXILIARY_FILES, "topic", 1,
                5, "group", MqiStateMessageEnum.READ, "readingPod", null,
                "sendingPod", null, null, 0, null));
        response.add(new MqiMessage(ProductCategory.AUXILIARY_FILES, "topic", 1,
                8, "group", MqiStateMessageEnum.READ, "readingPod", null,
                "sendingPod", null, null, 0, null));
        response.add(new MqiMessage(ProductCategory.AUXILIARY_FILES, "topic", 1,
                18, "group", MqiStateMessageEnum.READ, "readingPod", null,
                "sendingPod", null, null, 0, null));
        doReturn(response).when(mongoDBDAO).searchByPodStateCategory(
                Mockito.anyString(), Mockito.any(), Mockito.any());

        Set<MqiStateMessageEnum> ackStates = new HashSet<>();
        ackStates.add(MqiStateMessageEnum.ACK_KO);
        ackStates.add(MqiStateMessageEnum.ACK_OK);
        ackStates.add(MqiStateMessageEnum.ACK_WARN);
        List<MqiMessage> result = mongoDBServices.searchByPodStateCategory(
                "readingPod", ProductCategory.AUXILIARY_FILES, ackStates);

        List<MqiMessage> expectedResult = new ArrayList<>();
        expectedResult.add(new MqiMessage(ProductCategory.AUXILIARY_FILES,
                "topic", 1, 5, "group", MqiStateMessageEnum.READ, "readingPod",
                null, "sendingPod", null, null, 0, null));
        expectedResult.add(new MqiMessage(ProductCategory.AUXILIARY_FILES,
                "topic", 1, 8, "group", MqiStateMessageEnum.READ, "readingPod",
                null, "sendingPod", null, null, 0, null));
        expectedResult.add(new MqiMessage(ProductCategory.AUXILIARY_FILES,
                "topic", 1, 18, "group", MqiStateMessageEnum.READ, "readingPod",
                null, "sendingPod", null, null, 0, null));

        assertEquals(expectedResult, result);
        verify(mongoDBDAO, times(1)).searchByPodStateCategory(
                Mockito.eq("readingPod"),
                Mockito.eq(ProductCategory.AUXILIARY_FILES),
                Mockito.eq(ackStates));

    }

    @Test
    public void testSearchByID() {
        List<MqiMessage> response = new ArrayList<>();
        response.add(new MqiMessage(ProductCategory.AUXILIARY_FILES, "topic", 1,
                5, "group", MqiStateMessageEnum.READ, "readingPod", null,
                "sendingPod", null, null, 0, null));
        doReturn(response).when(mongoDBDAO).searchByID(Mockito.anyLong());

        List<MqiMessage> result = mongoDBServices.searchByID(1);

        MqiMessage expectedResult =
                new MqiMessage(ProductCategory.AUXILIARY_FILES, "topic", 1, 5,
                        "group", MqiStateMessageEnum.READ, "readingPod", null,
                        "sendingPod", null, null, 0, null);

        assertEquals(expectedResult, result.get(0));
        verify(mongoDBDAO, times(1)).searchByID(Mockito.eq(1L));

    }

    @Test
    public void testInsertMqiMessage() {
        MqiMessage messageToInsert =
                new MqiMessage(ProductCategory.AUXILIARY_FILES, "topic", 1, 5,
                        "group", MqiStateMessageEnum.READ, "readingPod", null,
                        "sendingPod", null, null, 0, null);
        doNothing().when(mongoDBDAO).insert(Mockito.any(MqiMessage.class));
        doReturn(2L).when(sequenceDao).getNextSequenceId(Mockito.anyString());

        mongoDBServices.insertMqiMessage(messageToInsert);

        verify(mongoDBDAO, times(1)).insert(Mockito.any(MqiMessage.class));
        verify(sequenceDao, times(1))
                .getNextSequenceId(Mockito.eq(MqiMessageService.MQI_MSG_SEQ_KEY));

    }

    @Test
    public void testUpdateByID() {
        doNothing().when(mongoDBDAO).updateByID(Mockito.anyLong(),
                Mockito.any());

        mongoDBServices.updateByID(1, new HashMap<>());

        verify(mongoDBDAO, times(1)).updateByID(Mockito.eq(1L),
                Mockito.eq(new HashMap<>()));

    }
}
