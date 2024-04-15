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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

public class DecimalsTest {

	@Test
	void testPackDecimalValid() {
		BigDecimal value = new BigDecimal(BigInteger.valueOf(123456), 2);
		long packedValue = Decimals.packDecimal(value);
		assertNotEquals(0L, packedValue);
	}

	@Test
	void testPackDecimalInvalidScale() {
		BigDecimal value = new BigDecimal(BigInteger.valueOf(123456), Decimals.MAX_DECIMAL_SCALE + 1);
		long packedValue = Decimals.packDecimal(value);
		assertEquals(0L, packedValue);
	}

	@Test
	void testPackDecimalInvalidValue() {
		BigDecimal value = new BigDecimal(BigInteger.valueOf(Decimals.MAX_DECIMAL_VALUE.longValue() + 1), 2);
		long packedValue = Decimals.packDecimal(value);
		assertEquals(0L, packedValue);
	}

	@Test
	void testUnpackDecimal() {
		BigDecimal value = new BigDecimal(BigInteger.valueOf(123456), 2);
		long packedValue = Decimals.packDecimal(value);
		Literal literal = Decimals.unpackDecimal(packedValue, SimpleValueFactory.getInstance());
		assertEquals(value, literal.decimalValue());
	}

	@Test
	void testPackDouble() {
		double[] values = {
				123.456, // typical positive
				3.14, // small positive
				-123.456, // typical negative
				-3.14, // small negative
				0, // positive zero
				-0.0, // negative zero
				1, // simple positive
				-1, // simple negative
				Double.NaN, // not-a-number
				Double.POSITIVE_INFINITY, // positive infinity
				Double.NEGATIVE_INFINITY // negative infinity
		};

		for (double value : values) {
			long packedValue = Decimals.packDouble(value);
			assertNotEquals(0L, packedValue, "Packing failed for value: " + value);
			Literal literal = Decimals.unpackDouble(packedValue, SimpleValueFactory.getInstance());
			if (Double.isNaN(value)) {
				assertTrue(Double.isNaN(literal.doubleValue()), "Expected NaN but got: " + literal.doubleValue());
			} else {
				assertEquals(value, literal.doubleValue(), 0.0, "Mismatch for value: " + value);
			}
		}
	}

	@Test
	void testPackFloat() {
		float[] values = {
				123.456f, // typical positive
				3.14f, // small positive
				-123.456f, // typical negative
				-3.14f, // small negative
				0f, // positive zero
				-0.0f, // negative zero
				1f, // simple positive
				-1f, // simple negative
				Float.NaN, // not-a-number
				Float.POSITIVE_INFINITY, // positive infinity
				Float.NEGATIVE_INFINITY // negative infinity
		};

		for (float value : values) {
			long packedValue = Decimals.packFloat(value);
			assertNotEquals(0L, packedValue, "Packing failed for value: " + value);
			Literal literal = Decimals.unpackFloat(packedValue, SimpleValueFactory.getInstance());
			if (Float.isNaN(value)) {
				assertTrue(Double.isNaN(literal.floatValue()), "Expected NaN but got: " + literal.floatValue());
			} else {
				assertEquals(value, literal.floatValue(), 0.0, "Mismatch for value: " + value);
			}
		}
	}
}