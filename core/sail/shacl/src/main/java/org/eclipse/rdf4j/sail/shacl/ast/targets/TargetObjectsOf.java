/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.targets;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ExternalFilterByPredicate;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnorderedSelect;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;

public class TargetObjectsOf extends Target {

	private final Set<IRI> targetObjectsOf;

	public TargetObjectsOf(Set<IRI> targetObjectsOf) {
		this.targetObjectsOf = targetObjectsOf;
		assert !this.targetObjectsOf.isEmpty();

	}

	@Override
	public IRI getPredicate() {
		return SHACL.TARGET_OBJECTS_OF;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		targetObjectsOf.forEach(t -> {
			model.add(subject, getPredicate(), t);
		});
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			ConstraintComponent.Scope scope) {
		return getAddedRemovedInner(connectionsGroup.getAddedStatements(), dataGraph, scope);
	}

	private PlanNode getAddedRemovedInner(SailConnection connection, Resource[] dataGraph,
			ConstraintComponent.Scope scope) {

		PlanNode planNode = targetObjectsOf.stream()
				.map(predicate -> (PlanNode) new UnorderedSelect(connection, null,
						predicate, null, dataGraph, UnorderedSelect.Mapper.ObjectScopedMapper.getFunction(scope)))
				.reduce(UnionNode::getInstance)
				.orElse(EmptyNode.getInstance());

		return Unique.getInstance(planNode, false);
	}

	@Override
	public String getQueryFragment(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		String tempVar = stableRandomVariableProvider.next().asSparqlVariable();

		return targetObjectsOf.stream()
				.map(target -> "\n{ BIND(<" + target + "> as " + tempVar + ") \n " + objectVariable + " "
						+ tempVar + " " + subjectVariable
						+ ". } \n")
				.reduce((a, b) -> a + " UNION " + b)
				.get();
	}

	@Override
	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			PlanNode parent) {
		return new ExternalFilterByPredicate(connectionsGroup.getBaseConnection(), targetObjectsOf, parent,
				ExternalFilterByPredicate.On.Object, dataGraph);
	}

	@Override
	public Stream<StatementMatcher> getStatementMatcher(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		assert (subject == null);

		return targetObjectsOf.stream()
				.map(t -> new StatementMatcher(
						null,
						new StatementMatcher.Variable(t),
						object
				)
				);
	}

	@Override
	public String getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		assert (subject == null);

		String tempVar = stableRandomVariableProvider.next().asSparqlVariable();

		if (targetObjectsOf.size() == 1) {

			return targetObjectsOf.stream()
					.map(t -> tempVar + " <" + t + "> ?" + object.getName() + " .")
					.reduce((a, b) -> a + "\n" + b)
					.orElse("");

		} else {

			String in = targetObjectsOf.stream()
					.map(t -> "<" + t + ">")
					.reduce((a, b) -> a + " , " + b)
					.orElse("");

			return tempVar + " ?predicatefjhfuewhw ?" + object.getName() + " .\n" +
					"FILTER(?predicatefjhfuewhw in (" + in + ")) \n";
		}

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TargetObjectsOf that = (TargetObjectsOf) o;
		return targetObjectsOf.equals(that.targetObjectsOf);
	}

	@Override
	public int hashCode() {
		return Objects.hash(targetObjectsOf);
	}
}
