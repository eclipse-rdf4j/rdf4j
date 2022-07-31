/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.common.io;

import java.util.BitSet;

/**
 * Class providing utility methods for handling byte arrays.
 */
public class ByteArrayUtil {

	/**
	 * Puts the entire <var>source</var> array in the <var>target</var> array at offset <var>offset</var>.
	 *
	 * @param source source array
	 * @param target target array
	 * @param offset non-negative offset
	 */
	public static void put(byte[] source, byte[] target, int offset) {
		System.arraycopy(source, 0, target, offset, source.length);
	}

	/**
	 * Gets the subarray from <var>array</var> that starts at <var>offset</var>.
	 *
	 * @param array  source array
	 * @param offset non-negative offset
	 * @return byte array
	 */
	public static byte[] get(byte[] array, int offset) {
		return get(array, offset, array.length - offset);
	}

	/**
	 * Gets the subarray of length <var>length</var> from <var>array</var> that starts at <var>offset</var>.
	 *
	 * @param array  byte array
	 * @param offset non-negative offset
	 * @param length length
	 * @return byte array
	 */
	public static byte[] get(byte[] array, int offset, int length) {
		byte[] result = new byte[length];
		System.arraycopy(array, offset, result, 0, length);
		return result;
	}

	/**
	 * Put an integer value (padded) in a byte array at a specific offset.
	 *
	 * @param value  integer value
	 * @param array  byte array
	 * @param offset non-negative offset
	 */
	public static void putInt(int value, byte[] array, int offset) {
		array[offset] = (byte) (0xff & (value >>> 24));
		array[offset + 1] = (byte) (0xff & (value >>> 16));
		array[offset + 2] = (byte) (0xff & (value >>> 8));
		array[offset + 3] = (byte) (0xff & value);
	}

	/**
	 * Get an integer value from a byte array at a specific offset.
	 *
	 * @param array  byte array
	 * @param offset non-negative offset
	 * @return integer value
	 */
	public static int getInt(byte[] array, int offset) {
		return ((array[offset] & 0xff) << 24) | ((array[offset + 1] & 0xff) << 16) | ((array[offset + 2] & 0xff) << 8)
				| (array[offset + 3] & 0xff);
	}

	/**
	 * Put a long value (padded) in a byte array at a specific offset.
	 *
	 * @param value  long value
	 * @param array  byte array
	 * @param offset non-negative offset
	 */
	public static void putLong(long value, byte[] array, int offset) {
		array[offset] = (byte) (0xff & (value >>> 56));
		array[offset + 1] = (byte) (0xff & (value >>> 48));
		array[offset + 2] = (byte) (0xff & (value >>> 40));
		array[offset + 3] = (byte) (0xff & (value >>> 32));
		array[offset + 4] = (byte) (0xff & (value >>> 24));
		array[offset + 5] = (byte) (0xff & (value >>> 16));
		array[offset + 6] = (byte) (0xff & (value >>> 8));
		array[offset + 7] = (byte) (0xff & value);
	}

	/**
	 * Get a long value from a byte array at a specific offset.
	 *
	 * @param array  byte array
	 * @param offset offset
	 * @return long value
	 */
	public static long getLong(byte[] array, int offset) {
		return ((long) (array[offset] & 0xff) << 56) | ((long) (array[offset + 1] & 0xff) << 48)
				| ((long) (array[offset + 2] & 0xff) << 40) | ((long) (array[offset + 3] & 0xff) << 32)
				| ((long) (array[offset + 4] & 0xff) << 24) | ((long) (array[offset + 5] & 0xff) << 16)
				| ((long) (array[offset + 6] & 0xff) << 8) | ((long) (array[offset + 7] & 0xff));
	}

	/**
	 * Retrieve a byte from a byte array.
	 *
	 * @param a         the byte array to look in
	 * @param fromIndex the position from which to start looking
	 * @param toIndex   the position up to which to look
	 * @param key       the byte to find
	 * @return the position of the byte in the array, or -1 if the byte was not found in the array
	 */
	public static int find(byte[] a, int fromIndex, int toIndex, byte key) {
		int result = -1;

		if (fromIndex < 0) {
			fromIndex = 0;
		}
		toIndex = Math.min(toIndex, a.length);

		for (int i = fromIndex; fromIndex < toIndex && result == -1 && i < toIndex; i++) {
			if (a[i] == key) {
				result = i;
			}
		}

		return result;
	}

	/**
	 * Look for a sequence of bytes in a byte array.
	 *
	 * @param a         the byte array to look in
	 * @param fromIndex the position from which to start looking
	 * @param toIndex   the position up to which to look
	 * @param key       the bytes to find
	 * @return the position of the bytes in the array, or -1 if the bytes were not found in the array
	 */
	public static int find(byte[] a, int fromIndex, int toIndex, byte[] key) {
		int result = -1;

		int sublen = key.length;
		int maxpos, first, sp = 0;

		maxpos = Math.min(toIndex, a.length) - sublen;

		for (first = fromIndex; sp != sublen && first <= maxpos; first++) {
			first = find(a, first, maxpos, key[0]);

			if ((first < 0) || (first > maxpos)) {
				break;
			}

			for (sp = 1; sp < sublen; sp++) {
				if (a[first + sp] != key[sp]) {
					sp = sublen;
				}
			}
		}

		if (sublen == 0) {
			result = 0;
		} else if (sp == sublen) {
			result = (first - 1);
		}

		return result;
	}

	/**
	 * Checks whether <var>value</var> matches <var>pattern</var> with respect to the bits specified by <var>mask</var>.
	 * In other words: this method returns true if <var>(value[i] ^ pattern[i]) &amp; mask[i] == 0</var> for all i.
	 *
	 * @param value   byte array
	 * @param mask
	 * @param pattern pattern
	 * @return true if pattern was found
	 */
	public static boolean matchesPattern(byte[] value, byte[] mask, byte[] pattern) {
		for (int i = 0; i < value.length; i++) {
			if (((value[i] ^ pattern[i]) & mask[i]) != 0) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Checks whether <var>subValue</var> matches the region in <var>superValue</var> starting at offset
	 * <var>offset</var>.
	 *
	 * @param subValue   value to search for
	 * @param superValue byte array
	 * @param offset     non-negative offset
	 * @return true upon exact match, false otherwise
	 */
	public static boolean regionMatches(byte[] subValue, byte[] superValue, int offset) {
		for (int i = 0; i < subValue.length; i++) {
			if (subValue[i] != superValue[i + offset]) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Compares two regions of bytes, indicating whether one is larger than the other.
	 *
	 * @param array1    The first byte array.
	 * @param startIdx1 The start of the region in the first array.
	 * @param array2    The second byte array.
	 * @param startIdx2 The start of the region in the second array.
	 * @param length    The length of the region that should be compared.
	 * @return A negative number when the first region is smaller than the second, a positive number when the first
	 *         region is larger than the second, or 0 if the regions are equal.
	 */
	public static int compareRegion(byte[] array1, int startIdx1, byte[] array2, int startIdx2, int length) {
		int result = 0;
		for (int i = 0; result == 0 && i < length; i++) {
			result = (array1[startIdx1 + i] & 0xff) - (array2[startIdx2 + i] & 0xff);
		}
		return result;
	}

	/**
	 * Convert a byte array to a vector of bits.
	 *
	 * @param array byte array
	 * @return bitset
	 */
	public static BitSet toBitSet(byte[] array) {
		BitSet bitSet = new BitSet(8 * array.length);

		for (int byteNo = 0; byteNo < array.length; byteNo++) {
			byte b = array[byteNo];

			for (int bitNo = 0; bitNo < 8; bitNo++) {
				if ((b & byteMask(bitNo)) != 0) {
					bitSet.set(8 * byteNo + bitNo);
				}
			}
		}

		return bitSet;
	}

	/**
	 * Convert a bitset to a byte array.
	 *
	 * @param bitSet bitset (should not be null)
	 * @return byte array
	 */
	public static byte[] toByteArray(BitSet bitSet) {
		byte[] array = new byte[bitSet.size() / 8 + 1];

		for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
			array[i / 8] |= byteMask(i);
		}

		return array;
	}

	/**
	 * Create a byte mask, setting bit bitNo to 1 and other bits to 0.
	 *
	 * @param bitNo bit
	 * @return byte mask
	 */
	private static byte byteMask(int bitNo) {
		return (byte) (0x80 >>> (bitNo % 8));
	}

	/**
	 * Returns the hexadecimal value of the supplied byte array. The resulting string always uses two hexadecimals per
	 * byte. As a result, the length of the resulting string is guaranteed to be twice the length of the supplied byte
	 * array.
	 *
	 * @param array byte array
	 * @return hexadecimal string
	 */
	public static String toHexString(byte[] array) {
		StringBuilder sb = new StringBuilder(2 * array.length);

		for (int i = 0; i < array.length; i++) {
			String hex = Integer.toHexString(array[i] & 0xff);

			if (hex.length() == 1) {
				sb.append('0');
			}

			sb.append(hex);
		}

		return sb.toString();
	}
}
