package esa.s1pdgs.cpoc.mqi.model.queue;

import java.util.Locale;
import java.util.Objects;

import esa.s1pdgs.cpoc.common.ProductFamily;

/**
 * Exchanged object for the product category LevelProducts.
 * 
 * @author Viveris Technologies
 */
public class LevelSegmentDto {

    /**
     * Product name of the metadata to index
     */
    private String name;

    /**
     * ObjectkeyStorage of the metatdata to index
     */
    private String keyObs;

    /**
     * Family name for L0 Slices
     */
    private ProductFamily family;

    /**
     * Mode
     */
    private String mode;

    /**
     * Default constructor
     */
    public LevelSegmentDto() {
        super();
        this.family = ProductFamily.BLANK;
    }

    /**
     * @param productName
     * @param keyObjectStorage
     */
    public LevelSegmentDto(final String productName,
            final String keyObjectStorage, final ProductFamily family, final String mode) {
        this();
        this.name = productName;
        this.keyObs = keyObjectStorage;
        this.family = family;
        if (mode != null) {
            this.mode = mode.toUpperCase(Locale.FRANCE);
        }
    }

    /**
     * 
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * @param name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * 
     * @return
     */
    public String getKeyObs() {
        return keyObs;
    }

    /**
     * 
     * @param keyObs
     */
    public void setKeyObs(final String keyObs) {
        this.keyObs = keyObs;
    }

    /**
     * @return the family
     */
    public ProductFamily getFamily() {
        return family;
    }

    /**
     * @param familyName
     *            the familyName to set
     */
    public void setFamily(final ProductFamily family) {
        this.family = family;
    }

    /**
     * @return
     */
    public String getMode() {
        return mode;
    }

    /**
     * @param mode
     */
    public void setMode(final String mode) {
        if (mode == null) {
            this.mode = mode;
        } else {
            this.mode = mode.toUpperCase(Locale.FRANCE);
        }
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format(
                "{name: %s, keyObs: %s, family: %s, mode: %s}",
                name, keyObs, family, mode);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, keyObs, family, mode);
    }

    /**
     * @see java.lang.Object#equals()
     */
    @Override
    public boolean equals(final Object obj) {
        boolean ret;
        if (this == obj) {
            ret = true;
        } else if (obj == null || getClass() != obj.getClass()) {
            ret = false;
        } else {
            LevelSegmentDto other = (LevelSegmentDto) obj;
            // field comparison
            ret = Objects.equals(name, other.name)
                    && Objects.equals(keyObs, other.keyObs)
                    && Objects.equals(family, other.family)
                    && Objects.equals(mode, other.mode);
        }
        return ret;
    }
}
