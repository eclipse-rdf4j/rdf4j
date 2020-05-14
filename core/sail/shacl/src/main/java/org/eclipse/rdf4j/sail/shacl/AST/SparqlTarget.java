/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.Stats;
import org.eclipse.rdf4j.sail.shacl.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.planNodes.SparqlTargetSelect;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;

public class SparqlTarget extends NodeShape {

	private final String sparqlQuery;

	SparqlTarget(Resource id, SailRepositoryConnection connection, boolean deactivated, Resource sparqlTarget) {
		super(id, connection, deactivated);

		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		sparqlQuery = connection
				.getStatements(sparqlTarget, SHACL.SELECT, null)
				.stream()
				.map(Statement::getObject)
				.map(Value::stringValue)
				.findFirst()
				.get();

	}

	@Override
	public PlanNode getPlan(ConnectionsGroup connectionsGroup, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {

		assert !negateSubPlans : "There are no subplans!";
		assert !negateThisPlan;

		PlanNode planNode = connectionsGroup
				.getCachedNodeFor(new Sort(new SparqlTargetSelect(connectionsGroup.getBaseConnection(), sparqlQuery)));

		return new Unique(new TrimTuple(planNode, 0, 1));
	}

	@Override
	public PlanNode getPlanAddedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {

		PlanNode planNode = connectionsGroup
				.getCachedNodeFor(new Sort(new SparqlTargetSelect(connectionsGroup.getBaseConnection(), sparqlQuery)));

		return new Unique(new TrimTuple(planNode, 0, 1));

	}

	@Override
	public PlanNode getPlanRemovedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {
		PlanNode planNode = connectionsGroup
				.getCachedNodeFor(new Sort(new SparqlTargetSelect(connectionsGroup.getBaseConnection(), sparqlQuery)));

		return new Unique(new TrimTuple(planNode, 0, 1));
	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements, Stats stats) {
		return true;
	}

	@Override
	public String getQuery(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {

		// THIS IS A HACK. It's ok for the prototype, but needs to be fixed before we merge/release.
		return sparqlQuery.replace("?this", subjectVariable);

//		return " BIND(<" + targetPredicate + "> as ?b1) \n " +
//				"BIND(<" + targetClass + "> as " + objectVariable + ") \n " + subjectVariable
//				+ " ?b1 " + objectVariable + ".  \n";

	}

	@Override
	public PlanNode getTargetFilter(SailConnection shaclSailConnection, PlanNode parent) {
		// TODO fix this
		return new EmptyNode();
//		return new ExternalTypeFilterNode(shaclSailConnection, targetPredicate,
//				new HashSet<>(Arrays.asList(targetClass)), parent, 0, true);

	}

}
