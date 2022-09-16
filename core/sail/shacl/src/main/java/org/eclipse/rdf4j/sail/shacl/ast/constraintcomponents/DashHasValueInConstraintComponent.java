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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclAstLists;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
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
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;

public class DashHasValueInConstraintComponent extends AbstractConstraintComponent {

	final Set<Value> hasValueIn;

	public DashHasValueInConstraintComponent(ShapeSource shapeSource, Resource hasValueIn) {
		super(hasValueIn);
		this.hasValueIn = Collections
				.unmodifiableSet(new LinkedHashSet<>(ShaclAstLists.toList(shapeSource, hasValueIn, Value.class)));
	}

	public DashHasValueInConstraintComponent(DashHasValueInConstraintComponent dashHasValueInConstraintComponent) {
		super(dashHasValueInConstraintComponent.getId());
		hasValueIn = dashHasValueInConstraintComponent.hasValueIn;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, DASH.hasValueIn, getId());

		if (!model.contains(getId(), null, null)) {
			ShaclAstLists.listToRdf(hasValueIn, getId(), model);
		}
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.HasValueConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new DashHasValueInConstraintComponent(this);
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings,
			PlanNodeProvider overrideTargetNode, Scope scope) {
		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget target = getTargetChain().getEffectiveTarget(scope,
				connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider);

		if (scope == Scope.propertyShape) {
			Path path = getTargetChain().getPath().get();

			PlanNode addedTargets;

			if (overrideTargetNode != null) {
				addedTargets = target.extend(overrideTargetNode.getPlanNode(), connectionsGroup,
						validationSettings.getDataGraph(), scope,
						EffectiveTarget.Extend.right,
						false, null);
			} else {
				addedTargets = target.getPlanNode(connectionsGroup, validationSettings.getDataGraph(), scope, true,
						null);
				PlanNode addedByPath = path.getAdded(connectionsGroup, validationSettings.getDataGraph(), null);

				addedByPath = target.getTargetFilter(connectionsGroup,
						validationSettings.getDataGraph(), Unique.getInstance(new TrimToTarget(addedByPath), false));
				addedByPath = target.extend(addedByPath, connectionsGroup, validationSettings.getDataGraph(), scope,
						EffectiveTarget.Extend.left, false,
						null);

				addedTargets = UnionNode.getInstance(addedByPath, addedTargets);
				addedTargets = Unique.getInstance(addedTargets, false);
			}

			PlanNode joined = new BulkedExternalLeftOuterJoin(
					addedTargets,
					connectionsGroup.getBaseConnection(),
					validationSettings.getDataGraph(),
					path.getTargetQueryFragment(new StatementMatcher.Variable("a"), new StatementMatcher.Variable("c"),
							connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider),
					(b) -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true,
							validationSettings.getDataGraph())
			);

			PlanNode invalidTargets = new GroupByFilter(joined, group -> {
				return group.stream().map(ValidationTuple::getValue).noneMatch(hasValueIn::contains);
			});

			return Unique.getInstance(new TrimToTarget(invalidTargets), false);

		} else if (scope == Scope.nodeShape) {

			PlanNode addedTargets;

			if (overrideTargetNode != null) {
				addedTargets = target.extend(overrideTargetNode.getPlanNode(), connectionsGroup,
						validationSettings.getDataGraph(), scope,
						EffectiveTarget.Extend.right,
						false, null);
			} else {
				addedTargets = target.getPlanNode(connectionsGroup, validationSettings.getDataGraph(), scope, false,
						null);
			}

			PlanNode falseNode = new ValueInFilter(addedTargets, hasValueIn)
					.getFalseNode(UnBufferedPlanNode.class);

			return falseNode;

		} else {
			throw new UnsupportedOperationException("Unknown scope: " + scope);
		}

	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Resource[] dataGraph, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		if (scope == Scope.propertyShape) {
			PlanNode allTargetsPlan = getTargetChain()
					.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.getPlanNode(connectionsGroup, dataGraph, Scope.nodeShape, true, null);

			return Unique.getInstance(new ShiftToPropertyShape(allTargetsPlan), true);
		}
		return EmptyNode.getInstance();
	}

	@Override
	public SparqlFragment buildSparqlValidNodes_rsx_targetShape(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {

		List<StatementMatcher> statementMatchers = Collections.emptyList();

		if (getTargetChain().getPath().isPresent()) {
			Path path = getTargetChain().getPath().get();

			statementMatchers = hasValueIn.stream()
					.flatMap(v -> path.getStatementMatcher(subject, new StatementMatcher.Variable(v),
							rdfsSubClassOfReasoner))
					.collect(Collectors.toList());
		}

		if (scope == Scope.propertyShape) {
			Path path = getTargetChain().getPath().get();

			String sparql = hasValueIn
					.stream()
					.map(value -> {

						if (value.isIRI()) {
							return "BIND(<" + value + "> as ?" + object.getName() + ")\n"
									+ path.getTargetQueryFragment(subject, object, rdfsSubClassOfReasoner,
											stableRandomVariableProvider);
						}
						if (value.isLiteral()) {
							return "BIND(" + value + " as ?" + object.getName() + ")\n"
									+ path.getTargetQueryFragment(subject, object, rdfsSubClassOfReasoner,
											stableRandomVariableProvider);
						}

						throw new UnsupportedOperationException(
								"value was unsupported type: " + value.getClass().getSimpleName());
					})
					.collect(
							Collectors.joining("} UNION {\n" + VALUES_INJECTION_POINT + "\n",
									"{\n" + VALUES_INJECTION_POINT + "\n",
									"}"));
			return SparqlFragment.bgp(sparql, statementMatchers);

		} else {

			String sparql = hasValueIn
					.stream()
					.map(value -> {
						if (value.isIRI()) {
							return "?" + object.getName() + " = <" + value + ">";
						} else if (value.isLiteral()) {
							return "?" + object.getName() + " = " + value;
						}
						throw new UnsupportedOperationException(
								"value was unsupported type: " + value.getClass().getSimpleName());
					})
					.reduce((a, b) -> a + " || " + b)
					.orElseThrow(() -> new IllegalStateException("hasValueIn was empty"));
			return SparqlFragment.filterCondition(sparql, statementMatchers);

		}
	}

}
