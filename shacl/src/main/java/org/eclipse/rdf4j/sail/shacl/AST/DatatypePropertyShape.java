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
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;

import org.eclipse.rdf4j.sail.shacl.planNodes.BufferedSplitter;
import org.eclipse.rdf4j.sail.shacl.planNodes.BufferedTupleFromFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.DatatypeFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.DirectTupleFromFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

/**
 * @author Håvard Ottestad
 */
public class DatatypePropertyShape extends PathPropertyShape {

	private final Resource datatype;
	private static final Logger logger = LoggerFactory.getLogger(DatatypePropertyShape.class);

	DatatypePropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape) {
		super(id, connection, nodeShape);

		try (Stream<Statement> stream = Iterations.stream(connection.getStatements(id, SHACL.DATATYPE, null, true))) {
			datatype = stream.map(Statement::getObject).map(v -> (Resource) v).findAny().orElseThrow(() -> new RuntimeException("Expected to find sh:datatype on " + id));
		}

	}


	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans, boolean assumeBaseSailValid) {

		PlanNode addedByShape = new LoggingNode(nodeShape.getPlanAddedStatements(shaclSailConnection, nodeShape));

		BufferedSplitter bufferedSplitter = new BufferedSplitter(addedByShape);

		PlanNode addedByPath = new LoggingNode(new Select(shaclSailConnection.getAddedStatements(), path.getQuery()));

		// this is essentially pushing the filter down below the join
		DirectTupleFromFilter invalidValuesDirectOnPath = new DirectTupleFromFilter();
		new DatatypeFilter(addedByPath, null, invalidValuesDirectOnPath, datatype);

		BufferedTupleFromFilter discardedRight = new BufferedTupleFromFilter();


		PlanNode top = new LoggingNode(new InnerJoin(bufferedSplitter.getPlanNode(), invalidValuesDirectOnPath, null, discardedRight));


		if (nodeShape instanceof TargetClass) {
			PlanNode typeFilterPlan = new LoggingNode(((TargetClass) nodeShape).getTypeFilterPlan(shaclSailConnection.getPreviousStateConnection(), discardedRight));

			top = new LoggingNode(new UnionNode(top, typeFilterPlan));
		}

		PlanNode bulkedEcternalInnerJoin = new LoggingNode(new BulkedExternalInnerJoin(bufferedSplitter.getPlanNode(), shaclSailConnection.getPreviousStateConnection(), path.getQuery()));

		top = new LoggingNode(new UnionNode(top, bulkedEcternalInnerJoin));

		DirectTupleFromFilter invalidValues = new DirectTupleFromFilter();
		new DatatypeFilter(top, null, invalidValues, datatype);

		if(printPlans){
			String planAsGraphvizDot = getPlanAsGraphvizDot(invalidValues, shaclSailConnection);
			logger.info(planAsGraphvizDot);
		}

		return new LoggingNode(invalidValues);

	}

	@Override
	public boolean requiresEvaluation(Repository addedStatements, Repository removedStatements) {
		return true;
	}
}
