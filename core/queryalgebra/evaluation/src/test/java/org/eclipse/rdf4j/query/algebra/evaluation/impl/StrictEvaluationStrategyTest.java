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
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StrictEvaluationStrategyTest {

	private EvaluationStrategy strategy;

	@BeforeEach
	public void setUp() throws Exception {
		strategy = new StrictEvaluationStrategy(new EmptyTripleSource(), null);
	}

	/**
	 * Verifies if only those input bindings that actually occur in the query are returned in the result. See SES-2373.
	 */
	@Test
	public void testBindings() throws Exception {
		String query = "SELECT ?a ?b WHERE {}";
		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);

		final ValueFactory vf = SimpleValueFactory.getInstance();
		QueryBindingSet constants = new QueryBindingSet();
		constants.addBinding("a", vf.createLiteral("foo"));
		constants.addBinding("b", vf.createLiteral("bar"));
		constants.addBinding("x", vf.createLiteral("X"));
		constants.addBinding("y", vf.createLiteral("Y"));

		CloseableIteration<BindingSet, QueryEvaluationException> result = strategy.evaluate(pq.getTupleExpr(),
				constants);
		assertNotNull(result);
		assertTrue(result.hasNext());
		BindingSet bs = result.next();
		assertTrue(bs.hasBinding("a"));
		assertTrue(bs.hasBinding("b"));
		assertFalse(bs.hasBinding("x"));
		assertFalse(bs.hasBinding("y"));
	}

	@Test
	public void testOptimize() throws Exception {

		QueryOptimizer optimizer1 = mock(QueryOptimizer.class);
		QueryOptimizer optimizer2 = mock(QueryOptimizer.class);

		strategy.setOptimizerPipeline(() -> Arrays.asList(optimizer1, optimizer2));

		TupleExpr expr = mock(TupleExpr.class);
		EvaluationStatistics stats = new EvaluationStatistics();
		BindingSet bindings = new QueryBindingSet();

		strategy.optimize(expr, stats, bindings);
		verify(optimizer1, times(1)).optimize(expr, null, bindings);
		verify(optimizer2, times(1)).optimize(expr, null, bindings);
	}

	@Test
	public void testEvaluateRegexFlags() throws Exception {

		String query = "SELECT ?a WHERE { "
				+ "VALUES ?a { \"foo.bar\" \"foo bar\" } \n"
				+ "FILTER REGEX(str(?a), \"foo.bar\")}";

		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);

		CloseableIteration<BindingSet, QueryEvaluationException> result = strategy.evaluate(pq.getTupleExpr(),
				new EmptyBindingSet());

		List<BindingSet> bindingSets = QueryResults.asList(result);
		assertThat(bindingSets).hasSize(2);

		// match with q flag
		query = "SELECT ?a WHERE { "
				+ "VALUES ?a { \"foo.bar\" \"foo bar\" } \n"
				+ "FILTER REGEX(str(?a), \"foo.bar\", \"q\")}";

		pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);

		result = strategy.evaluate(pq.getTupleExpr(),
				new EmptyBindingSet());

		bindingSets = QueryResults.asList(result);
		assertThat(bindingSets).hasSize(1);
		assertThat(bindingSets.get(0).getValue("a").stringValue()).isEqualTo("foo.bar");

		// match with i and q flag
		query = "SELECT ?a WHERE { "
				+ "VALUES ?a { \"foo.bar\" \"FOO.BAR\" \"foo bar\" } \n"
				+ "FILTER REGEX(str(?a), \"foo.bar\", \"iq\")}";

		pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);

		result = strategy.evaluate(pq.getTupleExpr(),
				new EmptyBindingSet());

		bindingSets = QueryResults.asList(result);
		assertThat(bindingSets).hasSize(2);

		List<String> values = bindingSets.stream().map(v -> v.getValue("a").stringValue()).collect(Collectors.toList());
		assertThat(values).containsExactlyInAnyOrder("foo.bar", "FOO.BAR");
	}

	@Test
	public void testComplex() {
		String query = "PREFIX dcat: <http://www.w3.org/ns/dcat#>\n" +
				"PREFIX foaf:  <http://xmlns.com/foaf/0.1/>\n" +
				"PREFIX dct: <http://purl.org/dc/terms/>\n" +

				"SELECT ?type1 ?type2 ?language ?mbox where {\n" +
				"        ?b dcat:dataset ?a.\n" +

				"        ?b a ?type1." +

				"        ?a a ?type2." +
				"        ?a dct:identifier ?identifier." +
				"        ?a dct:language ?language." +
				"        ?a dct:publisher [foaf:mbox ?mbox] .}";
		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);
		QueryEvaluationStep prepared = strategy.precompile(pq.getTupleExpr());
		assertNotNull(prepared);
	}

	@Test
	public void testNow() {
		String query = "SELECT ?now WHERE {BIND(NOW() AS ?now)}";
		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);
		QueryEvaluationStep prepared = strategy.precompile(pq.getTupleExpr());
		assertNotNull(prepared);
		try (CloseableIteration<BindingSet, QueryEvaluationException> evaluate = prepared
				.evaluate(EmptyBindingSet.getInstance())) {
			assertTrue(evaluate.hasNext());
			BindingSet next = evaluate.next();
			assertNotNull(next);
			Binding nowBound = next.getBinding("now");
			assertNotNull(nowBound);
			assertNotNull(nowBound.getValue());
			Value nowValue = nowBound.getValue();
			assertTrue(nowValue.isLiteral());
			Literal nowLiteral = (Literal) nowValue;
			assertEquals(CoreDatatype.XSD.DATETIME, nowLiteral.getCoreDatatype());
		}
	}

	@Test
	public void testDatetimeCast() {
		String query = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT (xsd:date(\"2022-09-xx\") AS ?date) { }";
		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);
		QueryEvaluationStep prepared = strategy.precompile(pq.getTupleExpr());
		assertNotNull(prepared);
		try (CloseableIteration<BindingSet, QueryEvaluationException> result = prepared
				.evaluate(EmptyBindingSet.getInstance())) {
			assertNotNull(result);
			assertTrue(result.hasNext());
			assertFalse(
					result.next().hasBinding("date"),
					"There should be no binding because the cast should have failed.");
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testSES1991NOWEvaluation() throws Exception {
		String query = "PREFIX ex:<http://example.org> SELECT ?d WHERE {VALUES(?s ?p ?o) {(ex:type rdf:type ex:type)(ex:type ex:type ex:type)} . BIND(NOW() as ?d) } LIMIT 2";
		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);
		QueryEvaluationStep prepared = strategy.precompile(pq.getTupleExpr());

		try (CloseableIteration<BindingSet, QueryEvaluationException> result = prepared
				.evaluate(EmptyBindingSet.getInstance())) {
			assertNotNull(result);
			assertTrue(result.hasNext());

			Literal d1 = (Literal) result.next().getValue("d");
			assertTrue(result.hasNext());
			Literal d2 = (Literal) result.next().getValue("d");
			assertFalse(result.hasNext());
			assertNotNull(d1);
			assertEquals(d1, d2);
			assertTrue(d1 == d2);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSES869ValueOfNow() {

		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL,
				"SELECT ?p ( NOW() as ?n ) { BIND (NOW() as ?p ) }", null);
		QueryEvaluationStep prepared = strategy.precompile(pq.getTupleExpr());

		try (CloseableIteration<BindingSet, QueryEvaluationException> result = prepared
				.evaluate(EmptyBindingSet.getInstance())) {
			assertNotNull(result);
			assertTrue(result.hasNext());

			BindingSet bs = result.next();
			Value p = bs.getValue("p");
			Value n = bs.getValue("n");

			assertNotNull(p);
			assertNotNull(n);
			assertEquals(p, n);
			assertTrue(p == n);
		}
	}

	@Test
	public void testGH4646() {
		ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL,
				"PREFIX  owl:  <http://www.w3.org/2002/07/owl#>\n"
						+ "PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#>\n"
						+ "PREFIX  skos: <http://www.w3.org/2004/02/skos/core#>\n"
						+ "\n"
						+ "SELECT  (?p AS ?resource) (?violates AS ?property) (?c AS ?value)\n"
						+ "WHERE\n"
						+ "  {   { SELECT DISTINCT  ?c ?violates ?p\n"
						+ "        WHERE\n"
						+ "          { GRAPH <http://test.com/inportdata>\n"
						+ "              { ?c  ?p  ?x\n"
						+ "                FILTER ( ?p IN (skos:hasTopConcept, skos:narrower, skos:broader, skos:related, skos:member, skos:broaderTransitive, skos:narrowerTransitive) )\n"
						+ "                OPTIONAL\n"
						+ "                  { ?c  rdf:type  ?cType }\n"
						+ "              }\n"
						+ "            { SELECT  ?p ?domainType\n"
						+ "              WHERE\n"
						+ "                { GRAPH <tmp:validationengine/uni-schema>\n"
						+ "                    {   { ?p (rdfs:domain/(((owl:unionOf/(rdf:rest)*)/rdf:first))*)/^(rdfs:subClassOf)* ?domainType\n"
						+ "                          FILTER isIRI(?domainType)\n"
						+ "                        }\n"
						+ "                      UNION\n"
						+ "                        { ?p (rdfs:subPropertyOf)+ ?parentProperty .\n"
						+ "                          ?parentProperty (rdfs:domain/(((owl:unionOf/(rdf:rest)*)/rdf:first))*)/^(rdfs:subClassOf)* ?domainType\n"
						+ "                          FILTER isIRI(?domainType)\n"
						+ "                        }\n"
						+ "                    }\n"
						+ "                }\n"
						+ "            }\n"
						+ "            BIND(coalesce(sameTerm(?cType, ?domainType), false) AS ?typeMatch)\n"
						+ "            BIND(<urn:domainViolationBy> AS ?violates)\n"
						+ "          }\n"
						+ "        GROUP BY ?c ?p ?violates\n"
						+ "        HAVING ( MAX(?typeMatch) = false )\n"
						+ "      }\n"
						+ "    UNION\n"
						+ "      { SELECT DISTINCT  ?c ?violates ?p\n"
						+ "        WHERE\n"
						+ "          { GRAPH <http://test.com/inportdata>\n"
						+ "              { ?x  ?p  ?c\n"
						+ "                FILTER ( ?p IN (skos:hasTopConcept, skos:narrower, skos:broader, skos:related, skos:member, skos:broaderTransitive, skos:narrowerTransitive) )\n"
						+ "                OPTIONAL\n"
						+ "                  { ?c  rdf:type  ?cType }\n"
						+ "              }\n"
						+ "            { SELECT  ?p ?rangeType\n"
						+ "              WHERE\n"
						+ "                { GRAPH <tmp:validationengine/uni-schema>\n"
						+ "                    {   { ?p (rdfs:range/(((owl:unionOf/(rdf:rest)*)/rdf:first))*)/^(rdfs:subClassOf)* ?rangeType\n"
						+ "                          FILTER isIRI(?rangeType)\n"
						+ "                        }\n"
						+ "                      UNION\n"
						+ "                        { ?p (rdfs:subPropertyOf)+ ?parentProperty .\n"
						+ "                          ?parentProperty (rdfs:range/(((owl:unionOf/(rdf:rest)*)/rdf:first))*)/^(rdfs:subClassOf)* ?rangeType\n"
						+ "                          FILTER isIRI(?rangeType)\n"
						+ "                        }\n"
						+ "                    }\n"
						+ "                }\n"
						+ "            }\n"
						+ "            BIND(coalesce(sameTerm(?cType, ?rangeType), false) AS ?typeMatch)\n"
						+ "            BIND(<urn:rangeViolationBy> AS ?violates)\n"
						+ "          }\n"
						+ "        GROUP BY ?c ?p ?violates\n"
						+ "        HAVING ( MAX(?typeMatch) = false )\n"
						+ "      }\n"
						+ "  }",
				null);
		Model m = new LinkedHashModel();
		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI alignedTest0 = vf.createIRI("http://example.org/", "alignedtest/0");
		IRI import1 = vf.createIRI("http://example.org/", "alignedtest/import_1");
		IRI import2 = vf.createIRI("http://example.org/", "alignedtest/import_2");
		IRI alignedTest1 = vf.createIRI("http://example.org/", "alignedtest/1");
		/**
		 * <http://localhost/alignedtest/0> skos:hasTopConcept <http://localhost/alignedtest/import_1>; a
		 * skos:ConceptScheme.
		 */
		m.add(alignedTest0, SKOS.HAS_TOP_CONCEPT, import1);
		m.add(alignedTest0, RDF.TYPE, SKOS.CONCEPT_SCHEME);
		/**
		 * <http://localhost/alignedtest/1> skos:hasTopConcept <http://localhost/alignedtest/import_1>.
		 */
		m.add(alignedTest1, SKOS.HAS_TOP_CONCEPT,
				import1);
		/**
		 * <http://localhost/alignedtest/import_1> a skos:Concept ; skos:prefLabel "imported concept 1"@en ;
		 * skos:altLabel "imported concept 1"@en ; skos:topConceptOf <http://localhost/alignedtest/0> ; skos:narrower
		 * <http://localhost/alignedtest/import_2> ; skos:topConceptOf <http://localhost/alignedtest/1> .
		 */
		m.add(import1, RDF.TYPE, SKOS.CONCEPT);
		m.add(import1, SKOS.PREF_LABEL, vf.createLiteral("imported concept 1", "en"));
		m.add(import1, SKOS.ALT_LABEL, vf.createLiteral("imported concept 1", "en"));
		m.add(import1, SKOS.TOP_CONCEPT_OF, alignedTest0);
		m.add(import1, SKOS.TOP_CONCEPT_OF, alignedTest1);
		m.add(import1, SKOS.NARROWER, import2);
		/**
		 * <http://localhost/alignedtest/import_2> skos:prefLabel "import concept 2"@en.
		 */
		m.add(import2, SKOS.PREF_LABEL, vf.createLiteral("imported concept 2", "en"));
		TripleSource ts = new ModelTripleSource(m, vf);
		strategy = new StrictEvaluationStrategy(ts, null);
		QueryEvaluationStep prepared = strategy.precompile(pq.getTupleExpr());

		try (CloseableIteration<BindingSet, QueryEvaluationException> result = prepared
				.evaluate(EmptyBindingSet.getInstance())) {
			assertNotNull(result);
			assertFalse(result.hasNext());
		}
	}
}
