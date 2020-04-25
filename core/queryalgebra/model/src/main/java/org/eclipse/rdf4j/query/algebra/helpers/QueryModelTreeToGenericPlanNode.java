/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.helpers;

import java.util.IdentityHashMap;

import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
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

	IdentityHashMap<QueryModelNode, GenericPlanNode> map = new IdentityHashMap<>();

	@Override
	protected void meetNode(QueryModelNode node) {
		GenericPlanNode genericPlanNode = new GenericPlanNode(node.getSignature());
		genericPlanNode.setCostEstimate(node.getCostEstimate());
		genericPlanNode.setResultSizeEstimate(node.getResultSizeEstimate());
		genericPlanNode.setResultSizeActual(node.getResultSizeActual());

		if (map.containsKey(node)) {
			throw new IllegalStateException("Node has been visited twice!");
		} else {
			map.put(node, genericPlanNode);
		}
		super.meetNode(node);

		if (node == topTupleExpr) {
			top = genericPlanNode;
		}

		QueryModelNode parentNode = node.getParentNode();
		if (parentNode != null) {
			GenericPlanNode genericParentNode = map.get(parentNode);
			if (genericParentNode != null) {
				genericParentNode.addPlans(genericPlanNode);
			} else {
				if (!(parentNode instanceof QueryRoot)) {
					throw new IllegalStateException(
							"Node parent is unknown! Maybe reuse of child node between parents?");
				}
			}
		}

	}

}
