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

	private final static boolean bigEndian = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

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

	public static RegionComparator capturedComparator(byte[] array, int offset, int len) {
		if (len <= 0) {
			return (firstByte, b) -> true;
		}
		switch (len) {
		case 1:
			return comparatorLen1(array, offset);
		case 2:
			return comparatorLen2(array, offset);
		case 3:
			return comparatorLen3(array, offset);
		case 4:
			return comparatorLen4(array, offset);
		case 5:
			return comparatorLen5(array, offset);
		case 6:
			return comparatorLen6(array, offset);
		case 7:
			return comparatorLen7(array, offset);
		case 8:
			return comparatorLen8(array, offset);
		case 9:
			return comparatorLen9(array, offset);
		case 10:
			return comparatorLen10(array, offset);
		case 11:
			return comparatorLen11(array, offset);
		case 12:
			return comparatorLen12(array, offset);
		case 13:
			return comparatorLen13(array, offset);
		case 14:
			return comparatorLen14(array, offset);
		case 15:
			return comparatorLen15(array, offset);
		case 16:
			return comparatorLen16(array, offset);
		case 17:
			return comparatorLen17(array, offset);
		case 18:
			return comparatorLen18(array, offset);
		case 19:
			return comparatorLen19(array, offset);
		case 20:
			return comparatorLen20(array, offset);
		default:
			return comparatorGeneric(array, offset, len);
		}
	}

	private static RegionComparator comparatorLen1(byte[] array, int offset) {
		return (firstByte, b) -> equals(array[offset], firstByte);
	}

	private static RegionComparator comparatorLen2(byte[] array, int offset) {

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			return equals(array[offset + 1], b.get());
		};
	}

	private static RegionComparator comparatorLen3(byte[] array, int offset) {

		final short expected = toShort(array, offset + 1);
		final short expectedEndian = bigEndian ? expected : Short.reverseBytes(expected);

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			return equals(expectedEndian, b.getShort());
		};
	}

	private static RegionComparator comparatorLen4(byte[] array, int offset) {

		final int expected = toInt(array, offset);
		final int expectedEndian = bigEndian ? expected : Integer.reverseBytes(expected);

		return (firstByte, b) -> {

			b.position(b.position() - 1);

			return equals(expectedEndian, b.getInt());
		};
	}

	private static RegionComparator comparatorLen5(byte[] array, int offset) {

		final int expected = toInt(array, offset + 1);
		final int expectedEndian = bigEndian ? expected : Integer.reverseBytes(expected);

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			return equals(expectedEndian, b.getInt());
		};
	}

	private static RegionComparator comparatorLen6(byte[] array, int offset) {

		final int expected = toInt(array, offset + 1);
		final int expectedEndian = bigEndian ? expected : Integer.reverseBytes(expected);
		final byte tail = array[offset + 5];

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(expectedEndian, b.getInt())) {
				return false;
			}

			return equals(tail, b.get());
		};
	}

	private static RegionComparator comparatorLen7(byte[] array, int offset) {

		final int expected = toInt(array, offset + 1);
		final int expectedEndian = bigEndian ? expected : Integer.reverseBytes(expected);
		final short expectedTail = toShort(array, offset + 5);
		final short expectedTailEndian = bigEndian ? expectedTail : Short.reverseBytes(expectedTail);

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(expectedEndian, b.getInt())) {
				return false;
			}

			return equals(expectedTailEndian, b.getShort());
		};
	}

	private static RegionComparator comparatorLen8(byte[] array, int offset) {

		final int expected = toInt(array, offset + 1);
		final int expectedEndian = bigEndian ? expected : Integer.reverseBytes(expected);
		final short expectedShort = toShort(array, offset + 5);
		final short expectedShortEndian = bigEndian ? expectedShort : Short.reverseBytes(expectedShort);
		final byte tail = array[offset + 7];

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(expectedEndian, b.getInt())) {
				return false;
			}

			if (!equals(expectedShortEndian, b.getShort())) {
				return false;
			}

			return equals(tail, b.get());
		};
	}

	private static RegionComparator comparatorLen9(byte[] array, int offset) {

		final int expected1 = toInt(array, offset + 1);
		final int expected1Endian = bigEndian ? expected1 : Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, offset + 5);
		final int expected2Endian = bigEndian ? expected2 : Integer.reverseBytes(expected2);

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(expected1Endian, b.getInt())) {
				return false;
			}

			return equals(expected2Endian, b.getInt());
		};
	}

	private static RegionComparator comparatorLen10(byte[] array, int offset) {

		final int expected1 = toInt(array, offset + 1);
		final int expected1Endian = bigEndian ? expected1 : Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, offset + 5);
		final int expected2Endian = bigEndian ? expected2 : Integer.reverseBytes(expected2);
		final byte tail = array[offset + 9];

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(expected1Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected2Endian, b.getInt())) {
				return false;
			}

			return equals(tail, b.get());
		};
	}

	private static RegionComparator comparatorLen11(byte[] array, int offset) {

		final int expected1 = toInt(array, offset + 1);
		final int expected1Endian = bigEndian ? expected1 : Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, offset + 5);
		final int expected2Endian = bigEndian ? expected2 : Integer.reverseBytes(expected2);
		final short expectedShort = toShort(array, offset + 9);
		final short expectedShortEndian = bigEndian ? expectedShort : Short.reverseBytes(expectedShort);

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(expected1Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected2Endian, b.getInt())) {
				return false;
			}

			return equals(expectedShortEndian, b.getShort());
		};
	}

	private static RegionComparator comparatorLen12(byte[] array, int offset) {

		final int expected1 = toInt(array, offset + 1);
		final int expected1Endian = bigEndian ? expected1 : Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, offset + 5);
		final int expected2Endian = bigEndian ? expected2 : Integer.reverseBytes(expected2);
		final short expectedShort = toShort(array, offset + 9);
		final short expectedShortEndian = bigEndian ? expectedShort : Short.reverseBytes(expectedShort);
		final byte tail = array[offset + 11];

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(expected1Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected2Endian, b.getInt())) {
				return false;
			}

			if (!equals(expectedShortEndian, b.getShort())) {
				return false;
			}

			return equals(tail, b.get());
		};
	}

	private static RegionComparator comparatorLen13(byte[] array, int offset) {

		final int expected1 = toInt(array, offset + 1);
		final int expected1Endian = bigEndian ? expected1 : Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, offset + 5);
		final int expected2Endian = bigEndian ? expected2 : Integer.reverseBytes(expected2);
		final int expected3 = toInt(array, offset + 9);
		final int expected3Endian = bigEndian ? expected3 : Integer.reverseBytes(expected3);

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(expected1Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected2Endian, b.getInt())) {
				return false;
			}

			return equals(expected3Endian, b.getInt());
		};
	}

	private static RegionComparator comparatorLen14(byte[] array, int offset) {

		final int expected1 = toInt(array, offset + 1);
		final int expected1Endian = bigEndian ? expected1 : Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, offset + 5);
		final int expected2Endian = bigEndian ? expected2 : Integer.reverseBytes(expected2);
		final int expected3 = toInt(array, offset + 9);
		final int expected3Endian = bigEndian ? expected3 : Integer.reverseBytes(expected3);
		final byte tail = array[offset + 13];

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(expected1Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected2Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected3Endian, b.getInt())) {
				return false;
			}

			return equals(tail, b.get());
		};
	}

	private static RegionComparator comparatorLen15(byte[] array, int offset) {

		final int expected1 = toInt(array, offset + 1);
		final int expected1Endian = bigEndian ? expected1 : Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, offset + 5);
		final int expected2Endian = bigEndian ? expected2 : Integer.reverseBytes(expected2);
		final int expected3 = toInt(array, offset + 9);
		final int expected3Endian = bigEndian ? expected3 : Integer.reverseBytes(expected3);
		final short expectedShort = toShort(array, offset + 13);
		final short expectedShortEndian = bigEndian ? expectedShort : Short.reverseBytes(expectedShort);

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(expected1Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected2Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected3Endian, b.getInt())) {
				return false;
			}

			return equals(expectedShortEndian, b.getShort());
		};
	}

	private static RegionComparator comparatorLen16(byte[] array, int offset) {

		final int expected1 = toInt(array, offset + 1);
		final int expected1Endian = bigEndian ? expected1 : Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, offset + 5);
		final int expected2Endian = bigEndian ? expected2 : Integer.reverseBytes(expected2);
		final int expected3 = toInt(array, offset + 9);
		final int expected3Endian = bigEndian ? expected3 : Integer.reverseBytes(expected3);
		final short expectedShort = toShort(array, offset + 13);
		final short expectedShortEndian = bigEndian ? expectedShort : Short.reverseBytes(expectedShort);
		final byte tail = array[offset + 15];

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(expected1Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected2Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected3Endian, b.getInt())) {
				return false;
			}

			if (!equals(expectedShortEndian, b.getShort())) {
				return false;
			}

			return equals(tail, b.get());
		};
	}

	private static RegionComparator comparatorLen17(byte[] array, int offset) {

		final int expected1 = toInt(array, offset + 1);
		final int expected1Endian = bigEndian ? expected1 : Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, offset + 5);
		final int expected2Endian = bigEndian ? expected2 : Integer.reverseBytes(expected2);
		final int expected3 = toInt(array, offset + 9);
		final int expected3Endian = bigEndian ? expected3 : Integer.reverseBytes(expected3);
		final int expected4 = toInt(array, offset + 13);
		final int expected4Endian = bigEndian ? expected4 : Integer.reverseBytes(expected4);

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(expected1Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected2Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected3Endian, b.getInt())) {
				return false;
			}

			return equals(expected4Endian, b.getInt());
		};
	}

	private static RegionComparator comparatorLen18(byte[] array, int offset) {

		final int expected1 = toInt(array, offset + 1);
		final int expected1Endian = bigEndian ? expected1 : Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, offset + 5);
		final int expected2Endian = bigEndian ? expected2 : Integer.reverseBytes(expected2);
		final int expected3 = toInt(array, offset + 9);
		final int expected3Endian = bigEndian ? expected3 : Integer.reverseBytes(expected3);
		final int expected4 = toInt(array, offset + 13);
		final int expected4Endian = bigEndian ? expected4 : Integer.reverseBytes(expected4);
		final byte tail = array[offset + 17];

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(expected1Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected2Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected3Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected4Endian, b.getInt())) {
				return false;
			}

			return equals(tail, b.get());
		};
	}

	private static RegionComparator comparatorLen19(byte[] array, int offset) {

		final int expected1 = toInt(array, offset + 1);
		final int expected1Endian = bigEndian ? expected1 : Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, offset + 5);
		final int expected2Endian = bigEndian ? expected2 : Integer.reverseBytes(expected2);
		final int expected3 = toInt(array, offset + 9);
		final int expected3Endian = bigEndian ? expected3 : Integer.reverseBytes(expected3);
		final int expected4 = toInt(array, offset + 13);
		final int expected4Endian = bigEndian ? expected4 : Integer.reverseBytes(expected4);
		final short expectedShort = toShort(array, offset + 17);
		final short expectedShortEndian = bigEndian ? expectedShort : Short.reverseBytes(expectedShort);

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(expected1Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected2Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected3Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected4Endian, b.getInt())) {
				return false;
			}

			return equals(expectedShortEndian, b.getShort());
		};
	}

	private static RegionComparator comparatorLen20(byte[] array, int offset) {

		final int expected1 = toInt(array, offset + 1);
		final int expected1Endian = bigEndian ? expected1 : Integer.reverseBytes(expected1);
		final int expected2 = toInt(array, offset + 5);
		final int expected2Endian = bigEndian ? expected2 : Integer.reverseBytes(expected2);
		final int expected3 = toInt(array, offset + 9);
		final int expected3Endian = bigEndian ? expected3 : Integer.reverseBytes(expected3);
		final int expected4 = toInt(array, offset + 13);
		final int expected4Endian = bigEndian ? expected4 : Integer.reverseBytes(expected4);
		final short expectedShort = toShort(array, offset + 17);
		final short expectedShortEndian = bigEndian ? expectedShort : Short.reverseBytes(expectedShort);
		final byte tail = array[offset + 19];

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(expected1Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected2Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected3Endian, b.getInt())) {
				return false;
			}

			if (!equals(expected4Endian, b.getInt())) {
				return false;
			}

			if (!equals(expectedShortEndian, b.getShort())) {
				return false;
			}

			return equals(tail, b.get());
		};
	}

	private static RegionComparator comparatorGeneric(byte[] array, int offset, int len) {
		final int start = offset;
		final int end = offset + len;
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
