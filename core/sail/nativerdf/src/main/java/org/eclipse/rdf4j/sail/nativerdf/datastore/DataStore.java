/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.datastore;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.CRC32;

import org.eclipse.rdf4j.common.io.ByteArrayUtil;

/**
 * Class that provides indexed storage and retrieval of arbitrary length data.
 *
 * @author Arjohn Kampman
 */
public class DataStore implements Closeable {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final DataFile dataFile;

	private final IDFile idFile;

	private final HashFile hashFile;

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

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the value for the specified ID.
	 *
	 * @param id A value ID, should be larger than 0.
	 * @return The value for the ID, or <var>null</var> if no such value could be found.
	 * @exception IOException If an I/O error occurred.
	 */
	public byte[] getData(int id) throws IOException {
		assert id > 0 : "id must be larger than 0, is: " + id;

		// Data not in cache or cache not used, fetch from file
		long offset = idFile.getOffset(id);

		if (offset != 0L) {
			return dataFile.getData(offset);
		}

		return null;
	}

	/**
	 * Gets the ID for the specified value.
	 *
	 * @param queryData The value to get the ID for, must not be <var>null</var>.
	 * @return The ID for the specified value, or <var>-1</var> if no such ID could be found.
	 * @exception IOException If an I/O error occurred.
	 */
	public int getID(byte[] queryData) throws IOException {
		assert queryData != null : "queryData must not be null";

		int id;

		// Value not in cache or cache not used, fetch from file
		int hash = getDataHash(queryData);
		HashFile.IDIterator iter = hashFile.getIDIterator(hash);
		try {
			while ((id = iter.next()) >= 0) {
				long offset = idFile.getOffset(id);
				byte[] data = dataFile.getData(offset);

				if (Arrays.equals(queryData, data)) {
					// Matching data found
					break;
				}
			}
		} finally {
			iter.close();
		}

		return id;
	}

	/**
	 * Returns the maximum value-ID that is in use.
	 *
	 * @return The largest ID, or <var>0</var> if the store does not contain any values.
	 * @throws IOException If an I/O error occurs.
	 */
	public int getMaxID() throws IOException {
		return idFile.getMaxID();
	}

	/**
	 * Stores the supplied value and returns the ID that has been assigned to it. In case the data to store is already
	 * present, the ID of this existing data is returned.
	 *
	 * @param data The data to store, must not be <var>null</var>.
	 * @return The ID that has been assigned to the value.
	 * @exception IOException If an I/O error occurred.
	 */
	public int storeData(byte[] data) throws IOException {
		assert data != null : "data must not be null";

		int id = getID(data);

		if (id == -1) {
			// Data not stored yet, store it under a new ID.
			long offset = dataFile.storeData(data);
			id = idFile.storeOffset(offset);
			hashFile.storeID(getDataHash(data), id);
		}

		return id;
	}

	/**
	 * Synchronizes any recent changes to the data to disk.
	 *
	 * @exception IOException If an I/O error occurred.
	 */
	public void sync() throws IOException {
		hashFile.sync();
		idFile.sync();
		dataFile.sync();
	}

	/**
	 * Removes all values from the DataStore.
	 *
	 * @exception IOException If an I/O error occurred.
	 */
	public void clear() throws IOException {
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
	 * @exception IOException If an I/O error occurred.
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
