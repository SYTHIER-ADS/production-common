package esa.s1pdgs.cpoc.errorrepo.model.rest;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import esa.s1pdgs.cpoc.appcatalog.rest.MqiGenericMessageDto;
import esa.s1pdgs.cpoc.appcatalog.rest.MqiStateMessageEnum;
import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;

/**
 * @author birol_colak@net.werum
 *
 */
public class FailedProcessingDto<T extends GenericMessageDto<?>> extends MqiGenericMessageDto<T> {

	// public static final String TOPIC = "t-pdgs-errors";

	@SuppressWarnings("rawtypes")
	private static class AscendingCreationTimeComparator implements Comparator<FailedProcessingDto>, Serializable {

		private static final long serialVersionUID = 1191382370884793376L;

		/**
		 * order by ascending creation time
		 */
		@Override
		public int compare(FailedProcessingDto o1, FailedProcessingDto o2) {

			return o1.getCreationDate().compareTo(o2.getCreationDate());
		}
	}

	@SuppressWarnings("rawtypes")
	public static final Comparator<FailedProcessingDto> ASCENDING_CREATION_TIME_COMPERATOR = new AscendingCreationTimeComparator();

	public FailedProcessingDto() {
		super();
	}

	private String processingType = null;
	private String failureMessage = null;

	// maybe map to 'readingPod'
	private String failedPod = null;

	// maybe map to 'lastReadDate'
	private Date lastAssignmentDate = null;

	// current time (at creation of this object)
	private Date failureDate = null;

	public FailedProcessingDto<T> processingType(String processingType) {
		this.processingType = processingType;
		return this;
	}

	public FailedProcessingDto<T> topic(String topic) {
		this.setTopic(topic);
		return this;
	}

	public FailedProcessingDto<T> processingStatus(MqiStateMessageEnum processingStatus) {
		this.setState(processingStatus);
		return this;
	}

	public FailedProcessingDto<T> productCategory(ProductCategory productCategory) {
		this.setCategory(productCategory);
		return this;
	}

	public FailedProcessingDto<T> partition(int partition) {
		this.setPartition(partition);
		return this;
	}

	public FailedProcessingDto<T> offset(long offset) {
		this.setOffset(offset);
		return this;
	}

	public FailedProcessingDto<T> group(String group) {
		this.setGroup(group);
		return this;
	}

	public FailedProcessingDto<T> lastAssignmentDate(Date lastAssignmentDate) {
		this.lastAssignmentDate = lastAssignmentDate;
		return this;
	}

	public FailedProcessingDto<T> failedPod(String failedPod) {
		this.failedPod = failedPod;
		return this;
	}

	public FailedProcessingDto<T> sendingPod(String sendingPod) {
		this.setSendingPod(sendingPod);
		return this;
	}

	public FailedProcessingDto<T> lastSendDate(Date lastSendDate) {
		this.setLastSendDate(lastSendDate);
		return this;
	}

	public FailedProcessingDto<T> lastAckDate(Date lastAckDate) {
		this.setLastAckDate(lastAckDate);
		return this;
	}

	public FailedProcessingDto<T> nbRetries(int nbRetries) {
		this.setNbRetries(nbRetries);
		return this;
	}

	public FailedProcessingDto<T> creationDate(Date creationDate) {
		this.setCreationDate(creationDate);
		return this;
	}

	@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone="UTC")
	public FailedProcessingDto<T> failureDate(Date failureDate) {
		this.failureDate = failureDate;
		return this;
	}

	public String getProcessingType() {
		return processingType;
	}

	public void setProcessingType(String processingType) {
		this.processingType = processingType;
	}

	public String getFailedPod() {
		return failedPod;
	}

	public void setFailedPod(String failedPod) {
		this.failedPod = failedPod;
	}

	@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone="UTC")
	public Date getLastAssignmentDate() {
		return lastAssignmentDate;
	}

	public void setLastAssignmentDate(Date lastAssignmentDate) {
		this.lastAssignmentDate = lastAssignmentDate;
	}

	@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone="UTC")
	public Date getFailureDate() {
		return failureDate;
	}

	public void setFailureDate(Date failureDate) {
		this.failureDate = failureDate;
	}

	public FailedProcessingDto<T> failureMessage(String failureMessage) {
		this.failureMessage = failureMessage;
		return this;
	}

	public String getFailureMessage() {
		return failureMessage;
	}

	public void setFailureMessage(String failureMessage) {
		this.failureMessage = failureMessage;
	}

	public FailedProcessingDto<T> processingDetails(T processingDetails) {
		this.setDto(processingDetails);
		return this;
	}
	
	@Override
	@JsonProperty("processingStatus")
	public MqiStateMessageEnum getState() {
		return super.getState();
	}
	
	@Override
	@JsonProperty("productCategory")
	public ProductCategory getCategory() {
		return super.getCategory();
	}
	
	@Override
	@JsonProperty("processingDetails")
	public T getDto() {
		return super.getDto();
	}

	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		@SuppressWarnings("unchecked")
		FailedProcessingDto<T> failedProcessing = (FailedProcessingDto<T>) o;

		return Objects.equals(this.identifier, failedProcessing.identifier)
				&& Objects.equals(this.processingType, failedProcessing.processingType)
				&& Objects.equals(this.state, failedProcessing.state)
				&& Objects.equals(this.category, failedProcessing.category)
				&& Objects.equals(this.partition, failedProcessing.partition)
				&& Objects.equals(this.offset, failedProcessing.offset)
				&& Objects.equals(this.group, failedProcessing.group)
				&& Objects.equals(this.failedPod, failedProcessing.failedPod)
				&& Objects.equals(this.lastAssignmentDate, failedProcessing.lastAssignmentDate)
				&& Objects.equals(this.sendingPod, failedProcessing.sendingPod)
				&& Objects.equals(this.lastSendDate, failedProcessing.lastSendDate)
				&& Objects.equals(this.lastAckDate, failedProcessing.lastAckDate)
				&& Objects.equals(this.nbRetries, failedProcessing.nbRetries)
				&& Objects.equals(this.creationDate, failedProcessing.creationDate)
				&& Objects.equals(this.failureDate, failedProcessing.failureDate)
				&& Objects.equals(this.failureMessage, failedProcessing.failureMessage);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(identifier, processingType, state, category, partition, offset, group, failedPod,
				lastAssignmentDate, sendingPod, lastSendDate, lastAckDate, nbRetries, creationDate, failureDate,
				failureMessage);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class FailedProcessing {\n");

		sb.append("    id: ").append(toIndentedString(identifier)).append("\n");
		sb.append("    processingType: ").append(toIndentedString(processingType)).append("\n");
		sb.append("    processingStatus: ").append(toIndentedString(state)).append("\n");
		sb.append("    productCategory: ").append(toIndentedString(category)).append("\n");
		sb.append("    partition: ").append(toIndentedString(partition)).append("\n");
		sb.append("    offset: ").append(toIndentedString(offset)).append("\n");
		sb.append("    group: ").append(toIndentedString(group)).append("\n");
		sb.append("    failedPod: ").append(toIndentedString(failedPod)).append("\n");
		sb.append("    lastAssignmentDate: ").append(toIndentedString(lastAssignmentDate)).append("\n");
		sb.append("    sendingPod: ").append(toIndentedString(sendingPod)).append("\n");
		sb.append("    lastSendDate: ").append(toIndentedString(lastSendDate)).append("\n");
		sb.append("    lastAckDate: ").append(toIndentedString(lastAckDate)).append("\n");
		sb.append("    nbRetries: ").append(toIndentedString(nbRetries)).append("\n");
		sb.append("    creationDate: ").append(toIndentedString(creationDate)).append("\n");
		sb.append("    failureDate: ").append(toIndentedString(failureDate)).append("\n");
		sb.append("    failureMessage: ").append(toIndentedString(failureMessage)).append("\n");
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Convert the given object to string with each line indented by 4 spaces
	 * (except the first line).
	 */
	private String toIndentedString(java.lang.Object o) {
		if (o == null) {
			return "null";
		}
		return o.toString().replace("\n", "\n    ");
	}
	
	@JsonProperty("id")
	@Override
	public long getIdentifier() {
		// TODO Auto-generated method stub
		return super.getIdentifier();
	}
	
	@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone="UTC")
	@Override
	public Date getCreationDate() {
		return super.getCreationDate();
	}
	
	@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone="UTC")
	@Override
	public Date getLastReadDate() {
		return super.getLastReadDate();
	}
	@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone="UTC")
	@Override
	public Date getLastSendDate() {
		return super.getLastSendDate();
	}
	
	@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone="UTC")
	@Override
	public Date getLastAckDate() {
		return super.getLastAckDate();
	}
}
