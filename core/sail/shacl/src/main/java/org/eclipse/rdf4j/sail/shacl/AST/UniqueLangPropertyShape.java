/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.NonUniqueTargetLang;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author Håvard Ottestad
 */
public class UniqueLangPropertyShape extends PathPropertyShape {

	private final boolean uniqueLang;
	private static final Logger logger = LoggerFactory.getLogger(UniqueLangPropertyShape.class);

	UniqueLangPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape, boolean deactivated,
			PathPropertyShape parent, Resource path,
			boolean uniqueLang) {
		super(id, connection, nodeShape, deactivated, parent, path);

		this.uniqueLang = uniqueLang;
		assert uniqueLang : "uniqueLang should always be true";

	}

	@Override
	public PlanNode getPlan(ConnectionsGroup connectionsGroup, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {

		if (deactivated) {
			return null;
		}

		assert !negateSubPlans : "There are no subplans!";
		assert !negateThisPlan;
		assert hasOwnPath();

		if (overrideTargetNode != null) {
			PlanNode relevantTargetsWithPath = new BulkedExternalInnerJoin(overrideTargetNode.getPlanNode(),
					connectionsGroup.getBaseConnection(), getPath().getQuery("?a", "?c", null), false, null, "?a",
					"?c");

			PlanNode planNode = new NonUniqueTargetLang(relevantTargetsWithPath);

			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(planNode, connectionsGroup);
				logger.info(planAsGraphvizDot);
			}

			return new EnrichWithShape(planNode, this);
		}

		if (connectionsGroup.getStats().isBaseSailEmpty()) {
			PlanNode addedTargets = nodeShape.getPlanAddedStatements(connectionsGroup, null);

			PlanNode addedByPath = super.getPlanAddedStatements(connectionsGroup, null);

			PlanNode innerJoin = new InnerJoin(addedTargets, addedByPath).getJoined(UnBufferedPlanNode.class);

			PlanNode planNode = new NonUniqueTargetLang(innerJoin);

			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(planNode, connectionsGroup);
				logger.info(planAsGraphvizDot);
			}

			return new EnrichWithShape(planNode, this);

		}

		PlanNode addedTargets = nodeShape.getPlanAddedStatements(connectionsGroup, null);

		PlanNode addedByPath = super.getPlanAddedStatements(connectionsGroup, null);

		addedByPath = nodeShape.getTargetFilter(connectionsGroup.getBaseConnection(), addedByPath);

		PlanNode mergeNode = new UnionNode(addedTargets, addedByPath);

		PlanNode trimmed = new TrimTuple(mergeNode, 0, 1);

		PlanNode allRelevantTargets = new Unique(trimmed);

		PlanNode relevantTargetsWithPath = new BulkedExternalInnerJoin(allRelevantTargets,
				connectionsGroup.getBaseConnection(),
				getPath().getQuery("?a", "?c", null), false, null, "?a", "?c");

		PlanNode planNode = new NonUniqueTargetLang(relevantTargetsWithPath);

		if (printPlans) {
			String planAsGraphvizDot = getPlanAsGraphvizDot(planNode, connectionsGroup);
			logger.info(planAsGraphvizDot);
		}

		return new EnrichWithShape(planNode, this);

	}

	@Override
	public SourceConstraintComponent getSourceConstraintComponent() {
		return SourceConstraintComponent.UniqueLangConstraintComponent;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		UniqueLangPropertyShape that = (UniqueLangPropertyShape) o;
		return uniqueLang == that.uniqueLang;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), uniqueLang);
	}

	@Override
	public String toString() {
		return "UniqueLangPropertyShape{" +
				"uniqueLang=" + uniqueLang +
				", path=" + getPath() +
				'}';
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
