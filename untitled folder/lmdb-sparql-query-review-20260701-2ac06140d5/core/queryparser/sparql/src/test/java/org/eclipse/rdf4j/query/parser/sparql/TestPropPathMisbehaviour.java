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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestPropPathMisbehaviour {
	private SPARQLParser parser;

	/**
	 */
	@BeforeEach
	public void setUp() {
		parser = new SPARQLParser();
	}

	/**
	 */
	@AfterEach
	public void tearDown() {
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
		ParsedQuery q = parser.parseQuery(query1, "http://example.org/");

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot, "expect queryroot");
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue(tupleExpr instanceof Projection, "expect projection");
		Projection proj = (Projection) tupleExpr;

		assertTrue(proj.getArg() instanceof Join, "expect join");
		assertTrue(((Join) proj.getArg()).getLeftArg() instanceof ArbitraryLengthPath, "expect left arg to be ALP");
		ArbitraryLengthPath alp = (ArbitraryLengthPath) ((Join) proj.getArg()).getLeftArg();

		assertTrue(alp.getPathExpression() instanceof StatementPattern, "expect single statement pattern in alp PE");
		StatementPattern sp = (StatementPattern) alp.getPathExpression();
		assertNotNull(sp.getSubjectVar());

		assertTrue("iri".equals(sp.getSubjectVar().getName()), "expect subj var to be iri");

		assertTrue(alp.getObjectVar().equals(sp.getObjectVar()),
				"expect obj var of the pattern to be same as the objVar of ALP");
	}

	@Test
	public void testGH3053() {
		String query1 = "select ?value where { \n" +
				"    <urn:non-existent> ^(<urn:p>*) / <urn:q>? ?value .\n" +
				"}";
		ParsedQuery q = parser.parseQuery(query1, "http://example.org/");

		assertNotNull(q);
		TupleExpr tupleExpr = q.getTupleExpr();
		assertTrue(tupleExpr instanceof QueryRoot, "expect queryroot");
		tupleExpr = ((QueryRoot) tupleExpr).getArg();
		assertTrue(tupleExpr instanceof Projection, "expect projection");
		Projection proj = (Projection) tupleExpr;

		assertTrue(proj.getArg() instanceof Join, "expect join");
		assertTrue(((Join) proj.getArg()).getLeftArg() instanceof ArbitraryLengthPath, "expect left arg to be ALP");
		ArbitraryLengthPath alp = (ArbitraryLengthPath) ((Join) proj.getArg()).getLeftArg();

		assertTrue(alp.getPathExpression() instanceof StatementPattern, "expect single statement pattern in alp PE");
		StatementPattern sp = (StatementPattern) alp.getPathExpression();
		assertNotNull(sp.getSubjectVar());

		assertTrue(((Join) proj.getArg()).getRightArg() instanceof Distinct, "expect right arg to be Distinct");
		Distinct dist = (Distinct) ((Join) proj.getArg()).getRightArg();
		assertTrue(dist.getArg() instanceof Projection, "expect projection");
		Projection proj2 = (Projection) dist.getArg();
		assertTrue(proj2.getArg() instanceof Union, "expect Union as projection arg");
		assertTrue(((Union) proj2.getArg()).getLeftArg() instanceof ZeroLengthPath,
				"expect Union Left arg to be ZeroPath");
		assertTrue(((Union) proj2.getArg()).getRightArg() instanceof StatementPattern,
				"expect Union Right arg to be StatementPattern");
		assertTrue(!proj2.isSubquery(), "expect projection to do NOT be a subQuery");
	}
}
