/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import java.util.Objects;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.planNodes.AggregateIteratorTypeOverride;
import org.eclipse.rdf4j.sail.shacl.planNodes.BulkedExternalLeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.GroupByCount;
import org.eclipse.rdf4j.sail.shacl.planNodes.MinCountFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.TupleMapper;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The AST (Abstract Syntax Tree) node that represents a sh:minCount property nodeShape restriction.
 *
 * @author Heshan Jayasinghe
 * @author Håvard M. Ottestad
 */
public class MinCountPropertyShape extends PathPropertyShape {

	private long minCount;
	private static final Logger logger = LoggerFactory.getLogger(MinCountPropertyShape.class);

	// toggle for switching on and off the optimization used when no statements have been removed in a transaction
	private boolean optimizeWhenNoStatementsRemoved = true;

	MinCountPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape, boolean deactivated,
			PathPropertyShape parent, Resource path,
			Long minCount) {
		super(id, connection, nodeShape, deactivated, parent, path);

		this.minCount = minCount;

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
			PlanNode allStatements = new BulkedExternalLeftOuterJoin(overrideTargetNode.getPlanNode(),
					connectionsGroup.getBaseConnection(), getPath().getQuery("?a", "?c", null), false, null, "?a",
					"?c");
			PlanNode groupBy = new GroupByCount(allStatements);

			PlanNode filteredStatements = new MinCountFilter(groupBy, minCount).getFalseNode(UnBufferedPlanNode.class);

			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(filteredStatements, connectionsGroup);
				logger.info(planAsGraphvizDot);
			}

			return new EnrichWithShape(filteredStatements, this);

		}

		PlanNode topNode;

		if (minCount == 1 && connectionsGroup.getStats().isBaseSailEmpty()) {
			String query = nodeShape.getQuery("?a", "?b", null);
			String query1 = getPath().getQuery("?a", "?d", null);

			String negationQuery = query + "\n FILTER(NOT EXISTS{" + query1 + "})";

			PlanNode select = new Select(connectionsGroup.getAddedStatements(), negationQuery, "?a");
			select = new TupleMapper(select, (a) -> {
				a.getLine().add(SimpleValueFactory.getInstance().createLiteral(0));

				return a;
			});
			select = new AggregateIteratorTypeOverride(select);
			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(select, connectionsGroup);
				logger.info(planAsGraphvizDot);
			}
			return new EnrichWithShape(select, this);

		}

		if (!optimizeWhenNoStatementsRemoved || connectionsGroup.getStats().hasRemoved()) {
			PlanNode planRemovedStatements = new Unique(
					new TrimTuple(getPlanRemovedStatements(connectionsGroup, null), 0, 1));

			PlanNode filteredPlanRemovedStatements = nodeShape.getTargetFilter(connectionsGroup,
					planRemovedStatements);

			PlanNode planAddedStatements = nodeShape.getPlanAddedStatements(connectionsGroup, null);

			PlanNode mergeNode = new UnionNode(planAddedStatements, filteredPlanRemovedStatements);

			PlanNode unique = new Unique(mergeNode);

			PlanNode planAddedStatements1 = getPlanAddedStatements(connectionsGroup, null);

			planAddedStatements1 = (nodeShape).getTargetFilter(connectionsGroup,
					planAddedStatements1);

			topNode = new UnionNode(unique, planAddedStatements1);

			// BulkedExternalLeftOuterJoin is slower, at least when the getPlanAddedStatements only returns
			// statements that have the correct type.
			// Persumably BulkedExternalLeftOuterJoin will be high if getPlanAddedStatements has a high number of
			// statements for other subjects that in "unique"
			// topNode = new BulkedExternalLeftOuterJoin(unique, shaclSailConnection.addedStatements,
			// getPath().getQuery()));

		} else {

			PlanNode planAddedForShape = nodeShape.getPlanAddedStatements(connectionsGroup, null);

			PlanNode addedByPath = getPlanAddedStatements(connectionsGroup, null);

			addedByPath = (nodeShape).getTargetFilter(connectionsGroup, addedByPath);

			topNode = new UnionNode(planAddedForShape, addedByPath);

		}

		PlanNode groupBy = new GroupByCount(topNode);

		PlanNode filteredStatements = new MinCountFilter(groupBy, minCount).getFalseNode(UnBufferedPlanNode.class);

		PlanNode minCountFilter = filteredStatements;

		PlanNode trimTuple = new Unique(new TrimTuple(minCountFilter, 0, 1));

		PlanNode bulkedExternalLeftOuterJoin2 = new BulkedExternalLeftOuterJoin(trimTuple,
				connectionsGroup.getBaseConnection(),
				getPath().getQuery("?a", "?c", null), false, null, "?a", "?c");

		PlanNode groupBy2 = new GroupByCount(bulkedExternalLeftOuterJoin2);

		PlanNode filteredStatements2 = new MinCountFilter(groupBy2, minCount).getFalseNode(UnBufferedPlanNode.class);

		if (printPlans) {
			String planAsGraphvizDot = getPlanAsGraphvizDot(filteredStatements2, connectionsGroup);
			logger.info(planAsGraphvizDot);
		}

		return new EnrichWithShape(filteredStatements2, this);

	}

	@Override
	public SourceConstraintComponent getSourceConstraintComponent() {
		return SourceConstraintComponent.MinCountConstraintComponent;
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
		MinCountPropertyShape that = (MinCountPropertyShape) o;
		return minCount == that.minCount;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), minCount);
	}

	@Override
	public String toString() {
		return "MinCountPropertyShape{" +
				"minCount=" + minCount +
				", path=" + getPath() +
				", id=" + id +
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

		return nodeShape.getTargetFilter(connectionsGroup, plan);
	}
}
