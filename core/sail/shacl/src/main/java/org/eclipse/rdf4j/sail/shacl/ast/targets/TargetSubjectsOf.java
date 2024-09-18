/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.targets;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.FilterByPredicate;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnorderedSelect;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;

public class TargetSubjectsOf extends Target {

	private final Set<IRI> targetSubjectsOf;

	public TargetSubjectsOf(Set<IRI> targetSubjectsOf) {
		this.targetSubjectsOf = targetSubjectsOf;
		assert !this.targetSubjectsOf.isEmpty();

	}

	@Override
	public IRI getPredicate() {
		return SHACL.TARGET_SUBJECTS_OF;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		targetSubjectsOf.forEach(t -> {
			model.add(subject, getPredicate(), t);
		});
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			ConstraintComponent.Scope scope) {
		return getAddedRemovedInner(connectionsGroup.getAddedStatements(), dataGraph, scope, connectionsGroup);
	}

	private PlanNode getAddedRemovedInner(SailConnection connection, Resource[] dataGraph,
			ConstraintComponent.Scope scope, ConnectionsGroup connectionsGroup) {

		PlanNode planNode = targetSubjectsOf.stream()
				.map(predicate -> (PlanNode) new UnorderedSelect(connection, null,
						predicate, null, dataGraph, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(scope),
						null))
				.reduce((nodes, nodes2) -> UnionNode.getInstance(connectionsGroup, nodes, nodes2))
				.orElse(EmptyNode.getInstance());

		return Unique.getInstance(planNode, false, connectionsGroup);
	}

	@Override
	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			PlanNode parent) {
		return new FilterByPredicate(connectionsGroup.getBaseConnection(), targetSubjectsOf, parent,
				FilterByPredicate.On.Subject, dataGraph, connectionsGroup);
	}

	@Override
	public SparqlFragment getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider, Set<String> inheritedVarNames) {
		assert (subject == null);

		StatementMatcher.Variable tempVar = stableRandomVariableProvider.next();

		List<StatementMatcher> statementMatchers = targetSubjectsOf.stream()
				.map(t -> new StatementMatcher(
						object,
						new StatementMatcher.Variable(t),
						tempVar, this, Set.of())
				)
				.collect(Collectors.toList());

		if (targetSubjectsOf.size() == 1) {

			String queryFragment = targetSubjectsOf.stream()
					.map(t -> object.asSparqlVariable() + " <" + t + "> " + tempVar.asSparqlVariable() + " .")
					.reduce((a, b) -> a + "\n" + b)
					.orElse("");

			return SparqlFragment.bgp(List.of(), queryFragment, statementMatchers);

		} else {

			String in = targetSubjectsOf.stream()
					.map(t -> "<" + t + ">")
					.reduce((a, b) -> a + " , " + b)
					.orElse("");

			StatementMatcher.Variable tempVarForIn = stableRandomVariableProvider.next();

			String queryFragment = object.asSparqlVariable() + " " + tempVarForIn.asSparqlVariable()
					+ tempVar.asSparqlVariable() + " .\n" +
					"FILTER(" + tempVarForIn.asSparqlVariable() + " in (" + in + "))";

			return SparqlFragment.bgp(List.of(), queryFragment, statementMatchers);

		}

	}

	@Override
	public Set<Namespace> getNamespaces() {
		return Set.of();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TargetSubjectsOf that = (TargetSubjectsOf) o;
		return targetSubjectsOf.equals(that.targetSubjectsOf);
	}

	@Override
	public int hashCode() {
		return Objects.hash(targetSubjectsOf) + "TargetSubjectsOf".hashCode();
	}
}
