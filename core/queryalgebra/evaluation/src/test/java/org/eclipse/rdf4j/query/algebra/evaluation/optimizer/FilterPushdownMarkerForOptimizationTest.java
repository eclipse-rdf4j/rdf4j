/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.not;

import java.util.List;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.FilterPushdownMarkerForOptimization.MarkedUpStatementPattern;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.jupiter.api.Test;

public class FilterPushdownMarkerForOptimizationTest {

	@Test
	public void basicJoinWherePredicateIsUsedAsSubjectLater() {
		ParsedTupleQuery query = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"select * where {?a ?b ?c. "
						+ "?b ?d ?e}",
				null);

		TupleExpr expr = query.getTupleExpr();

		new FilterPushdownMarkerForOptimization().optimize(expr, null, null);

		assertThat(expr instanceof QueryRoot);
		QueryRoot qr = (QueryRoot) expr;

		assertThat(qr.getArg() instanceof Projection);
		Projection pro = (Projection) qr.getArg();

		assertThat(pro.getArg() instanceof Join);
		Join join = (Join) pro.getArg();

		assertThat(join.getLeftArg() instanceof MarkedUpStatementPattern);
		MarkedUpStatementPattern left = (MarkedUpStatementPattern) join.getLeftArg();
		assertThat(not(left.getSubjectIck().isIri()));
		assertThat(left.getSubjectIck().isResource());
		assertThat(not(left.getObjectIck().isIri()));
		assertThat(not(left.getObjectIck().isResource()));
		assertThat(join.getRightArg() instanceof MarkedUpStatementPattern);
		MarkedUpStatementPattern right = (MarkedUpStatementPattern) join.getRightArg();
		assertThat(right.getSubjectIck().isIri());
		assertThat(right.getSubjectIck().isResource());
	}

	@Test
	public void unionWithDifferentArms() {
		ParsedTupleQuery query = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"select * where {{?a ?b ?c. "
						+ "?b ?d ?e} union {?b ?d ?e}}",
				null);

		TupleExpr expr = query.getTupleExpr();

		new FilterPushdownMarkerForOptimization().optimize(expr, null, null);

		assertThat(expr instanceof QueryRoot);
		QueryRoot qr = (QueryRoot) expr;

		assertThat(qr.getArg() instanceof Projection);
		Projection pro = (Projection) qr.getArg();

		assertThat(pro.getArg() instanceof Union);
		Union union = (Union) pro.getArg();

		assertThat(union.getLeftArg() instanceof Union);
		Join join = (Join) union.getLeftArg();

		assertThat(join.getLeftArg() instanceof MarkedUpStatementPattern);
		MarkedUpStatementPattern left = (MarkedUpStatementPattern) join.getLeftArg();
		assertThat(left.getSubjectIck().isResource());
		assertThat(not(left.getSubjectIck().isIri()));
		assertThat(not(left.getObjectIck().isIri()));
		assertThat(not(left.getObjectIck().isResource()));
		assertThat(not(left.getContextIck().isIri()));
		assertThat(left.getObjectIck().isResource());
		assertThat(join.getRightArg() instanceof MarkedUpStatementPattern);
		MarkedUpStatementPattern right = (MarkedUpStatementPattern) join.getRightArg();
		assertThat(right.getSubjectIck().isIri());
		assertThat(right.getSubjectIck().isResource());

		assertThat(union.getRightArg() instanceof StatementPattern);
		assertThat(not(union.getRightArg() instanceof MarkedUpStatementPattern));
	}

	@Test
	public void unionWithMoreDifferentArms() {
		ParsedTupleQuery query = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"select * where {{?a ?b ?c. "
						+ "?b ?d ?e} union {?b ?d ?e . [] ?e ?d }}",
				null);

		TupleExpr expr = query.getTupleExpr();

		new FilterPushdownMarkerForOptimization().optimize(expr, null, null);

		assertThat(expr instanceof QueryRoot);
		QueryRoot qr = (QueryRoot) expr;

		assertThat(qr.getArg() instanceof Projection);
		Projection pro = (Projection) qr.getArg();

		assertThat(pro.getArg() instanceof Union);
		Union union = (Union) pro.getArg();

		assertThat(union.getLeftArg() instanceof Union);
		Join join = (Join) union.getLeftArg();

		assertThat(join.getLeftArg() instanceof MarkedUpStatementPattern);
		MarkedUpStatementPattern left = (MarkedUpStatementPattern) join.getLeftArg();
		assertThat(left.getSubjectIck().isResource());
		assertThat(not(left.getSubjectIck().isIri()));
		assertThat(not(left.getObjectIck().isIri()));
		assertThat(not(left.getObjectIck().isResource()));
		assertThat(not(left.getContextIck().isIri()));
		assertThat(left.getObjectIck().isResource());
		assertThat(join.getRightArg() instanceof MarkedUpStatementPattern);
		MarkedUpStatementPattern right = (MarkedUpStatementPattern) join.getRightArg();
		assertThat(right.getSubjectIck().isResource());
		assertThat(right.getSubjectIck().isIri());

		assertThat(union.getRightArg() instanceof StatementPattern);

		join = (Join) union.getLeftArg();

		assertThat(join.getLeftArg() instanceof MarkedUpStatementPattern);
		left = (MarkedUpStatementPattern) join.getLeftArg();
		assertThat(left.getSubjectIck().isResource());
		assertThat(not(left.getSubjectIck().isIri()));
		assertThat(not(left.getObjectIck().isIri()));
		assertThat(not(left.getObjectIck().isResource()));
		assertThat(not(left.getContextIck().isIri()));
		assertThat(join.getRightArg() instanceof MarkedUpStatementPattern);
		right = (MarkedUpStatementPattern) join.getRightArg();
		assertThat(not(right.getSubjectIck().isIri()));
		assertThat(not(right.getSubjectIck().isResource()));
		assertThat(right.getObjectIck().isIri());
		assertThat(right.getObjectIck().isResource());
	}

	@Test
	public void context() {
		ParsedTupleQuery query = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"select * where {?a ?b ?c. "
						+ "graph ?a {?b ?d ?e}}",
				null);

		TupleExpr expr = query.getTupleExpr();

		new FilterPushdownMarkerForOptimization().optimize(expr, null, null);

		assertThat(expr instanceof QueryRoot);
		QueryRoot qr = (QueryRoot) expr;

		assertThat(qr.getArg() instanceof Projection);
		Projection pro = (Projection) qr.getArg();

		assertThat(pro.getArg() instanceof Join);
		Join join = (Join) pro.getArg();

		assertThat(join.getLeftArg() instanceof MarkedUpStatementPattern);
		MarkedUpStatementPattern left = (MarkedUpStatementPattern) join.getLeftArg();
		assertThat(left.getSubjectIck().isResource());
		assertThat(not(left.getSubjectIck().isIri()));
		assertThat(not(left.getObjectIck().isIri()));
		assertThat(not(left.getObjectIck().isResource()));
		assertThat(not(left.getContextIck().isIri()));
		assertThat(left.getObjectIck().isResource());
		assertThat(join.getRightArg() instanceof MarkedUpStatementPattern);
		MarkedUpStatementPattern right = (MarkedUpStatementPattern) join.getRightArg();
		assertThat(right.getSubjectIck().isResource());
		assertThat(right.getSubjectIck().isIri());
		assertThat(right.getContextIck().isIri());
	}

	@Test
	public void values() {
		ParsedTupleQuery query = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"prefix ex:<https://example.org/> " +
						" select * where {values (?s) {(ex:1) (ex:2) } . ?s ?p ?o. }",
				null);

		TupleExpr expr = query.getTupleExpr();

		new FilterPushdownMarkerForOptimization().optimize(expr, null, null);

		assertThat(expr instanceof QueryRoot);
		QueryRoot qr = (QueryRoot) expr;

		assertThat(qr.getArg() instanceof Projection);
		Projection pro = (Projection) qr.getArg();

		assertThat(pro.getArg() instanceof Join);
		Join join = (Join) pro.getArg();

		assertThat(join.getLeftArg() instanceof BindingSetAssignment);

		assertThat(join.getRightArg() instanceof MarkedUpStatementPattern);
		MarkedUpStatementPattern right = (MarkedUpStatementPattern) join.getRightArg();
		assertThat(right.getSubjectIck().isResource());
		assertThat(right.getSubjectIck().isIri());
		assertThat(not(right.getObjectIck().isIri()));
		assertThat(not(right.getObjectIck().isResource()));
		assertThat(not(right.getContextIck().isResource()));
	}

	@Test
	public void valuesButAlsoLiteral() {
		ParsedTupleQuery query = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"prefix ex:<https://example.org/> " +
						" select * where {values (?o) {(ex:1) ('lalal') } . ?s ?p ?o. }",
				null);

		TupleExpr expr = query.getTupleExpr();

		new FilterPushdownMarkerForOptimization().optimize(expr, null, null);

		assertThat(expr instanceof QueryRoot);
		QueryRoot qr = (QueryRoot) expr;

		assertThat(qr.getArg() instanceof Projection);
		Projection pro = (Projection) qr.getArg();

		assertThat(pro.getArg() instanceof Join);
		Join join = (Join) pro.getArg();

		assertThat(join.getLeftArg() instanceof BindingSetAssignment);

		assertThat(join.getRightArg() instanceof MarkedUpStatementPattern);
		MarkedUpStatementPattern right = (MarkedUpStatementPattern) join.getRightArg();
		assertThat(right.getSubjectIck().isResource());
		assertThat(right.getSubjectIck().isIri());
		assertThat(not(right.getObjectIck().isIri()));
		assertThat(not(right.getObjectIck().isResource()));
		assertThat(not(right.getContextIck().isIri()));
	}

	@Test
	public void filterIsIri() {
		ParsedTupleQuery query = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"prefix ex:<https://example.org/> " +
						" select * where {?s ?p ?o . filter(isIri(?o))}",
				null);

		TupleExpr expr = query.getTupleExpr();

		new FilterPushdownMarkerForOptimization().optimize(expr, null, null);

		assertThat(expr instanceof QueryRoot);
		QueryRoot qr = (QueryRoot) expr;

		assertThat(qr.getArg() instanceof Projection);
		Projection pro = (Projection) qr.getArg();

		assertThat(pro.getArg() instanceof Filter);
		Filter filter = (Filter) pro.getArg();

		assertThat(filter.getArg() instanceof MarkedUpStatementPattern);
		MarkedUpStatementPattern left = (MarkedUpStatementPattern) filter.getArg();
		assertThat(left.getSubjectIck().isIri());
		assertThat(left.getSubjectIck().isResource());
		assertThat(left.getObjectIck().isIri());
		assertThat(left.getObjectIck().isResource());
	}

	@Test
	public void filterCompareToConstant() {
		ParsedTupleQuery query = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"prefix ex:<https://example.org/> " +
						" select * where {?s ?p ?o . filter(?o > 1)}",
				null);

		TupleExpr expr = query.getTupleExpr();

		new FilterPushdownMarkerForOptimization().optimize(expr, null, null);

		assertThat(expr instanceof QueryRoot);
		QueryRoot qr = (QueryRoot) expr;

		assertThat(qr.getArg() instanceof Projection);
		Projection pro = (Projection) qr.getArg();

		assertThat(pro.getArg() instanceof Filter);
		Filter filter = (Filter) pro.getArg();

		assertThat(filter.getArg() instanceof MarkedUpStatementPattern);
		MarkedUpStatementPattern left = (MarkedUpStatementPattern) filter.getArg();
		assertThat(left.getSubjectIck().isIri());
		assertThat(left.getSubjectIck().isResource());
		List<ValueExpr> objFil = left.getObjectIck().filters();
		assertThat(not(objFil.isEmpty()));
		assertThat(not(left.getObjectIck().isIri()));
	}

	@Test
	public void filterIsLiteral() {
		ParsedTupleQuery query = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"prefix ex:<https://example.org/> " +
						" select * where {?s ?p ?o . filter(isLiteral(?o))}",
				null);

		TupleExpr expr = query.getTupleExpr();

		new FilterPushdownMarkerForOptimization().optimize(expr, null, null);

		assertThat(expr instanceof QueryRoot);
		QueryRoot qr = (QueryRoot) expr;

		assertThat(qr.getArg() instanceof Projection);
		Projection pro = (Projection) qr.getArg();

		assertThat(pro.getArg() instanceof Filter);
		Filter filter = (Filter) pro.getArg();

		assertThat(filter.getArg() instanceof MarkedUpStatementPattern);
		MarkedUpStatementPattern left = (MarkedUpStatementPattern) filter.getArg();
		assertThat(left.getSubjectIck().isIri());
		assertThat(left.getSubjectIck().isResource());
		List<ValueExpr> objFil = left.getObjectIck().filters();
		assertThat(not(objFil.isEmpty()));
		assertThat(not(left.getObjectIck().isIri()));
	}
}
