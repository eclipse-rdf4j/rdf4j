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
package org.eclipse.rdf4j.query.algebra.helpers;

import java.util.ArrayDeque;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.query.algebra.BinaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.VariableScopeChange;
import org.eclipse.rdf4j.query.explanation.GenericPlanNode;

/**
 * Convert TupleExpr (QueryModelNode) to GenericPlanNode for the Query.explain(...) feature.
 */
@Experimental
@InternalUseOnly
public class QueryModelTreeToGenericPlanNode extends AbstractQueryModelVisitor<RuntimeException> {

	GenericPlanNode top = null;
	QueryModelNode topTupleExpr;
	boolean includeDefaultRuntimeTelemetry;
	ArrayDeque<GenericPlanNode> planNodes = new ArrayDeque<>();

	public QueryModelTreeToGenericPlanNode(QueryModelNode topTupleExpr) {
		if (topTupleExpr instanceof QueryRoot) {
			topTupleExpr = ((QueryRoot) topTupleExpr).getArg();
		}
		this.topTupleExpr = topTupleExpr;
		this.includeDefaultRuntimeTelemetry = includeDefaultRuntimeTelemetry(topTupleExpr);
	}

	public GenericPlanNode getGenericPlanNode() {
		return top;
	}

	@Override
	protected void meetNode(QueryModelNode node) {
		GenericPlanNode genericPlanNode = new GenericPlanNode(node.getSignature());
		genericPlanNode.setCostEstimate(node.getCostEstimate());
		genericPlanNode.setResultSizeEstimate(node.getResultSizeEstimate());
		genericPlanNode.setResultSizeActual(node.getResultSizeActual());
		genericPlanNode.setHasNextCallCountActual(runtimeTelemetryMetric(node.getHasNextCallCountActual()));
		genericPlanNode.setHasNextTrueCountActual(runtimeTelemetryMetric(node.getHasNextTrueCountActual()));
		genericPlanNode.setHasNextTimeNanosActual(runtimeTelemetryMetric(node.getHasNextTimeNanosActual()));
		genericPlanNode.setNextCallCountActual(runtimeTelemetryMetric(node.getNextCallCountActual()));
		genericPlanNode.setNextTimeNanosActual(runtimeTelemetryMetric(node.getNextTimeNanosActual()));
		genericPlanNode
				.setJoinRightIteratorsCreatedActual(runtimeTelemetryMetric(node.getJoinRightIteratorsCreatedActual()));
		genericPlanNode
				.setJoinLeftBindingsConsumedActual(runtimeTelemetryMetric(node.getJoinLeftBindingsConsumedActual()));
		genericPlanNode
				.setJoinRightBindingsConsumedActual(runtimeTelemetryMetric(node.getJoinRightBindingsConsumedActual()));
		genericPlanNode.setSourceRowsScannedActual(runtimeTelemetryMetric(node.getSourceRowsScannedActual()));
		genericPlanNode.setSourceRowsMatchedActual(runtimeTelemetryMetric(node.getSourceRowsMatchedActual()));
		genericPlanNode.setSourceRowsFilteredActual(runtimeTelemetryMetric(node.getSourceRowsFilteredActual()));
		if (node instanceof VariableScopeChange) {
			boolean newScope = ((VariableScopeChange) node).isVariableScopeChange();
			genericPlanNode.setNewScope(newScope);
		}

		if (node instanceof BinaryTupleOperator) {
			String algorithmName = ((BinaryTupleOperator) node).getAlgorithmName();
			genericPlanNode.setAlgorithm(algorithmName);
		}

		// convert from nanoseconds to milliseconds
		genericPlanNode.setTotalTimeActual(node.getTotalTimeNanosActual() / 1_000_000.0);

		if (node == topTupleExpr) {
			top = genericPlanNode;
		}

		if (!planNodes.isEmpty()) {
			GenericPlanNode genericParentNode = planNodes.getLast();
			genericParentNode.addPlans(genericPlanNode);
		}

		planNodes.addLast(genericPlanNode);
		super.meetNode(node);
		planNodes.removeLast();
	}

	private long runtimeTelemetryMetric(long value) {
		if (includeDefaultRuntimeTelemetry) {
			return Math.max(0, value);
		}
		return value;
	}

	private static boolean includeDefaultRuntimeTelemetry(QueryModelNode node) {
		return node != null
				&& (node.getResultSizeActual() >= 0
						|| node.getTotalTimeNanosActual() >= 0
						|| node.getHasNextCallCountActual() >= 0
						|| node.getNextCallCountActual() >= 0);
	}

}
