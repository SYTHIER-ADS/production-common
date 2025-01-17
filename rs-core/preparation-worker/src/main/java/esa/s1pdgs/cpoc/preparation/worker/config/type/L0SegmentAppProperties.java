package esa.s1pdgs.cpoc.preparation.worker.config.type;


import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "app-l0-segment")
public class L0SegmentAppProperties {

    /**
     * The regular expression in Java format
     */
    private String nameRegexpPattern;
    
    private String blacklistPattern;
    
    /**
     * The regular expression in Java format
     */
    private Map<String, Integer> nameRegexpGroups;

    public L0SegmentAppProperties() {
        super();
        nameRegexpGroups = new HashMap<>();
    }

    /**
     * @return the nameRegexpPattern
     */
    public String getNameRegexpPattern() {
        return nameRegexpPattern;
    }

    /**
     * @param nameRegexpPattern the nameRegexpPattern to set
     */
    public void setNameRegexpPattern(String nameRegexpPattern) {
        this.nameRegexpPattern = nameRegexpPattern;
    }

    /**
     * @return
     */
    public String getBlacklistPattern() {
		return blacklistPattern;
	}

	/**
	 * @param blacklistPattern
	 */
	public void setBlacklistPattern(String blacklistPattern) {
		this.blacklistPattern = blacklistPattern;
	}

	/**
     * @return the nameRegexpGroups
     */
    public Map<String, Integer> getNameRegexpGroups() {
        return nameRegexpGroups;
    }

    /**
     * @param nameRegexpGroups the nameRegexpGroups to set
     */
    public void setNameRegexpGroups(Map<String, Integer> nameRegexpGroups) {
        this.nameRegexpGroups = nameRegexpGroups;
    }

}
