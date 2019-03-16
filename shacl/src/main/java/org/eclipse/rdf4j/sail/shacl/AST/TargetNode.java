/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.SetFilterNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * sh:targetNode
 *
 * @author Håvard Ottestad
 */
public class TargetNode extends NodeShape {

	private final Set<Value> targetNodeSet;

	TargetNode(Resource id, SailRepositoryConnection connection, List<Value> targetNode) {
		super(id, connection);
		this.targetNodeSet = new HashSet<>(targetNode);
		assert !this.targetNodeSet.isEmpty();
	}

	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans,
			PlanNode overrideTargetNode) {
		PlanNode parent = shaclSailConnection.getCachedNodeFor(
				new Select(shaclSailConnection, getQuery("?a", "?c", shaclSailConnection.getRdfsSubClassOfReasoner())));
		return new TrimTuple(new LoggingNode(parent, ""), 0, 1);
	}

	@Override
	public PlanNode getPlanAddedStatements(ShaclSailConnection shaclSailConnection, NodeShape nodeShape) {
		PlanNode parent = shaclSailConnection
				.getCachedNodeFor(new Select(shaclSailConnection.getAddedStatements(), getQuery("?a", "?c", null)));
		return new TrimTuple(new LoggingNode(parent, ""), 0, 1);

	}

	@Override
	public PlanNode getPlanRemovedStatements(ShaclSailConnection shaclSailConnection, NodeShape nodeShape) {
		PlanNode parent = shaclSailConnection
				.getCachedNodeFor(new Select(shaclSailConnection.getRemovedStatements(), getQuery("?a", "?c", null)));
		return new TrimTuple(parent, 0, 1);
	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements) {
		return true;
	}

	@Override
	public String getQuery(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {

		return targetNodeSet.stream()
				.map(r -> "{{ select * where {BIND(<" + r + "> as " + subjectVariable + "). " + subjectVariable
						+ " ?b1 " + objectVariable + " .}}}")
				.reduce((a, b) -> a + " UNION " + b)
				.get();

	}

	@Override
	public PlanNode getTargetFilter(NotifyingSailConnection shaclSailConnection, PlanNode parent) {
		return new LoggingNode(new SetFilterNode(targetNodeSet, parent, 0, true), "targetNode filter");
	}

}
