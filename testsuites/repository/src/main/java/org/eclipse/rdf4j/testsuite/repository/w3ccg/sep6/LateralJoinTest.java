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
	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	public String q1 = "PREFIX : <http://example/>\n"
			+ "SELECT * {\n"
			+ "    ?s a :T .\n"
			+ "    LATERAL { SELECT * { ?s :p ?p } ORDER BY ?p LIMIT 2 }\n"
			+ "    LATERAL { SELECT * { ?s :q ?q } ORDER BY ?q LIMIT 2 }\n"
			+ "} ORDER BY ?s ?p ?q\n" + "";

	public String q2 = "PREFIX : <http://example/>\n"
			+ "SELECT * {\n"
			+ "    ?s a :T .\n"
			+ "    { LATERAL { SELECT * { ?s :p ?p } ORDER BY ?p LIMIT 2 } }\n"
			+ "    UNION \n"
			+ "    { LATERAL { SELECT * { ?s :q ?q } ORDER BY ?q LIMIT 2 } }\n"
			+ "}";

	public String q2a = "PREFIX : <http://example/>\n"
			+ "\n"
			+ "SELECT * {\n"
			+ "    ?s a :T .\n"
			+ "    LATERAL {\n"
			+ "      { SELECT * { ?s :p ?p } ORDER BY ?p LIMIT 2 }\n"
			+ "      UNION\n"
			+ "      { SELECT * { ?s :q ?q } ORDER BY ?q LIMIT 2 }\n"
			+ "    }\n"
			+ "} ORDER BY ?s ?p ?q\n" + "";

	public String q3 = "PREFIX : <http://example/>\n"
			+ "\n"
			+ "SELECT * {\n"
			+ "    ?s a :T .\n"
			+ "    { LATERAL { SELECT * { ?s :p ?o } ORDER BY ?o LIMIT 2 } BIND(\"A\" AS ?x) }\n"
			+ "    UNION \n"
			+ "    { LATERAL { SELECT * { ?s :q ?o } ORDER BY ?o LIMIT 2 } BIND(\"B\" AS ?x) }\n"
			+ "}ORDER BY ?s ?o ?x\n"
			+ "";

	private Repository repository;

	private RepositoryConnection conn;

	private ValueFactory vf;

	private IRI s1;

	private IRI s2;

	private IRI tC;

	private IRI p;

	private IRI q;

	/**
	 * ----------------------------- | s | p | q | ============================= | :s1 | "s1-p-1" | "s1-q-1" | | :s1 |
	 * "s1-p-1" | "s1-q-2" | | :s1 | "s1-p-2" | "s1-q-1" | | :s1 | "s1-p-2" | "s1-q-2" | | :s2 | "s2-p-1" | "s2-q-1" | |
	 * :s2 | "s2-p-1" | "s2-q-2" | | :s2 | "s2-p-2" | "s2-q-1" | | :s2 | "s2-p-2" | "s2-q-2" |
	 * -----------------------------
	 *
	 * @throws Exception
	 */
	@Test
	public void q1() throws Exception {
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
	 * ----------------------------- | s | p | q | ============================= | :s1 | | "s1-q-1" | | :s1 | | "s1-q-2"
	 * | | :s1 | "s1-p-1" | | | :s1 | "s1-p-2" | | | :s2 | | "s2-q-1" | | :s2 | | "s2-q-2" | | :s2 | "s2-p-1" | | | :s2
	 * | "s2-p-2" | | -----------------------------
	 *
	 * @throws Exception
	 */
	@Test
	public void q2() throws Exception {
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
	 * | s | p | q | ============================= | :s1 | | "s1-q-1" | | :s1 | | "s1-q-2" | | :s2 | | "s2-q-1" | | :s2
	 * | | "s2-q-2" | | :s1 | "s1-p-1" | | | :s1 | "s1-p-2" | | | :s2 | "s2-p-1" | | | :s2 | "s2-p-2" | |
	 * -----------------------------
	 *
	 * @throws Exception
	 */
	@Test
	public void q2a() throws Exception {
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
	 * ------------------------ | s | o | x | ======================== | :s1 | "s1-p-1" | "A" | | :s1 | "s1-p-2" | "A" |
	 * | :s1 | "s1-q-1" | "B" | | :s1 | "s1-q-2" | "B" | | :s2 | "s2-p-1" | "A" | | :s2 | "s2-p-2" | "A" | | :s2 |
	 * "s2-q-1" | "B" | | :s2 | "s2-q-2" | "B" | ------------------------
	 *
	 * @throws Exception
	 */
	@Test
	public void q3() throws Exception {
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

	private void addTestData() throws RepositoryException {
		try (RepositoryConnection conn = repository.getConnection()) {
			String exns = "http://example/";
			s1 = vf.createIRI(exns, "s1");
			s2 = vf.createIRI(exns, "s2");
			tC = vf.createIRI(exns, "T");
			p = vf.createIRI(exns, "p");
			q = vf.createIRI(exns, "q");
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
