/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.sail.nativerdf.btree.RecordIterator;

/**
 * A cache for fixed size byte array records. This cache uses a temporary file
 * to store the records. This file is deleted upon calling {@link #discard()}.
 * 
 * @author Arjohn Kampman
 */
abstract class RecordCache {

	/*------------*
	 * Attributes *
	 *------------*/

	private final AtomicLong maxRecords;

	private final AtomicLong recordCount;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public RecordCache()
		throws IOException
	{
		this(Long.MAX_VALUE);
	}

	public RecordCache(long maxRecords)
		throws IOException
	{
		this.maxRecords = new AtomicLong(maxRecords);
		this.recordCount = new AtomicLong();
	}

	/*---------*
	 * Methods *
	 *---------*/

	public final long getMaxRecords() {
		return maxRecords.get();
	}

	public final void setMaxRecords(long maxRecords) {
		this.maxRecords.set(maxRecords);
	}

	/**
	 * Gets the number of records currently stored in the cache, throwing an
	 * {@link IllegalStateException} if the cache is no longer {@link #isValid()
	 * valid}.
	 * 
	 * @return
	 * @throws IllegalStateException
	 *         If the cache is not/no longer {@link #isValid() valid}.
	 */
	public final long getRecordCount() {
		if (isValid()) {
			return recordCount.get();
		}

		throw new IllegalStateException();
	}

	/**
	 * Stores a record in the cache.
	 * 
	 * @param data
	 *        The record to store.
	 */
	public final void storeRecord(byte[] data)
		throws IOException
	{
		long spareSlots = maxRecords.get() - recordCount.get();

		if (spareSlots > 0L) {
			storeRecordInternal(data);
			recordCount.incrementAndGet();
		}
		else if (spareSlots == 0L) {
			// invalidate the cache
			recordCount.incrementAndGet();
		}
	}

	/**
	 * Stores the records from the supplied cache into this cache.
	 * 
	 * @param otherCache
	 *        The cache to copy the records from.
	 */
	public final void storeRecords(RecordCache otherCache)
		throws IOException
	{
		if (recordCount.get() <= maxRecords.get()) {
			RecordIterator recIter = otherCache.getRecords();
			try {
				byte[] record;
				while ((record = recIter.next()) != null && recordCount.incrementAndGet() <= maxRecords.get()) {
					storeRecordInternal(record);
				}
			}
			finally {
				recIter.close();
			}
		}
	}

	protected abstract void storeRecordInternal(byte[] data)
		throws IOException;

	/**
	 * Clears the cache, deleting all stored records.
	 */
	public final void clear()
		throws IOException
	{
		clearInternal();
		recordCount.set(0L);
	}

	protected abstract void clearInternal()
		throws IOException;

	/**
	 * Gets all records that are stored in the cache, throwing an
	 * {@link IllegalStateException} if the cache is no longer {@link #isValid()
	 * valid}.
	 * 
	 * @return An iterator over all records.
	 * @throws IllegalStateException
	 *         If the cache is not/no longer {@link #isValid() valid}.
	 */
	public final RecordIterator getRecords() {
		if (isValid()) {
			return getRecordsInternal();
		}

		throw new IllegalStateException();
	}

	protected abstract RecordIterator getRecordsInternal();

	/**
	 * Checks whether the cache is still valid. Caches are valid if the number of
	 * stored records is smaller than or equal to the {@link #getMaxRecords()
	 * maximum number of records}.
	 */
	public final boolean isValid() {
		return recordCount.get() <= maxRecords.get();
	}

	/**
	 * Discards the cache, deleting any allocated files.
	 */
	public abstract void discard()
		throws IOException;
}
