package esa.s1pdgs.cpoc.ipf.preparation.worker.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "aiop")
public class AiopProperties {
	private Map<String,String> stationCodes;
	private Map<String,String> ptAssembly;
	private Map<String,String> processingMode;
	private Map<String,String> reprocessingMode;
	private Map<String,String> timeoutSec;
	private Map<String,String> descramble;
	private Map<String,String> rsEncode;
	private Map<String,String> nrtOutputPath;
	private Map<String,String> ptOutputPath;
	private long minimalWaitingTimeSec;
	private boolean disableTimeout;
	
	/**
	 * @return the stationCodes
	 */
	public Map<String, String> getStationCodes() {
		return stationCodes;
	}
	
	/**
	 * @param stationCodes the stationCodes to set
	 */
	public void setStationCodes(Map<String, String> stationCodes) {
		this.stationCodes = stationCodes;
	}
	
	/**
	 * @return the ptAssembly
	 */
	public Map<String, String> getPtAssembly() {
		return ptAssembly;
	}
	
	/**
	 * @param ptAssembly the ptAssembly to set
	 */
	public void setPtAssembly(Map<String, String> ptAssembly) {
		this.ptAssembly = ptAssembly;
	}
	
	/**
	 * @return the processingMode
	 */
	public Map<String, String> getProcessingMode() {
		return processingMode;
	}
	
	/**
	 * @param processingMode the processingMode to set
	 */
	public void setProcessingMode(Map<String, String> processingMode) {
		this.processingMode = processingMode;
	}
	
	/**
	 * @return the reprocessingMode
	 */
	public Map<String, String> getReprocessingMode() {
		return reprocessingMode;
	}
	
	/**
	 * @param reprocessingMode the reprocessingMode to set
	 */
	public void setReprocessingMode(Map<String, String> reprocessingMode) {
		this.reprocessingMode = reprocessingMode;
	}
	
	/**
	 * @return the timeout
	 */
	public Map<String, String> getTimeoutSec() {
		return timeoutSec;
	}
	
	/**
	 * @param timeoutSec the timeout to set
	 */
	public void setTimeoutSec(Map<String, String> timeoutSec) {
		this.timeoutSec = timeoutSec;
	}
	
	/**
	 * @return the descramble
	 */
	public Map<String, String> getDescramble() {
		return descramble;
	}
	
	/**
	 * @param descramble the descramble to set
	 */
	public void setDescramble(Map<String, String> descramble) {
		this.descramble = descramble;
	}
	
	/**
	 * @return the rsEncode
	 */
	public Map<String, String> getRsEncode() {
		return rsEncode;
	}
	
	/**
	 * @param rsEncode the rsEncode to set
	 */
	public void setRsEncode(Map<String, String> rsEncode) {
		this.rsEncode = rsEncode;
	}
	
	/**
	 * @return the nrtOutputPath
	 */
	public Map<String, String> getNrtOutputPath() {
		return nrtOutputPath;
	}

	/**
	 * @param nrtOutputPath the nrtOutputPath to set
	 */
	public void setNrtOutputPath(Map<String, String> nrtOutputPath) {
		this.nrtOutputPath = nrtOutputPath;
	}
	
	/**
	 * @return the ptOutputPath
	 */
	public Map<String, String> getPtOutputPath() {
		return ptOutputPath;
	}

	/**
	 * @param ptOutputPath the ptOutputPath to set
	 */
	public void setPtOutputPath(Map<String, String> ptOutputPath) {
		this.ptOutputPath = ptOutputPath;
	}
	
	public long getMinimalWaitingTimeSec() {
		return minimalWaitingTimeSec;
	}

	public void setMinimalWaitingTimeSec(long minimalWaitingTimeSec) {
		this.minimalWaitingTimeSec = minimalWaitingTimeSec;
	}

	public boolean getDisableTimeout() {
		return disableTimeout;
	}

	public void setDisableTimeout(boolean disableTimeout) {
		this.disableTimeout = disableTimeout;
	}
}
