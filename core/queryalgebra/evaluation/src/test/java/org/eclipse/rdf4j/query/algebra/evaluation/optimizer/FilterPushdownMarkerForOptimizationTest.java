package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.not;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
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
		assertThat(left.isSubjectIsResource());
		assertThat(not(left.isSubjectIsIri()));
		assertThat(not(left.isObjectIsIri()));
		assertThat(not(left.isContextIsIri()));
		assertThat(join.getRightArg() instanceof MarkedUpStatementPattern);
		MarkedUpStatementPattern right = (MarkedUpStatementPattern) join.getRightArg();
		assertThat(right.isSubjectIsResource());
		assertThat(right.isSubjectIsIri());
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
		assertThat(left.isSubjectIsResource());
		assertThat(not(left.isSubjectIsIri()));
		assertThat(not(left.isObjectIsIri()));
		assertThat(not(left.isContextIsIri()));
		assertThat(join.getRightArg() instanceof MarkedUpStatementPattern);
		MarkedUpStatementPattern right = (MarkedUpStatementPattern) join.getRightArg();
		assertThat(right.isSubjectIsResource());
		assertThat(right.isSubjectIsIri());

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
		assertThat(left.isSubjectIsResource());
		assertThat(not(left.isSubjectIsIri()));
		assertThat(not(left.isObjectIsIri()));
		assertThat(not(left.isContextIsIri()));
		assertThat(join.getRightArg() instanceof MarkedUpStatementPattern);
		MarkedUpStatementPattern right = (MarkedUpStatementPattern) join.getRightArg();
		assertThat(right.isSubjectIsResource());
		assertThat(right.isSubjectIsIri());

		assertThat(union.getRightArg() instanceof StatementPattern);

		join = (Join) union.getLeftArg();

		assertThat(join.getLeftArg() instanceof MarkedUpStatementPattern);
		left = (MarkedUpStatementPattern) join.getLeftArg();
		assertThat(left.isSubjectIsResource());
		assertThat(not(left.isSubjectIsIri()));
		assertThat(not(left.isObjectIsIri()));
		assertThat(not(left.isContextIsIri()));
		assertThat(join.getRightArg() instanceof MarkedUpStatementPattern);
		right = (MarkedUpStatementPattern) join.getRightArg();
		assertThat(not(right.isSubjectIsResource()));
		assertThat(not(right.isSubjectIsIri()));
		assertThat(right.isObjectIsIri());
		assertThat(right.isObjectIsResource());
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
		assertThat(left.isSubjectIsResource());
		assertThat(not(left.isSubjectIsIri()));
		assertThat(not(left.isObjectIsIri()));
		assertThat(not(left.isContextIsIri()));
		assertThat(join.getRightArg() instanceof MarkedUpStatementPattern);
		MarkedUpStatementPattern right = (MarkedUpStatementPattern) join.getRightArg();
		assertThat(right.isSubjectIsResource());
		assertThat(right.isSubjectIsIri());
		assertThat(right.isContextIsIri());
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
		assertThat(right.isSubjectIsResource());
		assertThat(right.isSubjectIsIri());
		assertThat(not(right.isObjectIsIri()));
		assertThat(not(right.isObjectIsResource()));
		assertThat(not(right.isContextIsIri()));
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
		assertThat(right.isSubjectIsResource());
		assertThat(right.isSubjectIsIri());
		assertThat(not(right.isObjectIsIri()));
		assertThat(not(right.isObjectIsResource()));
		assertThat(not(right.isContextIsIri()));
	}
}
