package esa.s1pdgs.cpoc.disseminator.outbox;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.commons.io.IOUtils;

import esa.s1pdgs.cpoc.disseminator.config.DisseminationProperties.OutboxConfiguration;
import esa.s1pdgs.cpoc.disseminator.path.PathEvaluater;
import esa.s1pdgs.cpoc.obs_sdk.ObsClient;
import esa.s1pdgs.cpoc.obs_sdk.ObsObject;
import esa.s1pdgs.cpoc.report.ReportingFactory;

public final class LocalOutboxClient extends AbstractOutboxClient {	
	public static final class Factory implements OutboxClient.Factory {
		@Override
		public OutboxClient newClient(final ObsClient obsClient, final OutboxConfiguration config, final PathEvaluater eval) {
			return new LocalOutboxClient(obsClient, config, eval);
		}
	}
	
	public LocalOutboxClient(final ObsClient obsClient, final OutboxConfiguration config, final PathEvaluater eval) {
		super(obsClient, config, eval);
	}

	@Override
	public final String transfer(final ObsObject obsObject, final ReportingFactory reportingFactory) throws Exception {		
		final Path path = evaluatePathFor(obsObject);
		for (final String entry : entries(obsObject)) {
			
			final File destination = path.resolve("." + entry).toFile();
			createParentIfRequired(destination);
			
			try (final InputStream in = stream(obsObject.getFamily(), entry);
				 final OutputStream out = new BufferedOutputStream(new FileOutputStream(destination))
			) {				
				logger.info("Transferring {} to {}", entry, destination);
				IOUtils.copyLarge(in, out, new byte[config.getBufferSize()]);    				
			}
		}
		final Path dirWithDot = path.resolve("." + obsObject.getKey());
		final Path finalDir = path.resolve(obsObject.getKey());
		logger.debug("Moving {} to {}", dirWithDot, finalDir);
		Files.move(dirWithDot, finalDir,
				StandardCopyOption.ATOMIC_MOVE);
		return path.toString();
	}
}