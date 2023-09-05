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

package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.CheckEqualsValuesBasedOnPathAndPredicate;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnorderedSelect;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetChain;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;

public class EqualsConstraintComponent extends AbstractConstraintComponent {

	private final IRI predicate;

	public EqualsConstraintComponent(IRI predicate) {
		this.predicate = predicate;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.EQUALS, this.predicate);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.EqualsConstraintComponent;
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, PlanNodeProvider overrideTargetNode, Scope scope) {
		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		TargetChain targetChain = getTargetChain();

		EffectiveTarget effectiveTarget = targetChain.getEffectiveTarget(scope,
				connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider);
		Optional<Path> path = targetChain.getPath();

		PlanNode allTargets;

		if (overrideTargetNode != null) {
			allTargets = effectiveTarget.extend(overrideTargetNode.getPlanNode(), connectionsGroup,
					validationSettings.getDataGraph(), scope, EffectiveTarget.Extend.right, false, null);
		} else {

			PlanNode targets = effectiveTarget.getPlanNode(connectionsGroup, validationSettings.getDataGraph(),
					scope, true, null);

			PlanNode allTargetsBasedOnPredicate = getAllTargetsBasedOnPredicate(connectionsGroup, validationSettings,
					effectiveTarget);

			allTargets = Unique.getInstance(UnionNode.getInstance(allTargetsBasedOnPredicate, targets), false);
		}

		StatementMatcher.Variable<Resource> subject = new StatementMatcher.Variable<>("a");
		StatementMatcher.Variable<Value> object = new StatementMatcher.Variable<>("c");

		SparqlFragment targetQueryFragment = null;
		if (path.isPresent()) {
			targetQueryFragment = path.get()
					.getTargetQueryFragment(subject, object, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider, Set.of());
		}

		return new CheckEqualsValuesBasedOnPathAndPredicate(connectionsGroup.getBaseConnection(),
				validationSettings.getDataGraph(), allTargets, predicate, subject, object, targetQueryFragment);
	}

	private PlanNode getAllTargetsBasedOnPredicate(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, EffectiveTarget effectiveTarget) {
		PlanNode addedByPredicate = new UnorderedSelect(connectionsGroup.getAddedStatements(), null, predicate, null,
				validationSettings.getDataGraph(), (s, d) -> {
					return new ValidationTuple(s.getSubject(), Scope.propertyShape, false, d);
				});

		PlanNode removedByPredicate = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, predicate,
				null, validationSettings.getDataGraph(), (s, d) -> {
					return new ValidationTuple(s.getSubject(), Scope.propertyShape, false, d);
				});

		PlanNode targetFilter1 = effectiveTarget.getTargetFilter(connectionsGroup, validationSettings.getDataGraph(),
				addedByPredicate);
		PlanNode targetFilter2 = effectiveTarget.getTargetFilter(connectionsGroup, validationSettings.getDataGraph(),
				removedByPredicate);

		PlanNode allTargetsBasedOnPredicate = Unique.getInstance(UnionNode.getInstance(targetFilter1, targetFilter2),
				false);
		return allTargetsBasedOnPredicate;
	}

	@Override
	public ValidationApproach getPreferredValidationApproach(ConnectionsGroup connectionsGroup) {
		return ValidationApproach.Transactional;
	}

	@Override
	public ValidationApproach getOptimalBulkValidationApproach() {
		return ValidationApproach.Transactional;
	}

	@Override
	public boolean requiresEvaluation(ConnectionsGroup connectionsGroup, Scope scope, Resource[] dataGraph,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {

		// todo both consider the target chain with added and removed values (path), and also added and removed values
		// for the predicate path

		return true;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new EqualsConstraintComponent(predicate);
	}

	@Override
	public List<Literal> getDefaultMessage() {
		return List.of();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		EqualsConstraintComponent that = (EqualsConstraintComponent) o;

		return predicate.equals(that.predicate);
	}

	@Override
	public int hashCode() {
		return predicate.hashCode() + "EqualsConstraintComponent".hashCode();
	}
}
