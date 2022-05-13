package esa.s1pdgs.cpoc.mqi.model.queue;

import java.util.Collections;
import java.util.Objects;

import esa.s1pdgs.cpoc.common.ApplicationLevel;
import esa.s1pdgs.cpoc.mqi.model.control.AllowedAction;
import esa.s1pdgs.cpoc.mqi.model.rest.GenericMessageDto;

public class IpfPreparationJob extends AbstractMessage {
	private ApplicationLevel level;
	private GenericMessageDto<CatalogEvent> eventMessage;
	private String taskTableName;
	private String startTime;
	private String stopTime;
	private String outputProductType;
	private String processingMode = "NOT_DEFINED";

	public IpfPreparationJob() {
		allowedActions = Collections.singletonList(AllowedAction.RESTART);
	}

	public ApplicationLevel getLevel() {
		return level;
	}

	public void setLevel(final ApplicationLevel level) {
		this.level = level;
	}

	public GenericMessageDto<CatalogEvent> getEventMessage() {
		return eventMessage;
	}

	public void setEventMessage(final GenericMessageDto<CatalogEvent> eventMessage) {
		this.eventMessage = eventMessage;
	}

	public String getTaskTableName() {
		return taskTableName;
	}

	public void setTaskTableName(final String taskTableName) {
		this.taskTableName = taskTableName;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(final String startTime) {
		this.startTime = startTime;
	}

	public String getStopTime() {
		return stopTime;
	}

	public void setStopTime(final String stopTime) {
		this.stopTime = stopTime;
	}

	public String getOutputProductType() {
		return outputProductType;
	}

	public void setOutputProductType(final String outputProductType) {
		this.outputProductType = outputProductType;
	}

	public String getProcessingMode() {
		return processingMode;
	}

	public void setProcessingMode(final String processingMode) {
		this.processingMode = processingMode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(allowedActions, creationDate, debug, demandType, eventMessage, podName,
				keyObjectStorage, level, outputProductType, processingMode, productFamily, retryCounter, startTime,
				stopTime, taskTableName, uid);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		final IpfPreparationJob other = (IpfPreparationJob) obj;
		return Objects.equals(allowedActions, other.allowedActions) && Objects.equals(creationDate, other.creationDate)
				&& debug == other.debug && demandType == other.demandType
				&& Objects.equals(eventMessage, other.eventMessage) && Objects.equals(podName, other.podName)
				&& Objects.equals(keyObjectStorage, other.keyObjectStorage) && level == other.level
				&& Objects.equals(outputProductType, other.outputProductType)
				&& Objects.equals(processingMode, other.processingMode) && productFamily == other.productFamily
				&& retryCounter == other.retryCounter && Objects.equals(startTime, other.startTime)
				&& Objects.equals(stopTime, other.stopTime) && Objects.equals(taskTableName, other.taskTableName)
				&& Objects.equals(uid, other.uid);
	}

	@Override
	public String toString() {
		return "IpfPreparationJob [productFamily=" + productFamily + ", keyObjectStorage=" + keyObjectStorage
				+ ", storagePath=" + storagePath + ", uid=" + uid + ", creationDate=" + creationDate + ", podName="
				+ podName + ", allowedActions=" + allowedActions + ", demandType=" + demandType + ", retryCounter="
				+ retryCounter + ", debug=" + debug + ", level=" + level + ", eventMessage=" + eventMessage
				+ ", taskTableName=" + taskTableName + ", startTime=" + startTime + ", stopTime=" + stopTime
				+ ", outputProductType=" + outputProductType + ", processingMode=" + processingMode + "]";
	}
}
