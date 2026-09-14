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

import org.eclipse.rdf4j.sail.lmdb.util.VarintTupleIO;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.lmdb.MDBVal;

public final class ChunkUpdater {

	final ByteBuffer targetKey;
	ByteBuffer existingBuffer;
	VarintTupleIO existing;
	VarintTupleIO.Encoder encoder;
	final List<Integer> splitPositions = new ArrayList<>();
	int currentChunkSize = 0;
	static final int maxChunkSize = 511 - TripleIndex.MAX_KEY_LENGTH;

	ChunkUpdater(ByteBuffer targetKey) {
		this.targetKey = targetKey;
	}

	int add(long cursor, int elements, MDBVal keyVal, MDBVal dataVal, ByteBuffer newValueBuf, ByteBuffer target)
			throws IOException {
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
					// this
					// chunk.
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
		if (merge && dataBuffer.limit() < maxChunkSize) {
			if (existingBuffer != null && MemoryUtil.memAddress(existingBuffer) != MemoryUtil.memAddress(dataBuffer) ||
					existingBuffer == null && encoder != null) {
				flush(cursor, keyVal, dataVal, target);

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

			if (existingBuffer == null) {
				// We are positioned at the first duplicate value < newValueBuf.
				// Find the correct insertion point for newValueBuf in the selected chunk, and check if it already
				// exists.
				var tupleInput = new VarintTupleIO(elements, dataBuffer);

				int diff = tupleInput.seek(newValueBuf);
				if (diff == 0) {
					return MDB_KEYEXIST;
				}

				existingBuffer = tupleInput.getBuffer();
				existing = tupleInput;

				// Copy the already-consumed prefix of the selected chunk, then insert newValueBuf, then
				// continue copying tuples from the selected chunk until the first output chunk is full.
				target.clear();
				encoder = existing.createEncoder(target);
				currentChunkSize = target.position();
			} else {
				while (existing.hasNext()) {
					int diff = existing.compareTuple(newValueBuf);
					if (diff == 0) {
						return MDB_KEYEXIST;
					} else if (diff > 0) {
						break;
					}

					int pos = target.position();
					if (currentChunkSize >= maxChunkSize) {
						splitPositions.add(pos);
						encoder.resetDeltaEncoding();
						currentChunkSize = 0;
					}
					encoder.appendNextTuple(existing);
					currentChunkSize += target.position() - pos;
				}
			}
		} else {
			if (targetKey.position() > 0) {
				if (compareRegion(targetKey, 0, keyBuffer, 0, Math.min(targetKey.limit(), keyBuffer.limit())) != 0) {
					flush(cursor, keyVal, dataVal, target);
					targetKey.put(keyBuffer);
				}
			} else {
				targetKey.put(keyBuffer);
			}
			if (encoder == null) {
				target.clear();
				encoder = new VarintTupleIO(elements).createEncoder(target);
			}
		}

		int pos = target.position();
		if (currentChunkSize >= maxChunkSize) {
			splitPositions.add(pos);
			encoder.resetDeltaEncoding();
			currentChunkSize = 0;
		}
		encoder.append(newValueBuf);
		currentChunkSize += target.position() - pos;

		return MDB_SUCCESS;
	}

	public void flush(long cursor, MDBVal keyVal, MDBVal dataVal, ByteBuffer target) throws IOException {
		if (encoder == null) {
			return;
		}

		int lastPos = target.position();

		targetKey.flip();
		if (existingBuffer != null) {
			while (existing.hasNext()) {
				int pos = target.position();
				if (currentChunkSize >= maxChunkSize) {
					splitPositions.add(pos);
					encoder.resetDeltaEncoding();
					currentChunkSize = 0;
				}
				encoder.appendNextTuple(existing);
				currentChunkSize += target.position() - pos;
			}
			lastPos = target.position();

			keyVal.mv_data(targetKey);

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
				// System.out.println("not found " + Varint.readUnsigned(targetKey, 0));
			}
		}

		keyVal.mv_data(targetKey);
		target.position(0);
		for (var splitPos : splitPositions) {
			target.limit(splitPos);
			dataVal.mv_data(target);
			E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
			target.position(splitPos);
		}

		if (target.position() < lastPos) {
			target.limit(lastPos);
			dataVal.mv_data(target);
			E(mdb_cursor_put(cursor, keyVal, dataVal, 0));
		}

		// System.out.println(
		// "Wrote (split=" + splitPositions + ") for key " + Varint.readUnsigned(targetKey, 0) + ": " +
		// valueToString(target));
		reset();
	}

	public void reset() {
		targetKey.clear();
		existingBuffer = null;
		existing = null;
		encoder = null;
		splitPositions.clear();
		currentChunkSize = 0;
	}

	String valueToString(ByteBuffer buffer) {
		var values = new VarintTupleIO(3, buffer.duplicate().position(0));
		var sb = new StringBuilder();
		while (values.hasNext()) {
			values.next();
			sb.append(values.readUnsigned()).append(" ");
		}
		return sb.toString();
	}
}
