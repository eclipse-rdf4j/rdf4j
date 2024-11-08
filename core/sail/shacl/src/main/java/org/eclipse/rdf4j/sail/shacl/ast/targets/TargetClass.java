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

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BufferedSplitter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.FilterByPredicateObject;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnorderedSelect;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;

public class TargetClass extends Target {

	private final Set<Resource> targetClass;

	public TargetClass(Set<Resource> targetClass) {
		this.targetClass = targetClass;
		assert !this.targetClass.isEmpty();
	}

	@Override
	public IRI getPredicate() {
		return SHACL.TARGET_CLASS;
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			ConstraintComponent.Scope scope) {
		return getAddedRemovedInner(connectionsGroup.getAddedStatements(), dataGraph, scope, connectionsGroup);
	}

	private PlanNode getAddedRemovedInner(SailConnection connection, Resource[] dataGraph,
			ConstraintComponent.Scope scope, ConnectionsGroup connectionsGroup) {
		PlanNode planNode;
		if (targetClass.size() == 1) {
			Resource clazz = targetClass.stream().findAny().get();
			planNode = new UnorderedSelect(connection, null, RDF.TYPE, clazz,
					dataGraph, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(scope), null);
//			return planNode;
		} else {
			planNode = new Select(connection,
					SparqlFragment.bgp(Set.of(),
							getQueryFragment("?a", "?c", null, new StatementMatcher.StableRandomVariableProvider())),
					"?a", b -> new ValidationTuple(b.getValue("a"), scope, false, dataGraph), dataGraph);
		}

		return Unique.getInstance(planNode, false, connectionsGroup);
	}

	String getQueryFragment(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		Set<Resource> targets = targetClass;

		if (rdfsSubClassOfReasoner != null) {
			targets = targets.stream()
					.flatMap(target -> rdfsSubClassOfReasoner.backwardsChain(target).stream())
					.collect(Collectors.toSet());
		}

		assert !targets.isEmpty();

		return targets.stream()
				.map(r -> "<" + r + ">")
				.sorted()
				.map(r -> String.join("\n",
						"{",
						"BIND(rdf:type as " + stableRandomVariableProvider.next().asSparqlVariable() + ")",
						"BIND(" + r + " as " + objectVariable + ")",
						subjectVariable + " " + stableRandomVariableProvider.current().asSparqlVariable()
								+ objectVariable + ".",
						"}"
				)
				)
				.reduce((l, r) -> l + " UNION " + r)
				.get();

	}

	@Override
	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			PlanNode parent) {

		if (connectionsGroup.hasAddedStatements()) {
			BufferedSplitter bufferedSplitter = BufferedSplitter.getInstance(parent);

			FilterByPredicateObject typeFoundInAdded = new FilterByPredicateObject(
					connectionsGroup.getAddedStatements(), dataGraph, RDF.TYPE, targetClass,
					bufferedSplitter.getPlanNode(), true, FilterByPredicateObject.FilterOn.activeTarget, false,
					connectionsGroup);

			FilterByPredicateObject typeNotFoundInAdded = new FilterByPredicateObject(
					connectionsGroup.getAddedStatements(), dataGraph, RDF.TYPE, targetClass,
					bufferedSplitter.getPlanNode(), false, FilterByPredicateObject.FilterOn.activeTarget, false,
					connectionsGroup);

			FilterByPredicateObject filterAgainstBaseConnection = new FilterByPredicateObject(
					connectionsGroup.getBaseConnection(), dataGraph, RDF.TYPE, targetClass, typeNotFoundInAdded, true,
					FilterByPredicateObject.FilterOn.activeTarget, true, connectionsGroup);

			return new Sort(UnionNode.getInstance(connectionsGroup, typeFoundInAdded, filterAgainstBaseConnection),
					connectionsGroup);
		} else {
			return new FilterByPredicateObject(connectionsGroup.getBaseConnection(), dataGraph, RDF.TYPE,
					targetClass, parent, true, FilterByPredicateObject.FilterOn.activeTarget, true, connectionsGroup);
		}

	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		targetClass.forEach(t -> model.add(subject, getPredicate(), t));
	}

	@Override
	public SparqlFragment getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider, Set<String> inheritedVarNames) {
		assert (subject == null);

		Collection<Resource> targetClass;

		if (rdfsSubClassOfReasoner != null) {
			if (this.targetClass.size() == 1) {
				targetClass = rdfsSubClassOfReasoner.backwardsChain((Resource) this.targetClass.toArray()[0]);
			} else {
				targetClass = this.targetClass
						.stream()
						.map(rdfsSubClassOfReasoner::backwardsChain)
						.flatMap(Collection::stream)
						.collect(Collectors.toSet());
			}
		} else {
			targetClass = this.targetClass;
		}

		List<StatementMatcher> statementMatchers = targetClass.stream()
				.map(t -> new StatementMatcher(object, new StatementMatcher.Variable(RDF.TYPE),
						new StatementMatcher.Variable(t), this, Set.of()))
				.collect(Collectors.toList());

		if (targetClass.size() == 1) {

			String queryFragment = targetClass.stream()
					.findFirst()
					.map(r -> object.asSparqlVariable() + " a <" + r + "> .")
					.orElseThrow(IllegalStateException::new);

			return SparqlFragment.bgp(List.of(), queryFragment, statementMatchers);

		} else {

			String in = targetClass.stream()
					.map(r -> "<" + r + ">")
					.sorted()
					.reduce((a, b) -> a + " , " + b)
					.orElse("");

			String randomSparqlVariable = stableRandomVariableProvider.next().asSparqlVariable();

			String queryFragment = object.asSparqlVariable() + " a " + randomSparqlVariable + ".\n" +
					"FILTER(" + randomSparqlVariable + " in ( " + in + " ))";

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
		TargetClass that = (TargetClass) o;
		return targetClass.equals(that.targetClass);
	}

	@Override
	public int hashCode() {
		return Objects.hash(targetClass) + "TargetClass".hashCode();
	}

}
