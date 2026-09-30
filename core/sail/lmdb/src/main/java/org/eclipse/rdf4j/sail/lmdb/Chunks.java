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
import static org.lwjgl.util.lmdb.LMDB.MDB_KEYEXIST;
import static org.lwjgl.util.lmdb.LMDB.MDB_LAST;
import static org.lwjgl.util.lmdb.LMDB.MDB_PREV;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_RANGE;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_del;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_put;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.eclipse.rdf4j.sail.lmdb.util.ChunkInput;
import org.eclipse.rdf4j.sail.lmdb.util.ChunkOutput;
import org.eclipse.rdf4j.sail.lmdb.util.MpmcRingBuffer;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * Utility methods for maintaining sorted LMDB duplicate-value chunks that contain varint-encoded tuples.
 */
public class Chunks {
	/**
	 * Maximum number of tuples to store in a single chunk. When a chunk exceeds this size, it is split into two chunks.
	 */
	public static final int MAX_CHUNK_SIZE = 32;

	static final MpmcRingBuffer<ChunkOutput> chunkPool = new MpmcRingBuffer<>(16);

	/**
	 * Inserts a tuple into the sorted duplicate-value chunks for the current key, rewriting the affected chunk when
	 * needed to preserve ordering and split oversized chunks.
	 *
	 * @param cursor       the LMDB cursor positioned on the duplicates for the key
	 * @param splitPoint   the index at which to split the tuple for chunking
	 * @param keyVal       the key buffer used for cursor operations
	 * @param dataVal      the data buffer used for cursor operations
	 * @param tuple        the encoded tuple to insert
	 * @param keyScratch   scratch buffer used to encode replacement chunks
	 * @param valueScratch scratch buffer used to encode replacement chunks
	 * @return the LMDB result code, or {@link org.lwjgl.util.lmdb.LMDB#MDB_KEYEXIST} when the tuple already exists
	 * @throws IOException if tuple decoding or encoding fails
	 */
	public static int mergeChunk(long cursor, int splitPoint, MDBVal keyVal, MDBVal dataVal,
			long[] tuple, ByteBuffer keyScratch, ByteBuffer valueScratch)
			throws IOException {
		keyScratch.clear();
		int keyPrefixLength;
		for (int i = 0; i < splitPoint; i++) {
			Varint.writeUnsigned(keyScratch, tuple[i]);
		}
		keyPrefixLength = keyScratch.position();
		for (int i = splitPoint; i < tuple.length; i++) {
			Varint.writeUnsigned(keyScratch, tuple[i]);
		}
		keyScratch.flip();

		keyVal.mv_data(keyScratch);

		// Position cursor at the anchor key.
		int rc = E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_SET));
		if (rc == MDB_SUCCESS) {
			return MDB_KEYEXIST;
		}

		boolean hasExistingChunk = false;
		ByteBuffer existingKey = null;
		rc = E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_SET_RANGE));
		if (rc == MDB_SUCCESS) {
			E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_PREV));
		} else {
			rc = E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_LAST));
		}
		if (rc == MDB_SUCCESS) {
			existingKey = keyVal.mv_data();
			if (compareRegion(keyScratch, 0, existingKey, 0, keyPrefixLength) == 0) {
				if (compareRegion(keyScratch, 0, existingKey, 0,
						Math.min(keyScratch.remaining(), existingKey.remaining())) > 0) {
					hasExistingChunk = true;
				}
			}
		}

		if (!hasExistingChunk) {
			keyVal.mv_data(keyScratch);
			valueScratch.clear().limit(0);
			dataVal.mv_data(valueScratch);
			E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
			return MDB_SUCCESS;
		}

		// We are positioned at the first duplicate value < newValueBuf.
		// Find the correct insertion point for newValueBuf in the selected chunk, and check if it already exists.
		var existing = new ChunkInput(existingKey, dataVal.mv_data(), 4, splitPoint);
		var chunk1 = getChunk(4, splitPoint);
		int diff = existing.seek(tuple, chunk1::addTuple);
		if (diff == 0) {
			chunkPool.offer(chunk1);
			return MDB_KEYEXIST;
		}

		ChunkOutput chunk2 = null;
		if (!chunk1.addTuple(tuple)) {
			// The new tuple does not fit in the first chunk; we will need to create a second chunk.
			chunk2 = getChunk(4, splitPoint);
			chunk2.addTuple(tuple);
		}

		long[] existingTuple;
		while ((existingTuple = existing.next()) != null) {
			if (chunk2 != null) {
				chunk2.addTuple(existingTuple);
			} else if (!chunk1.addTuple(existingTuple)) {
				chunk2 = getChunk(4, splitPoint);
				chunk2.addTuple(existingTuple);
			}
		}

		keyScratch.clear();
		valueScratch.clear();
		chunk1.write(keyScratch, valueScratch);
		keyVal.mv_data(keyScratch.flip());
		dataVal.mv_data(valueScratch.flip());
		E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
		chunkPool.offer(chunk1);

		if (chunk2 != null) {
			keyScratch.clear();
			valueScratch.clear();
			chunk2.write(keyScratch, valueScratch);
			keyVal.mv_data(keyScratch.flip());
			dataVal.mv_data(valueScratch.flip());
			E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
			chunkPool.offer(chunk2);
		}

		return MDB_SUCCESS;
	}

	static String chunkToString(ByteBuffer keyBuffer, ByteBuffer valueBuffer, int tupleLength, int splitPoint) {
		var input = new ChunkInput(keyBuffer.duplicate(), valueBuffer.duplicate(), tupleLength, splitPoint);
		StringBuilder sb = new StringBuilder();
		sb.append("Chunk{");
		long[] tuple;
		while ((tuple = input.next()) != null) {
			sb.append("[");
			for (int i = 0; i < tupleLength; i++) {
				if (i > 0) {
					sb.append(", ");
				}
				sb.append(tuple[i]);
			}
			sb.append("]");
		}
		sb.append("}");
		return sb.toString();
	}

	static ChunkOutput getChunk(int tupleLength, int splitPoint) {
		ChunkOutput chunk = chunkPool.poll();
		if (chunk == null) {
			chunk = new ChunkOutput(MAX_CHUNK_SIZE, tupleLength, splitPoint);
		} else {
			chunk.reset(tupleLength, splitPoint);
		}
		return chunk;
	}

	/**
	 * Removes a tuple from the sorted duplicate-value chunks for the current key.
	 *
	 * @param cursor       the LMDB cursor positioned on the duplicates for the key
	 * @param splitPoint   the split point used in chunk encoding
	 * @param keyVal       the key buffer used for cursor operations
	 * @param dataVal      the data buffer used for cursor operations
	 * @param tuple        the encoded tuple to remove
	 * @param matchPrefix  whether to match only the prefix of the tuple
	 * @param keyScratch   scratch buffer used to encode replacement chunks
	 * @param valueScratch scratch buffer used to encode replacement chunks
	 * @return {@code true} if the tuple was removed, otherwise {@code false}
	 * @throws IOException if tuple decoding or encoding fails
	 */
	public static boolean deleteFromChunk(long cursor, int splitPoint, MDBVal keyVal, MDBVal dataVal,
			long[] tuple, boolean matchPrefix, ByteBuffer keyScratch, ByteBuffer valueScratch)
			throws IOException {
		keyScratch.clear();
		int keyPrefixLength;
		if (matchPrefix) {
			// allows to use -1 as a wildcard, so we can delete all tuples that match a given prefix
			boolean prefixFinished = false;
			for (int i = 0; i < splitPoint; i++) {
				if (tuple[i] != -1) {
					Varint.writeUnsigned(keyScratch, tuple[i]);
				} else {
					prefixFinished = true;
					break;
				}
			}
			keyPrefixLength = keyScratch.position();
			if (!prefixFinished) {
				for (int i = splitPoint; i < tuple.length; i++) {
					if (tuple[i] == -1) {
						break;
					}
					Varint.writeUnsigned(keyScratch, tuple[i]);
				}
			}
		} else {
			for (int i = 0; i < splitPoint; i++) {
				Varint.writeUnsigned(keyScratch, tuple[i]);
			}
			keyPrefixLength = keyScratch.position();
			for (int i = splitPoint; i < tuple.length; i++) {
				Varint.writeUnsigned(keyScratch, tuple[i]);
			}
		}
		keyScratch.flip();

		keyVal.mv_data(keyScratch);

		ByteBuffer existingKey = null;
		ByteBuffer existingData = null;
		boolean isAnchorKey = false;
		// Position cursor at the anchor key. If the data value is empty, delete the key and return true.
		int rc;
		if (!matchPrefix) {
			rc = E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_SET));
			if (rc == MDB_SUCCESS) {
				existingKey = keyVal.mv_data();
				existingData = dataVal.mv_data();
				if (existingData.remaining() == 0) {
					E(mdb_cursor_del(cursor, 0));
					return true;
				}
				isAnchorKey = true;
			}
		}

		if (!isAnchorKey) {
			boolean hasExistingChunk = false;
			rc = E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_SET_RANGE));
			if (rc == MDB_SUCCESS) {
				E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_PREV));
			} else {
				rc = E(mdb_cursor_get(cursor, keyVal, dataVal, MDB_LAST));
			}
			if (rc == MDB_SUCCESS) {
				existingKey = keyVal.mv_data();
				if (compareRegion(keyScratch, 0, existingKey, 0, keyPrefixLength) == 0) {
					if (matchPrefix && compareRegion(keyScratch, 0, existingKey, 0,
							Math.min(keyScratch.remaining(), existingKey.remaining())) == 0) {
						hasExistingChunk = true;
					} else if (compareRegion(keyScratch, 0, existingKey, 0,
							Math.min(keyScratch.remaining(), existingKey.remaining())) > 0) {
						hasExistingChunk = true;
					}
				}
			}

			if (!hasExistingChunk) {
				return false;
			}
		}

		if (existingData == null) {
			existingData = dataVal.mv_data();
		}

		var existing = new ChunkInput(existingKey, existingData, 4, splitPoint);
		var chunk1 = getChunk(4, splitPoint);
		if (!isAnchorKey) {
			int diff = existing.seek(tuple, chunk1::addTuple);
			if (diff != 0) {
				if (!matchPrefix || diff < 0) {
					// The tuple to delete is not present in the chunk, and we are not matching by prefix, or the tuple
					// is less than the first tuple in the chunk.
					chunkPool.offer(chunk1);
					return false;
				}
			}
		}

		// skip's the tuple to be deleted
		existing.next();

		long[] existingTuple;
		while ((existingTuple = existing.next()) != null) {
			chunk1.addTuple(existingTuple);
		}
		if (isAnchorKey) {
			E(mdb_cursor_del(cursor, 0));
		}

		keyScratch.clear();
		valueScratch.clear();
		chunk1.write(keyScratch, valueScratch);
		keyVal.mv_data(keyScratch.flip());
		dataVal.mv_data(valueScratch.flip());
		E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
		chunkPool.offer(chunk1);
		return true;
	}

	public static int compareTuples(long[] a, long[] b) {
		for (int i = 0; i < a.length; i++) {
			int cmp = Long.compare(a[i], b[i]);
			if (cmp != 0) {
				return cmp;
			}
		}
		return 0;
	}

	public static boolean matches(int offset, long[] pattern, long[] tuple) {
		if (pattern.length != tuple.length) {
			return false;
		}
		for (int i = offset; i < pattern.length; i++) {
			if (pattern[i] != -1 && pattern[i] != tuple[i]) {
				return false;
			}
		}
		return true;
	}
}
