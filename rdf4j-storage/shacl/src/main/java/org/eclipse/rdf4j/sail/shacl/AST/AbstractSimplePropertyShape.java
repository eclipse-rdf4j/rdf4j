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
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
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

	static public PlanNode getGenericSingleObjectPlan(ConnectionsGroup connectionsGroup, NodeShape nodeShape,
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
				planNode = new BulkedExternalInnerJoin(overrideTargetNode.getPlanNode(),
						connectionsGroup.getBaseConnection(),
						pathPropertyShape.getPath().getQuery("?a", "?c", null), false, null, "?a", "?c");
			}

			if (negatePlan) {
				return filterAttacher.attachFilter(planNode).getTrueNode(UnBufferedPlanNode.class);
			} else {
				return filterAttacher.attachFilter(planNode).getFalseNode(UnBufferedPlanNode.class);
			}

		}

		if (pathPropertyShape.getPath() == null) {

			PlanNode targets = new ModifyTuple(
					nodeShape.getPlanAddedStatements(connectionsGroup, null), t -> {
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
			invalidValuesDirectOnPath = pathPropertyShape.getPlanAddedStatements(connectionsGroup,
					planNode -> filterAttacher.attachFilter(planNode).getTrueNode(UnBufferedPlanNode.class));
		} else {
			invalidValuesDirectOnPath = pathPropertyShape.getPlanAddedStatements(connectionsGroup,
					planNode -> filterAttacher.attachFilter(planNode).getFalseNode(UnBufferedPlanNode.class));
		}

		InnerJoin innerJoin = new InnerJoin(
				nodeShape.getPlanAddedStatements(connectionsGroup, null),
				invalidValuesDirectOnPath);

		if (connectionsGroup.getStats().isBaseSailEmpty()) {
			return innerJoin.getJoined(UnBufferedPlanNode.class);

		} else {

			PlanNode top = innerJoin.getJoined(BufferedPlanNode.class);

			PlanNode discardedRight = innerJoin.getDiscardedRight(BufferedPlanNode.class);

			PlanNode typeFilterPlan = nodeShape.getTargetFilter(connectionsGroup.getBaseConnection(), discardedRight);

			top = new UnionNode(top, typeFilterPlan);

			PlanNode bulkedExternalInnerJoin = new BulkedExternalInnerJoin(
					nodeShape.getPlanAddedStatements(connectionsGroup, null),
					connectionsGroup.getBaseConnection(), pathPropertyShape.getPath().getQuery("?a", "?c", null), true,
					connectionsGroup.getPreviousStateConnection(), "?a", "?c");

			top = new UnionNode(top, bulkedExternalInnerJoin);

			if (negatePlan) {
				return filterAttacher.attachFilter(top).getTrueNode(UnBufferedPlanNode.class);
			} else {
				return filterAttacher.attachFilter(top).getFalseNode(UnBufferedPlanNode.class);
			}

		}

	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, boolean negated) {
		PlanNode plan = nodeShape.getPlanAddedStatements(connectionsGroup, null);
		plan = new UnionNode(plan, nodeShape.getPlanRemovedStatements(connectionsGroup, null));

		Path path = getPath();
		if (path != null) {
			plan = new UnionNode(plan, getPlanAddedStatements(connectionsGroup, null));
			plan = new UnionNode(plan, getPlanRemovedStatements(connectionsGroup, null));
		}

		plan = new Unique(new TrimTuple(plan, 0, 1));

		return nodeShape.getTargetFilter(connectionsGroup.getBaseConnection(), plan);
	}
}
