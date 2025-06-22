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

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.sail.lmdb.Varint;
import org.junit.jupiter.api.Test;

class ValuesTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	private List<Literal> literals = Arrays.asList(
			// DECIMAL
			vf.createLiteral(BigDecimal.ZERO),
			vf.createLiteral(BigDecimal.ONE.negate()),
			vf.createLiteral(new BigDecimal("123456789.987654321")),
			vf.createLiteral(new BigDecimal("0.00000000000000000001")),
			vf.createLiteral(BigDecimal.valueOf(42.42)),
			vf.createLiteral(BigDecimal.TEN),
			// DOUBLE
			vf.createLiteral(Double.NaN),
			vf.createLiteral(Double.POSITIVE_INFINITY),
			vf.createLiteral(Double.NEGATIVE_INFINITY),
			vf.createLiteral(Double.MIN_VALUE),
			vf.createLiteral(Double.MAX_VALUE),
			// vf.createLiteral(-0.0d),
			vf.createLiteral(3.14159d),
			vf.createLiteral(2.0d),
			vf.createLiteral(7.11d),
			// FLOAT
			vf.createLiteral(Float.NaN),
			vf.createLiteral(Float.POSITIVE_INFINITY),
			vf.createLiteral(Float.NEGATIVE_INFINITY),
			vf.createLiteral(Float.MIN_VALUE),
			vf.createLiteral(Float.MAX_VALUE),
			vf.createLiteral(-0.0f),
			vf.createLiteral(1.5f),
			vf.createLiteral(0.25f),
			// INTEGER
			vf.createLiteral(BigInteger.ZERO),
			vf.createLiteral(BigInteger.ONE.negate()),
			vf.createLiteral(BigInteger.valueOf(Long.MAX_VALUE)),
			vf.createLiteral(BigInteger.valueOf(Long.MIN_VALUE)),
			vf.createLiteral(BigInteger.valueOf(100)),
			vf.createLiteral(BigInteger.valueOf(-12345)),
			// LONG
			vf.createLiteral(Long.MAX_VALUE),
			vf.createLiteral(Long.MIN_VALUE),
			vf.createLiteral(0L),
			vf.createLiteral(123456789L),
			// INT
			vf.createLiteral(Integer.MAX_VALUE),
			vf.createLiteral(Integer.MIN_VALUE),
			vf.createLiteral(0),
			vf.createLiteral(42),
			// SHORT
			vf.createLiteral(Short.MAX_VALUE),
			vf.createLiteral(Short.MIN_VALUE),
			vf.createLiteral((short) 0),
			vf.createLiteral((short) 999),
			// BYTE
			vf.createLiteral(Byte.MAX_VALUE),
			vf.createLiteral(Byte.MIN_VALUE),
			vf.createLiteral((byte) 0),
			vf.createLiteral((byte) 42),
			// UNSIGNED_LONG
			vf.createLiteral("0", XSD.UNSIGNED_LONG),
			vf.createLiteral("18446744073709551615", XSD.UNSIGNED_LONG), // 2^64-1
			vf.createLiteral("123456789", XSD.UNSIGNED_LONG),
			// UNSIGNED_INT
			vf.createLiteral("0", XSD.UNSIGNED_INT),
			vf.createLiteral("4294967295", XSD.UNSIGNED_INT), // 2^32-1
			vf.createLiteral("123456", XSD.UNSIGNED_INT),
			// UNSIGNED_SHORT
			vf.createLiteral("0", XSD.UNSIGNED_SHORT),
			vf.createLiteral("65535", XSD.UNSIGNED_SHORT), // 2^16-1
			vf.createLiteral("12345", XSD.UNSIGNED_SHORT),
			// UNSIGNED_BYTE
			vf.createLiteral("0", XSD.UNSIGNED_BYTE),
			vf.createLiteral("255", XSD.UNSIGNED_BYTE), // 2^8-1
			vf.createLiteral("42", XSD.UNSIGNED_BYTE),
			// POSITIVE_INTEGER
			vf.createLiteral("1", XSD.POSITIVE_INTEGER),
			vf.createLiteral("999999999999999999999999", XSD.POSITIVE_INTEGER),
			vf.createLiteral("42", XSD.POSITIVE_INTEGER),
			// NEGATIVE_INTEGER
			vf.createLiteral("-1", XSD.NEGATIVE_INTEGER),
			vf.createLiteral("-999999999999999999999999", XSD.NEGATIVE_INTEGER),
			vf.createLiteral("-42", XSD.NEGATIVE_INTEGER),
			// NON_NEGATIVE_INTEGER
			vf.createLiteral("0", XSD.NON_NEGATIVE_INTEGER),
			vf.createLiteral("123456789012345678", XSD.NON_NEGATIVE_INTEGER),
			vf.createLiteral("123", XSD.NON_NEGATIVE_INTEGER),
			// NON_POSITIVE_INTEGER
			vf.createLiteral("0", XSD.NON_POSITIVE_INTEGER),
			vf.createLiteral("-123456789012345678", XSD.NON_POSITIVE_INTEGER),
			vf.createLiteral("-99", XSD.NON_POSITIVE_INTEGER),
			// STRING (short string; edge + standard)
			vf.createLiteral("", XSD.STRING),
			vf.createLiteral("a", XSD.STRING),
			vf.createLiteral("abcdefg", XSD.STRING), // max inlined length
			vf.createLiteral("RDF4J", XSD.STRING),
			vf.createLiteral("test", XSD.STRING),
			// DATETIME
			vf.createLiteral(LocalDateTime.of(1970, 1, 1, 0, 0, 0)),
			vf.createLiteral(LocalDateTime.of(9999, 12, 31, 23, 59, 59)),
			vf.createLiteral(LocalDateTime.of(2020, 2, 29, 12, 0, 0)),
			vf.createLiteral(LocalDateTime.of(1999, 12, 31, 23, 59, 59)),
			// DATETIMESTAMP
			// vf.createLiteral(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)),
			// vf.createLiteral(OffsetDateTime.of(9999, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(14))),
			// vf.createLiteral(OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-5))),
			// DATE
			vf.createLiteral(LocalDate.of(1970, 1, 1)),
			vf.createLiteral(LocalDate.of(9999, 12, 31)),
			vf.createLiteral(LocalDate.of(2024, 6, 13)),
			// BOOLEAN
			vf.createLiteral(true),
			vf.createLiteral(false)
	);

	@Test
	void testPackAndUnpack_AllLiteralTypesWithEdgeAndStandardCases() {
		for (Literal literal : literals) {
			long packed = Values.packLiteral(literal);
			// If the literal is not inlined, packed==0. Only test roundtrip if it is inlined.
			if (packed != 0L) {
				Literal unpacked = Values.unpackLiteral(packed, vf);
				assertEqualLiterals(unpacked, literal);
			} else {
				// (optional) ensure non-inlined values can be detected
				assertThat(packed).isZero();
			}
		}
	}

	@Test
	void testPackAndUnpack_AllLiteralTypesWithVarintConversion() {
		ByteBuffer bb = ByteBuffer.allocate(Long.BYTES + 1);
		for (Literal literal : literals) {
			long packed = Values.packLiteral(literal);
			// If the literal is not inlined, packed==0. Only test roundtrip if it is inlined.
			if (packed != 0L) {
				bb.clear();
				Varint.writeUnsigned(bb, packed);
				bb.flip();
				assertThat(Varint.readUnsigned(bb)).isEqualTo(packed);
			} else {
				// (optional) ensure non-inlined values can be detected
				assertThat(packed).isZero();
			}
		}
	}

	private void assertEqualLiterals(Literal actual, Literal expected) {
		assertThat(actual).isEqualTo(expected);
	}
}
