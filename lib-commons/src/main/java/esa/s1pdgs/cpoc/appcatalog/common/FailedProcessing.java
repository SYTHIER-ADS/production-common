package esa.s1pdgs.cpoc.appcatalog.common;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import esa.s1pdgs.cpoc.common.MessageState;
import esa.s1pdgs.cpoc.common.ProductCategory;
import esa.s1pdgs.cpoc.errorrepo.model.rest.FailedProcessingDto;

public class FailedProcessing extends AbstractRequest {
	private static class AscendingCreationTimeComparator implements Comparator<FailedProcessing>, Serializable {
		private static final long serialVersionUID = 1191382370884793376L;

		/**
		 * order by ascending creation time
		 */
		@Override
		public int compare(FailedProcessing o1, FailedProcessing o2) {
			return o1.getCreationDate().compareTo(o2.getCreationDate());
		}
	}

	@JsonIgnore
	public static final Comparator<FailedProcessing> ASCENDING_CREATION_TIME_COMPARATOR = new AscendingCreationTimeComparator();
	
	protected long id;
	private String failedPod;	
	private Date lastAssignmentDate; 
	private Date failureDate;
	private String failureMessage;
	private Object predecessorDto;
	
	public FailedProcessing()	{		
	}

	public FailedProcessing(long id, ProductCategory category, String topic, int partition, long offset, String group,
			MessageState state, String sendingPod, Date lastSendDate, Date lastAckDate, int nbRetries, Object dto,
			Date creationDate, String failedPod, Date lastAssignmentDate, Date failureDate, String failureMessage,
			Object predecessorDto) {
		super(category, topic, partition, offset, group, state, sendingPod, lastSendDate, lastAckDate, nbRetries, dto,
				creationDate);
		this.id = id;
		this.failedPod = failedPod;
		this.lastAssignmentDate = lastAssignmentDate;
		this.failureDate = failureDate;
		this.failureMessage = failureMessage;
		this.predecessorDto = predecessorDto;
	}
	
	@JsonIgnore
	public static FailedProcessing valueOf(final MqiMessage message, final FailedProcessingDto failedProc)
	{
		return new FailedProcessing(
				message.getId(), 
				message.getCategory(), 
				message.getTopic(), 
				message.getPartition(),
				message.getOffset(), 
				message.getGroup(), 
				message.getState(), 
				message.getSendingPod(), 
				message.getLastSendDate(), 
				message.getLastAckDate(), 
				message.getNbRetries(), 
				failedProc.getProcessingDetails().getBody(), 
				message.getCreationDate(), 
				failedProc.getFailedPod(), 
				message.getLastReadDate(), 
				failedProc.getFailedDate(), 
				failedProc.getFailureMessage(),
				failedProc.getPredecessor().getBody()
		);
	}
	
	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
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

	public String getFailureMessage() {
		return failureMessage;
	}

	public void setFailureMessage(String failureMessage) {
		this.failureMessage = failureMessage;
	}

	@JsonProperty("productCategory")
	@Override
	public ProductCategory getCategory() {
		return super.getCategory();
	}

	@JsonProperty("processingType")
	@Override
	public String getTopic() {
		return super.getTopic();
	}

	@JsonProperty("processingStatus")
	@Override
	public MessageState getState() {
		return super.getState();
	}

	@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone="UTC")
	@Override
	public Date getLastSendDate() {
		// TODO Auto-generated method stub
		return super.getLastSendDate();
	}

	@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone="UTC")
	@Override
	public Date getLastAckDate() {
		// TODO Auto-generated method stub
		return super.getLastAckDate();
	}

	@JsonProperty("processingDetails")
	@Override
	public Object getDto() {
		// TODO Auto-generated method stub
		return super.getDto();
	}

	@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone="UTC")
	@Override
	public Date getCreationDate() {
		// TODO Auto-generated method stub
		return super.getCreationDate();
	}
	
	@JsonProperty("predecessor")
	public Object getPredecessorDto() {
		return this.predecessorDto;
	}
	
	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final FailedProcessing failedProcessing = (FailedProcessing) o;

		return Objects.equals(this.id, failedProcessing.id)
				&& Objects.equals(this.getTopic(), failedProcessing.getTopic())
				&& Objects.equals(this.getState(), failedProcessing.getState())
				&& Objects.equals(this.getCategory(), failedProcessing.getCategory())
				&& Objects.equals(this.getPartition(), failedProcessing.getPartition())
				&& Objects.equals(this.getOffset(), failedProcessing.getOffset())
				&& Objects.equals(this.getGroup(), failedProcessing.getGroup())
				&& Objects.equals(this.failedPod, failedProcessing.failedPod)
				&& Objects.equals(this.lastAssignmentDate, failedProcessing.lastAssignmentDate)
				&& Objects.equals(this.getSendingPod(), failedProcessing.getSendingPod())
				&& Objects.equals(this.getLastSendDate(), failedProcessing.getLastSendDate())
				&& Objects.equals(this.getLastAckDate(), failedProcessing.getLastAckDate())
				&& Objects.equals(this.getNbRetries(), failedProcessing.getNbRetries())
				&& Objects.equals(this.getCreationDate(), failedProcessing.getCreationDate())
				&& Objects.equals(this.failureDate, failedProcessing.failureDate)
				&& Objects.equals(this.failureMessage, failedProcessing.failureMessage)
				&& Objects.equals(this.getDto(), failedProcessing.getDto())
		        && Objects.equals(this.getPredecessorDto(), failedProcessing.getPredecessorDto());
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				id, 
				getTopic(), 
				getState(), 
				getCategory(), 
				getPartition(), 
				getOffset(), 
				getGroup(), 
				failedPod,
				lastAssignmentDate, 
				getSendingPod(), 
				getLastSendDate(), 
				getLastAckDate(),
				getNbRetries(), 
				getCreationDate(), 
				failureDate,
				failureMessage,
				getDto(),
				getPredecessorDto()
		);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("class FailedProcessing {\n");
		sb.append("    id: ").append(toIndentedString(id)).append("\n");
		sb.append("    processingType: ").append(toIndentedString(getTopic())).append("\n");
		sb.append("    processingStatus: ").append(toIndentedString(getState())).append("\n");
		sb.append("    productCategory: ").append(toIndentedString(getCategory())).append("\n");
		sb.append("    partition: ").append(toIndentedString(getPartition())).append("\n");
		sb.append("    offset: ").append(toIndentedString(getOffset())).append("\n");
		sb.append("    group: ").append(toIndentedString(getGroup())).append("\n");
		sb.append("    failedPod: ").append(toIndentedString(failedPod)).append("\n");
		sb.append("    lastAssignmentDate: ").append(toIndentedString(lastAssignmentDate)).append("\n");
		sb.append("    sendingPod: ").append(toIndentedString(getSendingPod())).append("\n");
		sb.append("    lastSendDate: ").append(toIndentedString(getLastSendDate())).append("\n");
		sb.append("    lastAckDate: ").append(toIndentedString(getLastAckDate())).append("\n");
		sb.append("    nbRetries: ").append(toIndentedString(getNbRetries())).append("\n");
		sb.append("    creationDate: ").append(toIndentedString(getCreationDate())).append("\n");
		sb.append("    failureDate: ").append(toIndentedString(failureDate)).append("\n");
		sb.append("    failureMessage: ").append(toIndentedString(failureMessage)).append("\n");
		sb.append("    processingDetails: ").append(toIndentedString(getDto())).append("\n");
		sb.append("    predecessor: ").append(toIndentedString(getPredecessorDto())).append("\n");
		sb.append("}");
		return sb.toString();
	}
	
	/**
	 * Convert the given object to string with each line indented by 4 spaces
	 * (except the first line).
	 */
	private final String toIndentedString(java.lang.Object o) {
		if (o == null) {
			return "null";
		}
		return o.toString().replace("\n", "\n    ");
	}
}
