/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.sail.nativerdf.btree.BTree;
import org.eclipse.rdf4j.sail.nativerdf.btree.RecordComparator;
import org.eclipse.rdf4j.sail.nativerdf.btree.RecordIterator;

/**
 * A cache for fixed size byte array records. This cache uses a temporary file to store the records. This file is
 * deleted upon calling {@link #discard()}.
 *
 * @author Arjohn Kampman
 */
@SuppressWarnings("deprecation")
final class SortedRecordCache extends AbstractRecordCache {

	/*------------*
	 * Attributes *
	 *------------*/

	private final BTree btree;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public SortedRecordCache(File cacheDir, int recordSize, RecordComparator comparator) throws IOException {
		this(cacheDir, recordSize, Long.MAX_VALUE, comparator);
	}

	public SortedRecordCache(File cacheDir, int recordSize, long maxRecords, RecordComparator comparator)
			throws IOException {
		super(maxRecords);
		btree = new BTree(cacheDir, "txncache", 4096, recordSize, comparator);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected void storeRecordInternal(byte[] record) throws IOException {
		btree.insert(record);
	}

	@Override
	protected RecordIterator getRecordsInternal() {
		return btree.iterateAll();
	}

	@Override
	protected void clearInternal() throws IOException {
		btree.clear();
	}

	@Override
	public void discard() throws IOException {
		btree.delete();
	}
}
