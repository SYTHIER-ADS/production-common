/**
 * 
 */
package esa.s1pdgs.cpoc.appcatalog.server.mqi.db;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import esa.s1pdgs.cpoc.appcatalog.common.MqiMessage;
import esa.s1pdgs.cpoc.appcatalog.server.mqi.db.MqiMessageDao;
import esa.s1pdgs.cpoc.appcatalog.server.sequence.db.SequenceDao;
import esa.s1pdgs.cpoc.common.MessageState;
import esa.s1pdgs.cpoc.common.ProductCategory;

/**
 * Services to access mongoDB data
 * 
 * @author Viveris Technologies
 */
@Service
public class MqiMessageService {

    /**
     * 
     */
    public static final String MQI_MSG_SEQ_KEY = "mqiMessage";

    /**
     * DAO for mongoDB
     */
    private final MqiMessageDao mongoDBDAO;

    /**
     * DAO for mongoDB
     */
    private final SequenceDao sequenceDao;

    /**
     * Constructor for the Services
     * 
     * @param mongoDBDAO
     */
    @Autowired
    public MqiMessageService(final MqiMessageDao mongoDBDAO,
            final SequenceDao sequenceDao) {
        this.mongoDBDAO = mongoDBDAO;
        this.sequenceDao = sequenceDao;
    }

    /**
     * Return a list of message which contains the right topic, partition,
     * offset and group
     * 
     * @param topic
     * @param partition
     * @param offset
     * @param group
     * @return the list of message
     */
    public List<MqiMessage> searchByTopicPartitionOffsetGroup(
            final String topic, final int partition, final long offset,
            final String group) {
        return mongoDBDAO.searchByTopicPartitionOffsetGroup(topic, partition,
                offset, group);
    }

    /**
     * Return a list of message which contains the right topic, partition and
     * group
     * 
     * @param topic
     * @param partition
     * @param group
     * @return the list of message
     */
    public List<MqiMessage> searchByTopicPartitionGroup(final String topic,
            final int partition, final String group,
            final Set<MessageState> states) {
        return mongoDBDAO.searchByTopicPartitionGroup(topic, partition, group,
                states);
    }

    /**
     * Return a list of message which contains the right pod name, product
     * category but its not in the states
     * 
     * @param pod
     * @param category
     * @param states
     * @return the list of message
     */
    public List<MqiMessage> searchByPodStateCategory(final String pod,
            final ProductCategory category,
            final Set<MessageState> states) {
        return mongoDBDAO.searchByPodStateCategory(pod, category, states);
    }

    /**
     * Return a list of message which contains the right pod name, product
     * category but its not in the states
     * 
     * @param pod
     * @param category
     * @param states
     * @return the list of message
     */
    public int countReadingMessages(final String pod, final String topic) {
        return mongoDBDAO.countReadingMessages(pod, topic);
    }

    /**
     * Return a list of message which contains the right ID
     * 
     * @param messageID
     * @return the list of message
     */
    public List<MqiMessage> searchByID(final long messageID) {
        return mongoDBDAO.searchByID(messageID);
    }

    /**
     * Function which insert new message to the database
     * 
     * @param messageToInsert
     */
    public void insertMqiMessage(final MqiMessage messageToInsert) {
        long sequence = sequenceDao.getNextSequenceId(MQI_MSG_SEQ_KEY);
        messageToInsert.setId(sequence);
        messageToInsert.setIdentifier(sequence);
        mongoDBDAO.insert(messageToInsert);
    }

    /**
     * Function which update a MqiMessage
     * 
     * @param messageID
     * @param updateMap
     */
    public void updateByID(final long messageID,
            final Map<String, Object> updateMap) {
        mongoDBDAO.updateByID(messageID, updateMap);
    }

}
