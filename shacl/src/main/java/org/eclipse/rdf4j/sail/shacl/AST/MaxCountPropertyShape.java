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
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
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
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {

		if (deactivated) {
			return null;
		}
		assert !negateSubPlans : "There are no subplans!";
		assert !negateThisPlan;
		assert hasOwnPath();

		if (overrideTargetNode != null) {
			PlanNode bulkedExternalInnerJoin = new BulkedExternalInnerJoin(overrideTargetNode.getPlanNode(),
					shaclSailConnection, getPath().getQuery("?a", "?c", null), false, "?a", "?c");
			PlanNode groupByCount = new GroupByCount(bulkedExternalInnerJoin);

			PlanNode directTupleFromFilter = new MaxCountFilter(groupByCount, maxCount)
					.getFalseNode(UnBufferedPlanNode.class);

			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(directTupleFromFilter, shaclSailConnection);
				logger.info(planAsGraphvizDot);
			}

			return new EnrichWithShape(directTupleFromFilter, this);
		}

		if (maxCount == 1 && shaclSailConnection.stats.isBaseSailEmpty()) {
			String query = nodeShape.getQuery("?a", "?b", null);
			String query1 = getPath().getQuery("?a", "?d", null);
			String query2 = getPath().getQuery("?a", "?e", null);

			String negationQuery = query + "\n" + query1 + "\n" + query2 + "\nFILTER(?d != ?e)";

			PlanNode select = new Select(shaclSailConnection.getAddedStatements(), negationQuery, "?a");
			select = new ModifyTuple(select, (a) -> {
				a.line.add(SimpleValueFactory.getInstance().createLiteral(">= 2"));

				return a;
			});
			select = new AggregateIteratorTypeOverride(select);

			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(select, shaclSailConnection);
				logger.info(planAsGraphvizDot);
			}

			return new EnrichWithShape(select, this);

		}

		PlanNode planAddedStatements = nodeShape.getPlanAddedStatements(shaclSailConnection, null);

		PlanNode planAddedStatements1 = super.getPlanAddedStatements(shaclSailConnection, null);

		planAddedStatements1 = nodeShape.getTargetFilter(shaclSailConnection, planAddedStatements1);

		PlanNode mergeNode = new UnionNode(planAddedStatements, planAddedStatements1);

		PlanNode groupByCount1 = new GroupByCount(mergeNode);

		MaxCountFilter maxCountFilter = new MaxCountFilter(groupByCount1, maxCount);

		PlanNode validValues = maxCountFilter.getTrueNode(BufferedPlanNode.class);
		PlanNode invalidValues = maxCountFilter.getFalseNode(BufferedPlanNode.class);

		PlanNode mergeNode1;
		if (!shaclSailConnection.stats.isBaseSailEmpty()) {

			PlanNode trimmed = new TrimTuple(validValues, 0, 1);

			PlanNode unique = new Unique(trimmed);

			PlanNode bulkedExternalInnerJoin = new BulkedExternalInnerJoin(unique, shaclSailConnection,
					getPath().getQuery("?a", "?c", null),
					true, "?a", "?c");

			PlanNode groupByCount = new GroupByCount(bulkedExternalInnerJoin);

			PlanNode directTupleFromFilter = new MaxCountFilter(groupByCount, maxCount)
					.getFalseNode(UnBufferedPlanNode.class);

			mergeNode1 = new UnionNode(directTupleFromFilter,
					invalidValues);
		} else {
			mergeNode1 = invalidValues;
		}

		if (printPlans) {
			String planAsGraphvizDot = getPlanAsGraphvizDot(mergeNode1, shaclSailConnection);
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
