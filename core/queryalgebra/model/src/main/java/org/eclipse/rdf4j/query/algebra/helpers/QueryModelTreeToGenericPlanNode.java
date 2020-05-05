/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.helpers;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.query.algebra.BinaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
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
	ArrayDeque<GenericPlanNode> deque = new ArrayDeque<>();
	ArrayDeque<QueryModelNode> deque2 = new ArrayDeque<>();
	IdentityHashMap<QueryModelNode, QueryModelNode> visited = new IdentityHashMap<>();

	public QueryModelTreeToGenericPlanNode(QueryModelNode topTupleExpr) {
		this.topTupleExpr = topTupleExpr;
	}

	public GenericPlanNode getGenericPlanNode() {
		return top;
	}

	// node.getParentNode() is not reliable because nodes are reused and parent is not maintained! This is why we use a
	// queue to maintain the effective parent stack.
	@Override
	protected void meetNode(QueryModelNode node) {
		assert !visited.containsKey(node);
		visited.put(node, node);

		GenericPlanNode genericPlanNode = new GenericPlanNode(node.getSignature());
		genericPlanNode.setCostEstimate(node.getCostEstimate());
		genericPlanNode.setResultSizeEstimate(node.getResultSizeEstimate());
		genericPlanNode.setResultSizeActual(node.getResultSizeActual());
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

		if (!deque.isEmpty()) {
			GenericPlanNode genericParentNode = deque.getLast();
			genericParentNode.addPlans(genericPlanNode);
			assert node.getParentNode() == deque2.getLast();
		}

		deque.addLast(genericPlanNode);
		deque2.addLast(node);
		super.meetNode(node);
		deque.removeLast();
		deque2.removeLast();

	}

}
