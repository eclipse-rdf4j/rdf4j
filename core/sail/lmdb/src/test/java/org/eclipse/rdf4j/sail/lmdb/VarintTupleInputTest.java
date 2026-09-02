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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.rdf4j.sail.lmdb.util.VarintTupleInput;
import org.junit.jupiter.api.Test;

class VarintTupleInputTest {

	private static final int ELEMENTS = 3;

	@Test
	void nextDecodesTuplesWithReuseMarkers() {
		ByteBuffer buffer = buildBuffer();
		VarintTupleInput input = new VarintTupleInput(ELEMENTS, buffer);

		long[] expected = {
				10L, 3_000L, 1_000_000L,
				10L, 42L, 1_000_000L,
				11L, 42L, 2L
		};

		for (long value : expected) {
			assertTrue(input.next());
			assertEquals(value, Varint.readUnsigned(buffer));
		}
		assertFalse(input.next());
	}

	@Test
	void skipSkipsBothDirectAndReusedValues() {
		ByteBuffer buffer = buildBuffer();
		VarintTupleInput input = new VarintTupleInput(ELEMENTS, buffer);

		assertTrue(input.skip());
		assertTrue(input.next());
		assertEquals(3_000L, Varint.readUnsigned(buffer));

		assertTrue(input.skip());
		assertTrue(input.next());
		assertEquals(10L, Varint.readUnsigned(buffer));

		assertTrue(input.skip());
		assertTrue(input.next());
		assertEquals(1_000_000L, Varint.readUnsigned(buffer));

		assertTrue(input.skip());
		assertTrue(input.next());
		assertEquals(42L, Varint.readUnsigned(buffer));

		assertFalse(input.skip());
	}

	@Test
	void nextReturnsFalseAfterFinalReusedElement() {
		ByteBuffer buffer = ByteBuffer.allocate(64).order(ByteOrder.nativeOrder());
		Varint.writeUnsigned(buffer, 5L);
		Varint.writeUnsigned(buffer, 6L);
		Varint.writeUnsigned(buffer, 7L);
		Varint.writeUnsigned(buffer, 8L);
		Varint.writeUnsigned(buffer, 9L);
		buffer.put((byte) 0); // reuse element index 2 from the previous tuple
		buffer.flip();

		VarintTupleInput input = new VarintTupleInput(ELEMENTS, buffer);
		long[] expected = { 5L, 6L, 7L, 8L, 9L, 7L };

		for (long value : expected) {
			assertTrue(input.next());
			assertEquals(value, Varint.readUnsigned(buffer));
		}
		assertFalse(input.next());
	}

	private static ByteBuffer buildBuffer() {
		ByteBuffer buffer = ByteBuffer.allocate(64).order(ByteOrder.nativeOrder());

		// Tuple 1: all values are directly encoded.
		Varint.writeUnsigned(buffer, 10L);
		Varint.writeUnsigned(buffer, 3_000L);
		Varint.writeUnsigned(buffer, 1_000_000L);

		// Tuple 2: element 0 and 2 reuse tuple 1 positions.
		buffer.put((byte) 0);
		Varint.writeUnsigned(buffer, 42L);
		buffer.put((byte) 0);

		// Tuple 3: element 1 reuses tuple 2 position.
		Varint.writeUnsigned(buffer, 11L);
		buffer.put((byte) 0);
		Varint.writeUnsigned(buffer, 2L);

		buffer.flip();
		return buffer;
	}
}
