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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jeen
 */
public class RandTest {

	private Rand rand;

	private final ValueFactory f = SimpleValueFactory.getInstance();

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		rand = new Rand();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testEvaluate() {
		try {
			Literal random = rand.evaluate(f);

			assertNotNull(random);
			assertEquals(XSD.DOUBLE, random.getDatatype());

			double randomValue = random.doubleValue();

			assertTrue(randomValue >= 0.0d);
			assertTrue(randomValue < 1.0d);
		} catch (ValueExprEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testPrepare() {
		QueryValueEvaluationStep precompile = new StrictEvaluationStrategy(new TripleSource() {

			@Override
			public ValueFactory getValueFactory() {
				return SimpleValueFactory.getInstance();
			}

			@Override
			public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(Resource subj,
					IRI pred, Value obj, Resource... contexts) throws QueryEvaluationException {
				// TODO Auto-generated method stub
				return null;
			}
		}, null).precompile(new FunctionCall(new Rand().getURI()), new QueryEvaluationContext.Minimal(null));
		try {
			Set<Double> previous = new HashSet<>();
			for (int i = 0; i < 100; i++) {
				Value random = precompile.evaluate(EmptyBindingSet.getInstance());

				assertNotNull(random);
				assertTrue(random instanceof Literal);
				Literal randomL = (Literal) random;
				assertEquals(XSD.DOUBLE, randomL.getDatatype());

				double randomValue = randomL.doubleValue();

				assertTrue(randomValue >= 0.0d);
				assertTrue(randomValue < 1.0d);
				assertFalse(previous.contains(randomValue));
				previous.add(randomValue);
			}
		} catch (ValueExprEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
