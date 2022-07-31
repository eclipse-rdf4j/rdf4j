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
import java.util.List;
import java.util.Random;

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

public abstract class TupleQueryResultTest {

	private final Logger logger = LoggerFactory.getLogger(TupleQueryResultTest.class);

	private Repository rep;

	private RepositoryConnection con;

	private String emptyResultQuery;

	private String multipleResultQuery;

	private final Random random = new Random(43252333);

	@BeforeEach
	public void setUp() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
		rep = createRepository();
		con = rep.getConnection();

		buildQueries();
		addData();
	}

	@AfterEach
	public void tearDown() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");

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

	protected Repository createRepository() throws Exception {
		Repository repository = newRepository();
		try (RepositoryConnection con = repository.getConnection()) {
			con.clear();
			con.clearNamespaces();
		}
		return repository;
	}

	protected abstract Repository newRepository() throws Exception;

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

			assertThat(headers.get(0)).isEqualTo("P").as("first header element");
			assertThat(headers.get(1)).isEqualTo("D").as("second header element");
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
	public void testNotClosingResultWithoutDebug() {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");

		con.begin();
		con.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
		con.commit();

		for (int i = 0; i < 100; i++) {
			try (RepositoryConnection repCon = rep.getConnection()) {
				evaluateQueryWithoutClosing(repCon);
			}
		}

	}

	private void evaluateQueryWithoutClosing(RepositoryConnection repCon) {
		String queryString = "select * where {?s ?p ?o}";
		TupleQuery tupleQuery = repCon.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

		// see if open results hangs test
		// DO NOT CLOSE THIS
		TupleQueryResult evaluate = tupleQuery.evaluate();
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
}
