/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import static org.junit.Assert.assertTrue;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jeen
 */
public class ZeroLengthPathIterationTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	private EvaluationStrategy evaluator;

	@Before
	public void setUp() {
		Model m = new LinkedHashModel();
		m.add(RDF.ALT, RDF.TYPE, RDFS.CLASS);
		m.add(RDF.BAG, RDF.TYPE, RDFS.CLASS);

		TripleSource ts = new TripleSource() {

			@Override
			public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(Resource subj,
					IRI pred, Value obj, Resource... contexts) throws QueryEvaluationException {
				return new CloseableIteratorIteration<>(m.getStatements(subj, pred, obj, contexts).iterator());
			}

			@Override
			public ValueFactory getValueFactory() {
				return vf;
			}
		};
		evaluator = new StrictEvaluationStrategy(ts, null);
	}

	/**
	 * Verify that evaluation of a {@link ZeroLengthPathIteration} does not discard input bindings.
	 *
	 * @see <a href="https://github.com/eclipse/rdf4j/issues/689">GH-689</a>
	 */
	@Test
	public void testRetainInputBindings() {

		MapBindingSet bindings = new MapBindingSet();
		bindings.addBinding("a", RDF.FIRST);

		Var subjectVar = new Var("x");
		Var objVar = new Var("y");
		try (ZeroLengthPathIteration zlp = new ZeroLengthPathIteration(evaluator, subjectVar, objVar, null, null, null,
				bindings, new QueryEvaluationContext.Minimal(null))) {
			BindingSet result = zlp.getNextElement();

			assertTrue("zlp evaluation should have retained unrelated input binding", result.hasBinding("a"));
			assertTrue("zlp evaluation should binding for subject var", result.hasBinding("x"));
			assertTrue("zlp evaluation should binding for object var", result.hasBinding("y"));
		}
	}
}
