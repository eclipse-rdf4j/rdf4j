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

import java.math.BigDecimal;
import java.math.BigInteger;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.sail.lmdb.ValueIds;

public class Decimals {

	static final int DECIMAL_VALUE_BITS = 48;
	static final int DECIMAL_SCALE_BITS = 8;
	static final BigInteger MAX_DECIMAL_VALUE = BigInteger.valueOf((1L << (DECIMAL_VALUE_BITS - 1)) - 1);
	static final BigInteger MIN_DECIMAL_VALUE = BigInteger.valueOf(-(1L << (DECIMAL_VALUE_BITS - 1)));
	static final int MAX_DECIMAL_SCALE = 2 ^ (DECIMAL_SCALE_BITS - 1) - 1;
	static final int MIN_DECIMAL_SCALE = -2 ^ (DECIMAL_SCALE_BITS - 1);

	/**
	 * Encodes a {@link BigDecimal} in 56 bits [48 bits value, 8 bits scale].
	 *
	 * @param value The decimal value
	 * @return Encoded value with type marker
	 */
	static long packDecimal(BigDecimal value) {
		BigInteger unscaled = value.unscaledValue();
		if (unscaled.compareTo(MAX_DECIMAL_VALUE) > 0 || unscaled.compareTo(MIN_DECIMAL_VALUE) < 0) {
			return 0L;
		}
		int scale = value.scale();
		if (scale > MAX_DECIMAL_SCALE || scale < MIN_DECIMAL_SCALE) {
			return 0L;
		}
		long encoded = Integers.encodeZigZag(unscaled.longValue()) << DECIMAL_SCALE_BITS | scale;
		return ValueIds.createId(ValueIds.T_DECIMAL, encoded);
	}

	/**
	 * Extracts the exponent of a double, unbiased, and encodes it into 10 bits if possible. Handles special cases: NaN
	 * and Infinity.
	 *
	 * @param exponent11 The original 11-bit exponent.
	 * @return Encoded 10-bit exponent as int (0-1023), or -1 if not encodable.
	 */
	public static int encodeExponent10Bits(int exponent11) {
		boolean isNaN = exponent11 == 0x7FF && (exponent11 & 0xFFFFFFFFFFFFFL) != 0;
		boolean isInf = exponent11 == 0x7FF && (exponent11 & 0xFFFFFFFFFFFFFL) == 0;

		if (isNaN || isInf) {
			// Reserve special pattern, e.g., 0x3FF (all 10 bits set) for NaN/Inf
			return 0x3FF;
		}

		if (exponent11 == 0) {
			// Subnormal number or zero, exponent = -1022
			return 0; // Use 0 for subnormal/zero
		}

		// Normal number, unbiased exponent in [-1022, 1023]
		int unbiasedExp = exponent11 - 1023;
		int encoded = unbiasedExp + 511; // Shift range to [0, 1023]
		if (encoded < 1 || encoded > 1022) {
			// Out of range for 10 bits (excluding reserved 0 and 0x3FF)
			return -1;
		}
		return encoded;
	}

	/**
	 * Decodes a 10-bit encoded exponent back to unbiased exponent.
	 *
	 * @param encoded 10-bit encoded exponent
	 * @return 11-bit biased exponent or special values for reserved patterns
	 */
	public static int decodeExponent10Bits(int encoded) {
		if (encoded == 0) {
			// Subnormal/zero
			return 0; // -1022;
		}
		if (encoded == 0x3FF) {
			// Reserved for NaN/Inf
			return 0x7FF;
		}
		// Normal
		int unbiased = encoded - 511;
		return unbiased + 1023;
	}

	static long packDouble(double value) {
		long valueBits = Double.doubleToRawLongBits(value);
		// 11-bit exponent
		int exponent11 = (int) ((valueBits >>> 52) & 0x7FF);
		// encode to 10 bits
		int exponent10 = encodeExponent10Bits(exponent11);
		if (exponent10 >= 0) {
			// encoding of exponent was possible
			int sign = value < 0 ? 1 : 0;
			long mantissa = valueBits & 0x000fffffffffffffL;
			long encoded = ((long) exponent10) << 54 | mantissa << 2 | sign << 1 | 1;
			return encoded;
		}
		return 0L;
	}

	static long packFloat(float value) {
		return ValueIds.createId(ValueIds.T_FLOAT, Integers.encodeZigZag(Float.floatToRawIntBits(value)));
	}

	static Literal unpackDecimal(long value, ValueFactory valueFactory) {
		long encoded = ValueIds.getValue(value);
		int scale = (byte) (encoded & 0xFF);
		long unscaled = Integers.decodeZigZag(encoded >>> DECIMAL_SCALE_BITS);
		return valueFactory.createLiteral(new BigDecimal(BigInteger.valueOf(unscaled), scale));
	}

	static boolean isDouble(long value) {
		return (value & 1L) != 0;
	}

	static Literal unpackDouble(long value, ValueFactory valueFactory) {
		if ((value & 1L) == 0) {
			throw new IllegalArgumentException("Invalid packed double value: zero bit not set.");
		}
		int sign = (int) ((value >> 1) & 1);
		long mantissa = (value >> 2) & 0x000fffffffffffffL;
		int exponent10 = (int) (value >>> 54);

		// Decode back to original exponent
		int exponent11 = decodeExponent10Bits(exponent10);

		// Reconstruct raw bits
		long valueBits = ((long) sign << 63) |
				((long) (exponent11 & 0x7FF) << 52) |
				mantissa;

		return valueFactory.createLiteral(Double.longBitsToDouble(valueBits));
	}

	static Literal unpackFloat(long value, ValueFactory valueFactory) {
		float floatValue = Float.intBitsToFloat((int) Integers.decodeZigZag(ValueIds.getValue(value)));
		return valueFactory.createLiteral(floatValue);
	}
}
