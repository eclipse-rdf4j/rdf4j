/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.datetime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.datetime.Tz;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jeen
 */
public class TzTest {

	private Tz tz;

	private ValueFactory f = SimpleValueFactory.getInstance();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp()
		throws Exception
	{
		tz = new Tz();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown()
		throws Exception
	{
	}

	@Test
	public void testEvaluate1() {
		try {

			Literal result = tz.evaluate(f, f.createLiteral("2011-01-10T14:45:13.815-05:00", XMLSchema.DATETIME));

			assertNotNull(result);
			assertEquals(XMLSchema.STRING, result.getDatatype());

			assertEquals("-05:00", result.getLabel());

		}
		catch (ValueExprEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate2() {
		try {

			Literal result = tz.evaluate(f, f.createLiteral("2011-01-10T14:45:13.815Z", XMLSchema.DATETIME));

			assertNotNull(result);
			assertEquals(XMLSchema.STRING, result.getDatatype());

			assertEquals("Z", result.getLabel());

		}
		catch (ValueExprEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testEvaluate3() {
		try {

			Literal result = tz.evaluate(f, f.createLiteral("2011-01-10T14:45:13.815", XMLSchema.DATETIME));

			assertNotNull(result);
			assertEquals(XMLSchema.STRING, result.getDatatype());

			assertEquals("", result.getLabel());

		}
		catch (ValueExprEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
