/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

import org.eclipse.rdf4j.common.concurrent.locks.Properties;
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
import org.junit.After;
import org.junit.AfterClass;
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

	@AfterClass
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	private Repository rep;

	private RepositoryConnection con;

	private String emptyResultQuery;

	private String multipleResultQuery;

	private final Random random = new Random(43252333);

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
		InputStream defaultGraph = TupleQueryResultTest.class.getResourceAsStream("/testcases/default-graph-1.ttl");
		try {
			con.add(defaultGraph, "", RDFFormat.TURTLE);
		} finally {
			defaultGraph.close();
		}
	}

	@Test
	public void testGetBindingNames() throws Exception {
		TupleQueryResult result = con.prepareTupleQuery(multipleResultQuery).evaluate();
		try {
			List<String> headers = result.getBindingNames();

			assertThat(headers.get(0)).isEqualTo("P").as("first header element");
			assertThat(headers.get(1)).isEqualTo("D").as("second header element");
		} finally {
			result.close();
		}
	}

	@Test
	public void testIterator() throws Exception {
		TupleQueryResult result = con.prepareTupleQuery(multipleResultQuery).evaluate();

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
		TupleQueryResult result = con.prepareTupleQuery(emptyResultQuery).evaluate();

		try {
			assertFalse("Query result should be empty", result.hasNext());
		} finally {
			result.close();
		}
	}

	@Test
	public void testCountMatchesAllSelect() throws Exception {
		TupleQueryResult result = con.prepareTupleQuery("SELECT * WHERE {?s ?p ?o}").evaluate();
		long size = con.size();
		try {
			for (int i = 0; i < size; i++) {
				assertTrue(result.hasNext());
				BindingSet next = result.next();
				assertNotNull(next);
			}
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
			try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
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
	public void testNotClosingResult() throws InterruptedException {
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
				String queryString = "select * where {?s ?p ?o}";
				TupleQuery tupleQuery = repCon.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

				// see if open results hangs test
				// DO NOT CLOSE THIS
				tupleQuery.evaluate();
			}
		}
	}

	@Test(expected = SailException.class)
	public void testNotClosingResultThrowsException() throws InterruptedException {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");

		con.begin();
		con.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
		con.commit();

		TupleQueryResult evaluate;
		try (RepositoryConnection repCon = rep.getConnection()) {
			String queryString = "select * where {?s ?p ?o}";
			TupleQuery tupleQuery = repCon.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

			evaluate = tupleQuery.evaluate();
		}

		evaluate.close();

	}
}
