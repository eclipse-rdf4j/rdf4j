/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.GlobalValidationExecutionLogging;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract class AbstractBulkJoinPlanNode implements PlanNode {

	protected String[] variables;
	ValidationExecutionLogger validationExecutionLogger;

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
					.visitChildren(new AbstractQueryModelVisitor<Exception>() {
						@Override
						public void meet(BindingSetAssignment node) {
							node.setBindingSets(newBindindingset);
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

					boolean hasStatement = previousStateConnection.hasStatement(((Resource) tuple.line.get(0)), null,
							null, true);

					if (!hasStatement && GlobalValidationExecutionLogging.loggingEnabled) {
						validationExecutionLogger.log(depth(),
								this.getClass().getSimpleName() + ":IgnoredDueToPreviousStateConnection", tuple, this,
								getId());
					}
					return hasStatement;

				})
				.map(tuple -> (Resource) tuple.line.get(0))
				.map(r -> new ListBindingSet(Collections.singletonList("a"), Collections.singletonList(r)))
				.collect(Collectors.toList());
	}

}
