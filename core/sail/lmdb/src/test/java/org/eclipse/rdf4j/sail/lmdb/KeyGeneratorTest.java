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

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

class KeyGeneratorTest {

	private static final long[] SAMPLE_KEY = { 1L, 2L, 3L, 4L };

	@Test
	void hotKeyUsesCacheInsteadOfSupplier() {
		TestKeyWriter writer = new TestKeyWriter();
		KeyGenerator generator = new KeyGenerator(writer);
		CountingSupplier supplier = new CountingSupplier(false);

		ByteBuffer last = null;
		for (int i = 0; i < 6; i++) {
			last = generator.keyFor(SAMPLE_KEY[0], SAMPLE_KEY[1], SAMPLE_KEY[2], SAMPLE_KEY[3], supplier).buffer();
		}
		assertEquals(6, supplier.invocations, "Supplier should be used while key warms up");

		ByteBuffer cached = generator.keyFor(SAMPLE_KEY[0], SAMPLE_KEY[1], SAMPLE_KEY[2], SAMPLE_KEY[3], supplier)
				.buffer();
		assertEquals(6, supplier.invocations, "Cached path must not invoke supplier");
		assertArrayEquals(bufferBytes(last), bufferBytes(cached), "Cached result must match original encoding");
	}

	@Test
	void cachedBufferIndepedentOfPooledSource() {
		TestKeyWriter writer = new TestKeyWriter();
		KeyGenerator generator = new KeyGenerator(writer);
		CountingSupplier supplier = new CountingSupplier(true);

		ByteBuffer original = null;
		for (int i = 0; i < 6; i++) {
			original = generator.keyFor(SAMPLE_KEY[0], SAMPLE_KEY[1], SAMPLE_KEY[2], SAMPLE_KEY[3], supplier).buffer();
		}

		byte[] expected = bufferBytes(original);

		// mutate the pooled buffer that was most recently handed out to simulate pool reuse
		ByteBuffer pooled = supplier.lastBuffer;
		pooled.clear();
		pooled.put((byte) 0x7F);
		pooled.flip();

		ByteBuffer cached = generator.keyFor(SAMPLE_KEY[0], SAMPLE_KEY[1], SAMPLE_KEY[2], SAMPLE_KEY[3], supplier)
				.buffer();
		assertEquals(6, supplier.invocations, "Cache hit should avoid supplier");
		assertArrayEquals(expected, bufferBytes(cached),
				"Cached buffer must be independent of pooled buffer mutations");
	}

	private static byte[] bufferBytes(ByteBuffer buffer) {
		ByteBuffer duplicate = buffer.duplicate();
		byte[] bytes = new byte[duplicate.remaining()];
		duplicate.get(bytes);
		return bytes;
	}

	private static final class CountingSupplier implements KeyGenerator.BufferSupplier {

		private final boolean pooled;
		private int invocations;
		private ByteBuffer lastBuffer;

		CountingSupplier(boolean pooled) {
			this.pooled = pooled;
		}

		@Override
		public KeyGenerator.KeyBuffer acquire() {
			invocations++;
			lastBuffer = ByteBuffer.allocate(128);
			return new KeyGenerator.KeyBuffer(lastBuffer, pooled);
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
