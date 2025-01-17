package esa.s1pdgs.cpoc.preparation.worker.type;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import esa.s1pdgs.cpoc.appcatalog.AppDataJob;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.errors.processing.IpfPrepWorkerInputsMissingException;
import esa.s1pdgs.cpoc.mqi.model.queue.IpfExecutionJob;
import esa.s1pdgs.cpoc.mqi.model.queue.IpfPreparationJob;
import esa.s1pdgs.cpoc.mqi.model.queue.util.CatalogEventAdapter;
import esa.s1pdgs.cpoc.preparation.worker.model.exception.DiscardedException;
import esa.s1pdgs.cpoc.preparation.worker.model.exception.TimedOutException;
import esa.s1pdgs.cpoc.preparation.worker.service.AppCatJobService;
import esa.s1pdgs.cpoc.preparation.worker.tasktable.adapter.TaskTableAdapter;
import esa.s1pdgs.cpoc.xml.model.joborder.JobOrder;

public interface ProductTypeAdapter {	
	Logger LOGGER = LogManager.getLogger(ProductTypeAdapter.class);
	
	default Product mainInputSearch(final AppDataJob job, final TaskTableAdapter tasktableAdpter) throws IpfPrepWorkerInputsMissingException, DiscardedException {
		return Product.nullProduct(job);
	}
	
	default void validateInputSearch(final AppDataJob job, final TaskTableAdapter tasktableAdpter) throws IpfPrepWorkerInputsMissingException, DiscardedException, TimedOutException {
		// default implementation: don't validate
	}
	
	List<AppDataJob> createAppDataJobs(final IpfPreparationJob job) throws Exception;
	
    void customJobOrder(final AppDataJob job, final JobOrder jobOrder);
	
    void customJobDto(final AppDataJob job, final IpfExecutionJob dto);	
    
	// default implementation. Only required for S1 special scenarios (session, segments)
	default Optional<AppDataJob> findAssociatedJobFor(
			final AppCatJobService appCat, 
			final CatalogEventAdapter catEvent,
			final AppDataJob job
    ) throws AbstractCodedException {
		return Optional.empty();
	}
}
