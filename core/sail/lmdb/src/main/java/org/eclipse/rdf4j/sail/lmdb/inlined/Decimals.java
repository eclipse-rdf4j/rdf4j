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
	static final int DOUBLE_EXPONENT_BITS = 9;
	private static final int DOUBLE_EXPONENT_ZERO_OR_SUBNORMAL = 0;
	private static final int DOUBLE_EXPONENT_INF_OR_NAN = (1 << DOUBLE_EXPONENT_BITS) - 1;
	private static final int DOUBLE_EXPONENT_BIAS = (1 << (DOUBLE_EXPONENT_BITS - 1)) - 1;
	private static final int DOUBLE_EXPONENT_MIN_NORMAL = -DOUBLE_EXPONENT_BIAS + 1;
	private static final int DOUBLE_EXPONENT_MAX_NORMAL = DOUBLE_EXPONENT_BIAS;

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
	 * Encodes a double exponent into 9 bits if possible. Handles special cases for zero/subnormal and NaN/Infinity.
	 *
	 * @param exponent11 The original 11-bit exponent.
	 * @return Encoded 9-bit exponent as int (0-511), or -1 if not encodable.
	 */
	public static int encodeExponent9Bits(int exponent11) {
		if (exponent11 == 0x7FF) {
			return DOUBLE_EXPONENT_INF_OR_NAN;
		}

		if (exponent11 == 0) {
			return DOUBLE_EXPONENT_ZERO_OR_SUBNORMAL;
		}

		int unbiasedExp = exponent11 - 1023;
		if (unbiasedExp < DOUBLE_EXPONENT_MIN_NORMAL || unbiasedExp > DOUBLE_EXPONENT_MAX_NORMAL) {
			return -1;
		}
		return unbiasedExp + DOUBLE_EXPONENT_BIAS;
	}

	/**
	 * Decodes a 9-bit exponent back to the original 11-bit exponent.
	 *
	 * @param encoded 9-bit encoded exponent
	 * @return 11-bit biased exponent or special values for reserved patterns
	 */
	public static int decodeExponent9Bits(int encoded) {
		if (encoded == DOUBLE_EXPONENT_ZERO_OR_SUBNORMAL) {
			return 0;
		}
		if (encoded == DOUBLE_EXPONENT_INF_OR_NAN) {
			return 0x7FF;
		}
		int unbiased = encoded - DOUBLE_EXPONENT_BIAS;
		return unbiased + 1023;
	}

	/**
	 * @deprecated Use {@link #decodeExponent9Bits(int)}.
	 */
	@Deprecated
	public static int decodeExponent10Bits(int encoded) {
		return decodeExponent9Bits(encoded);
	}

	static long packDouble(double value) {
		long valueBits = Double.doubleToRawLongBits(value);
		int exponent11 = (int) ((valueBits >>> 52) & 0x7FF);
		int exponent9 = encodeExponent9Bits(exponent11);
		if (exponent9 >= 0) {
			int sign = value < 0 ? 1 : 0;
			long mantissa = valueBits & 0x000fffffffffffffL;
			return ((long) exponent9) << 54 | mantissa << 2 | sign << 1 | 1;
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

	static Literal unpackDouble(long value, ValueFactory valueFactory) {
		if ((value & 1L) == 0) {
			throw new IllegalArgumentException("Invalid packed double value: zero bit not set.");
		}
		int sign = (int) ((value >> 1) & 1);
		long mantissa = (value >> 2) & 0x000fffffffffffffL;
		int exponent9 = (int) ((value >>> 54) & DOUBLE_EXPONENT_INF_OR_NAN);

		int exponent11 = decodeExponent9Bits(exponent9);

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
