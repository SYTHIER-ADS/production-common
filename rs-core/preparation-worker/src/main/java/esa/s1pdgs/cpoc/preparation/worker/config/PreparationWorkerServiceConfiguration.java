package esa.s1pdgs.cpoc.preparation.worker.config;

import java.util.List;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import esa.s1pdgs.cpoc.mqi.model.queue.CatalogEvent;
import esa.s1pdgs.cpoc.mqi.model.queue.IpfExecutionJob;
import esa.s1pdgs.cpoc.preparation.worker.service.AppCatJobService;
import esa.s1pdgs.cpoc.preparation.worker.service.PreparationWorkerService;
import esa.s1pdgs.cpoc.preparation.worker.service.TaskTableMapperService;

/**
 * Configuration class containing the interface for Spring Cloud Dataflow.
 */
@Configuration
public class PreparationWorkerServiceConfiguration {

	@Autowired
	private ProcessProperties processProperties;
	
	@Autowired	
	private TaskTableMapperService taskTableMapperService;
	
	@Autowired
	private AppCatJobService appCatJobService;
	
	@Bean
	public Function<CatalogEvent, List<IpfExecutionJob>> prepareExecutionJobs() {
		return new PreparationWorkerService(taskTableMapperService, null, processProperties, appCatJobService);
	}
}