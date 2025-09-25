/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class KeyGeneratorTest {

	private static final long[] SAMPLE_KEY = { 1L, 2L, 3L, 4L };

	@Test
	@Disabled
	void hotKeyUsesCacheInsteadOfSupplier() {
		TestKeyWriter writer = new TestKeyWriter();
		KeyGenerator generator = new KeyGenerator(writer);
		CountingSupplier supplier = new CountingSupplier(false);

		ByteBuffer last = null;
		for (int i = 0; i < KeyGenerator.CACHE_THRESHOLD; i++) {
			last = generator.keyFor(SAMPLE_KEY[0], SAMPLE_KEY[1], SAMPLE_KEY[2], SAMPLE_KEY[3], supplier, false);
		}
		assertEquals(KeyGenerator.CACHE_THRESHOLD, supplier.invocations,
				"Supplier should be used while key warms up");
		assertFalse(last.isReadOnly(), "Hot path buffer should remain writable");

		ByteBuffer cached = generator.keyFor(SAMPLE_KEY[0], SAMPLE_KEY[1], SAMPLE_KEY[2], SAMPLE_KEY[3], supplier,
				false);
		assertEquals(KeyGenerator.CACHE_THRESHOLD, supplier.invocations, "Cached path must not invoke supplier");
		assertTrue(cached.isReadOnly(), "Cached buffer should be read-only");
		assertArrayEquals(bufferBytes(last), bufferBytes(cached), "Cached result must match original encoding");
	}

	@Test
	void cacheEvictsLeastRecentlyUsedEntries() {
		TestKeyWriter writer = new TestKeyWriter();
		KeyGenerator generator = new KeyGenerator(writer);
		CountingSupplier supplier = new CountingSupplier(false);

		for (int i = 0; i < KeyGenerator.CACHE_THRESHOLD; i++) {
			generator.keyFor(SAMPLE_KEY[0], SAMPLE_KEY[1], SAMPLE_KEY[2], SAMPLE_KEY[3], supplier, false);
		}
		assertEquals(KeyGenerator.CACHE_THRESHOLD, supplier.invocations,
				"Supplier should be used while key warms up");

		int cachePressure = (1 << 16) + 10;
		long[] nextSeed = { 10_000L };
		boolean evicted = false;

		for (int attempt = 0; attempt < 6 && !evicted; attempt++) {
			for (int i = 0; i < cachePressure; i++) {
				long seed = nextSeed[0]++;
				for (int j = 0; j < KeyGenerator.CACHE_THRESHOLD; j++) {
					generator.keyFor(seed, seed + 1, seed + 2, seed + 3, supplier, false);
				}
			}

			int beforeLookup = supplier.invocations;
			generator.keyFor(SAMPLE_KEY[0], SAMPLE_KEY[1], SAMPLE_KEY[2], SAMPLE_KEY[3], supplier, false);
			evicted = supplier.invocations > beforeLookup;
		}

		assertTrue(evicted, "Evicted key should eventually require supplier again");
	}

	@Test
	void cacheReplacesEntryOnFilterCollision() {
		TestKeyWriter writer = new TestKeyWriter();
		KeyGenerator generator = new KeyGenerator(writer);
		CountingSupplier supplier = new CountingSupplier(false);

		long collisionOffset = 1L << 16;
		long subjA = SAMPLE_KEY[0];
		long predA = SAMPLE_KEY[1];
		long objA = SAMPLE_KEY[2];
		long ctxA = SAMPLE_KEY[3];
		long subjB = subjA + collisionOffset;
		long predB = predA;
		long objB = objA;
		long ctxB = ctxA;

		for (int i = 0; i < KeyGenerator.CACHE_THRESHOLD; i++) {
			generator.keyFor(subjA, predA, objA, ctxA, supplier, false);
		}
		int afterFirstWarmup = supplier.invocations;

		for (int i = 0; i < KeyGenerator.CACHE_THRESHOLD; i++) {
			generator.keyFor(subjB, predB, objB, ctxB, supplier, false);
		}

		int beforeRetry = supplier.invocations;
		generator.keyFor(subjA, predA, objA, ctxA, supplier, false);
		assertTrue(supplier.invocations > beforeRetry,
				"Collision should evict previous entry stored in same slot");
		assertEquals(afterFirstWarmup + KeyGenerator.CACHE_THRESHOLD + 1, supplier.invocations,
				"Second key warmup plus eviction retry should account for supplier calls");
	}

	private static byte[] bufferBytes(ByteBuffer buffer) {
		ByteBuffer duplicate = buffer.duplicate();
		byte[] bytes = new byte[duplicate.remaining()];
		duplicate.get(bytes);
		return bytes;
	}

	private static final class CountingSupplier implements Supplier<ByteBuffer> {

		private final boolean pooled;
		private int invocations;
		private ByteBuffer lastBuffer;

		CountingSupplier(boolean pooled) {
			this.pooled = pooled;
		}

		@Override
		public ByteBuffer get() {
			invocations++;
			lastBuffer = ByteBuffer.allocate(128);
			return lastBuffer;
		}
	}

	private static final class TestKeyWriter implements IndexKeyWriters.KeyWriter {
		@Override
		public void write(ByteBuffer bb, long subj, long pred, long obj, long context) {
			Varint.writeUnsigned(bb, subj);
			Varint.writeUnsigned(bb, pred);
			Varint.writeUnsigned(bb, obj);
			Varint.writeUnsigned(bb, context);
		}
	}
}
