package esa.s1pdgs.cpoc.mqi.model.queue;

import java.util.Objects;

import esa.s1pdgs.cpoc.common.ProductFamily;

/**
 * Exchanged object for the product category LevelReports.
 * 
 * @author Viveris Technologies
 */
public class LevelReportDto {

    /**
     * Product name of the reports
     */
    private String productName;

    /**
     * ObjectkeyStorage of the reports
     */
    private String content;

    /**
     * Family name for reports
     */
    private ProductFamily family;

    /**
     * Default constructor
     */
    public LevelReportDto() {
        super();
    }

    /**
     * @param productName
     * @param content
     */
    public LevelReportDto(final String productName, final String content,
            final ProductFamily family) {
        this();
        this.productName = productName;
        this.content = content;
        this.family = family;
    }

    /**
     * @return the productName
     */
    public String getProductName() {
        return productName;
    }

    /**
     * @param productName
     *            the productName to set
     */
    public void setProductName(final String productName) {
        this.productName = productName;
    }

    /**
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * @param content
     *            the content to set
     */
    public void setContent(final String content) {
        this.content = content;
    }

    /**
     * @return the family
     */
    public ProductFamily getFamily() {
        return family;
    }

    /**
     * @param family
     *            the family to set
     */
    public void setFamily(final ProductFamily family) {
        this.family = family;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("{productName: %s, content: %s, family: %s}",
                productName, content, family);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(productName, content, family);
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
            LevelReportDto other = (LevelReportDto) obj;
            // field comparison
            ret = Objects.equals(productName, other.productName)
                    && Objects.equals(content, other.content)
                    && Objects.equals(family, other.family);
        }
        return ret;
    }

}
