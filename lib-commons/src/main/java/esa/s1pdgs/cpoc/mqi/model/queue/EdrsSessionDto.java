package esa.s1pdgs.cpoc.mqi.model.queue;

import java.util.Objects;

import esa.s1pdgs.cpoc.common.EdrsSessionFileType;
import esa.s1pdgs.cpoc.common.ProductFamily;

/**
 * DTO object used to transfer EDRS session files between MQI and application
 * 
 * @author Viveris technologies
 */
public class EdrsSessionDto extends AbstractDto {

    /**
     * Channel identifier
     */
    private int channelId;

    /**
     * Type of the EDRS session file: raw or XML file
     */
    private EdrsSessionFileType productType;

    /**
     * Satellite identifier
     */
    private String satelliteId;

    /**
     * Mission identifier
     */
    private String missionId;
    
    /**
     * Default constructor
     */
    public EdrsSessionDto() {
        super();
    }

    /**
     * Default constructor
     */
    public EdrsSessionDto(final String objectStorageKey, final int channelId,
            final EdrsSessionFileType productType, final String missionId,
            final String satelliteId) {
        super(objectStorageKey, ProductFamily.EDRS_SESSION);
        this.channelId = channelId;
        this.productType = productType;
        this.missionId = missionId;
        this.satelliteId = satelliteId;
    }

    /**
     * @return the objectStorageKey
     */
    public String getKeyObjectStorage() {
        return getProductName();
    }

    /**
     * @param objectStorageKey
     *            the objectStorageKey to set
     */
    public void setKeyObjectStorage(final String keyObjectStorage) {
        this.setProductName(keyObjectStorage);
    }

    /**
     * @return the channelId
     */
    public int getChannelId() {
        return channelId;
    }

    /**
     * @param channelId
     *            the channelId to set
     */
    public void setChannelId(final int channelId) {
        this.channelId = channelId;
    }

    /**
     * @return the productType
     */
    public EdrsSessionFileType getProductType() {
        return productType;
    }

    /**
     * @param productType
     *            the productType to set
     */
    public void setProductType(final EdrsSessionFileType productType) {
        this.productType = productType;
    }

    /**
     * @return the satelliteId
     */
    public String getSatelliteId() {
        return satelliteId;
    }

    /**
     * @param satelliteId
     *            the satelliteId to set
     */
    public void setSatelliteId(final String satelliteId) {
        this.satelliteId = satelliteId;
    }

    /**
     * @return the missionId
     */
    public String getMissionId() {
        return missionId;
    }

    /**
     * @param missionId
     *            the missionId to set
     */
    public void setMissionId(final String missionId) {
        this.missionId = missionId;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format(
                "{objectStorageKey: %s, channelId: %s, productType: %s, satelliteId: %s, missionId: %s}",
                getKeyObjectStorage(), channelId, productType, satelliteId,
                missionId);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(getKeyObjectStorage(), getFamily(), channelId, productType,
                satelliteId, missionId);
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
            EdrsSessionDto other = (EdrsSessionDto) obj;
            // field comparison
            ret = Objects.equals(getKeyObjectStorage(), other.getKeyObjectStorage())
            		&&  Objects.equals(getFamily(), other.getFamily())
                    && channelId == other.channelId
                    && Objects.equals(productType, other.productType)
                    && Objects.equals(satelliteId, other.satelliteId)
                    && Objects.equals(missionId, other.missionId);
        }
        return ret;
    }

}
