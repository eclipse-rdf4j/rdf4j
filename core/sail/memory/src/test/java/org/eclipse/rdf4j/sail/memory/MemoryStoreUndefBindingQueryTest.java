/**
 * ******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * ******************************************************************************
 */
package org.eclipse.rdf4j.sail.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Reproduces the scenario described in the issue: a GRAPH clause containing an UNDEF binding should not cause a
 * NullPointerException during evaluation when joined with another pattern. The query should evaluate without error and
 * produce one solution binding for mappingProp.
 */
public class MemoryStoreUndefBindingQueryTest {

	private SailRepository repository;

	@BeforeEach
	public void setUp() {
		repository = new SailRepository(new MemoryStore());
		repository.init();
	}

	@AfterEach
	public void tearDown() {
		if (repository != null) {
			repository.shutDown();
		}
	}

	@Test
	public void testGraphBindUndefDoesNotThrowAndBindsMappingProp() {
		try (SailRepositoryConnection conn = repository.getConnection()) {
			SimpleNamespace NS1 = new SimpleNamespace("ex1", "http://example.org/");
			SimpleNamespace NS2 = new SimpleNamespace("ex2", "http://example.org/2/");

			// Add a statement so that the named graph exists
			conn.add(Values.iri(NS1, "A1"), OWL.EQUIVALENTCLASS, Values.iri(NS2, "A2"),
					Values.iri("http://example.org/"));

			String sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
					+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
					+ "SELECT * {\n"
					+ "  ?mappingProp rdfs:subPropertyOf* owl:equivalentClass .\n"
					+ "  GRAPH <http://example.org/> {\n"
					+ "    BIND(?unbound as ?myVar)\n"
					+ "    # Also reproduces with: VALUES(?myVar) { (UNDEF) }\n"
					+ "  }\n"
					+ "}";

			var query = conn.prepareTupleQuery(sparql);

			int count = 0;
			try (var res = query.evaluate()) {
				while (res.hasNext()) {
					BindingSet bs = res.next();
					assertTrue(bs.hasBinding("mappingProp"), "Expected mappingProp binding");
					assertEquals(OWL.EQUIVALENTCLASS, bs.getValue("mappingProp"));
					count++;
				}
			}
			assertEquals(1, count, "Expected exactly one result row");
		}
	}

	@Test
	public void testSubselect() {
		try (SailRepositoryConnection conn = repository.getConnection()) {
			SimpleNamespace NS1 = new SimpleNamespace("ex1", "http://example.org/");
			SimpleNamespace NS2 = new SimpleNamespace("ex2", "http://example.org/2/");

			// Add a statement so that the named graph exists
			conn.add(Values.iri(NS1, "A1"), FOAF.KNOWS, Values.iri(NS2, "A2"),
					Values.iri("http://example.org/"));

			String sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
					+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
					+ "SELECT * WHERE { SELECT * WHERE {\n"
					+ "  ?a ?prop ?b .\n"
					+ "}}";

			var query = conn.prepareTupleQuery(sparql);

			int count = 0;
			try (var res = query.evaluate()) {
				while (res.hasNext()) {
					BindingSet bs = res.next();
					assertFalse(bs.isEmpty(), "Expected non-empty binding set");
					assertTrue(bs.hasBinding("a"), "Expected binding, was: " + bs);
					count++;
				}
			}
			assertEquals(1, count, "Expected exactly one result row");
		}
	}

	@Test
	public void testSubSubselect() {
		try (SailRepositoryConnection conn = repository.getConnection()) {
			SimpleNamespace NS1 = new SimpleNamespace("ex1", "http://example.org/");
			SimpleNamespace NS2 = new SimpleNamespace("ex2", "http://example.org/2/");

			// Add a statement so that the named graph exists
			conn.add(Values.iri(NS1, "A1"), FOAF.KNOWS, Values.iri(NS2, "A2"),
					Values.iri("http://example.org/"));

			String sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
					+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
					+ "SELECT * WHERE { SELECT * WHERE { SELECT * WHERE {\n"
					+ "  ?a ?prop ?b .\n"
					+ "}}}";

			var query = conn.prepareTupleQuery(sparql);

			int count = 0;
			try (var res = query.evaluate()) {
				while (res.hasNext()) {
					BindingSet bs = res.next();
					assertFalse(bs.isEmpty(), "Expected non-empty binding set");
					assertTrue(bs.hasBinding("a"), "Expected binding, was: " + bs);
					count++;
				}
			}
			assertEquals(1, count, "Expected exactly one result row");
		}
	}

	@Test
	public void testSubSubselect2() {
		try (SailRepositoryConnection conn = repository.getConnection()) {
			SimpleNamespace NS1 = new SimpleNamespace("ex1", "http://example.org/");
			SimpleNamespace NS2 = new SimpleNamespace("ex2", "http://example.org/2/");

			// Add a statement so that the named graph exists
			conn.add(Values.iri(NS1, "A1"), FOAF.KNOWS, Values.iri(NS2, "A2"),
					Values.iri("http://example.org/"));

			String sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
					+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
					+ "SELECT * WHERE { BIND(1 as ?one)\n {SELECT * WHERE { BIND(1 as ?one)\n {SELECT * WHERE { SELECT * WHERE {\n"
					+ "  ?a ?prop ?b .\n"
					+ "}}}}}}";

			var query = conn.prepareTupleQuery(sparql);

			int count = 0;
			try (var res = query.evaluate()) {
				while (res.hasNext()) {
					BindingSet bs = res.next();
					assertFalse(bs.isEmpty(), "Expected non-empty binding set");
					assertTrue(bs.hasBinding("a"), "Expected binding, was: " + bs);
					count++;
				}
			}
			assertEquals(1, count, "Expected exactly one result row");
		}
	}

	// Temporary helper for debugging the failing test: dump optimized plan
	@Test
	public void debugExplainSubSubselect() {
		try (SailRepositoryConnection conn = repository.getConnection()) {
			SimpleNamespace NS1 = new SimpleNamespace("ex1", "http://example.org/");
			SimpleNamespace NS2 = new SimpleNamespace("ex2", "http://example.org/2/");

			conn.add(Values.iri(NS1, "A1"), FOAF.KNOWS, Values.iri(NS2, "A2"),
					Values.iri("http://example.org/"));

			String sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
					+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
					+ "SELECT * WHERE { SELECT * WHERE { SELECT * WHERE {\n"
					+ "  ?a ?prop ?b .\n"
					+ "}}}";

			var query = conn.prepareTupleQuery(sparql);
			System.out.println("\n==== Optimized Plan (debug) ====");
			System.out.println(query.explain(org.eclipse.rdf4j.query.explanation.Explanation.Level.Optimized));
			System.out.println("==== Executed Plan (debug) ====");
			System.out.println(query.explain(org.eclipse.rdf4j.query.explanation.Explanation.Level.Executed));

			try (var res = query.evaluate()) {
				System.out.println("==== Results (debug subsubselect) ====");
				while (res.hasNext()) {
					var bs = res.next();
					System.out.println(bs);
				}
			}
		}
	}

	@Test
	public void debugExplainSubselect() {
		try (SailRepositoryConnection conn = repository.getConnection()) {
			SimpleNamespace NS1 = new SimpleNamespace("ex1", "http://example.org/");
			SimpleNamespace NS2 = new SimpleNamespace("ex2", "http://example.org/2/");

			conn.add(Values.iri(NS1, "A1"), FOAF.KNOWS, Values.iri(NS2, "A2"),
					Values.iri("http://example.org/"));

			String sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
					+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
					+ "SELECT * WHERE { SELECT * WHERE {\n"
					+ "  ?a ?prop ?b .\n"
					+ "}}";

			var query = conn.prepareTupleQuery(sparql);
			System.out.println("\n==== Optimized Plan (debug subselect) ====");
			System.out.println(query.explain(org.eclipse.rdf4j.query.explanation.Explanation.Level.Optimized));
			System.out.println("==== Executed Plan (debug subselect) ====");
			System.out.println(query.explain(org.eclipse.rdf4j.query.explanation.Explanation.Level.Executed));

			try (var res = query.evaluate()) {
				while (res.hasNext()) {
					res.next();
				}
			}
		}
	}

	@Test
	public void debugAllVariablesUsedInQueryForSubSubselect() throws Exception {
		String sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
				+ "SELECT * WHERE { SELECT * WHERE { SELECT * WHERE {\n"
				+ "  ?a ?prop ?b .\n"
				+ "}}}";

		var pq = org.eclipse.rdf4j.query.parser.QueryParserUtil
				.parseQuery(org.eclipse.rdf4j.query.QueryLanguage.SPARQL, sparql, null);
		var te = pq.getTupleExpr();
		org.eclipse.rdf4j.query.algebra.QueryRoot root;
		if (te instanceof org.eclipse.rdf4j.query.algebra.QueryRoot) {
			root = (org.eclipse.rdf4j.query.algebra.QueryRoot) te;
		} else {
			root = new org.eclipse.rdf4j.query.algebra.QueryRoot(te);
		}
		String[] all = org.eclipse.rdf4j.query.algebra.evaluation.impl.ArrayBindingBasedQueryEvaluationContext
				.findAllVariablesUsedInQuery(root);
		System.out.println("==== allVariables (debug subsubselect) ====");
		for (String v : all) {
			System.out.println(v);
		}
		System.out.println("==== tuple expr (raw) ====");
		System.out.println(te);
	}
}
