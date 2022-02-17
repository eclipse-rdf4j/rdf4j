/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.targets;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ExternalPredicateObjectFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnorderedSelect;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;

public class TargetClass extends Target {

	private final Set<Resource> targetClass;

	public TargetClass(Set<Resource> targetClass) {
		if (targetClass.size() == 1) {
			this.targetClass = Collections.singleton(((Resource) targetClass.toArray()[0]));
		} else {
			this.targetClass = targetClass;
		}
		assert !this.targetClass.isEmpty();
	}

	@Override
	public IRI getPredicate() {
		return SHACL.TARGET_CLASS;
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, ConstraintComponent.Scope scope) {
		return getAddedRemovedInner(connectionsGroup.getAddedStatements(),
				connectionsGroup.getAddedStatementsValueFactory(), scope);
	}

	private PlanNode getAddedRemovedInner(SailConnection connection, ValueFactory valueFactory,
			ConstraintComponent.Scope scope) {
		PlanNode planNode;
		if (targetClass.size() == 1) {
			Resource clazz = targetClass.stream().findAny().get();
			planNode = new UnorderedSelect(connection, null, RDF.TYPE, clazz,
					UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(scope));
		} else {
			planNode = new Select(connection,
					valueFactory,
					getQueryFragment("?a", "?c", null, new StatementMatcher.StableRandomVariableProvider()),
					"?a", b -> new ValidationTuple(b.getValue("a"), scope, false));
		}

		return Unique.getInstance(planNode, false);
	}

	@Override
	public String getQueryFragment(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		Set<Resource> targets = targetClass;

		if (rdfsSubClassOfReasoner != null) {
			targets = targets.stream()
					.flatMap(target -> rdfsSubClassOfReasoner.backwardsChain(target).stream())
					.collect(Collectors.toSet());
		}

		assert targets.size() >= 1;

		return targets.stream()
				.map(r -> "<" + r + ">")
				.sorted()
				.map(r -> String.join("\n", "",
						"{",
						"\tBIND(rdf:type as " + stableRandomVariableProvider.next().asSparqlVariable() + ")",
						"\tBIND(" + r + " as " + objectVariable + ")",
						"\t" + subjectVariable + " " + stableRandomVariableProvider.current().asSparqlVariable()
								+ objectVariable + ".",
						"}"
				)
				)
				.reduce((l, r) -> l + " UNION " + r)
				.get();

	}

	@Override
	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, PlanNode parent) {
		return new ExternalPredicateObjectFilter(connectionsGroup.getBaseConnection(), RDF.TYPE, targetClass, parent,
				true, ExternalPredicateObjectFilter.FilterOn.activeTarget);

	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		targetClass.forEach(t -> model.add(subject, getPredicate(), t));
	}

	@Override
	public Stream<StatementMatcher> getStatementMatcher(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {
		assert (subject == null);

		Stream<Resource> stream = targetClass.stream();

		if (rdfsSubClassOfReasoner != null) {
			stream = stream
					.map(rdfsSubClassOfReasoner::backwardsChain)
					.flatMap(Collection::stream)
					.distinct();
		}

		return stream
				.map(t -> new StatementMatcher(object, new StatementMatcher.Variable(RDF.TYPE),
						new StatementMatcher.Variable(t)));

	}

	@Override
	public String getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		assert (subject == null);

		Collection<Resource> targetClass;

		if (rdfsSubClassOfReasoner != null) {
			targetClass = this.targetClass
					.stream()
					.map(rdfsSubClassOfReasoner::backwardsChain)
					.flatMap(Collection::stream)
					.distinct()
					.collect(Collectors.toList());
		} else {
			targetClass = this.targetClass;
		}

		if (targetClass.size() == 1) {

			return targetClass.stream()
					.findFirst()
					.map(r -> object.asSparqlVariable() + " a <" + r + "> .")
					.orElseThrow(IllegalStateException::new);

		} else {

			String in = targetClass.stream()
					.map(r -> "<" + r + ">")
					.sorted()
					.reduce((a, b) -> a + " , " + b)
					.orElse("");

			String randomSparqlVariable = stableRandomVariableProvider.next().asSparqlVariable();

			return object.asSparqlVariable() + " a " + randomSparqlVariable + ".\n" +
					"FILTER(" + randomSparqlVariable + " in ( " + in + " )) \n";
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
		TargetClass that = (TargetClass) o;
		return targetClass.equals(that.targetClass);
	}

	@Override
	public int hashCode() {
		return Objects.hash(targetClass);
	}
}
