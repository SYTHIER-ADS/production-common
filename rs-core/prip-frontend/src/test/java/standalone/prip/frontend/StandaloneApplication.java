package standalone.prip.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("esa.s1pdgs.cpoc.prip")
public class StandaloneApplication {
    public static void main(String[] args) {

    	/*
    	 * PRIP Standalone Application
    	 * 
    	 * How to get it running:
    	 * 
    	 * 1. Download and unpack https://www.elastic.co/de/downloads/past-releases/elasticsearch-7-7-0
    	 * 		--> better use open source version: https://www.elastic.co/de/downloads/past-releases/elasticsearch-oss-7-7-0
    	 * 
    	 * 2. Execute bin/elasticsearch
    	 * 
    	 * 3. Create an index for PRIP metadata:
    	 *    $ curl -XPUT "http://localhost:9200/prip" -H 'Content-Type: application/json' -d '{"mappings":{"properties":{"id": {"type":"keyword"},"obsKey":{"type":"keyword"},"name":{"type":"keyword"},"productFamily":{"type":"keyword"},"contentType":{"type":"keyword"},"contentLength":{"type":"long"},"contentDateStart":{"type":"date"},"contentDateEnd":{"type":"date"},"creationDate":{"type":"date"},"evictionDate":{"type":"date"},"checksum":{"type":"nested","properties":{"algorithm":{"type":"keyword"},"value":{"type":"keyword"},"checksum_date":{"type":"date"}}},"footprint":{"type":"geo_shape"}}}}'
    	 *    
    	 * 4. Create one ...
    	 *    $ curl -XPOST "http://localhost:9200/prip/_doc" -H 'Content-Type: application/json' -d '{"id":"00000000-0000-0000-0000-000000000001","obsKey":"S1B_GP_RAW__0____20181001T101010_20181001T151653_012957________0001.SAFE.zip","name":"S1B_GP_RAW__0____20181001T101010_20181001T151653_012957________0001.SAFE.zip","productFamily":"L0_SEGMENT_ZIP","contentType":"application/zip","contentLength":3600003,"contentDateStart":"2020-04-04T16:47:32.944000Z","contentDateEnd":"2020-04-04T16:47:32.944000Z","creationDate":"2020-04-04T16:47:32.944000Z","evictionDate":"2020-04-11T16:47:32.944000Z","checksum":[{"algorithm":"MD5","value":"4d21b35de4619315e8ba36dfa596eb44","checksum_date":"2020-04-04T17:07:07.777Z"}],"footprint":{"type":"polygon","coordinates":[[[-74.8571,-120.3411],[-75.4484,-121.9204],[-76.4321,-122.3625],[-74.8571,-120.3411]]]}}'
    	 *    
    	 *    ... or more records
    	 *    	 (adjust path to bulk data file (see src/test/resources/prip-testdata.json)
    	 *    $ curl -XPOST "http://localhost:9200/_bulk" -H 'Content-Type: application/json' --data-binary @prip-testdata.json
    	 *    
    	 * 5. Retrieve them over the PRIP Frontend
    	 *    $ curl http://localhost:8080/odata/v1/Products
    	 */
    	
        SpringApplication.run(StandaloneApplication.class, args);
    }
}
