/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TupleQueryResultTest {

	private final Logger logger = LoggerFactory.getLogger(TupleQueryResultTest.class);

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	private Repository rep;

	private RepositoryConnection con;

	private String emptyResultQuery;

	private String singleResultQuery;

	private String multipleResultQuery;

	@Before
	public void setUp() throws Exception {
		rep = createRepository();
		con = rep.getConnection();

		buildQueries();
		addData();
	}

	@After
	public void tearDown() throws Exception {
		try {
			con.close();
			con = null;
		} finally {
			rep.shutDown();
			rep = null;
		}
	}

	protected Repository createRepository() throws Exception {
		Repository repository = newRepository();
		repository.initialize();
		RepositoryConnection con = repository.getConnection();
		con.clear();
		con.clearNamespaces();
		con.close();
		return repository;
	}

	protected abstract Repository newRepository() throws Exception;

	/*
	 * build some simple SeRQL queries to use for testing the result set object.
	 */
	private void buildQueries() {
		StringBuilder query = new StringBuilder();

		query.append("SELECT * ");
		query.append("FROM {X} P {Y} ");
		query.append("WHERE X != X ");

		emptyResultQuery = query.toString();

		query = new StringBuilder();

		query.append("SELECT DISTINCT P ");
		query.append("FROM {} dc:publisher {P} ");
		query.append("USING NAMESPACE ");
		query.append("   dc = <http://purl.org/dc/elements/1.1/>");

		singleResultQuery = query.toString();

		query = new StringBuilder();
		query.append("SELECT DISTINCT P, D ");
		query.append("FROM {} dc:publisher {P}; ");
		query.append("        dc:date {D} ");
		query.append("USING NAMESPACE ");
		query.append("   dc = <http://purl.org/dc/elements/1.1/>");

		multipleResultQuery = query.toString();
	}

	private void addData() throws IOException, UnsupportedRDFormatException, RDFParseException, RepositoryException {
		InputStream defaultGraph = TupleQueryResultTest.class.getResourceAsStream("/testcases/default-graph-1.ttl");
		try {
			con.add(defaultGraph, "", RDFFormat.TURTLE);
		} finally {
			defaultGraph.close();
		}
	}

	@Test
	public void testGetBindingNames() throws Exception {
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SERQL, multipleResultQuery).evaluate();
		try {
			List<String> headers = result.getBindingNames();

			assertThat(headers.get(0)).isEqualTo("P").as("first header element");
			assertThat(headers.get(1)).isEqualTo("D").as("second header element");
		} finally {
			result.close();
		}
	}

	/*
	 * deprecated public void testIsDistinct() throws Exception { TupleQueryResult result =
	 * con.prepareTupleQuery(QueryLanguage.SERQL, emptyResultQuery).evaluate(); try { if (result.isDistinct()) {
	 * fail("query result should not be distinct."); } } finally { result.close(); } result =
	 * con.prepareTupleQuery(QueryLanguage.SERQL, singleResultQuery).evaluate(); try { if (!result.isDistinct()) {
	 * fail("query result should be distinct."); } } finally { result.close(); } }
	 */

	@Test
	public void testIterator() throws Exception {
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SERQL, multipleResultQuery).evaluate();

		try {
			int count = 0;
			while (result.hasNext()) {
				result.next();
				count++;
			}

			assertTrue("query should have multiple results.", count > 1);
		} finally {
			result.close();
		}
	}

	@Test
	public void testIsEmpty() throws Exception {
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SERQL, emptyResultQuery).evaluate();

		try {
			assertFalse("Query result should be empty", result.hasNext());
		} finally {
			result.close();
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
			if (Math.round(Math.random()) > 0) {
				subjectIndex++;
			}
			if (Math.round(Math.random()) > 0) {
				predicateIndex++;
			}
			if (Math.round(Math.random()) > 0) {
				objectIndex++;
			}
			count++;
		}
		con.commit();

		for (int evaluateCount = 0; evaluateCount < 1000; evaluateCount++) {
			try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
					RepositoryConnection nextCon = rep.getConnection();) {
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
			if (Math.round(Math.random()) > 0) {
				subjectIndex++;
			}
			if (Math.round(Math.random()) > 0) {
				predicateIndex++;
			}
			if (Math.round(Math.random()) > 0) {
				objectIndex++;
			}
			count++;
		}
		con.commit();

		logger.info("Open lots of TupleQueryResults without closing them");
		for (int i = 0; i < 100; i++) {
			try (RepositoryConnection repCon = rep.getConnection()) {
				String queryString = "select * where {?s ?p ?o}";
				TupleQuery tupleQuery = repCon.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

				// see if open results hangs test
				try (TupleQueryResult result = tupleQuery.evaluate()) {
				}
			}
		}
	}
}
