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

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(array[offset + 1], b.get())) {
				return false;
			}

			return equals(array[offset + 2], b.get());
		};
	}

	private static RegionComparator comparatorLen4(byte[] array, int offset) {

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(array[offset + 1], b.get())) {
				return false;
			}

			if (!equals(array[offset + 2], b.get())) {
				return false;
			}

			return equals(array[offset + 3], b.get());
		};
	}

	private static RegionComparator comparatorLen5(byte[] array, int offset) {

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(array[offset + 1], b.get())) {
				return false;
			}

			if (!equals(array[offset + 2], b.get())) {
				return false;
			}

			if (!equals(array[offset + 3], b.get())) {
				return false;
			}

			return equals(array[offset + 4], b.get());
		};
	}

	private static RegionComparator comparatorLen6(byte[] array, int offset) {

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(array[offset + 1], b.get())) {
				return false;
			}

			if (!equals(array[offset + 2], b.get())) {
				return false;
			}

			if (!equals(array[offset + 3], b.get())) {
				return false;
			}

			if (!equals(array[offset + 4], b.get())) {
				return false;
			}

			return equals(array[offset + 5], b.get());
		};
	}

	private static RegionComparator comparatorLen7(byte[] array, int offset) {

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(array[offset + 1], b.get())) {
				return false;
			}

			if (!equals(array[offset + 2], b.get())) {
				return false;
			}

			if (!equals(array[offset + 3], b.get())) {
				return false;
			}

			if (!equals(array[offset + 4], b.get())) {
				return false;
			}

			if (!equals(array[offset + 5], b.get())) {
				return false;
			}

			return equals(array[offset + 6], b.get());
		};
	}

	private static RegionComparator comparatorLen8(byte[] array, int offset) {

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(array[offset + 1], b.get())) {
				return false;
			}

			if (!equals(array[offset + 2], b.get())) {
				return false;
			}

			if (!equals(array[offset + 3], b.get())) {
				return false;
			}

			if (!equals(array[offset + 4], b.get())) {
				return false;
			}

			if (!equals(array[offset + 5], b.get())) {
				return false;
			}

			if (!equals(array[offset + 6], b.get())) {
				return false;
			}

			return equals(array[offset + 7], b.get());
		};
	}

	private static RegionComparator comparatorLen9(byte[] array, int offset) {

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(array[offset + 1], b.get())) {
				return false;
			}

			if (!equals(array[offset + 2], b.get())) {
				return false;
			}

			if (!equals(array[offset + 3], b.get())) {
				return false;
			}

			if (!equals(array[offset + 4], b.get())) {
				return false;
			}

			if (!equals(array[offset + 5], b.get())) {
				return false;
			}

			if (!equals(array[offset + 6], b.get())) {
				return false;
			}

			if (!equals(array[offset + 7], b.get())) {
				return false;
			}

			return equals(array[offset + 8], b.get());
		};
	}

	private static RegionComparator comparatorLen10(byte[] array, int offset) {

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(array[offset + 1], b.get())) {
				return false;
			}

			if (!equals(array[offset + 2], b.get())) {
				return false;
			}

			if (!equals(array[offset + 3], b.get())) {
				return false;
			}

			if (!equals(array[offset + 4], b.get())) {
				return false;
			}

			if (!equals(array[offset + 5], b.get())) {
				return false;
			}

			if (!equals(array[offset + 6], b.get())) {
				return false;
			}

			if (!equals(array[offset + 7], b.get())) {
				return false;
			}

			if (!equals(array[offset + 8], b.get())) {
				return false;
			}

			if (!equals(array[offset + 9], b.get())) {
				return false;
			}

			return true;
		};
	}

	private static RegionComparator comparatorLen11(byte[] array, int offset) {

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(array[offset + 1], b.get())) {
				return false;
			}

			if (!equals(array[offset + 2], b.get())) {
				return false;
			}

			if (!equals(array[offset + 3], b.get())) {
				return false;
			}

			if (!equals(array[offset + 4], b.get())) {
				return false;
			}

			if (!equals(array[offset + 5], b.get())) {
				return false;
			}

			if (!equals(array[offset + 6], b.get())) {
				return false;
			}

			if (!equals(array[offset + 7], b.get())) {
				return false;
			}

			if (!equals(array[offset + 8], b.get())) {
				return false;
			}

			if (!equals(array[offset + 9], b.get())) {
				return false;
			}

			if (!equals(array[offset + 10], b.get())) {
				return false;
			}

			return true;
		};
	}

	private static RegionComparator comparatorLen12(byte[] array, int offset) {

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(array[offset + 1], b.get())) {
				return false;
			}

			if (!equals(array[offset + 2], b.get())) {
				return false;
			}

			if (!equals(array[offset + 3], b.get())) {
				return false;
			}

			if (!equals(array[offset + 4], b.get())) {
				return false;
			}

			if (!equals(array[offset + 5], b.get())) {
				return false;
			}

			if (!equals(array[offset + 6], b.get())) {
				return false;
			}

			if (!equals(array[offset + 7], b.get())) {
				return false;
			}

			if (!equals(array[offset + 8], b.get())) {
				return false;
			}

			if (!equals(array[offset + 9], b.get())) {
				return false;
			}

			if (!equals(array[offset + 10], b.get())) {
				return false;
			}

			if (!equals(array[offset + 11], b.get())) {
				return false;
			}

			return true;
		};
	}

	private static RegionComparator comparatorLen13(byte[] array, int offset) {

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(array[offset + 1], b.get())) {
				return false;
			}

			if (!equals(array[offset + 2], b.get())) {
				return false;
			}

			if (!equals(array[offset + 3], b.get())) {
				return false;
			}

			if (!equals(array[offset + 4], b.get())) {
				return false;
			}

			if (!equals(array[offset + 5], b.get())) {
				return false;
			}

			if (!equals(array[offset + 6], b.get())) {
				return false;
			}

			if (!equals(array[offset + 7], b.get())) {
				return false;
			}

			if (!equals(array[offset + 8], b.get())) {
				return false;
			}

			if (!equals(array[offset + 9], b.get())) {
				return false;
			}

			if (!equals(array[offset + 10], b.get())) {
				return false;
			}

			if (!equals(array[offset + 11], b.get())) {
				return false;
			}

			if (!equals(array[offset + 12], b.get())) {
				return false;
			}

			return true;
		};
	}

	private static RegionComparator comparatorLen14(byte[] array, int offset) {

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(array[offset + 1], b.get())) {
				return false;
			}

			if (!equals(array[offset + 2], b.get())) {
				return false;
			}

			if (!equals(array[offset + 3], b.get())) {
				return false;
			}

			if (!equals(array[offset + 4], b.get())) {
				return false;
			}

			if (!equals(array[offset + 5], b.get())) {
				return false;
			}

			if (!equals(array[offset + 6], b.get())) {
				return false;
			}

			if (!equals(array[offset + 7], b.get())) {
				return false;
			}

			if (!equals(array[offset + 8], b.get())) {
				return false;
			}

			if (!equals(array[offset + 9], b.get())) {
				return false;
			}

			if (!equals(array[offset + 10], b.get())) {
				return false;
			}

			if (!equals(array[offset + 11], b.get())) {
				return false;
			}

			if (!equals(array[offset + 12], b.get())) {
				return false;
			}

			if (!equals(array[offset + 13], b.get())) {
				return false;
			}

			return true;
		};
	}

	private static RegionComparator comparatorLen15(byte[] array, int offset) {

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(array[offset + 1], b.get())) {
				return false;
			}

			if (!equals(array[offset + 2], b.get())) {
				return false;
			}

			if (!equals(array[offset + 3], b.get())) {
				return false;
			}

			if (!equals(array[offset + 4], b.get())) {
				return false;
			}

			if (!equals(array[offset + 5], b.get())) {
				return false;
			}

			if (!equals(array[offset + 6], b.get())) {
				return false;
			}

			if (!equals(array[offset + 7], b.get())) {
				return false;
			}

			if (!equals(array[offset + 8], b.get())) {
				return false;
			}

			if (!equals(array[offset + 9], b.get())) {
				return false;
			}

			if (!equals(array[offset + 10], b.get())) {
				return false;
			}

			if (!equals(array[offset + 11], b.get())) {
				return false;
			}

			if (!equals(array[offset + 12], b.get())) {
				return false;
			}

			if (!equals(array[offset + 13], b.get())) {
				return false;
			}

			if (!equals(array[offset + 14], b.get())) {
				return false;
			}

			return true;
		};
	}

	private static RegionComparator comparatorLen16(byte[] array, int offset) {

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(array[offset + 1], b.get())) {
				return false;
			}

			if (!equals(array[offset + 2], b.get())) {
				return false;
			}

			if (!equals(array[offset + 3], b.get())) {
				return false;
			}

			if (!equals(array[offset + 4], b.get())) {
				return false;
			}

			if (!equals(array[offset + 5], b.get())) {
				return false;
			}

			if (!equals(array[offset + 6], b.get())) {
				return false;
			}

			if (!equals(array[offset + 7], b.get())) {
				return false;
			}

			if (!equals(array[offset + 8], b.get())) {
				return false;
			}

			if (!equals(array[offset + 9], b.get())) {
				return false;
			}

			if (!equals(array[offset + 10], b.get())) {
				return false;
			}

			if (!equals(array[offset + 11], b.get())) {
				return false;
			}

			if (!equals(array[offset + 12], b.get())) {
				return false;
			}

			if (!equals(array[offset + 13], b.get())) {
				return false;
			}

			if (!equals(array[offset + 14], b.get())) {
				return false;
			}

			if (!equals(array[offset + 15], b.get())) {
				return false;
			}

			return true;
		};
	}

	private static RegionComparator comparatorLen17(byte[] array, int offset) {

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(array[offset + 1], b.get())) {
				return false;
			}

			if (!equals(array[offset + 2], b.get())) {
				return false;
			}

			if (!equals(array[offset + 3], b.get())) {
				return false;
			}

			if (!equals(array[offset + 4], b.get())) {
				return false;
			}

			if (!equals(array[offset + 5], b.get())) {
				return false;
			}

			if (!equals(array[offset + 6], b.get())) {
				return false;
			}

			if (!equals(array[offset + 7], b.get())) {
				return false;
			}

			if (!equals(array[offset + 8], b.get())) {
				return false;
			}

			if (!equals(array[offset + 9], b.get())) {
				return false;
			}

			if (!equals(array[offset + 10], b.get())) {
				return false;
			}

			if (!equals(array[offset + 11], b.get())) {
				return false;
			}

			if (!equals(array[offset + 12], b.get())) {
				return false;
			}

			if (!equals(array[offset + 13], b.get())) {
				return false;
			}

			if (!equals(array[offset + 14], b.get())) {
				return false;
			}

			if (!equals(array[offset + 15], b.get())) {
				return false;
			}

			if (!equals(array[offset + 16], b.get())) {
				return false;
			}

			return true;
		};
	}

	private static RegionComparator comparatorLen18(byte[] array, int offset) {

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(array[offset + 1], b.get())) {
				return false;
			}

			if (!equals(array[offset + 2], b.get())) {
				return false;
			}

			if (!equals(array[offset + 3], b.get())) {
				return false;
			}

			if (!equals(array[offset + 4], b.get())) {
				return false;
			}

			if (!equals(array[offset + 5], b.get())) {
				return false;
			}

			if (!equals(array[offset + 6], b.get())) {
				return false;
			}

			if (!equals(array[offset + 7], b.get())) {
				return false;
			}

			if (!equals(array[offset + 8], b.get())) {
				return false;
			}

			if (!equals(array[offset + 9], b.get())) {
				return false;
			}

			if (!equals(array[offset + 10], b.get())) {
				return false;
			}

			if (!equals(array[offset + 11], b.get())) {
				return false;
			}

			if (!equals(array[offset + 12], b.get())) {
				return false;
			}

			if (!equals(array[offset + 13], b.get())) {
				return false;
			}

			if (!equals(array[offset + 14], b.get())) {
				return false;
			}

			if (!equals(array[offset + 15], b.get())) {
				return false;
			}

			if (!equals(array[offset + 16], b.get())) {
				return false;
			}

			if (!equals(array[offset + 17], b.get())) {
				return false;
			}

			return true;
		};
	}

	private static RegionComparator comparatorLen19(byte[] array, int offset) {

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(array[offset + 1], b.get())) {
				return false;
			}

			if (!equals(array[offset + 2], b.get())) {
				return false;
			}

			if (!equals(array[offset + 3], b.get())) {
				return false;
			}

			if (!equals(array[offset + 4], b.get())) {
				return false;
			}

			if (!equals(array[offset + 5], b.get())) {
				return false;
			}

			if (!equals(array[offset + 6], b.get())) {
				return false;
			}

			if (!equals(array[offset + 7], b.get())) {
				return false;
			}

			if (!equals(array[offset + 8], b.get())) {
				return false;
			}

			if (!equals(array[offset + 9], b.get())) {
				return false;
			}

			if (!equals(array[offset + 10], b.get())) {
				return false;
			}

			if (!equals(array[offset + 11], b.get())) {
				return false;
			}

			if (!equals(array[offset + 12], b.get())) {
				return false;
			}

			if (!equals(array[offset + 13], b.get())) {
				return false;
			}

			if (!equals(array[offset + 14], b.get())) {
				return false;
			}

			if (!equals(array[offset + 15], b.get())) {
				return false;
			}

			if (!equals(array[offset + 16], b.get())) {
				return false;
			}

			if (!equals(array[offset + 17], b.get())) {
				return false;
			}

			if (!equals(array[offset + 18], b.get())) {
				return false;
			}

			return true;
		};
	}

	private static RegionComparator comparatorLen20(byte[] array, int offset) {

		return (firstByte, b) -> {
			if (!equals(array[offset], firstByte)) {
				return false;
			}

			if (!equals(array[offset + 1], b.get())) {
				return false;
			}

			if (!equals(array[offset + 2], b.get())) {
				return false;
			}

			if (!equals(array[offset + 3], b.get())) {
				return false;
			}

			if (!equals(array[offset + 4], b.get())) {
				return false;
			}

			if (!equals(array[offset + 5], b.get())) {
				return false;
			}

			if (!equals(array[offset + 6], b.get())) {
				return false;
			}

			if (!equals(array[offset + 7], b.get())) {
				return false;
			}

			if (!equals(array[offset + 8], b.get())) {
				return false;
			}

			if (!equals(array[offset + 9], b.get())) {
				return false;
			}

			if (!equals(array[offset + 10], b.get())) {
				return false;
			}

			if (!equals(array[offset + 11], b.get())) {
				return false;
			}

			if (!equals(array[offset + 12], b.get())) {
				return false;
			}

			if (!equals(array[offset + 13], b.get())) {
				return false;
			}

			if (!equals(array[offset + 14], b.get())) {
				return false;
			}

			if (!equals(array[offset + 15], b.get())) {
				return false;
			}

			if (!equals(array[offset + 16], b.get())) {
				return false;
			}

			if (!equals(array[offset + 17], b.get())) {
				return false;
			}

			if (!equals(array[offset + 18], b.get())) {
				return false;
			}

			if (!equals(array[offset + 19], b.get())) {
				return false;
			}

			return true;
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
