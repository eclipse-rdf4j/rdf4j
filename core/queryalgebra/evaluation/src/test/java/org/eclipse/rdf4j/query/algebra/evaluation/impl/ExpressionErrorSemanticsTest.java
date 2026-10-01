/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
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

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.transaction.QueryEvaluationMode;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Checks the SPARQL 1.1 error rules of OR, AND, IF, COALESCE and BIND when an operand is a constant expression that
 * always raises an error, such as <code>1 / 0</code>.
 */
public class ExpressionErrorSemanticsTest {

	@ParameterizedTest
	@EnumSource(QueryEvaluationMode.class)
	public void trueOrErrorIsTrue(QueryEvaluationMode mode) {
		assertThat(filter(mode, "(?v = 1) || ((1 / 0) = 1)")).containsExactly("1");
	}

	@ParameterizedTest
	@EnumSource(QueryEvaluationMode.class)
	public void errorOrTrueIsTrue(QueryEvaluationMode mode) {
		assertThat(filter(mode, "((1 / 0) = 1) || (?v = 1)")).containsExactly("1");
	}

	@ParameterizedTest
	@EnumSource(QueryEvaluationMode.class)
	public void negatedTrueOrErrorKeepsNoRow(QueryEvaluationMode mode) {
		// !true is false and !(false || error) is an error
		assertThat(filter(mode, "!((?v = 1) || ((1 / 0) = 1))")).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(QueryEvaluationMode.class)
	public void negatedErrorOrFalseIsAnError(QueryEvaluationMode mode) {
		// !(error || true) is false and !(error || false) is an error
		assertThat(filter(mode, "!(((1 / 0) = 1) || (?v = 1))")).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(QueryEvaluationMode.class)
	public void trueOrConstantRegexErrorIsTrue(QueryEvaluationMode mode) {
		assertThat(filter(mode, "(?v = 1) || REGEX(1, \"x|y\")")).containsExactly("1");
	}

	@ParameterizedTest
	@EnumSource(QueryEvaluationMode.class)
	public void errorOrFalseLeavesBindUnbound(QueryEvaluationMode mode) {
		List<BindingSet> result = evaluate(mode,
				"SELECT ?v ?x WHERE { VALUES ?v { 1 2 } BIND(((1 / 0) = 1) || (?v = 1) AS ?x) } ORDER BY ?v");

		assertThat(result).hasSize(2);
		assertThat(result.get(0).getValue("x").stringValue()).isEqualTo("true");
		assertThat(result.get(1).hasBinding("x")).isFalse();
	}

	@ParameterizedTest
	@EnumSource(QueryEvaluationMode.class)
	public void negatedFalseAndErrorIsTrue(QueryEvaluationMode mode) {
		// !(false && error) is true and !(true && error) is an error
		assertThat(filter(mode, "!((?v = 2) && ((1 / 0) = 1))")).containsExactly("1");
	}

	@ParameterizedTest
	@EnumSource(QueryEvaluationMode.class)
	public void negatedFalseAndConstantRegexErrorIsTrue(QueryEvaluationMode mode) {
		assertThat(filter(mode, "!((?v = 2) && REGEX(1, \"x|y\"))")).containsExactly("1");
	}

	@ParameterizedTest
	@EnumSource(QueryEvaluationMode.class)
	public void constantErrorLeavesBindUnbound(QueryEvaluationMode mode) {
		List<BindingSet> result = evaluate(mode,
				"SELECT ?v ?x WHERE { VALUES ?v { 1 2 } BIND((1 / 0) AS ?x) } ORDER BY ?v");

		assertThat(result).hasSize(2);
		assertThat(result).noneMatch(bindings -> bindings.hasBinding("x"));
	}

	@ParameterizedTest
	@EnumSource(QueryEvaluationMode.class)
	public void ifDoesNotEvaluateUnselectedAlternative(QueryEvaluationMode mode) {
		assertThat(filter(mode, "IF(?v = 1, true, (1 / 0) = 1)")).containsExactly("1");
	}

	@ParameterizedTest
	@EnumSource(QueryEvaluationMode.class)
	public void ifDoesNotEvaluateUnselectedResult(QueryEvaluationMode mode) {
		assertThat(filter(mode, "IF(?v = 2, (1 / 0) = 1, true)")).containsExactly("1");
	}

	@ParameterizedTest
	@EnumSource(QueryEvaluationMode.class)
	public void ifDoesNotEvaluateUnselectedConstantRegexError(QueryEvaluationMode mode) {
		assertThat(filter(mode, "IF(?v = 1, true, REGEX(1, \"x|y\"))")).containsExactly("1");
	}

	@ParameterizedTest
	@EnumSource(QueryEvaluationMode.class)
	public void ifWithErrorConditionKeepsNoRow(QueryEvaluationMode mode) {
		assertThat(filter(mode, "IF((1 / 0) = 1, true, true)")).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(QueryEvaluationMode.class)
	public void coalesceSkipsErrorArgument(QueryEvaluationMode mode) {
		assertThat(filter(mode, "COALESCE(1 / 0, 2) = ?v")).containsExactly("2");
	}

	private static List<String> filter(QueryEvaluationMode mode, String condition) {
		return evaluate(mode, "SELECT ?v WHERE { VALUES ?v { 1 2 } FILTER(" + condition + ") } ORDER BY ?v")
				.stream()
				.map(bindings -> bindings.getValue("v"))
				.map(Value::stringValue)
				.collect(Collectors.toList());
	}

	private static List<BindingSet> evaluate(QueryEvaluationMode mode, String query) {
		DefaultEvaluationStrategy strategy = new DefaultEvaluationStrategy(new EmptyTripleSource(), null);
		strategy.setQueryEvaluationMode(mode);

		ParsedQuery parsedQuery = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, query, null);
		TupleExpr optimized = strategy.optimize(parsedQuery.getTupleExpr(), new EvaluationStatistics(),
				EmptyBindingSet.getInstance());

		try (CloseableIteration<BindingSet> result = strategy.precompile(optimized)
				.evaluate(EmptyBindingSet.getInstance())) {
			return QueryResults.asList(result);
		}
	}
}
