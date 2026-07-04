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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ConvertingIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.TripleTerm;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TripleRef;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.NativeTripleTermSource;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test verifies the evaluation of TripleRef nodes in SPARQL 1.2 / RDF 1.2 context with two different triple term
 * storage strategies.
 *
 * <h2>Native Triple Term Storage (nativeSupport=true)</h2>
 * <p>
 * Store implements {@link NativeTripleTermSource} and stores triple terms as first-class values. Triple terms
 * {@code <<( s p o )>>} are stored and queried directly without encoding.
 * </p>
 *
 * <pre>
 * // Native storage: triple term stored as-is
 * store.add(:alice, :claims, <<( :bob :knows :carol )>>)
 *
 * // Queried directly via getTripleTerms()
 * </pre>
 *
 * <h2>Encoded Triple Term Storage (nativeSupport=false)</h2>
 * <p>
 * Store does NOT support native triple terms and uses RDF 1.2 "basic encoding". Each triple term is encoded using a
 * proposition form resource with RDF vocabulary:
 * </p>
 *
 * <pre>
 * // Input triple term:
 * <<( :s :p :o )>>
 *
 * // Encoded as proposition form:
 * _:propForm a rdf:PropositionForm ;
 *   rdf:propositionFormSubject :s ;
 *   rdf:propositionFormPredicate :p ;
 *   rdf:propositionFormObject :o .
 * </pre>
 *
 * <p>
 * When reification is involved (e.g., {@code << :s :p :o >> :q "value"}), a reifier resource links to the proposition
 * form:
 * </p>
 *
 * <pre>
 * // Complete encoding with reifier:
 * _:reifier rdf:reifies _:propForm .
 * _:reifier :q "value" .
 *
 * _:propForm a rdf:PropositionForm ;
 *   rdf:propositionFormSubject :s ;
 *   rdf:propositionFormPredicate :p ;
 *   rdf:propositionFormObject :o .
 * </pre>
 *
 * <h2>What This Test Verifies</h2>
 * <p>
 * This test focuses on TripleRef evaluation with proposition form encoding. The proposition form resource is what gets
 * bound to the TripleRef's extern variable during evaluation when native support is not available.
 * </p>
 *
 * <p>
 * Both storage strategies should produce equivalent query results.
 * </p>
 *
 * @author damyan.ognyanov
 */
public class EvaluationStrategyWithTripleTermsTest {

	/**
	 * Map of proposition form resources to their corresponding triple terms.
	 *
	 * <p>
	 * <b>Native mode (nativeSupport=true):</b><br>
	 * Key = triple term itself (acts as its own identifier)<br>
	 * Value = the same triple term<br>
	 * The triple term is bound directly to the extern variable.
	 * </p>
	 *
	 * <p>
	 * <b>Encoded mode (nativeSupport=false):</b><br>
	 * Key = proposition form resource (e.g., _:propForm or urn:1)<br>
	 * Value = the triple term it encodes ({@code <<( s p o )>>})<br>
	 * The proposition form resource is bound to the extern variable.
	 * </p>
	 */
	private final Map<Resource, TripleTerm> triples = new HashMap<>();

	ValueFactory vf = SimpleValueFactory.getInstance();

	TripleRef tripleRefNode;

	CommonBaseSource baseSource;

	/**
	 * Provides data access for both native and encoded triple term storage modes.
	 *
	 * <p>
	 * <b>Native mode:</b> Returns triple terms directly via {@link #getTripleTerms(Resource, IRI, Value)}
	 * </p>
	 * <p>
	 * <b>Encoded mode:</b> Returns proposition form statements via
	 * {@link #getStatements(Resource, IRI, Value, Resource...)}
	 * </p>
	 *
	 * @author damyan.ognyanov
	 */
	class CommonBaseSource {
		/**
		 * Returns triple terms directly (used when native support is available).
		 *
		 * <p>
		 * This method is called by {@link NativeTripleTermSource#getTripleTerms(Resource, IRI, Value)} when the store
		 * natively supports triple terms as first-class values.
		 * </p>
		 *
		 * @param subj subject filter (not used in this test)
		 * @param pred predicate filter (not used in this test)
		 * @param obj  object filter (not used in this test)
		 * @return iteration over all triple terms
		 */
		public CloseableIteration<? extends TripleTerm> getTripleTerms(Resource subj,
				IRI pred, Value obj)
				throws QueryEvaluationException {
			return new AbstractCloseableIteration<>() {

				final Iterator<TripleTerm> iter = triples.values().iterator();

				@Override
				public boolean hasNext()
						throws QueryEvaluationException {
					return iter.hasNext();
				}

				@Override
				public TripleTerm next()
						throws QueryEvaluationException {
					return iter.next();
				}

				@Override
				public void remove()
						throws QueryEvaluationException {
				}

				@Override
				protected void handleClose() {

				}
			};
		}

		/**
		 * Returns proposition form resources for iteration (used in encoded mode).
		 *
		 * <p>
		 * In encoded mode, proposition forms represent triple terms that cannot be stored natively. This method returns
		 * the proposition form resources that will be queried for their RDF 1.2 encoding statements.
		 * </p>
		 *
		 * @param subj subject filter (the proposition form resource)
		 * @param pred predicate filter (not used)
		 * @param obj  object filter (not used)
		 * @return iteration over proposition form resources
		 */
		public CloseableIteration<? extends Resource> getStatementsInternal(Resource subj,
				IRI pred, Value obj)
				throws QueryEvaluationException {
			return new AbstractCloseableIteration<>() {

				final Iterator<Resource> iter = triples.keySet().iterator();

				@Override
				public boolean hasNext()
						throws QueryEvaluationException {
					return iter.hasNext();
				}

				@Override
				public Resource next()
						throws QueryEvaluationException {
					return iter.next();
				}

				@Override
				public void remove()
						throws QueryEvaluationException {
				}

				@Override
				protected void handleClose() {

				}
			};
		}

		/**
		 * Returns RDF 1.2 proposition form encoding statements (used when native triple term support is NOT available).
		 *
		 * <p>
		 * Implements the RDF 1.2 basic encoding vocabulary for triple terms. When a store cannot natively handle triple
		 * terms {@code <<( s p o )>>}, it encodes them using these RDF statements:
		 * </p>
		 *
		 * <ul>
		 * <li>{@code ?propForm a rdf:PropositionForm} - Identifies proposition form resources</li>
		 * <li>{@code ?propForm rdf:propositionFormSubject ?s} - Encodes the subject component</li>
		 * <li>{@code ?propForm rdf:propositionFormPredicate ?p} - Encodes the predicate component</li>
		 * <li>{@code ?propForm rdf:propositionFormObject ?o} - Encodes the object component</li>
		 * </ul>
		 *
		 * <p>
		 * The evaluation strategy queries these statements to reconstruct triple terms during query evaluation.
		 * </p>
		 *
		 * @param subj     subject filter (the proposition form resource)
		 * @param pred     predicate filter (which encoding property to query)
		 * @param obj      object filter (value to match)
		 * @param contexts context filter (not used)
		 * @return iteration over matching encoding statements
		 */
		public CloseableIteration<? extends Statement> getStatements(Resource subj,
				IRI pred, Value obj, Resource... contexts)
				throws QueryEvaluationException {
			// handle only arguments with reification vocabulary
			// and return iterations accordingly from the same set of TripleTerms

			// handle (?propForm a rdf:PropositionForm)
			if (pred != null && pred.equals(RDF.TYPE) && obj != null && obj.equals(RDF.PROPOSITION_FORM)) {
				return new ConvertingIteration<Resource, Statement>(
						getStatementsInternal(null, null, null)) {
					@Override
					protected Statement convert(Resource sourceObject)
							throws QueryEvaluationException {
						return vf.createStatement(sourceObject, RDF.TYPE, RDF.PROPOSITION_FORM);
					}
				};
			} else if (pred != null && pred.equals(RDF.PROPOSITION_FORM_SUBJECT)) {
				// handle (?propForm rdf:propositionFormSubject ?s)
				return new ConvertingIteration<Resource, Statement>(
						getStatementsInternal(null, null, null)) {
					@Override
					protected Statement convert(Resource sourceObject)
							throws QueryEvaluationException {
						return vf.createStatement(sourceObject, RDF.PROPOSITION_FORM_SUBJECT,
								triples.get(sourceObject).getSubject());
					}
				};
			} else if (pred != null && pred.equals(RDF.PROPOSITION_FORM_PREDICATE)) {
				// handle (?propForm rdf:propositionFormPredicate ?p)
				return new ConvertingIteration<Resource, Statement>(
						getStatementsInternal(null, null, null)) {
					@Override
					protected Statement convert(Resource sourceObject)
							throws QueryEvaluationException {
						return vf.createStatement(sourceObject, RDF.PROPOSITION_FORM_PREDICATE,
								triples.get(sourceObject).getPredicate());
					}
				};
			} else if (pred != null && pred.equals(RDF.PROPOSITION_FORM_OBJECT)) {
				// handle (?propForm rdf:propositionFormObject ?o)
				return new ConvertingIteration<Resource, Statement>(
						getStatementsInternal(null, null, null)) {
					@Override
					protected Statement convert(Resource sourceObject)
							throws QueryEvaluationException {
						return vf.createStatement(sourceObject, RDF.PROPOSITION_FORM_OBJECT,
								triples.get(sourceObject).getObject());
					}
				};
			}
			// DO NOT handle anything else
			return null;
		}

	}

	@BeforeEach
	public void setUp() {
		// Prepare test data
		//
		// Native mode: key = triple term (used as extern var)
		// Encoded mode: key = proposition form resource (used as extern var)
		// Both modes: value = the triple term <<( s p o )>>
		triples.clear();
		triples.put(vf.createIRI("urn:1"),
				vf.createTripleTerm(vf.createIRI("urn:a"), vf.createIRI("urn:p"), vf.createIRI("urn:b")));
		triples.put(vf.createIRI("urn:2"),
				vf.createTripleTerm(vf.createIRI("urn:a"), vf.createIRI("urn:q"), vf.createIRI("urn:b:1")));
		triples.put(vf.createIRI("urn:3"),
				vf.createTripleTerm(vf.createIRI("urn:a:1"), vf.createIRI("urn:p"), vf.createIRI("urn:b")));
		triples.put(vf.createIRI("urn:4"),
				vf.createTripleTerm(vf.createIRI("urn:a:2"), vf.createIRI("urn:q"), vf.createIRI("urn:c")));

		baseSource = new CommonBaseSource();

		tripleRefNode = new TripleRef(new Var("s"), new Var("p"), new Var("o"), new Var("extern"));
	}

	/**
	 * parametrized: either {@link NativeTripleTermSource} or {@link TripleSource}
	 *
	 * @param nativeSupport
	 */
	private TripleSource createSource(boolean nativeSupport) {
		if (nativeSupport) {
			return new NativeTripleTermSource() {

				@Override
				public ValueFactory getValueFactory() {
					return vf;
				}

				@Override
				public CloseableIteration<? extends Statement> getStatements(Resource subj,
						IRI pred, Value obj, Resource... contexts)
						throws QueryEvaluationException {
					return baseSource.getStatements(subj, pred, obj, contexts);
				}

				@Override
				public CloseableIteration<? extends TripleTerm> getTripleTerms(Resource subj,
						IRI pred, Value obj)
						throws QueryEvaluationException {
					return baseSource.getTripleTerms(subj, pred, obj);
				}
			};
		} else {
			return new TripleSource() {
				@Override
				public ValueFactory getValueFactory() {
					return vf;
				}

				@Override
				public CloseableIteration<? extends Statement> getStatements(Resource subj,
						IRI pred, Value obj, Resource... contexts)
						throws QueryEvaluationException {
					return baseSource.getStatements(subj, pred, obj, contexts);
				}
			};
		}
	}

	@ParameterizedTest(name = "nativeSupport={0}")
	@ValueSource(booleans = { false, true })
	public void testMatchAllUnbound(boolean nativeSupport) {
		EvaluationStrategy strategy = new StrictEvaluationStrategy(createSource(nativeSupport), null);

		// Query pattern: << ?s ?p ?o >> (all variables unbound)
		// Should return all triple terms with their components bound
		try (CloseableIteration<BindingSet> iter = strategy.precompile(tripleRefNode)
				.evaluate(
						new EmptyBindingSet())) {
			ArrayList<BindingSet> expected = new ArrayList<>();
			triples.forEach((k, v) -> {
				expected.add(nativeSupport ? fromTriple(v) : fromTriple(k, v));
			});
			ArrayList<BindingSet> received = new ArrayList<>();
			while (iter.hasNext()) {
				received.add(iter.next());
			}
			assertTrue(received.containsAll(expected), "all expected must be received");
			assertTrue(expected.containsAll(received), "all received must be expected");
		}
	}

	@ParameterizedTest(name = "nativeSupport={0}")
	@ValueSource(booleans = { false, true })
	public void testSubjVarBound(boolean nativeSupport) {

		// Query pattern: << urn:a ?p ?o >>
		// Should match triple terms where subject component = urn:a
		EvaluationStrategy strategy = new StrictEvaluationStrategy(createSource(nativeSupport), null);
		try (CloseableIteration<BindingSet> iter = strategy.precompile(tripleRefNode)
				.evaluate(
						createWithVarValue(tripleRefNode.getSubjectVar(), vf.createIRI("urn:a")))) {
			ArrayList<BindingSet> expected = new ArrayList<>();
			triples.forEach((k, v) -> {
				if (v.getSubject().equals(vf.createIRI("urn:a"))) {
					expected.add(nativeSupport ? fromTriple(v) : fromTriple(k, v));
				}
			});
			ArrayList<BindingSet> received = new ArrayList<>();
			while (iter.hasNext()) {
				received.add(iter.next());
			}
			assertTrue(received.containsAll(expected), "all expected must be received");
			assertTrue(expected.containsAll(received), "all received must be expected");
		}
	}

	@ParameterizedTest(name = "nativeSupport={0}")
	@ValueSource(booleans = { false, true })
	public void testPredVarBound(boolean nativeSupport) {

		// Query pattern: << ?s urn:p ?o >>
		// Should match triple terms where predicate component = urn:p
		EvaluationStrategy strategy = new StrictEvaluationStrategy(createSource(nativeSupport), null);
		try (CloseableIteration<BindingSet> iter = strategy.precompile(tripleRefNode)
				.evaluate(
						createWithVarValue(tripleRefNode.getPredicateVar(), vf.createIRI("urn:p")))) {

			ArrayList<BindingSet> expected = new ArrayList<>();
			triples.forEach((k, v) -> {
				if (v.getPredicate().equals(vf.createIRI("urn:p"))) {
					expected.add(nativeSupport ? fromTriple(v) : fromTriple(k, v));
				}
			});
			ArrayList<BindingSet> received = new ArrayList<>();
			while (iter.hasNext()) {
				received.add(iter.next());
			}
			assertTrue(received.containsAll(expected), "all expected must be received");
			assertTrue(expected.containsAll(received), "all received must be expected");
		}
	}

	@ParameterizedTest(name = "nativeSupport={0}")
	@ValueSource(booleans = { false, true })
	public void testObjVarBound(boolean nativeSupport) {

		// Query pattern: << ?s ?p urn:b >>
		// Should match triple terms where object component = urn:b
		EvaluationStrategy strategy = new StrictEvaluationStrategy(createSource(nativeSupport), null);
		try (CloseableIteration<BindingSet> iter = strategy.precompile(tripleRefNode)
				.evaluate(
						createWithVarValue(tripleRefNode.getObjectVar(), vf.createIRI("urn:b")))) {

			ArrayList<BindingSet> expected = new ArrayList<>();
			triples.forEach((k, v) -> {
				if (v.getObject().equals(vf.createIRI("urn:b"))) {
					expected.add(nativeSupport ? fromTriple(v) : fromTriple(k, v));
				}
			});
			ArrayList<BindingSet> received = new ArrayList<>();
			while (iter.hasNext()) {
				received.add(iter.next());
			}
			assertTrue(received.containsAll(expected), "all expected must be received");
			assertTrue(expected.containsAll(received), "all received must be expected");
		}
	}

	@ParameterizedTest(name = "nativeSupport={0}")
	@ValueSource(booleans = { false, true })
	public void testSubjAndObjVarBound(boolean nativeSupport) {

		// Query pattern: << urn:a:2 ?p urn:c >>
		// Should match triple terms where subject = urn:a:2 AND object = urn:c
		QueryBindingSet set = (QueryBindingSet) createWithVarValue(tripleRefNode.getObjectVar(), vf.createIRI("urn:c"));
		set.addBinding(tripleRefNode.getSubjectVar().getName(), vf.createIRI("urn:a:2"));

		EvaluationStrategy strategy = new StrictEvaluationStrategy(createSource(nativeSupport), null);
		try (CloseableIteration<BindingSet> iter = strategy.precompile(tripleRefNode).evaluate(set)) {

			ArrayList<BindingSet> expected = new ArrayList<>();
			triples.forEach((k, t) -> {
				if (t.getObject().equals(vf.createIRI("urn:c")) && t.getSubject().equals(vf.createIRI("urn:a:2"))) {
					expected.add(nativeSupport ? fromTriple(t) : fromTriple(k, t));
				}
			});
			ArrayList<BindingSet> received = new ArrayList<>();
			while (iter.hasNext()) {
				received.add(iter.next());
			}
			assertTrue(received.containsAll(expected), "all expected must be received");
			assertTrue(expected.containsAll(received), "all received must be expected");
		}
	}

	@ParameterizedTest(name = "nativeSupport={0}")
	@ValueSource(booleans = { false, true })
	public void testExtVarBound(boolean nativeSupport) {

		// Test binding the extern variable (exprVar in TripleRef)
		//
		// Native mode: extern = triple term itself
		// Encoded mode: extern = proposition form resource that encodes the triple term
		for (Map.Entry<Resource, TripleTerm> entry : triples.entrySet()) {
			QueryBindingSet set = (QueryBindingSet) createWithVarValue(tripleRefNode.getExprVar(),
					nativeSupport ? entry.getValue() : entry.getKey());

			EvaluationStrategy strategy = new StrictEvaluationStrategy(createSource(nativeSupport), null);
			try (CloseableIteration<BindingSet> iter = strategy.precompile(tripleRefNode).evaluate(set)) {

				ArrayList<BindingSet> expected = new ArrayList<>();
				expected.add(
						nativeSupport ? fromTriple(entry.getValue()) : fromTriple(entry.getKey(), entry.getValue()));
				ArrayList<BindingSet> received = new ArrayList<>();
				while (iter.hasNext()) {
					received.add(iter.next());
				}

				assertThat(received).containsAll(expected);
				assertThat(expected).containsAll(received);
			}
		}
	}

	private BindingSet createWithVarValue(Var var, Value value) {
		QueryBindingSet ret = new QueryBindingSet();
		ret.addBinding(var.getName(), value);
		return ret;
	}

	/**
	 * Creates a binding set for native mode.
	 *
	 * <p>
	 * In native mode, the extern variable is bound to the triple term itself, since triple terms are stored as
	 * first-class values.
	 * </p>
	 *
	 * @param t the triple term
	 * @return binding set with: extern = triple term, s/p/o = components
	 */
	private BindingSet fromTriple(TripleTerm t) {
		QueryBindingSet ret = new QueryBindingSet();
		ret.addBinding("extern", t);
		ret.addBinding("s", t.getSubject());
		ret.addBinding("p", t.getPredicate());
		ret.addBinding("o", t.getObject());
		return ret;
	}

	/**
	 * Creates a binding set for encoded mode.
	 *
	 * <p>
	 * In encoded mode, the extern variable is bound to the proposition form resource that encodes the triple term using
	 * RDF 1.2 basic encoding vocabulary.
	 * </p>
	 *
	 * @param ext the proposition form resource (encodes the triple term via rdf:PropositionForm)
	 * @param t   the triple term it represents
	 * @return binding set with: extern = proposition form, s/p/o = components
	 */
	private BindingSet fromTriple(Resource ext, TripleTerm t) {
		QueryBindingSet ret = new QueryBindingSet();
		ret.addBinding("extern", ext);
		ret.addBinding("s", t.getSubject());
		ret.addBinding("p", t.getPredicate());
		ret.addBinding("o", t.getObject());
		return ret;
	}
}
