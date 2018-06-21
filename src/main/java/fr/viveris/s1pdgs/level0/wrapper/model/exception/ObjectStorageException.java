package fr.viveris.s1pdgs.level0.wrapper.model.exception;

import fr.viveris.s1pdgs.level0.wrapper.model.ProductFamily;

/**
 * Exception concerning the object storage
 * 
 * @author Viveris Technologies
 */
public class ObjectStorageException extends AbstractCodedException {

    /**
     * Serial UID
     */
    private static final long serialVersionUID = -3680895691846942569L;

    /**
     * Key in object storage
     */
    private final String key;

    /**
     * Family
     */
    private final ProductFamily family;

    /**
     * Constructor
     * 
     * @param key
     * @param bucket
     * @param e
     */
    public ObjectStorageException(final ProductFamily family, final String key,
            final Throwable cause) {
        super(ErrorCode.OBS_ERROR, cause.getMessage(), cause);
        this.key = key;
        this.family = family;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the family
     */
    public ProductFamily getFamily() {
        return family;
    }

    /**
     * @see AbstractCodedException#getLogMessage()
     */
    @Override
    public String getLogMessage() {
        return String.format("[family %s] [key %s] [msg %s]", family, key,
                getMessage());
    }
}
