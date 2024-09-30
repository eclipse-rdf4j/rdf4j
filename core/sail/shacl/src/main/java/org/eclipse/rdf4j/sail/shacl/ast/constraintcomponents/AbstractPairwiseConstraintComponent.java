/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.CanProduceValidationReport;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.TrimToTarget;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnorderedSelect;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetChain;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;

abstract class AbstractPairwiseConstraintComponent extends AbstractConstraintComponent
		implements CanProduceValidationReport {

	final Shape shape;
	final IRI predicate;
	boolean producesValidationReport;

	public AbstractPairwiseConstraintComponent(IRI predicate, Shape shape) {
		this.predicate = predicate;
		this.shape = shape;
	}

	/**
	 * The abstract method that is implemented by the subclasses to return the IRI of the constraint component, e.g.
	 * sh:equals, sh:disjoint etc.
	 *
	 * @return The IRI of the constraint component
	 */
	abstract IRI getConstraintIri();

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, getConstraintIri(), this.predicate);
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
			if (scope == Scope.propertyShape) {
				allTargets = getAllTargetsIncludingThoseAddedByPath(connectionsGroup, validationSettings, scope,
						effectiveTarget, path.get(), true);

				PlanNode allTargetsBasedOnPredicate = getAllTargetsBasedOnPredicate(connectionsGroup,
						validationSettings,
						effectiveTarget);

				allTargets = Unique.getInstance(
						UnionNode.getInstance(connectionsGroup, allTargets, allTargetsBasedOnPredicate), false,
						connectionsGroup);

			} else {
				allTargets = effectiveTarget.getPlanNode(connectionsGroup, validationSettings.getDataGraph(), scope,
						false, null);

				PlanNode allTargetsBasedOnPredicate = getAllTargetsBasedOnPredicate(connectionsGroup,
						validationSettings,
						effectiveTarget);

				allTargets = Unique.getInstance(
						UnionNode.getInstance(connectionsGroup, allTargets, allTargetsBasedOnPredicate), false,
						connectionsGroup);
			}

		}

		StatementMatcher.Variable<Resource> subject = new StatementMatcher.Variable<>("a");
		StatementMatcher.Variable<Value> object = new StatementMatcher.Variable<>("c");

		SparqlFragment targetQueryFragment = null;

		if (path.isPresent()) {
			targetQueryFragment = path.get()
					.getTargetQueryFragment(subject, object, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider, Set.of());
		}

		return getPairwiseCheck(connectionsGroup, validationSettings, allTargets, subject, object, targetQueryFragment);
	}

	/**
	 * The abstract method that is implemented by the subclasses to add the pairwise check as a PlanNode to the
	 * validation plan.
	 *
	 * @param connectionsGroup
	 * @param validationSettings
	 * @param allTargets
	 * @param subject
	 * @param object
	 * @param targetQueryFragment
	 * @return The PlanNode that performs the pairwise check
	 */
	abstract PlanNode getPairwiseCheck(ConnectionsGroup connectionsGroup, ValidationSettings validationSettings,
			PlanNode allTargets, StatementMatcher.Variable<Resource> subject, StatementMatcher.Variable<Value> object,
			SparqlFragment targetQueryFragment);

	private PlanNode getAllTargetsBasedOnPredicate(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, EffectiveTarget effectiveTarget) {
		PlanNode addedByPredicate = new UnorderedSelect(connectionsGroup.getAddedStatements(), null, predicate, null,
				validationSettings.getDataGraph(), (s, d) -> {
					return new ValidationTuple(s.getSubject(), Scope.propertyShape, false, d);
				}, null);

		PlanNode removedByPredicate = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, predicate,
				null, validationSettings.getDataGraph(), (s, d) -> {
					return new ValidationTuple(s.getSubject(), Scope.propertyShape, false, d);
				}, null);

		PlanNode targetFilter1 = effectiveTarget.getTargetFilter(connectionsGroup, validationSettings.getDataGraph(),
				addedByPredicate);
		PlanNode targetFilter2 = effectiveTarget.getTargetFilter(connectionsGroup, validationSettings.getDataGraph(),
				removedByPredicate);

		return Unique.getInstance(UnionNode.getInstance(connectionsGroup, targetFilter1, targetFilter2), false,
				connectionsGroup);
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Resource[] dataGraph, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider,
			ValidationSettings validationSettings) {
		if (scope == Scope.propertyShape) {
			PlanNode allTargetsPlan = getTargetChain()
					.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.getPlanNode(connectionsGroup, dataGraph, Scope.nodeShape, true, null);

			allTargetsPlan = new ShiftToPropertyShape(allTargetsPlan, connectionsGroup);

			// removed statements that match predicate could affect sh:or
			if (connectionsGroup.getStats().hasRemoved()) {
				PlanNode deletedPredicates = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null,
						predicate, null, dataGraph,
						UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.propertyShape), null);
				deletedPredicates = getTargetChain()
						.getEffectiveTarget(Scope.propertyShape, connectionsGroup.getRdfsSubClassOfReasoner(),
								stableRandomVariableProvider)
						.getTargetFilter(connectionsGroup, dataGraph, deletedPredicates);
				deletedPredicates = getTargetChain()
						.getEffectiveTarget(Scope.propertyShape, connectionsGroup.getRdfsSubClassOfReasoner(),
								stableRandomVariableProvider)
						.extend(deletedPredicates, connectionsGroup, dataGraph, Scope.propertyShape,
								EffectiveTarget.Extend.left,
								false,
								null);
				allTargetsPlan = UnionNode.getInstanceDedupe(connectionsGroup, allTargetsPlan, deletedPredicates);
			}

			// added statements that match predicate could affect sh:not
			if (connectionsGroup.getStats().hasAdded()) {
				PlanNode addedPredicates = new UnorderedSelect(connectionsGroup.getAddedStatements(), null, predicate,
						null, dataGraph, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.propertyShape),
						null);
				addedPredicates = getTargetChain()
						.getEffectiveTarget(Scope.propertyShape, connectionsGroup.getRdfsSubClassOfReasoner(),
								stableRandomVariableProvider)
						.getTargetFilter(connectionsGroup, dataGraph, addedPredicates);
				addedPredicates = getTargetChain()
						.getEffectiveTarget(Scope.propertyShape, connectionsGroup.getRdfsSubClassOfReasoner(),
								stableRandomVariableProvider)
						.extend(addedPredicates, connectionsGroup, dataGraph, Scope.propertyShape,
								EffectiveTarget.Extend.left,
								false,
								null);
				allTargetsPlan = UnionNode.getInstanceDedupe(connectionsGroup, allTargetsPlan, addedPredicates);
			}

			return Unique.getInstance(new TrimToTarget(allTargetsPlan, connectionsGroup), false, connectionsGroup);
		} else {
			assert scope == Scope.nodeShape;

			PlanNode allTargetsPlan = EmptyNode.getInstance();

			// removed type statements that match clazz could affect sh:or
			if (connectionsGroup.getStats().hasRemoved()) {
				PlanNode deletedPredicates = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null,
						predicate, null,
						dataGraph, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape), null);
				deletedPredicates = getTargetChain()
						.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
								stableRandomVariableProvider)
						.getTargetFilter(connectionsGroup, dataGraph, deletedPredicates);
				deletedPredicates = getTargetChain()
						.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
								stableRandomVariableProvider)
						.extend(deletedPredicates, connectionsGroup, dataGraph, Scope.nodeShape,
								EffectiveTarget.Extend.left,
								false, null);
				allTargetsPlan = UnionNode.getInstanceDedupe(connectionsGroup, allTargetsPlan, deletedPredicates);

			}

			// added type statements that match clazz could affect sh:not
			if (connectionsGroup.getStats().hasAdded()) {
				PlanNode addedPredicates = new UnorderedSelect(connectionsGroup.getAddedStatements(), null, predicate,
						null,
						dataGraph, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape), null);
				addedPredicates = getTargetChain()
						.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
								stableRandomVariableProvider)
						.getTargetFilter(connectionsGroup, dataGraph, addedPredicates);
				addedPredicates = getTargetChain()
						.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
								stableRandomVariableProvider)
						.extend(addedPredicates, connectionsGroup, dataGraph, Scope.nodeShape,
								EffectiveTarget.Extend.left,
								false, null);
				allTargetsPlan = UnionNode.getInstanceDedupe(connectionsGroup, allTargetsPlan, addedPredicates);

			}

			return Unique.getInstance(new TrimToTarget(allTargetsPlan, connectionsGroup), false, connectionsGroup);

		}

	}

	@Override
	public boolean requiresEvaluation(ConnectionsGroup connectionsGroup, Scope scope, Resource[] dataGraph,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		return super.requiresEvaluation(connectionsGroup, scope, dataGraph, stableRandomVariableProvider)
				|| connectionsGroup.getRemovedStatements().hasStatement(null, predicate, null, true, dataGraph)
				|| connectionsGroup.getAddedStatements().hasStatement(null, predicate, null, true, dataGraph);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		AbstractPairwiseConstraintComponent that = (AbstractPairwiseConstraintComponent) o;

		return predicate.equals(that.predicate);
	}

	@Override
	public int hashCode() {
		return predicate.hashCode() + "AbstractPairwiseConstraintComponent".hashCode();
	}

	@Override
	public void setProducesValidationReport(boolean producesValidationReport) {
		this.producesValidationReport = producesValidationReport;
	}

	@Override
	public boolean producesValidationReport() {
		return producesValidationReport;
	}
}
