package fr.viveris.s1pdgs.jobgenerator.tasks.dispatcher;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import fr.viveris.s1pdgs.jobgenerator.config.JobGeneratorSettings;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.errors.InternalErrorException;
import esa.s1pdgs.cpoc.common.errors.processing.JobGenMissingRoutingEntryException;
import fr.viveris.s1pdgs.jobgenerator.model.Job;
import fr.viveris.s1pdgs.jobgenerator.model.l1routing.L1Routing;
import fr.viveris.s1pdgs.jobgenerator.model.product.L0Slice;
import fr.viveris.s1pdgs.jobgenerator.model.product.L0SliceProduct;
import fr.viveris.s1pdgs.jobgenerator.service.XmlConverter;
import fr.viveris.s1pdgs.jobgenerator.tasks.generator.AbstractJobsGenerator;
import fr.viveris.s1pdgs.jobgenerator.tasks.generator.JobsGeneratorFactory;

/**
 * Dispatcher of L0 slice product<br/>
 * 1 product to 1 or several task table<br/>
 * The routing is given in a XML file and is done by mapping the acquisition and
 * the mission and satellite identifier to a list of task tables
 * 
 * @author Cyrielle Gailliard
 */
@Service
@ConditionalOnProperty(prefix = "kafka.enable-consumer", name = "l0-slices")
public class L0SliceJobsDispatcher extends AbstractJobsDispatcher<L0Slice> {

	/**
	 * Logger
	 */
	private static final Logger LOGGER = LogManager.getLogger(L0SliceJobsDispatcher.class);

	/**
	 * XML converter
	 */
	private final XmlConverter xmlConverter;

	/**
	 * Routing table<br/>
	 * key = {acquisition_satelliteId} 'IW_A' -> {taskTable1.xml; TaskTable2.xml}
	 */
	protected final Map<String, List<String>> routingMap;

	/**
	 * Path of the routinh XML file
	 */
	protected final String pathRoutingXmlFile;

	/**
	 * 
	 * @param settings
	 * @param factory
	 * @param taskScheduler
	 * @param xmlConverter
	 * @param pathRoutingXmlFile
	 */
	@Autowired
	public L0SliceJobsDispatcher(final JobGeneratorSettings settings, final JobsGeneratorFactory factory,
			final ThreadPoolTaskScheduler taskScheduler, final XmlConverter xmlConverter,
			@Value("${level1.pathroutingxmlfile}") String pathRoutingXmlFile) {
		super(settings, factory, taskScheduler);
		this.xmlConverter = xmlConverter;
		this.routingMap = new HashMap<String, List<String>>();
		this.pathRoutingXmlFile = pathRoutingXmlFile;
	}

	/**
	 * 
	 * @throws AbstractCodedException
	 */
	@PostConstruct
	public void initialize() throws AbstractCodedException {
		// Init job generators from task tables
		super.initTaskTables();

		// Init the routing map from XML file located in the task table folder
		try {
			L1Routing routing = (L1Routing) xmlConverter.convertFromXMLToObject(this.pathRoutingXmlFile);
			if (routing != null && routing.getRoutes() != null) {
				routing.getRoutes().stream().forEach(route -> {
					String key = route.getRouteFrom().getAcquisition() + "_" + route.getRouteFrom().getSatelliteId();
					this.routingMap.put(key, route.getRouteTo().getTaskTables());
				});
			}
		} catch (IOException | JAXBException e) {
			throw new InternalErrorException(
					String.format("Cannot parse routing XML file located in %s", this.pathRoutingXmlFile), e);
		}
	}

	/**
	 * 
	 */
	@Override
	protected AbstractJobsGenerator<L0Slice> createJobGenerator(final File xmlFile) throws AbstractCodedException {
		return this.factory.createJobGeneratorForL0Slice(xmlFile);
	}

	/**
	 * 
	 */
	@Override
	public void dispatch(final Job<L0Slice> job) throws AbstractCodedException {
		String key = job.getProduct().getObject().getAcquisition() + "_" + job.getProduct().getSatelliteId();
		if (this.routingMap.containsKey(key)) {
			for (String taskTable : this.routingMap.get(key)) {
				if (this.generators.containsKey(taskTable)) {
					L0SliceProduct p = (L0SliceProduct) job.getProduct();
					L0SliceProduct pClone = new L0SliceProduct(p.getIdentifier(), p.getSatelliteId(), p.getMissionId(),
							p.getStartTime(), p.getStopTime(), new L0Slice(p.getObject().getAcquisition()));
					Job<L0Slice> cloneJob = new Job<>(pClone, job.getInputMessage());
					LOGGER.info("[MONITOR] [Step 2] [productName {}] [taskTable {}] Caching job",
							job.getProduct().getIdentifier(), taskTable);
					this.generators.get(taskTable).addJob(cloneJob);
				} else {
					LOGGER.warn("[MONITOR] [Step 2] [productName {}] Task table {} not found",
							job.getProduct().getIdentifier(), taskTable);
				}
			}
		} else {
			throw new JobGenMissingRoutingEntryException(String.format("No found routing entries for %s", key));
		}
	}

}
