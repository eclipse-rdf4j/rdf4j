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
import org.eclipse.rdf4j.sail.shacl.planNodes.BufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.BulkedExternalLeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.GroupByCount;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.MaxCountFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The AST (Abstract Syntax Tree) node that represents a sh:maxCount property nodeShape restriction.
 *
 * @author Håvard Ottestad
 */
public class MaxCountPropertyShape extends PathPropertyShape {

	private static final Logger logger = LoggerFactory.getLogger(MaxCountPropertyShape.class);

	private long maxCount;

	MaxCountPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape, Long maxCount) {
		super(id, connection, nodeShape);

		this.maxCount = maxCount;

	}

	@Override
	public String toString() {
		return "MaxCountPropertyShape{" + "maxCount=" + maxCount + '}';
	}

	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans, PlanNode overrideTargetNode) {

		if (overrideTargetNode != null) {
			PlanNode bulkedExternalLeftOuterJoin = new LoggingNode(new BulkedExternalLeftOuterJoin(overrideTargetNode, shaclSailConnection, path.getQuery("?a", "?c", null), false), "");
			PlanNode groupByCount = new LoggingNode(new GroupByCount(bulkedExternalLeftOuterJoin), "");

			PlanNode directTupleFromFilter = new LoggingNode(new MaxCountFilter(groupByCount, maxCount).getFalseNode(UnBufferedPlanNode.class), "");

			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(directTupleFromFilter, shaclSailConnection);
				logger.info(planAsGraphvizDot);
			}

			return new EnrichWithShape(new LoggingNode(directTupleFromFilter, ""), this);
		}


		PlanNode planAddedStatements = new LoggingNode(nodeShape.getPlanAddedStatements(shaclSailConnection, nodeShape), "");

		PlanNode planAddedStatements1 = new LoggingNode(super.getPlanAddedStatements(shaclSailConnection, nodeShape), "");

		planAddedStatements1 = new LoggingNode(nodeShape.getTargetFilter(shaclSailConnection, planAddedStatements1), "");

		PlanNode mergeNode = new LoggingNode(new UnionNode(planAddedStatements, planAddedStatements1), "");

		PlanNode groupByCount1 = new LoggingNode(new GroupByCount(mergeNode), "");


		MaxCountFilter maxCountFilter = new MaxCountFilter(groupByCount1, maxCount);

		PlanNode validValues = maxCountFilter.getTrueNode(BufferedPlanNode.class);
		PlanNode invalidValues = maxCountFilter.getFalseNode(BufferedPlanNode.class);

		PlanNode trimmed = new LoggingNode(new TrimTuple(validValues, 0, 1), "");

		PlanNode unique = new LoggingNode(new Unique(trimmed), "");

		PlanNode bulkedExternalLeftOuterJoin = new LoggingNode(new BulkedExternalLeftOuterJoin(unique, shaclSailConnection, path.getQuery("?a", "?c", null), false), "");

		PlanNode groupByCount = new LoggingNode(new GroupByCount(bulkedExternalLeftOuterJoin), "");

		PlanNode directTupleFromFilter = new MaxCountFilter(groupByCount, maxCount).getFalseNode(UnBufferedPlanNode.class);

		PlanNode mergeNode1 = new UnionNode(new LoggingNode(directTupleFromFilter, ""), new LoggingNode(invalidValues, ""));

		if (printPlans) {
			String planAsGraphvizDot = getPlanAsGraphvizDot(mergeNode1, shaclSailConnection);
			logger.info(planAsGraphvizDot);
		}

		return new EnrichWithShape(new LoggingNode(mergeNode1, ""), this);

	}

	@Override
	public SourceConstraintComponent getSourceConstraintComponent() {
		return SourceConstraintComponent.MaxCountConstraintComponent;
	}
}
