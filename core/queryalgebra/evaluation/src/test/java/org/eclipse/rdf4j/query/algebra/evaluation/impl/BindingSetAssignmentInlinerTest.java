/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.model.util.Values.iri;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.Bound;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerTest;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.BindingSetAssignmentInlinerOptimizer;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.jupiter.api.Test;

/**
 * @author Jeen
 */
public class BindingSetAssignmentInlinerTest extends QueryOptimizerTest {

	@Test
	public void testOptimizeAssignsVars() {
		String query = "select * \n"
				+ "where { values ?z { <urn:z1> } \n"
				+ "        ?x <urn:pred1> ?y ; \n"
				+ "           (<urn:pred2>/<urn:pred3>)* ?z . \n"
				+ "}";

		ParsedTupleQuery parsedQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);

		QueryOptimizer optimizer = getOptimizer();
		optimizer.optimize(parsedQuery.getTupleExpr(), new SimpleDataset(), EmptyBindingSet.getInstance());

		TupleExpr optimizedTreeRoot = parsedQuery.getTupleExpr();

		TupleExpr optimizedTree = ((QueryRoot) optimizedTreeRoot).getArg();

		assertThat(optimizedTree).isInstanceOf(Projection.class);

		Projection projection = (Projection) optimizedTree;

		Join join = (Join) projection.getArg();
		assertThat(join.getRightArg()).isInstanceOf(ArbitraryLengthPath.class);

		ArbitraryLengthPath path = (ArbitraryLengthPath) join.getRightArg();
		assertThat(path.getObjectVar().getName()).isEqualTo("z");
		assertThat(path.getObjectVar().getValue()).isEqualTo(iri("urn:z1"));
	}

	@Test
	public void testEmptyValues() {
		String query = "select * \n"
				+ "where { values ?z { } \n"
				+ "        ?x <urn:pred1> ?y ; \n"
				+ "           (<urn:pred2>/<urn:pred3>)* ?z . \n"
				+ "}";

		ParsedTupleQuery parsedQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);

		QueryOptimizer optimizer = getOptimizer();
		optimizer.optimize(parsedQuery.getTupleExpr(), new SimpleDataset(), EmptyBindingSet.getInstance());

		TupleExpr optimizedTreeRoot = parsedQuery.getTupleExpr();
		TupleExpr optimizedTree = ((QueryRoot) optimizedTreeRoot).getArg();
		assertThat(optimizedTree).isInstanceOf(Projection.class);

		Projection projection = (Projection) optimizedTree;

		Join join = (Join) projection.getArg();
		assertThat(join.getRightArg()).isInstanceOf(ArbitraryLengthPath.class);

		ArbitraryLengthPath path = (ArbitraryLengthPath) join.getRightArg();
		assertThat(path.getObjectVar().getName()).isEqualTo("z");
		assertThat(path.getObjectVar().getValue()).isNull();
	}

	@Test
	public void testOptimize_MultipleValues() {
		String query = "select * \n"
				+ "where { values ?z { <urn:z1> <urn:z2> } \n"
				+ "        ?x <urn:pred1> ?y ; \n"
				+ "           (<urn:pred2>/<urn:pred3>)* ?z . \n"
				+ "}";

		ParsedTupleQuery parsedQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);

		QueryOptimizer optimizer = getOptimizer();
		optimizer.optimize(parsedQuery.getTupleExpr(), new SimpleDataset(), EmptyBindingSet.getInstance());

		TupleExpr optimizedTreeRoot = parsedQuery.getTupleExpr();
		TupleExpr optimizedTree = ((QueryRoot) optimizedTreeRoot).getArg();
		assertThat(optimizedTree).isInstanceOf(Projection.class);

		Projection projection = (Projection) optimizedTree;

		Join join = (Join) projection.getArg();
		assertThat(join.getRightArg()).isInstanceOf(ArbitraryLengthPath.class);

		ArbitraryLengthPath path = (ArbitraryLengthPath) join.getRightArg();
		assertThat(path.getObjectVar().getName()).isEqualTo("z");
		assertThat(path.getObjectVar().getValue()).isNull();
	}

	@Test
	public void testOptimize_leftJoin() {
		String query = "PREFIX : <http://example.org/> \n"
				+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n"
				+ "SELECT ?s ?o1 ?o2\n"
				+ "{\n"
				+ "  ?s ?p1 ?o1 \n"
				+ "  OPTIONAL { ?s foaf:knows ?o2 }\n"
				+ "} VALUES (?o2) {\n"
				+ " (:b)\n"
				+ "}";
		ParsedTupleQuery parsedQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);

		QueryOptimizer optimizer = getOptimizer();
		optimizer.optimize(parsedQuery.getTupleExpr(), new SimpleDataset(), EmptyBindingSet.getInstance());

		TupleExpr optimizedTreeRoot = parsedQuery.getTupleExpr();
		TupleExpr optimizedTree = ((QueryRoot) optimizedTreeRoot).getArg();

		assertThat(optimizedTree).isInstanceOf(Projection.class);

		Projection projection = (Projection) optimizedTree;

		Join join = (Join) projection.getArg();
		assertThat(join.getRightArg()).isInstanceOf(LeftJoin.class);
		LeftJoin optional = (LeftJoin) join.getRightArg();

		Var o2 = ((StatementPattern) optional.getRightArg()).getObjectVar();
		assertThat(o2.getName()).isEqualTo("o2");
		assertThat(o2.getValue()).isNull();
	}

	/**
	 * @see <a href="https://github.com/eclipse/rdf4j/issues/3091">GH-3091</a>
	 */
	@Test
	public void testOptimize_LeftJoinWithValuesInScope() {
		String query = "SELECT ?datasetBound {\n"
				+ "  OPTIONAL {\n"
				+ "    VALUES(?dataset ?uriSpace) {\n"
				+ "       (<http://example.org/void.ttl#FOAF> \"http://xmlns.com/foaf/0.1/\")  \n"
				+ "    }\n"
				+ "    FILTER(STRSTARTS(STR(<http://example.com>), ?uriSpace))\n"
				+ "  }\n"
				+ "  BIND(BOUND(?dataset) as ?datasetBound)\n"
				+ "}";

		ParsedTupleQuery parsedQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);

		QueryOptimizer optimizer = getOptimizer();
		optimizer.optimize(parsedQuery.getTupleExpr(), new SimpleDataset(), EmptyBindingSet.getInstance());

		TupleExpr optimizedTreeRoot = parsedQuery.getTupleExpr();
		TupleExpr optimizedTree = ((QueryRoot) optimizedTreeRoot).getArg();

		assertThat(optimizedTree).isInstanceOf(Projection.class);
		Projection projection = (Projection) optimizedTree;

		Extension extension = (Extension) projection.getArg();
		assertThat(extension.getArg()).isInstanceOf(LeftJoin.class);
		ExtensionElem elem = extension.getElements().iterator().next();
		Bound bound = (Bound) elem.getExpr();

		Var datasetVar = bound.getArg();
		assertThat(datasetVar.getName()).isEqualTo("dataset");
		assertThat(datasetVar.getValue()).isNull();

	}

	@Test
	public void testOptimize_Union_OutOfScope() {
		String query = "SELECT * WHERE { VALUES ?s1 { <urn:a> } { ?s1 ?p1 ?o1 } UNION { ?s1 ?p1 ?o2 } }";

		ParsedTupleQuery parsedQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);

		QueryOptimizer optimizer = getOptimizer();
		optimizer.optimize(parsedQuery.getTupleExpr(), new SimpleDataset(), EmptyBindingSet.getInstance());

		TupleExpr optimizedTreeRoot = parsedQuery.getTupleExpr();
		TupleExpr optimizedTree = ((QueryRoot) optimizedTreeRoot).getArg();

		assertThat(optimizedTree).isInstanceOf(Projection.class);

		Projection projection = (Projection) optimizedTree;

		Join join = (Join) projection.getArg();
		assertThat(join.getRightArg()).isInstanceOf(Union.class);
		Union union = (Union) join.getRightArg();

		Var s1 = ((StatementPattern) union.getLeftArg()).getSubjectVar();
		assertThat(s1.getName()).isEqualTo("s1");
		assertThat(s1.getValue()).isNull();
		s1 = ((StatementPattern) union.getRightArg()).getSubjectVar();
		assertThat(s1.getName()).isEqualTo("s1");
		assertThat(s1.getValue()).isNull();
	}

	@Test
	public void testOptimize_Union_InScope() {
		String query = "SELECT * WHERE { { VALUES ?s1 { <urn:a> } ?s1 ?p1 ?o1 } UNION { ?s1 ?p1 ?o2 } }";

		ParsedTupleQuery parsedQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);

		QueryOptimizer optimizer = getOptimizer();
		optimizer.optimize(parsedQuery.getTupleExpr(), new SimpleDataset(), EmptyBindingSet.getInstance());

		TupleExpr optimizedTreeRoot = parsedQuery.getTupleExpr();
		TupleExpr optimizedTree = ((QueryRoot) optimizedTreeRoot).getArg();

		assertThat(optimizedTree).isInstanceOf(Projection.class);

		Projection projection = (Projection) optimizedTree;

		Union union = (Union) projection.getArg();

		Join join = (Join) union.getLeftArg();

		Var s1 = ((StatementPattern) join.getRightArg()).getSubjectVar();
		assertThat(s1.getName()).isEqualTo("s1");
		assertThat(s1.getValue()).isEqualTo(iri("urn:a"));
		s1 = ((StatementPattern) union.getRightArg()).getSubjectVar();
		assertThat(s1.getName()).isEqualTo("s1");
		assertThat(s1.getValue()).isNull();
	}

	@Test
	public void testOptimize_FilterNotExists() {
		String query = "SELECT * WHERE { VALUES ?s1 { <urn:a> } ?s1 ?p1 ?o1 . FILTER NOT EXISTS { ?s1 ?p2 ?o2 } }";

		ParsedTupleQuery parsedQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);

		QueryOptimizer optimizer = getOptimizer();
		optimizer.optimize(parsedQuery.getTupleExpr(), new SimpleDataset(), EmptyBindingSet.getInstance());

		TupleExpr optimizedTreeRoot = parsedQuery.getTupleExpr();
		TupleExpr optimizedTree = ((QueryRoot) optimizedTreeRoot).getArg();

		assertThat(optimizedTree).isInstanceOf(Projection.class);

		Projection projection = (Projection) optimizedTree;

		Filter filter = (Filter) projection.getArg();
		assertThat(filter.getArg()).isInstanceOf(Join.class);
		Join join = (Join) filter.getArg();

		Var s1 = ((StatementPattern) join.getRightArg()).getSubjectVar();
		assertThat(s1.getName()).isEqualTo("s1");
		assertThat(s1.getValue()).isEqualTo(iri("urn:a"));

		Not not = (Not) filter.getCondition();
		Exists exists = (Exists) not.getArg();
		StatementPattern sp = (StatementPattern) exists.getSubQuery();
		assertThat(sp.getSubjectVar().getName()).isEqualTo("s1");
		assertThat(sp.getSubjectVar().getValue()).isNull();
	}

	@Test
	public void testOptimize_Minus() {
		String query = "SELECT * WHERE { VALUES ?s1 { <urn:a> } ?s1 ?p1 ?o1 MINUS { ?s1 ?p2 ?o2 } }";

		ParsedTupleQuery parsedQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);

		QueryOptimizer optimizer = getOptimizer();
		optimizer.optimize(parsedQuery.getTupleExpr(), new SimpleDataset(), EmptyBindingSet.getInstance());

		TupleExpr optimizedTreeRoot = parsedQuery.getTupleExpr();
		TupleExpr optimizedTree = ((QueryRoot) optimizedTreeRoot).getArg();

		assertThat(optimizedTree).isInstanceOf(Projection.class);

		Projection projection = (Projection) optimizedTree;

		Difference difference = (Difference) projection.getArg();

		Join join = (Join) difference.getLeftArg();
		Var s1 = ((StatementPattern) join.getRightArg()).getSubjectVar();
		assertThat(s1.getName()).isEqualTo("s1");
		assertThat(s1.getValue()).isEqualTo(iri("urn:a"));

		StatementPattern sp = (StatementPattern) difference.getRightArg();
		assertThat(sp.getSubjectVar().getName()).isEqualTo("s1");
		assertThat(sp.getSubjectVar().getValue()).isNull();
	}

	@Override
	public QueryOptimizer getOptimizer() {
		return new BindingSetAssignmentInlinerOptimizer();
	}
}
