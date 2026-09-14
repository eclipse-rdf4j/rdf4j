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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.sail.lmdb.util.VarintTupleIO;
import org.junit.jupiter.api.Test;

class VarintTupleIOTest {

	private static final int ELEMENTS = 3;

	@Test
	void nextDecodesTuplesWithReuseMarkers() {
		ByteBuffer buffer = buildBuffer();
		VarintTupleIO input = new VarintTupleIO(ELEMENTS, buffer);

		long[] expected = {
				10L, 42L, 1_000_000L,
				10L, 3_000L, 1_000_000L,
				11L, 3_000L, 2L
		};

		for (long value : expected) {
			assertTrue(input.next());
			assertEquals(value, Varint.readUnsigned(buffer));
		}
		assertFalse(input.next());
	}

	@Test
	void skipSkipsMultipleValues() {
		ByteBuffer buffer = buildBuffer();
		VarintTupleIO input = new VarintTupleIO(ELEMENTS, buffer);

		assertTrue(input.skip());
		assertTrue(input.skip());
		assertTrue(input.skip());

		assertTrue(input.next());
		assertEquals(10L, Varint.readUnsigned(buffer));

		assertTrue(input.skip());
		assertTrue(input.skip());
		assertTrue(input.next());
		assertEquals(11L, Varint.readUnsigned(buffer));

		assertTrue(input.skip());
		assertFalse(input.skip());
	}

	@Test
	void skipSkipsBothDirectAndReusedValues() {
		ByteBuffer buffer = buildBuffer();
		VarintTupleIO input = new VarintTupleIO(ELEMENTS, buffer);

		assertTrue(input.skip());
		assertTrue(input.next());
		assertEquals(42L, Varint.readUnsigned(buffer));

		assertTrue(input.skip());
		assertTrue(input.next());
		assertEquals(10L, Varint.readUnsigned(buffer));

		assertTrue(input.skip());
		assertTrue(input.next());
		assertEquals(1_000_000L, Varint.readUnsigned(buffer));

		assertTrue(input.skip());
		assertTrue(input.next());
		assertEquals(3_000L, Varint.readUnsigned(buffer));

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

		VarintTupleIO input = new VarintTupleIO(ELEMENTS, buffer);
		long[] expected = { 5L, 6L, 7L, 8L, 9L, 7L };

		for (long value : expected) {
			assertTrue(input.next());
			assertEquals(value, Varint.readUnsigned(buffer));
		}
		assertFalse(input.next());
	}

	@Test
	void seekFindsExactTuple() {
		ByteBuffer buffer = buildBuffer();
		VarintTupleIO input = new VarintTupleIO(ELEMENTS, buffer);

		int diff = input.seek(tupleBuffer(10L, 3_000L, 1_000_000L));
		assertEquals(0, diff);

		assertTrue(input.next());
		assertEquals(10L, input.readUnsigned());
		assertTrue(input.next());
		assertEquals(3_000L, input.readUnsigned());
		assertTrue(input.next());
		assertEquals(1_000_000L, input.readUnsigned());
		input.nextTuple();

		assertTrue(input.next());
		assertEquals(11L, input.readUnsigned());
	}

	@Test
	void seekFindsNextGreaterTuple() {
		ByteBuffer buffer = buildBuffer();
		VarintTupleIO input = new VarintTupleIO(ELEMENTS, buffer);

		int diff = input.seek(tupleBuffer(10L, 52L, 1_000_000L));
		assertTrue(diff > 0);

		assertTrue(input.next());
		assertEquals(10L, input.readUnsigned());
		assertTrue(input.next());
		assertEquals(3_000L, input.readUnsigned());
		assertTrue(input.next());
		assertEquals(1_000_000L, input.readUnsigned());
		input.nextTuple();

		assertTrue(input.next());
		assertEquals(11L, input.readUnsigned());
	}

	@Test
	void seekSkipsAll() {
		ByteBuffer buffer = buildBuffer();
		VarintTupleIO input = new VarintTupleIO(ELEMENTS, buffer);

		int diff = input.seek(tupleBuffer(12L, 3_000L, 1_000_000L));
		;
		assertTrue(diff < 0);
		assertFalse(input.hasNext());
	}

	private static ByteBuffer buildBuffer() {
		ByteBuffer buffer = ByteBuffer.allocate(64).order(ByteOrder.nativeOrder());

		// Tuple 1: all values are directly encoded.
		Varint.writeUnsigned(buffer, 10L);
		Varint.writeUnsigned(buffer, 42L);
		Varint.writeUnsigned(buffer, 1_000_000L);

		// Tuple 2: element 0 and 2 reuse tuple 1 positions.
		buffer.put((byte) 0);
		Varint.writeUnsigned(buffer, 3_000L);
		buffer.put((byte) 0);

		// Tuple 3: element 1 reuses tuple 2 position.
		Varint.writeUnsigned(buffer, 11L);
		buffer.put((byte) 0);
		Varint.writeUnsigned(buffer, 2L);

		buffer.flip();
		return buffer;
	}

	@Test
	void encoderAppendRoundTripsAndEmitsReuseMarkers() {
		ByteBuffer out = ByteBuffer.allocate(128).order(ByteOrder.nativeOrder());
		VarintTupleIO encoderState = new VarintTupleIO(ELEMENTS, ByteBuffer.allocate(0).order(ByteOrder.nativeOrder()));
		VarintTupleIO.Encoder encoder = encoderState.createEncoder(out);

		encoder.append(tupleBuffer(1L, 300L, 70_000L));
		encoder.append(tupleBuffer(1L, 301L, 70_000L));
		encoder.append(tupleBuffer(2L, 301L, 5L));

		ByteBuffer encoded = out.duplicate();
		encoded.flip();
		assertEquals(3L, encodedBytes(encoded, (byte) 0));

		assertArrayEquals(
				new long[] { 1L, 300L, 70_000L, 1L, 301L, 70_000L, 2L, 301L, 5L },
				decodeAll(encoded, ELEMENTS));
	}

	@Test
	void encoderAppendNextTupleReadsAndAdvancesInputByTuple() {
		ByteBuffer source = buildBuffer();
		VarintTupleIO input = new VarintTupleIO(ELEMENTS, source);

		ByteBuffer out = ByteBuffer.allocate(128).order(ByteOrder.nativeOrder());
		VarintTupleIO.Encoder encoder = new VarintTupleIO(ELEMENTS,
				ByteBuffer.allocate(0).order(ByteOrder.nativeOrder())).createEncoder(out);

		encoder.appendNextTuple(input);
		encoder.appendNextTuple(input);
		encoder.appendNextTuple(input);
		assertFalse(input.hasNext());

		ByteBuffer encoded = out.duplicate();
		encoded.flip();
		assertArrayEquals(
				new long[] { 10L, 42L, 1_000_000L, 10L, 3_000L, 1_000_000L, 11L, 3_000L, 2L },
				decodeAll(encoded, ELEMENTS));
	}

	@Test
	void createEncoderCopiesConsumedPrefixBeforeEncodingContinues() {
		ByteBuffer source = buildBuffer();
		VarintTupleIO input = new VarintTupleIO(ELEMENTS, source);

		assertTrue(input.next());
		assertEquals(10L, input.readUnsigned());
		assertTrue(input.next());
		assertEquals(42L, input.readUnsigned());
		assertTrue(input.next());
		assertEquals(1_000_000L, input.readUnsigned());
		input.nextTuple();

		ByteBuffer out = ByteBuffer.allocate(128).order(ByteOrder.nativeOrder());
		VarintTupleIO.Encoder encoder = input.createEncoder(out);

		int copiedPrefixLength = out.position();
		assertTrue(copiedPrefixLength > 0);
		ByteBuffer sourcePrefix = source.duplicate();
		sourcePrefix.position(0).limit(copiedPrefixLength);
		ByteBuffer outPrefix = out.duplicate();
		outPrefix.flip().limit(copiedPrefixLength);
		assertArrayEquals(toByteArray(sourcePrefix), toByteArray(outPrefix));

		encoder.appendNextTuple(input);
		encoder.appendNextTuple(input);

		ByteBuffer encoded = out.duplicate();
		encoded.flip();
		assertArrayEquals(
				new long[] { 10L, 42L, 1_000_000L, 10L, 3_000L, 1_000_000L, 11L, 3_000L, 2L },
				decodeAll(encoded, ELEMENTS));
	}

	@Test
	void encoderAppendNextTupleThrowsForIncompleteTuple() {
		ByteBuffer truncated = ByteBuffer.allocate(16).order(ByteOrder.nativeOrder());
		Varint.writeUnsigned(truncated, 99L);
		Varint.writeUnsigned(truncated, 100L);
		truncated.flip();

		VarintTupleIO input = new VarintTupleIO(ELEMENTS, truncated);
		ByteBuffer out = ByteBuffer.allocate(16).order(ByteOrder.nativeOrder());
		VarintTupleIO.Encoder encoder = new VarintTupleIO(ELEMENTS,
				ByteBuffer.allocate(0).order(ByteOrder.nativeOrder())).createEncoder(out);

		NoSuchElementException error = assertThrows(NoSuchElementException.class, () -> encoder.appendNextTuple(input));
		assertEquals("No element at index 2", error.getMessage());
	}

	private static long[] decodeAll(ByteBuffer encoded, int elements) {
		VarintTupleIO input = new VarintTupleIO(elements, encoded.duplicate());
		List<Long> values = new ArrayList<>();
		while (input.next()) {
			values.add(input.readUnsigned());
		}
		long[] result = new long[values.size()];
		for (int i = 0; i < values.size(); i++) {
			result[i] = values.get(i);
		}
		return result;
	}

	private static ByteBuffer tupleBuffer(long... values) {
		ByteBuffer tuple = ByteBuffer.allocate(values.length * 10).order(ByteOrder.nativeOrder());
		for (long value : values) {
			Varint.writeUnsigned(tuple, value);
		}
		tuple.flip();
		return tuple;
	}

	private static long encodedBytes(ByteBuffer buffer, byte value) {
		long count = 0;
		ByteBuffer duplicate = buffer.duplicate();
		while (duplicate.hasRemaining()) {
			if (duplicate.get() == value) {
				count++;
			}
		}
		return count;
	}

	private static byte[] toByteArray(ByteBuffer buffer) {
		ByteBuffer duplicate = buffer.duplicate();
		byte[] bytes = new byte[duplicate.remaining()];
		duplicate.get(bytes);
		return bytes;
	}
}
