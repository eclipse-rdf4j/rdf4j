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
package org.eclipse.rdf4j.query.parser.sparql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTQueryContainer;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTServiceGraphPattern;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTUpdateSequence;
import org.eclipse.rdf4j.query.parser.sparql.ast.ParseException;
import org.eclipse.rdf4j.query.parser.sparql.ast.SyntaxTreeBuilder;
import org.eclipse.rdf4j.query.parser.sparql.ast.TokenMgrError;
import org.eclipse.rdf4j.query.parser.sparql.ast.VisitorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * @author jeen
 */
public class TupleExprBuilderTest {

	@Test
	public void testSimpleAliasHandling() {
		String query = "SELECT (?a as ?b) WHERE { ?a ?x ?z }";

		try {
			TupleExprBuilder builder = new TupleExprBuilder(SimpleValueFactory.getInstance());
			ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(query);
			TupleExpr result = builder.visit(qc, null);

			assertThat(result instanceof Projection).isTrue();

			Projection p = (Projection) result;
			assertThat(p.getArg()).isInstanceOf(Extension.class);
			Extension extension = (Extension) p.getArg();

			assertThat(extension.getElements()).hasSize(1)
					.allMatch(elem -> elem.getName().equals("b") && ((Var) elem.getExpr()).getName().equals("a"));

		} catch (Exception e) {
			e.printStackTrace();
			fail("should parse simple select query");
		}
	}

	@Test
	public void testAskQuerySolutionModifiers() {
		String query = "ASK WHERE { ?foo ?bar ?baz . } ORDER BY ?foo LIMIT 1";

		try {
			TupleExprBuilder builder = new TupleExprBuilder(SimpleValueFactory.getInstance());
			ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(query);
			TupleExpr result = builder.visit(qc, null);
			assertTrue(result instanceof Order);
		} catch (Exception e) {
			e.printStackTrace();
			fail("should parse ask query with solution modifiers");
		}

	}

	@Test
	public void testNegatedPathWithFixedObject() {
		String query = "ASK WHERE { ?s !<http://example.org/p> <http://example.org/o> . }";

		try {
			TupleExprBuilder builder = new TupleExprBuilder(SimpleValueFactory.getInstance());
			ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(query);
			TupleExpr result = builder.visit(qc, null);

			assertTrue(result instanceof Slice);
		} catch (Exception e) {
			e.printStackTrace();
			fail("should parse ask query with negated property path");
		}
	}

	/**
	 * Verifies that a missing close brace does not cause an endless loop. Timeout is set to avoid test itself endlessly
	 * looping. See SES-2389.
	 */
	@Test
	@Timeout(10)
	public void testMissingCloseBrace() {
		String query = "INSERT DATA { <urn:a> <urn:b> <urn:c> .";
		try {
			final ASTUpdateSequence us = SyntaxTreeBuilder.parseUpdateSequence(query);
			fail("should result in parse error");
		} catch (ParseException e) {
			// fall through, expected
		}
	}

	@Test
	public void testServiceGraphPatternStringDetection1() throws TokenMgrError, ParseException, VisitorException {

		String servicePattern = "SERVICE <foo:bar> { ?x <foo:baz> ?y }";

		StringBuilder qb = new StringBuilder();
		qb.append("SELECT * \n");
		qb.append("WHERE { \n");
		qb.append(" { ?s ?p ?o } \n");
		qb.append(" UNION \n");
		qb.append(" { ?p ?q ?r } \n");
		qb.append(servicePattern);
		qb.append("\n");
		qb.append(" FILTER (?s = <foo:bar>) ");
		qb.append(" } ");

		ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(qb.toString());

		ServiceNodeFinder f = new ServiceNodeFinder();
		f.visit(qc, null);

		assertTrue(f.getGraphPatterns().size() == 1);
		assertTrue(servicePattern.equals(f.getGraphPatterns().get(0)));
	}

	@Test
	public void testServiceGraphPatternStringDetection2() throws TokenMgrError, ParseException, VisitorException {

		String servicePattern = "SERVICE <foo:bar> \r\n { ?x <foo:baz> ?y. \r\n \r\n }";

		StringBuilder qb = new StringBuilder();
		qb.append("SELECT * \n");
		qb.append("WHERE { \n");
		qb.append(" { ?s ?p ?o } \n");
		qb.append(" UNION \n");
		qb.append(" { ?p ?q ?r } \n");
		qb.append(servicePattern);
		qb.append("\n");
		qb.append(" FILTER (?s = <foo:bar>) ");
		qb.append(" } ");

		ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(qb.toString());

		ServiceNodeFinder f = new ServiceNodeFinder();
		f.visit(qc, null);

		assertTrue(f.getGraphPatterns().size() == 1);
		assertTrue(servicePattern.equals(f.getGraphPatterns().get(0)));
	}

	@Test
	public void testServiceGraphPatternStringDetection3() throws TokenMgrError, ParseException, VisitorException {

		String servicePattern1 = "SERVICE <foo:bar> \n { ?x <foo:baz> ?y. }";
		String servicePattern2 = "SERVICE <foo:bar2> \n { ?x <foo:baz> ?y. }";

		StringBuilder qb = new StringBuilder();
		qb.append("SELECT * \n");
		qb.append("WHERE { \n");
		qb.append(servicePattern1);
		qb.append(" OPTIONAL { \n");
		qb.append(servicePattern2);
		qb.append("    } \n");
		qb.append(" } ");

		ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(qb.toString());

		ServiceNodeFinder f = new ServiceNodeFinder();
		f.visit(qc, null);

		assertTrue(f.getGraphPatterns().size() == 2);
		assertTrue(servicePattern1.equals(f.getGraphPatterns().get(0)));
		assertTrue(servicePattern2.equals(f.getGraphPatterns().get(1)));
	}

	@Test
	public void testServiceGraphPatternStringDetection4() throws TokenMgrError, ParseException, VisitorException {

		String servicePattern1 = "SERVICE <http://localhost:18080/openrdf/repositories/endpoint1> {  ?s ?p ?o1 . "
				+ "OPTIONAL {	SERVICE SILENT <http://invalid.endpoint.org/sparql> { ?s ?p2 ?o2 } } }";

		String servicePattern2 = "SERVICE SILENT <http://invalid.endpoint.org/sparql> { ?s ?p2 ?o2 }";

		StringBuilder qb = new StringBuilder();
		qb.append("SELECT * \n");
		qb.append("WHERE { \n");
		qb.append(servicePattern1);
		qb.append(" } ");

		ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(qb.toString());

		ServiceNodeFinder f = new ServiceNodeFinder();
		f.visit(qc, null);

		assertTrue(f.getGraphPatterns().size() == 2);
		assertTrue(servicePattern1.equals(f.getGraphPatterns().get(0)));
		assertTrue(servicePattern2.equals(f.getGraphPatterns().get(1)));
	}

	@Test
	public void testServiceGraphPatternChopping() throws Exception {

		// just for construction
		Service service = new Service(new Var(null, null, false, false), new SingletonSet(), "", null, null, false);

		service.setExpressionString("SERVICE <a> { ?s ?p ?o }");
		assertEquals("?s ?p ?o", service.getServiceExpressionString());

		service.setExpressionString("SERVICE <a> {?s ?p ?o}");
		assertEquals("?s ?p ?o", service.getServiceExpressionString());

	}

	private class ServiceNodeFinder extends AbstractASTVisitor {

		private final List<String> graphPatterns = new ArrayList<>();

		@Override
		public Object visit(ASTServiceGraphPattern node, Object data) throws VisitorException {
			graphPatterns.add(node.getPatternString());
			return super.visit(node, data);
		}

		public List<String> getGraphPatterns() {
			return graphPatterns;
		}
	}
}
