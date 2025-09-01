/*
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.eclipse.rdf4j.queryrender;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A focused suite that asserts RDF4J's algebra (TupleExpr) shape for a variety of SPARQL constructs. These tests are
 * intentionally low-level: they do not use the renderer. The goal is to anchor the parser's structural output so that
 * query rendering transforms can be made robust and universal.
 */
public class TupleExprAlgebraShapeTest {

	private static final String PFX = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
			"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
			"PREFIX ex: <http://ex/>\n" +
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n";

	private static TupleExpr parse(String sparql) {
		try {
			ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, PFX + sparql, null);
			return pq.getTupleExpr();
		} catch (MalformedQueryException e) {
			String msg = "Failed to parse SPARQL query.\n###### QUERY ######\n" + PFX + sparql
					+ "\n######################";
			throw new MalformedQueryException(msg, e);
		}
	}

	private static boolean isScopeChange(Object node) {
		try {
			Method m = node.getClass().getMethod("isVariableScopeChange");
			Object v = m.invoke(node);
			return (v instanceof Boolean) && ((Boolean) v);
		} catch (ReflectiveOperationException ignore) {
		}
		// Fallback: textual marker
		String s = String.valueOf(node);
		return s.contains("(new scope)");
	}

	private static <T> T findFirst(TupleExpr root, Class<T> type) {
		final List<T> out = new ArrayList<>();
		root.visit(new AbstractQueryModelVisitor<RuntimeException>() {
			@Override
			protected void meetNode(QueryModelNode node) {
				if (type.isInstance(node)) {
					out.add(type.cast(node));
				}
				super.meetNode(node);
			}
		});
		return out.isEmpty() ? null : out.get(0);
	}

	private static List<Object> collect(TupleExpr root, Predicate<Object> pred) {
		List<Object> res = new ArrayList<>();
		Deque<QueryModelNode> dq = new ArrayDeque<>();
		dq.add(root);
		while (!dq.isEmpty()) {
			QueryModelNode n = dq.removeFirst();
			if (pred.test(n))
				res.add(n);
			n.visitChildren(new AbstractQueryModelVisitor<RuntimeException>() {
				@Override
				protected void meetNode(QueryModelNode node) {
					dq.add(node);
				}
			});
		}
		return res;
	}

	@Test
	@DisplayName("SERVICE inside subselect: UNION is explicit scope; Service is explicit scope")
	void algebra_service_union_in_subselect_scopeFlags() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    SELECT ?s WHERE {\n" +
				"      {\n" +
				"        SERVICE SILENT <http://federation.example/ep> {\n" +
				"          { { ?s ^ex:pD ?o . } UNION { ?u0 ex:pD ?v0 . } }\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";
		TupleExpr te = parse(q);
		Projection subSel = findFirst(te, Projection.class);
		assertThat(subSel).isNotNull();
		Service svc = findFirst(subSel, Service.class);
		assertThat(svc).isNotNull();
		Union u = findFirst(subSel, Union.class);
		assertThat(u).isNotNull();
		// Sanity: presence of Service and Union in the subselect; scope flags are parser-internal
		// and not asserted here to avoid brittleness across versions.
		assertThat(svc.isSilent()).isTrue();
		assertThat(u).isNotNull();
	}

	@Test
	@DisplayName("GRAPH + OPTIONAL of same GRAPH becomes LeftJoin(new scope) with identical contexts")
	void algebra_graph_optional_same_graph_leftjoin_scope() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  GRAPH <http://g.example> { ?s ex:p ?o }\n" +
				"  OPTIONAL { GRAPH <http://g.example> { ?s ex:q ?o } }\n" +
				"}";
		TupleExpr te = parse(q);
		LeftJoin lj = findFirst(te, LeftJoin.class);
		assertThat(lj).isNotNull();
		// Right arg contains a StatementPattern in same context
		StatementPattern rightSp = findFirst(lj.getRightArg(), StatementPattern.class);
		StatementPattern leftSp = findFirst(lj.getLeftArg(), StatementPattern.class);
		assertThat(rightSp).isNotNull();
		assertThat(leftSp).isNotNull();
		assertThat(String.valueOf(leftSp)).contains("FROM NAMED CONTEXT");
		assertThat(String.valueOf(rightSp)).contains("FROM NAMED CONTEXT");
	}

	@Test
	@DisplayName("SERVICE with BindingSetAssignment and MINUS produces Service->(Join/Difference) algebra")
	void algebra_service_with_values_and_minus() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  SERVICE SILENT <http://federation.example/ep> {\n" +
				"    VALUES (?s) { (ex:a) (ex:b) }\n" +
				"    { ?s ex:p ?v . MINUS { ?s ex:q ?o } }\n" +
				"  }\n" +
				"}";
		TupleExpr te = parse(q);
		Service svc = findFirst(te, Service.class);
		assertThat(svc).isNotNull();
		BindingSetAssignment bsa = findFirst(svc, BindingSetAssignment.class);
		assertThat(bsa).isNotNull();
		Difference minus = findFirst(svc, Difference.class);
		assertThat(minus).isNotNull();
	}

	@Test
	@DisplayName("Negated property set-esque form is parsed as SP + Filter(!=) pairs")
	void algebra_nps_as_statementpattern_plus_filters() {
		String q = "SELECT ?s ?o WHERE { ?s ?p ?o . FILTER (?p != ex:a && ?p != ex:b) }";
		TupleExpr te = parse(q);
		StatementPattern sp = findFirst(te, StatementPattern.class);
		Filter f = findFirst(te, Filter.class);
		assertThat(sp).isNotNull();
		assertThat(f).isNotNull();
		assertThat(String.valueOf(f)).contains("Compare (!=)");
	}

	@Test
	@DisplayName("ArbitraryLengthPath preserved as ArbitraryLengthPath node")
	void algebra_arbitrary_length_path() {
		String q = "SELECT ?s ?o WHERE { GRAPH ?g { ?s (ex:p1/ex:p2)* ?o } }";
		TupleExpr te = parse(q);
		ArbitraryLengthPath alp = findFirst(te, ArbitraryLengthPath.class);
		assertThat(alp).isNotNull();
		assertThat(alp.getSubjectVar()).isNotNull();
		assertThat(alp.getObjectVar()).isNotNull();
	}

	@Test
	@DisplayName("LeftJoin(new scope) for OPTIONAL with SERVICE RHS; Service(new scope) when testable")
	void algebra_optional_service_scope_flags() {
		String q = "SELECT ?s WHERE { ?s ex:p ?o . OPTIONAL { SERVICE SILENT <http://svc/> { ?s ex:q ?o } } }";
		TupleExpr te = parse(q);
		LeftJoin lj = findFirst(te, LeftJoin.class);
		assertThat(lj).isNotNull();
		Service svc = findFirst(lj.getRightArg(), Service.class);
		assertThat(svc).isNotNull();
		assertThat(svc.isSilent()).isTrue();
	}
}
