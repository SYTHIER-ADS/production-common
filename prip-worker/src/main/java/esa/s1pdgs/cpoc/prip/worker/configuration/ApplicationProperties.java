package esa.s1pdgs.cpoc.prip.worker.configuration;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("prip-worker")
public class ApplicationProperties {
	
	public static class ProductTypeRegexp {
		private String l0Std;
		private String l0Ann;
		private String l0Cal;
		private String l0Noise;
		private String l0Gps;
		private String l0Hktm;
		private String l1;
		private String l2;
		private String auxSafe;
		private String auxEof;
		
		public String getL0Std() {
			return l0Std;
		}
		
		public void setL0Std(String l0Std) {
			this.l0Std = l0Std;
		}
		
		public String getL0Ann() {
			return l0Ann;
		}
		
		public void setL0Ann(String l0Ann) {
			this.l0Ann = l0Ann;
		}
		
		public String getL0Cal() {
			return l0Cal;
		}
		
		public void setL0Cal(String l0Cal) {
			this.l0Cal = l0Cal;
		}
		
		public String getL0Noise() {
			return l0Noise;
		}
		
		public void setL0Noise(String l0Noise) {
			this.l0Noise = l0Noise;
		}
		
		public String getL0Gps() {
			return l0Gps;
		}
		
		public void setL0Gps(String l0Gps) {
			this.l0Gps = l0Gps;
		}
		
		public String getL0Hktm() {
			return l0Hktm;
		}
		
		public void setL0Hktm(String l0Hktm) {
			this.l0Hktm = l0Hktm;
		}
		
		public String getL1() {
			return l1;
		}
		
		public void setL1(String l1) {
			this.l1 = l1;
		}
		
		public String getL2() {
			return l2;
		}
		
		public void setL2(String l2) {
			this.l2 = l2;
		}
		
		public String getAuxSafe() {
			return auxSafe;
		}
		
		public void setAuxSafe(String auxSafe) {
			this.auxSafe = auxSafe;
		}
		
		public String getAuxEof() {
			return auxEof;
		}
		
		public void setAuxEof(String auxEof) {
			this.auxEof = auxEof;
		}	
	}
	
	public static class MetadataMapping {
		private Map<String, String> l0Std;
		private Map<String, String> l0Ann;
		private Map<String, String> l0Cal;
		private Map<String, String> l0Noise;
		private Map<String, String> l0Gps;
		private Map<String, String> l0Hktm;
		private Map<String, String> l1;
		private Map<String, String> l2;
		private Map<String, String> auxSafe;
		private Map<String, String> auxEof;
	
		public Map<String, String> getL0Std() {
			return l0Std;
		}
		
		public void setL0Std(Map<String, String> l0Std) {
			this.l0Std = l0Std;
		}
		
		public Map<String, String> getL0Ann() {
			return l0Ann;
		}
		
		public void setL0Ann(Map<String, String> l0Ann) {
			this.l0Ann = l0Ann;
		}
		
		public Map<String, String> getL0Cal() {
			return l0Cal;
		}
		
		public void setL0Cal(Map<String, String> l0Cal) {
			this.l0Cal = l0Cal;
		}
		
		public Map<String, String> getL0Noise() {
			return l0Noise;
		}
		
		public void setL0Noise(Map<String, String> l0Noise) {
			this.l0Noise = l0Noise;
		}
		
		public Map<String, String> getL0Gps() {
			return l0Gps;
		}
		
		public void setL0Gps(Map<String, String> l0Gps) {
			this.l0Gps = l0Gps;
		}
		
		public Map<String, String> getL0Hktm() {
			return l0Hktm;
		}
		
		public void setL0Hktm(Map<String, String> l0Hktm) {
			this.l0Hktm = l0Hktm;
		}
		
		public Map<String, String> getL1() {
			return l1;
		}
		
		public void setL1(Map<String, String> l1) {
			this.l1 = l1;
		}
		
		public Map<String, String> getL2() {
			return l2;
		}
		
		public void setL2(Map<String, String> l2) {
			this.l2 = l2;
		}
		
		public Map<String, String> getAuxSafe() {
			return auxSafe;
		}
		
		public void setAuxSafe(Map<String, String> auxSafe) {
			this.auxSafe = auxSafe;
		}
		
		public Map<String, String> getAuxEof() {
			return auxEof;
		}
		
		public void setAuxEof(Map<String, String> auxEof) {
			this.auxEof = auxEof;
		}		
	}
	
	private String hostname;
	private int metadataUnavailableRetriesNumber = 10;
	private long metadataUnavailableRetriesIntervalMs = 5000;
	private ProductTypeRegexp productTypeRegexp;
	private MetadataMapping metadataMapping;

	public int getMetadataUnavailableRetriesNumber() {
		return metadataUnavailableRetriesNumber;
	}
	
	public void setMetadataUnavailableRetriesNumber(final int metadataUnavailableRetriesNumber) {
		this.metadataUnavailableRetriesNumber = metadataUnavailableRetriesNumber;
	}
	
	public long getMetadataUnavailableRetriesIntervalMs() {
		return metadataUnavailableRetriesIntervalMs;
	}
	
	public void setMetadataUnavailableRetriesIntervalMs(final long metadataUnavailableRetriesIntervalMs) {
		this.metadataUnavailableRetriesIntervalMs = metadataUnavailableRetriesIntervalMs;
	}
	
	public String getHostname() {
		return hostname;
	}
	
	public void setHostname(final String hostname) {
		this.hostname = hostname;
	}

	public ProductTypeRegexp getProductTypeRegexp() {
		return productTypeRegexp;
	}
	
	public void setProductTypeRegexp(ProductTypeRegexp productTypeRegexp) {
		this.productTypeRegexp = productTypeRegexp;
	}

	public MetadataMapping getMetadataMapping() {
		return metadataMapping;
	}

	public void setMetadataMapping(MetadataMapping metadataMapping) {
		this.metadataMapping = metadataMapping;
	}

}
