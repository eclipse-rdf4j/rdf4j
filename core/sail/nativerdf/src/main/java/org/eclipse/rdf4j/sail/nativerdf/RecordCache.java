/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.nativerdf;

import java.io.IOException;

import org.eclipse.rdf4j.sail.nativerdf.btree.RecordIterator;

/**
 * A cache for fixed size byte array records.
 *
 * @author Håvard M. Ottestad
 */
interface RecordCache {

	int BLOCK_SIZE = 1024 * 4;

	void setMaxRecords(long maxRecords);

	/**
	 * Gets the number of records currently stored in the cache, throwing an {@link IllegalStateException} if the cache
	 * is no longer {@link #isValid() valid}.
	 *
	 * @return records in the cache
	 * @throws IllegalStateException If the cache is not/no longer {@link #isValid() valid}.
	 */
	long getRecordCount();

	/**
	 * Stores a record in the cache.
	 *
	 * @param data The record to store.
	 */
	void storeRecord(byte[] data) throws IOException;

	/**
	 * Stores the records from the supplied cache into this cache.
	 *
	 * @param otherCache The cache to copy the records from.
	 */
	void storeRecords(RecordCache otherCache) throws IOException;

	/**
	 * Clears the cache, deleting all stored records.
	 */
	void clear() throws IOException;

	/**
	 * Gets all records that are stored in the cache, throwing an {@link IllegalStateException} if the cache is no
	 * longer {@link #isValid() valid}.
	 *
	 * @return An iterator over all records.
	 * @throws IllegalStateException If the cache is not/no longer {@link #isValid() valid}.
	 */
	RecordIterator getRecords() throws IOException;

	/**
	 * Checks whether the cache is still valid. Caches are valid if the number of stored records is smaller than or
	 * equal to the maximum number of records.
	 */
	boolean isValid();

	/**
	 * Discards the cache, deleting any allocated files.
	 */
	void discard() throws IOException;
}
