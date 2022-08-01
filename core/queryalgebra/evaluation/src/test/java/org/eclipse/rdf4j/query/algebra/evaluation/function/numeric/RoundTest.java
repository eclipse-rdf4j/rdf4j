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
package org.eclipse.rdf4j.query.algebra.evaluation.function.numeric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jeen
 */
public class RoundTest {

	private Round round;

	private final ValueFactory f = SimpleValueFactory.getInstance();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		round = new Round();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testEvaluateDouble() {
		try {
			double dVal = 1.6;
			Literal rounded = round.evaluate(f, f.createLiteral(dVal));

			double roundValue = rounded.doubleValue();

			assertEquals((double) 2.0, roundValue, 0.001d);
		} catch (ValueExprEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluateInt() {
		try {
			int iVal = 1;
			Literal rounded = round.evaluate(f, f.createLiteral(iVal));

			int roundValue = rounded.intValue();

			assertEquals(iVal, roundValue);
		} catch (ValueExprEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluateBigDecimal() {
		try {
			BigDecimal bd = new BigDecimal(1234567.567);

			Literal rounded = round.evaluate(f, f.createLiteral(bd.toPlainString(), XSD.DECIMAL));

			BigDecimal roundValue = rounded.decimalValue();

			assertEquals(new BigDecimal(1234568.0), roundValue);
		} catch (ValueExprEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
