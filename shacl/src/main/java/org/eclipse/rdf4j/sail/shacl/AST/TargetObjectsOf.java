/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.ExternalFilterByPredicate;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;

import java.util.Set;

/**
 * sh:targetObjectsOf
 *
 * @author Håvard Mikkelsen Ottestad
 */
public class TargetObjectsOf extends NodeShape {

	private final Set<IRI> targetObjectsOf;

	TargetObjectsOf(Resource id, SailRepositoryConnection connection, boolean deactivated, Set<IRI> targetObjectsOf) {
		super(id, connection, deactivated);
		this.targetObjectsOf = targetObjectsOf;
		assert !this.targetObjectsOf.isEmpty();

	}

	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans,
			PlanNode overrideTargetNode) {
		PlanNode parent = shaclSailConnection
				.getCachedNodeFor(new Select(shaclSailConnection, getQuery("?a", "?c", null), "?a", "?b1", "?c"));
		return new TrimTuple(new LoggingNode(parent, ""), 0, 1);
	}

	@Override
	public PlanNode getPlanAddedStatements(ShaclSailConnection shaclSailConnection, NodeShape nodeShape) {
		PlanNode cachedNodeFor = shaclSailConnection.getCachedNodeFor(
				new Select(shaclSailConnection.getAddedStatements(), getQuery("?a", "?c", null), "?a", "?b1", "?c"));
		return new TrimTuple(new LoggingNode(cachedNodeFor, ""), 0, 1);

	}

	@Override
	public PlanNode getPlanRemovedStatements(ShaclSailConnection shaclSailConnection, NodeShape nodeShape) {
		PlanNode parent = shaclSailConnection.getCachedNodeFor(
				new Select(shaclSailConnection.getRemovedStatements(), getQuery("?a", "?c", null), "?a", "?b1", "?c"));
		return new TrimTuple(parent, 0, 1);
	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements) {
		return targetObjectsOf.stream()
				.map(target -> addedStatements.hasStatement(null, target, null, false))
				.reduce((a, b) -> a || b)
				.orElseThrow(IllegalStateException::new);
	}

	@Override
	public String getQuery(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		return targetObjectsOf.stream()
				.map(target -> "\n{ BIND(<" + target + "> as ?b1) \n " + objectVariable + " ?b1 " + subjectVariable
						+ ". } \n")
				.reduce((a, b) -> a + " UNION " + b)
				.get();
	}

	@Override
	public PlanNode getTargetFilter(NotifyingSailConnection shaclSailConnection, PlanNode parent) {
		return new ExternalFilterByPredicate(shaclSailConnection, targetObjectsOf, parent, 0,
				ExternalFilterByPredicate.On.Object);
	}

}
