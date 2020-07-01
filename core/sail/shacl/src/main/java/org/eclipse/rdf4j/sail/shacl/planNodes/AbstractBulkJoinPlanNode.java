/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.planNodes;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserFactory;
import org.eclipse.rdf4j.query.parser.QueryParserRegistry;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.GlobalValidationExecutionLogging;

abstract class AbstractBulkJoinPlanNode implements PlanNode {

	protected String[] variables;
	ValidationExecutionLogger validationExecutionLogger;

	ParsedQuery parseQuery(String query) {
		QueryParserFactory queryParserFactory = QueryParserRegistry.getInstance().get(QueryLanguage.SPARQL).get();

		// #VALUES_INJECTION_POINT# is an annotation in the query where there is a "new scope" due to the bottom up
		// semantics of SPARQL but where we don't actually want a new scope.
		query = query.replace("#VALUES_INJECTION_POINT#", "\nVALUES (?a) {}\n");
		String completeQuery = "select * where { \nVALUES (?a) {}\n" + query + "\n}\nORDER BY ?a";
		return queryParserFactory.getParser().parseQuery(completeQuery, null);
	}

	void runQuery(ArrayDeque<Tuple> left, ArrayDeque<Tuple> right, SailConnection connection,
			ParsedQuery parsedQuery, boolean skipBasedOnPreviousConnection, SailConnection previousStateConnection,
			String[] variables) {
		List<BindingSet> newBindindingset = buildBindingSets(left, connection, skipBasedOnPreviousConnection,
				previousStateConnection);

		if (!newBindindingset.isEmpty()) {
			updateQuery(parsedQuery, newBindindingset);
			executeQuery(right, connection, parsedQuery, variables);
		}
	}

	private static void executeQuery(ArrayDeque<Tuple> right, SailConnection connection, ParsedQuery parsedQuery,
			String[] variables) {

//		Explanation explain = connection.explain(Explanation.Level.Timed, parsedQuery.getTupleExpr(), parsedQuery.getDataset(), new MapBindingSet(), true, 10000);
//		System.out.println(explain);

		try (Stream<? extends BindingSet> stream = connection
				.evaluate(parsedQuery.getTupleExpr(), parsedQuery.getDataset(), new MapBindingSet(), true)
				.stream()) {
			stream
					.map(t -> new Tuple(t, variables))
					.forEachOrdered(right::addFirst);
		}

	}

	private void updateQuery(ParsedQuery parsedQuery, List<BindingSet> newBindindingset) {
		try {

			parsedQuery.getTupleExpr()
					.visit(new AbstractQueryModelVisitor<Exception>() {
						@Override
						public void meet(BindingSetAssignment node) throws Exception {
							Set<String> bindingNames = node.getBindingNames();
							if (bindingNames.size() == 1 && bindingNames.contains("a")) {
								node.setBindingSets(newBindindingset);
							}

							super.meet(node);
						}

					});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private List<BindingSet> buildBindingSets(ArrayDeque<Tuple> left, SailConnection connection,
			boolean skipBasedOnPreviousConnection, SailConnection previousStateConnection) {
		return left.stream()

				.filter(tuple -> {
					if (!skipBasedOnPreviousConnection) {
						return true;
					}

					boolean hasStatement;

					if (!(tuple.getLine().get(0) instanceof Resource)) {
						hasStatement = previousStateConnection.hasStatement(null, null, tuple.getLine().get(0), true);
					} else {
						hasStatement = previousStateConnection
								.hasStatement(((Resource) tuple.getLine().get(0)), null, null, true) ||
								previousStateConnection.hasStatement(null, null, tuple.getLine().get(0), true);
					}

					if (!hasStatement && GlobalValidationExecutionLogging.loggingEnabled) {
						validationExecutionLogger.log(depth(),
								this.getClass().getSimpleName() + ":IgnoredDueToPreviousStateConnection", tuple, this,
								getId());
					}
					return hasStatement;

				})
				.map(tuple -> tuple.getLine().get(0))
				.map(r -> new ListBindingSet(Collections.singletonList("a"), Collections.singletonList(r)))
				.collect(Collectors.toList());
	}

}
