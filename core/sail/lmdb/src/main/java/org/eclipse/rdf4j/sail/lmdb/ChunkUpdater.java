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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.rdf4j.sail.lmdb.util.VarintTupleIO;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.lmdb.MDBEnvInfo;
import org.lwjgl.util.lmdb.MDBVal;

public final class ChunkUpdater {
	final ByteBuffer targetKey;
	ByteBuffer existingBuffer;
	static final int maxChunkSize = 511 - TripleIndex.MAX_KEY_LENGTH;

	ByteBuffer targetBuffer, tupleBuffer;

	boolean sortedInsertion = false;

	record Tuple (int offset, int length) {}

	final TreeSet<Tuple> newTuples = new TreeSet<>((a, b) -> compareRegion(tupleBuffer, a.offset, tupleBuffer, b.offset, Math.min(a.length, b.length)));

	VarintTupleIO chunkInput;

	ChunkUpdater(MemoryStack stack) {
		this.targetKey = stack.malloc(TripleIndex.MAX_KEY_LENGTH);
		this.targetBuffer = stack.malloc(512);
		this.tupleBuffer = stack.malloc(4096);
	}

	int add(long cursor, int elements, MDBVal keyVal, MDBVal dataVal, ByteBuffer newValueBuf)
			throws IOException {
		if (tupleBuffer.position() + newValueBuf.remaining() > tupleBuffer.capacity()) {
			flush(cursor, elements, keyVal, dataVal);
		}

		final var keyBuffer = keyVal.mv_data();

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
			if (targetKey.position() == 0) {
				targetKey.put(keyBuffer);
			}
		} else {
			if (targetKey.position() > 0) {
				if (compareRegion(targetKey, 0, keyBuffer, 0, Math.min(targetKey.limit(), keyBuffer.limit())) != 0) {
					flush(cursor, elements, keyVal, dataVal);
					targetKey.put(keyBuffer);
				}
			} else {
				targetKey.put(keyBuffer);
			}
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
			chunkInput = tupleInput;
		} else if (chunkInput != null) {
			if (! sortedInsertion) {
				existingBuffer.rewind();
				chunkInput.setBuffer(existingBuffer);
			} else {
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
		}

		int pos = tupleBuffer.position();
		int length = newValueBuf.remaining();
		tupleBuffer.put(newValueBuf);
		if (!newTuples.add(new Tuple(pos, length))) {
			return MDB_KEYEXIST;
		}
		return MDB_SUCCESS;
	}

	public void flush(long cursor, int elements, MDBVal keyVal, MDBVal dataVal) throws IOException {
		if (newTuples.isEmpty()) {
			return;
		}

		targetKey.flip();
		boolean hasExisting = existingBuffer != null;
		if (hasExisting) {
			existingBuffer.rewind();
			chunkInput.setBuffer(existingBuffer);
		}
		targetBuffer.clear();

		VarintTupleIO.Encoder encoder = chunkInput == null ? new VarintTupleIO(elements).createEncoder(targetBuffer) :
			chunkInput.createEncoder(targetBuffer);
		for (var tuple : newTuples) {
			tupleBuffer.limit(tuple.offset + tuple.length);
			tupleBuffer.position(tuple.offset);
			System.out.println("storing: " + valueToString(tupleBuffer));
			if (hasExisting) {
				while (chunkInput.hasNext()) {
					int diff = chunkInput.compareTuple(tupleBuffer);
					if (diff < 0) {
						encoder.appendNextTuple(chunkInput);
						if (targetBuffer.position() > maxChunkSize) {
							dataVal.mv_data(targetBuffer.flip());
							E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
							targetBuffer.clear();
							encoder.reset();
						}
					} else if (diff > 0) {
						break;
					}
				}
			}
			encoder.append(tupleBuffer);
			if (targetBuffer.position() > maxChunkSize) {
				dataVal.mv_data(targetBuffer.flip());
				E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
				targetBuffer.clear();
				encoder.reset();
			}
		}

		if (hasExisting) {
			while (chunkInput.hasNext()) {
				encoder.appendNextTuple(chunkInput);
				if (targetBuffer.position() > maxChunkSize) {
					dataVal.mv_data(targetBuffer.flip());
					E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
					targetBuffer.clear();
					encoder.reset();
				}
			}
			if (targetBuffer.position() > 0) {
				dataVal.mv_data(targetBuffer.flip());
				E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
				targetBuffer.clear();
				encoder.reset();
			}

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
			chunkInput.setBuffer(existingBuffer);
		}

		reset();
	}

	public void reset() {
		targetKey.clear();
		existingBuffer = null;
		newTuples.clear();
		tupleBuffer.clear();
		chunkInput = null;
	}

	String valueToString(ByteBuffer buffer) {
		var values = new VarintTupleIO(3, buffer.duplicate());
		var sb = new StringBuilder();
		while (values.hasNext()) {
			values.next();
			sb.append(values.readUnsigned()).append(" ");
		}
		return sb.toString();
	}
}
