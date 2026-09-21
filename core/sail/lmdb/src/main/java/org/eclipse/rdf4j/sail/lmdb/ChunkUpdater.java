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
import static org.lwjgl.util.lmdb.LMDB.MDB_GET_BOTH;
import static org.lwjgl.util.lmdb.LMDB.MDB_GET_BOTH_RANGE;
import static org.lwjgl.util.lmdb.LMDB.MDB_KEYEXIST;
import static org.lwjgl.util.lmdb.LMDB.MDB_LAST_DUP;
import static org.lwjgl.util.lmdb.LMDB.MDB_PREV_DUP;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_del;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_put;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.TreeSet;

import org.eclipse.rdf4j.sail.lmdb.util.VarintTupleIO;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * Buffers tuple insertions for a single LMDB key and merges them with an existing encoded chunk when flushed.
 */
public final class ChunkUpdater {
	final ByteBuffer targetKey;
	ByteBuffer existingBuffer;

	ByteBuffer targetBuffer, tupleBuffer, existingBufferCache;
	VarintTupleIO.Encoder encoder;

	boolean sortedInsertion = false;

	/**
	 * Describes the location of a pending tuple inside {@link #tupleBuffer}.
	 *
	 * @param offset the tuple offset in {@link #tupleBuffer}
	 * @param length the tuple length in bytes
	 */
	record Tuple(int offset, int length) {
	}

	final TreeSet<Tuple> newTuples = new TreeSet<>(
			(a, b) -> compareRegion(tupleBuffer, a.offset, tupleBuffer, b.offset, Math.min(a.length, b.length)));

	VarintTupleIO chunkInput;

	/**
	 * Creates a new updater backed by buffers allocated from the supplied memory stack.
	 *
	 * @param stack the memory stack used to allocate the working buffers
	 */
	ChunkUpdater(MemoryStack stack) {
		this.targetKey = stack.malloc(TripleIndex.MAX_KEY_LENGTH);
		this.targetBuffer = stack.malloc(512);
		this.existingBufferCache = stack.malloc(512);
		this.tupleBuffer = stack.malloc(4096);
	}

	/**
	 * Enables or disables the optimization that assumes tuples are added in sorted order.
	 *
	 * @param sortedInsertion {@code true} when added tuples are already sorted
	 */
	void setSortedInsertion(boolean sortedInsertion) {
		this.sortedInsertion = sortedInsertion;
	}

	/**
	 * Adds a tuple to the pending update set for the current key.
	 * <p>
	 * When the key already exists, the tuple is checked against the selected duplicate chunk and merged on the next
	 * {@link #flush(long, int, MDBVal, MDBVal)} call. If the tuple already exists either in LMDB or among the pending
	 * tuples, {@link org.lwjgl.util.lmdb.LMDB#MDB_KEYEXIST} is returned.
	 *
	 * @param cursor      the LMDB cursor positioned on the target database
	 * @param elements    the number of tuple elements in the encoded chunk
	 * @param keyVal      the LMDB key wrapper
	 * @param dataVal     the LMDB value wrapper
	 * @param newValueBuf the encoded tuple to add
	 * @return the LMDB status code for the operation
	 * @throws IOException if tuple encoding fails while flushing buffered state
	 */
	int add(long cursor, int elements, MDBVal keyVal, MDBVal dataVal, ByteBuffer newValueBuf)
			throws IOException {
		final var keyBuffer = keyVal.mv_data();
		if (tupleBuffer.position() + newValueBuf.remaining() > tupleBuffer.capacity()) {
			flush(cursor, elements, keyVal, dataVal);
			keyVal.mv_data(keyBuffer);
		}

		int rc = E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_SET));
		boolean merge = true;
		if (rc == MDB_SUCCESS) {
			// Position cursor at the first duplicate value for this key that is >= newValueBuf.
			dataVal.mv_data(newValueBuf);
			rc = E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_GET_BOTH_RANGE));
			if (rc == MDB_SUCCESS) {
				var buffer = dataVal.mv_data();
				if (compareRegion(newValueBuf, 0, buffer, 0,
						Math.min(newValueBuf.remaining(), buffer.remaining())) == 0) {
					// The new value is equal to the first duplicate value >= newValueBuf. The tuple already exists in
					// this chunk.
					return MDB_KEYEXIST;
				}
				// The new value is smaller than the first duplicate value >= newValueBuf. Step back to the previous
				// duplicate value.
				if (E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_PREV_DUP)) != MDB_SUCCESS) {
					// ignore
				}
			} else {
				keyVal.mv_data(keyBuffer);
				if (E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_SET)) == MDB_SUCCESS) {
					E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_LAST_DUP));
				}
			}
		} else {
			merge = false;
		}

		var dataBuffer = dataVal.mv_data();
		if (merge) {
			if (existingBuffer != null && MemoryUtil.memAddress(existingBuffer) != MemoryUtil.memAddress(dataBuffer) ||
					existingBuffer == null && !newTuples.isEmpty()) {
				flush(cursor, elements, keyVal, dataVal);

				// position cursor at the first duplicate value for this key that is <= newValueBuf.
				dataVal.mv_data(newValueBuf);
				rc = E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_GET_BOTH_RANGE));
				if (rc == MDB_SUCCESS) {
					// The new value is smaller than the first duplicate value >= newValueBuf. Step back to the previous
					// duplicate value.
					if (E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_PREV_DUP)) != MDB_SUCCESS) {
						// ignore
					}
				} else {
					keyVal.mv_data(keyBuffer);
					if (E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_SET)) == MDB_SUCCESS) {
						E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_LAST_DUP));
					}
				}
				dataBuffer = dataVal.mv_data();
			}
		} else {
			if (targetKey.position() > 0) {
				if (compareRegion(targetKey, 0, keyBuffer, 0, Math.min(targetKey.limit(), keyBuffer.limit())) != 0) {
					flush(cursor, elements, keyVal, dataVal);
				}
			}
		}

		if (targetKey.position() == 0) {
			targetKey.put(keyBuffer);
		}

		if (merge && existingBuffer == null) {
			// We are positioned at the first duplicate value < newValueBuf.
			// Find the correct insertion point for newValueBuf in the selected chunk, and check if it already
			// exists.
			var tupleInput = new VarintTupleIO(elements, dataBuffer);
			int diff = tupleInput.seek(newValueBuf);
			if (diff == 0) {
				return MDB_KEYEXIST;
			}

			existingBuffer = dataBuffer;
			existingBuffer.rewind();
			existingBufferCache.clear().put(existingBuffer);
			existingBufferCache.flip();
			chunkInput = tupleInput;
		} else if (chunkInput != null) {
			if (!sortedInsertion) {
				existingBuffer.rewind();
				chunkInput.setBuffer(existingBuffer);
			}
			while (chunkInput.hasNext()) {
				int diff = chunkInput.compareTuple(newValueBuf);
				if (diff == 0) {
					return MDB_KEYEXIST;
				} else if (diff > 0) {
					break;
				}
				chunkInput.skipTuple();
			}
		}

		int pos = tupleBuffer.position();
		int length = newValueBuf.remaining();
		tupleBuffer.put(newValueBuf);
		if (!newTuples.add(new Tuple(pos, length))) {
			return MDB_KEYEXIST;
		}
		return MDB_SUCCESS;
	}

	/**
	 * Writes the currently encoded target buffer as soon as it grows beyond the configured chunk size.
	 *
	 * @param cursor  the LMDB cursor positioned on the target database
	 * @param keyVal  the LMDB key wrapper
	 * @param dataVal the LMDB value wrapper
	 * @throws IOException if tuple encoding fails
	 */
	private void flushTargetBuffer(long cursor, MDBVal keyVal, MDBVal dataVal) throws IOException {
		if (targetBuffer.position() > Chunks.MAX_CHUNK_SIZE) {
			keyVal.mv_data(targetKey);
			dataVal.mv_data(targetBuffer.flip());
			E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
			targetBuffer.clear();
			encoder.reset();
		}
	}

	/**
	 * Flushes all buffered tuples for the current key to LMDB.
	 * <p>
	 * If an existing duplicate chunk is being merged, that chunk is removed and replaced by one or more newly encoded
	 * chunks containing both the existing and pending tuples.
	 *
	 * @param cursor   the LMDB cursor positioned on the target database
	 * @param elements the number of tuple elements in the encoded chunk
	 * @param keyVal   the LMDB key wrapper
	 * @param dataVal  the LMDB value wrapper
	 * @throws IOException if tuple encoding fails while writing the updated chunks
	 */
	public void flush(long cursor, int elements, MDBVal keyVal, MDBVal dataVal) throws IOException {
		if (newTuples.isEmpty()) {
			return;
		}

		targetKey.flip();
		keyVal.mv_data(targetKey);

		boolean hasExisting = existingBuffer != null;
		if (hasExisting) {
			existingBuffer.rewind();
			chunkInput.setBuffer(existingBufferCache);

			int rc = E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_SET));
			if (rc == MDB_SUCCESS) {
				existingBuffer.rewind();
				dataVal.mv_data(existingBuffer);
				E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_GET_BOTH));
			}
			if (rc == MDB_SUCCESS) {
				// System.out.println("delete key " + Varint.readUnsigned(targetKey, 0) + " value " +
				// valueToString(existingBuffer));
				// Replace the selected duplicate value with one or two newly encoded duplicate values.
				E(mdb_cursor_del(cursor, 0));
			} else {
				System.out.println("not found " + Varint.readUnsigned(targetKey, 0));
			}
		}
		keyVal.mv_data(targetKey);
		targetBuffer.clear();

		encoder = chunkInput == null ? new VarintTupleIO(elements).createEncoder(targetBuffer)
				: chunkInput.createEncoder(targetBuffer);
		flushTargetBuffer(cursor, keyVal, dataVal);
		for (var tuple : newTuples) {
			tupleBuffer.limit(tuple.offset + tuple.length);
			tupleBuffer.position(tuple.offset);
			// System.out.println("storing: " + valueToString(tupleBuffer));
			if (hasExisting) {
				while (chunkInput.hasNext()) {
					int diff = chunkInput.compareTuple(tupleBuffer);
					if (diff < 0) {
						encoder.appendNextTuple(chunkInput);
						flushTargetBuffer(cursor, keyVal, dataVal);
					} else if (diff > 0) {
						break;
					}
				}
			}

			encoder.append(tupleBuffer);
			flushTargetBuffer(cursor, keyVal, dataVal);
		}

		if (hasExisting) {
			while (chunkInput.hasNext()) {
				encoder.appendNextTuple(chunkInput);
				flushTargetBuffer(cursor, keyVal, dataVal);
			}
		}

		if (targetBuffer.position() > 0) {
			dataVal.mv_data(targetBuffer.flip());
			E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
			targetBuffer.clear();
			encoder.reset();
		}

		reset();
	}

	/**
	 * Clears all buffered state so the updater can be reused for the next key.
	 */
	public void reset() {
		targetKey.clear();
		existingBuffer = null;
		existingBufferCache.clear();
		newTuples.clear();
		tupleBuffer.clear();
		chunkInput = null;
		encoder = null;
	}

	/**
	 * Renders the tuples in the supplied buffer as a simple space-separated debug string.
	 *
	 * @param buffer the buffer containing encoded triples
	 * @return a textual representation of the encoded tuples
	 */
	String valueToString(ByteBuffer buffer) {
		var values = new VarintTupleIO(3, buffer.duplicate());
		var sb = new StringBuilder();
		while (values.hasNext()) {
			for (int i = 0; i < 3; i++) {
				values.next();
				sb.append(values.readUnsigned()).append(" ");
			}
			values.nextTuple();
		}
		return sb.toString();
	}
}
