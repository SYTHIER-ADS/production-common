package esa.s1pdgs.cpoc.preparation.worker.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import esa.s1pdgs.cpoc.appcatalog.AppDataJob;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobGeneration;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobGenerationState;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobState;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.utils.CollectionUtil;
import esa.s1pdgs.cpoc.common.utils.LogUtils;
import esa.s1pdgs.cpoc.metadata.model.MissionId;
import esa.s1pdgs.cpoc.mqi.model.queue.CatalogEvent;
import esa.s1pdgs.cpoc.mqi.model.queue.IpfExecutionJob;
import esa.s1pdgs.cpoc.mqi.model.queue.IpfPreparationJob;
import esa.s1pdgs.cpoc.mqi.model.queue.util.CatalogEventAdapter;
import esa.s1pdgs.cpoc.preparation.worker.config.ProcessProperties;
import esa.s1pdgs.cpoc.preparation.worker.report.TaskTableLookupReportingOutput;
import esa.s1pdgs.cpoc.preparation.worker.type.ProductTypeAdapter;
import esa.s1pdgs.cpoc.report.Reporting;
import esa.s1pdgs.cpoc.report.ReportingMessage;
import esa.s1pdgs.cpoc.report.ReportingUtils;

public class PreparationWorkerService implements Function<CatalogEvent, List<IpfExecutionJob>> {

	static final Logger LOGGER = LogManager.getLogger(PreparationWorkerService.class);

	private TaskTableMapperService taskTableService;

	private ProductTypeAdapter typeAdapter;

	private ProcessProperties processProperties;

	private AppCatJobService appCatJobService;

	public PreparationWorkerService(final TaskTableMapperService taskTableService, final ProductTypeAdapter typeAdapter,
			final ProcessProperties properties, final AppCatJobService appCat) {
		this.taskTableService = taskTableService;
		this.typeAdapter = typeAdapter;
		this.processProperties = properties;
		this.appCatJobService = appCat;
	}

	@Override
	public List<IpfExecutionJob> apply(CatalogEvent catalogEvent) {
		final Reporting reporting = ReportingUtils
				.newReportingBuilder(MissionId.fromFileName(catalogEvent.getKeyObjectStorage()))
				.predecessor(catalogEvent.getUid()).newReporting("PreparationWorkerService");

		reporting.begin(
				ReportingUtils.newFilenameReportingInputFor(catalogEvent.getProductFamily(),
						catalogEvent.getProductName()),
				new ReportingMessage("Check if any jobs can be finalized for the IPF"));

		try {
			// Map event to tasktables
			List<IpfPreparationJob> preparationJobs = taskTableService.mapEventToTaskTables(catalogEvent, reporting);

			// Create new Jobs
			for (IpfPreparationJob preparationJob : preparationJobs) {
				dispatch(preparationJob);
			}

			// Find matching jobs

			// Check if jobs are ready

			// Save new status
		} catch (Exception e) {
			reporting.error(new ReportingMessage("Preparation worker failed: %s", LogUtils.toString(e)));
			throw new RuntimeException(e);
		}

		reporting.end(null, new ReportingMessage("End preparation of new execution jobs"));

		return new ArrayList<>();
	}

	public final List<AppDataJob> dispatch(final IpfPreparationJob preparationJob) throws Exception {

		MissionId mission = MissionId
				.valueOf((String) preparationJob.getCatalogEvent().getMetadata().get(MissionId.FIELD_NAME));

		final Reporting reporting = ReportingUtils.newReportingBuilder(mission).predecessor(preparationJob.getUid())
				.newReporting("TaskTableLookup");

		final List<AppDataJob> jobs = typeAdapter.createAppDataJobs(preparationJob);

		reporting.begin(
				ReportingUtils.newFilenameReportingInputFor(preparationJob.getProductFamily(),
						preparationJob.getKeyObjectStorage()),
				new ReportingMessage("Start associating TaskTables to created AppDataJobs"));

		List<AppDataJob> result = new ArrayList<>();
		try {
			if (CollectionUtil.isNotEmpty(jobs)) {
				final AppDataJob firstJob = jobs.get(0);

				final String tasktableFilename = firstJob.getTaskTableName();

				LOGGER.trace("Got TaskTable {}", tasktableFilename);

				result = handleJobs(preparationJob, jobs, reporting.getUid(), tasktableFilename);
			}
		} catch (Exception e) {
			reporting.error(
					new ReportingMessage("Error associating TaskTables to AppDataJobs: %s", LogUtils.toString(e)));
		}

		reporting.end(new TaskTableLookupReportingOutput(Collections.singletonList(preparationJob.getTaskTableName())),
				new ReportingMessage("End associating TaskTables to AppDataJobs"));

		return result;
	}

	// This needs to be synchronized to avoid duplicate jobs
	private final synchronized List<AppDataJob> handleJobs(final IpfPreparationJob preparationJob,
			final List<AppDataJob> jobsFromMessage, final UUID reportingUid, final String tasktableFilename)
			throws AbstractCodedException {
		final AppDataJob firstJob = jobsFromMessage.get(0);

		final CatalogEvent firstEvent = firstJob.getCatalogEvents().get(0);
		final List<AppDataJob> jobForMess = appCatJobService.findByCatalogEventsUid(firstEvent.getUid());

		List<AppDataJob> dispatchedJobs = new ArrayList<>();

		// there is already a job for this message --> possible restart scenario -->
		// just update the pod name
		if (!jobForMess.isEmpty() && getJobMatchingTasktable(jobForMess, tasktableFilename) != null) {
			final AppDataJob job = getJobMatchingTasktable(jobForMess, tasktableFilename);
			LOGGER.warn("Found job {} already associated to catalogEvent {}. Ignoring new message ...", job.getId(),
					firstEvent.getUid());
		} else {
			// no job yet associated to this message --> check special cases otherwise
			// create and persist
			final CatalogEventAdapter eventAdapter = CatalogEventAdapter.of(firstEvent);
			for (final AppDataJob job : jobsFromMessage) {
				final Optional<AppDataJob> specificJob = typeAdapter.findAssociatedJobFor(appCatJobService,
						eventAdapter, job);

				if (specificJob.isPresent()) {
					final AppDataJob existingJob = specificJob.get();
					LOGGER.info("Found job {} already being handled. Appending new event {} ...", existingJob.getId(),
							firstEvent.getUid());
					appCatJobService.appendCatalogEvent(existingJob.getId(), firstEvent);
				} else {
					LOGGER.debug("Persisting new job for preparation job {} (catalog event {}) ...", preparationJob.getUid(),
							firstEvent.getUid());
					final Date now = new Date();
					final AppDataJobGeneration gen = new AppDataJobGeneration();
					gen.setState(AppDataJobGenerationState.INITIAL);
					gen.setTaskTable(tasktableFilename);
					gen.setNbErrors(0);
					gen.setCreationDate(now);
					gen.setLastUpdateDate(now);

					job.setGeneration(gen);
					job.setPrepJob(preparationJob);
					job.setReportingId(reportingUid);
					job.setState(AppDataJobState.GENERATING); // will activate that this request can be polled
					job.setPod(processProperties.getHostname());

					final AppDataJob newlyCreatedJob = appCatJobService.newJob(job);
					LOGGER.info("dispatched job {}", newlyCreatedJob.getId());
					dispatchedJobs.add(newlyCreatedJob);
				}
			}
		}

		return dispatchedJobs;
	}

	/**
	 * Returns the job of the list with the matching tasktable name. Returns null if
	 * no matching job was found
	 */
	private AppDataJob getJobMatchingTasktable(final List<AppDataJob> jobs, final String taskTableName) {
		for (final AppDataJob job : jobs) {
			if (job.getTaskTableName().equals(taskTableName)) {
				return job;
			}
		}
		return null;
	}

}