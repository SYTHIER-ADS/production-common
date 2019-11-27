package esa.s1pdgs.cpoc.mdc.worker.extraction;

import org.json.JSONObject;

import esa.s1pdgs.cpoc.common.errors.AbstractCodedException;
import esa.s1pdgs.cpoc.mqi.model.queue.CatalogJob;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;
import esa.s1pdgs.cpoc.report.Reporting;

public interface MetadataExtractor {	
    interface ThrowingSupplier<E> {
    	E get() throws AbstractCodedException;
    }
    
	public static final MetadataExtractor FAIL = new MetadataExtractor() {
		@Override
		public final JSONObject extract(final Reporting.Factory reportingFactory, final GenericMessageDto<CatalogJob> message) {
			throw new IllegalArgumentException(
				String.format("No Metadata extractor defined for catalog job %s", message)	
			);
		}		
	};	
	JSONObject extract(Reporting.Factory reportingFactory, GenericMessageDto<CatalogJob> message) throws AbstractCodedException;
}
