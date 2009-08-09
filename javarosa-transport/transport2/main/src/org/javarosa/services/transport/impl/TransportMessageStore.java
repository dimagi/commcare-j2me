package org.javarosa.services.transport.impl;

import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import org.javarosa.services.transport.TransportCache;
import org.javarosa.services.transport.TransportMessage;

import de.enough.polish.io.RmsStorage;

/**
 * A TransportMessageStore is necessary since not all attempts to send succeed.
 * 
 * Every message given to the TransportService is persisted immediately, but
 * distinctions are made when querying the TransportMessageStore based on the
 * message status (i.e. the number of "cached" messages is not equal to the
 * number of messages in the store, but the number of messages in the store with
 * the status CACHED)
 * 
 */
public class TransportMessageStore implements TransportCache {

	/**
	 * These constants are used to identify objects in persistent storage
	 * 
	 * Q_STORENAME - the queue of messages to be sent RECENTLY_SENT_STORENAME -
	 * messages recently sent QID_STORENAME - storage for the message id counter
	 * 
	 */

	private static final String Q_STORENAME = "JavaROSATransQ";
	private static final String QID_STORENAME = "JavaROSATransQId";
	private static final String RECENTLY_SENT_STORENAME = "JavaROSATransQSent";

	private static final int RECENTLY_SENT_STORE_MAXSIZE = 15;

	/**
	 * The persistent store - it is partitioned into three corresponding to the
	 * three constants above
	 */
	private RmsStorage storage = new RmsStorage();

	/**
	 * We cache the size (in terms of numbers of records) of each of the
	 * persistent store partitions
	 */
	private Hashtable cachedCounts = new Hashtable();

	/**
	 * @param testing
	 */
	public TransportMessageStore() {
		updateCachedCounts();
	}

	/**
	 * @return
	 */
	public int getCachedMessagesSize() {
		Integer size = (Integer) this.cachedCounts.get(Integer
				.toString(TransportMessageStatus.CACHED));
		return size.intValue();
	}

	/**
	 * @return A Vector of TransportMessages waiting to be sent
	 */
	public Vector getCachedMessages() {
		Vector messages = readAll(Q_STORENAME);
		Vector cached = new Vector();
		for (int i = 0; i < messages.size(); i++) {
			TransportMessage message = (TransportMessage) messages.elementAt(i);
			if (message.getStatus() == TransportMessageStatus.CACHED) {
				cached.addElement(message);
			} else {
				if (isQueuingExpired(message)) {
					cached.addElement(message);
				}
			}
		}
		return messages;
	}

	/**
	 * 
	 * If a SenderThread is interrupted in some way, a message might not get the
	 * "Cached" status and be stuck with the "Queued" status instead
	 * 
	 * This method fixes that by checking the age of the message. If the age is
	 * greater than the time it would live in the SenderThread, then it should
	 * have the Cached status.
	 * 
	 * @param message
	 * @return
	 */
	private static boolean isQueuingExpired(TransportMessage message) {
		long now = new Date().getTime();
		long deadline = message.getQueuingDeadline();
		return (deadline > now);
	}

	/**
	 * @return A Vector of TransportMessages recently sent
	 */
	public Vector getRecentlySentMessages() {
		return readAll(RECENTLY_SENT_STORENAME);
	}

	/**
	 * 
	 * Add a new message to the send queue
	 * 
	 * @param message
	 * @throws IOException
	 */
	public String cache(TransportMessage message) throws TransportException {
		String id = getNextQueueIdentifier();
		message.setQueueIdentifier(id);
		message.setStatus(TransportMessageStatus.QUEUED);
		Vector records = readAll(Q_STORENAME);
		records.addElement(message);
		saveAll(records, Q_STORENAME);
		return id;
	}

	/**
	 * 
	 * Remove a message from the send queue
	 * 
	 * 
	 * @param success
	 * @throws IOException
	 */
	public void decache(TransportMessage message) throws TransportException {
		Vector records = readAll(Q_STORENAME);
		TransportMessage m = find(message.getQueueIdentifier(), records);
		if (m == null) {
			throw new IllegalArgumentException("No queued message with id="
					+ message.getQueueIdentifier());
		}
		records.removeElement(m);
		saveAll(records, Q_STORENAME);

		// if we're dequeuing a successfully sent message
		// then transfer it to the recently sent list
		if (message.isSuccess()) {
			Vector recentlySent = readAll(RECENTLY_SENT_STORENAME);
			if (recentlySent == null) {
				recentlySent = new Vector();
			}

			// ensure that the recently sent store doesn't grow indefinitely
			// by limiting its size
			if (recentlySent.size() == RECENTLY_SENT_STORE_MAXSIZE) {
				recentlySent.removeElementAt(0);
			}

			recentlySent.addElement(message);
			saveAll(recentlySent, RECENTLY_SENT_STORENAME);
		}

	}

	/**
	 * 
	 * 
	 * Given a vector of messages, find the message with the given id
	 * 
	 * 
	 * @param id
	 * @param records
	 * @return
	 */
	private TransportMessage find(String id, Vector records) {
		for (int i = 0; i < records.size(); i++) {
			TransportMessage message = (TransportMessage) records.elementAt(i);
			if (message.getQueueIdentifier().equals(id))
				return message;
		}
		return null;
	}

	/**
	 * Given an id, look in the send queue and the recently sent queue,
	 * returning the message if it is found (null otherwise)
	 * 
	 * If the message is in the transport queue, then the success parameter will
	 * be false (and if found in the recentlySent queue, it will be set to true)
	 * 
	 * 
	 * @param id
	 * @return
	 */
	public TransportMessage findMessage(String id) {
		Vector records = readAll(Q_STORENAME);
		for (int i = 0; i < records.size(); i++) {
			TransportMessage message = (TransportMessage) records.elementAt(i);
			if (message.getQueueIdentifier().equals(id))
				return message;
		}

		Vector sentRecords = readAll(RECENTLY_SENT_STORENAME);
		for (int i = 0; i < sentRecords.size(); i++) {
			TransportMessage message = (TransportMessage) sentRecords
					.elementAt(i);
			if (message.getQueueIdentifier().equals(id))
				return message;
		}

		return null;
	}

	/**
	 * 
	 * Get the next available queue identifier to assign to a newly queued
	 * message
	 * 
	 * 
	 * @return
	 * @throws IOException
	 */
	private String getNextQueueIdentifier() throws TransportException {
		// get the most recently used id
		Vector v = readAll(QID_STORENAME);
		if ((v == null) || (v.size() == 0)) {
			// null means there wasn't one, so create, save and return one
			Integer i = new Integer(1);
			v = new Vector();
			v.addElement(i);
			saveAll(v, QID_STORENAME);
			return i.toString();
		} else {

			Integer i = (Integer) v.firstElement();

			// increment the count to create a new one, save it and return it
			Integer newI = new Integer(i.intValue() + 1);

			v.removeAllElements();
			v.addElement(newI);
			saveAll(v, QID_STORENAME);
			return newI.toString();
		}

	}

	/**
	 * @param store
	 * @return
	 */
	public Vector readAll(String store) {
		Vector records = new Vector();
		try {

			records = (Vector) storage.read(store);
		} catch (IOException e) {
			// storage doesn't yet exist (according to Polish)
		}
		return records;
	}

	/**
	 * @param records
	 * @param c
	 * @throws IOException
	 */
	private void saveAll(Vector records, String store)
			throws TransportException {
		try {
			this.storage.delete(store);
		} catch (IOException e) {
			// storage didn't exist (according to Polish)
		}
		try {
			this.storage.save(records, store);
		} catch (IOException e) {
			throw new TransportException(e);
		}

		updateCachedCounts();
	}

	/**
	 * 
	 */
	private void updateCachedCounts() {
		int queued = 0;
		int cached = 0;
		// cache the counts first
		Vector messages = readAll(Q_STORENAME);
		for (int i = 0; i < messages.size(); i++) {
			TransportMessage message = (TransportMessage) messages.elementAt(i);
			if (message.getStatus() == TransportMessageStatus.QUEUED)
				queued++;
			if (message.getStatus() == TransportMessageStatus.CACHED)
				cached++;
			if (message.getStatus() == TransportMessageStatus.SENT)
				throw new RuntimeException("Sent message in the queue");
		}
		this.cachedCounts.put(Integer.toString(TransportMessageStatus.CACHED),
				new Integer(cached));
		this.cachedCounts.put(Integer.toString(TransportMessageStatus.QUEUED),
				new Integer(queued));

		// sent messages in another store
		int recentlySentSize = readAll(RECENTLY_SENT_STORENAME).size();
		this.cachedCounts.put(Integer.toString(TransportMessageStatus.QUEUED),
				new Integer(recentlySentSize));
	}

	/**
	 * @param message
	 * @throws IOException
	 */
	public void updateMessage(TransportMessage message)
			throws TransportException {
		Vector records = readAll(Q_STORENAME);
		for (int i = 0; i < records.size(); i++) {
			TransportMessage m = (TransportMessage) records.elementAt(i);
			if (m.getQueueIdentifier().equals(message.getQueueIdentifier())) {
				m.setStatus(message.getStatus());
				m.setFailureReason(message.getFailureReason());
			}
		}

		saveAll(records, Q_STORENAME);

	}

	public void clearCache() {
		try {
			storage.delete(Q_STORENAME);
			storage.save(new Vector(), Q_STORENAME);
		} catch (IOException e) {
			throw new RuntimeException(
					"Problem clearing the cache of TransportMessages.");
		}
	}

}