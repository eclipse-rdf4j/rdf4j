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
package org.eclipse.rdf4j.sail.lmdb.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.eclipse.rdf4j.sail.lmdb.util.Bytes.RegionComparator;
import org.junit.jupiter.api.Test;

class BytesRegionComparatorTest {

	@Test
	void comparesAllLengthsAndPositions() {
		for (int len = 0; len <= 25; len++) {
			byte[] reference = buildReference(len);
			RegionComparator comparator = Bytes.capturedComparator(reference, 0, len);

			if (len == 0) {
				assertEquals(0, comparator.compare((byte) 7, ByteBuffer.wrap(new byte[] { 1 }), 0));
				continue;
			}

			ByteBuffer identicalBuffer = ByteBuffer.wrap(reference.clone());
			assertEquals(0, comparator.compare(reference[0], identicalBuffer, 0));

			for (int index = 0; index < len; index++) {
				if (index == 0) {
					byte otherFirst = mutate(reference[0], index);
					int expected = diff(reference[0], otherFirst);
					assertEquals(expected, comparator.compare(otherFirst, ByteBuffer.wrap(reference.clone()), 0));
				} else {
					byte[] candidate = reference.clone();
					candidate[index] = mutate(candidate[index], index);
					ByteBuffer differingBuffer = ByteBuffer.wrap(candidate);
					int expected = diff(reference[index], candidate[index]);
					assertEquals(expected, comparator.compare(reference[0], differingBuffer, 0));
				}
			}
		}
	}

	@Test
	void honoursOffsetWithinSourceArray() {
		byte[] base = new byte[16];
		for (int i = 0; i < base.length; i++) {
			base[i] = (byte) (i * 9 + 5);
		}

		int offset = 3;
		int len = 6;
		RegionComparator comparator = Bytes.capturedComparator(base, offset, len);

		byte[] slice = Arrays.copyOfRange(base, offset, offset + len);
		assertEquals(0, comparator.compare(base[offset], ByteBuffer.wrap(slice.clone()), 0));

		byte otherFirst = mutate(base[offset], 0);
		int expectedFirst = diff(base[offset], otherFirst);
		assertEquals(expectedFirst, comparator.compare(otherFirst, ByteBuffer.wrap(slice.clone()), 0));

		byte[] differingTail = slice.clone();
		differingTail[len - 2] = mutate(differingTail[len - 2], 1);
		ByteBuffer tailBuffer = ByteBuffer.wrap(differingTail);
		int expectedTail = diff(base[offset + len - 2], differingTail[len - 2]);
		assertEquals(expectedTail, comparator.compare(base[offset], tailBuffer, 0));
	}

	private static byte[] buildReference(int len) {
		byte[] data = new byte[len];
		for (int i = 0; i < len; i++) {
			data[i] = (byte) (i * 17 + 3);
		}
		return data;
	}

	private static byte mutate(byte value, int index) {
		return (byte) (value + (index % 2 == 0 ? 1 : -1));
	}

	private static int diff(byte a, byte b) {
		return (a & 0xFF) - (b & 0xFF);
	}
}
