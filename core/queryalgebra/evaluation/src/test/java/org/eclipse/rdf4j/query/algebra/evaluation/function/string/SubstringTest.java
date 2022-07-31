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
package org.eclipse.rdf4j.query.algebra.evaluation.function.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jeen
 */
public class SubstringTest {

	private Substring substrFunc;

	private final ValueFactory f = SimpleValueFactory.getInstance();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		substrFunc = new Substring();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testEvaluate1() {

		Literal pattern = f.createLiteral("foobar");
		Literal startIndex = f.createLiteral(4);

		try {
			Literal result = substrFunc.evaluate(f, pattern, startIndex);

			assertTrue(result.getLabel().equals("bar"));
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate2() {

		Literal pattern = f.createLiteral("foobar");
		Literal startIndex = f.createLiteral(4);
		Literal length = f.createLiteral(2);

		try {
			Literal result = substrFunc.evaluate(f, pattern, startIndex, length);

			assertTrue(result.getLabel().equals("ba"));
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate3() {

		Literal pattern = f.createLiteral("foobar");
		Literal startIndex = f.createLiteral(4);
		Literal length = f.createLiteral(5);

		assertEquals("bar", substrFunc.evaluate(f, pattern, startIndex, length).getLabel());
	}

	@Test
	public void testEvaluate4() {

		Literal pattern = f.createLiteral("foobar");

		try {
			substrFunc.evaluate(f, pattern);
			fail("illegal number of args hould have resulted in error");
		} catch (ValueExprEvaluationException e) {
			// do nothing, expected
		}
	}

	@Test
	public void testEvaluateStartBefore1() {
		Literal pattern = f.createLiteral("ABC");
		Literal startIndex = f.createLiteral(0);
		Literal length = f.createLiteral(1);
		try {
			Literal result = substrFunc.evaluate(f, pattern, startIndex, length);

			assertTrue(result.getLabel().isEmpty());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testXpathExamples1() {
		Literal pattern = f.createLiteral("motor car");
		Literal startIndex = f.createLiteral(6);
		Literal result = substrFunc.evaluate(f, pattern, startIndex);
		assertEquals(" car", result.getLabel());
	}

	@Test
	public void testXpathExamples2() {
		Literal pattern = f.createLiteral("metadata");
		Literal startIndex = f.createLiteral(4);
		Literal length = f.createLiteral(3);
		Literal result = substrFunc.evaluate(f, pattern, startIndex, length);
		assertEquals("ada", result.getLabel());
	}

	@Test
	public void testXpathExamples3() {
		Literal pattern = f.createLiteral("12345");
		Literal startIndex = f.createLiteral(1.5);
		Literal length = f.createLiteral(2.6);
		try {
			Literal result = substrFunc.evaluate(f, pattern, startIndex, length);
			fail("illegal use of float args hould have resulted in error");
		} catch (ValueExprEvaluationException e) {
			// do nothing, expected
			// this is unlike the xpath standard as sparql only allows int input
		}
	}

	@Test
	public void testXpathExamples3int() {
		Literal pattern = f.createLiteral("12345");
		Literal startIndex = f.createLiteral(2);
		Literal length = f.createLiteral(3);
		Literal result = substrFunc.evaluate(f, pattern, startIndex, length);
		assertEquals("234", result.getLabel());
	}

	@Test
	public void testXpathExamples4() {
		Literal pattern = f.createLiteral("12345");
		Literal startIndex = f.createLiteral(0);
		Literal length = f.createLiteral(3);
		Literal result = substrFunc.evaluate(f, pattern, startIndex, length);
		assertEquals("12", result.getLabel());
	}

	@Test
	public void testXpathExamples5() {
		Literal pattern = f.createLiteral("12345");
		Literal startIndex = f.createLiteral(5);
		Literal length = f.createLiteral(-3);
		Literal result = substrFunc.evaluate(f, pattern, startIndex, length);
		assertEquals("", result.getLabel());
	}

	@Test
	public void testXpathExamples6() {
		Literal pattern = f.createLiteral("12345");
		Literal startIndex = f.createLiteral(-3);
		Literal length = f.createLiteral(5);
		Literal result = substrFunc.evaluate(f, pattern, startIndex, length);
		assertEquals("1", result.getLabel());
	}

	@Test
	public void testXpathExamples7() {
		Literal pattern = f.createLiteral("12345");
		Literal startIndex = f.createLiteral(1);
		Literal length = f.createLiteral(Float.NaN);
		try {
			Literal result = substrFunc.evaluate(f, pattern, startIndex, length);
		} catch (ValueExprEvaluationException e) {
			// do nothing, expected
			// this is unlike the xpath standard as sparql only allows int input
		}
	}

	@Test
	public void testXpathExample8Inspired() {
		Literal pattern = f.createLiteral("12345");
		Literal startIndex = f.createLiteral(-42);
		// This test was inspired by the xpath test cases.
		// However there is no Integer infinite value in java that
		// could be used
		Literal length = f.createLiteral(50);
		Literal result = substrFunc.evaluate(f, pattern, startIndex, length);
		assertEquals("12345", result.getLabel());
	}

	@Test
	public void testXpathExamples9() {
		Literal pattern = f.createLiteral("12345");
		Literal startIndex = f.createLiteral(Float.NEGATIVE_INFINITY);
		Literal length = f.createLiteral(Float.POSITIVE_INFINITY);
		try {
			Literal result = substrFunc.evaluate(f, pattern, startIndex, length);
			fail("illegal use of float args hould have resulted in error");
		} catch (ValueExprEvaluationException e) {
			// do nothing, expected
		}
	}
}
