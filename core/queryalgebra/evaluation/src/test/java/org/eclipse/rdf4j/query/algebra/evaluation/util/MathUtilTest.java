/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.MathExpr.MathOp;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.junit.jupiter.api.Test;

/**
 * @author Jerven Bolleman
 */
public class MathUtilTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	public void testCompute() {
		Literal float1 = vf.createLiteral("12", XSD.INTEGER);
		Literal float2 = vf.createLiteral("2", CoreDatatype.XSD.INTEGER);

		assertComputeEquals(vf.createLiteral("14", XSD.INTEGER), float1, float2, MathOp.PLUS);
		assertComputeEquals(vf.createLiteral("10", CoreDatatype.XSD.INTEGER), float1, float2, MathOp.MINUS);
		assertComputeEquals(vf.createLiteral("24", XSD.INTEGER), float1, float2, MathOp.MULTIPLY);
		assertComputeEquals(vf.createLiteral("6", CoreDatatype.XSD.DECIMAL), float1, float2, MathOp.DIVIDE);
	}

	/**
	 * @link https://www.w3.org/TR/xpath-functions/#func-numeric-divide
	 */
	@Test
	public void testDivideByZero() {
		Literal in1 = vf.createLiteral("12", XSD.INTEGER);
		divideByZero(in1, CoreDatatype.XSD.INTEGER);

		Literal dec1 = vf.createLiteral("12", XSD.DECIMAL);
		divideByZero(dec1, CoreDatatype.XSD.DECIMAL);

		Literal float1 = vf.createLiteral("12", XSD.FLOAT);
		assertTrue(Float.isInfinite(divideByZeroFloat(float1, CoreDatatype.XSD.FLOAT).floatValue()));
		assertTrue(
				Float.isNaN(divideByZeroFloat(vf.createLiteral("0", XSD.FLOAT), CoreDatatype.XSD.FLOAT).floatValue()));

		Literal double1 = vf.createLiteral("12", XSD.DOUBLE);
		assertTrue(Double.isInfinite(divideByZeroFloat(double1, CoreDatatype.XSD.DOUBLE).doubleValue()));

		Literal double1neg = vf.createLiteral("-12", XSD.DOUBLE);
		assertTrue(divideByZeroFloat(double1neg, CoreDatatype.XSD.DOUBLE).doubleValue() == Double.NEGATIVE_INFINITY);
		assertTrue(Double
				.isNaN(divideByZeroFloat(vf.createLiteral("0", XSD.DOUBLE), CoreDatatype.XSD.DOUBLE).doubleValue()));

	}

	private void divideByZero(Literal float1, CoreDatatype.XSD zeroDatatype) {
		try {
			Literal float2 = vf.createLiteral("0", zeroDatatype);
			MathUtil.compute(float1, float2, MathOp.DIVIDE);
			fail();
		} catch (ValueExprEvaluationException e) {
			// Expected
		} catch (Exception e) {
			fail();
		}
	}

	private Literal divideByZeroFloat(Literal float1, CoreDatatype.XSD zeroDatatype) {
		try {
			Literal float2 = vf.createLiteral("0", zeroDatatype);
			return MathUtil.compute(float1, float2, MathOp.DIVIDE);
			// Expected
		} catch (ValueExprEvaluationException e) {
			fail();
		} catch (Exception e) {
			fail();
		}
		return null;
	}

	private void assertComputeEquals(Literal result, Literal lit1, Literal lit2, MathOp op) {
		assertEquals(result, MathUtil.compute(lit1, lit2, op));
	}
}
