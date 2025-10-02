/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

public class VarintTest {

	long[] values = new long[] {
			240, 2287, 67823, 16777215, 4294967295L, 1099511627775L, 281474976710655L, 72057594037927935L,
			72057594037927935L + 1
	};

	@Test
	public void testVarint() {
		ByteBuffer bb = ByteBuffer.allocate(9).order(ByteOrder.nativeOrder());
		for (int i = 0; i < values.length; i++) {
			bb.clear();
			Varint.writeUnsigned(bb, values[i]);
			bb.flip();
			assertEquals("Encoding should use " + (i + 1) + " bytes", i + 1, bb.remaining());
			assertEquals("Encoded and decoded value should be equal", values[i], Varint.readUnsigned(bb));
		}
	}

	@Test
	public void testVarint2() {
		ByteBuffer bb = ByteBuffer.allocate(9).order(ByteOrder.nativeOrder());
		bb.clear();
		Varint.writeUnsigned(bb, values[1]);
		bb.flip();
		assertEquals("Encoding should use " + (2) + " bytes", 2, bb.remaining());
		assertEquals("Encoded and decoded value should be equal", values[1], Varint.readUnsigned(bb));

	}

	@Test
	public void testVarint3() {
		ByteBuffer bb = ByteBuffer.allocate(9).order(ByteOrder.nativeOrder());
		bb.clear();
		Varint.writeUnsigned(bb, 67823);
		bb.flip();
		assertEquals("Encoded and decoded value should be equal", 67823, Varint.readUnsigned(bb));

	}

	@Test
	public void testVarint4() {
		ByteBuffer bb = ByteBuffer.allocate(9).order(ByteOrder.nativeOrder());
		bb.clear();
		Varint.writeUnsigned(bb, 67824);
		bb.flip();
		assertEquals("Encoded and decoded value should be equal", 67824, Varint.readUnsigned(bb));

	}

	@Test
	public void testVarint5() {
		ByteBuffer bb = ByteBuffer.allocate(9).order(ByteOrder.nativeOrder());
		bb.clear();
		Varint.writeUnsigned(bb, 4299999999L);
		bb.flip();
		assertEquals("Encoded and decoded value should be equal", 4299999999L, Varint.readUnsigned(bb));

	}

	@Test
	public void testVarintSequential() {
		for (long i = 0; i < 99999999; i++) {
			ByteBuffer bb = ByteBuffer.allocate(9).order(ByteOrder.nativeOrder());
			bb.clear();
			Varint.writeUnsigned(bb, i);
			bb.flip();
			try {
				assertEquals("Encoded and decoded value should be equal", i, Varint.readUnsigned(bb));
			} catch (Exception e) {
				System.err.println("Failed for i=" + i);
				throw e;
			}
		}

		for (long i = 99999999; i < 999999999999999L; i += 10000000) {
			try {
				ByteBuffer bb = ByteBuffer.allocate(9).order(ByteOrder.nativeOrder());
				bb.clear();
				Varint.writeUnsigned(bb, i);
				bb.flip();

				assertEquals("Encoded and decoded value should be equal", i, Varint.readUnsigned(bb));
			} catch (Exception e) {
				System.err.println("Failed for i=" + i);
				throw e;
			}
		}

		for (long i = Long.MAX_VALUE; i > Long.MAX_VALUE - 999999L; i -= 1) {
			ByteBuffer bb = ByteBuffer.allocate(9).order(ByteOrder.nativeOrder());
			bb.clear();
			Varint.writeUnsigned(bb, i);
			bb.flip();
			try {
				assertEquals("Encoded and decoded value should be equal", i, Varint.readUnsigned(bb));
			} catch (Exception e) {
				System.err.println("Failed for i=" + i);
				throw e;
			}
		}

	}

	@Test
	public void testVarintList() {
		ByteBuffer bb = ByteBuffer.allocate(2 + 4 * Long.BYTES).order(ByteOrder.nativeOrder());
		for (int i = 0; i < values.length - 4; i++) {
			long[] expected = new long[4];
			System.arraycopy(values, 0, expected, 0, 4);
			bb.clear();
			Varint.writeListUnsigned(bb, expected);
			bb.flip();
			long[] actual = new long[4];
			Varint.readListUnsigned(bb, actual);
			assertArrayEquals("Encoded and decoded value should be equal", expected, actual);
		}
	}

	@Test
	public void testVarintReadUnsignedAtPositionThreeByteEncoding() {
		long value = 3000L;
		ByteBuffer bb = ByteBuffer.allocate(Varint.calcLengthUnsigned(value))
				.order(ByteOrder.nativeOrder());
		Varint.writeUnsigned(bb, value);
		bb.flip();
		assertEquals("Expected three byte encoding", 3, bb.remaining());
		long decoded = Varint.readUnsigned(bb, 0);
		assertEquals("Encoded and decoded value using positional read should match", value, decoded);
	}
}
