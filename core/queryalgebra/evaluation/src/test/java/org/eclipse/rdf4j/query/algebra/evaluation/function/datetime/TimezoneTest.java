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
package org.eclipse.rdf4j.query.algebra.evaluation.function.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author jeen
 */
public class TimezoneTest {

	private Timezone timezone;

	private final ValueFactory f = SimpleValueFactory.getInstance();

	/**
	 */
	@BeforeEach
	public void setUp() {
		timezone = new Timezone();
	}

	/**
	 */
	@AfterEach
	public void tearDown() {
	}

	@Test
	public void testEvaluate1() {
		try {

			Literal result = timezone.evaluate(f, f.createLiteral("2011-01-10T14:45:13.815-05:00", XSD.DATETIME));

			assertNotNull(result);
			assertEquals(XSD.DAYTIMEDURATION, result.getDatatype());

			assertEquals("-PT5H", result.getLabel());

		} catch (ValueExprEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate2() {
		try {

			Literal result = timezone.evaluate(f, f.createLiteral("2011-01-10T14:45:13.815Z", XSD.DATETIME));

			assertNotNull(result);
			assertEquals(XSD.DAYTIMEDURATION, result.getDatatype());

			assertEquals("PT0S", result.getLabel());

		} catch (ValueExprEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate3() {
		try {

			timezone.evaluate(f, f.createLiteral("2011-01-10T14:45:13.815", XSD.DATETIME));

			fail("should have resulted in a type error");

		} catch (ValueExprEvaluationException e) {
			// do nothing, expected
		}
	}

}
