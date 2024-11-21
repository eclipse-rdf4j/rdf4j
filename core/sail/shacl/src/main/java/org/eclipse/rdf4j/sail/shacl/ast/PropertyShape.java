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
package org.eclipse.rdf4j.sail.shacl.ast;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher.Variable;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToNodeShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.TargetChainPopper;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationReportNode;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetChain;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyShape extends Shape {
	private static final Logger logger = LoggerFactory.getLogger(PropertyShape.class);

	List<Literal> name;
	List<Literal> description;
	Value defaultValue;
	Value group;
	Value order;

	Path path;

	public PropertyShape() {
	}

	public PropertyShape(PropertyShape propertyShape) {
		super(propertyShape);
		this.name = propertyShape.name;
		this.description = propertyShape.description;
		this.defaultValue = propertyShape.defaultValue;
		this.group = propertyShape.group;
		this.path = propertyShape.path;
		this.order = propertyShape.order;
	}

	public static PropertyShape getInstance(ShaclProperties properties, ShapeSource shapeSource,
			ParseSettings parseSettings, Cache cache) {

		Shape shape = cache.get(properties.getId());

		if (shape == null) {
			shape = new PropertyShape();
			cache.put(properties.getId(), shape);
			shape.populate(properties, shapeSource, parseSettings, cache);
		}

		if (shape.constraintComponents.isEmpty()) {
			shape.deactivated = true;
		}

		return (PropertyShape) shape;
	}

	@Override
	public void populate(ShaclProperties properties, ShapeSource connection, ParseSettings parseSettings, Cache cache) {

		super.populate(properties, connection, parseSettings, cache);

		this.path = Path.buildPath(connection, properties.getPath());

		if (this.path == null) {
			throw new IllegalStateException(properties.getId() + " is a sh:PropertyShape without a sh:path!");
		}

		this.name = properties.getName();
		this.description = properties.getDescription();
		this.defaultValue = properties.getDefaultValue();
		this.order = properties.getOrder();
		this.group = properties.getGroup();

		constraintComponents = getConstraintComponents(properties, connection, parseSettings, cache);
	}

	@Override
	protected Shape shallowClone() {
		return new PropertyShape(this);
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {

		super.toModel(subject, predicate, model, cycleDetection);
		model.add(getId(), RDF.TYPE, SHACL.PROPERTY_SHAPE);

		for (Literal literal : name) {
			model.add(getId(), SHACL.NAME, literal);
		}

		for (Literal literal : description) {
			model.add(getId(), SHACL.DESCRIPTION, literal);
		}

		if (defaultValue != null) {
			model.add(getId(), SHACL.DEFAULT_VALUE, defaultValue);
		}

		if (order != null) {
			model.add(getId(), SHACL.ORDER, order);
		}

		if (group != null) {
			model.add(getId(), SHACL.GROUP, group);
		}

		if (subject != null) {
			if (predicate == null) {
				model.add(subject, SHACL.PROPERTY, getId());
			} else {
				model.add(subject, predicate, getId());
			}
		}

//		if (cycleDetection.contains(getId())) {
//			return;
//		}
//		cycleDetection.add(getId());

		if (!cycleDetection.contains(getId())) {
			model.add(getId(), SHACL.PATH, path.getId());
			path.toModel(path.getId(), null, model, cycleDetection);
		}
		cycleDetection.add(getId());

		constraintComponents.forEach(c -> c.toModel(getId(), null, model, cycleDetection));

	}

	@Override
	public void setTargetChain(TargetChain targetChain) {
		super.setTargetChain(targetChain.add(path));
	}

	@Override
	public ValidationQuery generateSparqlValidationQuery(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, boolean negatePlan, boolean negateChildren, Scope scope) {

		if (deactivated) {
			return ValidationQuery.Deactivated.getInstance();
		}

		if (!getPath().isSupported()) {
			logger.error("Unsupported SHACL feature detected: {}. Shape ignored!\n{}", path, this);
			return ValidationQuery.Deactivated.getInstance();
		}

		ValidationQuery validationQuery = constraintComponents.stream()
				.map(c -> {
					ValidationQuery validationQuery1 = c.generateSparqlValidationQuery(connectionsGroup,
							validationSettings, negatePlan,
							negateChildren, Scope.propertyShape);
					if (!(c instanceof PropertyShape)) {
						return validationQuery1.withConstraintComponent(c);
					}
					return validationQuery1;
				})
				.reduce((a, b) -> ValidationQuery.union(a, b, !produceValidationReports))
				.orElseThrow(IllegalStateException::new);

		if (produceValidationReports) {
			assert constraintComponents.size() == 1;
			assert !(constraintComponents.get(0) instanceof PropertyShape);

			validationQuery.withShape(this);
			validationQuery.withSeverity(getSeverity());
			validationQuery.makeCurrentStateValidationReport();
		}

		if (scope == Scope.propertyShape) {
			validationQuery.popTargetChain();
		} else {
			validationQuery.shiftToNodeShape();
		}

		return validationQuery;

	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, PlanNodeProvider overrideTargetNode,
			Scope scope) {

		if (isDeactivated()) {
			return EmptyNode.getInstance();
		}

		if (!getPath().isSupported()) {
			logger.error("Unsupported SHACL feature detected: {}. Shape ignored!\n{}", path, this);
			return EmptyNode.getInstance();
		}

		PlanNode union = EmptyNode.getInstance();

		for (ConstraintComponent constraintComponent : constraintComponents) {

			PlanNode validationPlanNode = constraintComponent
					.generateTransactionalValidationPlan(connectionsGroup, validationSettings, overrideTargetNode,
							Scope.propertyShape);

			if (produceValidationReports) {
				validationPlanNode = new ValidationReportNode(validationPlanNode, t -> {
					return new ValidationResult(t.getActiveTarget(), t.getValue(), this,
							constraintComponent, getSeverity(), t.getScope(), t.getContexts(),
							getContexts());
				}, connectionsGroup);
			}

			if (scope == Scope.propertyShape) {
				validationPlanNode = Unique.getInstance(new TargetChainPopper(validationPlanNode, connectionsGroup),
						true, connectionsGroup);
			} else {
				validationPlanNode = Unique.getInstance(new ShiftToNodeShape(validationPlanNode, connectionsGroup),
						true, connectionsGroup);
			}

			union = UnionNode.getInstance(connectionsGroup, union, validationPlanNode);
		}

		return union;
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Resource[] dataGraph, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider,
			ValidationSettings validationSettings) {
		PlanNode planNode = constraintComponents.stream()
				.map(c -> c.getAllTargetsPlan(connectionsGroup, dataGraph, Scope.propertyShape,
						new StatementMatcher.StableRandomVariableProvider(), validationSettings))
				.distinct()
				.reduce((nodes, nodes2) -> UnionNode.getInstanceDedupe(connectionsGroup, nodes, nodes2))
				.orElse(EmptyNode.getInstance());

		if (connectionsGroup.getStats().hasRemoved()) {
			PlanNode planNodeEffectiveTarget = getTargetChain()
					.getEffectiveTarget(Scope.propertyShape, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.getPlanNode(connectionsGroup, dataGraph, Scope.propertyShape, true, null);

			planNode = UnionNode.getInstanceDedupe(connectionsGroup, planNode, planNodeEffectiveTarget);
		}

		if (scope == Scope.propertyShape) {
			planNode = Unique.getInstance(new TargetChainPopper(planNode, connectionsGroup), true, connectionsGroup);
		} else {
			planNode = new ShiftToNodeShape(planNode, connectionsGroup);
		}

		planNode = Unique.getInstance(planNode, false, connectionsGroup);

		return planNode;
	}

	@Override
	public ValidationApproach getPreferredValidationApproach(ConnectionsGroup connectionsGroup) {
		return constraintComponents.stream()
				.map(constraintComponent -> constraintComponent.getPreferredValidationApproach(connectionsGroup))
				.reduce(ValidationApproach::reducePreferred)
				.orElse(ValidationApproach.MOST_COMPATIBLE);
	}

	public Path getPath() {
		return path;
	}

	@Override
	public boolean requiresEvaluation(ConnectionsGroup connectionsGroup, Scope scope, Resource[] dataGraph,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		if (!getPath().isSupported()) {
			logger.error("Unsupported SHACL feature detected: {}. Shape ignored!\n{}", path, this);
			return false;
		}

		return super.requiresEvaluation(connectionsGroup, scope, dataGraph, stableRandomVariableProvider);
	}

	@Override
	public ConstraintComponent deepClone() {
		PropertyShape nodeShape = new PropertyShape(this);

		nodeShape.constraintComponents = constraintComponents.stream()
				.map(ConstraintComponent::deepClone)
				.collect(Collectors.toList());

		return nodeShape;
	}

	@Override
	public SparqlFragment buildSparqlValidNodes_rsx_targetShape(Variable<Value> subject,
			Variable<Value> object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {

		List<SparqlFragment> sparqlFragments = constraintComponents.stream()
				.map(shape -> shape.buildSparqlValidNodes_rsx_targetShape(object,
						stableRandomVariableProvider.next(), rdfsSubClassOfReasoner, Scope.propertyShape,
						stableRandomVariableProvider))
				.collect(Collectors.toList());

		if (SparqlFragment.isFilterCondition(sparqlFragments)) {
			return SparqlFragment.and(sparqlFragments);
		} else {
			return SparqlFragment.join(sparqlFragments);
		}

	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.PropertyConstraintComponent;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}

		PropertyShape that = (PropertyShape) o;

		if (!Objects.equals(name, that.name)) {
			return false;
		}
		if (!Objects.equals(description, that.description)) {
			return false;
		}
		if (!Objects.equals(defaultValue, that.defaultValue)) {
			return false;
		}
		if (!Objects.equals(group, that.group)) {
			return false;
		}
		return Objects.equals(path, that.path);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (description != null ? description.hashCode() : 0);
		result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
		result = 31 * result + (group != null ? group.hashCode() : 0);
		result = 31 * result + (path != null ? path.hashCode() : 0);
		return result;
	}
}
