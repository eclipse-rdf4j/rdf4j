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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author jeen
 */
public class TestDateTimeCast {

	private DateTimeCast dtCast;

	private final ValueFactory f = SimpleValueFactory.getInstance();

	/**
	 */
	@BeforeEach
	public void setUp() {
		dtCast = new DateTimeCast();
	}

	/**
	 */
	@AfterEach
	public void tearDown() {
	}

	@Test
	public void testCastPlainLiteral() {
		Literal plainLit = f.createLiteral("1999-09-09T00:00:01");
		try {
			Literal result = dtCast.evaluate(f, plainLit);
			assertNotNull(result);
			assertEquals(XSD.DATETIME, result.getDatatype());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testCastDateLiteral() {
		Literal dateLit = f.createLiteral("1999-09-09", XSD.DATE);
		try {
			Literal result = dtCast.evaluate(f, dateLit);
			assertNotNull(result);
			assertEquals(XSD.DATETIME, result.getDatatype());

		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testCastDateTimeLiteral() {
		String lexVal = "2000-01-01T00:00:00";
		Literal dtLit = f.createLiteral(XMLDatatypeUtil.parseCalendar(lexVal));
		try {
			Literal result = dtCast.evaluate(f, dtLit);
			assertNotNull(result);
			assertEquals(XSD.DATETIME, result.getDatatype());
			assertFalse(result.getLanguage().isPresent());
			assertEquals(lexVal, result.getLabel());
		} catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

}
