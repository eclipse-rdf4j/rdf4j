/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.io.IOException;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;

/**
 * A value-level view of a prefix-run scan: owns the read transaction and the cursor and resolves the ids of the
 * representative statements to values on demand.
 */
final class LmdbPrefixRunScan implements AutoCloseable {

	private static final LmdbPrefixRunScan EMPTY = new LmdbPrefixRunScan(null, LmdbPrefixRunCursor.EMPTY, null);

	private final Txn txn;
	private final LmdbPrefixRunCursor cursor;
	private final ValueStore valueStore;

	LmdbPrefixRunScan(Txn txn, LmdbPrefixRunCursor cursor, ValueStore valueStore) {
		this.txn = txn;
		this.cursor = cursor;
		this.valueStore = valueStore;
	}

	/** A scan that emits nothing, used when a bound value of the pattern does not exist in the store. */
	static LmdbPrefixRunScan empty() {
		return EMPTY;
	}

	boolean next() throws IOException {
		return cursor.next();
	}

	/** See {@link LmdbPrefixRunCursor#quad()}. */
	long[] quad() {
		return cursor.quad();
	}

	long runRowCount() {
		return cursor.runRowCount();
	}

	/**
	 * Resolves the given field of the current representative statement.
	 *
	 * @return the value, or {@code null} for the null context
	 */
	Value getValue(int field) throws IOException {
		long id = cursor.quad()[field];
		if (field == TripleIndex.CONTEXT_IDX && (id == 0L || id == LmdbValue.UNKNOWN_ID)) {
			return null;
		}
		return valueStore.getValue(id);
	}

	String getIndexName() {
		return cursor.getIndexName();
	}

	@Override
	public void close() {
		try {
			cursor.close();
		} finally {
			if (txn != null) {
				txn.close();
			}
		}
	}
}
