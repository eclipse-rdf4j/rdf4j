/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
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
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.Stats;
import org.eclipse.rdf4j.sail.shacl.planNodes.ExternalFilterByPredicate;
import org.eclipse.rdf4j.sail.shacl.planNodes.ExternalFilterIsObject;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnorderedSelect;

/**
 * sh:targetObjectsOf
 *
 * @author Håvard Mikkelsen Ottestad
 */
public class AllObjectsTarget extends NodeShape {

	AllObjectsTarget(Resource id, SailRepositoryConnection connection, boolean deactivated) {
		super(id, connection, deactivated);
	}

	@Override
	public PlanNode getPlan(ConnectionsGroup connectionsGroup, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {
		assert !negateSubPlans : "There are no subplans!";
		assert !negateThisPlan;

		PlanNode select = new Unique(
				new Sort(
						new TrimTuple(
								new UnorderedSelect(
										connectionsGroup.getBaseConnection(),
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

		PlanNode select = new Unique(
				new Sort(
						new TrimTuple(
								new UnorderedSelect(
										connectionsGroup.getAddedStatements(),
										null,
										null,
										null,
										UnorderedSelect.OutputPattern.ObjectPredicateSubject),
								0,
								1)));

		return connectionsGroup.getCachedNodeFor(select);
	}

	@Override
	public PlanNode getPlanRemovedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {
		PlanNode select = new Unique(
				new Sort(
						new TrimTuple(
								new UnorderedSelect(
										connectionsGroup.getRemovedStatements(),
										null,
										null,
										null,
										UnorderedSelect.OutputPattern.ObjectPredicateSubject),
								0,
								1)));

		return connectionsGroup.getCachedNodeFor(select);
	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements, Stats stats) {
		if (stats.isEmpty()) {
			return false;
		}

		return true;
	}

	@Override
	public String getQuery(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		return objectVariable + " ?allObjectsTarget" + UUID.randomUUID().toString().replace("-", "") + " "
				+ subjectVariable + " .";
	}

	@Override
	public PlanNode getTargetFilter(SailConnection shaclSailConnection, PlanNode parent) {
		assertConnectionIsShaclSailConnection(shaclSailConnection);
		return new ExternalFilterIsObject(shaclSailConnection, parent, 0).getTrueNode(UnBufferedPlanNode.class);
	}

	@Override
	public String toString() {
		return "AllSubjectsTarget{" +
				"id=" + id +
				'}';
	}
}
