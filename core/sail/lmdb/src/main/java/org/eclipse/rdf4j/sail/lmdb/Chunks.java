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

import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.E;
import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.compareRegion;
import static org.lwjgl.util.lmdb.LMDB.MDB_GET_BOTH_RANGE;
import static org.lwjgl.util.lmdb.LMDB.MDB_KEYEXIST;
import static org.lwjgl.util.lmdb.LMDB.MDB_LAST_DUP;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOOVERWRITE;
import static org.lwjgl.util.lmdb.LMDB.MDB_PREV_DUP;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_del;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_put;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.rdf4j.sail.lmdb.util.VarintTupleIO;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * Utility methods for maintaining sorted LMDB duplicate-value chunks that contain varint-encoded tuples.
 */
public class Chunks {
	/**
	 * Maximum number of bytes to keep in the first encoded output chunk before spilling remaining tuples into a second
	 * chunk.
	 */
	public static final int MAX_CHUNK_SIZE = 100; // 511 - TripleIndex.MAX_KEY_LENGTH;

	/**
	 * Inserts a tuple into the sorted duplicate-value chunks for the current key, rewriting the affected chunk when
	 * needed to preserve ordering and split oversized chunks.
	 *
	 * @param cursor      the LMDB cursor positioned on the duplicates for the key
	 * @param elements    the number of tuple elements encoded in each chunk entry
	 * @param keyVal      the key buffer used for cursor operations
	 * @param dataVal     the data buffer used for cursor operations
	 * @param newValueBuf the encoded tuple to insert
	 * @param target      scratch buffer used to encode replacement chunks
	 * @return the LMDB result code, or {@link org.lwjgl.util.lmdb.LMDB#MDB_KEYEXIST} when the tuple already exists
	 * @throws IOException if tuple decoding or encoding fails
	 */
	static int mergeChunk(long cursor, int elements, MDBVal keyVal, MDBVal dataVal,
			ByteBuffer newValueBuf, ByteBuffer target) throws IOException {

		dataVal.mv_data(newValueBuf);
		int rc = E(mdb_cursor_put(cursor, keyVal, dataVal, MDB_NOOVERWRITE));
		if (rc == MDB_SUCCESS) {
			return MDB_SUCCESS;
		}

		final var keyBuffer = keyVal.mv_data();

		// Position cursor at the first duplicate value for this key that is >= newValueBuf.
		dataVal.mv_data(newValueBuf);
		rc = E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_GET_BOTH_RANGE));

		if (rc == MDB_SUCCESS) {
			var buffer = dataVal.mv_data();
			if (compareRegion(newValueBuf, 0, buffer, 0, Math.min(newValueBuf.remaining(), buffer.remaining())) == 0) {
				// The new value is equal to the first duplicate value >= newValueBuf. The tuple already exists in this
				// chunk.
				return MDB_KEYEXIST;
			}
			// The new value is smaller than the first duplicate value >= newValueBuf. Step back to the previous
			// duplicate value.
			if (E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_PREV_DUP)) != MDB_SUCCESS) {
				// ignore
			}
		} else {
			E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_SET));
			E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_LAST_DUP));
		}

		// We are positioned at the first duplicate value < newValueBuf.
		// Find the correct insertion point for newValueBuf in the selected chunk, and check if it already exists.
		var existing = new VarintTupleIO(elements, dataVal.mv_data());
		int diff = existing.seek(newValueBuf);
		if (diff == 0) {
			return MDB_KEYEXIST;
		}

		target.clear();

		// Copy the already-consumed prefix of the selected chunk, then insert newValueBuf, then
		// continue copying tuples from the selected chunk until the first output chunk is full.
		var encoder = existing.createEncoder(target);
		int firstPos = target.position();

		boolean addValueToSecondChunk = false;
		boolean addedAll = false;
		if (firstPos < MAX_CHUNK_SIZE) {
			encoder.append(newValueBuf);
			addedAll = encoder.appendAllTuples(existing, MAX_CHUNK_SIZE);

			firstPos = target.position();
		} else {
			// No room left in the first chunk after copying the prefix; start a second chunk with the new value.
			addValueToSecondChunk = true;
		}

		if (addValueToSecondChunk || !addedAll && existing.hasNext()) {
			encoder.resetDeltaEncoding();

			if (addValueToSecondChunk) {
				encoder.append(newValueBuf);
			}

			if (existing.hasNext()) {
				encoder.appendNextTuple(existing);
				encoder.appendAllTuples(existing, Integer.MAX_VALUE);
			}
		}

		int secondPos = target.position();

		// Replace the selected duplicate value with one or two newly encoded duplicate values.
		E(mdb_cursor_del(cursor, 0));

		keyVal.mv_data(keyBuffer);

		target.position(0);
		target.limit(firstPos);
		dataVal.mv_data(target);
		E(mdb_cursor_put(cursor, keyVal, dataVal, 0));

		if (firstPos < secondPos) {
			target.position(firstPos);
			target.limit(secondPos);
			dataVal.mv_data(target);
			E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
		}

		return MDB_SUCCESS;
	}

	/**
	 * Removes a tuple from the sorted duplicate-value chunks for the current key.
	 *
	 * @param cursor        the LMDB cursor positioned on the duplicates for the key
	 * @param elements      the number of tuple elements encoded in each chunk entry
	 * @param keyVal        the key buffer used for cursor operations
	 * @param dataVal       the data buffer used for cursor operations
	 * @param valueToDelete the encoded tuple to remove
	 * @param target        scratch buffer used to encode the remaining tuples in the affected chunk
	 * @return {@code true} if the tuple was removed, otherwise {@code false}
	 * @throws IOException if tuple decoding or encoding fails
	 */
	static boolean deleteFromChunk(long cursor, int elements, MDBVal keyVal, MDBVal dataVal,
			ByteBuffer valueToDelete, ByteBuffer target) throws IOException {
		int rc = E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_GET_BOTH_RANGE));
		if (rc != MDB_SUCCESS) {
			return false;
		}

		var buffer = dataVal.mv_data();
		if (compareRegion(valueToDelete, 0, buffer, 0, Math.min(valueToDelete.remaining(), buffer.remaining())) < 0) {
			// The value to delete is smaller than the first duplicate value >= valueToDelete. Step back to the previous
			// duplicate value.
			if (E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_PREV_DUP)) != MDB_SUCCESS) {
				// ignore
			}
		}

		var existing = new VarintTupleIO(elements, dataVal.mv_data());
		int diff = existing.seek(valueToDelete);
		if (diff != 0) {
			return false;
		}

		existing.resetTuple();
		target.clear();
		var encoder = existing.createEncoder(target);
		existing.skipTuple();
		while (existing.hasNext()) {
			encoder.appendNextTuple(existing);
		}
		E(mdb_cursor_del(cursor, 0));
		if (target.position() > 0) {
			target.flip();
			dataVal.mv_data(target);
			E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
		}
		return true;
	}
}
