package esa.s1pdgs.cpoc.appcatalog.services.mongodb;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Component that will clean the mongoDB entries that are older
 * Configurable 
 *
 * @author Viveris Technologies
 */
@Component
public class MongoDBClean {

    /**
     * DAO for mongoDB
     */
    private MongoDBDAO mongoDBDAO;
    
    /**
     * Second after an entry is considered old
     */
    private int oldms;
    
    /**
     * Constructor for the Services
     * 
     * @param mongoDBDAO
     */
    @Autowired
    public MongoDBClean(final MongoDBDAO mongoDBDAO,
            @Value("${mongodb.old-entry-ms}") int oldms) {
        this.mongoDBDAO = mongoDBDAO;
        this.oldms = oldms;
    }
    
    /**
     * Function that will clean all the old message (configurable to be launch using cron notaion)
     */
    @Scheduled(cron = "${mongodb.clean-cron}")
    public void clean() {
        Date oldDate = new Date(System.currentTimeMillis() - oldms);
        Query query = query(where("lastReadDate").lt(oldDate).and("lastSendPod").lt(oldDate)
                .and("lastAckDate").lt(oldDate));
        mongoDBDAO.findAllAndRemove(query);
    }
    
    
}
