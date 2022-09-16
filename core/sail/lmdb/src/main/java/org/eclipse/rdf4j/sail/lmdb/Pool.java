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

	private final MDBVal[] valPool = new MDBVal[1024];
	private final ByteBuffer[] keyPool = new ByteBuffer[1024];
	private final Statistics[] statisticsPool = new Statistics[512];
	private volatile int valPoolIndex = -1;
	private volatile int keyPoolIndex = -1;
	private volatile int statisticsPoolIndex = -1;

	MDBVal getVal() {
		synchronized (valPool) {
			if (valPoolIndex >= 0) {
				return valPool[valPoolIndex--];
			}
		}
		return MDBVal.malloc();
	}

	ByteBuffer getKeyBuffer() {
		synchronized (keyPool) {
			if (keyPoolIndex >= 0) {
				ByteBuffer bb = keyPool[keyPoolIndex--];
				bb.clear();
				return bb;
			}
		}
		return MemoryUtil.memAlloc(TripleStore.MAX_KEY_LENGTH);
	}

	Statistics getStatistics() {
		synchronized (statisticsPool) {
			if (statisticsPoolIndex >= 0) {
				return statisticsPool[statisticsPoolIndex--];
			}
		}
		return new Statistics();
	}

	void free(MDBVal val) {
		synchronized (valPool) {
			if (valPoolIndex < valPool.length - 1) {
				valPool[++valPoolIndex] = val;
			} else {
				val.close();
			}
		}
	}

	void free(ByteBuffer bb) {
		synchronized (keyPool) {
			if (keyPoolIndex < keyPool.length - 1) {
				keyPool[++keyPoolIndex] = bb;
			} else {
				MemoryUtil.memFree(bb);
			}
		}
	}

	void free(Statistics statistics) {
		synchronized (statisticsPool) {
			if (statisticsPoolIndex < statisticsPool.length - 1) {
				statisticsPool[++statisticsPoolIndex] = statistics;
			}
		}
	}

	void close() {
		synchronized (valPool) {
			while (valPoolIndex >= 0) {
				valPool[valPoolIndex--].close();
			}
		}
		synchronized (keyPool) {
			while (keyPoolIndex >= 0) {
				MemoryUtil.memFree(keyPool[keyPoolIndex--]);
			}
		}
	}
}
