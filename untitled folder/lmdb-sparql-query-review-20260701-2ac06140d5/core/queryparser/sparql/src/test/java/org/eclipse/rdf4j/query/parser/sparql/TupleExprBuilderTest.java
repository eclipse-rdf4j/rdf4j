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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.AggregateOperator;
import org.eclipse.rdf4j.query.algebra.Count;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Group;
import org.eclipse.rdf4j.query.algebra.GroupElem;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTQueryContainer;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTServiceGraphPattern;
import org.eclipse.rdf4j.query.parser.sparql.ast.ASTUpdateSequence;
import org.eclipse.rdf4j.query.parser.sparql.ast.ParseException;
import org.eclipse.rdf4j.query.parser.sparql.ast.SyntaxTreeBuilder;
import org.eclipse.rdf4j.query.parser.sparql.ast.TokenMgrError;
import org.eclipse.rdf4j.query.parser.sparql.ast.VisitorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * @author jeen
 */
public class TupleExprBuilderTest {

	private TupleExprBuilder builder;

	@BeforeEach
	public void setupBuilder() {
		builder = new TupleExprBuilder(SimpleValueFactory.getInstance());
	}

	@Test
	public void testSimpleAliasHandling() {
		String query = "SELECT (?a as ?b) WHERE { ?a ?x ?z }";

		try {
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
	public void testAggregateProjectionParentReferences() throws Exception {
		String query = "SELECT (COUNT(?s) AS ?count) WHERE { ?s ?p ?o }";

		ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(query);
		TupleExpr tupleExpr = builder.visit(qc, null);

		assertThat(tupleExpr).isInstanceOf(Projection.class);
		Projection projection = (Projection) tupleExpr;

		assertThat(projection.getArg()).isInstanceOf(Extension.class);
		Extension extension = (Extension) projection.getArg();

		assertThat(extension.getArg()).isInstanceOf(Group.class);
		Group group = (Group) extension.getArg();

		assertThat(group.getGroupElements()).hasSize(1);
		GroupElem groupElem = group.getGroupElements().get(0);
		AggregateOperator operator = groupElem.getOperator();

		assertThat(operator).isInstanceOf(Count.class);
		assertThat(operator.getParentNode()).isSameAs(groupElem);

		assertThat(extension.getElements()).hasSize(1);
		ExtensionElem extensionElem = extension.getElements().get(0);
		ValueExpr extExpr = extensionElem.getExpr();

		assertThat(extExpr).isInstanceOf(Count.class);
		assertThat(extExpr.getParentNode()).isSameAs(extensionElem);
		assertThat(extExpr).isNotSameAs(operator);
	}

	@Test
	public void testAggregateOrderByParentReferences() throws Exception {
		String query = "SELECT (COUNT(?s) AS ?count) WHERE { ?s ?p ?o } ORDER BY (COUNT(?s))";

		ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(query);
		TupleExpr tupleExpr = builder.visit(qc, null);

		AggregateOperatorContext context = collectAggregateOperators(tupleExpr);

		assertThat(context.groupOperators).isNotEmpty();
		assertThat(context.extensionOperators).hasSizeGreaterThanOrEqualTo(2);

		for (AggregateOperator operator : context.groupOperators) {
			assertThat(operator.getParentNode()).isInstanceOf(GroupElem.class);
		}

		for (AggregateOperator operator : context.extensionOperators) {
			assertThat(operator.getParentNode()).isInstanceOf(ExtensionElem.class);
			assertThat(context.containsSameInstanceInGroup(operator)).isFalse();
		}
	}

	@Test
	public void testAggregateHavingParentReferences() throws Exception {
		String query = "SELECT (COUNT(?s) AS ?count) WHERE { ?s ?p ?o } HAVING (COUNT(?s) > 1)";

		ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(query);
		TupleExpr tupleExpr = builder.visit(qc, null);

		AggregateOperatorContext context = collectAggregateOperators(tupleExpr);

		assertThat(context.groupOperators).isNotEmpty();
		assertThat(context.extensionOperators).hasSizeGreaterThanOrEqualTo(2);

		for (AggregateOperator operator : context.groupOperators) {
			assertThat(operator.getParentNode()).isInstanceOf(GroupElem.class);
		}

		for (AggregateOperator operator : context.extensionOperators) {
			assertThat(operator.getParentNode()).isInstanceOf(ExtensionElem.class);
			assertThat(context.containsSameInstanceInGroup(operator)).isFalse();
		}

		Filter filter = findNode(tupleExpr, Filter.class);
		assertThat(filter).isNotNull();
	}

	@Test
	public void testAggregateGroupConditionParentReferences() throws Exception {
		String query = "SELECT (COUNT(?s) AS ?count) WHERE { ?s ?p ?o } GROUP BY (COUNT(?s) AS ?groupCount)";

		ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(query);
		TupleExpr tupleExpr = builder.visit(qc, null);

		AggregateOperatorContext context = collectAggregateOperators(tupleExpr);
		assertThat(context.groupOperators).isNotEmpty();
		assertThat(context.extensionOperators).isNotEmpty();

		for (AggregateOperator operator : context.groupOperators) {
			assertThat(operator.getParentNode()).isInstanceOf(GroupElem.class);
		}

		for (AggregateOperator operator : context.extensionOperators) {
			assertThat(operator.getParentNode()).isInstanceOf(ExtensionElem.class);
			assertThat(context.containsSameInstanceInGroup(operator)).isFalse();
		}

		ExtensionElem groupAliasExtension = findExtensionElem(tupleExpr, "groupCount");
		assertThat(groupAliasExtension).isNotNull();
		assertThat(groupAliasExtension.getExpr()).isInstanceOf(AggregateOperator.class);
		AggregateOperator groupAliasOperator = (AggregateOperator) groupAliasExtension.getExpr();
		assertThat(groupAliasOperator.getParentNode()).isSameAs(groupAliasExtension);
		assertThat(context.containsSameInstanceInGroup(groupAliasOperator)).isFalse();
	}

	@Test
	public void testBindVarReuseHandling() {
		String query = "SELECT * WHERE { ?s ?p ?o. BIND(<foo:bar> as ?o) }";

		assertThatExceptionOfType(VisitorException.class).isThrownBy(() -> {
			ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(query);
			builder.visit(qc, null);
		}).withMessageContaining("BIND clause alias 'o' was previously used");
	}

	@Test
	public void testBindVarReuseHandling2() {
		String query = "SELECT * WHERE { { ?s ?p ?o } BIND(<foo:bar> as ?o) }";

		assertThatExceptionOfType(VisitorException.class).isThrownBy(() -> {
			ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(query);
			builder.visit(qc, null);
		}).withMessageContaining("BIND clause alias 'o' was previously used");
	}

	@Test
	public void testBindVarReuseHandling3() {
		String query = "SELECT * WHERE {  BIND(<foo:bar> as ?o) ?s ?p ?o. }";

		ASTQueryContainer qc;
		try {
			qc = SyntaxTreeBuilder.parseQuery(query);
			builder.visit(qc, null);
		} catch (Exception e) {
			fail("BIND alias before reuse in BGP should be allowed");
		}
	}

	@Test
	public void testAskQuerySolutionModifiers() {
		String query = "ASK WHERE { ?foo ?bar ?baz . } ORDER BY ?foo LIMIT 1";

		try {
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
	public void testServiceGraphPatternChopping() {

		// just for construction
		Service service = new Service(Var.of(null, null, false, false), new SingletonSet(), "", null, null, false);

		service.setExpressionString("SERVICE <a> { ?s ?p ?o }");
		assertEquals("?s ?p ?o", service.getServiceExpressionString());

		service.setExpressionString("SERVICE <a> {?s ?p ?o}");
		assertEquals("?s ?p ?o", service.getServiceExpressionString());

	}

	@Test
	public void testOtionalBindCoalesce() throws Exception {
		StringBuilder qb = new StringBuilder();
		qb.append("SELECT ?result \n");
		qb.append("WHERE { \n");
		qb.append("OPTIONAL {\n" +
				"        OPTIONAL {\n" +
				"            BIND(\"value\" AS ?foo)\n" +
				"        }\n" +
				"        BIND(COALESCE(?foo, \"no value\") AS ?result)\n" +
				"    }");
		qb.append(" } ");

		ASTQueryContainer qc = SyntaxTreeBuilder.parseQuery(qb.toString());
		TupleExpr result = builder.visit(qc, null);
		String expected = "Projection\n" +
				"   ProjectionElemList\n" +
				"      ProjectionElem \"result\"\n" +
				"   LeftJoin\n" +
				"      SingletonSet\n" +
				"      Extension\n" +
				"         LeftJoin\n" +
				"            SingletonSet\n" +
				"            Extension\n" +
				"               SingletonSet\n" +
				"               ExtensionElem (foo)\n" +
				"                  ValueConstant (value=\"value\")\n" +
				"         ExtensionElem (result)\n" +
				"            Coalesce\n" +
				"               Var (name=foo)\n" +
				"               ValueConstant (value=\"no value\")\n";
		assertEquals(expected.replace("\r\n", "\n"), result.toString().replace("\r\n", "\n"));
//		System.out.println(result);
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

	private AggregateOperatorContext collectAggregateOperators(TupleExpr tupleExpr) {
		AggregateOperatorContext context = new AggregateOperatorContext();
		tupleExpr.visit(new AbstractQueryModelVisitor<RuntimeException>() {
			@Override
			public void meet(GroupElem node) {
				AggregateOperator operator = node.getOperator();
				context.groupOperators.add(operator);
				context.groupIdentities.put(operator, Boolean.TRUE);
				super.meet(node);
			}

			@Override
			public void meet(ExtensionElem node) {
				ValueExpr expr = node.getExpr();
				if (expr instanceof AggregateOperator) {
					context.extensionOperators.add((AggregateOperator) expr);
				}
				super.meet(node);
			}
		});
		return context;
	}

	private <T> T findNode(TupleExpr tupleExpr, Class<T> type) {
		class Finder extends AbstractQueryModelVisitor<RuntimeException> {
			private T result;

			@Override
			protected void meetNode(org.eclipse.rdf4j.query.algebra.QueryModelNode node) {
				if (result != null) {
					return;
				}
				if (type.isInstance(node)) {
					result = type.cast(node);
				} else {
					super.meetNode(node);
				}
			}
		}

		Finder finder = new Finder();
		tupleExpr.visit(finder);
		return finder.result;
	}

	private ExtensionElem findExtensionElem(TupleExpr tupleExpr, String name) {
		class Finder extends AbstractQueryModelVisitor<RuntimeException> {
			private ExtensionElem result;

			@Override
			public void meet(ExtensionElem node) {
				if (result == null && name.equals(node.getName())) {
					result = node;
					return;
				}
				super.meet(node);
			}
		}

		Finder finder = new Finder();
		tupleExpr.visit(finder);
		return finder.result;
	}

	private static final class AggregateOperatorContext {
		private final List<AggregateOperator> groupOperators = new ArrayList<>();
		private final Map<AggregateOperator, Boolean> groupIdentities = new IdentityHashMap<>();
		private final List<AggregateOperator> extensionOperators = new ArrayList<>();

		boolean containsSameInstanceInGroup(AggregateOperator operator) {
			return groupIdentities.containsKey(operator);
		}
	}
}
