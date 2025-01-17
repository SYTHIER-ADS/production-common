package esa.s1pdgs.cpoc.metadata.extraction.service.elastic;

import java.io.IOException;

import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ElasticsearchDAO {

	/**
	 * Elasticsearch client
	 */
	@Autowired
	private RestHighLevelClient restHighLevelClient;
	
	
	public GetResponse get (GetRequest getRequest) throws IOException {
		return this.restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
	}
	
	public IndexResponse index (IndexRequest request) throws IOException {
		return this.restHighLevelClient.index(request, RequestOptions.DEFAULT);
	}
	
	public SearchResponse search (SearchRequest searchRequest) throws IOException {		
		return this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
	}

	public RefreshResponse refresh(RefreshRequest refreshRequest) throws IOException {
		return this.restHighLevelClient.indices().refresh(refreshRequest, RequestOptions.DEFAULT);
	}

}
