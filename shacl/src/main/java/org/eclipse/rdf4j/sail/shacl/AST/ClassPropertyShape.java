/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.AST;


import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.planNodes.BufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.BulkedExternalLeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.ExternalTypeFilterNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.ModifyTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.planNodes.TupleLengthFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Håvard Ottestad
 */
public class ClassPropertyShape extends PathPropertyShape {

	private final Resource classResource;
	private static final Logger logger = LoggerFactory.getLogger(ClassPropertyShape.class);

	ClassPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape) {
		super(id, connection, nodeShape);

		try (Stream<Statement> stream = Iterations.stream(connection.getStatements(id, SHACL.CLASS, null, true))) {
			classResource = stream.map(Statement::getObject).map(v -> (Resource) v).findAny().orElseThrow(() -> new RuntimeException("Expected to find sh:class on " + id));
		}

	}


	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans, PlanNode overrideTargetNode) {

		SailConnection addedStatements = shaclSailConnection.getAddedStatements();

		if (overrideTargetNode != null) {
			PlanNode bulkedEternalLeftOuter = new LoggingNode(new BulkedExternalLeftOuterJoin(overrideTargetNode, shaclSailConnection, path.getQuery("?a", "?c", null), false), "");
			// filter by type against addedStatements, this is an optimization for when you add the type statement in the same transaction
			PlanNode addedStatementsTypeFilter = new LoggingNode(new ExternalTypeFilterNode(addedStatements, classResource, bulkedEternalLeftOuter, 1, false), "");

			// filter by type against the base sail
			PlanNode invalidTuplesDueToDataAddedThatMatchesTargetOrPath = new LoggingNode(new ExternalTypeFilterNode(shaclSailConnection, classResource, addedStatementsTypeFilter, 1, false), "");
			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(invalidTuplesDueToDataAddedThatMatchesTargetOrPath, shaclSailConnection);
				logger.info(planAsGraphvizDot);
			}
			return new EnrichWithShape(invalidTuplesDueToDataAddedThatMatchesTargetOrPath, this);
		} else {


			PlanNode addedByPath = new LoggingNode(getPlanAddedStatements(shaclSailConnection, nodeShape), "");

			// join all added by type and path
			InnerJoin innerJoinHolder = new InnerJoin(new LoggingNode(nodeShape.getPlanAddedStatements(shaclSailConnection, nodeShape), ""), addedByPath);
			PlanNode innerJoin = new LoggingNode(innerJoinHolder.getJoined(BufferedPlanNode.class), "");
			PlanNode discardedRight = new LoggingNode(innerJoinHolder.getDiscardedRight(BufferedPlanNode.class), "");

			PlanNode typeFilterPlan = new LoggingNode(nodeShape.getTargetFilter(shaclSailConnection, discardedRight), "");

			innerJoin = new LoggingNode(new Unique(new UnionNode(innerJoin, typeFilterPlan)), "");


			// also add anything that matches the path from the previousConnection, eg. if you add ":peter a foaf:Person", and ":peter foaf:knows :steve" is already added
			PlanNode bulkedExternalLeftOuter = new LoggingNode(new BulkedExternalLeftOuterJoin(new LoggingNode(nodeShape.getPlanAddedStatements(shaclSailConnection, nodeShape), ""), shaclSailConnection, path.getQuery("?a", "?c", null), true), "");

			// only get tuples that came from the first or the innerJoin or bulkedExternalLeftOuter,
			// we don't care if you added ":peter a foaf:Person" and nothing else and there is nothing else in the underlying sail
			PlanNode joined = new TupleLengthFilter(new UnionNode(innerJoin, bulkedExternalLeftOuter), 2, false).getTrueNode(UnBufferedPlanNode.class);

			// filter by type against addedStatements, this is an optimization for when you add the type statement in the same transaction
			PlanNode addedStatementsTypeFilter = new LoggingNode(new ExternalTypeFilterNode(addedStatements, classResource, joined, 1, false), "");

			// filter by type against the base sail
			PlanNode invalidTuplesDueToDataAddedThatMatchesTargetOrPath = new LoggingNode(new ExternalTypeFilterNode(shaclSailConnection, classResource, addedStatementsTypeFilter, 1, false), "");

			if (shaclSailConnection.stats.hasRemoved()) {

				// Handle when a type statement has been removed, first get all removed type statements that match the classResource for this shape
				PlanNode removedTypeStatements = new LoggingNode(new Select(shaclSailConnection.getRemovedStatements(), "?a a <" + classResource + ">"), "removedTypeStatements");

				// Build a query to run against the base sail. eg:
				//	?c foaf:knows ?a.
				// ?c a foaf:Person.
				String query = path.getQuery("?c", "?a", null) + nodeShape.getQuery("?c", "?q", shaclSailConnection.getRdfsSubClassOfReasoner());

				// do bulked external join for the removed class statements again the query above.
				// Essentially gets data that is now invalid because of the removed type statement
				PlanNode invalidDataDueToRemovedTypeStatement = new Sort(new ModifyTuple(new LoggingNode(new BulkedExternalInnerJoin(removedTypeStatements, shaclSailConnection, query, false), ""), t -> {
					List<Value> line = t.line;
					t.line = new ArrayList<>();
					t.line.add(line.get(2));
					t.line.add(line.get(0));


					return t;
				}));

				invalidTuplesDueToDataAddedThatMatchesTargetOrPath = new LoggingNode(new UnionNode(invalidTuplesDueToDataAddedThatMatchesTargetOrPath, invalidDataDueToRemovedTypeStatement), "");
			}

			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(invalidTuplesDueToDataAddedThatMatchesTargetOrPath, shaclSailConnection);
				logger.info(planAsGraphvizDot);
			}

			return new EnrichWithShape(invalidTuplesDueToDataAddedThatMatchesTargetOrPath, this);
		}
	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements) {
		return removedStatements.hasStatement(null, RDF.TYPE, classResource, true) || super.requiresEvaluation(addedStatements, removedStatements);
	}

	@Override
	public SourceConstraintComponent getSourceConstraintComponent() {
		return SourceConstraintComponent.ClassConstraintComponent;
	}
}
