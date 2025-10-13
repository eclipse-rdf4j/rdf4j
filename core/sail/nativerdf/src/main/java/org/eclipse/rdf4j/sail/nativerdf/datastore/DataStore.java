/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.datastore;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.nativerdf.ValueStore;
import org.eclipse.rdf4j.sail.nativerdf.model.NativeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that provides indexed storage and retrieval of arbitrary length data.
 *
 * @author Arjohn Kampman
 */
public class DataStore implements Closeable {

	/*-----------*
	 * Variables *
	 *-----------*/

	private static final Logger logger = LoggerFactory.getLogger(DataStore.class);

	private final DataFile dataFile;

	private final IDFile idFile;

	private final HashFile hashFile;

	private ValueStore valueStore;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public DataStore(File dataDir, String filePrefix) throws IOException {
		this(dataDir, filePrefix, false);
	}

	public DataStore(File dataDir, String filePrefix, boolean forceSync) throws IOException {
		dataFile = new DataFile(new File(dataDir, filePrefix + ".dat"), forceSync);
		idFile = new IDFile(new File(dataDir, filePrefix + ".id"), forceSync);
		hashFile = new HashFile(new File(dataDir, filePrefix + ".hash"), forceSync);
	}

	public DataStore(File dataDir, String filePrefix, boolean forceSync, ValueStore valueStore) throws IOException {
		this(dataDir, filePrefix, forceSync);
		this.valueStore = valueStore;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the value for the specified ID.
	 *
	 * @param id A value ID, should be larger than 0.
	 * @return The value for the ID, or <var>null</var> if no such value could be found.
	 * @throws IOException If an I/O error occurred.
	 */
	public byte[] getData(int id) throws IOException {
		assert id > 0 : "id must be larger than 0, is: " + id;

		long offset = idFile.getOffset(id);

		if (offset != 0L) {
			byte[] data = dataFile.getData(offset);

			assert data != null : "data must not be null";

			if (logger.isDebugEnabled()) {
				logger.debug("getData thread={} id={} offset={} resultLength={}", threadName(), id, offset,
						data.length);
			}

			if (data.length == 0 && NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES) {
				data = attemptToRecoverCorruptData(id, offset, data);
			}
			return data;
		}

		return null;
	}

	/**
	 *
	 * @param id
	 * @param offset
	 * @param data
	 * @return
	 * @throws IOException
	 * @throws RecoveredDataException if recovery was successful
	 */
	private byte[] attemptToRecoverCorruptData(int id, long offset, byte[] data) throws IOException {
		// Data corruption detected: ID points to an offset, but no data could be read. Attempt to recover.
		try {
			long offsetNoCache = idFile.getOffsetNoCache(id);
			if (offset != offsetNoCache) {
				logger.error("IDFile cache mismatch for id {}: cached={}, raw={}. Using raw.", id, offset,
						offsetNoCache);
				offset = offsetNoCache;
				data = dataFile.getData(offset);
			}
		} catch (IOException e) {
			// If raw read fails, keep cached offset
		}

		// Attempt recovery by using neighboring offsets to infer the bounds
		long startData = offset + 4; // default start if no previous valid entry
		// Find previous entry end: prevOffset + 4 + prevLength
		int prev = id - 1;
		for (; prev >= 1; prev--) {
			long po = idFile.getOffset(prev);
			try {
				long poRaw = idFile.getOffsetNoCache(prev);
				if (po != poRaw) {
					logger.error("IDFile cache mismatch for prev id {}: cached={}, raw={}. Using raw.", prev,
							po, poRaw);
					po = poRaw;
				}
			} catch (IOException e) {
				// use cached po if raw read fails
			}
			if (po > 0L) {
				try {
					byte[] prevData = dataFile.getData(po);
					if (prevData != null && prevData.length > 0) {
						try {
							if (valueStore != null && Thread.currentThread().getStackTrace().length < 512) {
								NativeValue nativeValue = valueStore.data2value(prev, prevData);
								logger.warn("Data in previous ID ({}) is: {}", prev, nativeValue);
							} else {
								logger.warn("Data in previous ID ({}) is: {}", prev,
										new String(prevData, StandardCharsets.UTF_8));
							}
						} catch (Exception ignored) {
						}
						startData = po + 4L + prevData.length;
						break;
					}
				} catch (Exception ignored) {
				}
			}
		}

		// Find next entry start as the end bound
		long endOffset = 0L;
		int maxId = idFile.getMaxID();
		int next = id + 1;
		for (; next <= maxId; next++) {
			long no = idFile.getOffset(next);
			try {
				long noRaw = idFile.getOffsetNoCache(next);
				if (no != noRaw) {
					logger.error("IDFile cache mismatch for next id {}: cached={}, raw={}. Using raw.", next,
							no, noRaw);
					no = noRaw;
				}
			} catch (IOException e) {
				// use cached value if raw read fails
			}
			if (no > 0L) {

				try {
					byte[] nextData = dataFile.getData(no);
					if (nextData != null && nextData.length > 0) {
						try {
							if (valueStore != null && Thread.currentThread().getStackTrace().length < 512) {
								NativeValue nativeValue = valueStore.data2value(next, nextData);
								logger.warn("Data in next ID ({}) is: {}", next, nativeValue);
							} else {
								logger.warn("Data in next ID ({}) is: {}", next,
										new String(nextData, StandardCharsets.UTF_8));
							}
						} catch (Exception ignored) {
						}
						endOffset = no;
						break;
					}
				} catch (Exception e) {
				}

			}
		}
		if (endOffset == 0L) {
			// Fallback: use current file size as end bound
			endOffset = dataFile.getFileSize();
		}
		if (endOffset > startData) {
			// tryRecoverBetweenOffsets expects an offset to a 4-byte length, so pass (startData - 4)
			byte[] recovered = dataFile.tryRecoverBetweenOffsets(Math.max(0L, startData - 4L), endOffset);
			throw new RecoveredDataException(id, recovered);
		}

		assert data.length == 0;
		return data;
	}

	/**
	 * Gets the ID for the specified value.
	 *
	 * @param queryData The value to get the ID for, must not be <var>null</var>.
	 * @return The ID for the specified value, or <var>-1</var> if no such ID could be found.
	 * @throws IOException If an I/O error occurred.
	 */
	public int getID(byte[] queryData) throws IOException {
		assert queryData != null : "queryData must not be null";

		int id;

		int hash = getDataHash(queryData);
		if (logger.isDebugEnabled()) {
			logger.debug("getID start thread={} hash={} summary={}", threadName(), hash, summarize(queryData));
		}

		HashFile.IDIterator iter = hashFile.getIDIterator(hash);
		try {
			while ((id = iter.next()) >= 0) {
				long offset = idFile.getOffset(id);
				byte[] data = dataFile.getData(offset);
				boolean match = Arrays.equals(queryData, data);

				if (logger.isDebugEnabled()) {
					logger.debug(
							"getID candidate thread={} hash={} candidateId={} offset={} match={} candidateSummary={}",
							threadName(), hash, id, offset, match, summarize(data));
				}

				if (match) {
					break;
				}
			}
		} finally {
			iter.close();
		}

		if (logger.isDebugEnabled()) {
			logger.debug("getID result thread={} hash={} id={}", threadName(), hash, id);
		}

		return id;
	}

	/**
	 * Returns the maximum value-ID that is in use.
	 *
	 * @return The largest ID, or <var>0</var> if the store does not contain any values.
	 */
	public int getMaxID() {
		return idFile.getMaxID();
	}

	/**
	 * Stores the supplied value and returns the ID that has been assigned to it. In case the data to store is already
	 * present, the ID of this existing data is returned.
	 *
	 * @param data The data to store, must not be <var>null</var>.
	 * @return The ID that has been assigned to the value.
	 * @throws IOException If an I/O error occurred.
	 */
	public synchronized int storeData(byte[] data) throws IOException {
		assert data != null : "data must not be null";

		if (logger.isDebugEnabled()) {
			int hash = getDataHash(data);
			logger.debug("storeData start thread={} hash={} summary={}", threadName(), hash, summarize(data));
		}

		int id = getID(data);

		if (id == -1) {
			int hash = getDataHash(data);
			long offset = dataFile.storeData(data);
			id = idFile.storeOffset(offset);
			hashFile.storeID(hash, id);
			if (logger.isDebugEnabled()) {
				logger.debug("storeData stored thread={} hash={} id={} offset={} summary={}", threadName(), hash, id,
						offset, summarize(data));
			}
		} else if (logger.isDebugEnabled()) {
			int hash = getDataHash(data);
			logger.debug("storeData reuse thread={} hash={} existingId={} summary={}", threadName(), hash, id,
					summarize(data));
		}

		return id;
	}

	/**
	 * Synchronizes any recent changes to the data to disk.
	 *
	 * @throws IOException If an I/O error occurred.
	 */
	public void sync() throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("sync thread={} invoked", threadName());
		}
		hashFile.sync();
		idFile.sync();
		dataFile.sync();
	}

	/**
	 * Removes all values from the DataStore.
	 *
	 * @throws IOException If an I/O error occurred.
	 */
	public void clear() throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("clear thread={} invoked", threadName());
		}
		try {
			hashFile.clear();
		} finally {
			try {
				idFile.clear();
			} finally {
				dataFile.clear();
			}
		}
	}

	/**
	 * Closes the DataStore, releasing any file references, etc. In case a transaction is currently open, it will be
	 * rolled back. Once closed, the DataStore can no longer be used.
	 *
	 * @throws IOException If an I/O error occurred.
	 */
	@Override
	public void close() throws IOException {
		try {
			hashFile.close();
		} finally {
			try {
				idFile.close();
			} finally {
				dataFile.close();
			}
		}
	}

	/**
	 * Gets a hash code for the supplied data.
	 *
	 * @param data The data to calculate the hash code for.
	 * @return A hash code for the supplied data.
	 */
	private static String summarize(byte[] data) {
		if (data == null) {
			return "null";
		}
		return "len=" + data.length + ",hash=" + Arrays.hashCode(data);
	}

	private static String threadName() {
		return Thread.currentThread().getName();
	}

	private int getDataHash(byte[] data) {
		CRC32 crc32 = new CRC32();
		crc32.update(data);
		return (int) crc32.getValue();
	}

	/*--------------------*
	 * Test/debug methods *
	 *--------------------*/

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println(
					"Usage: java org.eclipse.rdf4j.sesame.sailimpl.nativerdf.datastore.DataStore <data-dir> <file-prefix>");
			return;
		}

		System.out.println("Dumping DataStore contents...");
		File dataDir = new File(args[0]);
		DataFile.DataIterator iter;
		try (DataStore dataStore = new DataStore(dataDir, args[1])) {
			iter = dataStore.dataFile.iterator();
			while (iter.hasNext()) {
				byte[] data = iter.next();

				System.out.println(ByteArrayUtil.toHexString(data));
			}
		}
	}
}
