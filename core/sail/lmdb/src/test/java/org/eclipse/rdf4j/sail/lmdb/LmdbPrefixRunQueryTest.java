/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Query-level tests for prefix-run evaluation of distinct projections and distinct counts over single statement
 * patterns.
 */
public class LmdbPrefixRunQueryTest {

	private static final String EX = "http://example.com/";

	@TempDir
	File dataDir;

	private SailRepository repository;

	@AfterEach
	public void tearDown() {
		System.clearProperty(LmdbPrefixRunPlan.ENABLED_PROPERTY);
		if (repository != null) {
			repository.shutDown();
		}
	}

	@Test
	public void distinctPredicateUsesPrefixRun() {
		openRepository("spoc,posc,ospc");

		long before = LmdbPrefixRunPlan.OPENED.get();
		assertThat(values("SELECT DISTINCT ?p WHERE { ?s ?p ?o }", "p")).containsExactly(EX + "knows", EX + "likes",
				EX + "tag", RDF.TYPE.stringValue());

		assertThat(LmdbPrefixRunPlan.OPENED.get()).isGreaterThan(before);
	}

	@Test
	public void distinctPredicateObjectUsesPrefixRun() {
		openRepository("spoc,posc,ospc");

		long before = LmdbPrefixRunPlan.OPENED.get();
		assertThat(pairs("SELECT DISTINCT ?p ?o WHERE { ?s ?p ?o }", "p", "o")).containsExactly(
				EX + "knows/" + EX + "alice", EX + "knows/" + EX + "bob", EX + "likes/" + EX + "carol",
				EX + "tag/" + EX + "dave", RDF.TYPE + "/" + EX + "Person", RDF.TYPE + "/" + EX + "Robot");

		assertThat(LmdbPrefixRunPlan.OPENED.get()).isGreaterThan(before);
	}

	@Test
	public void distinctObjectWithBoundPredicateUsesPrefixRun() {
		openRepository("spoc,posc,ospc");

		long before = LmdbPrefixRunPlan.OPENED.get();
		assertThat(values("SELECT DISTINCT ?type WHERE { ?instance a ?type }", "type")).containsExactly(EX + "Person",
				EX + "Robot");

		assertThat(LmdbPrefixRunPlan.OPENED.get()).isGreaterThan(before);
	}

	@Test
	public void distinctSubjectObjectWithBoundPredicateUsesPredicateFirstIndex() {
		openRepository("spoc,posc,ospc");

		long before = LmdbPrefixRunPlan.OPENED.get();
		assertThat(pairs("SELECT DISTINCT ?s ?o WHERE { ?s <" + EX + "knows> ?o }", "s", "o")).containsExactly(
				EX + "s1/" + EX + "alice", EX + "s2/" + EX + "alice", EX + "s3/" + EX + "bob");

		assertThat(LmdbPrefixRunPlan.OPENED.get()).isGreaterThan(before);
	}

	@Test
	public void reducedPredicateUsesPrefixRun() {
		openRepository("spoc,posc,ospc");

		long before = LmdbPrefixRunPlan.OPENED.get();
		assertThat(values("SELECT REDUCED ?p WHERE { ?s ?p ?o }", "p")).containsExactly(EX + "knows", EX + "likes",
				EX + "tag", RDF.TYPE.stringValue());

		assertThat(LmdbPrefixRunPlan.OPENED.get()).isGreaterThan(before);
	}

	@Test
	public void projectionAliasIsHonoured() {
		openRepository("spoc,posc,ospc");

		long before = LmdbPrefixRunPlan.OPENED.get();
		assertThat(values("SELECT DISTINCT (?p AS ?predicate) WHERE { ?s ?p ?o }", "predicate"))
				.containsExactly(EX + "knows", EX + "likes", EX + "tag", RDF.TYPE.stringValue());

		// SELECT expressions are evaluated by the regular engine
		assertThat(LmdbPrefixRunPlan.OPENED.get()).isEqualTo(before);
	}

	@Test
	public void countDistinctTypeUsesPrefixRun() {
		openRepository("spoc,posc,ospc");

		long before = LmdbPrefixRunPlan.OPENED.get();
		assertThat(count("SELECT (COUNT(DISTINCT ?type) AS ?c) WHERE { ?instance a ?type }", "c")).isEqualTo(2L);
		assertThat(count("SELECT (COUNT(DISTINCT ?p) AS ?c) WHERE { ?s ?p ?o }", "c")).isEqualTo(4L);
		assertThat(count("SELECT (COUNT(DISTINCT ?o) AS ?c) WHERE { ?s ?p ?o }", "c")).isEqualTo(6L);

		assertThat(LmdbPrefixRunPlan.OPENED.get()).isEqualTo(before + 3);
	}

	@Test
	public void countDistinctOnMissingValueIsZero() {
		openRepository("spoc,posc,ospc");

		long before = LmdbPrefixRunPlan.OPENED.get();
		assertThat(count("SELECT (COUNT(DISTINCT ?o) AS ?c) WHERE { ?s <" + EX + "unknown> ?o }", "c"))
				.isEqualTo(0L);
		assertThat(values("SELECT DISTINCT ?o WHERE { ?s <" + EX + "unknown> ?o }", "o")).isEmpty();

		// an unknown bound value cannot match anything: no cursor is opened at all
		assertThat(LmdbPrefixRunPlan.OPENED.get()).isEqualTo(before);
	}

	@Test
	public void countDistinctWithGroupByFallsBack() {
		openRepository("spoc,posc,ospc");

		long before = LmdbPrefixRunPlan.OPENED.get();
		List<BindingSet> result = query("SELECT ?p (COUNT(DISTINCT ?o) AS ?c) WHERE { ?s ?p ?o } GROUP BY ?p");
		assertThat(result).hasSize(4);

		assertThat(LmdbPrefixRunPlan.OPENED.get()).isEqualTo(before);
	}

	@Test
	public void multipleDistinctCountsShareTheRewrite() {
		openRepository("spoc,posc,ospc");

		long before = LmdbPrefixRunPlan.OPENED.get();
		List<BindingSet> result = query(
				"SELECT (COUNT(DISTINCT ?p) AS ?predicates) (COUNT(DISTINCT ?s) AS ?subjects) WHERE { ?s ?p ?o }");
		assertThat(result).hasSize(1);
		assertThat(((Literal) result.get(0).getValue("predicates")).longValue()).isEqualTo(4L);
		assertThat(((Literal) result.get(0).getValue("subjects")).longValue()).isEqualTo(8L);

		assertThat(LmdbPrefixRunPlan.OPENED.get()).isEqualTo(before + 2);
	}

	@Test
	public void largeRunsScanFewerRowsThanStatements() {
		openRepository("spoc,posc,ospc");
		int perPredicate = 2000;
		try (SailRepositoryConnection conn = repository.getConnection()) {
			ValueFactory vf = conn.getValueFactory();
			conn.begin();
			for (int i = 0; i < perPredicate; i++) {
				conn.add(vf.createIRI(EX, "big" + i), vf.createIRI(EX, "knows"), vf.createIRI(EX, "alice"));
				conn.add(vf.createIRI(EX, "big" + i), vf.createIRI(EX, "likes"), vf.createIRI(EX, "carol"));
			}
			conn.commit();
		}

		LmdbPrefixRunPlan.resetMetrics();
		assertThat(values("SELECT DISTINCT ?p WHERE { ?s ?p ?o }", "p")).hasSize(4);
		assertThat(LmdbPrefixRunPlan.OPENED.get()).isEqualTo(1L);
		assertThat(LmdbPrefixRunPlan.PREFIXES_EMITTED.get()).isEqualTo(4L);
		assertThat(LmdbPrefixRunPlan.ROWS_SCANNED.get()).isLessThan(statementCount() / 10);
	}

	@Test
	public void missingPrefixIndexFallsBackToExistingEvaluation() {
		openRepository("spoc");

		long before = LmdbPrefixRunPlan.OPENED.get();
		assertThat(values("SELECT DISTINCT ?p WHERE { ?s ?p ?o }", "p")).containsExactly(EX + "knows", EX + "likes",
				EX + "tag", RDF.TYPE.stringValue());
		assertThat(count("SELECT (COUNT(DISTINCT ?p) AS ?c) WHERE { ?s ?p ?o }", "c")).isEqualTo(4L);

		assertThat(LmdbPrefixRunPlan.OPENED.get()).isEqualTo(before);
	}

	@Test
	public void disabledPrefixRunPropertyFallsBackToExistingEvaluation() {
		System.setProperty(LmdbPrefixRunPlan.ENABLED_PROPERTY, "false");
		openRepository("spoc,posc,ospc");

		long before = LmdbPrefixRunPlan.OPENED.get();
		assertThat(values("SELECT DISTINCT ?p WHERE { ?s ?p ?o }", "p")).containsExactly(EX + "knows", EX + "likes",
				EX + "tag", RDF.TYPE.stringValue());

		assertThat(LmdbPrefixRunPlan.OPENED.get()).isEqualTo(before);
	}

	@Test
	public void uncommittedChangesAreVisibleInsideTransaction() {
		openRepository("spoc,posc,ospc");

		long before = LmdbPrefixRunPlan.OPENED.get();
		try (SailRepositoryConnection conn = repository.getConnection()) {
			ValueFactory vf = conn.getValueFactory();
			conn.begin();
			conn.add(vf.createIRI(EX, "s7"), vf.createIRI(EX, "hates"), vf.createIRI(EX, "eve"));
			List<String> predicates = QueryResults
					.asList(conn.prepareTupleQuery("SELECT DISTINCT ?p WHERE { ?s ?p ?o }").evaluate())
					.stream()
					.map(bs -> bs.getValue("p").stringValue())
					.sorted()
					.collect(Collectors.toList());
			assertThat(predicates).contains(EX + "hates");
			conn.rollback();
		}

		// inside a transaction the regular evaluation sees the transaction's own changes
		assertThat(LmdbPrefixRunPlan.OPENED.get()).isEqualTo(before);
	}

	@Test
	public void queryDatasetFallsBackToExistingEvaluation() {
		openRepository("spoc,posc,ospc");
		try (SailRepositoryConnection conn = repository.getConnection()) {
			ValueFactory vf = conn.getValueFactory();
			conn.add(vf.createIRI(EX, "s8"), vf.createIRI(EX, "graphed"), vf.createIRI(EX, "eve"),
					vf.createIRI(EX, "graph"));
		}

		long before = LmdbPrefixRunPlan.OPENED.get();
		assertThat(values("SELECT DISTINCT ?p FROM <" + EX + "graph> WHERE { ?s ?p ?o }", "p"))
				.containsExactly(EX + "graphed");
		assertThat(LmdbPrefixRunPlan.OPENED.get()).isEqualTo(before);

		// without a dataset the statement in the named graph is part of the default scope
		assertThat(values("SELECT DISTINCT ?p WHERE { ?s ?p ?o }", "p")).contains(EX + "graphed");
		assertThat(LmdbPrefixRunPlan.OPENED.get()).isEqualTo(before + 1);
	}

	@Test
	public void namedGraphScopeFallsBackToExistingEvaluation() {
		openRepository("spoc,posc,ospc,cspo");
		try (SailRepositoryConnection conn = repository.getConnection()) {
			ValueFactory vf = conn.getValueFactory();
			conn.add(vf.createIRI(EX, "s8"), vf.createIRI(EX, "graphed"), vf.createIRI(EX, "eve"),
					vf.createIRI(EX, "graph"));
		}

		long before = LmdbPrefixRunPlan.OPENED.get();
		// GRAPH ?g excludes the null context, which a prefix-run over the context field cannot express
		assertThat(values("SELECT DISTINCT ?g WHERE { GRAPH ?g { ?s ?p ?o } }", "g")).containsExactly(EX + "graph");
		assertThat(LmdbPrefixRunPlan.OPENED.get()).isEqualTo(before);
	}

	@Test
	public void inferredStatementsFallBackToExistingEvaluation() {
		openRepository("spoc,posc,ospc");
		try (SailRepositoryConnection conn = repository.getConnection()) {
			ValueFactory vf = conn.getValueFactory();
			conn.begin();
			((LmdbStoreConnection) conn.getSailConnection()).addInferredStatement(vf.createIRI(EX, "s9"),
					vf.createIRI(EX, "inferred"), vf.createIRI(EX, "eve"));
			conn.commit();
		}

		long before = LmdbPrefixRunPlan.OPENED.get();
		assertThat(values("SELECT DISTINCT ?p WHERE { ?s ?p ?o }", "p")).contains(EX + "inferred");
		assertThat(LmdbPrefixRunPlan.OPENED.get()).isEqualTo(before);

		try (SailRepositoryConnection conn = repository.getConnection()) {
			TupleQuery query = conn.prepareTupleQuery("SELECT DISTINCT ?p WHERE { ?s ?p ?o }");
			query.setIncludeInferred(false);
			List<String> predicates = QueryResults.asList(query.evaluate())
					.stream()
					.map(bs -> bs.getValue("p").stringValue())
					.collect(Collectors.toList());
			assertThat(predicates).doesNotContain(EX + "inferred").hasSize(4);
		}
		assertThat(LmdbPrefixRunPlan.OPENED.get()).isEqualTo(before + 1);
	}

	private void openRepository(String indexes) {
		LmdbPrefixRunPlan.resetMetrics();
		repository = new SailRepository(new LmdbStore(dataDir, new LmdbStoreConfig(indexes)));
		try (SailRepositoryConnection conn = repository.getConnection()) {
			ValueFactory vf = conn.getValueFactory();
			IRI knows = vf.createIRI(EX, "knows");
			IRI likes = vf.createIRI(EX, "likes");
			IRI tag = vf.createIRI(EX, "tag");
			IRI alice = vf.createIRI(EX, "alice");
			IRI bob = vf.createIRI(EX, "bob");
			IRI carol = vf.createIRI(EX, "carol");
			IRI dave = vf.createIRI(EX, "dave");
			IRI person = vf.createIRI(EX, "Person");
			IRI robot = vf.createIRI(EX, "Robot");
			conn.add(vf.createIRI(EX, "s1"), knows, alice);
			conn.add(vf.createIRI(EX, "s2"), knows, alice);
			conn.add(vf.createIRI(EX, "s3"), knows, bob);
			conn.add(vf.createIRI(EX, "s4"), likes, carol);
			conn.add(vf.createIRI(EX, "s5"), likes, carol);
			conn.add(vf.createIRI(EX, "s6"), tag, dave);
			conn.add(vf.createIRI(EX, "s1"), RDF.TYPE, person);
			conn.add(vf.createIRI(EX, "s2"), RDF.TYPE, person);
			conn.add(vf.createIRI(EX, "s3"), RDF.TYPE, robot);
			conn.add(alice, RDF.TYPE, person);
			conn.add(bob, RDF.TYPE, robot);
		}
	}

	private List<BindingSet> query(String query) {
		try (SailRepositoryConnection conn = repository.getConnection()) {
			return QueryResults.asList(conn.prepareTupleQuery(query).evaluate());
		}
	}

	private List<String> values(String query, String bindingName) {
		return query(query).stream().map(bs -> bs.getValue(bindingName).stringValue()).sorted().toList();
	}

	private List<String> pairs(String query, String first, String second) {
		return query(query).stream()
				.map(bs -> bs.getValue(first).stringValue() + "/" + bs.getValue(second).stringValue())
				.sorted()
				.toList();
	}

	private long count(String query, String bindingName) {
		List<BindingSet> result = query(query);
		assertThat(result).hasSize(1);
		return ((Literal) result.get(0).getValue(bindingName)).longValue();
	}

	private long statementCount() {
		try (SailRepositoryConnection conn = repository.getConnection()) {
			return conn.size();
		}
	}
}
