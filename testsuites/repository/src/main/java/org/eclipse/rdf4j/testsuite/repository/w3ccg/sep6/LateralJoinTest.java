/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.repository.w3ccg.sep6;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public abstract class LateralJoinTest {

	private static final String EX_NS = "http://example.org/";;

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	public String q1 = "PREFIX : <http://example.org/>\n"
			+ "SELECT * {\n"
			+ "    ?s a :T .\n"
			+ "    LATERAL { SELECT * { ?s :p ?p } ORDER BY ?p LIMIT 2 }\n"
			+ "    LATERAL { SELECT * { ?s :q ?q } ORDER BY ?q LIMIT 2 }\n"
			+ "} ORDER BY ?s ?p ?q\n" + "";

	public String q2 = "PREFIX : <http://example.org/>\n"
			+ "SELECT * {\n"
			+ "    ?s a :T .\n"
			+ "    { LATERAL { SELECT * { ?s :p ?p } ORDER BY ?p LIMIT 2 } }\n"
			+ "    UNION \n"
			+ "    { LATERAL { SELECT * { ?s :q ?q } ORDER BY ?q LIMIT 2 } }\n"
			+ "}";

	public String q2a = "PREFIX : <http://example.org/>\n"
			+ "\n"
			+ "SELECT * {\n"
			+ "    ?s a :T .\n"
			+ "    LATERAL {\n"
			+ "      { SELECT * { ?s :p ?p } ORDER BY ?p LIMIT 2 }\n"
			+ "      UNION\n"
			+ "      { SELECT * { ?s :q ?q } ORDER BY ?q LIMIT 2 }\n"
			+ "    }\n"
			+ "} ORDER BY ?s ?p ?q\n" + "";

	public String q3 = "PREFIX : <http://example.org/>\n"
			+ "\n"
			+ "SELECT * {\n"
			+ "    ?s a :T .\n"
			+ "    { LATERAL { SELECT * { ?s :p ?o } ORDER BY ?o LIMIT 2 } BIND(\"A\" AS ?x) }\n"
			+ "    UNION \n"
			+ "    { LATERAL { SELECT * { ?s :q ?o } ORDER BY ?o LIMIT 2 } BIND(\"B\" AS ?x) }\n"
			+ "} ORDER BY ?s ?o ?x";

	public String simple = "PREFIX : <http://example.org/>\n"
			+ "SELECT ?s ?o WHERE {\n"
			+ "    VALUES ?s { :S }\n"
			+ "    LATERAL {\n"
			+ "        { VALUES ?o { :O } }\n"
			+ "        { FILTER(BOUND(?s) && !BOUND(?o)) }\n"
			+ "    }\n"
			+ "}";

	public String graph = "PREFIX : <http://example.org/>\n"
			+ "SELECT ?s ?o WHERE {\n"
			+ "    VALUES ?s { :S }\n"
			+ "    LATERAL { GRAPH :G { FILTER(BOUND(?s)) . VALUES ?o { :O } } }\n"
			+ "}";

	public String subselect = "PREFIX : <http://example.org/>\n"
			+ "SELECT ?s ?o WHERE {\n"
			+ "    ?s a :T.\n"
			+ "    LATERAL {SELECT ?s ?o WHERE { ?s :p ?o } ORDER BY ?o LIMIT 2}\n"
			+ "}";

	private Repository repository;

	private RepositoryConnection conn;

	private ValueFactory vf;

	private IRI s1;

	private IRI s2;

	private IRI tC;

	private IRI p;

	private IRI q;

	/**
	 * Adapted from OxiGraph MIT licensed test suite
	 *
	 * <pre>
	 * -----------------------------
	 * | s | o |
	 * =============================
	 * | ex:S | ex:O |
	 * </pre>
	 */
	@Test
	public void simple() {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, simple);
		try (TupleQueryResult result = query.evaluate()) {

			assertTrue(result.hasNext());
			final BindingSet next = result.next();
			assertEquals(vf.createIRI(EX_NS, "S"), next.getValue("s"));
			assertEquals(vf.createIRI(EX_NS, "O"), next.getValue("o"));
			assertFalse(result.hasNext());
		}
	}

	/**
	 * Adapted from OxiGraph MIT licensed test suite
	 *
	 * <pre>
	 * -----------------------------
	 * | s | o |
	 * =============================
	 * | ex:S | ex:O |
	 * </pre>
	 */
	@Test
	public void graph() {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, graph);
		try (TupleQueryResult result = query.evaluate()) {

			assertTrue(result.hasNext());
			final BindingSet next = result.next();
			assertEquals(vf.createIRI(EX_NS, "S"), next.getValue("s"));
			assertEquals(vf.createIRI(EX_NS, "O"), next.getValue("o"));
			assertFalse(result.hasNext());
		}
	}

	/**
	 * Query is from the OxiGraph MIT licensed test set but results are using the W3C contrib licensed examples from
	 * <a href="https://github.com/w3c/sparql-12/issues/100">issue 100</a>.
	 */
	@Test
	public void subselect() {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, subselect);
		try (TupleQueryResult result = query.evaluate()) {

			assertTrue(result.hasNext());
			BindingSet next = result.next();
			assertEquals(vf.createIRI(EX_NS, "s1"), next.getValue("s"));
			assertEquals(vf.createLiteral("s1-p-1"), next.getValue("o"));
			assertTrue(result.hasNext());
			next = result.next();
			assertEquals(vf.createIRI(EX_NS, "s1"), next.getValue("s"));
			assertEquals(vf.createLiteral("s1-p-2"), next.getValue("o"));
			assertTrue(result.hasNext());
			next = result.next();
			assertEquals(vf.createIRI(EX_NS, "s2"), next.getValue("s"));
			assertEquals(vf.createLiteral("s2-p-1"), next.getValue("o"));
			assertTrue(result.hasNext());
			next = result.next();
			assertEquals(vf.createIRI(EX_NS, "s2"), next.getValue("s"));
			assertEquals(vf.createLiteral("s2-p-2"), next.getValue("o"));
			assertFalse(result.hasNext());
		}
	}

	/**
	 * <pre>
	 * -----------------------------
	 * | s | p | q |
	 * =============================
	 * | :s1 | "s1-p-1" | "s1-q-1" |
	 * | :s1 | "s1-p-1" | "s1-q-2" |
	 * | :s1 | "s1-p-2" | "s1-q-1" |
	 * | :s1 | "s1-p-2" | "s1-q-2" |
	 * | :s2 | "s2-p-1" | "s2-q-1" |
	 * | :s2 | "s2-p-1" | "s2-q-2" |
	 * | :s2 | "s2-p-2" | "s2-q-1" |
	 * | :s2 | "s2-p-2" | "s2-q-2" |
	 * -----------------------------
	 * </pre>
	 *
	 */
	@Test
	public void q1() {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, q1);
		try (TupleQueryResult result = query.evaluate()) {
			for (int i = 0; i < 4; i++) {
				assertTrue(result.hasNext());
				final BindingSet next = result.next();
				assertEquals(s1, next.getValue("s"));
			}
			for (int i = 0; i < 4; i++) {
				assertTrue(result.hasNext());
				final BindingSet next = result.next();
				assertEquals(s2, next.getValue("s"));
			}
			assertFalse(result.hasNext());
		}
	}

	/**
	 * <pre>
	 * -----------------------------
	 * | s | p | q |
	 * =============================
	 * | :s1 | | "s1-q-1" |
	 * | :s1 | | "s1-q-2" |
	 * | :s1 | "s1-p-1" | |
	 * | :s1 | "s1-p-2" | |
	 * | :s2 | | "s2-q-1" |
	 * | :s2 | | "s2-q-2" |
	 * | :s2 | "s2-p-1" | |
	 * | :s2 | "s2-p-2" | |
	 * -----------------------------
	 * </pre>
	 *
	 */
	@Test
	public void q2() {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, q2);
		try (TupleQueryResult result = query.evaluate()) {
			for (int i = 0; i < 4; i++) {
				assertTrue(result.hasNext());
				final BindingSet next = result.next();
				assertEquals(s1, next.getValue("s"));
				if (i < 2) {
					assertNull(next.getValue("p"));
					assertNotNull(next.getValue("q"));
				} else {
					assertNotNull(next.getValue("p"));
					assertNull(next.getValue("q"));
				}
			}
			for (int i = 0; i < 4; i++) {
				assertTrue(result.hasNext());
				final BindingSet next = result.next();
				assertEquals(s2, next.getValue("s"));
				if (i < 2) {
					assertNull(next.getValue("p"));
					assertNotNull(next.getValue("q"));
				} else {
					assertNotNull(next.getValue("p"));
					assertNull(next.getValue("q"));
				}
			}
			assertFalse(result.hasNext());
		}
	}

	/**
	 * <pre>
	 * | s | p | q |
	 * =============================
	 * | :s1 | | "s1-q-1" |
	 * | :s1 | | "s1-q-2" |
	 * | :s2 | | "s2-q-1" |
	 * | :s2 | | "s2-q-2" |
	 * | :s1 | "s1-p-1" | |
	 * | :s1 | "s1-p-2" | |
	 * | :s2 | "s2-p-1" | |
	 * | :s2 | "s2-p-2" | |
	 * -----------------------------
	 * </pre>
	 *
	 */
	@Test
	public void q2a() {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, q2a);
		try (TupleQueryResult result = query.evaluate()) {
			for (int i = 0; i < 4; i++) {
				assertTrue(result.hasNext());
				final BindingSet next = result.next();
				assertEquals(s1, next.getValue("s"));
				if (i < 2) {
					assertNull(next.getValue("p"));
					assertNotNull(next.getValue("q"));
				} else {
					assertNotNull(next.getValue("p"));
					assertNull(next.getValue("q"));
				}
			}
			for (int i = 0; i < 4; i++) {
				assertTrue(result.hasNext());
				final BindingSet next = result.next();
				assertEquals(s2, next.getValue("s"));
				if (i < 2) {
					assertNull(next.getValue("p"));
					assertNotNull(next.getValue("q"));
				} else {
					assertNotNull(next.getValue("p"));
					assertNull(next.getValue("q"));
				}
			}
			assertFalse(result.hasNext());
		}
	}

	/**
	 * <pre>
	 * ------------------------
	 * | s | o | x |
	 * ========================
	 * | :s1 | "s1-p-1" | "A" |
	 * | :s1 | "s1-p-2" | "A" |
	 * | :s1 | "s1-q-1" | "B" |
	 * | :s1 | "s1-q-2" | "B" |
	 * | :s2 | "s2-p-1" | "A" |
	 * | :s2 | "s2-p-2" | "A" |
	 * | :s2 | "s2-q-1" | "B" |
	 * | :s2 | "s2-q-2" | "B" |
	 * ------------------------
	 * </pre>
	 *
	 */
	@Test
	public void q3() {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, q3);
		try (TupleQueryResult result = query.evaluate()) {
			for (int i = 0; i < 4; i++) {
				assertTrue(result.hasNext());
				final BindingSet next = result.next();
				assertEquals(s1, next.getValue("s"));
				if (i < 2) {
					assertEquals("A", next.getValue("x").stringValue());
				} else {
					assertEquals("B", next.getValue("x").stringValue());
				}
			}
			for (int i = 0; i < 4; i++) {
				assertTrue(result.hasNext());
				final BindingSet next = result.next();
				assertEquals(s2, next.getValue("s"));
				if (i < 2) {
					assertEquals("A", next.getValue("x").stringValue());
				} else {
					assertEquals("B", next.getValue("x").stringValue());
				}
			}
			assertFalse(result.hasNext());
		}
	}

	@Before
	public void setUp() throws Exception {
		repository = createRepository();
		vf = repository.getValueFactory();
		addTestData();
		conn = repository.getConnection();
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

	@After
	public void tearDown() throws Exception {
		conn.close();
		conn = null;

		repository.shutDown();
		repository = null;

		vf = null;
	}

	/**
	 * Example dataset from <a href="https://github.com/w3c/sparql-12/issues/100">issue 100</a>
	 *
	 * @throws RepositoryException
	 */
	private void addTestData() throws RepositoryException {
		try (RepositoryConnection conn = repository.getConnection()) {

			s1 = vf.createIRI(EX_NS, "s1");
			s2 = vf.createIRI(EX_NS, "s2");
			tC = vf.createIRI(EX_NS, "T");
			p = vf.createIRI(EX_NS, "p");
			q = vf.createIRI(EX_NS, "q");
			conn.add(s1, RDF.TYPE, tC);
			conn.add(s1, p, vf.createLiteral("s1-p-1"));
			conn.add(s1, p, vf.createLiteral("s1-p-2"));
			conn.add(s1, p, vf.createLiteral("s1-p-3"));
			conn.add(s1, q, vf.createLiteral("s1-q-1"));
			conn.add(s1, q, vf.createLiteral("s1-q-2"));
			conn.add(s1, q, vf.createLiteral("s1-q-3"));

			conn.add(s2, RDF.TYPE, tC);
			conn.add(s2, p, vf.createLiteral("s2-p-1"));
			conn.add(s2, p, vf.createLiteral("s2-p-2"));
			conn.add(s2, p, vf.createLiteral("s2-p-3"));
			conn.add(s2, q, vf.createLiteral("s2-q-1"));
			conn.add(s2, q, vf.createLiteral("s2-q-2"));
			conn.add(s2, q, vf.createLiteral("s2-q-3"));
		}
	}
}
