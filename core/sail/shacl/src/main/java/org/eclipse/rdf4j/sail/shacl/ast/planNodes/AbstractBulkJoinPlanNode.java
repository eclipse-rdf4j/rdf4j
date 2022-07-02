/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserFactory;
import org.eclipse.rdf4j.query.parser.QueryParserRegistry;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.AbstractConstraintComponent;

public abstract class AbstractBulkJoinPlanNode implements PlanNode {

	protected Function<BindingSet, ValidationTuple> mapper;
	ValidationExecutionLogger validationExecutionLogger;

	ParsedQuery parseQuery(String query) {
		QueryParserFactory queryParserFactory = QueryParserRegistry.getInstance().get(QueryLanguage.SPARQL).get();

		// #VALUES_INJECTION_POINT# is an annotation in the query where there is a "new scope" due to the bottom up
		// semantics of SPARQL but where we don't actually want a new scope.
		query = query.replace(AbstractConstraintComponent.VALUES_INJECTION_POINT, "\nVALUES (?a) {}\n");
		String completeQuery = "select * where { \nVALUES (?a) {}\n" + query + "\n}\nORDER BY ?a";
		return queryParserFactory.getParser().parseQuery(completeQuery, null);
	}

	void runQuery(ArrayDeque<ValidationTuple> left, ArrayDeque<ValidationTuple> right, SailConnection connection,
			ParsedQuery parsedQuery, Dataset dataset, Resource[] dataGraph, boolean skipBasedOnPreviousConnection,
			SailConnection previousStateConnection, Function<BindingSet, ValidationTuple> mapper) {
		List<BindingSet> newBindindingset = buildBindingSets(left, connection, skipBasedOnPreviousConnection,
				previousStateConnection, dataGraph);

		if (!newBindindingset.isEmpty()) {
			updateQuery(parsedQuery, newBindindingset);
			executeQuery(right, connection, dataset, parsedQuery, mapper);
		}
	}

	private static void executeQuery(ArrayDeque<ValidationTuple> right, SailConnection connection,
			Dataset dataset, ParsedQuery parsedQuery,
			Function<BindingSet, ValidationTuple> mapper) {

		try (Stream<? extends BindingSet> stream = connection
				.evaluate(parsedQuery.getTupleExpr(), dataset, new MapBindingSet(), true)
				.stream()) {
			stream
					.map(mapper)
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

	private List<BindingSet> buildBindingSets(ArrayDeque<ValidationTuple> left, SailConnection connection,
			boolean skipBasedOnPreviousConnection, SailConnection previousStateConnection, Resource[] dataGraph) {
		return left.stream()

				.filter(tuple -> {
					if (!skipBasedOnPreviousConnection) {
						return true;
					}

					boolean hasStatement;

					if (!(tuple.getActiveTarget().isResource())) {
						hasStatement = previousStateConnection.hasStatement(null, null, tuple.getActiveTarget(),
								true, dataGraph);

					} else {
						hasStatement = previousStateConnection.hasStatement(((Resource) tuple.getActiveTarget()),
								null, null, true, dataGraph) ||
								previousStateConnection.hasStatement(null, null, tuple.getActiveTarget(), true,
										dataGraph);

					}

					if (!hasStatement && validationExecutionLogger.isEnabled()) {
						validationExecutionLogger.log(depth(),
								this.getClass().getSimpleName() + ":IgnoredDueToPreviousStateConnection", tuple, this,
								getId(), null);
					}
					return hasStatement;

				})
				.map(validationTuple -> validationTuple.getActiveTarget())
				.map(r -> new SimpleBindingSet(Collections.singleton("a"), Collections.singletonList("a"),
						Collections.singletonList(r)))
				.collect(Collectors.toList());
	}

	@Override
	public boolean producesSorted() {
		return true;
	}

	@Override
	public boolean requiresSorted() {
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AbstractBulkJoinPlanNode that = (AbstractBulkJoinPlanNode) o;
		return mapper.equals(that.mapper);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mapper);
	}
}
