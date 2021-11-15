/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;

public class VarintTest {

	long[] values = new long[] {
			240, 2287, 67823, 16777215, 4294967295L, 1099511627775L, 281474976710655L, 72057594037927935L,
			72057594037927935L + 1
	};

	@Test
	public void testVarint() {
		ByteBuffer bb = ByteBuffer.allocate(9);
		for (int i = 0; i < values.length; i++) {
			bb.clear();
			Varint.writeUnsigned(bb, values[i]);
			bb.flip();
			assertEquals("Encoding should use " + (i + 1) + " bytes", i + 1, bb.remaining());
			assertEquals("Encoded and decoded value should be equal", values[i], Varint.readUnsigned(bb));
		}
	}

	@Test
	public void testVarintGroup() {
		ByteBuffer bb = ByteBuffer.allocate(2 + 4 * Long.BYTES);
		for (int i = 0; i < values.length - 4; i++) {
			long[] expected = new long[4];
			System.arraycopy(values, 0, expected, 0, 4);
			bb.clear();
			Varint.writeGroupUnsigned4(bb, expected[0], expected[1], expected[2], expected[3]);
			bb.flip();
			long[] actual = new long[4];
			Varint.readGroupUnsigned(bb, actual);
			assertArrayEquals("Encoded and decoded value should be equal", expected, actual);
		}
	}

	@Test
	public void testVarintGroupSmallValues() {
		ByteBuffer bb = ByteBuffer.allocate(2 + 4 * Long.BYTES);
		Varint.writeGroupUnsigned4(bb, 0, 1, 2, 3);
		bb.flip();
		assertEquals(bb.remaining(), 2 + 4);
		bb.clear();
		Varint.writeGroupUnsigned4(bb, 10, 4, 7, 9);
		bb.flip();
		assertEquals(bb.remaining(), 2 + 4);
	}
}
