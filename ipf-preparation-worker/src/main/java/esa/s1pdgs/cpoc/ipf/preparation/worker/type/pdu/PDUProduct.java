package esa.s1pdgs.cpoc.ipf.preparation.worker.type.pdu;

import java.util.List;

import esa.s1pdgs.cpoc.appcatalog.AppDataJob;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobProduct;
import esa.s1pdgs.cpoc.appcatalog.AppDataJobTaskInputs;
import esa.s1pdgs.cpoc.appcatalog.util.AppDataJobProductAdapter;
import esa.s1pdgs.cpoc.ipf.preparation.worker.type.AbstractProduct;

public class PDUProduct extends AbstractProduct {

	public static final String FRAME_NUMBER = "frameNumber";
	
	public static final PDUProduct of(final AppDataJob job) {
		return of(job.getProduct());
	}
	
	public static final PDUProduct of(final AppDataJobProduct product) {
		return new PDUProduct(new AppDataJobProductAdapter(product));
	}
	
	/**
	 * List of additional inputs, that were set during the main input search
	 */
	private List<AppDataJobTaskInputs> additionalInputs;
	

	public PDUProduct(final AppDataJobProductAdapter product) {
		super(product);
	}
	
	public final void setFrameNumber(final int frameNumber) {
		product.setIntegerValue("frameNumber", frameNumber);
	}
	
	public final int getFrameNumber() {
		return product.getIntegerValue("frameNumber");
	}
	
	public void setAdditionalInputs(final List<AppDataJobTaskInputs> additionalInputs) {
		this.additionalInputs = additionalInputs;
	}
	
	public final void setStartTime(final String start) {
		product.setStartTime(start);
	}
	
	public final void setStopTime(final String stop) {
		product.setStopTime(stop);
	}
	
	@Override
	public List<AppDataJobTaskInputs> overridingInputs() {
		return additionalInputs;
	}
}
