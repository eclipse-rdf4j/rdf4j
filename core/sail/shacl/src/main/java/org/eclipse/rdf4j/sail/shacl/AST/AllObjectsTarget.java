/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import java.util.UUID;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.Stats;
import org.eclipse.rdf4j.sail.shacl.planNodes.ExternalFilterIsObject;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnorderedSelect;

/**
 *
 * @author Håvard Mikkelsen Ottestad
 */
public class AllObjectsTarget extends NodeShape {

	AllObjectsTarget(Resource id, ShaclSail shaclSail, SailRepositoryConnection connection, boolean deactivated) {
		super(id, shaclSail, connection, deactivated);
	}

	@Override
	public PlanNode getPlan(ConnectionsGroup connectionsGroup, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {
		assert !negateSubPlans : "There are no subplans!";
		assert !negateThisPlan;

		return getPlanNode(connectionsGroup, connectionsGroup.getBaseConnection());
	}

	private PlanNode getPlanNode(ConnectionsGroup connectionsGroup, SailConnection baseConnection) {
		PlanNode select = new Unique(
				new Sort(
						new TrimTuple(
								new UnorderedSelect(
										baseConnection,
										null,
										null,
										null,
										UnorderedSelect.OutputPattern.ObjectPredicateSubject),
								0,
								1)));

		return connectionsGroup.getCachedNodeFor(select);
	}

	@Override
	public PlanNode getPlanAddedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {

		assert planeNodeWrapper == null;
		return getPlanNode(connectionsGroup, connectionsGroup.getAddedStatements());
	}

	@Override
	public PlanNode getPlanRemovedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {
		assert planeNodeWrapper == null;
		return getPlanNode(connectionsGroup, connectionsGroup.getRemovedStatements());
	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements, Stats stats) {
		return !stats.isEmpty();
	}

	@Override
	public String getQuery(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		return objectVariable + " ?allObjectsTarget" + UUID.randomUUID().toString().replace("-", "") + " "
				+ subjectVariable + " .";
	}

	@Override
	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, PlanNode parent) {
		return new ExternalFilterIsObject(connectionsGroup.getBaseConnection(), parent, 0)
				.getTrueNode(UnBufferedPlanNode.class);
	}

	@Override
	public String toString() {
		return "AllSubjectsTarget{" +
				"id=" + id +
				'}';
	}

}
