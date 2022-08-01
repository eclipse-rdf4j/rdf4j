/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.Distinct;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPropPathMisbehaviour {
	private SPARQLParser parser;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		parser = new SPARQLParser();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		parser = null;
	}

	/**
	 * reproduces GH-2343: the obj var of the nested statement pattern should be equal to the objVar of the ALP path
	 * that is using the PE
	 */
	@Test
	public void testGH2343() {
		String query1 = "select ?iri ?value where { \n" +
				"    ?iri (<urn:p>+) / <urn:q> ?value .\n" +
				"}";
		ParsedQuery q = parser.parseQuery(query1, "http://base.org/");

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue("expect queryroot", tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue("expect projection", tupleExpr instanceof Projection);
		Projection proj = (Projection) tupleExpr;

		assertTrue("expect join", proj.getArg() instanceof Join);
		assertTrue("expect left arg to be ALP", ((Join) proj.getArg()).getLeftArg() instanceof ArbitraryLengthPath);
		ArbitraryLengthPath alp = (ArbitraryLengthPath) ((Join) proj.getArg()).getLeftArg();

		assertTrue("expect single statement pattern in alp PE", alp.getPathExpression() instanceof StatementPattern);
		StatementPattern sp = (StatementPattern) alp.getPathExpression();
		assertNotNull(sp.getSubjectVar());

		assertTrue("expect subj var to be iri", "iri".equals(sp.getSubjectVar().getName()));

		assertTrue("expect obj var of the pattern to be same as the objVar of ALP",
				alp.getObjectVar().equals(sp.getObjectVar()));
	}

	@Test
	public void testGH3053() {
		String query1 = "select ?value where { \n" +
				"    <urn:non-existent> ^(<urn:p>*) / <urn:q>? ?value .\n" +
				"}";
		ParsedQuery q = parser.parseQuery(query1, "http://base.org/");

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue("expect queryroot", tupleExpr instanceof QueryRoot);
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue("expect projection", tupleExpr instanceof Projection);
		Projection proj = (Projection) tupleExpr;

		assertTrue("expect join", proj.getArg() instanceof Join);
		assertTrue("expect left arg to be ALP", ((Join) proj.getArg()).getLeftArg() instanceof ArbitraryLengthPath);
		ArbitraryLengthPath alp = (ArbitraryLengthPath) ((Join) proj.getArg()).getLeftArg();

		assertTrue("expect single statement pattern in alp PE", alp.getPathExpression() instanceof StatementPattern);
		StatementPattern sp = (StatementPattern) alp.getPathExpression();
		assertNotNull(sp.getSubjectVar());

		assertTrue("expect right arg to be Distinct", ((Join) proj.getArg()).getRightArg() instanceof Distinct);
		Distinct dist = (Distinct) ((Join) proj.getArg()).getRightArg();
		assertTrue("expect projection", dist.getArg() instanceof Projection);
		Projection proj2 = (Projection) dist.getArg();
		assertTrue("expect Union as projection arg", proj2.getArg() instanceof Union);
		assertTrue("expect Union Left arg to be ZeroPath",
				((Union) proj2.getArg()).getLeftArg() instanceof ZeroLengthPath);
		assertTrue("expect Union Right arg to be StatementPattern",
				((Union) proj2.getArg()).getRightArg() instanceof StatementPattern);
		assertTrue("expect projection to do NOT be a subQuery", !proj2.isSubquery());
	}
}
