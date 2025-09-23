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

import org.junit.jupiter.api.Test;

class KeyGeneratorTest {

	private static final long[] SAMPLE_KEY = { 1L, 2L, 3L, 4L };

	@Test
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
	void cachedBufferIndepedentOfPooledSource() {
		TestKeyWriter writer = new TestKeyWriter();
		KeyGenerator generator = new KeyGenerator(writer);
		CountingSupplier supplier = new CountingSupplier(true);

		ByteBuffer original = null;
		for (int i = 0; i < KeyGenerator.CACHE_THRESHOLD; i++) {
			original = generator.keyFor(SAMPLE_KEY[0], SAMPLE_KEY[1], SAMPLE_KEY[2], SAMPLE_KEY[3], supplier, true);
		}

		byte[] expected = bufferBytes(original);

		// mutate the pooled buffer that was most recently handed out to simulate pool reuse
		ByteBuffer pooled = supplier.lastBuffer;
		pooled.clear();
		pooled.put((byte) 0x7F);
		pooled.flip();

		ByteBuffer cached = generator.keyFor(SAMPLE_KEY[0], SAMPLE_KEY[1], SAMPLE_KEY[2], SAMPLE_KEY[3], supplier,
				true);
		assertEquals(KeyGenerator.CACHE_THRESHOLD, supplier.invocations, "Cache hit should avoid supplier");
		assertTrue(cached.isReadOnly(), "Cached buffer should be read-only");
		assertArrayEquals(expected, bufferBytes(cached),
				"Cached buffer must be independent of pooled buffer mutations");
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

		int cachePressure = KeyGenerator.CACHE_MAX_ENTRIES + 10;
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
