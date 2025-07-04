/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.inlined;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BytesTest {

	@Test
	void testPackBytesWithValidInput() {
		byte[] bytes = { 0x01, 0x02, 0x03, 0x04 };
		long expected = 0x01020304L;
		assertEquals(expected, Bytes.packBytes(bytes), "Packing bytes should result in the correct long value.");
		assertArrayEquals(bytes, Bytes.unpackBytes(expected, 4),
				"Unpacking long should result in the correct byte array.");
	}

	@Test
	void testPackBytesWithEmptyArray() {
		byte[] bytes = {};
		long expected = 0L;
		assertEquals(expected, Bytes.packBytes(bytes), "Packing an empty array should result in 0L.");
	}

	@Test
	void testPackBytesWithSingleByte() {
		byte[] bytes = { 0x7F };
		long expected = 0x7FL;
		assertEquals(expected, Bytes.packBytes(bytes),
				"Packing a single byte should result in its long representation.");
	}

	@Test
	void testUnpackBytesWithZeroLength() {
		long value = 0x01020304L;
		byte[] expected = {};
		assertArrayEquals(expected, Bytes.unpackBytes(value, 0),
				"Unpacking with zero length should result in an empty array.");
	}

	@Test
	void testUnpackBytesWithSingleByte() {
		long value = 0x7FL;
		byte[] expected = { 0x7F };
		assertArrayEquals(expected, Bytes.unpackBytes(value, 1),
				"Unpacking a single byte should return the correct byte array.");
	}

	@Test
	void testPackAndUnpackConsistency() {
		byte[] originalBytes = { 0x01, 0x02, 0x03, 0x04 };
		long packedValue = Bytes.packBytes(originalBytes);
		byte[] unpackedBytes = Bytes.unpackBytes(packedValue, originalBytes.length);
		assertArrayEquals(originalBytes, unpackedBytes,
				"Packing then unpacking should result in the original byte array.");
	}
}
