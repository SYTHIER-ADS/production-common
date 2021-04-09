package esa.s1pdgs.cpoc.ingestion.worker.obs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import esa.s1pdgs.cpoc.common.ProductFamily;
import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.common.errors.obs.ObsException;
import esa.s1pdgs.cpoc.common.utils.Exceptions;
import esa.s1pdgs.cpoc.common.utils.LogUtils;
import esa.s1pdgs.cpoc.ingestion.worker.inbox.InboxAdapterEntry;
import esa.s1pdgs.cpoc.obs_sdk.ObsClient;
import esa.s1pdgs.cpoc.obs_sdk.ObsEmptyFileException;
import esa.s1pdgs.cpoc.obs_sdk.ObsObject;
import esa.s1pdgs.cpoc.obs_sdk.SdkClientException;
import esa.s1pdgs.cpoc.obs_sdk.StreamObsUploadObject;
import esa.s1pdgs.cpoc.report.ReportingFactory;

public class ObsAdapter {
    private static final int BUFFER_SIZE = 8 * 1024 * 1024; // copy in 8M blocks

    private final ObsClient obsClient;
    private final ReportingFactory reportingFactory;
    private final boolean copyInputStreamToBuffer;

    public ObsAdapter(
            final ObsClient obsClient,
            final ReportingFactory reportingFactory,
            final boolean copyInputStreamToBuffer
    ) {
        this.obsClient = obsClient;
        this.reportingFactory = reportingFactory;
        this.copyInputStreamToBuffer = copyInputStreamToBuffer;
    }

    public final void upload(final ProductFamily family, final List<InboxAdapterEntry> entries, final String obsKey) throws ObsEmptyFileException {
        try {
            obsClient.uploadStreams(toUploadObjects(family, entries), reportingFactory);
        } catch (final AbstractCodedException e) {
            throw new RuntimeException(
                    String.format("Error uploading %s (%s): %s", obsKey, family, LogUtils.toString(e))
            );
        }
    }

    public final long sizeOf(final ProductFamily family, final String obsKey) {
        try {
            final ObsObject obsObject = new ObsObject(family, obsKey);

            //obsObject is a file and exists
            if (obsClient.exists(obsObject)) {
                return obsClient.size(obsObject);
            }

            //obsObject is a directory and exists
            if (obsClient.prefixExists(obsObject)) {
                List<String> list = obsClient.list(family, obsObject.getKey());
                long totalSize = 0;
                for (String key : list) {
                    totalSize += obsClient.size(new ObsObject(family, key));
                }

                return totalSize;
            }

            return -1;
        } catch (ObsException | SdkClientException e) {
            throw new RuntimeException(
                    String.format("Error while retrieving size for obs object with key %s: %s",
                            obsKey,
                            LogUtils.toString(e)));
        }
    }

    private List<StreamObsUploadObject> toUploadObjects(final ProductFamily family, final List<InboxAdapterEntry> entries) {
        return entries.stream()
                .map(e -> new StreamObsUploadObject(family, e.key(), inputStreamOf(e), e.size()))
                .collect(Collectors.toList());
    }

    private InputStream inputStreamOf(final InboxAdapterEntry entry) {
        // S1PRO-2117: Make the buffer explicit here and avoid having too many concurrent open connection
        // for product download
        if (copyInputStreamToBuffer) {
            // No use of retries here as input stream is closed anyway and needs to be re-read,
            // i.e. retries may make sense in ProductServiceImpl
            return copyFromInputStream(entry);
        }
        // old behavior
        return entry.inputStream();
    }

    private static InputStream copyFromInputStream(final InboxAdapterEntry entry) {
        try (final InputStream in = entry.inputStream();
             final ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            final byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            return new ByteArrayInputStream(out.toByteArray());
        } catch (final IOException e) {
            throw new RuntimeException(
                    String.format("Error on downloading '%s': %s", entry.key(), Exceptions.messageOf(e)),
                    e
            );
        }
    }
}
