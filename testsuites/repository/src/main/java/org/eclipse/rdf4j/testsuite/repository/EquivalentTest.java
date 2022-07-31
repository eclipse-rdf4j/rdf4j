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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public abstract class EquivalentTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	private static final ValueFactory vf = SimpleValueFactory.getInstance();

	private static final Literal xyz_simple = vf.createLiteral("xyz");

	private static final Literal xyz_en = vf.createLiteral("xyz", "en");

	private static final Literal xyz_EN = vf.createLiteral("xyz", "EN");

	private static final Literal xyz_string = vf.createLiteral("xyz", CoreDatatype.XSD.STRING);

	private static final Literal xyz_integer = vf.createLiteral("xyz", XSD.INTEGER);

	private static final Literal xyz_unknown = vf.createLiteral("xyz", vf.createIRI("http://example/unknown"));

	private static final IRI xyz_uri = vf.createIRI("http://example/xyz");

	private static final Literal abc_simple = vf.createLiteral("abc");

	private static final Literal abc_en = vf.createLiteral("abc", "en");

	private static final Literal abc_EN = vf.createLiteral("abc", "EN");

	private static final Literal abc_string = vf.createLiteral("abc", XSD.STRING);

	private static final Literal abc_integer = vf.createLiteral("abc", CoreDatatype.XSD.INTEGER);

	private static final Literal abc_unknown = vf.createLiteral("abc", vf.createIRI("http://example/unknown"));

	private static final IRI abc_uri = vf.createIRI("http://example/abc");

	private static final IRI t1 = vf.createIRI("http://example/t1");

	private static final IRI t2 = vf.createIRI("http://example/t2");

	private static final String IND = "?";

	private static final String EQ = "=";

	private static final String NEQ = "!=";

	private static final String PREFIX = "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"
			+ "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>" + "PREFIX ex:<http://example/>";

	private static final String matrix = "\"xyz\"	\"xyz\"	eq\n" + "\"xyz\"	\"xyz\"@en	neq\n"
			+ "\"xyz\"	\"xyz\"@EN	neq\n" + "\"xyz\"	\"xyz\"^^xsd:string	eq\n"
			+ "\"xyz\"	\"xyz\"^^xsd:integer	ind\n" + "\"xyz\"	\"xyz\"^^ex:unknown	ind\n"
			+ "\"xyz\"	_:xyz	neq\n" + "\"xyz\"	:xyz	neq\n" + "\"xyz\"@en	\"xyz\"	neq\n"
			+ "\"xyz\"@en	\"xyz\"@en	eq\n" + "\"xyz\"@en	\"xyz\"@EN	eq\n" + "\"xyz\"@en	\"xyz\"^^xsd:string	neq\n"
			+ "\"xyz\"@en	\"xyz\"^^xsd:integer	neq\n" + "\"xyz\"@en	\"xyz\"^^ex:unknown	neq\n"
			+ "\"xyz\"@en	_:xyz	neq\n" + "\"xyz\"@en	:xyz	neq\n" + "\"xyz\"@EN	\"xyz\"	neq\n"
			+ "\"xyz\"@EN	\"xyz\"@en	eq\n" + "\"xyz\"@EN	\"xyz\"@EN	eq\n" + "\"xyz\"@EN	\"xyz\"^^xsd:string	neq\n"
			+ "\"xyz\"@EN	\"xyz\"^^xsd:integer	neq\n" + "\"xyz\"@EN	\"xyz\"^^ex:unknown	neq\n"
			+ "\"xyz\"@EN	_:xyz	neq\n" + "\"xyz\"@EN	:xyz	neq\n" + "\"xyz\"^^xsd:string	\"xyz\"	eq\n"
			+ "\"xyz\"^^xsd:string	\"xyz\"@en	neq\n" + "\"xyz\"^^xsd:string	\"xyz\"@EN	neq\n"
			+ "\"xyz\"^^xsd:string	\"xyz\"^^xsd:string	eq\n" + "\"xyz\"^^xsd:string	\"xyz\"^^xsd:integer	ind\n"
			+ "\"xyz\"^^xsd:string	\"xyz\"^^ex:unknown	ind\n" + "\"xyz\"^^xsd:string	_:xyz	neq\n"
			+ "\"xyz\"^^xsd:string	:xyz	neq\n" + "\"xyz\"^^xsd:integer	\"xyz\"	ind\n"
			+ "\"xyz\"^^xsd:integer	\"xyz\"@en	neq\n" + "\"xyz\"^^xsd:integer	\"xyz\"@EN	neq\n"
			+ "\"xyz\"^^xsd:integer	\"xyz\"^^xsd:string	ind\n" + "\"xyz\"^^xsd:integer	\"xyz\"^^xsd:integer	eq\n"
			+ "\"xyz\"^^xsd:integer	\"xyz\"^^ex:unknown	ind\n" + "\"xyz\"^^xsd:integer	_:xyz	neq\n"
			+ "\"xyz\"^^xsd:integer	:xyz	neq\n" + "\"xyz\"^^ex:unknown	\"xyz\"	ind\n"
			+ "\"xyz\"^^ex:unknown	\"xyz\"@en	neq\n" + "\"xyz\"^^ex:unknown	\"xyz\"@EN	neq\n"
			+ "\"xyz\"^^ex:unknown	\"xyz\"^^xsd:string	ind\n" + "\"xyz\"^^ex:unknown	\"xyz\"^^xsd:integer	ind\n"
			+ "\"xyz\"^^ex:unknown	\"xyz\"^^ex:unknown	eq\n" + "\"xyz\"^^ex:unknown	_:xyz	neq\n"
			+ "\"xyz\"^^ex:unknown	:xyz	neq\n" + "_:xyz	\"xyz\"	neq\n" + "_:xyz	\"xyz\"@en	neq\n"
			+ "_:xyz	\"xyz\"@EN	neq\n" + "_:xyz	\"xyz\"^^xsd:string	neq\n" + "_:xyz	\"xyz\"^^xsd:integer	neq\n"
			+ "_:xyz	\"xyz\"^^ex:unknown	neq\n" + "_:xyz	_:xyz	eq\n" + "_:xyz	:xyz	neq\n"
			+ ":xyz	\"xyz\"	neq\n" + ":xyz	\"xyz\"@en	neq\n" + ":xyz	\"xyz\"@EN	neq\n"
			+ ":xyz	\"xyz\"^^xsd:string	neq\n" + ":xyz	\"xyz\"^^xsd:integer	neq\n"
			+ ":xyz	\"xyz\"^^ex:unknown	neq\n" + ":xyz	_:xyz	neq\n" + ":xyz	:xyz	eq\n"
			+ "\"xyz\"	\"abc\"		neq	\n" + "\"xyz\"	\"abc\"@en		neq	\n" + "\"xyz\"	\"abc\"@EN		neq	\n"
			+ "\"xyz\"	\"abc\"^^xsd:string		neq	\n" + "\"xyz\"	\"abc\"^^xsd:integer			ind\n"
			+ "\"xyz\"	\"abc\"^^:unknown			ind\n" + "\"xyz\"	_:abc		neq	\n"
			+ "\"xyz\"	:abc		neq	\n" + "\"xyz\"@en	\"abc\"		neq	\n" + "\"xyz\"@en	\"abc\"@en		neq	\n"
			+ "\"xyz\"@en	\"abc\"@EN		neq	\n" + "\"xyz\"@en	\"abc\"^^xsd:string		neq	\n"
			+ "\"xyz\"@en	\"abc\"^^xsd:integer		neq	\n" + "\"xyz\"@en	\"abc\"^^:unknown		neq	\n"
			+ "\"xyz\"@en	_:abc		neq	\n" + "\"xyz\"@en	:abc		neq	\n" + "\"xyz\"@EN	\"abc\"		neq	\n"
			+ "\"xyz\"@EN	\"abc\"@en		neq	\n" + "\"xyz\"@EN	\"abc\"@EN		neq	\n"
			+ "\"xyz\"@EN	\"abc\"^^xsd:string		neq	\n" + "\"xyz\"@EN	\"abc\"^^xsd:integer		neq	\n"
			+ "\"xyz\"@EN	\"abc\"^^:unknown		neq	\n" + "\"xyz\"@EN	_:abc		neq	\n"
			+ "\"xyz\"@EN	:abc		neq	\n" + "\"xyz\"^^xsd:string	\"abc\"		neq	\n"
			+ "\"xyz\"^^xsd:string	\"abc\"@en		neq	\n" + "\"xyz\"^^xsd:string	\"abc\"@EN		neq	\n"
			+ "\"xyz\"^^xsd:string	\"abc\"^^xsd:string		neq	\n"
			+ "\"xyz\"^^xsd:string	\"abc\"^^xsd:integer			ind\n"
			+ "\"xyz\"^^xsd:string	\"abc\"^^:unknown			ind\n" + "\"xyz\"^^xsd:string	_:abc		neq	\n"
			+ "\"xyz\"^^xsd:string	:abc		neq	\n" + "\"xyz\"^^xsd:integer	\"abc\"			ind\n"
			+ "\"xyz\"^^xsd:integer	\"abc\"@en		neq	\n" + "\"xyz\"^^xsd:integer	\"abc\"@EN		neq	\n"
			+ "\"xyz\"^^xsd:integer	\"abc\"^^xsd:string			ind\n"
			+ "\"xyz\"^^xsd:integer	\"abc\"^^xsd:integer			ind\n"
			+ "\"xyz\"^^xsd:integer	\"abc\"^^:unknown			ind\n" + "\"xyz\"^^xsd:integer	_:abc		neq	\n"
			+ "\"xyz\"^^xsd:integer	:abc		neq	\n" + "\"xyz\"^^:unknown	\"abc\"			ind\n"
			+ "\"xyz\"^^:unknown	\"abc\"@en		neq	\n" + "\"xyz\"^^:unknown	\"abc\"@EN		neq	\n"
			+ "\"xyz\"^^:unknown	\"abc\"^^xsd:string			ind\n"
			+ "\"xyz\"^^:unknown	\"abc\"^^xsd:integer			ind\n"
			+ "\"xyz\"^^:unknown	\"abc\"^^:unknown			ind\n" + "\"xyz\"^^:unknown	_:abc		neq	\n"
			+ "\"xyz\"^^:unknown	:abc		neq	\n" + "_:xyz	\"abc\"		neq	\n"
			+ "_:xyz	\"abc\"@en		neq	\n" + "_:xyz	\"abc\"@EN		neq	\n"
			+ "_:xyz	\"abc\"^^xsd:string		neq	\n" + "_:xyz	\"abc\"^^xsd:integer		neq	\n"
			+ "_:xyz	\"abc\"^^:unknown		neq	\n" + "_:xyz	_:abc		neq	\n" + "_:xyz	:abc		neq	\n"
			+ ":xyz	\"abc\"		neq	\n" + ":xyz	\"abc\"@en		neq	\n" + ":xyz	\"abc\"@EN		neq	\n"
			+ ":xyz	\"abc\"^^xsd:string		neq	\n" + ":xyz	\"abc\"^^xsd:integer		neq	\n"
			+ ":xyz	\"abc\"^^:unknown		neq	\n" + ":xyz	_:abc		neq	\n" + ":xyz	:abc		neq	";

	@Parameters(name = "{1} {0} {2}")
	public static Collection<Object[]> params() {
		LinkedList<Object[]> params = new LinkedList<>();
		for (String row : matrix.split("\n")) {
			if (row.contains("_:")) {
				continue;
			}
			String[] fields = row.split("\t", 3);
			if (fields[2].contains("neq")) {
				params.add(new Object[] { NEQ, fields[0], fields[1] });
			} else if (fields[2].contains("eq")) {
				params.add(new Object[] { EQ, fields[0], fields[1] });
			} else if (fields[2].contains("ind")) {
				params.add(new Object[] { IND, fields[0], fields[1] });
			} else {
				throw new AssertionError(row);
			}
		}
		return params;
	}

	private final Value term1;

	private final Value term2;

	private final String operator;

	private Repository repository;

	public EquivalentTest(String operator, String term1, String term2) {
		this.operator = operator;
		this.term1 = getTerm(term1);
		this.term2 = getTerm(term2);
	}

	@Before
	public void setUp() throws Exception {
		repository = createRepository();
		try (RepositoryConnection con = repository.getConnection()) {
			con.begin();
			con.clear();
			con.add(t1, RDF.VALUE, term1);
			con.add(t2, RDF.VALUE, term2);
			con.commit();
		}
	}

	@After
	public void tearDown() throws Exception {
		repository.shutDown();
		repository = null;
	}

	@Test
	public void testOperator() throws Throwable {
		assertEquals(operator, compare(term1, term2));
	}

	protected Repository createRepository() throws Exception {
		Repository repository = newRepository();
		try (RepositoryConnection con = repository.getConnection()) {
			con.begin();
			con.clear();
			con.clearNamespaces();
			con.commit();
		}
		return repository;
	}

	protected abstract Repository newRepository() throws Exception;

	private static Value getTerm(String label) {
		if (label.contains("xyz")) {
			if (label.contains("integer")) {
				return xyz_integer;
			}
			if (label.contains("string")) {
				return xyz_string;
			}
			if (label.contains("unknown")) {
				return xyz_unknown;
			}
			if (label.contains("en")) {
				return xyz_en;
			}
			if (label.contains("EN")) {
				return xyz_EN;
			}
			if (label.contains(":xyz")) {
				return xyz_uri;
			}
			if (label.contains("\"xyz\"")) {
				return xyz_simple;
			}
		}
		if (label.contains("abc")) {
			if (label.contains("integer")) {
				return abc_integer;
			}
			if (label.contains("string")) {
				return abc_string;
			}
			if (label.contains("unknown")) {
				return abc_unknown;
			}
			if (label.contains("en")) {
				return abc_en;
			}
			if (label.contains("EN")) {
				return abc_EN;
			}
			if (label.contains(":abc")) {
				return abc_uri;
			}
			if (label.contains("\"abc\"")) {
				return abc_simple;
			}
		}
		throw new AssertionError(label);
	}

	private String compare(Value term1, Value term2) throws Exception {
		boolean eq = evaluate(EQ);
		boolean neq = evaluate(NEQ);
		assertTrue(!eq || !neq);
		if (eq && !neq) {
			return EQ;
		}
		if (!eq && neq) {
			return NEQ;
		}
		if (!eq && !neq) {
			return IND;
		}
		throw new AssertionError();
	}

	private boolean evaluate(String op) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		String qry = PREFIX + "SELECT ?term1 ?term2 " + "WHERE {ex:t1 rdf:value ?term1 . ex:t2 rdf:value ?term2 "
				+ "FILTER (?term1 " + op + " ?term2)}";
		return evaluateSparql(qry);
	}

	private boolean evaluateSparql(String qry)
			throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		try (RepositoryConnection con = repository.getConnection()) {
			con.begin();
			TupleQuery query = con.prepareTupleQuery(QueryLanguage.SPARQL, qry);
			try (TupleQueryResult evaluate = query.evaluate()) {
				return evaluate.hasNext();
			} finally {
				con.commit();
			}
		}
	}
}
