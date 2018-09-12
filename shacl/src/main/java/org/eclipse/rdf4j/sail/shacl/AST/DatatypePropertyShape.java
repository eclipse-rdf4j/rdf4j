/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.AST;


import org.apache.commons.lang.StringEscapeUtils;
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

import java.util.stream.Stream;

/**
 * @author Håvard Ottestad
 */
public class DatatypePropertyShape extends PathPropertyShape {

	private final Resource datatype;

	DatatypePropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape) {
		super(id, connection, nodeShape);

		try (Stream<Statement> stream = Iterations.stream(connection.getStatements(id, SHACL.DATATYPE, null, true))) {
			datatype = stream.map(Statement::getObject).map(v -> (Resource) v).findAny().orElseThrow(() -> new RuntimeException("Expected to find sh:datatype on " + id));
		}

	}


	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape) {

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


		if(shaclSailConnection.sail.isDebugPrintPlans()){
			System.out.println("digraph  {");
			System.out.println("labelloc=t;\nfontsize=30;\nlabel=\""+this.getClass().getSimpleName()+"\";");

			invalidValues.printPlan();
			System.out.println(System.identityHashCode(shaclSailConnection) + " [label=\"" + StringEscapeUtils.escapeJava("Base sail") + "\" nodeShape=pentagon fillcolor=lightblue style=filled];");
			System.out.println(System.identityHashCode(shaclSailConnection.getAddedStatements()) + " [label=\"" + StringEscapeUtils.escapeJava("Added statements") + "\" nodeShape=pentagon fillcolor=lightblue style=filled];");
			System.out.println(System.identityHashCode(shaclSailConnection.getRemovedStatements()) + " [label=\"" + StringEscapeUtils.escapeJava("Removed statements") + "\" nodeShape=pentagon fillcolor=lightblue style=filled];");
			System.out.println(System.identityHashCode(shaclSailConnection.getPreviousStateConnection()) + " [label=\"" + StringEscapeUtils.escapeJava("Previous state connection") + "\" nodeShape=pentagon fillcolor=lightblue style=filled];");

			System.out.println("}");

		}
		return new LoggingNode(invalidValues);

	}

	@Override
	public boolean requiresEvaluation(Repository addedStatements, Repository removedStatements) {
		return true;
	}
}
