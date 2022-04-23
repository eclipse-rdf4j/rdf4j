/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.string;

import static org.junit.Assert.assertEquals;
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
public class ReplaceTest {

	private Replace replaceFunc;

	private final ValueFactory f = SimpleValueFactory.getInstance();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		replaceFunc = new Replace();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testEvaluate1() {

		Literal arg = f.createLiteral("foobar");
		Literal pattern = f.createLiteral("ba");
		Literal replacement = f.createLiteral("Z");

		try {
			Literal result = replaceFunc.evaluate(f, arg, pattern, replacement);

			assertEquals("fooZr", result.getLabel());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate2() {

		Literal arg = f.createLiteral("foobar");
		Literal pattern = f.createLiteral("BA");
		Literal replacement = f.createLiteral("Z");

		try {
			Literal result = replaceFunc.evaluate(f, arg, pattern, replacement);

			assertEquals("foobar", result.getLabel());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate3() {

		Literal arg = f.createLiteral("foobar");
		Literal pattern = f.createLiteral("BA");
		Literal replacement = f.createLiteral("Z");
		Literal flags = f.createLiteral("i");

		try {
			Literal result = replaceFunc.evaluate(f, arg, pattern, replacement, flags);

			assertEquals("fooZr", result.getLabel());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate4() {

		Literal arg = f.createLiteral(10);
		Literal pattern = f.createLiteral("BA");
		Literal replacement = f.createLiteral("Z");

		try {
			Literal result = replaceFunc.evaluate(f, arg, pattern, replacement);

			fail("error expected on incompatible operand");
		} catch (ValueExprEvaluationException e) {
			// do nothing, expected
		}
	}

	@Test
	public void testEvaluate5() {

		Literal arg = f.createLiteral("foobarfoobarbarfoo");
		Literal pattern = f.createLiteral("ba");
		Literal replacement = f.createLiteral("Z");

		try {
			Literal result = replaceFunc.evaluate(f, arg, pattern, replacement);
			assertEquals("fooZrfooZrZrfoo", result.getLabel());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate6() {

		Literal arg = f.createLiteral("foobarfoobarbarfooba");
		Literal pattern = f.createLiteral("ba.");
		Literal replacement = f.createLiteral("Z");

		try {
			Literal result = replaceFunc.evaluate(f, arg, pattern, replacement);
			assertEquals("fooZfooZZfooba", result.getLabel());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate7() {

		Literal arg = f.createLiteral("日本語", "ja");
		Literal pattern = f.createLiteral("[^a-zA-Z0-9]");
		Literal replacement = f.createLiteral("-");

		try {
			Literal result = replaceFunc.evaluate(f, arg, pattern, replacement);
			assertEquals("---", result.getLabel());
			assertEquals("ja", result.getLanguage().orElse(null));
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}
}
