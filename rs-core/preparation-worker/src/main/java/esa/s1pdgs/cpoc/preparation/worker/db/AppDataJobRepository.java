package esa.s1pdgs.cpoc.preparation.worker.db;

import java.util.Date;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Service;

import esa.s1pdgs.cpoc.appcatalog.AppDataJob;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobGenerationState;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobState;
import esa.s1pdgs.cpoc.common.ApplicationLevel;

/**
 * Access class to AppDataJob in mongo DB
 * 
 * @author Viveris Technologies
 */
@Service
public interface AppDataJobRepository extends MongoRepository<AppDataJob, Long> {
	@Query(value = "{ 'catalogEvents.uid' : ?0, 'pod' : ?1, 'state' : { $ne: 'TERMINATED' } }")
	List<AppDataJob> findByCatalogEventsUid(final String uid, final String podName);

	@Query(value = "{ 'product.metadata.dataTakeId' : ?0, 'product.metadata.productType' : { $ne: 'RF_RAW__0S' }, 'state' : { $ne: 'TERMINATED' } }")
	List<AppDataJob> findByProductDataTakeId_NonRfc(final String dataTakeId);

	@Query(value = "{ 'product.metadata.dataTakeId' : ?0, 'product.metadata.productType' : 'RF_RAW__0S', 'state' : { $ne: 'TERMINATED' } }")
	List<AppDataJob> findByProductDataTakeId_Rfc(final String dataTakeId);

	@Query(value = "{ 'productName' : { $regex : ?0 }, 'pod' : ?1, 'state' : { $ne: 'TERMINATED' } }")
	List<AppDataJob> findByProductType(final String productType, final String podName);

	@Query(value = "{ 'triggerProducts' : ?0, 'pod' : ?1 }")
	List<AppDataJob> findByTriggerProduct(final String productType, final String podName);

	@Query(value = "{ 'product.metadata.sessionId' : ?0, 'state' : { $ne: 'TERMINATED' } }")
	List<AppDataJob> findByProductSessionId(final String sessionId);
}
