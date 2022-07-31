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
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jeen
 */
public class StrAfterTest {

	private StrAfter strAfterFunc;

	private final ValueFactory f = SimpleValueFactory.getInstance();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		strAfterFunc = new StrAfter();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testEvaluate1() {

		Literal leftArg = f.createLiteral("foobar");
		Literal rightArg = f.createLiteral("ba");

		try {
			Literal result = strAfterFunc.evaluate(f, leftArg, rightArg);

			assertEquals("r", result.getLabel());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate2() {

		Literal leftArg = f.createLiteral("foobar");
		Literal rightArg = f.createLiteral("xyz");

		try {
			Literal result = strAfterFunc.evaluate(f, leftArg, rightArg);

			assertEquals("", result.getLabel());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate3() {

		Literal leftArg = f.createLiteral("foobar", "en");
		Literal rightArg = f.createLiteral("b");

		try {
			Literal result = strAfterFunc.evaluate(f, leftArg, rightArg);

			assertEquals("ar", result.getLabel());
			assertEquals("en", result.getLanguage().orElse(null));
			assertEquals(RDF.LANGSTRING, result.getDatatype());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate4() {

		Literal leftArg = f.createLiteral("foobar", XSD.STRING);
		Literal rightArg = f.createLiteral("b");

		try {
			Literal result = strAfterFunc.evaluate(f, leftArg, rightArg);

			assertEquals("ar", result.getLabel());
			assertEquals(XSD.STRING, result.getDatatype());

		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate4a() {

		Literal leftArg = f.createLiteral("foobar");
		Literal rightArg = f.createLiteral("b", XSD.STRING);

		try {
			Literal result = strAfterFunc.evaluate(f, leftArg, rightArg);

			assertEquals("ar", result.getLabel());
			assertEquals(XSD.STRING, result.getDatatype());

		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate5() {

		Literal leftArg = f.createLiteral("foobar", XSD.STRING);
		Literal rightArg = f.createLiteral("b", XSD.DATE);

		try {
			Literal result = strAfterFunc.evaluate(f, leftArg, rightArg);

			fail("operand with incompatible datatype, should have resulted in error");
		} catch (ValueExprEvaluationException e) {
			assertEquals(
					"incompatible operands for STRAFTER: \"foobar\", \"b\"^^<http://www.w3.org/2001/XMLSchema#date>",
					e.getMessage());
		}
	}

	@Test
	public void testEvaluate6() {

		Literal leftArg = f.createLiteral(10);
		Literal rightArg = f.createLiteral("b");

		try {
			Literal result = strAfterFunc.evaluate(f, leftArg, rightArg);

			fail("operand with incompatible datatype, should have resulted in error");
		} catch (ValueExprEvaluationException e) {
			assertEquals("incompatible operands for STRAFTER: \"10\"^^<http://www.w3.org/2001/XMLSchema#int>, \"b\"",
					e.getMessage());
		}
	}

	@Test
	public void testEvaluate7() {

		IRI leftArg = f.createIRI("http://example.org/foobar");
		Literal rightArg = f.createLiteral("b");

		try {
			Literal result = strAfterFunc.evaluate(f, leftArg, rightArg);

			fail("operand of incompatible type, should have resulted in error");
		} catch (ValueExprEvaluationException e) {
			assertEquals("incompatible operands for STRAFTER: http://example.org/foobar, \"b\"", e.getMessage());
		}
	}

	@Test
	public void testEvaluate8() {
		Literal leftArg = f.createLiteral("foobar", "en");
		Literal rightArg = f.createLiteral("b", "nl");

		try {
			Literal result = strAfterFunc.evaluate(f, leftArg, rightArg);

			fail("operand of incompatible type, should have resulted in error");
		} catch (ValueExprEvaluationException e) {
			assertEquals("incompatible operands for STRAFTER: \"foobar\"@en, \"b\"@nl", e.getMessage());
		}
	}

	@Test
	public void testEvaluate9() {
		Literal leftArg = f.createLiteral("foobar");
		Literal rightArg = f.createLiteral("b", "nl");

		try {
			Literal result = strAfterFunc.evaluate(f, leftArg, rightArg);

			fail("operand of incompatible type, should have resulted in error");
		} catch (ValueExprEvaluationException e) {
			assertEquals("incompatible operands for STRAFTER: \"foobar\", \"b\"@nl", e.getMessage());
		}
	}

	@Test
	public void testEvaluate10() {
		Literal leftArg = f.createLiteral("foobar", "en");
		Literal rightArg = f.createLiteral("b", XSD.STRING);

		try {
			Literal result = strAfterFunc.evaluate(f, leftArg, rightArg);

			assertEquals("ar", result.getLabel());
			assertEquals(RDF.LANGSTRING, result.getDatatype());
			assertEquals("en", result.getLanguage().orElse(null));

		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate11() {
		Literal leftArg = f.createLiteral("foobar", "nl");
		Literal rightArg = f.createLiteral("b", "nl");

		try {
			Literal result = strAfterFunc.evaluate(f, leftArg, rightArg);

			assertEquals("ar", result.getLabel());
			assertEquals(RDF.LANGSTRING, result.getDatatype());
			assertEquals("nl", result.getLanguage().orElse(null));

		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

}
