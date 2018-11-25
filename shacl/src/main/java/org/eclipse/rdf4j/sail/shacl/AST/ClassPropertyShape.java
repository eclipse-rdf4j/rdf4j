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
import org.eclipse.rdf4j.sail.shacl.planNodes.ExternalTypeFilterNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.LeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans, boolean assumeBaseSailValid) {

		PlanNode addedByShape = new LoggingNode(nodeShape.getPlanAddedStatements(shaclSailConnection, nodeShape));
		PlanNode addedByPath = new LoggingNode(new Select(shaclSailConnection.getAddedStatements(), path.getQuery()));


		PlanNode leftOuterJoin = new LoggingNode(new InnerJoin(addedByShape, addedByPath, null,null));





		PlanNode externalTypeFilterNode = new ExternalTypeFilterNode(shaclSailConnection, classResource, leftOuterJoin, 1, false);


		return externalTypeFilterNode;

	}

	@Override
	public boolean requiresEvaluation(Repository addedStatements, Repository removedStatements) {
		return true;
	}
}
