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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class Bytes {
	private Bytes() {
	}

	@FunctionalInterface
	public interface RegionComparator {
		boolean equals(byte firstByte, ByteBuffer other);
	}

	private static boolean equals(int a, int b) {
		return a == b;
	}

	private static short toShort(byte[] array, int offset) {
		return (short) (((array[offset] & 0xFF) << 8) | (array[offset + 1] & 0xFF));
	}

	private static int toInt(byte[] array, int offset) {
		return ((array[offset] & 0xFF) << 24)
				| ((array[offset + 1] & 0xFF) << 16)
				| ((array[offset + 2] & 0xFF) << 8)
				| (array[offset + 3] & 0xFF);
	}

	public static RegionComparator capturedComparator(byte[] array, int len) {
		if (len <= 0) {
			return (firstByte, b) -> true;
		}
		switch (len) {
		case 1:
			return comparatorLen1(array);
		case 2:
			return comparatorLen2(array);
		case 3:
			return comparatorLen3(array);
		case 4:
			return comparatorLen4(array);
		case 5:
			return comparatorLen5(array);
		case 6:
			return comparatorLen6(array);
		case 7:
			return comparatorLen7(array);
		case 8:
			return comparatorLen8(array);
		case 9:
			return comparatorLen9(array);
		case 10:
			return comparatorLen10(array);
		case 11:
			return comparatorLen11(array);
		case 12:
			return comparatorLen12(array);
		case 13:
			return comparatorLen13(array);
		case 14:
			return comparatorLen14(array);
		case 15:
			return comparatorLen15(array);
		case 16:
			return comparatorLen16(array);
		case 17:
			return comparatorLen17(array);
		case 18:
			return comparatorLen18(array);
		case 19:
			return comparatorLen19(array);
		case 20:
			return comparatorLen20(array);
		default:
			return comparatorGeneric(array, len);
		}
	}

	private static RegionComparator comparatorLen1(byte[] array) {
		return (firstByte, b) -> equals(array[0], firstByte);
	}

	private static RegionComparator comparatorLen2(byte[] array) {

		return (firstByte, b) -> {
			if (!equals(array[0], firstByte)) {
				return false;
			}

			return equals(array[0 + 1], b.get());
		};
	}

	private static RegionComparator comparatorLen3(byte[] array) {

		final short expected = toShort(array, 0 + 1);
		final short expectedLE = Short.reverseBytes(expected);

		return (firstByte, b) -> {
			if (!equals(array[0], firstByte)) {
				return false;
			}

			boolean bigEndian = b.order() == ByteOrder.BIG_ENDIAN;
			return equals(bigEndian ? expected : expectedLE, b.getShort());
		};
	}

	private static RegionComparator comparatorLen4(byte[] array) {

		final short expected = toShort(array, 0 + 1);
		final short expectedLE = Short.reverseBytes(expected);
		final byte expectedTail = array[0 + 3];

		return (firstByte, b) -> {
			if (!equals(array[0], firstByte)) {
				return false;
			}

			boolean bigEndian = b.order() == ByteOrder.BIG_ENDIAN;
			if (!equals(bigEndian ? expected : expectedLE, b.getShort())) {
				return false;
			}

			return equals(expectedTail, b.get());
		};
	}

	private static RegionComparator comparatorLen5(byte[] array) {

		final int expected = toInt(array, 0 + 1);
		final int expectedLE = Integer.reverseBytes(expected);

		return (firstByte, b) -> {
			if (!equals(array[0], firstByte)) {
				return false;
			}

			boolean bigEndian = b.order() == ByteOrder.BIG_ENDIAN;
			return equals(bigEndian ? expected : expectedLE, b.getInt());
		};
	}

	private static RegionComparator comparatorLen6(byte[] array) {

		final int expected = toInt(array, 0 + 1);
		final int expectedLE = Integer.reverseBytes(expected);
		final byte tail = array[0 + 5];

		return (firstByte, b) -> {
			if (!equals(array[0], firstByte)) {
				return false;
			}

			boolean bigEndian = b.order() == ByteOrder.BIG_ENDIAN;
			if (!equals(bigEndian ? expected : expectedLE, b.getInt())) {
				return false;
			}

			return equals(tail, b.get());
		};
	}

	private static RegionComparator comparatorLen7(byte[] array) {

		final int expected = toInt(array, 0 + 1);
		final int expectedLE = Integer.reverseBytes(expected);
		final short expectedTail = toShort(array, 0 + 5);
		final short expectedTailLE = Short.reverseBytes(expectedTail);

		return (firstByte, b) -> {
			if (!equals(array[0], firstByte)) {
				return false;
			}

			boolean bigEndian = b.order() == ByteOrder.BIG_ENDIAN;
			if (!equals(bigEndian ? expected : expectedLE, b.getInt())) {
				return false;
			}

			return equals(bigEndian ? expectedTail : expectedTailLE, b.getShort());
		};
	}

	private static RegionComparator comparatorLen8(byte[] array) {

		final int expected = toInt(array, 0 + 1);
		final int expectedLE = Integer.reverseBytes(expected);
		final short expectedShort = toShort(array, 0 + 5);
		final short expectedShortLE = Short.reverseBytes(expectedShort);
		final byte tail = array[0 + 7];

		return (firstByte, b) -> {
			if (!equals(array[0], firstByte)) {
				return false;
			}

			boolean bigEndian = b.order() == ByteOrder.BIG_ENDIAN;
			if (!equals(bigEndian ? expected : expectedLE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expectedShort : expectedShortLE, b.getShort())) {
				return false;
			}

			return equals(tail, b.get());
		};
	}

	private static RegionComparator comparatorLen9(byte[] array) {

		final int expected1 = toInt(array, 0 + 1);
		final int expected1LE = Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, 0 + 5);
		final int expected2LE = Integer.reverseBytes(expected2);

		return (firstByte, b) -> {
			if (!equals(array[0], firstByte)) {
				return false;
			}

			boolean bigEndian = b.order() == ByteOrder.BIG_ENDIAN;
			if (!equals(bigEndian ? expected1 : expected1LE, b.getInt())) {
				return false;
			}

			return equals(bigEndian ? expected2 : expected2LE, b.getInt());
		};
	}

	private static RegionComparator comparatorLen10(byte[] array) {

		final int expected1 = toInt(array, 0 + 1);
		final int expected1LE = Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, 0 + 5);
		final int expected2LE = Integer.reverseBytes(expected2);
		final byte tail = array[0 + 9];

		return (firstByte, b) -> {
			if (!equals(array[0], firstByte)) {
				return false;
			}

			boolean bigEndian = b.order() == ByteOrder.BIG_ENDIAN;
			if (!equals(bigEndian ? expected1 : expected1LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected2 : expected2LE, b.getInt())) {
				return false;
			}

			return equals(tail, b.get());
		};
	}

	private static RegionComparator comparatorLen11(byte[] array) {

		final int expected1 = toInt(array, 0 + 1);
		final int expected1LE = Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, 0 + 5);
		final int expected2LE = Integer.reverseBytes(expected2);
		final short expectedShort = toShort(array, 0 + 9);
		final short expectedShortLE = Short.reverseBytes(expectedShort);

		return (firstByte, b) -> {
			if (!equals(array[0], firstByte)) {
				return false;
			}

			boolean bigEndian = b.order() == ByteOrder.BIG_ENDIAN;
			if (!equals(bigEndian ? expected1 : expected1LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected2 : expected2LE, b.getInt())) {
				return false;
			}

			return equals(bigEndian ? expectedShort : expectedShortLE, b.getShort());
		};
	}

	private static RegionComparator comparatorLen12(byte[] array) {

		final int expected1 = toInt(array, 0 + 1);
		final int expected1LE = Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, 0 + 5);
		final int expected2LE = Integer.reverseBytes(expected2);
		final short expectedShort = toShort(array, 0 + 9);
		final short expectedShortLE = Short.reverseBytes(expectedShort);
		final byte tail = array[0 + 11];

		return (firstByte, b) -> {
			if (!equals(array[0], firstByte)) {
				return false;
			}

			boolean bigEndian = b.order() == ByteOrder.BIG_ENDIAN;
			if (!equals(bigEndian ? expected1 : expected1LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected2 : expected2LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expectedShort : expectedShortLE, b.getShort())) {
				return false;
			}

			return equals(tail, b.get());
		};
	}

	private static RegionComparator comparatorLen13(byte[] array) {

		final int expected1 = toInt(array, 0 + 1);
		final int expected1LE = Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, 0 + 5);
		final int expected2LE = Integer.reverseBytes(expected2);
		final int expected3 = toInt(array, 0 + 9);
		final int expected3LE = Integer.reverseBytes(expected3);

		return (firstByte, b) -> {
			if (!equals(array[0], firstByte)) {
				return false;
			}

			boolean bigEndian = b.order() == ByteOrder.BIG_ENDIAN;
			if (!equals(bigEndian ? expected1 : expected1LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected2 : expected2LE, b.getInt())) {
				return false;
			}

			return equals(bigEndian ? expected3 : expected3LE, b.getInt());
		};
	}

	private static RegionComparator comparatorLen14(byte[] array) {

		final int expected1 = toInt(array, 0 + 1);
		final int expected1LE = Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, 0 + 5);
		final int expected2LE = Integer.reverseBytes(expected2);
		final int expected3 = toInt(array, 0 + 9);
		final int expected3LE = Integer.reverseBytes(expected3);
		final byte tail = array[0 + 13];

		return (firstByte, b) -> {
			if (!equals(array[0], firstByte)) {
				return false;
			}

			boolean bigEndian = b.order() == ByteOrder.BIG_ENDIAN;
			if (!equals(bigEndian ? expected1 : expected1LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected2 : expected2LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected3 : expected3LE, b.getInt())) {
				return false;
			}

			return equals(tail, b.get());
		};
	}

	private static RegionComparator comparatorLen15(byte[] array) {

		final int expected1 = toInt(array, 0 + 1);
		final int expected1LE = Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, 0 + 5);
		final int expected2LE = Integer.reverseBytes(expected2);
		final int expected3 = toInt(array, 0 + 9);
		final int expected3LE = Integer.reverseBytes(expected3);
		final short expectedShort = toShort(array, 0 + 13);
		final short expectedShortLE = Short.reverseBytes(expectedShort);

		return (firstByte, b) -> {
			if (!equals(array[0], firstByte)) {
				return false;
			}

			boolean bigEndian = b.order() == ByteOrder.BIG_ENDIAN;
			if (!equals(bigEndian ? expected1 : expected1LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected2 : expected2LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected3 : expected3LE, b.getInt())) {
				return false;
			}

			return equals(bigEndian ? expectedShort : expectedShortLE, b.getShort());
		};
	}

	private static RegionComparator comparatorLen16(byte[] array) {

		final int expected1 = toInt(array, 0 + 1);
		final int expected1LE = Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, 0 + 5);
		final int expected2LE = Integer.reverseBytes(expected2);
		final int expected3 = toInt(array, 0 + 9);
		final int expected3LE = Integer.reverseBytes(expected3);
		final short expectedShort = toShort(array, 0 + 13);
		final short expectedShortLE = Short.reverseBytes(expectedShort);
		final byte tail = array[0 + 15];

		return (firstByte, b) -> {
			if (!equals(array[0], firstByte)) {
				return false;
			}

			boolean bigEndian = b.order() == ByteOrder.BIG_ENDIAN;
			if (!equals(bigEndian ? expected1 : expected1LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected2 : expected2LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected3 : expected3LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expectedShort : expectedShortLE, b.getShort())) {
				return false;
			}

			return equals(tail, b.get());
		};
	}

	private static RegionComparator comparatorLen17(byte[] array) {

		final int expected1 = toInt(array, 0 + 1);
		final int expected1LE = Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, 0 + 5);
		final int expected2LE = Integer.reverseBytes(expected2);
		final int expected3 = toInt(array, 0 + 9);
		final int expected3LE = Integer.reverseBytes(expected3);
		final int expected4 = toInt(array, 0 + 13);
		final int expected4LE = Integer.reverseBytes(expected4);

		return (firstByte, b) -> {
			if (!equals(array[0], firstByte)) {
				return false;
			}

			boolean bigEndian = b.order() == ByteOrder.BIG_ENDIAN;
			if (!equals(bigEndian ? expected1 : expected1LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected2 : expected2LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected3 : expected3LE, b.getInt())) {
				return false;
			}

			return equals(bigEndian ? expected4 : expected4LE, b.getInt());
		};
	}

	private static RegionComparator comparatorLen18(byte[] array) {

		final int expected1 = toInt(array, 0 + 1);
		final int expected1LE = Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, 0 + 5);
		final int expected2LE = Integer.reverseBytes(expected2);
		final int expected3 = toInt(array, 0 + 9);
		final int expected3LE = Integer.reverseBytes(expected3);
		final int expected4 = toInt(array, 0 + 13);
		final int expected4LE = Integer.reverseBytes(expected4);
		final byte tail = array[0 + 17];

		return (firstByte, b) -> {
			if (!equals(array[0], firstByte)) {
				return false;
			}

			boolean bigEndian = b.order() == ByteOrder.BIG_ENDIAN;
			if (!equals(bigEndian ? expected1 : expected1LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected2 : expected2LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected3 : expected3LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected4 : expected4LE, b.getInt())) {
				return false;
			}

			return equals(tail, b.get());
		};
	}

	private static RegionComparator comparatorLen19(byte[] array) {

		final int expected1 = toInt(array, 0 + 1);
		final int expected1LE = Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, 0 + 5);
		final int expected2LE = Integer.reverseBytes(expected2);
		final int expected3 = toInt(array, 0 + 9);
		final int expected3LE = Integer.reverseBytes(expected3);
		final int expected4 = toInt(array, 0 + 13);
		final int expected4LE = Integer.reverseBytes(expected4);
		final short expectedShort = toShort(array, 0 + 17);
		final short expectedShortLE = Short.reverseBytes(expectedShort);

		return (firstByte, b) -> {
			if (!equals(array[0], firstByte)) {
				return false;
			}

			boolean bigEndian = b.order() == ByteOrder.BIG_ENDIAN;
			if (!equals(bigEndian ? expected1 : expected1LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected2 : expected2LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected3 : expected3LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected4 : expected4LE, b.getInt())) {
				return false;
			}

			return equals(bigEndian ? expectedShort : expectedShortLE, b.getShort());
		};
	}

	private static RegionComparator comparatorLen20(byte[] array) {

		final int expected1 = toInt(array, 0 + 1);
		final int expected1LE = Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, 0 + 5);
		final int expected2LE = Integer.reverseBytes(expected2);
		final int expected3 = toInt(array, 0 + 9);
		final int expected3LE = Integer.reverseBytes(expected3);
		final int expected4 = toInt(array, 0 + 13);
		final int expected4LE = Integer.reverseBytes(expected4);
		final short expectedShort = toShort(array, 0 + 17);
		final short expectedShortLE = Short.reverseBytes(expectedShort);
		final byte tail = array[0 + 19];

		return (firstByte, b) -> {
			if (!equals(array[0], firstByte)) {
				return false;
			}

			boolean bigEndian = b.order() == ByteOrder.BIG_ENDIAN;
			if (!equals(bigEndian ? expected1 : expected1LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected2 : expected2LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected3 : expected3LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expected4 : expected4LE, b.getInt())) {
				return false;
			}

			if (!equals(bigEndian ? expectedShort : expectedShortLE, b.getShort())) {
				return false;
			}

			return equals(tail, b.get());
		};
	}

	private static RegionComparator comparatorGeneric(byte[] array, int len) {
		final int start = 0;
		final int end = 0 + len;
		return (firstByte, b) -> {
			if (!equals(array[start], firstByte)) {
				return false;
			}

			int idx = start + 1;
			while (idx < end) {
				if (!equals(array[idx], b.get())) {
					return false;
				}
				idx++;
			}
			return true;
		};
	}
}
