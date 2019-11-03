/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository;

import static org.junit.Assert.assertFalse;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public abstract class CascadeValueExceptionTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	private static String queryStrLT = "SELECT * WHERE { ?s ?p ?o FILTER( !(\"2002\" < \"2007\"^^<http://www.w3.org/2001/XMLSchema#gYear>))}";

	private static String queryStrLE = "SELECT * WHERE { ?s ?p ?o FILTER( !(\"2002\" <= \"2007\"^^<http://www.w3.org/2001/XMLSchema#gYear>))}";

	private static String queryStrEQ = "SELECT * WHERE { ?s ?p ?o FILTER( !(\"2002\" = \"2007\"^^<http://www.w3.org/2001/XMLSchema#gYear>))}";

	private static String queryStrNE = "SELECT * WHERE { ?s ?p ?o FILTER( !(\"2002\" != \"2007\"^^<http://www.w3.org/2001/XMLSchema#gYear>))}";

	private static String queryStrGE = "SELECT * WHERE { ?s ?p ?o FILTER( !(\"2002\" >= \"2007\"^^<http://www.w3.org/2001/XMLSchema#gYear>))}";

	private static String queryStrGT = "SELECT * WHERE { ?s ?p ?o FILTER( !(\"2002\" > \"2007\"^^<http://www.w3.org/2001/XMLSchema#gYear>))}";

	private static String queryStrAltLT = "SELECT * WHERE { ?s ?p ?o FILTER( !(\"2002\"^^<http://www.w3.org/2001/XMLSchema#string> < \"2007\"^^<http://www.w3.org/2001/XMLSchema#gYear>))}";

	private static String queryStrAltLE = "SELECT * WHERE { ?s ?p ?o FILTER( !(\"2002\"^^<http://www.w3.org/2001/XMLSchema#string> <= \"2007\"^^<http://www.w3.org/2001/XMLSchema#gYear>))}";

	private static String queryStrAltEQ = "SELECT * WHERE { ?s ?p ?o FILTER( !(\"2002\"^^<http://www.w3.org/2001/XMLSchema#string> = \"2007\"^^<http://www.w3.org/2001/XMLSchema#gYear>))}";

	private static String queryStrAltNE = "SELECT * WHERE { ?s ?p ?o FILTER( !(\"2002\"^^<http://www.w3.org/2001/XMLSchema#string> != \"2007\"^^<http://www.w3.org/2001/XMLSchema#gYear>))}";

	private static String queryStrAltGE = "SELECT * WHERE { ?s ?p ?o FILTER( !(\"2002\"^^<http://www.w3.org/2001/XMLSchema#string> >= \"2007\"^^<http://www.w3.org/2001/XMLSchema#gYear>))}";

	private static String queryStrAltGT = "SELECT * WHERE { ?s ?p ?o FILTER( !(\"2002\"^^<http://www.w3.org/2001/XMLSchema#string> > \"2007\"^^<http://www.w3.org/2001/XMLSchema#gYear>))}";

	private RepositoryConnection conn;

	private Repository repository;

	@Test
	public void testValueExceptionLessThanPlain() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStrLT);
		try (TupleQueryResult evaluate = query.evaluate()) {
			assertFalse(evaluate.hasNext());
		}
	}

	@Test
	public void testValueExceptionLessThanOrEqualPlain() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStrLE);
		try (TupleQueryResult evaluate = query.evaluate()) {
			assertFalse(evaluate.hasNext());
		}
	}

	@Test
	public void testValueExceptionEqualPlain() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStrEQ);
		try (TupleQueryResult evaluate = query.evaluate()) {
			assertFalse(evaluate.hasNext());
		}
	}

	@Test
	public void testValueExceptionNotEqualPlain() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStrNE);
		try (TupleQueryResult evaluate = query.evaluate()) {
			assertFalse(evaluate.hasNext());
		}
	}

	@Test
	public void testValueExceptionGreaterThanOrEqualPlain() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStrGE);
		try (TupleQueryResult evaluate = query.evaluate()) {
			assertFalse(evaluate.hasNext());
		}
	}

	@Test
	public void testValueExceptionGreaterThanPlain() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStrGT);
		try (TupleQueryResult evaluate = query.evaluate()) {
			assertFalse(evaluate.hasNext());
		}
	}

	@Test
	public void testValueExceptionLessThanTyped() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStrAltLT);
		try (TupleQueryResult evaluate = query.evaluate()) {
			assertFalse(evaluate.hasNext());
		}
	}

	@Test
	public void testValueExceptionLessThanOrEqualTyped() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStrAltLE);
		try (TupleQueryResult evaluate = query.evaluate()) {
			assertFalse(evaluate.hasNext());
		}
	}

	@Test
	public void testValueExceptionEqualTyped() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStrAltEQ);
		try (TupleQueryResult evaluate = query.evaluate()) {
			assertFalse(evaluate.hasNext());
		}
	}

	@Test
	public void testValueExceptionNotEqualTyped() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStrAltNE);
		try (TupleQueryResult evaluate = query.evaluate()) {
			assertFalse(evaluate.hasNext());
		}
	}

	@Test
	public void testValueExceptionGreaterThanOrEqualTyped() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStrAltGE);
		try (TupleQueryResult evaluate = query.evaluate()) {
			assertFalse(evaluate.hasNext());
		}
	}

	@Test
	public void testValueExceptionGreaterThanTyped() throws Exception {
		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStrAltGT);
		try (TupleQueryResult evaluate = query.evaluate()) {
			assertFalse(evaluate.hasNext());
		}
	}

	@Before
	public void setUp() throws Exception {
		repository = createRepository();
		conn = repository.getConnection();
		conn.add(RDF.NIL, RDF.TYPE, RDF.LIST);
	}

	protected Repository createRepository() throws Exception {
		Repository repository = newRepository();
		repository.initialize();
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
	}
}
