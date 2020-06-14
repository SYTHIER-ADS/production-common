package esa.s1pdgs.cpoc.ingestion.worker.product;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.ingestion.worker.inbox.InboxAdapter;
import esa.s1pdgs.cpoc.ingestion.worker.obs.ObsAdapter;
import esa.s1pdgs.cpoc.mqi.model.queue.IngestionEvent;
import esa.s1pdgs.cpoc.mqi.model.queue.IngestionJob;
import esa.s1pdgs.cpoc.obs_sdk.ObsClient;
import esa.s1pdgs.cpoc.report.ReportingFactory;

@Service
public class ProductServiceImpl implements ProductService {
	static final Logger LOG = LogManager.getLogger(ProductServiceImpl.class);

	private final ObsClient obsClient;

	@Autowired
	public ProductServiceImpl(final ObsClient obsClient) {
		this.obsClient = obsClient;
	}

	@Override
	public List<Product<IngestionEvent>> ingest(
			final ProductFamily family, 
			final InboxAdapter inboxAdapter,
			final IngestionJob ingestion,
			final ReportingFactory reportingFactory
	) throws Exception {		
		final URI uri = IngestionJobs.toUri(ingestion);

		final ObsAdapter obsAdapter = newObsAdapterFor(reportingFactory);

		final IngestionEvent dto = new IngestionEvent(
				family, 
				ingestion.getProductName(), 
				ingestion.getRelativePath(), 
				ingestion.getProductSizeByte(),
				ingestion.getStationName()
		);
		final Product<IngestionEvent> prod = new Product<IngestionEvent>(family, uri, dto);
		obsAdapter.upload(
				family, 
				inboxAdapter.read(uri, ingestion.getProductName()), 
				ingestion.getProductName()
		);
		return Collections.singletonList(prod);
	}

	final String toObsKey(final Path relPath) {
		return relPath.toString();
	}

	private final ObsAdapter newObsAdapterFor(final ReportingFactory reportingFactory) {
		return new ObsAdapter(obsClient, reportingFactory);
	}

}
