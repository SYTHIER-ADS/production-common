package esa.s1pdgs.cpoc.auxip.client.odata;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntitySetIteratorRequest;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.domain.ClientEntitySetIterator;
import org.apache.olingo.client.api.domain.ClientPrimitiveValue;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.apache.olingo.client.api.uri.FilterArgFactory;
import org.apache.olingo.client.api.uri.FilterFactory;
import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.client.api.uri.URIFilter;
import org.springframework.lang.NonNull;

import esa.s1pdgs.cpoc.auxip.client.AuxipClient;
import esa.s1pdgs.cpoc.auxip.client.AuxipProductMetadata;
import esa.s1pdgs.cpoc.auxip.client.config.AuxipClientConfigurationProperties.AuxipHostConfiguration;

/**
 * OData implementation of the AUXIP client
 */
public class AuxipOdataClient implements AuxipClient {
	static final Logger LOG = LogManager.getLogger(AuxipOdataClient.class);

	private final ODataClient odataClient;
	private final CloseableHttpClient downloadClient;
	private final HttpClientContext context;
	private final AuxipHostConfiguration hostConfig;
	private final URI rootServiceUrl;
	
	private final String entitySetName;
	private final String creationDateAttrName;
	private final String productNameAttrName;
	private final String idAttrName;
	private final String contentLengthAttrName;

	// --------------------------------------------------------------------------

	AuxipOdataClient(final ODataClient odataClient, final AuxipHostConfiguration hostConfig,
			final String entitySetName, final CloseableHttpClient downloadClient, final HttpClientContext context) {
		// TODO FIXME move logic/assertions to factory
		this.odataClient = Objects.requireNonNull(odataClient, "OData client must not be null!");
		this.hostConfig = Objects.requireNonNull(hostConfig, "host configuration must not be null!");
		this.entitySetName = Objects.requireNonNull(entitySetName, "entity set name must not be null!");
		this.creationDateAttrName = Objects.requireNonNull(hostConfig.getCreationDateAttributeName(),
				"creation date attribute name must not be null!");
		this.productNameAttrName = Objects.requireNonNull(hostConfig.getProductNameAttrName(),
				"product name attribute name must not be null!");
		this.idAttrName = Objects.requireNonNull(hostConfig.getIdAttrName(), "id attribute name must not be null!");
		this.contentLengthAttrName = Objects.requireNonNull(hostConfig.getContentLengthAttrName(),
				"content length attribute name must not be null!");

		this.rootServiceUrl = URI.create(
				Objects.requireNonNull(this.hostConfig.getServiceRootUri(), "the root service URL must not be null!"));
		this.downloadClient = downloadClient;
		this.context = context;
	}

	// --------------------------------------------------------------------------

	@Override
	public List<AuxipProductMetadata> getMetadata(@NonNull final LocalDateTime from, @NonNull final LocalDateTime to,
			final Integer pageSize, final Integer offset) {
		return this.getMetadata(from, to, pageSize, offset, null);
	}

	@Override
	public List<AuxipProductMetadata> getMetadata(@NonNull final LocalDateTime from, @NonNull final LocalDateTime to,
			final Integer pageSize, final Integer offset, final String productNameContains) {
		// prepare
		final URIFilter filters = this.buildFilters(from, to, productNameContains);
		final URI queryUri = this.buildQueryUri(Collections.singletonList(filters), pageSize, offset);
		
		// retrieve and map
		final ClientEntitySetIterator<ClientEntitySet, ClientEntity> response = this.readEntities(queryUri);
		final List<AuxipProductMetadata> result = this.mapToMetadata(response);
		LOG.debug("metadata search returned " + result.size() + " elements");

		return result;
	}
	
	@Override
	public InputStream read(@NonNull final UUID productMetadataId) {		
		// FIXME trailing slash is mandatory for this to work
		final URI productDownloadUrl = this.rootServiceUrl
				.resolve("Products(" + productMetadataId.toString() + ")/$value");
		
		LOG.debug("sending download request: " + productDownloadUrl);		
		if (hostConfig.isUseHttpClientDownload()) {			
		    final HttpGet httpget = new HttpGet(productDownloadUrl.toString());
		    try {
				final CloseableHttpResponse response = downloadClient.execute(httpget, context);
				final HttpEntity entity = response.getEntity();
				
			    // check if something has been returned and no error occurred
			    if ((entity == null) || (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK))
			    {
			    	throw new RuntimeException(
			    			String.format("Error on download of %s: %s", productMetadataId, response.getStatusLine())
			    	);
			    }				
				return new BufferedInputStream(entity.getContent())
				{
				  @Override public final void close() throws IOException
				  {
				    try
				    {
				      super.close();
				    }
				    finally
				    {
				      response.close();
				    }
				  }
				};
			} catch (final Exception e) {
				// TODO error handling
				throw new RuntimeException(e);
			}	
		}
		else {
			final ODataRetrieveResponse<InputStream> response = this.odataClient.getRetrieveRequestFactory()
					.getMediaRequest(productDownloadUrl).execute();
			
			LOG.debug("download request (" + productDownloadUrl + ") response status: " + response.getStatusCode() + " - "
					+ response.getStatusMessage());
			
			return response.getBody();
		}
	}
	
	// --------------------------------------------------------------------------

	private URIFilter buildFilters(final LocalDateTime from, final LocalDateTime to, final String productNameContains) {
		final FilterFactory filterFactory = this.odataClient.getFilterFactory();

		// timeframe filter
		final URIFilter lowerBoundFilter = filterFactory.ge(this.creationDateAttrName, from.toInstant(ZoneOffset.UTC));
		final URIFilter upperBoundFilter = filterFactory.lt(this.creationDateAttrName, to.toInstant(ZoneOffset.UTC));
		final URIFilter timeframeFilter = filterFactory.and(lowerBoundFilter, upperBoundFilter);

		// product name filter
		URIFilter productNameFilter = null;
		if (null != productNameContains && !productNameContains.isEmpty()) {
			final FilterArgFactory argFactory = filterFactory.getArgFactory();
			productNameFilter = filterFactory.match(argFactory.contains(argFactory.property(this.productNameAttrName),
					argFactory.literal(productNameContains)));
		}

		if (null != productNameFilter) {
			return filterFactory.and(timeframeFilter, productNameFilter);
		} else {
			return timeframeFilter;
		}
	}

	private URI buildQueryUri(final List<URIFilter> filters, final Integer pageSize, final Integer offset) {
		final URIBuilder uriBuilder = this.odataClient.newURIBuilder(this.rootServiceUrl.toString())
				.appendEntitySetSegment(this.entitySetName);

		if (null != filters && !filters.isEmpty()) {
			filters.forEach(f -> uriBuilder.filter(f));
		}

		if (null != pageSize) {
			uriBuilder.top(pageSize);
		}

		if (null != offset) {
			uriBuilder.skip(offset);
		}

		uriBuilder.orderBy(this.creationDateAttrName + " asc");

		return uriBuilder.build();
	}

	private ClientEntitySetIterator<ClientEntitySet, ClientEntity> readEntities(final URI absoluteUri) {
		final ODataEntitySetIteratorRequest<ClientEntitySet, ClientEntity> request = this.odataClient
				.getRetrieveRequestFactory().getEntitySetIteratorRequest(absoluteUri);
		request.setAccept("application/json");
		LOG.debug("sending request to PRIP: " + absoluteUri);
		//LOG.debug("sending request to PRIP: " + request.toString());
		
		final ODataRetrieveResponse<ClientEntitySetIterator<ClientEntitySet, ClientEntity>> response = request
				.execute();
		LOG.debug("metadata search response status: " + response.getStatusCode() + " - " + response.getStatusMessage());
		
		return response.getBody();
	}

	private List<AuxipProductMetadata> mapToMetadata(final ClientEntitySetIterator<ClientEntitySet, ClientEntity> response) {
		final List<AuxipProductMetadata> result = new ArrayList<>();

		if (null != response) {
			while (response.hasNext()) {
				final ClientEntity entity = response.next();
				final AuxipOdataProductMetadata metadata = new AuxipOdataProductMetadata(this.rootServiceUrl);

				// map ID
				final ClientProperty idProperty = entity.getProperty(this.idAttrName);
				if (null != idProperty) {
					final ClientPrimitiveValue idValue = idProperty.getPrimitiveValue();

					if (null != idValue) {
						final String idString = idValue.toString();
						try {
							metadata.setId(UUID.fromString(idString));
						} catch (final IllegalArgumentException e) {
							metadata.addParsingError("could not parse ID attribute '" + this.idAttrName
									+ "': error parsing value '" + idString + "' to UUID; " + e.getMessage());
						}
					} else {
						metadata.addParsingError("could not parse ID attribute '" + this.idAttrName + "': value null");
					}
				} else {
					metadata.addParsingError("could not parse ID attribute '" + this.idAttrName
							+ "': attribute not found or value null");
				}

				// map product name
				final ClientProperty productNameProperty = entity.getProperty(this.productNameAttrName);
				if (null != productNameProperty) {
					final ClientPrimitiveValue productNameValue = productNameProperty.getPrimitiveValue();

					if (null != productNameValue) {
						metadata.setProductName(productNameValue.toString());
					} else {
						metadata.addParsingError("could not parse product name attribute '" + this.productNameAttrName
								+ "': value null");
					}
				} else {
					metadata.addParsingError("could not parse product name attribute '" + this.productNameAttrName
							+ "': attribute not found or value null");
				}

				// map creation date
				final ClientProperty creationDateProperty = entity.getProperty(this.creationDateAttrName);
				if (null != creationDateProperty) {
					final ClientPrimitiveValue creationDateValue = creationDateProperty.getPrimitiveValue();

					if (null != creationDateValue) {
						final String creationDateString = creationDateValue.toString();
						try {
							metadata.setCreationDate(LocalDateTime.ofInstant(Instant.parse(creationDateString), ZoneId.of("UTC")));
						} catch (final Exception e) {
							metadata.addParsingError(
									"could not parse creation date attribute '" + this.creationDateAttrName
											+ "': error parsing date '" + creationDateString + "'; " + e.getMessage());
						}
					} else {
						metadata.addParsingError("could not parse creation date attribute '" + this.creationDateAttrName
								+ "': value null");
					}
				} else {
					metadata.addParsingError("could not parse creation date attribute '" + this.creationDateAttrName
							+ "': attribute not found or value null");
				}

				// map content length
				final ClientProperty contentLengthProperty = entity.getProperty(this.contentLengthAttrName);
				if (null != contentLengthProperty) {
					final ClientPrimitiveValue contentLengthValue = contentLengthProperty.getPrimitiveValue();

					if (null != contentLengthValue) {
						final String contentLengthString = contentLengthValue.toString();
						try {
							metadata.setContentLength(Long.parseLong(contentLengthString));
						} catch (final IllegalArgumentException e) {
							metadata.addParsingError("could not parse contentLength attribute '" + this.contentLengthAttrName
									+ "': error parsing value '" + contentLengthString + "' to long; " + e.getMessage());
						}
					} else {
						metadata.addParsingError("could not parse contentLength attribute '" + this.contentLengthAttrName + "': value null");
					}
				} else {
					metadata.addParsingError("could not parse contentLength attribute '" + this.contentLengthAttrName
							+ "': attribute not found or value null");
				}


				result.add(metadata);
			}

			response.close();
		}

		return result;
	}

}
