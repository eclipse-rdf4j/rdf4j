/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * A simple pool for {@link MDBVal}, {@link ByteBuffer} and {@link Statistics} instances.
 */
class Pool {
	// thread-local pool instance
	private static final ThreadLocal<Pool> threadlocal = ThreadLocal.withInitial(Pool::new);

	private final MDBVal[] valPool = new MDBVal[1024];
	private final ByteBuffer[] keyPool = new ByteBuffer[1024];
	private final Statistics[] statisticsPool = new Statistics[512];
	// LMDB cursor pooling (per-thread). We store cursor pointers along with their DBI.
	private final long[] cursorPool = new long[1024];
	private final int[] cursorDbiPool = new int[1024];
	private final Object[] cursorOwnerPool = new Object[1024];
	private int valPoolIndex = -1;
	private int keyPoolIndex = -1;
	private int statisticsPoolIndex = -1;
	private int cursorPoolIndex = -1;

	final MDBVal getVal() {
		if (valPoolIndex >= 0) {
			return valPool[valPoolIndex--];
		}
		return MDBVal.malloc();
	}

	final ByteBuffer getKeyBuffer() {
		if (keyPoolIndex >= 0) {
			ByteBuffer bb = keyPool[keyPoolIndex--];
			bb.clear();
			return bb;
		}
		return MemoryUtil.memAlloc(TripleStore.MAX_KEY_LENGTH);
	}

	final Statistics getStatistics() {
		if (statisticsPoolIndex >= 0) {
			return statisticsPool[statisticsPoolIndex--];
		}
		return new Statistics();
	}

	final void free(MDBVal val) {
		if (valPoolIndex < valPool.length - 1) {
			valPool[++valPoolIndex] = val;
		} else {
			val.close();
		}
	}

	final void free(ByteBuffer bb) {
		if (keyPoolIndex < keyPool.length - 1) {
			keyPool[++keyPoolIndex] = bb;
		} else {
			MemoryUtil.memFree(bb);
		}
	}

	final void free(Statistics statistics) {
		if (statisticsPoolIndex < statisticsPool.length - 1) {
			statisticsPool[++statisticsPoolIndex] = statistics;
		}
	}

	/**
	 * Try to obtain a pooled LMDB cursor for the given {@code dbi}. Returns {@code 0} if none is available.
	 */
	final long getCursor(int dbi, Object owner) {
		for (int i = cursorPoolIndex; i >= 0; i--) {
			if (cursorDbiPool[i] == dbi && cursorOwnerPool[i] == owner) {
				long cursor = cursorPool[i];
				// compact the stack by moving the top entry to this slot
				if (i != cursorPoolIndex) {
					cursorPool[i] = cursorPool[cursorPoolIndex];
					cursorDbiPool[i] = cursorDbiPool[cursorPoolIndex];
					cursorOwnerPool[i] = cursorOwnerPool[cursorPoolIndex];
				}
				cursorPoolIndex--;
				return cursor;
			}
		}
		return 0L;
	}

	/**
	 * Return an LMDB cursor to the pool for potential reuse. If the pool is full, the cursor is closed.
	 */
	final void freeCursor(int dbi, Object owner, long cursor) {
		if (cursorPoolIndex < cursorPool.length - 1) {
			cursorPool[++cursorPoolIndex] = cursor;
			cursorDbiPool[cursorPoolIndex] = dbi;
			cursorOwnerPool[cursorPoolIndex] = owner;
		} else {
			mdb_cursor_close(cursor);
		}
	}

	final void close() {
		while (valPoolIndex >= 0) {
			valPool[valPoolIndex--].close();
		}
		while (keyPoolIndex >= 0) {
			MemoryUtil.memFree(keyPool[keyPoolIndex--]);
		}
		while (cursorPoolIndex >= 0) {
			mdb_cursor_close(cursorPool[cursorPoolIndex--]);
		}
	}

	/**
	 * Get a pool instance for the current thread.
	 *
	 * @return a Pool instance
	 */
	public static Pool get() {
		return threadlocal.get();
	}

	/**
	 * Release the pool instance for the current thread.
	 */
	public static void release() {
		Pool pool = threadlocal.get();
		pool.close();
		threadlocal.remove();
	}
}
