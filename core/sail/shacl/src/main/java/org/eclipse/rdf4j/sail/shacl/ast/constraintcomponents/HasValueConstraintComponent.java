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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher.Variable;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationQuery;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.AbstractBulkJoinPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalLeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.GroupByFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.TrimToTarget;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValueInFilter;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;

public class HasValueConstraintComponent extends AbstractConstraintComponent {

	private final Value hasValue;

	public HasValueConstraintComponent(Value hasValue) {
		this.hasValue = hasValue;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.HAS_VALUE, hasValue);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.HasValueConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new HasValueConstraintComponent(hasValue);
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, PlanNodeProvider overrideTargetNode, Scope scope) {

		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget target = getTargetChain().getEffectiveTarget(scope,
				connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider);

		if (scope == Scope.propertyShape) {
			Path path = getTargetChain().getPath().get();

			PlanNode addedTargets;

			if (overrideTargetNode != null) {
				addedTargets = target.extend(overrideTargetNode.getPlanNode(), connectionsGroup,
						validationSettings.getDataGraph(), scope, EffectiveTarget.Extend.right, false, null);

			} else {
				addedTargets = target.getPlanNode(connectionsGroup, validationSettings.getDataGraph(), scope, true,
						null);
				PlanNode addedByPath = path.getAllAdded(connectionsGroup, validationSettings.getDataGraph(), null);

				addedByPath = target.getTargetFilter(connectionsGroup, validationSettings.getDataGraph(),
						Unique.getInstance(new TrimToTarget(addedByPath, connectionsGroup), false, connectionsGroup));
				addedByPath = target.extend(addedByPath, connectionsGroup, validationSettings.getDataGraph(), scope,
						EffectiveTarget.Extend.left, false, null);

				addedTargets = UnionNode.getInstance(connectionsGroup, addedByPath, addedTargets);
			}

			addedTargets = Unique.getInstance(new TrimToTarget(addedTargets, connectionsGroup), false,
					connectionsGroup);

			PlanNode joined = new BulkedExternalLeftOuterJoin(addedTargets, connectionsGroup.getBaseConnection(),
					validationSettings.getDataGraph(),
					path.getTargetQueryFragment(new Variable<>("a"), new Variable<>("c"),
							connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider, Set.of()),
					(b) -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true,
							validationSettings.getDataGraph()),
					connectionsGroup, AbstractBulkJoinPlanNode.DEFAULT_VARS);

			PlanNode invalidTargets = new GroupByFilter(joined, group -> {
				return group
						.stream()
						.map(ValidationTuple::getValue)
						.noneMatch(v -> hasValue.equals(v));
			}, connectionsGroup);

			return Unique.getInstance(new TrimToTarget(invalidTargets, connectionsGroup), false, connectionsGroup);

		} else if (scope == Scope.nodeShape) {

			PlanNode addedTargets;

			if (overrideTargetNode != null) {
				addedTargets = target.extend(overrideTargetNode.getPlanNode(), connectionsGroup,
						validationSettings.getDataGraph(), scope, EffectiveTarget.Extend.right, false, null);
			} else {
				addedTargets = target.getPlanNode(connectionsGroup, validationSettings.getDataGraph(), scope, false,
						null);
			}

			PlanNode falseNode = new ValueInFilter(addedTargets, new HashSet<>(Collections.singletonList(hasValue)),
					connectionsGroup)
					.getFalseNode(UnBufferedPlanNode.class);

			return falseNode;

		} else {
			throw new UnsupportedOperationException("Unknown scope: " + scope);
		}

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

			return Unique.getInstance(new ShiftToPropertyShape(allTargetsPlan, connectionsGroup), true,
					connectionsGroup);
		}
		return EmptyNode.getInstance();
	}

	@Override
	public SparqlFragment buildSparqlValidNodes_rsx_targetShape(Variable<Value> subject,
			Variable<Value> object, RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {

		if (scope == Scope.propertyShape) {

			Path path = getTargetChain().getPath().get();

			SparqlFragment targetQueryFragment = path.getTargetQueryFragment(subject, object, rdfsSubClassOfReasoner,
					stableRandomVariableProvider, Set.of());

			return SparqlFragment.bgp(List.of(),
					"BIND(" + stringRepresentationOfValue(hasValue) + " as " + object.asSparqlVariable() + ")\n"
							+ targetQueryFragment.getFragment(),
					StatementMatcher.swap(targetQueryFragment.getStatementMatchers(), object,
							new Variable<>(hasValue)),
					null);

		} else {
			return SparqlFragment.filterCondition(List.of(),
					object.asSparqlVariable() + " = " + stringRepresentationOfValue(hasValue),
					List.of());

		}
	}

	@Override
	public ValidationQuery generateSparqlValidationQuery(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, boolean negatePlan, boolean negateChildren, Scope scope) {

		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget effectiveTarget = getTargetChain().getEffectiveTarget(scope,
				connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider);
		String query = effectiveTarget.getQuery(false);

		if (scope == Scope.nodeShape) {

			query += "\n" + "FILTER(" + effectiveTarget.getTargetVar().asSparqlVariable() + " != "
					+ stringRepresentationOfValue(hasValue) + ")";

		} else {
			var value = StatementMatcher.Variable.VALUE;

			String pathQuery = getTargetChain().getPath()
					.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(), value,
							connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider, Set.of()))
					.orElseThrow(IllegalStateException::new)
					.getFragment();

			query += "\n" + "FILTER( " + "NOT EXISTS{" + "	BIND(" + stringRepresentationOfValue(hasValue) + " as "
					+ value.asSparqlVariable() + ")\n" + pathQuery + "\n" + "})";

		}

		var allTargetVariables = effectiveTarget.getAllTargetVariables();

		return new ValidationQuery(getTargetChain().getNamespaces(), query, allTargetVariables, null, scope, this, null,
				null);

	}

	@Override
	public ValidationApproach getOptimalBulkValidationApproach() {
		return ValidationApproach.SPARQL;
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

		HasValueConstraintComponent that = (HasValueConstraintComponent) o;

		return hasValue.equals(that.hasValue);
	}

	@Override
	public int hashCode() {
		return hasValue.hashCode() + "HasValueConstraintComponent".hashCode();
	}
}
