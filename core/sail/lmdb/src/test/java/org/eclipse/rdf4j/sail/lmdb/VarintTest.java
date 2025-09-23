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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VarintTest {

	private final ByteOrder[] byteOrders = new ByteOrder[] { ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN };

	long[] values = new long[] {
			240, 2287, 67823, 16777215, 4294967295L, 1099511627775L, 281474976710655L, 72057594037927935L,
			72057594037927935L + 1
	};

	@Test
	public void testVarint() {
		for (ByteOrder order : byteOrders) {
			ByteBuffer bb = ByteBuffer.allocate(9).order(order);
			for (int i = 0; i < values.length; i++) {
				bb.clear();
				Varint.writeUnsigned(bb, values[i]);
				bb.flip();
				Assertions.assertEquals(i + 1, bb.remaining(), "Encoding should use " + (i + 1) + " bytes");
				Assertions.assertEquals(values[i], Varint.readUnsigned(bb),
						"Encoded and decoded value should be equal");
			}
		}
	}

	@Test
	public void testVarintList() {
		for (ByteOrder order : byteOrders) {
			ByteBuffer bb = ByteBuffer.allocate(2 + 4 * Long.BYTES).order(order);
			for (int i = 0; i < values.length - 4; i++) {
				long[] expected = new long[4];
				System.arraycopy(values, 0, expected, 0, 4);
				bb.clear();
				Varint.writeListUnsigned(bb, expected);
				bb.flip();
				long[] actual = new long[4];
				Varint.readListUnsigned(bb, actual);
				Assertions.assertArrayEquals(expected, actual, "Encoded and decoded value should be equal");
			}
		}
	}
}
