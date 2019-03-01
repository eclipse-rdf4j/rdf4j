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
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.ExternalTypeFilterNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.SetFilterNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The AST (Abstract Syntax Tree) node
 *
 * @author Heshan Jayasinghe
 */
public class TargetNode extends NodeShape {

	Set<Value> targetNodeList;

	TargetNode(Resource id, SailRepositoryConnection connection) {
		super(id, connection);

		try (Stream<Statement> stream = Iterations.stream(connection.getStatements(id, SHACL.TARGET_NODE, null))) {
			targetNodeList = stream.map(Statement::getObject).collect(Collectors.toSet());
		}

	}

	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans, PlanNode overrideTargetNode) {
		return new TrimTuple(new LoggingNode(new Select(shaclSailConnection, getQuery("?a", "?c", shaclSailConnection.getRdfsSubClassOfReasoner())), ""), 0, 1);
	}

	@Override
	public PlanNode getPlanAddedStatements(ShaclSailConnection shaclSailConnection, NodeShape nodeShape) {
		return new TrimTuple(new LoggingNode(new Select(shaclSailConnection.getAddedStatements(), getQuery("?a", "?c", null)), ""), 0, 1);

	}

	@Override
	public PlanNode getPlanRemovedStatements(ShaclSailConnection shaclSailConnection, NodeShape nodeShape) {
		return new TrimTuple(new Select(shaclSailConnection.getRemovedStatements(), getQuery("?a", "?c", null)), 0, 1);
	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements) {
		return true;
	}

	@Override
	public String getQuery(String subjectVariable, String objectVariable, RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {

		return targetNodeList.stream()
			.map(r -> "{{ select * where {BIND(<" + r + "> as " + subjectVariable + "). " + subjectVariable + " ?b1 " + objectVariable + " .}}}")
			.reduce((a, b) -> a + " UNION " + b)
			.get();

	}

	@Override
	public PlanNode getTargetFilter(NotifyingSailConnection shaclSailConnection, PlanNode parent) {
		return new LoggingNode(new SetFilterNode(targetNodeList, parent, 0, true), "targetNode filter");
	}

}
