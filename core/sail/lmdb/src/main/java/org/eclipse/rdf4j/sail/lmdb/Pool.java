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

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * A simple pool for {@link MDBVal}, {@link ByteBuffer} and {@link Statistics} instances.
 */
class Pool {
	// thread-local pool instance
	private static final ThreadLocal<Pool> threadlocal = ThreadLocal.withInitial(Pool::new);

	private final MDBVal[] valPool = new MDBVal[2048];
	private final ByteBuffer[] keyPool = new ByteBuffer[2048];
	private final Statistics[] statisticsPool = new Statistics[512];
	private final LmdbRecordIterator.State[] statePool = new LmdbRecordIterator.State[128];
	private int valPoolIndex = -1;
	private int keyPoolIndex = -1;
	private int statisticsPoolIndex = -1;
	private int statePoolIndex = -1;

	final MDBVal getVal() {
		if (valPoolIndex >= 0) {
			return valPool[valPoolIndex--];
		}
		return MDBVal.malloc();
	}

	final LmdbRecordIterator.State getState() {
		if (statePoolIndex >= 0) {
			return statePool[statePoolIndex--];
		}
		return new LmdbRecordIterator.State();
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

	final void free(LmdbRecordIterator.State state) {
		if (statePoolIndex < statePool.length - 1) {
			statePool[++statePoolIndex] = state;
		} else {
			state.close();
		}
	}

	final void close() {
		while (statePoolIndex >= 0) {
			statePool[statePoolIndex--].close();
		}
		while (valPoolIndex >= 0) {
			valPool[valPoolIndex--].close();
		}
		while (keyPoolIndex >= 0) {
			MemoryUtil.memFree(keyPool[keyPoolIndex--]);
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
