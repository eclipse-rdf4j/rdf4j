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

import java.math.BigInteger;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype.XSD;
import org.eclipse.rdf4j.sail.lmdb.ValueIds;

/**
 * Functions for inlining of values into long ids.
 */
public class Integers {

	static final int INTEGER_VALUE_BITS = 56;
	static final long MAX_INTEGER = (1L << (INTEGER_VALUE_BITS - 1)) - 1;
	static final BigInteger MAX_BIG_INTEGER = BigInteger.valueOf(MAX_INTEGER);
	static final long MIN_INTEGER = -(1L << (INTEGER_VALUE_BITS - 1));
	static final BigInteger MIN_BIG_INTEGER = BigInteger.valueOf(MIN_INTEGER);

	/**
	 * Encode a signed long to ZigZag-encoded long.
	 *
	 * @param value the long value to be encoded
	 * @return the encoded long value
	 */
	static long encodeZigZag(long value) {
		return (value << 1) ^ (value >> 63);
	}

	/**
	 * Decode a ZigZag-encoded long back to signed long.
	 *
	 * @param encoded the encoded long value
	 * @return the original long value with proper sign
	 */
	static long decodeZigZag(long encoded) {
		return (encoded >>> 1) ^ -(encoded & 0x1);
	}

	private static long packInteger(Literal literal, int idType) {
		BigInteger value = literal.integerValue();
		if (value.compareTo(MAX_BIG_INTEGER) > 0 || value.compareTo(MIN_BIG_INTEGER) < 0) {
			return 0L;
		}
		return ValueIds.createId(idType, encodeZigZag(value.longValue()));
	}

	static long packInteger(Literal literal) {
		return packInteger(literal, ValueIds.T_INTEGER);
	}

	static long packLong(Literal literal) {
		long value = literal.longValue();
		if (value > MAX_INTEGER || value < MIN_INTEGER) {
			return 0L;
		}
		return ValueIds.createId(ValueIds.T_LONG, encodeZigZag(value));
	}

	static long packInt(Literal literal) {
		return ValueIds.createId(ValueIds.T_INT, encodeZigZag(literal.intValue()));
	}

	static long packShort(Literal literal) {
		return ValueIds.createId(ValueIds.T_SHORT, encodeZigZag(literal.shortValue()));
	}

	static long packByte(Literal literal) {
		return ValueIds.createId(ValueIds.T_BYTE, 0xFF & literal.byteValue());
	}

	static long packUnsignedLong(Literal literal) {
		long value = Long.parseUnsignedLong(literal.getLabel());
		if (value > MAX_INTEGER || value < MIN_INTEGER) {
			return 0L;
		}
		return ValueIds.createId(ValueIds.T_UNSIGNEDLONG, encodeZigZag(value));
	}

	static long packUnsignedInt(Literal literal) {
		return ValueIds.createId(ValueIds.T_UNSIGNEDINT, encodeZigZag(literal.longValue()));
	}

	static long packUnsignedShort(Literal literal) {
		return ValueIds.createId(ValueIds.T_UNSIGNEDSHORT, encodeZigZag(literal.intValue()));
	}

	static long packUnsignedByte(Literal literal) {
		return ValueIds.createId(ValueIds.T_UNSIGNEDBYTE, literal.intValue());
	}

	static long packPositiveInteger(Literal literal) {
		return packInteger(literal, ValueIds.T_POSITIVE_INTEGER);
	}

	static long packNegativeInteger(Literal literal) {
		return packInteger(literal, ValueIds.T_NEGATIVE_INTEGER);
	}

	static long packNonNegativeInteger(Literal literal) {
		return packInteger(literal, ValueIds.T_NON_NEGATIVE_INTEGER);
	}

	static long packNonPositiveInteger(Literal literal) {
		return packInteger(literal, ValueIds.T_NON_POSITIVE_INTEGER);
	}

	static Literal unpackInteger(long value, ValueFactory valueFactory) {
		long decoded = decodeZigZag(ValueIds.getValue(value));
		return valueFactory.createLiteral(BigInteger.valueOf(decoded));
	}

	static Literal unpackLong(long value, ValueFactory valueFactory) {
		long decoded = decodeZigZag(ValueIds.getValue(value));
		return valueFactory.createLiteral(decoded);
	}

	static Literal unpackInt(long value, ValueFactory valueFactory) {
		long decoded = decodeZigZag(ValueIds.getValue(value));
		return valueFactory.createLiteral((int) decoded);
	}

	static Literal unpackShort(long value, ValueFactory valueFactory) {
		long decoded = decodeZigZag(ValueIds.getValue(value));
		return valueFactory.createLiteral((short) decoded);
	}

	static Literal unpackByte(long value, ValueFactory valueFactory) {
		return valueFactory.createLiteral((byte) ValueIds.getValue(value));
	}

	static Literal unpackUnsignedLong(long value, ValueFactory valueFactory) {
		long decoded = decodeZigZag(ValueIds.getValue(value));
		return valueFactory.createLiteral(Long.toUnsignedString(decoded), XSD.UNSIGNED_LONG);
	}

	static Literal unpackUnsignedInt(long value, ValueFactory valueFactory) {
		long decoded = decodeZigZag(ValueIds.getValue(value));
		return valueFactory.createLiteral(Integer.toUnsignedString((int) decoded), XSD.UNSIGNED_INT);
	}

	static Literal unpackUnsignedShort(long value, ValueFactory valueFactory) {
		long decoded = decodeZigZag(ValueIds.getValue(value));
		return valueFactory.createLiteral(Integer.toUnsignedString((int) decoded), XSD.UNSIGNED_SHORT);
	}

	static Literal unpackUnsignedByte(long value, ValueFactory valueFactory) {
		long decoded = ValueIds.getValue(value);
		return valueFactory.createLiteral(Integer.toUnsignedString((int) decoded), XSD.UNSIGNED_BYTE);
	}

	static Literal unpackPositiveInteger(long value, ValueFactory valueFactory) {
		long decoded = decodeZigZag(ValueIds.getValue(value));
		return valueFactory.createLiteral(Long.toString(decoded), XSD.POSITIVE_INTEGER);
	}

	static Literal unpackNegativeInteger(long value, ValueFactory valueFactory) {
		long decoded = decodeZigZag(ValueIds.getValue(value));
		return valueFactory.createLiteral(Long.toString(decoded), XSD.NEGATIVE_INTEGER);
	}

	static Literal unpackNonNegativeInteger(long value, ValueFactory valueFactory) {
		long decoded = decodeZigZag(ValueIds.getValue(value));
		return valueFactory.createLiteral(Long.toString(decoded), XSD.NON_NEGATIVE_INTEGER);
	}

	static Literal unpackNonPositiveInteger(long value, ValueFactory valueFactory) {
		long decoded = decodeZigZag(ValueIds.getValue(value));
		return valueFactory.createLiteral(Long.toString(decoded), XSD.NON_POSITIVE_INTEGER);
	}

	// ---------------------------------------------------------------------------------------------------------
	// Value-ORDERED (biased/offset-binary) variants: the 56-bit value field holds value + 2^55, so the value
	// fields of two ordered ids compare as plain longs exactly like the numbers they encode. Written only by
	// stores whose store.properties records numeric-id-encoding=ordered-v1; decoded everywhere (ids are
	// self-describing by type code).
	// ---------------------------------------------------------------------------------------------------------

	/** Encode a signed long (within the 56-bit inline range) as its biased, order-preserving form. */
	static long encodeBiased(long value) {
		return value + ValueIds.ORDERED_BIAS;
	}

	/** Decode a biased value field back to the signed long. */
	static long decodeBiased(long encoded) {
		return encoded - ValueIds.ORDERED_BIAS;
	}

	private static long packOrderedBigInteger(Literal literal, int idType) {
		BigInteger value = literal.integerValue();
		if (value.compareTo(MAX_BIG_INTEGER) > 0 || value.compareTo(MIN_BIG_INTEGER) < 0) {
			return 0L;
		}
		return ValueIds.createId(idType, encodeBiased(value.longValue()));
	}

	static long packOrderedInteger(Literal literal) {
		return packOrderedBigInteger(literal, ValueIds.T_ORD_INTEGER);
	}

	static long packOrderedLong(Literal literal) {
		long value = literal.longValue();
		if (value > MAX_INTEGER || value < MIN_INTEGER) {
			return 0L;
		}
		return ValueIds.createId(ValueIds.T_ORD_LONG, encodeBiased(value));
	}

	static long packOrderedInt(Literal literal) {
		return ValueIds.createId(ValueIds.T_ORD_INT, encodeBiased(literal.intValue()));
	}

	static long packOrderedShort(Literal literal) {
		return ValueIds.createId(ValueIds.T_ORD_SHORT, encodeBiased(literal.shortValue()));
	}

	static long packOrderedByte(Literal literal) {
		return ValueIds.createId(ValueIds.T_ORD_BYTE, encodeBiased(literal.byteValue()));
	}

	static long packOrderedPositiveInteger(Literal literal) {
		return packOrderedBigInteger(literal, ValueIds.T_ORD_POSITIVE_INTEGER);
	}

	static long packOrderedNegativeInteger(Literal literal) {
		return packOrderedBigInteger(literal, ValueIds.T_ORD_NEGATIVE_INTEGER);
	}

	static long packOrderedNonNegativeInteger(Literal literal) {
		return packOrderedBigInteger(literal, ValueIds.T_ORD_NON_NEGATIVE_INTEGER);
	}

	static long packOrderedNonPositiveInteger(Literal literal) {
		return packOrderedBigInteger(literal, ValueIds.T_ORD_NON_POSITIVE_INTEGER);
	}

	static Literal unpackOrderedInteger(long value, ValueFactory valueFactory) {
		return valueFactory.createLiteral(BigInteger.valueOf(decodeBiased(ValueIds.getValue(value))));
	}

	static Literal unpackOrderedLong(long value, ValueFactory valueFactory) {
		return valueFactory.createLiteral(decodeBiased(ValueIds.getValue(value)));
	}

	static Literal unpackOrderedInt(long value, ValueFactory valueFactory) {
		return valueFactory.createLiteral((int) decodeBiased(ValueIds.getValue(value)));
	}

	static Literal unpackOrderedShort(long value, ValueFactory valueFactory) {
		return valueFactory.createLiteral((short) decodeBiased(ValueIds.getValue(value)));
	}

	static Literal unpackOrderedByte(long value, ValueFactory valueFactory) {
		return valueFactory.createLiteral((byte) decodeBiased(ValueIds.getValue(value)));
	}

	static Literal unpackOrderedPositiveInteger(long value, ValueFactory valueFactory) {
		return valueFactory.createLiteral(Long.toString(decodeBiased(ValueIds.getValue(value))),
				XSD.POSITIVE_INTEGER);
	}

	static Literal unpackOrderedNegativeInteger(long value, ValueFactory valueFactory) {
		return valueFactory.createLiteral(Long.toString(decodeBiased(ValueIds.getValue(value))),
				XSD.NEGATIVE_INTEGER);
	}

	static Literal unpackOrderedNonNegativeInteger(long value, ValueFactory valueFactory) {
		return valueFactory.createLiteral(Long.toString(decodeBiased(ValueIds.getValue(value))),
				XSD.NON_NEGATIVE_INTEGER);
	}

	static Literal unpackOrderedNonPositiveInteger(long value, ValueFactory valueFactory) {
		return valueFactory.createLiteral(Long.toString(decodeBiased(ValueIds.getValue(value))),
				XSD.NON_POSITIVE_INTEGER);
	}

}
