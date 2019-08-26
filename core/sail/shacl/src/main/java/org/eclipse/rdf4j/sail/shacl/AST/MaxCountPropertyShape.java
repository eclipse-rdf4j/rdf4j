/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.planNodes.AggregateIteratorTypeOverride;
import org.eclipse.rdf4j.sail.shacl.planNodes.BufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.GroupByCount;
import org.eclipse.rdf4j.sail.shacl.planNodes.MaxCountFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.ModifyTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * The AST (Abstract Syntax Tree) node that represents a sh:maxCount property nodeShape restriction.
 *
 * @author Håvard Ottestad
 */
public class MaxCountPropertyShape extends PathPropertyShape {

	private static final Logger logger = LoggerFactory.getLogger(MaxCountPropertyShape.class);

	private long maxCount;

	MaxCountPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape, boolean deactivated,
			PathPropertyShape parent, Resource path, Long maxCount) {
		super(id, connection, nodeShape, deactivated, parent, path);

		this.maxCount = maxCount;

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
			PlanNode bulkedExternalInnerJoin = new BulkedExternalInnerJoin(overrideTargetNode.getPlanNode(),
					connectionsGroup.getBaseConnection(), getPath().getQuery("?a", "?c", null), false, null, "?a",
					"?c");
			PlanNode groupByCount = new GroupByCount(bulkedExternalInnerJoin);

			PlanNode directTupleFromFilter = new MaxCountFilter(groupByCount, maxCount)
					.getFalseNode(UnBufferedPlanNode.class);

			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(directTupleFromFilter, connectionsGroup);
				logger.info(planAsGraphvizDot);
			}

			return new EnrichWithShape(directTupleFromFilter, this);
		}

		if (maxCount == 1 && connectionsGroup.getStats().isBaseSailEmpty()) {
			String query = nodeShape.getQuery("?a", "?b", null);
			String query1 = getPath().getQuery("?a", "?d", null);
			String query2 = getPath().getQuery("?a", "?e", null);

			String negationQuery = query + "\n" + query1 + "\n" + query2 + "\nFILTER(?d != ?e)";

			PlanNode select = new Select(connectionsGroup.getAddedStatements(), negationQuery, "?a");
			select = new ModifyTuple(select, (a) -> {
				a.line.add(SimpleValueFactory.getInstance().createLiteral(">= 2"));

				return a;
			});
			select = new AggregateIteratorTypeOverride(select);

			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(select, connectionsGroup);
				logger.info(planAsGraphvizDot);
			}

			return new EnrichWithShape(select, this);

		}

		PlanNode planAddedStatements = nodeShape.getPlanAddedStatements(connectionsGroup, null);

		PlanNode planAddedStatements1 = super.getPlanAddedStatements(connectionsGroup, null);

		planAddedStatements1 = nodeShape.getTargetFilter(connectionsGroup.getBaseConnection(), planAddedStatements1);

		PlanNode mergeNode = new UnionNode(planAddedStatements, planAddedStatements1);

		PlanNode groupByCount1 = new GroupByCount(mergeNode);

		MaxCountFilter maxCountFilter = new MaxCountFilter(groupByCount1, maxCount);

		PlanNode validValues = maxCountFilter.getTrueNode(BufferedPlanNode.class);
		PlanNode invalidValues = maxCountFilter.getFalseNode(BufferedPlanNode.class);

		PlanNode mergeNode1;
		if (!connectionsGroup.getStats().isBaseSailEmpty()) {

			PlanNode trimmed = new TrimTuple(validValues, 0, 1);

			PlanNode unique = new Unique(trimmed);

			PlanNode bulkedExternalInnerJoin = new BulkedExternalInnerJoin(unique, connectionsGroup.getBaseConnection(),
					getPath().getQuery("?a", "?c", null),
					true, connectionsGroup.getPreviousStateConnection(), "?a", "?c");

			PlanNode groupByCount = new GroupByCount(bulkedExternalInnerJoin);

			PlanNode directTupleFromFilter = new MaxCountFilter(groupByCount, maxCount)
					.getFalseNode(UnBufferedPlanNode.class);

			mergeNode1 = new UnionNode(directTupleFromFilter,
					invalidValues);
		} else {
			mergeNode1 = invalidValues;
		}

		if (printPlans) {
			String planAsGraphvizDot = getPlanAsGraphvizDot(mergeNode1, connectionsGroup);
			logger.info(planAsGraphvizDot);
		}

		return new EnrichWithShape(mergeNode1, this);

	}

	@Override
	public SourceConstraintComponent getSourceConstraintComponent() {
		return SourceConstraintComponent.MaxCountConstraintComponent;
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
		MaxCountPropertyShape that = (MaxCountPropertyShape) o;
		return maxCount == that.maxCount;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), maxCount);
	}

	@Override
	public String toString() {
		return "MaxCountPropertyShape{" +
				"maxCount=" + maxCount +
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
