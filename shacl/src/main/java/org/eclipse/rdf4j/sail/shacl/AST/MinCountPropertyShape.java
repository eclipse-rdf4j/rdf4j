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
import org.eclipse.rdf4j.sail.shacl.planNodes.DirectTupleFromFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.GroupByCount;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.MinCountFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
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

		if(overrideTargetNode != null){
			PlanNode allStatements = new LoggingNode(new BulkedExternalLeftOuterJoin(overrideTargetNode, shaclSailConnection, path.getQuery("?a", "?c"), false), "");
			PlanNode groupBy = new LoggingNode(new GroupByCount(allStatements), "");

			DirectTupleFromFilter filteredStatements = new DirectTupleFromFilter();
			new MinCountFilter(groupBy, null, filteredStatements, minCount);

			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(filteredStatements, shaclSailConnection);
				logger.info(planAsGraphvizDot);
			}

			return new EnrichWithShape(new LoggingNode(filteredStatements, ""), this);

		}

		PlanNode topNode;

		if (!optimizeWhenNoStatementsRemoved || shaclSailConnection.stats.hasRemoved()) {
			PlanNode planRemovedStatements = new LoggingNode(new TrimTuple(new LoggingNode(super.getPlanRemovedStatements(shaclSailConnection, nodeShape), ""), 0, 1), "");

			PlanNode filteredPlanRemovedStatements = planRemovedStatements;

			if (nodeShape instanceof TargetClass) {
				filteredPlanRemovedStatements = new LoggingNode(((TargetClass) nodeShape).getTypeFilterPlan(shaclSailConnection, planRemovedStatements), "");
			}

			PlanNode planAddedStatements = new LoggingNode(nodeShape.getPlanAddedStatements(shaclSailConnection, nodeShape), "");

			PlanNode mergeNode = new LoggingNode(new UnionNode(planAddedStatements, filteredPlanRemovedStatements), "");

			PlanNode unique = new LoggingNode(new Unique(mergeNode), "");


			PlanNode planAddedStatements1 = super.getPlanAddedStatements(shaclSailConnection, nodeShape);

			if (nodeShape instanceof TargetClass) {
				planAddedStatements1 = new LoggingNode(((TargetClass) nodeShape).getTypeFilterPlan(shaclSailConnection, planAddedStatements1), "");
			}
			topNode = new LoggingNode(new UnionNode(unique, planAddedStatements1), "");


			// BulkedExternalLeftOuterJoin is slower, at least when the super.getPlanAddedStatements only returns statements that have the correct type.
			// Persumably BulkedExternalLeftOuterJoin will be high if super.getPlanAddedStatements has a high number of statements for other subjects that in "unique"
			//topNode = new LoggingNode(new BulkedExternalLeftOuterJoin(unique, shaclSailConnection.addedStatements, path.getQuery()));

		} else {

			PlanNode planAddedForShape = new LoggingNode(nodeShape.getPlanAddedStatements(shaclSailConnection, nodeShape), "");

			PlanNode select = new LoggingNode(shaclSailConnection.getCachedNodeFor(new Select(shaclSailConnection.getAddedStatements(), path.getQuery("?a", "?c"))), "");


			if (nodeShape instanceof TargetClass) {
				select = new LoggingNode(((TargetClass) nodeShape).getTypeFilterPlan(shaclSailConnection, select), "");
			}
			topNode = new LoggingNode(new UnionNode(planAddedForShape, select), "");

		}


		PlanNode groupBy = new LoggingNode(new GroupByCount(topNode), "");

		DirectTupleFromFilter filteredStatements = new DirectTupleFromFilter();
		new MinCountFilter(groupBy, null, filteredStatements, minCount);

		PlanNode minCountFilter = new LoggingNode(filteredStatements, "");

		PlanNode trimTuple = new LoggingNode(new TrimTuple(minCountFilter, 0, 1), "");

		PlanNode bulkedExternalLeftOuterJoin2 = new LoggingNode(new BulkedExternalLeftOuterJoin(trimTuple, shaclSailConnection, path.getQuery("?a", "?c"), false), "");

		PlanNode groupBy2 = new LoggingNode(new GroupByCount(bulkedExternalLeftOuterJoin2), "");

		DirectTupleFromFilter filteredStatements2 = new DirectTupleFromFilter();
		new MinCountFilter(groupBy2, null, filteredStatements2, minCount);

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
