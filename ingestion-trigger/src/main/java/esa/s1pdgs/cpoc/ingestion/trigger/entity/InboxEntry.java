package esa.s1pdgs.cpoc.ingestion.trigger.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Objects;

@Document
public class InboxEntry {

	@Transient
	public static final String ENTRY_SEQ_KEY = "inboxEntry";

	@Id
	private long id;
	private String name;
	private String relativePath;
	private String pickupURL;
	private Date lastModified;
	private long size;

	public InboxEntry() {
	}

	public InboxEntry(final long id, final String name, final String relativePath, final String pickupURL, final Date lastModified,
			final long size) {
		this.id = id;
		this.name = name;
		this.relativePath = relativePath;
		this.pickupURL = pickupURL;
		this.lastModified = lastModified;
		this.setSize(size);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getRelativePath() {
		return relativePath;
	}

	public void setRelativePath(final String relativePath) {
		this.relativePath = relativePath;
	}

	public String getPickupURL() {
		return pickupURL;
	}

	public void setPickupURL(final String pickupURL) {
		this.pickupURL = pickupURL;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		// WARNING: Don't take 'id' into account when implementing equals/hashCode
		// because it's always 0 when created from Inbox
		final InboxEntry other = (InboxEntry) obj;
		return Objects.equals(name, other.name) && Objects.equals(pickupURL, other.pickupURL)
				&& Objects.equals(relativePath, other.relativePath)
				&& size == other.size;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, pickupURL, relativePath, size);
	}

	@Override
	public String toString() {
		return String.format("InboxEntry [name=%s, relativePath=%s, pickupURL=%s, lastModified=%s, size=%s]", name,
				relativePath, pickupURL, lastModified, size);
	}
}
