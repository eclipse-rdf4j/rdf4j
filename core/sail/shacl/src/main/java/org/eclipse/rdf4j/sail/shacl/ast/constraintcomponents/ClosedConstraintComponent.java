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

import static org.eclipse.rdf4j.model.util.Values.literal;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.CanProduceValidationReport;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclAstLists;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationQuery;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.paths.SimplePath;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.AbstractBulkJoinPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BufferedSplitter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ExternalFilterByQuery;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ReduceTargets;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.TrimToTarget;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnorderedSelect;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;

public class ClosedConstraintComponent extends AbstractConstraintComponent implements CanProduceValidationReport {

	private final List<IRI> paths;
	private final List<IRI> ignoredProperties;
	private final Resource ignoredPropertiesHead;
	private final HashSet<IRI> allAllowedPredicates;
	private final Shape shape;
	public boolean produceValidationReports;

	public ClosedConstraintComponent(ShapeSource shapeSource, List<Resource> property, Resource ignoredPropertiesHead,
			Shape shape) {

		paths = property.stream().flatMap(r -> {
			return shapeSource.getObjects(r, ShapeSource.Predicates.PATH)
					.map(o -> ((Resource) o))
					.map(path -> Path.buildPath(shapeSource, path))
					.filter(p -> p instanceof SimplePath)
					.map(p -> ((IRI) p.getId()));

		}).collect(Collectors.toList());

		if (ignoredPropertiesHead != null) {
			this.ignoredPropertiesHead = ignoredPropertiesHead;
			this.ignoredProperties = ShaclAstLists.toList(shapeSource, ignoredPropertiesHead, IRI.class);
		} else {
			this.ignoredProperties = Collections.emptyList();
			this.ignoredPropertiesHead = null;
		}
		HashSet<IRI> allAllowedPredicates = new HashSet<>(paths);
		allAllowedPredicates.addAll(ignoredProperties);
		this.allAllowedPredicates = allAllowedPredicates;
		this.shape = shape;
	}

	public ClosedConstraintComponent(ClosedConstraintComponent closedConstraintComponent) {
		paths = closedConstraintComponent.paths;
		ignoredProperties = closedConstraintComponent.ignoredProperties;
		ignoredPropertiesHead = closedConstraintComponent.ignoredPropertiesHead;
		allAllowedPredicates = closedConstraintComponent.allAllowedPredicates;
		shape = closedConstraintComponent.shape;
		produceValidationReports = closedConstraintComponent.produceValidationReports;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {

		if (!ignoredProperties.isEmpty()) {
			model.add(subject, SHACL.IGNORED_PROPERTIES, ignoredPropertiesHead);
			if (!model.contains(ignoredPropertiesHead, null, null)) {
				ShaclAstLists.listToRdf(ignoredProperties, ignoredPropertiesHead, model);
			}
		}

		model.add(subject, SHACL.CLOSED, literal(true));
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.ClosedConstraintComponent;
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, PlanNodeProvider overrideTargetNode, Scope scope) {

		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget effectiveTarget = getTargetChain().getEffectiveTarget(scope,
				connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider);

		if (scope == Scope.propertyShape) {
			Path path = getTargetChain().getPath().get();

			PlanNode addedTargets;

			if (overrideTargetNode != null) {
				addedTargets = effectiveTarget.extend(overrideTargetNode.getPlanNode(), connectionsGroup,
						validationSettings.getDataGraph(), scope,
						EffectiveTarget.Extend.right,
						false, null);
			} else {

				BufferedSplitter addedTargetsBufferedSplitter = BufferedSplitter.getInstance(
						effectiveTarget.getPlanNode(connectionsGroup, validationSettings.getDataGraph(), scope, false,
								null));
				addedTargets = addedTargetsBufferedSplitter.getPlanNode();
				PlanNode addedByPath = path.getAllAdded(connectionsGroup, validationSettings.getDataGraph(), null);

				addedByPath = effectiveTarget.getTargetFilter(connectionsGroup,
						validationSettings.getDataGraph(),
						Unique.getInstance(new TrimToTarget(addedByPath, connectionsGroup), false, connectionsGroup));

				addedByPath = new ReduceTargets(addedByPath, addedTargetsBufferedSplitter.getPlanNode(),
						connectionsGroup);

				addedByPath = effectiveTarget.extend(addedByPath, connectionsGroup, validationSettings.getDataGraph(),
						scope,
						EffectiveTarget.Extend.left, false,
						null);

				PlanNode addedByValue = new UnorderedSelect(connectionsGroup.getAddedStatements(), null, null,
						null, validationSettings.getDataGraph(),
						UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(scope), (statement -> {
							return !allAllowedPredicates.contains(statement.getPredicate());
						}));

				addedByValue = getTargetChain()
						.getEffectiveTarget(Scope.nodeShape,
								connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider)
						.extend(addedByValue, connectionsGroup, validationSettings.getDataGraph(), Scope.nodeShape,
								EffectiveTarget.Extend.left,
								false, null);

				addedByValue = getTargetChain()
						.getEffectiveTarget(Scope.nodeShape,
								connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider)
						.getTargetFilter(connectionsGroup, validationSettings.getDataGraph(), addedByValue);

				addedTargets = UnionNode.getInstance(connectionsGroup, addedTargets,
						new TrimToTarget(new ShiftToPropertyShape(addedByValue, connectionsGroup), connectionsGroup));

				addedTargets = UnionNode.getInstance(connectionsGroup, addedByPath, addedTargets);
				addedTargets = Unique.getInstance(addedTargets, false, connectionsGroup);

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

			StatementMatcher.Variable<Value> subjectVariable = stableRandomVariableProvider.next();
			StatementMatcher.Variable<Value> predicateVariable = stableRandomVariableProvider.next();
			StatementMatcher.Variable<Value> objectVariable = stableRandomVariableProvider.next();

			SparqlFragment bgp = SparqlFragment.bgp(List.of(),
					subjectVariable.asSparqlVariable() + " " + predicateVariable.asSparqlVariable() + " "
							+ objectVariable.asSparqlVariable() + ".",
					List.of());
			String notInSparqlFilter = "FILTER( " + predicateVariable.asSparqlVariable() + " NOT IN( "
					+ allAllowedPredicates.stream().map(p -> "<" + p.toString() + ">").collect(Collectors.joining(", "))
					+ " ) )";
			SparqlFragment sparqlFragmentFilter = SparqlFragment.bgp(List.of(), notInSparqlFilter, List.of());
			SparqlFragment sparqlFragment = SparqlFragment.join(List.of(bgp, sparqlFragmentFilter));

			PlanNode falseNode1 = new ExternalFilterByQuery(connectionsGroup.getBaseConnection(),
					validationSettings.getDataGraph(),
					falseNode,
					sparqlFragment,
					subjectVariable,
					ValidationTuple::getValue,
					(ValidationTuple validationTuple, BindingSet b) -> {
						if (produceValidationReports) {
							return validationTuple.addValidationResult(t -> {
								ValidationResult validationResult = new ValidationResult(t.getActiveTarget(),
										b.getValue(objectVariable.getName()),
										shape,
										this, shape.getSeverity(),
										ConstraintComponent.Scope.nodeShape, t.getContexts(),
										shape.getContexts());
								validationResult.setPathIri(b.getValue(predicateVariable.getName()));
								return validationResult;

							});
						}

						return validationTuple;

					}, connectionsGroup)
					.getTrueNode(UnBufferedPlanNode.class);

			return falseNode1;

		} else {
			assert scope == Scope.nodeShape;

			PlanNode targetNodePlanNode;

			if (overrideTargetNode != null) {
				targetNodePlanNode = getTargetChain()
						.getEffectiveTarget(scope, connectionsGroup.getRdfsSubClassOfReasoner(),
								stableRandomVariableProvider)
						.extend(overrideTargetNode.getPlanNode(), connectionsGroup, validationSettings.getDataGraph(),
								scope, EffectiveTarget.Extend.right,
								false, null);
			} else {
				PlanNode addedTargets = effectiveTarget.getPlanNode(connectionsGroup, validationSettings.getDataGraph(),
						scope, false, null);

				// get all subjects of all triples where the predicate is not in the allAllowedPredicates set
				PlanNode unorderedSelect = new UnorderedSelect(connectionsGroup.getAddedStatements(), null, null,
						null, validationSettings.getDataGraph(),
						UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(scope), (statement -> {
							return !allAllowedPredicates.contains(statement.getPredicate());
						}));

				// then remove any that are in the addedTargets node
				PlanNode notValuesIn = new ReduceTargets(unorderedSelect, addedTargets, connectionsGroup);

				// remove duplicates
				PlanNode unique = Unique.getInstance(notValuesIn, false, connectionsGroup);

				// then check that the rest are actually targets
				PlanNode targetFilter = effectiveTarget.getTargetFilter(connectionsGroup,
						validationSettings.getDataGraph(),
						unique);

				// this should now be targets that are not valid
				PlanNode extend = effectiveTarget.extend(targetFilter, connectionsGroup,
						validationSettings.getDataGraph(),
						scope, EffectiveTarget.Extend.left, false, null);

				targetNodePlanNode = UnionNode.getInstance(connectionsGroup, extend,
						effectiveTarget.getPlanNode(connectionsGroup,
								validationSettings.getDataGraph(), scope, false, null));
			}

			StatementMatcher.Variable<Value> predicateVariable = stableRandomVariableProvider.next();

			SparqlFragment bgp = SparqlFragment.bgp(List.of(), "?a " + predicateVariable.asSparqlVariable() + " ?c.",
					List.of());
			String notInSparqlFilter = "FILTER( " + predicateVariable.asSparqlVariable() + " NOT IN( "
					+ allAllowedPredicates.stream().map(p -> "<" + p.toString() + ">").collect(Collectors.joining(", "))
					+ " ) )";
			SparqlFragment sparqlFragmentFilter = SparqlFragment.bgp(List.of(), notInSparqlFilter, List.of());
			SparqlFragment sparqlFragment = SparqlFragment.join(List.of(bgp, sparqlFragmentFilter));

			BulkedExternalInnerJoin bulkedExternalInnerJoin = new BulkedExternalInnerJoin(
					Unique.getInstance(targetNodePlanNode, false, connectionsGroup),
					connectionsGroup.getBaseConnection(),
					validationSettings.getDataGraph(),
					sparqlFragment,
					false,
					null,
					(b) -> {

						ValidationTuple validationTuple = new ValidationTuple(b.getValue("a"), b.getValue("c"),
								Scope.propertyShape, true, validationSettings.getDataGraph());

						if (produceValidationReports) {
							validationTuple = validationTuple.addValidationResult(t -> {
								ValidationResult validationResult = new ValidationResult(t.getActiveTarget(),
										t.getValue(),
										shape,
										this, shape.getSeverity(),
										ConstraintComponent.Scope.nodeShape, t.getContexts(),
										shape.getContexts());
								validationResult.setPathIri(b.getValue(predicateVariable.getName()));
								return validationResult;

							});
						}
						return validationTuple;
					},
					connectionsGroup,
					List.of(AbstractBulkJoinPlanNode.DEFAULT_VARS.get(0), AbstractBulkJoinPlanNode.DEFAULT_VARS.get(1),
							predicateVariable)
			);

			return bulkedExternalInnerJoin;
		}

	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Resource[] dataGraph, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider,
			ValidationSettings validationSettings) {

		EffectiveTarget effectiveTarget = getTargetChain().getEffectiveTarget(scope,
				connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider);

		switch (scope) {
		case none:
			throw new IllegalStateException();
		case nodeShape:

			BufferedSplitter targets = BufferedSplitter.getInstance(
					effectiveTarget.getPlanNode(connectionsGroup, dataGraph, scope, false,
							null));
			// get all subjects of all triples where the predicate is not in the allAllowedPredicates set
			PlanNode statementsNotMatchingPredicateList = new UnorderedSelect(connectionsGroup.getAddedStatements(),
					null, null,
					null, dataGraph,
					UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(scope),
					(statement -> !allAllowedPredicates.contains(statement.getPredicate())));

			// then remove any that are in the targets node
			statementsNotMatchingPredicateList = new ReduceTargets(statementsNotMatchingPredicateList,
					targets.getPlanNode(), connectionsGroup);

			// then check that the rest are actually targets
			statementsNotMatchingPredicateList = effectiveTarget.getTargetFilter(connectionsGroup,
					dataGraph,
					statementsNotMatchingPredicateList);

			if (connectionsGroup.getStats().hasRemoved()) {

				// get all subjects of all triples where the predicate is not in the allAllowedPredicates set
				PlanNode removed = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, null,
						null, dataGraph,
						UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(scope),
						(statement -> !allAllowedPredicates.contains(statement.getPredicate())));

				removed = new ReduceTargets(removed, targets.getPlanNode(), connectionsGroup);

				// then check that the rest are actually targets
				removed = effectiveTarget.getTargetFilter(connectionsGroup, dataGraph, removed);

				statementsNotMatchingPredicateList = UnionNode.getInstance(connectionsGroup,
						statementsNotMatchingPredicateList, removed);

			}

			// union and remove duplicates
			PlanNode unique = Unique.getInstance(statementsNotMatchingPredicateList, false, connectionsGroup);

			// this should now be targets that are not valid
			PlanNode extend = effectiveTarget.extend(unique, connectionsGroup,
					dataGraph,
					scope, EffectiveTarget.Extend.left, false, null);

			return extend;

		case propertyShape:
			Path path = getTargetChain().getPath().get();

			BufferedSplitter addedTargetsBufferedSplitter = BufferedSplitter.getInstance(
					effectiveTarget.getPlanNode(connectionsGroup, dataGraph, scope, false,
							null));
			PlanNode addedTargets = addedTargetsBufferedSplitter.getPlanNode();
			PlanNode addedByPath = path.getAllAdded(connectionsGroup, dataGraph, null);

			addedByPath = effectiveTarget.getTargetFilter(connectionsGroup,
					dataGraph,
					Unique.getInstance(new TrimToTarget(addedByPath, connectionsGroup), false, connectionsGroup));

			addedByPath = new ReduceTargets(addedByPath, addedTargetsBufferedSplitter.getPlanNode(), connectionsGroup);

			addedByPath = effectiveTarget.extend(addedByPath, connectionsGroup, dataGraph,
					scope,
					EffectiveTarget.Extend.left, false,
					null);

			PlanNode addedByValue = new UnorderedSelect(connectionsGroup.getAddedStatements(), null, null,
					null, dataGraph,
					UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(scope), (statement -> {
						return !allAllowedPredicates.contains(statement.getPredicate());
					}));

			PlanNode removedByValue = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, null,
					null, dataGraph,
					UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(scope), (statement -> {
						return !allAllowedPredicates.contains(statement.getPredicate());
					}));

			addedByValue = UnionNode.getInstance(connectionsGroup, addedByValue, removedByValue);

			addedByValue = getTargetChain()
					.getEffectiveTarget(Scope.nodeShape,
							connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider)
					.extend(addedByValue, connectionsGroup, dataGraph, Scope.nodeShape,
							EffectiveTarget.Extend.left,
							false, null);

			addedByValue = getTargetChain()
					.getEffectiveTarget(Scope.nodeShape,
							connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider)
					.getTargetFilter(connectionsGroup, dataGraph, addedByValue);

			addedTargets = UnionNode.getInstance(connectionsGroup, addedTargets,
					new TrimToTarget(new ShiftToPropertyShape(addedByValue, connectionsGroup), connectionsGroup));

			addedTargets = UnionNode.getInstance(connectionsGroup, addedByPath, addedTargets);
			addedTargets = Unique.getInstance(addedTargets, false, connectionsGroup);

			return addedTargets;

		}

		throw new UnsupportedOperationException();
	}

	@Override
	public ValidationQuery generateSparqlValidationQuery(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, boolean negatePlan, boolean negateChildren, Scope scope) {
		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget effectiveTarget = getTargetChain().getEffectiveTarget(scope,
				connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider);
		String query = effectiveTarget.getQuery(false);

		StatementMatcher.Variable<Value> predicateVariable = stableRandomVariableProvider.next();
		StatementMatcher.Variable<Value> objectVariable = stableRandomVariableProvider.next();

		StatementMatcher.Variable<Value> value;

		if (scope == Scope.nodeShape) {

			value = null;

			var target = effectiveTarget.getTargetVar();

			query += "\n" + getFilter(target, predicateVariable, objectVariable);

		} else {
			value = new StatementMatcher.Variable<>("value");

			SparqlFragment sparqlFragment = getTargetChain().getPath()
					.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(), value,
							connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider, Set.of()))
					.orElseThrow(IllegalStateException::new);

			String pathQuery = sparqlFragment.getFragment();

			query += "\n" + pathQuery;
			query += "\n" + getFilter(value, predicateVariable, objectVariable);
		}

		var allTargetVariables = effectiveTarget.getAllTargetVariables();

		ValidationQuery validationQuery = new ValidationQuery(getTargetChain().getNamespaces(), query,
				allTargetVariables, value, scope, this,
				null, null);

		if (produceValidationReports) {
			validationQuery = validationQuery
					.withShape(shape)
					.withSeverity(shape.getSeverity());

			validationQuery.makeCurrentStateValidationReport();

			validationQuery.setValidationResultGenerator(List.of(predicateVariable, objectVariable),
					new ValidationQuery.ValidationResultGenerator() {
						@Override
						public Function<ValidationTuple, ValidationResult> getValidationTupleValidationResultFunction(
								ValidationQuery validationQuery, Resource[] shapesGraphs, BindingSet bindings) {
							Function<ValidationTuple, ValidationResult> validationResultFunction = t -> {
								ValidationResult validationResult = new ValidationResult(t.getActiveTarget(),
										bindings.getValue(objectVariable.getName()), validationQuery.getShape(),
										validationQuery.getConstraintComponent_validationReport(),
										validationQuery.getSeverity(), t.getScope(), t.getContexts(), shapesGraphs);
								validationResult.setPathIri(bindings.getValue(predicateVariable.getName()));
								return validationResult;
							};
							return validationResultFunction;
						}
					});

		}

		return validationQuery;
	}

	private String getFilter(StatementMatcher.Variable<Value> target,
			StatementMatcher.Variable<Value> predicateVariable, StatementMatcher.Variable<Value> objectVariable) {

		SparqlFragment bgp = SparqlFragment.bgp(List.of(),
				target.asSparqlVariable() + " " + predicateVariable.asSparqlVariable() + " "
						+ objectVariable.asSparqlVariable() + ".",
				List.of());
		String notInSparqlFilter = "FILTER( " + predicateVariable.asSparqlVariable() + " NOT IN( "
				+ allAllowedPredicates.stream().map(p -> "<" + p.toString() + ">").collect(Collectors.joining(", "))
				+ " ) )";
		SparqlFragment sparqlFragmentFilter = SparqlFragment.bgp(List.of(), notInSparqlFilter, List.of());
		SparqlFragment sparqlFragment = SparqlFragment.join(List.of(bgp, sparqlFragmentFilter));

		return sparqlFragment.getFragment();
	}

	@Override
	public ValidationApproach getOptimalBulkValidationApproach() {
		return ValidationApproach.SPARQL;
	}

	@Override
	public boolean requiresEvaluation(ConnectionsGroup connectionsGroup, Scope scope, Resource[] dataGraph,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		return true;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new ClosedConstraintComponent(this);
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

		ClosedConstraintComponent that = (ClosedConstraintComponent) o;

		if (!Objects.equals(paths, that.paths)) {
			return false;
		}
		return Objects.equals(ignoredProperties, that.ignoredProperties);
	}

	@Override
	public int hashCode() {
		int result = paths != null ? paths.hashCode() : 0;
		result = 31 * result + (ignoredProperties != null ? ignoredProperties.hashCode() : 0);
		return result;
	}

	@Override
	public void setProducesValidationReport(boolean producesValidationReport) {
		this.produceValidationReports = producesValidationReport;
	}

	@Override
	public boolean producesValidationReport() {
		return produceValidationReports;
	}
}
