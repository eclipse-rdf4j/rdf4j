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
package org.eclipse.rdf4j.query.algebra.evaluation.function.xsd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jeen
 */
public class TestStringCast {

	private StringCast stringCast;

	private final ValueFactory f = SimpleValueFactory.getInstance();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		stringCast = new StringCast();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCastPlainLiteral() {
		Literal plainLit = f.createLiteral("foo");
		try {
			Literal result = stringCast.evaluate(f, plainLit);
			assertNotNull(result);
			assertEquals(XSD.STRING, result.getDatatype());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testCastLangtagLiteral() {
		Literal langLit = f.createLiteral("foo", "en");
		try {
			Literal result = stringCast.evaluate(f, langLit);
			fail("casting of language-tagged literal to xsd:string should result in type error");
		} catch (ValueExprEvaluationException e) {
			// do nothing, expected
		}
	}

	@Test
	public void testCastIntegerLiteral() {
		Literal intLit = f.createLiteral(10);
		try {
			Literal result = stringCast.evaluate(f, intLit);
			assertNotNull(result);
			assertEquals(XSD.STRING, result.getDatatype());
			assertFalse(result.getLanguage().isPresent());
			assertEquals("10", result.getLabel());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testCastDateTimeLiteral() {
		String lexVal = "2000-01-01T00:00:00";
		Literal dtLit = f.createLiteral(XMLDatatypeUtil.parseCalendar(lexVal));
		try {
			Literal result = stringCast.evaluate(f, dtLit);
			assertNotNull(result);
			assertEquals(XSD.STRING, result.getDatatype());
			assertFalse(result.getLanguage().isPresent());
			assertEquals(lexVal, result.getLabel());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testCastUnknownDatatypedLiteral() {
		String lexVal = "foobar";
		Literal dtLit = f.createLiteral(lexVal, f.createIRI("foo:unknownDt"));
		try {
			Literal result = stringCast.evaluate(f, dtLit);
			assertNotNull(result);
			assertEquals(XSD.STRING, result.getDatatype());
			assertFalse(result.getLanguage().isPresent());
			assertEquals(lexVal, result.getLabel());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}
}
