/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.IntegerCast;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestIntegerCast {

	private IntegerCast ic;

	private ValueFactory f = new ValueFactoryImpl();

	@Before
	public void setUp()
		throws Exception
	{
		this.ic = new IntegerCast();
	}

	@After
	public void tearDown()
		throws Exception
	{
	}

	@Test
	public void testCastDouble() {
		Literal dbl = f.createLiteral(100.01d);
		try {
			Literal result = ic.evaluate(f, dbl);
			assertNotNull(result);
			assertEquals(XMLSchema.INTEGER, result.getDatatype());
			assertEquals(100, result.intValue());
		}
		catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testCastDoubleWithLargeFraction() {
		Literal dbl = f.createLiteral(100.987456d);
		try {
			Literal result = ic.evaluate(f, dbl);
			assertNotNull(result);
			assertEquals(XMLSchema.INTEGER, result.getDatatype());
			assertEquals(100, result.intValue());
		}
		catch (ValueExprEvaluationException e) {
			fail(e.getMessage());
		}
	}
}
