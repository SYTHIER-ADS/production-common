package esa.s1pdgs.cpoc.compression.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * Compression application
 * 
 * @author Florian Sievert
 *
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties
@ComponentScan("esa.s1pdgs.cpoc")
public class Application {
	
    /**
     * Main application
     * @param args
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
