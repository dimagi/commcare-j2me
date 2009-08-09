package org.javarosa.services.transport.impl;

import java.util.Date;

import org.javarosa.services.transport.TransportMessage;

/**
 * Abstract part implementation of TransportMessage, for subclassing by full
 * implementations
 * 
 */
public abstract class BasicTransportMessage implements TransportMessage {

	private Object content;
	private String contentType;
	private int status;
	private String failureReason;
	private int failureCount;
	private String queueIdentifier;
	private Date created;
	private Date sent;
	private long queuingDeadline;

	public Date getSent() {
		return sent;
	}

	public void setSent(Date sent) {
		this.sent = sent;
	}

	public long getQueuingDeadline() {
		return queuingDeadline;
	}

	public void setQueuingDeadline(long queuingDeadline) {
		this.queuingDeadline = queuingDeadline;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public Object getContent() {
		return content;
	}

	public void setContent(Object content) {
		this.content = content;
	}

	public int getStatus() {
		return this.status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public boolean isSuccess() {
		return this.status == TransportMessageStatus.SENT;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public void setFailureReason(String failureReason) {
		this.failureReason = failureReason;
	}

	public int getFailureCount() {
		return failureCount;
	}

	public void incrementFailureCount() {
		this.failureCount++;
	}

	public String getQueueIdentifier() {
		return queueIdentifier;
	}

	public void setQueueIdentifier(String queueIdentifier) {
		this.queueIdentifier = queueIdentifier;
	}

}