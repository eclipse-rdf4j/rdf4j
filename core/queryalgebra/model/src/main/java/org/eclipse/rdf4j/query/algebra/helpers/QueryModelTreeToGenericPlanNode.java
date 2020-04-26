/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.helpers;

import java.util.ArrayDeque;

import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.explanation.GenericPlanNode;

public class QueryModelTreeToGenericPlanNode extends AbstractQueryModelVisitor<RuntimeException> {

	GenericPlanNode top = null;
	QueryModelNode topTupleExpr;

	public QueryModelTreeToGenericPlanNode(QueryModelNode topTupleExpr) {
		this.topTupleExpr = topTupleExpr;

	}

	/*---------*
	 * Methods *
	 *---------*/

	public GenericPlanNode getGenericPlanNode() {
		return top;
	}

	ArrayDeque<GenericPlanNode> deque = new ArrayDeque<>();

	// node.getParentNode() is not reliable because nodes are reused and parent is not maintained! This is why we use a
	// queue to maintain the effective parent.
	@Override
	protected void meetNode(QueryModelNode node) {
		GenericPlanNode genericPlanNode = new GenericPlanNode(node.getSignature());
		genericPlanNode.setCostEstimate(node.getCostEstimate());
		genericPlanNode.setResultSizeEstimate(node.getResultSizeEstimate());
		genericPlanNode.setResultSizeActual(node.getResultSizeActual());
		genericPlanNode.setTotalTimeActual(node.getTotalTimeNanosActual() / 1_000_000.0); // convert from nanoseconds to
																							// milliseconds

		if (node == topTupleExpr) {
			top = genericPlanNode;
		}

		if (!deque.isEmpty()) {
			GenericPlanNode genericParentNode = deque.getLast();
			genericParentNode.addPlans(genericPlanNode);
		}

		deque.addLast(genericPlanNode);
		super.meetNode(node);
		deque.removeLast();

	}

}
