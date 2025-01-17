package esa.s1pdgs.cpoc.metadata.extraction.service.extraction.model;

import java.util.Objects;

public class S2FileDescriptor extends AbstractFileDescriptor {

	private String instrumentShortName;
	
	public String getInstrumentShortName() {
		return instrumentShortName;
	}

	public void setInstrumentShortName(String instrumentShortName) {
		this.instrumentShortName = instrumentShortName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(extension, filename, keyObjectStorage, missionId, mode, productClass, productFamily,
				productName, productType, relativePath, satelliteId, instrumentShortName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		S2FileDescriptor other = (S2FileDescriptor) obj;
		return extension == other.extension && Objects.equals(filename, other.filename)
				&& Objects.equals(keyObjectStorage, other.keyObjectStorage)
				&& Objects.equals(missionId, other.missionId) && Objects.equals(mode, other.mode)
				&& Objects.equals(productClass, other.productClass) && productFamily == other.productFamily
				&& Objects.equals(productName, other.productName) && Objects.equals(productType, other.productType)
				&& Objects.equals(relativePath, other.relativePath) && Objects.equals(satelliteId, other.satelliteId)
				&& Objects.equals(instrumentShortName, other.instrumentShortName);
	}

	@Override
	public String toString() {
		return "S2FileDescriptor [productType=" + productType + ", productClass=" + productClass + ", relativePath="
				+ relativePath + ", filename=" + filename + ", extension=" + extension + ", productName=" + productName
				+ ", missionId=" + missionId + ", satelliteId=" + satelliteId + ", keyObjectStorage=" + keyObjectStorage
				+ ", productFamily=" + productFamily + ", mode=" + mode + ", instrumentShortName" + instrumentShortName
				+ "]";
	}

}
