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
package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlQueryParserCache;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.AbstractConstraintComponent;

public abstract class AbstractBulkJoinPlanNode implements PlanNode {

	public static final String BINDING_NAME = "a";
	protected static final int BULK_SIZE = 1000;
	private StackTraceElement[] stackTrace;
	protected Function<BindingSet, ValidationTuple> mapper;
	ValidationExecutionLogger validationExecutionLogger;

	public AbstractBulkJoinPlanNode() {
//		this.stackTrace = Thread.currentThread().getStackTrace();
	}

	TupleExpr parseQuery(String query) {

		// #VALUES_INJECTION_POINT# is an annotation in the query where there is a "new scope" due to the bottom up
		// semantics of SPARQL but where we don't actually want a new scope.
		query = query.replace(AbstractConstraintComponent.VALUES_INJECTION_POINT, "\nVALUES (?a) {}\n");
		String completeQuery = "select * where {\nVALUES (?a) {}\n" + query + "\n}";
		return SparqlQueryParserCache.get(completeQuery);
	}

	void runQuery(ArrayDeque<ValidationTuple> left, ArrayDeque<ValidationTuple> right, SailConnection connection,
			TupleExpr parsedQuery, Dataset dataset, Resource[] dataGraph, boolean skipBasedOnPreviousConnection,
			SailConnection previousStateConnection) {
		List<BindingSet> newBindindingSet = buildBindingSets(left, connection, skipBasedOnPreviousConnection,
				previousStateConnection, dataGraph);

		if (!newBindindingSet.isEmpty()) {
			updateQuery(parsedQuery, newBindindingSet);
			executeQuery(right, connection, dataset, parsedQuery);
		}
	}

	private void executeQuery(ArrayDeque<ValidationTuple> right, SailConnection connection,
			Dataset dataset, TupleExpr parsedQuery) {

//		System.out.println(stackTrace[3].getClassName());
		try (Stream<? extends BindingSet> stream = connection
				.evaluate(parsedQuery, dataset, EmptyBindingSet.getInstance(), true)
				.stream()) {
			stream
					.map(mapper)
					.sorted(ValidationTuple::compareActiveTarget)
					.forEachOrdered(right::addFirst);
		}

	}

	private void updateQuery(TupleExpr parsedQuery, List<BindingSet> newBindindingSet) {
		parsedQuery
				.visit(new AbstractSimpleQueryModelVisitor<>(false) {
					@Override
					public void meet(BindingSetAssignment node) {
						Set<String> bindingNames = node.getBindingNames();
						if (bindingNames.size() == 1 && bindingNames.contains(BINDING_NAME)) {
							node.setBindingSets(newBindindingSet);
						}
						super.meet(node);
					}

				});
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
				.map(ValidationTuple::getActiveTarget)
				.map(r -> new SingletonBindingSet(BINDING_NAME, r))
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
