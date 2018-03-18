/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
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
import org.eclipse.rdf4j.sail.shacl.planNodes.MergeNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;

import java.util.stream.Stream;

/**
 * @author Håvard Ottestad
 */
public class DatatypePropertyShape extends PathPropertyShape {

	private final Resource datatype;

	DatatypePropertyShape(Resource id, SailRepositoryConnection connection, Shape shape) {
		super(id, connection, shape);

		try (Stream<Statement> stream = Iterations.stream(connection.getStatements(id, SHACL.DATATYPE, null, true))) {
			datatype = stream.map(Statement::getObject).map(v -> (Resource) v).findAny().orElseThrow(() -> new RuntimeException("Expected to find sh:datatype on " + id));
		}

	}


	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, Shape shape) {

		PlanNode addedByShape = new LoggingNode(shape.getPlanAddedStatements(shaclSailConnection, shape));

		BufferedSplitter bufferedSplitter = new BufferedSplitter(addedByShape);

		PlanNode addedByPath = new LoggingNode(new Select(shaclSailConnection.addedStatements, path.getQuery()));

		BufferedTupleFromFilter discardedRight = new BufferedTupleFromFilter();


		PlanNode top = new LoggingNode(new InnerJoin(bufferedSplitter.getPlanNode(), addedByPath, null, discardedRight));


		if (shape instanceof TargetClass) {
			PlanNode typeFilterPlan = new LoggingNode(((TargetClass) shape).getTypeFilterPlan(shaclSailConnection.separateConnection, discardedRight));

			top = new LoggingNode(new MergeNode(top, typeFilterPlan));
		}

		PlanNode bulkedExternalLeftOuterJoin = new LoggingNode(new BulkedExternalInnerJoin(bufferedSplitter.getPlanNode(), shaclSailConnection.separateConnection, path.getQuery()));

		top = new LoggingNode(new MergeNode(top, bulkedExternalLeftOuterJoin));

		DirectTupleFromFilter invalidValues = new DirectTupleFromFilter();
		new DatatypeFilter(top, null, invalidValues, datatype);


		return new LoggingNode(invalidValues);

	}

	@Override
	public boolean requiresEvalutation(Repository addedStatements, Repository removedStatements) {
		return true;
	}
}
