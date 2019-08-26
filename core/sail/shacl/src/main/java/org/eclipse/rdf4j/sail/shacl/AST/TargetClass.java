/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.planNodes.ExternalTypeFilterNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnorderedSelect;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * sh:targetClass
 *
 * @author Heshan Jayasinghe
 */
public class TargetClass extends NodeShape {

	private final Set<Resource> targetClass;

	TargetClass(Resource id, SailRepositoryConnection connection, boolean deactivated, Set<Resource> targetClass) {
		super(id, connection, deactivated);
		this.targetClass = targetClass;
		assert !this.targetClass.isEmpty();

	}

	@Override
	public PlanNode getPlan(ConnectionsGroup connectionsGroup, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {

		assert !negateSubPlans : "There are no subplans!";
		assert !negateThisPlan;

		PlanNode parent = connectionsGroup.getCachedNodeFor(new Select(connectionsGroup.getBaseConnection(),
				getQuery("?a", "?c", connectionsGroup.getRdfsSubClassOfReasoner()), "?a", "?c"));
		return new Unique(new TrimTuple(parent, 0, 1));
	}

	@Override
	public PlanNode getPlanAddedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {
		PlanNode planNode;
		if (targetClass.size() == 1) {
			Resource clazz = targetClass.stream().findAny().get();
			planNode = connectionsGroup
					.getCachedNodeFor(new Sort(new UnorderedSelect(connectionsGroup.getAddedStatements(), null,
							RDF.TYPE, clazz, UnorderedSelect.OutputPattern.SubjectPredicateObject)));
		} else {
			planNode = connectionsGroup.getCachedNodeFor(
					new Select(connectionsGroup.getAddedStatements(), getQuery("?a", "?c", null), "?a", "?c"));
		}

		return new Unique(new TrimTuple(planNode, 0, 1));

	}

	@Override
	public PlanNode getPlanRemovedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {
		PlanNode planNode;
		if (targetClass.size() == 1) {
			Resource clazz = targetClass.stream().findAny().get();
			planNode = connectionsGroup
					.getCachedNodeFor(new Sort(new UnorderedSelect(connectionsGroup.getRemovedStatements(), null,
							RDF.TYPE, clazz, UnorderedSelect.OutputPattern.SubjectPredicateObject)));
		} else {
			planNode = connectionsGroup.getCachedNodeFor(
					new Select(connectionsGroup.getRemovedStatements(), getQuery("?a", "?c", null), "?a", "?c"));
		}
		return new Unique(new TrimTuple(planNode, 0, 1));
	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements) {
		return targetClass.stream()
				.map(target -> addedStatements.hasStatement(null, RDF.TYPE, target, false))
				.reduce((a, b) -> a || b)
				.orElseThrow(IllegalStateException::new);

	}

	@Override
	public String getQuery(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		Set<Resource> targets = targetClass;

		if (rdfsSubClassOfReasoner != null) {
			targets = new HashSet<>(targets);

			targets = targets.stream()
					.flatMap(target -> rdfsSubClassOfReasoner.backwardsChain(target).stream())
					.collect(Collectors.toSet());
		}

		assert targets.size() >= 1;

		return targets.stream()
				.map(r -> "{ BIND(rdf:type as ?b1) \n BIND(<" + r + "> as " + objectVariable + ") \n " + subjectVariable
						+ " ?b1 " + objectVariable + ". } \n")
				.reduce((l, r) -> l + " UNION " + r)
				.get();

	}

	@Override
	public PlanNode getTargetFilter(SailConnection shaclSailConnection, PlanNode parent) {
		return new ExternalTypeFilterNode(shaclSailConnection, targetClass, parent, 0, true);
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
		TargetClass that = (TargetClass) o;
		return targetClass.equals(that.targetClass);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), targetClass);
	}

	@Override
	public String toString() {
		return "TargetClass{" +
				"targetClass=" + Arrays.asList(targetClass.toArray()) +
				'}';
	}
}
