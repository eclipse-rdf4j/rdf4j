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
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.planNodes.BulkedExternalLeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.GroupByCount;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.MinCountFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The AST (Abstract Syntax Tree) node that represents a sh:minCount property nodeShape restriction.
 *
 * @author Heshan Jayasinghe
 */
public class MinCountPropertyShape extends PathPropertyShape {

	private long minCount;
	private static final Logger logger = LoggerFactory.getLogger(MinCountPropertyShape.class);

	// toggle for switching on and off the optimization used when no statements have been removed in a transaction
	private boolean optimizeWhenNoStatementsRemoved = true;


	MinCountPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape, Long minCount) {
		super(id, connection, nodeShape);

		this.minCount = minCount;

	}

	@Override
	public String toString() {
		return "MinCountPropertyShape{" + "minCount=" + minCount + '}';
	}

	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans, PlanNode overrideTargetNode) {

		if (overrideTargetNode != null) {
			PlanNode allStatements = new LoggingNode(new BulkedExternalLeftOuterJoin(overrideTargetNode, shaclSailConnection, path.getQuery("?a", "?c", null), false), "");
			PlanNode groupBy = new LoggingNode(new GroupByCount(allStatements), "");

			PlanNode filteredStatements = new MinCountFilter(groupBy, minCount).getFalseNode(UnBufferedPlanNode.class);

			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(filteredStatements, shaclSailConnection);
				logger.info(planAsGraphvizDot);
			}

			return new EnrichWithShape(new LoggingNode(filteredStatements, ""), this);

		}

		PlanNode topNode;

		if (!optimizeWhenNoStatementsRemoved || shaclSailConnection.stats.hasRemoved()) {
			PlanNode planRemovedStatements = new LoggingNode(new TrimTuple(new LoggingNode(super.getPlanRemovedStatements(shaclSailConnection, nodeShape), ""), 0, 1), "");

			PlanNode filteredPlanRemovedStatements = new LoggingNode(nodeShape.getTargetFilter(shaclSailConnection, planRemovedStatements), "");

			PlanNode planAddedStatements = new LoggingNode(nodeShape.getPlanAddedStatements(shaclSailConnection, nodeShape), "");

			PlanNode mergeNode = new LoggingNode(new UnionNode(planAddedStatements, filteredPlanRemovedStatements), "");

			PlanNode unique = new LoggingNode(new Unique(mergeNode), "");


			PlanNode planAddedStatements1 = super.getPlanAddedStatements(shaclSailConnection, nodeShape);

			planAddedStatements1 = new LoggingNode((nodeShape).getTargetFilter(shaclSailConnection, planAddedStatements1), "");

			topNode = new LoggingNode(new UnionNode(unique, planAddedStatements1), "");


			// BulkedExternalLeftOuterJoin is slower, at least when the super.getPlanAddedStatements only returns statements that have the correct type.
			// Persumably BulkedExternalLeftOuterJoin will be high if super.getPlanAddedStatements has a high number of statements for other subjects that in "unique"
			//topNode = new LoggingNode(new BulkedExternalLeftOuterJoin(unique, shaclSailConnection.addedStatements, path.getQuery()));

		} else {

			PlanNode planAddedForShape = new LoggingNode(nodeShape.getPlanAddedStatements(shaclSailConnection, nodeShape), "");

			PlanNode addedByPath = new LoggingNode(getPlanAddedStatements(shaclSailConnection, nodeShape), "");

			addedByPath = new LoggingNode((nodeShape).getTargetFilter(shaclSailConnection, addedByPath), "");

			topNode = new LoggingNode(new UnionNode(planAddedForShape, addedByPath), "");

		}


		PlanNode groupBy = new LoggingNode(new GroupByCount(topNode), "");

		PlanNode filteredStatements = new MinCountFilter(groupBy, minCount).getFalseNode(UnBufferedPlanNode.class);

		PlanNode minCountFilter = new LoggingNode(filteredStatements, "");

		PlanNode trimTuple = new LoggingNode(new TrimTuple(minCountFilter, 0, 1), "");

		PlanNode bulkedExternalLeftOuterJoin2 = new LoggingNode(new BulkedExternalLeftOuterJoin(trimTuple, shaclSailConnection, path.getQuery("?a", "?c", null), false), "");

		PlanNode groupBy2 = new LoggingNode(new GroupByCount(bulkedExternalLeftOuterJoin2), "");

		PlanNode filteredStatements2 = new MinCountFilter(groupBy2, minCount).getFalseNode(UnBufferedPlanNode.class);

		if (printPlans) {
			String planAsGraphvizDot = getPlanAsGraphvizDot(filteredStatements2, shaclSailConnection);
			logger.info(planAsGraphvizDot);
		}

		return new EnrichWithShape(new LoggingNode(filteredStatements2, ""), this);

	}

	@Override
	public SourceConstraintComponent getSourceConstraintComponent() {
		return SourceConstraintComponent.MinCountConstraintComponent;
	}
}
