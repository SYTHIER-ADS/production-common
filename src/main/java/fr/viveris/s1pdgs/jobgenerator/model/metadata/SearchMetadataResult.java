package fr.viveris.s1pdgs.jobgenerator.model.metadata;

import java.util.Objects;

/**
 * Class representing the result of a search query for metadata
 * 
 * @author Cyrielle Gailliard
 *
 */
public class SearchMetadataResult {

	/**
	 * Query
	 */
	private SearchMetadataQuery query;

	/**
	 * Result. Null if not found
	 */
	private SearchMetadata result;

	/**
	 * Constructor using fields
	 * 
	 * @param query
	 * @param res
	 */
	public SearchMetadataResult(final SearchMetadataQuery query) {
		this.query = query;
	}

	/**
	 * @return the query
	 */
	public SearchMetadataQuery getQuery() {
		return query;
	}

	/**
	 * @param query
	 *            the query to set
	 */
	public void setQuery(final SearchMetadataQuery query) {
		this.query = query;
	}

	/**
	 * @return the result
	 */
	public SearchMetadata getResult() {
		return result;
	}

	/**
	 * @param result
	 *            the result to set
	 */
	public void setResult(final SearchMetadata result) {
		this.result = result;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("{query: %s, result: %s}", query, result);
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(query, result);
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		boolean ret;
		if (this == obj) {
			ret = true;
		} else if (obj == null || getClass() != obj.getClass()) {
			ret = false;
		} else {
			SearchMetadataResult other = (SearchMetadataResult) obj;
			ret = Objects.equals(query, other.query) && Objects.equals(result, other.result);
		}
		return ret;
	}

}
