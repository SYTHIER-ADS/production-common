package fr.viveris.s1pdgs.ingestor.model.exception;

public class MetadataExtractionException extends FileException {

	private static final long serialVersionUID = 2134771514034032034L;
	
	private static final String MESSAGE = "Metadata extraction failed for %s: %s";

	public MetadataExtractionException(String productName, Throwable cause) {
		super(String.format(MESSAGE, productName, cause.getMessage()), productName, cause);
	}

}
