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
package org.eclipse.rdf4j.common.io;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.BitSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ByteArrayUtilTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testMatchesPattern() {
		assertTrue(ByteArrayUtil.matchesPattern(new byte[] {}, new byte[] { 0, 32 }, new byte[] { 0, 16 }));
		assertTrue(ByteArrayUtil.matchesPattern(new byte[] { 4 }, new byte[] { 0 }, new byte[] { 2 }));

		assertFalse(ByteArrayUtil.matchesPattern(new byte[] { 2, 0 }, new byte[] { 2, 0 }, new byte[] { 0, 16 }));
	}

	@Test
	public void testRegionMatches() {
		assertTrue(ByteArrayUtil.regionMatches(new byte[] {}, new byte[] { 1, 0 }, 0));
		assertTrue(ByteArrayUtil.regionMatches(new byte[] { 2, 3, 4, 5 }, new byte[] { 1, 2, 3, 4, 5, 6 }, 1));

		assertFalse(ByteArrayUtil.regionMatches(new byte[] { 2, 3, 4, 5 }, new byte[] { 1, 2, 3, 4, 5, 6 }, 0));
		assertFalse(ByteArrayUtil.regionMatches(new byte[] { 2, 3, 4, 5 }, new byte[] { 1, 2, 3, 4, 5, 6 }, 4));
	}

	@Test
	public void testCompareRegion() {
		assertEquals(0, ByteArrayUtil.compareRegion(new byte[] {}, 0, new byte[] {}, 0, 0));
		assertEquals(0, ByteArrayUtil.compareRegion(new byte[] { 0, 2 }, 0, new byte[] { 0, 1 }, 0, 1));

		assertEquals(-1, ByteArrayUtil.compareRegion(new byte[] { 0, 0 }, 0, new byte[] { 0, 1 }, 0, 2));
		assertEquals(1, ByteArrayUtil.compareRegion(new byte[] { 0, 2 }, 0, new byte[] { 0, 1 }, 0, 2));
	}

	@Test
	public void testFind() {
		assertEquals(-1, ByteArrayUtil.find(new byte[] {}, 0, 1, (byte) 0));
		assertEquals(-1, ByteArrayUtil.find(new byte[] { 0, 1, 2, 3, 4, 5, 6 }, 0, 20, (byte) 7));
		assertEquals(-1, ByteArrayUtil.find(new byte[] { 0, 1, 2, 3, 4, 5, 6 }, -5, 20, (byte) 7));

		assertEquals(3, ByteArrayUtil.find(new byte[] { 0, 1, 2, 3, 4, 5, 6 }, 0, 7, (byte) 3));
		assertEquals(3, ByteArrayUtil.find(new byte[] { 0, 1, 2, 3, 4, 5, 6 }, -10, 20, (byte) 3));
	}

	@Test
	public void testFindArray() {
		assertEquals(0, ByteArrayUtil.find(new byte[] {}, 0, 1, new byte[] {}));
		assertEquals(2, ByteArrayUtil.find(new byte[] { 0, 1, 2, 3, 4, 5, 6 }, 0, 7, new byte[] { 2, 3 }));
		assertEquals(2, ByteArrayUtil.find(new byte[] { 0, 1, 2, 3, 4, 5, 6 }, 0, 20, new byte[] { 2, 3 }));

		assertEquals(-1, ByteArrayUtil.find(new byte[] { 0, 1, 2, 3, 4, 5, 6 }, 0, 7, new byte[] { 2, 4 }));
		assertEquals(-1, ByteArrayUtil.find(new byte[] { 0, 1, 2, 3, 4, 5, 6 }, 0, 20, new byte[] { 7, 8 }));
	}

	@Test
	public void testGetInt() {
		assertEquals(0, ByteArrayUtil.getInt(new byte[] { 0, 0, 0, 0, 0, 0 }, 0));
		assertEquals((1 << 24) + (2 << 16) + (3 << 8) + 4, ByteArrayUtil.getInt(new byte[] { 1, 2, 3, 4 }, 0));
		assertEquals((1 << 24) + (2 << 16) + (3 << 8) + 4, ByteArrayUtil.getInt(new byte[] { 10, 1, 2, 3, 4, 10 }, 1));

		thrown.expect(ArrayIndexOutOfBoundsException.class);
		ByteArrayUtil.getInt(new byte[] { 1 }, 0);
	}

	@Test
	public void testGetLong() {
		assertEquals(0L, ByteArrayUtil.getLong(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 }, 0));
		assertEquals(1L, ByteArrayUtil.getLong(new byte[] { 0, 0, 0, 0, 0, 0, 0, 1 }, 0));
		assertEquals(1L << 56, ByteArrayUtil.getLong(new byte[] { 1, 0, 0, 0, 0, 0, 0, 0 }, 0));
		assertEquals(0L, ByteArrayUtil.getLong(new byte[] { 127, 0, 0, 0, 0, 0, 0, 0, 0, 127 }, 1));

		thrown.expect(ArrayIndexOutOfBoundsException.class);
		ByteArrayUtil.getLong(new byte[] { 1 }, 0);
	}

	@Test
	public void testToBitSet() {
		assertEquals(new BitSet(), ByteArrayUtil.toBitSet(new byte[] {}));

		final BitSet bitSet = ByteArrayUtil.toBitSet(new byte[] { 1, 2, 3, 4 });
		assertArrayEquals(new byte[] { -128, 64, -64, 32 }, bitSet.toByteArray());
	}

	@Test
	public void testToByteArray() {
		final BitSet bitSet = new BitSet();

		assertArrayEquals(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 }, ByteArrayUtil.toByteArray(bitSet));

		bitSet.set(1);
		assertArrayEquals(new byte[] { 64, 0, 0, 0, 0, 0, 0, 0, 0 }, ByteArrayUtil.toByteArray(bitSet));

		bitSet.set(4);
		assertArrayEquals(new byte[] { 72, 0, 0, 0, 0, 0, 0, 0, 0 }, ByteArrayUtil.toByteArray(bitSet));
	}

	@Test
	public void testToHexString() {
		assertEquals("", ByteArrayUtil.toHexString(new byte[] {}));
		assertEquals("1a", ByteArrayUtil.toHexString(new byte[] { 0x1A }));
		assertEquals("1a2b3c", ByteArrayUtil.toHexString(new byte[] { 0x1A, 0x2B, 0x3C }));
		assertEquals("010203", ByteArrayUtil.toHexString(new byte[] { 0x01, 0x02, 0x03 }));
	}

	@Test
	public void testPutLong() {
		final byte[] byteArray = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

		ByteArrayUtil.putLong(1L, byteArray, 0);
		assertArrayEquals(new byte[] { 0, 0, 0, 0, 0, 0, 0, 1, 0, 0 }, byteArray);

		ByteArrayUtil.putLong(1L << 56, byteArray, 0);
		assertArrayEquals(new byte[] { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, byteArray);

		ByteArrayUtil.putLong(0L, byteArray, 0);
		assertArrayEquals(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, byteArray);

		ByteArrayUtil.putLong(1L, byteArray, 2);
		assertArrayEquals(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }, byteArray);
	}

	@Test
	public void testPutInt() {
		final byte[] byteArray = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

		ByteArrayUtil.putInt(1, byteArray, 0);
		assertArrayEquals(new byte[] { 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 }, byteArray);

		ByteArrayUtil.putInt(1 << 24, byteArray, 0);
		assertArrayEquals(new byte[] { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, byteArray);

		ByteArrayUtil.putInt(0, byteArray, 0);
		assertArrayEquals(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, byteArray);

		ByteArrayUtil.putInt(1, byteArray, 2);
		assertArrayEquals(new byte[] { 0, 0, 0, 0, 0, 1, 0, 0, 0, 0 }, byteArray);
	}

	@Test
	public void testGet() {
		assertArrayEquals(new byte[] {}, ByteArrayUtil.get(new byte[] {}, 0));
		assertArrayEquals(new byte[] { 3, 4 }, ByteArrayUtil.get(new byte[] { 1, 2, 3, 4 }, 2));
		assertArrayEquals(new byte[] { 3, 4, 5 }, ByteArrayUtil.get(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }, 2, 3));
		assertArrayEquals(new byte[] {}, ByteArrayUtil.get(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }, 2, 0));
	}

	@Test
	public void testPut() {
		final byte[] byteArray = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

		ByteArrayUtil.put(new byte[] { 1, 2, 3, 4 }, byteArray, 0);
		assertArrayEquals(new byte[] { 1, 2, 3, 4, 0, 0, 0, 0, 0, 0 }, byteArray);

		ByteArrayUtil.put(new byte[] { 5, 6, 7, 8 }, byteArray, 4);
		assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 0, 0 }, byteArray);
	}
}
