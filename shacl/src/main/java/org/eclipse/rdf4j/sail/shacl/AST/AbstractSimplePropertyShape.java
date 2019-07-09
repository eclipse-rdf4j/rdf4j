/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.BufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.ModifyTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;

/**
 * @author Håvard Ottestad
 */
public abstract class AbstractSimplePropertyShape extends PathPropertyShape {

	AbstractSimplePropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape,
			boolean deactivated, PathPropertyShape parent, Resource path) {
		super(id, connection, nodeShape, deactivated, parent, path);
	}

	interface FilterAttacher {
		FilterPlanNode attachFilter(PlanNode parent);
	}

	static public PlanNode getGenericSingleObjectPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape,
			FilterAttacher filterAttacher, PathPropertyShape pathPropertyShape, PlanNodeProvider overrideTargetNode,
			boolean negatePlan) {
		if (overrideTargetNode != null) {

			PlanNode planNode;

			if (pathPropertyShape.getPath() == null) {
				planNode = new ModifyTuple(overrideTargetNode.getPlanNode(), t -> {
					t.line.add(t.line.get(0));
					return t;
				});
			} else {
				planNode = new BulkedExternalInnerJoin(overrideTargetNode.getPlanNode(), shaclSailConnection,
						pathPropertyShape.getPath().getQuery("?a", "?c", null), false, "?a", "?c");
			}

			if (negatePlan) {
				return filterAttacher.attachFilter(planNode).getTrueNode(UnBufferedPlanNode.class);
			} else {
				return filterAttacher.attachFilter(planNode).getFalseNode(UnBufferedPlanNode.class);
			}

		}

		if (pathPropertyShape.getPath() == null) {

			PlanNode targets = new ModifyTuple(
					nodeShape.getPlanAddedStatements(shaclSailConnection, null), t -> {
						t.line.add(t.line.get(0));
						return t;
					});

			if (negatePlan) {
				return filterAttacher.attachFilter(targets).getTrueNode(UnBufferedPlanNode.class);
			} else {
				return filterAttacher.attachFilter(targets).getFalseNode(UnBufferedPlanNode.class);
			}

		}

		PlanNode invalidValuesDirectOnPath;

		if (negatePlan) {
			invalidValuesDirectOnPath = pathPropertyShape.getPlanAddedStatements(shaclSailConnection,
					planNode -> filterAttacher.attachFilter(planNode).getTrueNode(UnBufferedPlanNode.class));
		} else {
			invalidValuesDirectOnPath = pathPropertyShape.getPlanAddedStatements(shaclSailConnection,
					planNode -> filterAttacher.attachFilter(planNode).getFalseNode(UnBufferedPlanNode.class));
		}

		InnerJoin innerJoin = new InnerJoin(
				nodeShape.getPlanAddedStatements(shaclSailConnection, null),
				invalidValuesDirectOnPath);

		if (shaclSailConnection.stats.isBaseSailEmpty()) {
			return innerJoin.getJoined(UnBufferedPlanNode.class);

		} else {

			PlanNode top = innerJoin.getJoined(BufferedPlanNode.class);

			PlanNode discardedRight = innerJoin.getDiscardedRight(BufferedPlanNode.class);

			PlanNode typeFilterPlan = nodeShape.getTargetFilter(shaclSailConnection, discardedRight);

			top = new UnionNode(top, typeFilterPlan);

			PlanNode bulkedExternalInnerJoin = new BulkedExternalInnerJoin(
					nodeShape.getPlanAddedStatements(shaclSailConnection, null),
					shaclSailConnection, pathPropertyShape.getPath().getQuery("?a", "?c", null), true, "?a", "?c");

			top = new UnionNode(top, bulkedExternalInnerJoin);

			if (negatePlan) {
				return filterAttacher.attachFilter(top).getTrueNode(UnBufferedPlanNode.class);
			} else {
				return filterAttacher.attachFilter(top).getFalseNode(UnBufferedPlanNode.class);
			}

		}

	}

	@Override
	public PlanNode getAllTargetsPlan(ShaclSailConnection shaclSailConnection, boolean negated) {
		PlanNode plan = nodeShape.getPlanAddedStatements(shaclSailConnection, null);
		plan = new UnionNode(plan, nodeShape.getPlanRemovedStatements(shaclSailConnection, null));

		Path path = getPath();
		if (path != null) {
			plan = new UnionNode(plan, getPlanAddedStatements(shaclSailConnection, null));
			plan = new UnionNode(plan, getPlanRemovedStatements(shaclSailConnection, null));
		}

		plan = new Unique(new TrimTuple(plan, 0, 1));

		return nodeShape.getTargetFilter(shaclSailConnection, plan);
	}
}
