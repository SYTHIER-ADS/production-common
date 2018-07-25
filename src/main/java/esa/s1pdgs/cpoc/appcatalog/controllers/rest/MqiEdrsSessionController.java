/**
 * 
 */
package esa.s1pdgs.cpoc.appcatalog.controllers.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import esa.s1pdgs.cpoc.appcatalog.model.MqiMessage;
import esa.s1pdgs.cpoc.appcatalog.rest.MqiEdrsSessionMessageDto;
import esa.s1pdgs.cpoc.appcatalog.rest.MqiGenericReadMessageDto;
import esa.s1pdgs.cpoc.appcatalog.rest.MqiLightMessageDto;
import esa.s1pdgs.cpoc.appcatalog.rest.MqiSendMessageDto;
import esa.s1pdgs.cpoc.appcatalog.rest.MqiStateMessageEnum;
import esa.s1pdgs.cpoc.appcatalog.services.mongodb.MongoDBServices;
import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.mqi.model.queue.EdrsSessionDto;
import esa.s1pdgs.cpoc.mqi.model.rest.Ack;
import esa.s1pdgs.cpoc.mqi.model.rest.AckMessageDto;

/**
 * Rest server for ERDS Session file
 * 
 * @author Viveris Technologies
 */
@RestController
@RequestMapping(path = "/mqi/edrs_session")
public class MqiEdrsSessionController {

    private static final Logger LOGGER = LogManager.getLogger(MqiEdrsSessionController.class);
    
    private final MongoDBServices mongoDBServices;
    
    private final int maxRetries;

    @Autowired
    public MqiEdrsSessionController(final MongoDBServices mongoDBServices, 
            @Value("${mqi.max-retries}") final int maxRetries) {
        this.mongoDBServices = mongoDBServices;
        this.maxRetries = maxRetries;
    }    

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, 
            path = "/{topic}/{partition}/{offset}/read")
    public ResponseEntity<MqiLightMessageDto> readMessage(@PathVariable(name = "topic") String topic, 
            @PathVariable(name = "partition") int partition, @PathVariable(name = "offset") long offset, 
            @RequestBody MqiGenericReadMessageDto<EdrsSessionDto> body) {
        
        LOGGER.info("test");
        List<MqiMessage> responseFromDB = 
                mongoDBServices.searchByTopicPartitionOffsetGroup(topic, partition, offset, body.getGroup());
        
        //Si un objet n'existe pas dans la BDD avec topic / partition / offset / group
        if(responseFromDB.isEmpty()) {
            //On créer le message dans la BDD
            MqiMessage messageToInsert = new MqiMessage(ProductCategory.EDRS_SESSIONS, 
                    topic, partition, offset, body.getGroup(), MqiStateMessageEnum.READ, 
                    body.getPod(), new Date(), null, null, null, 0, body.getDto());
            mongoDBServices.insertMqiMessage(messageToInsert);
            
            //On renvoie le message que l'on vient de créer
            return new ResponseEntity<MqiLightMessageDto>(transformMqiMessageToMqiLightMessage(messageToInsert), HttpStatus.OK);
        } else { //Sinon on récupère le premier de la liste
            MqiMessage messageFromDB = responseFromDB.get(0);
            //Si l'état est à ACK
            if(messageFromDB.getState().equals(MqiStateMessageEnum.ACK_OK) || 
                    messageFromDB.getState().equals(MqiStateMessageEnum.ACK_KO) ||
                    messageFromDB.getState().equals(MqiStateMessageEnum.ACK_WARN)) {
                //on renvoie l’objet
                return new ResponseEntity<MqiLightMessageDto>(transformMqiMessageToMqiLightMessage(messageFromDB), HttpStatus.OK);
            } else if(body.isForce()) { // sinon si force = true
                HashMap<String, Object> updateMap = new HashMap<>();
                // on incrémente nb_retry
                messageFromDB.setNbRetries(messageFromDB.getNbRetries() + 1);
                updateMap.put("nbRetries", messageFromDB.getNbRetries());
                if(messageFromDB.getNbRetries() == maxRetries) {
                    // on publie un message d’erreur dans queue (via mqi du catalogue)
                    //TODO 
                    // on met status = ACK_KO
                    messageFromDB.setState(MqiStateMessageEnum.ACK_KO);
                    updateMap.put("state", messageFromDB.getState());
                    // on met à jour les éventuelles dates
                    Date now = new Date();
                    messageFromDB.setLastAckDate(now);
                    messageFromDB.setLastReadDate(now);
                    updateMap.put("lastAckDate", now);
                    updateMap.put("lastReadDate", now);
                    // Modifier l'objet dans la bdd
                    mongoDBServices.updateByID(messageFromDB.getIdentifier() ,updateMap);
                    // on renvoie l’objet
                    return new ResponseEntity<MqiLightMessageDto>(transformMqiMessageToMqiLightMessage(messageFromDB), HttpStatus.OK);     
                } else {
                    // on met status = READ
                    messageFromDB.setState(MqiStateMessageEnum.READ);
                    updateMap.put("state", messageFromDB.getState());
                    // on met le reading_pod au pod recu
                    messageFromDB.setReadingPod(body.getPod());
                    updateMap.put("readingPod", messageFromDB.getReadingPod());
                    // on met le processing_pod à null
                    messageFromDB.setSendingPod(null);
                    updateMap.put("sendingPod", messageFromDB.getSendingPod());
                    // on met à jour les éventuelles dates
                    Date now = new Date();
                    messageFromDB.setLastSendPod(now);
                    messageFromDB.setLastReadDate(now);
                    updateMap.put("lastSendPod", now);
                    updateMap.put("lastReadDate", now);
                    // Modifier l'objet dans la bdd
                    mongoDBServices.updateByID(messageFromDB.getIdentifier() ,updateMap);
                    // on renvoie l’objet
                    return new ResponseEntity<MqiLightMessageDto>(transformMqiMessageToMqiLightMessage(messageFromDB), HttpStatus.OK);
                }
            } else {
                HashMap<String, Object> updateMap = new HashMap<>();
                if(messageFromDB.getState().equals(MqiStateMessageEnum.READ)) {
                    // on met à jour les éventuelles dates et le reading_pod
                    Date now = new Date();
                    messageFromDB.setLastReadDate(now);
                    updateMap.put("lastReadDate", now);
                    messageFromDB.setReadingPod(body.getPod());
                    updateMap.put("readingPod", messageFromDB.getReadingPod());                    
                    // Modifier l'objet dans la bdd
                    mongoDBServices.updateByID(messageFromDB.getIdentifier() ,updateMap);
                    // on renvoie l’objet
                    return new ResponseEntity<MqiLightMessageDto>(transformMqiMessageToMqiLightMessage(messageFromDB), HttpStatus.OK);
                }
                if(messageFromDB.getState().equals(MqiStateMessageEnum.SEND)) {
                 // on met à jour les éventuelles dates et le reading_pod
                    Date now = new Date();
                    messageFromDB.setLastSendPod(now);
                    updateMap.put("lastSendPod", now);
                    messageFromDB.setReadingPod(body.getPod());
                    updateMap.put("readingPod", messageFromDB.getReadingPod());                    
                    // Modifier l'objet dans la bdd
                    mongoDBServices.updateByID(messageFromDB.getIdentifier() ,updateMap);
                    // on renvoie l’objet
                    return new ResponseEntity<MqiLightMessageDto>(transformMqiMessageToMqiLightMessage(messageFromDB), HttpStatus.OK);
                }
            }
        }
        return new ResponseEntity<MqiLightMessageDto>(HttpStatus.NOT_FOUND);
        
    }
    
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, path = "/next")
    public ResponseEntity<List<MqiEdrsSessionMessageDto>> next(@RequestParam("pod") String pod) {
        //TODO : modify search function when we will use priority
        Set<MqiStateMessageEnum> ackStates = new HashSet<>();
        ackStates.add(MqiStateMessageEnum.ACK_KO);
        ackStates.add(MqiStateMessageEnum.ACK_OK);
        ackStates.add(MqiStateMessageEnum.ACK_WARN);
        List<MqiMessage> mqiMessages  = mongoDBServices.searchByPodStateCategory(pod,
                ProductCategory.AUXILIARY_FILES, ackStates);
        if(mqiMessages.isEmpty()) {
            return new ResponseEntity<List<MqiEdrsSessionMessageDto>>(HttpStatus.NOT_FOUND);
        } else {
            List<MqiEdrsSessionMessageDto> messagesToReturn = new ArrayList<>();
            mqiMessages.forEach(x-> messagesToReturn.add(transformMqiMessageToMqiMqiEdrsSessionMessage(x)));
            return new ResponseEntity<List<MqiEdrsSessionMessageDto>>(messagesToReturn, HttpStatus.OK);
        }
    }
    
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, 
            path = "/{messageID}/send")
    public ResponseEntity<Boolean> sendMessage(@PathVariable(name = "messageID") long messageID, 
            @RequestBody MqiSendMessageDto body) {
        
List<MqiMessage> responseFromDB = mongoDBServices.searchByID(messageID);
        
        if(responseFromDB.isEmpty()) {
            return new ResponseEntity<Boolean>(HttpStatus.NOT_FOUND);
        } else { // Si le message existe
            MqiMessage messageFromDB = responseFromDB.get(0);
            if(messageFromDB.getState().equals(MqiStateMessageEnum.ACK_OK)) {
                return new ResponseEntity<Boolean>(Boolean.FALSE, HttpStatus.OK);
            } else if(messageFromDB.getState().equals(MqiStateMessageEnum.READ)) {
                HashMap<String, Object> updateMap = new HashMap<>();
                // on met status à SEND et son processing_pod
                messageFromDB.setState(MqiStateMessageEnum.ACK_KO);
                updateMap.put("state", messageFromDB.getState());
                // on met à jour les éventuelles dates
                Date now = new Date();
                messageFromDB.setLastAckDate(now);
                messageFromDB.setLastSendPod(now);
                updateMap.put("lastAckDate", now);
                updateMap.put("lastSendPod", now);
                mongoDBServices.updateByID(messageID, updateMap);
                return new ResponseEntity<Boolean>(Boolean.TRUE, HttpStatus.OK);
            } else {
                HashMap<String, Object> updateMap = new HashMap<>();
                // on incrémente nb_retry
                messageFromDB.setNbRetries(messageFromDB.getNbRetries() + 1);
                updateMap.put("nbRetries", messageFromDB.getNbRetries());
                if(messageFromDB.getNbRetries() == maxRetries) {
                    // on publie un message d’erreur dans queue (via mqi du catalogue)
                    //TODO
                    // on met status = ACK_KO
                    messageFromDB.setState(MqiStateMessageEnum.ACK_KO);
                    updateMap.put("state", messageFromDB.getState());
                    // on met à jour les éventuelles dates
                    Date now = new Date();
                    messageFromDB.setLastAckDate(now);
                    messageFromDB.setLastSendPod(now);
                    updateMap.put("lastAckDate", now);
                    updateMap.put("lastSendPod", now);
                    mongoDBServices.updateByID(messageID, updateMap);
                    return new ResponseEntity<Boolean>(Boolean.FALSE, HttpStatus.OK);
                } else {
                    // on met status = à SEND et son processing_pod
                    messageFromDB.setState(MqiStateMessageEnum.SEND);
                    updateMap.put("state", messageFromDB.getState());
                    // on met à jour les éventuelles dates
                    Date now = new Date();
                    messageFromDB.setLastSendPod(now);
                    updateMap.put("lastSendPod", now);
                    mongoDBServices.updateByID(messageID, updateMap);
                    return new ResponseEntity<Boolean>(Boolean.TRUE, HttpStatus.OK);
                }
            }
        }
    }
    
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, 
            path = "/{messageID}/ack")
    public ResponseEntity<MqiEdrsSessionMessageDto> ackMessage(@PathVariable(name = "messageID") long messageID,
            @RequestBody AckMessageDto ackMessageDto) {
        
        HashMap<String, Object> updateMap = new HashMap<>();
        if(ackMessageDto.getAck().equals(Ack.OK)) {
            updateMap.put("state", MqiStateMessageEnum.ACK_OK);
        } else if(ackMessageDto.getAck().equals(Ack.ERROR)) {
            updateMap.put("state", MqiStateMessageEnum.ACK_KO);
        } else if(ackMessageDto.getAck().equals(Ack.WARN)) {
            updateMap.put("state", MqiStateMessageEnum.ACK_WARN);
        } else {
            return new ResponseEntity<MqiEdrsSessionMessageDto>(HttpStatus.NOT_FOUND);
        }
        mongoDBServices.updateByID(messageID, updateMap);
        List<MqiMessage> responseFromDB = mongoDBServices.searchByID(messageID);
        //on met le status à ak_ok ou ack_ko
        
        return new ResponseEntity<MqiEdrsSessionMessageDto>(
                transformMqiMessageToMqiMqiEdrsSessionMessage(responseFromDB.get(0)), HttpStatus.OK);
    }
    
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, 
            path = "/{topic}/{partition}/earliestOffset")
    public ResponseEntity<Long> earliestOffset(@PathVariable(name = "topic") String topic, 
            @PathVariable(name = "partition") int partition, @RequestParam("group") String group) {
        
        // Pour le topic / partition / group donné, on récupère l’offset du message avec status != ACK et la plus petite date de lecture (à voir si on prend le plus petit offset)
        List<MqiMessage> responseFromDB = mongoDBServices.searchByTopicPartitionGroup(topic, partition, group);
        if(responseFromDB.isEmpty()) {
            //TODO define the strategy
            // Si pas d’entrée, on renvoie valeur par défaut :
            // -2 : on laisse le consumer faire ce qu’il veut
            // -1 : on démarre à l’offset du début
            // 0 : on démarre à l’offset de fin
            return new ResponseEntity<Long>(Long.valueOf(0), HttpStatus.OK);
        } else {
            return new ResponseEntity<Long>(responseFromDB.get(0).getOffset(), HttpStatus.OK);
        }
    }
    
    private MqiLightMessageDto transformMqiMessageToMqiLightMessage(MqiMessage messageToTransform) {
        MqiLightMessageDto messageTransformed = new MqiLightMessageDto();
        messageTransformed.setCategory(messageToTransform.getCategory());
        messageTransformed.setGroup(messageToTransform.getGroup());
        messageTransformed.setIdentifier(messageToTransform.getIdentifier());
        messageTransformed.setLastAckDate(messageToTransform.getLastAckDate());
        messageTransformed.setLastReadDate(messageToTransform.getLastReadDate());
        messageTransformed.setLastSendDate(messageToTransform.getLastSendPod());
        messageTransformed.setNbRetries(messageToTransform.getNbRetries());
        messageTransformed.setOffset(messageToTransform.getOffset());
        messageTransformed.setPartition(messageToTransform.getPartition());
        messageTransformed.setReadingPod(messageToTransform.getReadingPod());
        messageTransformed.setSendingPod(messageToTransform.getSendingPod());
        messageTransformed.setState(messageToTransform.getState());
        messageTransformed.setTopic(messageToTransform.getTopic());
        return messageTransformed;
    }
    
    private MqiEdrsSessionMessageDto transformMqiMessageToMqiMqiEdrsSessionMessage(MqiMessage messageToTransform) {
        MqiEdrsSessionMessageDto messageTransformed = new MqiEdrsSessionMessageDto();
        messageTransformed.setCategory(messageToTransform.getCategory());
        messageTransformed.setGroup(messageToTransform.getGroup());
        messageTransformed.setIdentifier(messageToTransform.getIdentifier());
        messageTransformed.setLastAckDate(messageToTransform.getLastAckDate());
        messageTransformed.setLastReadDate(messageToTransform.getLastReadDate());
        messageTransformed.setLastSendDate(messageToTransform.getLastSendPod());
        messageTransformed.setNbRetries(messageToTransform.getNbRetries());
        messageTransformed.setOffset(messageToTransform.getOffset());
        messageTransformed.setPartition(messageToTransform.getPartition());
        messageTransformed.setReadingPod(messageToTransform.getReadingPod());
        messageTransformed.setSendingPod(messageToTransform.getSendingPod());
        messageTransformed.setState(messageToTransform.getState());
        messageTransformed.setTopic(messageToTransform.getTopic());
        messageTransformed.setDto((EdrsSessionDto) messageToTransform.getDto());
        return messageTransformed;
    }
}
