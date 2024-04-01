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
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.paths.SimplePath;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BufferedSplitter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ExternalFilterByQuery;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.NotValuesIn;
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
		if (!ignoredProperties.isEmpty() && !model.contains(getId(), SHACL.IGNORED_PROPERTIES, null)) {
			model.add(subject, SHACL.IGNORED_PROPERTIES, ignoredPropertiesHead);
			ShaclAstLists.listToRdf(ignoredProperties, ignoredPropertiesHead, model);
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

				BufferedSplitter addedTargetsBufferedSplitter = new BufferedSplitter(
						effectiveTarget.getPlanNode(connectionsGroup, validationSettings.getDataGraph(), scope, false,
								null));
				addedTargets = addedTargetsBufferedSplitter.getPlanNode();
				PlanNode addedByPath = path.getAllAdded(connectionsGroup, validationSettings.getDataGraph(), null);

				addedByPath = effectiveTarget.getTargetFilter(connectionsGroup,
						validationSettings.getDataGraph(), Unique.getInstance(new TrimToTarget(addedByPath), false));

				addedByPath = new ReduceTargets(addedByPath, addedTargetsBufferedSplitter.getPlanNode());

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

				addedTargets = UnionNode.getInstance(addedTargets,
						new TrimToTarget(new ShiftToPropertyShape(addedByValue)));

				addedTargets = UnionNode.getInstance(addedByPath, addedTargets);
				addedTargets = Unique.getInstance(addedTargets, false);

			}

			PlanNode falseNode = new BulkedExternalInnerJoin(
					addedTargets,
					connectionsGroup.getBaseConnection(),
					validationSettings.getDataGraph(),
					path.getTargetQueryFragment(new StatementMatcher.Variable("a"), new StatementMatcher.Variable("c"),
							connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider, Set.of()),
					false,
					null,
					BulkedExternalInnerJoin.getMapper("a", "c", scope, validationSettings.getDataGraph())
			);

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

					})
					.getTrueNode(UnBufferedPlanNode.class);

			return falseNode1;

		} else {

			PlanNode targetNodePlanNode;

			if (overrideTargetNode != null) {
				targetNodePlanNode = overrideTargetNode.getPlanNode();
			} else {
				PlanNode addedTargets = effectiveTarget.getPlanNode(connectionsGroup, validationSettings.getDataGraph(),
						scope, false, null);

				// get all subjects of all triples where the predicate is not in the allAllowedPredicates set
				UnorderedSelect unorderedSelect = new UnorderedSelect(connectionsGroup.getAddedStatements(), null, null,
						null, validationSettings.getDataGraph(),
						UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(scope), (statement -> {
							return !allAllowedPredicates.contains(statement.getPredicate());
						}));

				// then remove any that are in the addedTargets node
				NotValuesIn notValuesIn = new NotValuesIn(unorderedSelect, addedTargets);

				// trim to target and remove duplicates
				TrimToTarget trimToTarget = new TrimToTarget(notValuesIn);
				PlanNode unique = Unique.getInstance(trimToTarget, false);

				// then check that the rest are actually targets
				PlanNode targetFilter = effectiveTarget.getTargetFilter(connectionsGroup,
						validationSettings.getDataGraph(),
						unique);

				// this should now be targets that are not valid
				PlanNode extend = effectiveTarget.extend(targetFilter, connectionsGroup,
						validationSettings.getDataGraph(),
						scope, EffectiveTarget.Extend.left, false, null);

				targetNodePlanNode = UnionNode.getInstance(extend, effectiveTarget.getPlanNode(connectionsGroup,
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
					Unique.getInstance(new TrimToTarget(targetNodePlanNode), false),
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
					}
			);

			return bulkedExternalInnerJoin;
		}

	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Resource[] dataGraph, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		throw new UnsupportedOperationException();
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
