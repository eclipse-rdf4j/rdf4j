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
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
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
 * sh:targetSubjectsOf
 *
 * @author Håvard Mikkelsen Ottestad
 */
public class TargetSubjectsOf extends NodeShape {

	private final Set<IRI> targetSubjectsOf;

	TargetSubjectsOf(Resource id, SailRepositoryConnection connection, boolean deactivated, Set<IRI> targetSubjectsOf) {
		super(id, connection, deactivated);
		this.targetSubjectsOf = targetSubjectsOf;
		assert !this.targetSubjectsOf.isEmpty();
	}

	@Override
	public PlanNode getPlan(ConnectionsGroup connectionsGroup, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {
		assert !negateSubPlans : "There are no subplans!";
		assert !negateThisPlan;

		PlanNode parent = connectionsGroup
				.getCachedNodeFor(
						new Select(connectionsGroup.getBaseConnection(), getQuery("?a", "?c", null), "?a", "?c"));
		return new Unique(new TrimTuple(parent, 0, 1));
	}

	@Override
	public PlanNode getPlanAddedStatements(ConnectionsGroup connection,
			PlaneNodeWrapper planeNodeWrapper) {

		PlanNode select;
		if (targetSubjectsOf.size() == 1) {
			IRI iri = targetSubjectsOf.stream().findAny().get();
			select = new Sort(new UnorderedSelect(connection.getAddedStatements(), null, iri, null,
					UnorderedSelect.OutputPattern.SubjectPredicateObject));
		} else {
			select = new Select(connection.getAddedStatements(), getQuery("?a", "?c", null), "?a", "?b1", "?c");
		}

		PlanNode cachedNodeFor = connection.getCachedNodeFor(select);
		return new Unique(new TrimTuple(cachedNodeFor, 0, 1));

	}

	@Override
	public PlanNode getPlanRemovedStatements(ConnectionsGroup connection,
			PlaneNodeWrapper planeNodeWrapper) {

		PlanNode select;
		if (targetSubjectsOf.size() == 1) {
			IRI iri = targetSubjectsOf.stream().findAny().get();
			select = new Sort(new UnorderedSelect(connection.getRemovedStatements(), null, iri, null,
					UnorderedSelect.OutputPattern.SubjectPredicateObject));
		} else {
			select = new Select(connection.getRemovedStatements(), getQuery("?a", "?c", null), "?a", "?b1", "?c");
		}

		PlanNode cachedNodeFor = connection.getCachedNodeFor(select);
		return new Unique(new TrimTuple(cachedNodeFor, 0, 1));

	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements) {
		return targetSubjectsOf.stream()
				.map(target -> addedStatements.hasStatement(null, target, null, false))
				.reduce((a, b) -> a || b)
				.orElseThrow(IllegalStateException::new);

	}

	@Override
	public String getQuery(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		return targetSubjectsOf.stream()
				.map(target -> "\n { BIND(<" + target + "> as ?b1) \n " + subjectVariable + " ?b1 " + objectVariable
						+ ".  } \n")
				.reduce((a, b) -> a + " UNION " + b)
				.get();
	}

	@Override
	public PlanNode getTargetFilter(SailConnection connection, PlanNode parent) {
		return new ExternalFilterByPredicate(connection, targetSubjectsOf, parent, 0,
				ExternalFilterByPredicate.On.Subject);
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
		TargetSubjectsOf that = (TargetSubjectsOf) o;
		return targetSubjectsOf.equals(that.targetSubjectsOf);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), targetSubjectsOf);
	}

	@Override
	public String toString() {
		return "TargetSubjectsOf{" +
				"targetSubjectsOf=" + Arrays.toString(targetSubjectsOf.toArray()) +
				'}';
	}
}
