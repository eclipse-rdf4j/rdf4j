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

import java.nio.ByteBuffer;

import org.eclipse.rdf4j.sail.lmdb.util.MpmcRingBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * A simple pool for {@link MDBVal} and {@link ByteBuffer} instances.
 * <p>
 * Passing an instance to {@code free} transfers ownership to the pool; callers must not use it afterwards.
 */
final class Pool {

	private static final int POOL_SIZE = 2048;

	private final MpmcRingBuffer<MDBVal> valPool = new MpmcRingBuffer<>(POOL_SIZE);
	private final MpmcRingBuffer<ByteBuffer> keyPool = new MpmcRingBuffer<>(POOL_SIZE);

	private volatile boolean closed;

	MDBVal getVal() {
		MDBVal val = valPool.poll();
		return val != null ? val : MDBVal.malloc();
	}

	ByteBuffer getKeyBuffer() {
		ByteBuffer bb = keyPool.poll();
		if (bb != null) {
			return bb.clear();
		}
		return MemoryUtil.memAlloc(TripleIndex.MAX_KEY_LENGTH);
	}

	void free(MDBVal val) {
		if (closed || !valPool.offer(val)) {
			val.close();
		}
	}

	void free(ByteBuffer bb) {
		assert bb.isDirect() && bb.capacity() == TripleIndex.MAX_KEY_LENGTH
				: "buffer not allocated by this pool";
		if (closed || !keyPool.offer(bb)) {
			MemoryUtil.memFree(bb);
		}
	}

	void close() {
		closed = true;
		MDBVal val;
		while ((val = valPool.poll()) != null) {
			val.close();
		}
		ByteBuffer bb;
		while ((bb = keyPool.poll()) != null) {
			MemoryUtil.memFree(bb);
		}
	}
}
