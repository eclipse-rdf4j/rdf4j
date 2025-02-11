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
package org.eclipse.rdf4j.testsuite.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

public abstract class TupleQueryResultTest {

	private final Logger logger = LoggerFactory.getLogger(TupleQueryResultTest.class);

	private Repository rep;

	private RepositoryConnection con;

	private String emptyResultQuery;

	private String multipleResultQuery;

	private final Random random = new Random(43252333);

	private List<WeakReference<TupleQueryResult>> unclosedQueryResults;

	@BeforeEach
	public void setUp() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
				.getLogger("org.eclipse.rdf4j.sail.helpers.AbstractSailConnection");
		root.setLevel(Level.ERROR);

		rep = createRepository();
		con = rep.getConnection();

		buildQueries();
		addData();
		unclosedQueryResults = new ArrayList<>();

	}

	@AfterEach
	public void tearDown() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
				.getLogger("org.eclipse.rdf4j.sail.helpers.AbstractSailConnection");
		root.setLevel(Level.WARN);
		try {
			con.close();
			con = null;
		} finally {
			System.gc();
			Thread.sleep(1);
			rep.shutDown();
			rep = null;
		}
	}

	protected Repository createRepository() {
		Repository repository = newRepository();
		try (RepositoryConnection con = repository.getConnection()) {
			con.clear();
			con.clearNamespaces();
		}
		return repository;
	}

	protected abstract Repository newRepository();

	/*
	 * build some simple SPARQL queries to use for testing the result set object.
	 */
	private void buildQueries() {
		StringBuilder query = new StringBuilder();

		query.append("SELECT * ");
		query.append("WHERE { ?X ?P ?Y . FILTER (?X != ?X) }");

		emptyResultQuery = query.toString();

		query = new StringBuilder();

		query.append("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n");
		query.append("SELECT DISTINCT ?P \n");
		query.append("WHERE { [] dc:publisher ?P }\n");

		String singleResultQuery = query.toString();

		query = new StringBuilder();

		query.append("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n");
		query.append("SELECT DISTINCT ?P ?D \n");
		query.append("WHERE { [] dc:publisher ?P;\n");
		query.append("        dc:date ?D. }\n");

		multipleResultQuery = query.toString();
	}

	private void addData() throws IOException, UnsupportedRDFormatException, RDFParseException, RepositoryException {
		try (InputStream defaultGraph = TupleQueryResultTest.class
				.getResourceAsStream("/testcases/default-graph-1.ttl")) {
			con.add(defaultGraph, "", RDFFormat.TURTLE);
		}
	}

	@Test
	public void testGetBindingNames() {
		try (TupleQueryResult result = con.prepareTupleQuery(multipleResultQuery).evaluate()) {
			List<String> headers = result.getBindingNames();

			assertThat(headers.get(0)).as("first header element").isEqualTo("P");
			assertThat(headers.get(1)).as("second header element").isEqualTo("D");
		}
	}

	@Test
	public void testIterator() {

		try (TupleQueryResult result = con.prepareTupleQuery(multipleResultQuery).evaluate()) {
			int count = 0;
			while (result.hasNext()) {
				result.next();
				count++;
			}

			assertTrue(count > 1, "query should have multiple results.");
		}
	}

	@Test
	public void testIsEmpty() {

		try (TupleQueryResult result = con.prepareTupleQuery(emptyResultQuery).evaluate()) {
			Assertions.assertFalse(result.hasNext(), "Query result should be empty");
		}
	}

	@Test
	public void testCountMatchesAllSelect() {
		try (TupleQueryResult result = con.prepareTupleQuery("SELECT * WHERE {?s ?p ?o}").evaluate()) {
			long size = con.size();
			for (int i = 0; i < size; i++) {
				assertTrue(result.hasNext());
				BindingSet next = result.next();
				Assertions.assertNotNull(next);
			}
			Assertions.assertFalse(result.hasNext(), "Query result should be empty");
		}
	}

	@Test
	public void testStreaming() throws Exception {
		ValueFactory vf = con.getValueFactory();
		int subjectIndex = 0;
		int predicateIndex = 100;
		int objectIndex = 1000;
		int testStatementCount = 1000;
		int count = 0;
		con.begin();
		while (count < testStatementCount) {
			con.add(vf.createIRI("urn:test:" + subjectIndex), vf.createIRI("urn:test:" + predicateIndex),
					vf.createIRI("urn:test:" + objectIndex));
			if (Math.round(random.nextDouble()) > 0) {
				subjectIndex++;
			}
			if (Math.round(random.nextDouble()) > 0) {
				predicateIndex++;
			}
			if (Math.round(random.nextDouble()) > 0) {
				objectIndex++;
			}
			count++;
		}
		con.commit();

		for (int evaluateCount = 0; evaluateCount < 1000; evaluateCount++) {
			try (ByteArrayOutputStream stream = new ByteArrayOutputStream(191226);
					RepositoryConnection nextCon = rep.getConnection()) {
				TupleQueryResultWriter sparqlWriter = QueryResultIO.createTupleWriter(TupleQueryResultFormat.SPARQL,
						stream);
				TupleQuery tupleQuery = nextCon.prepareTupleQuery(QueryLanguage.SPARQL,
						"SELECT ?s ?p ?o WHERE { ?s ?p ?o . }");
				tupleQuery.setIncludeInferred(false);
				tupleQuery.evaluate(sparqlWriter);
			}
		}
	}

	@Test
	public void testNotClosingResult() {
		ValueFactory vf = con.getValueFactory();
		int subjectIndex = 0;
		int predicateIndex = 100;
		int objectIndex = 1000;
		int testStatementCount = 1000;
		int count = 0;
		con.begin();
		while (count < testStatementCount) {
			con.add(vf.createIRI("urn:test:" + subjectIndex), vf.createIRI("urn:test:" + predicateIndex),
					vf.createIRI("urn:test:" + objectIndex));
			if (Math.round(random.nextDouble()) > 0) {
				subjectIndex++;
			}
			if (Math.round(random.nextDouble()) > 0) {
				predicateIndex++;
			}
			if (Math.round(random.nextDouble()) > 0) {
				objectIndex++;
			}
			count++;
		}
		con.commit();

		logger.info("Open lots of TupleQueryResults without closing them");
		for (int i = 0; i < 100; i++) {
			try (RepositoryConnection repCon = rep.getConnection()) {
				evaluateQueryWithoutClosing(repCon);
			} catch (SailException e) {
				assertTrue(e.toString()
						.startsWith(
								"org.eclipse.rdf4j.sail.SailException: Connection closed before all iterations were closed: org.eclipse.rdf4j.sail.helpers.SailBaseIteration@"));
			}
		}
	}

	@Test
	public void testNotClosingResultWithoutDebug() throws InterruptedException {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");

		StopWatch stopWatch = StopWatch.createStarted();

		con.begin();
		con.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
		con.commit();

		for (int i = 0; i < 100; i++) {
			try (RepositoryConnection repCon = rep.getConnection()) {
				evaluateQueryWithoutClosing(repCon);
				System.gc();
				Thread.sleep(1);
				while (unclosedQueryResults.stream().map(Reference::get).anyMatch(Objects::nonNull)) {
					System.gc();
					Thread.sleep(100);
					assertTrue(stopWatch.getTime(TimeUnit.SECONDS) < 60, "Test timed out after 60 seconds");

				}
			}
		}

	}

	private void evaluateQueryWithoutClosing(RepositoryConnection repCon) {
		String queryString = "select * where {?s ?p ?o}";
		TupleQuery tupleQuery = repCon.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

		// see if open results hangs test
		// DO NOT CLOSE THIS
		TupleQueryResult evaluate = tupleQuery.evaluate();
		unclosedQueryResults.add(new WeakReference<>(evaluate));
		Assertions.assertNotNull(evaluate);
	}

	@Test
	public void testNotClosingResultThrowsException() {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");

		con.begin();
		con.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
		con.commit();

		Assertions.assertThrows(SailException.class, () -> {
			TupleQueryResult evaluate = null;
			try (RepositoryConnection repCon = rep.getConnection()) {
				String queryString = "select * where {?s ?p ?o}";
				TupleQuery tupleQuery = repCon.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

				evaluate = tupleQuery.evaluate();
			} finally {
				evaluate.close();
			}
		});

	}

	@Test
	public void testLeftJoinWithJoinCondition() throws IOException {

		String data = "@prefix wdt: <http://www.wikidata.org/prop/direct/> .\n" +
				"@prefix wd: <http://www.wikidata.org/entity/> .\n" +
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
				"@prefix wikibase: <http://wikiba.se/ontology#> .\n" +

				"\n" +
				"wd:other1 wdt:P106 wd:Q27532438 .\n" +
				"wd:other2 wdt:P106 wd:Q27532438 .\n" +
				"wd:other3 wdt:P106 wd:Q27532438 .\n" +

				"wd:Q27532438 a wikibase:Item;\n" +
				"  rdfs:label \"suffragist\"@en, \"sufrageto\"@eo, \"суфражистка\"@ru, \"suffragetti\"@fi, \"szüfrazsett\"@lv.\n"
				+

				"wd:Q38203 wdt:P106 wd:Q27532437 .\n" +
				"\n" +
				"wd:Q27532437 a wikibase:Item;\n" +
				"  rdfs:label \"suffragist\"@en, \"sufrageto\"@eo, \"суфражистка\"@ru, \"suffragetti\"@fi, \"szüfrazsett\"@hu.\n"
				+
				"\n" +

				"wd:other9 wdt:P106 wd:Q27532439 .\n" +

				"wd:Q27532438 a wikibase:Item;\n" +
				"  rdfs:label \"suffragist\"@en, \"sufrageto\"@eo, \"суфражистка\"@ru, \"suffragetti\"@fi, \"szüfrazsett\"@no-nb.\n"
				+

				"wd:Q89166696 wdt:P106 wd:Q27532437 .\n";

		con.add(new StringReader(data), "", RDFFormat.TURTLE);

		TupleQuery tupleQuery = con.prepareTupleQuery("PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" +
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
				"SELECT ?occup ?occupLabel ?count\n" +
				"WHERE\n" +
				"{\n" +
				"    {\n" +
				"        SELECT ?occup (COUNT(?person) as ?count)\n" +
				"        WHERE\n" +
				"        {\n" +
				"            ?person wdt:P106 ?occup\n" +
				"        }\n" +
				"        GROUP BY ?occup\n" +
				"        ORDER BY DESC(?count)\n" +
				"        LIMIT 1000\n" +
				"    }\n" +
				"BIND(\"lv\" AS ?lang)\n" +
				"      OPTIONAL {" +
				"			?occup rdfs:label ?label1.     \n" +
				"			filter(lang(?label1) = ?lang)\n" +
				"}\n" +
				"    FILTER(!BOUND(?label1))\n" +
				"?occup rdfs:label ?occupLabel FILTER (LANG(?occupLabel)=\"en\") .\n" +
				"\n" +
				"}\n" +
				"ORDER BY DESC(?count)\n" +
				"LIMIT 50");
		con.begin();
		try (TupleQueryResult evaluate = tupleQuery.evaluate()) {
			assertTrue(evaluate.hasNext());
		}
		con.commit();

	}

}
