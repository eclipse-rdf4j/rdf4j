/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TripleRef;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.RDFStarTripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * The test verifies the evaluation of TripleRef nodes through evaluation strategy that uses a tripleSource implementing
 * either {@link TripleSource} or {@link RDFStarTripleSource} interfaces
 *
 * @author damyan.ognyanov
 *
 */
@RunWith(Parameterized.class)
public class EvaluationStrategyWithRDFStarTest {

	@Parameters(name = "RDF-star={0}")
	public static Object[] params() {
		return new Object[] { false, true };
	}

	@Parameter
	public boolean bRDFStarData;

	// the triples over which the evaluations is carried
	private final ArrayList<Triple> triples = new ArrayList<>();

	ValueFactory vf = SimpleValueFactory.getInstance();

	TripleRef tripleRefNode;

	EvaluationStrategy strategy;

	CommonBaseSource baseSource;

	/**
	 * this class does it all over a collection of triples but do not IMPLEMENT either TripleSource nor
	 * RDFStarTripleSource The sources for the eval strategies just forward the evaluation to an instance of that
	 *
	 * @author damyan.ognyanov
	 *
	 */
	class CommonBaseSource {
		public CloseableIteration<? extends Triple, QueryEvaluationException> getRdfStarTriples(Resource subj,
				IRI pred, Value obj)
				throws QueryEvaluationException {
			return new AbstractCloseableIteration<Triple, QueryEvaluationException>() {
				final Iterator<Triple> iter = triples.iterator();

				@Override
				public boolean hasNext()
						throws QueryEvaluationException {
					return iter.hasNext();
				}

				@Override
				public Triple next()
						throws QueryEvaluationException {
					return iter.next();
				}

				@Override
				public void remove()
						throws QueryEvaluationException {
				}
			};
		}

		public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(Resource subj,
				IRI pred, Value obj, Resource... contexts)
				throws QueryEvaluationException {
			// handle only arguments with reification vocabulary
			// and return iterations accordingly from the same set of Triples

			// handle (*, rdf:type, rdf:Statement)
			if (pred != null && pred.equals(RDF.TYPE) && obj != null && obj.equals(RDF.STATEMENT)) {
				return new ConvertingIteration<Triple, Statement, QueryEvaluationException>(
						getRdfStarTriples(null, null, null)) {
					@Override
					protected Statement convert(Triple sourceObject)
							throws QueryEvaluationException {
						return vf.createStatement(sourceObject, RDF.TYPE, RDF.STATEMENT);
					}
				};
			} else if (pred != null && pred.equals(RDF.SUBJECT)) {
				// handle (*, rdf:subject, *)
				return new ConvertingIteration<Triple, Statement, QueryEvaluationException>(
						getRdfStarTriples(null, null, null)) {
					@Override
					protected Statement convert(Triple sourceObject)
							throws QueryEvaluationException {
						return vf.createStatement(sourceObject, RDF.SUBJECT, sourceObject.getSubject());
					}
				};
			} else if (pred != null && pred.equals(RDF.PREDICATE)) {
				// handle (*, rdf:predicate, *)
				return new ConvertingIteration<Triple, Statement, QueryEvaluationException>(
						getRdfStarTriples(null, null, null)) {
					@Override
					protected Statement convert(Triple sourceObject)
							throws QueryEvaluationException {
						return vf.createStatement(sourceObject, RDF.PREDICATE, sourceObject.getPredicate());
					}
				};
			} else if (pred != null && pred.equals(RDF.OBJECT)) {
				// handle (*, rdf:object, *)
				return new ConvertingIteration<Triple, Statement, QueryEvaluationException>(
						getRdfStarTriples(null, null, null)) {
					@Override
					protected Statement convert(Triple sourceObject)
							throws QueryEvaluationException {
						return vf.createStatement(sourceObject, RDF.OBJECT, sourceObject.getObject());
					}
				};
			}
			// DO NOT handle anything else
			return null;
		}

	}

	@Before
	public void setUp() throws Exception {
		// prepare data
		triples.clear();
		triples.add(vf.createTriple(vf.createIRI("urn:a"), vf.createIRI("urn:p"), vf.createIRI("urn:b")));
		triples.add(vf.createTriple(vf.createIRI("urn:a"), vf.createIRI("urn:q"), vf.createIRI("urn:b:1")));
		triples.add(vf.createTriple(vf.createIRI("urn:a:1"), vf.createIRI("urn:p"), vf.createIRI("urn:b")));
		triples.add(vf.createTriple(vf.createIRI("urn:a:2"), vf.createIRI("urn:q"), vf.createIRI("urn:c")));

		baseSource = new CommonBaseSource();

		tripleRefNode = new TripleRef(new Var("s"), new Var("p"), new Var("o"), new Var("extern"));

		strategy = new StrictEvaluationStrategy(createSource(), null);
	}

	/**
	 * parametrized: either {@link RDFStarTripleSource} or {@link TripleSource}
	 */
	private TripleSource createSource() {
		if (bRDFStarData) {
			return new RDFStarTripleSource() {

				@Override
				public ValueFactory getValueFactory() {
					return vf;
				}

				@Override
				public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(Resource subj,
						IRI pred, Value obj, Resource... contexts)
						throws QueryEvaluationException {
					return baseSource.getStatements(subj, pred, obj, contexts);
				}

				@Override
				public CloseableIteration<? extends Triple, QueryEvaluationException> getRdfStarTriples(Resource subj,
						IRI pred, Value obj)
						throws QueryEvaluationException {
					return baseSource.getRdfStarTriples(subj, pred, obj);
				}
			};
		} else {
			return new TripleSource() {
				@Override
				public ValueFactory getValueFactory() {
					return vf;
				}

				@Override
				public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(Resource subj,
						IRI pred, Value obj, Resource... contexts)
						throws QueryEvaluationException {
					return baseSource.getStatements(subj, pred, obj, contexts);
				}
			};
		}
	}

	@Test
	public void testMatchAllUnbound() {
		// case check all unbound
		try (CloseableIteration<BindingSet, QueryEvaluationException> iter = strategy.evaluate(tripleRefNode,
				new EmptyBindingSet())) {
			ArrayList<BindingSet> expected = new ArrayList<>();
			triples.forEach(t -> {
				expected.add(fromTriple(t));
			});
			ArrayList<BindingSet> received = new ArrayList<>();
			while (iter.hasNext()) {
				received.add(iter.next());
			}
			assertTrue("all expected must be received", received.containsAll(expected));
			assertTrue("all received must be expected", expected.containsAll(received));
		}
	}

	@Test
	public void testSubjVarBound() {
		try (CloseableIteration<BindingSet, QueryEvaluationException> iter = strategy.evaluate(tripleRefNode,
				createWithVarValue(tripleRefNode.getSubjectVar(), vf.createIRI("urn:a")))) {
			ArrayList<BindingSet> expected = new ArrayList<>();
			triples.forEach(t -> {
				if (t.getSubject().equals(vf.createIRI("urn:a"))) {
					expected.add(fromTriple(t));
				}
			});
			ArrayList<BindingSet> received = new ArrayList<>();
			while (iter.hasNext()) {
				received.add(iter.next());
			}
			assertTrue("all expected must be received", received.containsAll(expected));
			assertTrue("all received must be expected", expected.containsAll(received));
		}
	}

	@Test
	public void testPredVarBound() {
		try (CloseableIteration<BindingSet, QueryEvaluationException> iter = strategy.evaluate(tripleRefNode,
				createWithVarValue(tripleRefNode.getPredicateVar(), vf.createIRI("urn:p")))) {

			ArrayList<BindingSet> expected = new ArrayList<>();
			triples.forEach(t -> {
				if (t.getPredicate().equals(vf.createIRI("urn:p"))) {
					expected.add(fromTriple(t));
				}
			});
			ArrayList<BindingSet> received = new ArrayList<>();
			while (iter.hasNext()) {
				received.add(iter.next());
			}
			assertTrue("all expected must be received", received.containsAll(expected));
			assertTrue("all received must be expected", expected.containsAll(received));
		}
	}

	@Test
	public void testObjVarBound() {
		try (CloseableIteration<BindingSet, QueryEvaluationException> iter = strategy.evaluate(tripleRefNode,
				createWithVarValue(tripleRefNode.getObjectVar(), vf.createIRI("urn:b")))) {

			ArrayList<BindingSet> expected = new ArrayList<>();
			triples.forEach(t -> {
				if (t.getObject().equals(vf.createIRI("urn:b"))) {
					expected.add(fromTriple(t));
				}
			});
			ArrayList<BindingSet> received = new ArrayList<>();
			while (iter.hasNext()) {
				received.add(iter.next());
			}
			assertTrue("all expected must be received", received.containsAll(expected));
			assertTrue("all received must be expected", expected.containsAll(received));
		}
	}

	@Test
	public void testSubjAndObjVarBound() {
		QueryBindingSet set = (QueryBindingSet) createWithVarValue(tripleRefNode.getObjectVar(), vf.createIRI("urn:c"));
		set.addBinding(tripleRefNode.getSubjectVar().getName(), vf.createIRI("urn:a:2"));

		try (CloseableIteration<BindingSet, QueryEvaluationException> iter = strategy.evaluate(tripleRefNode, set)) {

			ArrayList<BindingSet> expected = new ArrayList<>();
			triples.forEach(t -> {
				if (t.getObject().equals(vf.createIRI("urn:c")) && t.getSubject().equals(vf.createIRI("urn:a:2"))) {
					expected.add(fromTriple(t));
				}
			});
			ArrayList<BindingSet> received = new ArrayList<>();
			while (iter.hasNext()) {
				received.add(iter.next());
			}
			assertTrue("all expected must be received", received.containsAll(expected));
			assertTrue("all received must be expected", expected.containsAll(received));
		}
	}

	@Test
	public void testExtVarBound() {
		Triple triple = triples.get(0);
		QueryBindingSet set = (QueryBindingSet) createWithVarValue(tripleRefNode.getExprVar(), triple);

		try (CloseableIteration<BindingSet, QueryEvaluationException> iter = strategy.evaluate(tripleRefNode, set)) {

			ArrayList<BindingSet> expected = new ArrayList<>();
			expected.add(fromTriple(triple));
			ArrayList<BindingSet> received = new ArrayList<>();
			while (iter.hasNext()) {
				received.add(iter.next());
			}

			assertThat(received).containsAll(expected);
			assertThat(expected).containsAll(received);
		}
	}

	private BindingSet createWithVarValue(Var var, Value value) {
		QueryBindingSet ret = new QueryBindingSet();
		ret.addBinding(var.getName(), value);
		return ret;
	}

	private BindingSet fromTriple(Triple t) {
		QueryBindingSet ret = new QueryBindingSet();
		ret.addBinding("extern", t);
		ret.addBinding("s", t.getSubject());
		ret.addBinding("p", t.getPredicate());
		ret.addBinding("o", t.getObject());
		return ret;
	}
}
