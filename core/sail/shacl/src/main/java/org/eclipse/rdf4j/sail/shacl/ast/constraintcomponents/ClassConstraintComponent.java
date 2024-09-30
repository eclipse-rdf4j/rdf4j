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
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.AllTargetsPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BufferedSplitter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.FilterByPredicateObject;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ReduceTargets;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.TrimToTarget;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnorderedSelect;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;

public class ClassConstraintComponent extends AbstractConstraintComponent {

	private final IRI clazz;
	private final Set<Resource> clazzSet;

	public ClassConstraintComponent(IRI clazz) {
		this.clazz = clazz;
		this.clazzSet = Set.of(clazz);
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.CLASS, clazz);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.ClassConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new ClassConstraintComponent(clazz);
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings,
			PlanNodeProvider overrideTargetNode, Scope scope) {
		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget effectiveTarget = getTargetChain().getEffectiveTarget(scope,
				connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider);

		if (scope == Scope.propertyShape) {
			Path path = getTargetChain().getPath().get();

			PlanNode addedTargets;

			if (overrideTargetNode != null) {
				PlanNode planNode = overrideTargetNode.getPlanNode();
				if (planNode instanceof AllTargetsPlanNode) {
					// We are cheating a bit here by retrieving all the targets and values at the same time by
					// pretending to be in node shape scope and then shifting the results back to property shape scope
					PlanNode allTargets = getTargetChain()
							.getEffectiveTarget(Scope.nodeShape,
									connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider)
							.getAllTargets(connectionsGroup, validationSettings.getDataGraph(), Scope.nodeShape);
					allTargets = new ShiftToPropertyShape(allTargets, connectionsGroup);

					// filter by type against the base sail
					allTargets = new FilterByPredicateObject(
							connectionsGroup.getBaseConnection(),
							validationSettings.getDataGraph(), RDF.TYPE, clazzSet,
							allTargets, false, FilterByPredicateObject.FilterOn.value, true, connectionsGroup);

					return allTargets;

				} else {
					addedTargets = effectiveTarget.extend(planNode, connectionsGroup,
							validationSettings.getDataGraph(), scope,
							EffectiveTarget.Extend.right,
							false, null);
				}

			} else {
				BufferedSplitter addedTargetsBufferedSplitter = BufferedSplitter.getInstance(
						effectiveTarget.getPlanNode(connectionsGroup, validationSettings.getDataGraph(), scope, false,
								null));
				addedTargets = addedTargetsBufferedSplitter.getPlanNode();
				PlanNode addedByPath = path.getAllAdded(connectionsGroup, validationSettings.getDataGraph(), null);

				addedByPath = effectiveTarget.getTargetFilter(connectionsGroup,
						validationSettings.getDataGraph(),
						Unique.getInstance(new TrimToTarget(addedByPath, connectionsGroup), true, connectionsGroup));

				addedByPath = new ReduceTargets(addedByPath, addedTargetsBufferedSplitter.getPlanNode(),
						connectionsGroup);

				addedByPath = effectiveTarget.extend(addedByPath, connectionsGroup, validationSettings.getDataGraph(),
						scope,
						EffectiveTarget.Extend.left, false,
						null);

				if (connectionsGroup.getStats().hasRemoved()) {
					PlanNode deletedTypes = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, RDF.TYPE,
							clazz, validationSettings.getDataGraph(),
							UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape), null);

					deletedTypes = getTargetChain()
							.getEffectiveTarget(Scope.nodeShape,
									connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider)
							.extend(deletedTypes, connectionsGroup, validationSettings.getDataGraph(), Scope.nodeShape,
									EffectiveTarget.Extend.left,
									false, null);

					deletedTypes = getTargetChain()
							.getEffectiveTarget(Scope.nodeShape,
									connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider)
							.getTargetFilter(connectionsGroup, validationSettings.getDataGraph(), deletedTypes);

					addedTargets = UnionNode.getInstance(connectionsGroup, addedTargets,
							new TrimToTarget(new ShiftToPropertyShape(deletedTypes, connectionsGroup),
									connectionsGroup));
				}

				addedTargets = UnionNode.getInstance(connectionsGroup, addedByPath, addedTargets);
				addedTargets = Unique.getInstance(addedTargets, false, connectionsGroup);
			}

			int size = effectiveTarget.size();

			if (size > 1) {
				addedTargets = Unique.getInstance(addedTargets, true, connectionsGroup);
			}

			PlanNode falseNode = new BulkedExternalInnerJoin(
					addedTargets,
					connectionsGroup.getBaseConnection(),
					validationSettings.getDataGraph(),
					path.getTargetQueryFragment(new StatementMatcher.Variable("a"), new StatementMatcher.Variable("c"),
							connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider, Set.of()),
					false,
					null,
					BulkedExternalInnerJoin.getMapper("a", "c", scope, validationSettings.getDataGraph()),
					connectionsGroup, AbstractBulkJoinPlanNode.DEFAULT_VARS);

			if (connectionsGroup.getAddedStatements() != null) {
				// filter by type against the added statements
				falseNode = new FilterByPredicateObject(
						connectionsGroup.getAddedStatements(),
						validationSettings.getDataGraph(), RDF.TYPE, clazzSet,
						falseNode, false, FilterByPredicateObject.FilterOn.value, false, connectionsGroup);
			}

			// filter by type against the base sail
			falseNode = new FilterByPredicateObject(
					connectionsGroup.getBaseConnection(),
					validationSettings.getDataGraph(), RDF.TYPE, clazzSet,
					falseNode, false, FilterByPredicateObject.FilterOn.value, true, connectionsGroup);

			return falseNode;

		} else if (scope == Scope.nodeShape) {

			PlanNode addedTargets;

			if (overrideTargetNode != null) {
				addedTargets = effectiveTarget.extend(overrideTargetNode.getPlanNode(), connectionsGroup,
						validationSettings.getDataGraph(), scope,
						EffectiveTarget.Extend.right,
						false, null);
			} else {
				addedTargets = effectiveTarget.getPlanNode(connectionsGroup, validationSettings.getDataGraph(), scope,
						false,
						null);

				if (connectionsGroup.getStats().hasRemoved()) {
					PlanNode deletedTypes = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, RDF.TYPE,
							clazz, validationSettings.getDataGraph(),
							UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(scope), null);
					deletedTypes = getTargetChain()
							.getEffectiveTarget(scope, connectionsGroup.getRdfsSubClassOfReasoner(),
									stableRandomVariableProvider)
							.getTargetFilter(connectionsGroup, validationSettings.getDataGraph(), deletedTypes);
					deletedTypes = getTargetChain()
							.getEffectiveTarget(scope, connectionsGroup.getRdfsSubClassOfReasoner(),
									stableRandomVariableProvider)
							.extend(deletedTypes, connectionsGroup, validationSettings.getDataGraph(), scope,
									EffectiveTarget.Extend.left, false, null);
					addedTargets = UnionNode.getInstance(connectionsGroup, addedTargets, deletedTypes);
				}
			}

			// filter by type against the base sail
			PlanNode falseNode = new FilterByPredicateObject(
					connectionsGroup.getBaseConnection(),
					validationSettings.getDataGraph(), RDF.TYPE, clazzSet,
					addedTargets, false, FilterByPredicateObject.FilterOn.value, true, connectionsGroup);

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

			// removed type statements that match clazz could affect sh:or
			if (connectionsGroup.getStats().hasRemoved()) {
				PlanNode deletedTypes = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, RDF.TYPE,
						clazz, dataGraph, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape),
						null);
				deletedTypes = getTargetChain()
						.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
								stableRandomVariableProvider)
						.getTargetFilter(connectionsGroup, dataGraph, deletedTypes);
				deletedTypes = getTargetChain()
						.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
								stableRandomVariableProvider)
						.extend(deletedTypes, connectionsGroup, dataGraph, Scope.nodeShape, EffectiveTarget.Extend.left,
								false,
								null);
				allTargetsPlan = UnionNode.getInstanceDedupe(connectionsGroup, allTargetsPlan, deletedTypes);
			}

			// added type statements that match clazz could affect sh:not
			if (connectionsGroup.getStats().hasAdded()) {
				PlanNode addedTypes = new UnorderedSelect(connectionsGroup.getAddedStatements(), null, RDF.TYPE,
						clazz, dataGraph, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape),
						null);
				addedTypes = getTargetChain()
						.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
								stableRandomVariableProvider)
						.getTargetFilter(connectionsGroup, dataGraph, addedTypes);
				addedTypes = getTargetChain()
						.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
								stableRandomVariableProvider)
						.extend(addedTypes, connectionsGroup, dataGraph, Scope.nodeShape, EffectiveTarget.Extend.left,
								false,
								null);
				allTargetsPlan = UnionNode.getInstanceDedupe(connectionsGroup, allTargetsPlan, addedTypes);
			}

			return Unique.getInstance(
					new TrimToTarget(new ShiftToPropertyShape(allTargetsPlan, connectionsGroup), connectionsGroup),
					false, connectionsGroup);
		}
		PlanNode allTargetsPlan = EmptyNode.getInstance();

		// removed type statements that match clazz could affect sh:or
		if (connectionsGroup.getStats().hasRemoved()) {
			PlanNode deletedTypes = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, RDF.TYPE, clazz,
					dataGraph, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape), null);
			deletedTypes = getTargetChain()
					.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.getTargetFilter(connectionsGroup, dataGraph, deletedTypes);
			deletedTypes = getTargetChain()
					.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.extend(deletedTypes, connectionsGroup, dataGraph, Scope.nodeShape, EffectiveTarget.Extend.left,
							false, null);
			allTargetsPlan = UnionNode.getInstanceDedupe(connectionsGroup, allTargetsPlan, deletedTypes);

		}

		// added type statements that match clazz could affect sh:not
		if (connectionsGroup.getStats().hasAdded()) {
			PlanNode addedTypes = new UnorderedSelect(connectionsGroup.getAddedStatements(), null, RDF.TYPE, clazz,
					dataGraph, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape), null);
			addedTypes = getTargetChain()
					.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.getTargetFilter(connectionsGroup, dataGraph, addedTypes);
			addedTypes = getTargetChain()
					.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.extend(addedTypes, connectionsGroup, dataGraph, Scope.nodeShape, EffectiveTarget.Extend.left,
							false, null);
			allTargetsPlan = UnionNode.getInstanceDedupe(connectionsGroup, allTargetsPlan, addedTypes);

		}

		return Unique.getInstance(allTargetsPlan, false, connectionsGroup);
	}

	@Override
	public boolean requiresEvaluation(ConnectionsGroup connectionsGroup, Scope scope, Resource[] dataGraph,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		return super.requiresEvaluation(connectionsGroup, scope, dataGraph, stableRandomVariableProvider)
				|| connectionsGroup.getRemovedStatements().hasStatement(null, RDF.TYPE, clazz, true, dataGraph)
				|| connectionsGroup.getAddedStatements().hasStatement(null, RDF.TYPE, clazz, true, dataGraph);
	}

	@Override
	public ValidationQuery generateSparqlValidationQuery(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, boolean negatePlan, boolean negateChildren, Scope scope) {

		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget effectiveTarget = getTargetChain().getEffectiveTarget(scope,
				connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider);
		String query = effectiveTarget.getQuery(false);

		Variable<Value> value;

		if (scope == Scope.nodeShape) {

			value = null;

			var target = effectiveTarget.getTargetVar();

			query += "\n" + getFilter(connectionsGroup, target);

		} else {
			value = new Variable<>("value");

			SparqlFragment sparqlFragment = getTargetChain().getPath()
					.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(), value,
							connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider, Set.of()))
					.orElseThrow(IllegalStateException::new);

			String pathQuery = sparqlFragment.getFragment();

			query += "\n" + pathQuery;
			query += "\n" + getFilter(connectionsGroup, value);
		}

		var allTargetVariables = effectiveTarget.getAllTargetVariables();

		return new ValidationQuery(getTargetChain().getNamespaces(), query, allTargetVariables, value, scope, this,
				null, null);

	}

	private String getFilter(ConnectionsGroup connectionsGroup, Variable<Value> target) {

		RdfsSubClassOfReasoner rdfsSubClassOfReasoner = connectionsGroup.getRdfsSubClassOfReasoner();
		Set<Resource> allClasses;

		if (rdfsSubClassOfReasoner != null) {
			allClasses = rdfsSubClassOfReasoner.backwardsChain(clazz);
		} else {
			allClasses = clazzSet;
		}

		String condition = allClasses.stream()
				.map(c -> "EXISTS{" + target.asSparqlVariable() + " a <" + c.stringValue() + ">}")
				.reduce((a, b) -> a + " || " + b)
				.orElseThrow(IllegalStateException::new);

		return "FILTER(!(" + condition + "))";
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

		ClassConstraintComponent that = (ClassConstraintComponent) o;

		return clazz.equals(that.clazz);
	}

	@Override
	public int hashCode() {
		return clazz.hashCode() + "ClassConstraintComponent".hashCode();
	}
}
