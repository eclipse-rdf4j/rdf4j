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
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.planNodes.ExternalFilterByPredicate;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnorderedSelect;

import java.util.Arrays;
import java.util.Objects;
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
	public PlanNode getPlan(ConnectionsGroup connectionsGroup, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {
		assert !negateSubPlans : "There are no subplans!";
		assert !negateThisPlan;

		PlanNode parent = connectionsGroup
				.getCachedNodeFor(
						new Select(connectionsGroup.getBaseConnection(), getQuery("?a", "?c", null), "?a", "?b1",
								"?c"));
		return new Unique(new TrimTuple(parent, 0, 1));
	}

	@Override
	public PlanNode getPlanAddedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {

		PlanNode select;
		if (targetObjectsOf.size() == 1) {
			IRI iri = targetObjectsOf.stream().findAny().get();

			select = new Sort(new UnorderedSelect(connectionsGroup.getAddedStatements(), null, iri, null,
					UnorderedSelect.OutputPattern.ObjectPredicateSubject));
		} else {
			select = new Select(connectionsGroup.getAddedStatements(), getQuery("?a", "?c", null), "?a", "?b1",
					"?c");
		}

		PlanNode cachedNodeFor = connectionsGroup.getCachedNodeFor(select);
		return new Unique(new TrimTuple(cachedNodeFor, 0, 1));

	}

	@Override
	public PlanNode getPlanRemovedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {
		PlanNode select;
		if (targetObjectsOf.size() == 1) {
			IRI iri = targetObjectsOf.stream().findAny().get();

			select = new Sort(new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, iri, null,
					UnorderedSelect.OutputPattern.ObjectPredicateSubject));
		} else {
			select = new Select(connectionsGroup.getRemovedStatements(), getQuery("?a", "?c", null), "?a", "?b1",
					"?c");
		}

		PlanNode cachedNodeFor = connectionsGroup.getCachedNodeFor(select);
		return new Unique(new TrimTuple(cachedNodeFor, 0, 1));
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
	public PlanNode getTargetFilter(SailConnection shaclSailConnection, PlanNode parent) {
		return new ExternalFilterByPredicate(shaclSailConnection, targetObjectsOf, parent, 0,
				ExternalFilterByPredicate.On.Object);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		TargetObjectsOf that = (TargetObjectsOf) o;
		return targetObjectsOf.equals(that.targetObjectsOf);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), targetObjectsOf);
	}

	@Override
	public String toString() {
		return "TargetObjectsOf{" +
				"targetObjectsOf=" + Arrays.toString(targetObjectsOf.toArray()) +
				'}';
	}
}
