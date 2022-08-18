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
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
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

	List<String> name;
	List<String> description;
	Object defaultValue;
	Object group;

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
	}

	public static PropertyShape getInstance(ShaclProperties properties, ShapeSource shapeSource, Cache cache,
			ShaclSail shaclSail) {
		Shape shape = cache.get(properties.getId());
		if (shape == null) {
			shape = new PropertyShape();
			cache.put(properties.getId(), shape);
			shape.populate(properties, shapeSource, cache, shaclSail);
		}

		if (shape.constraintComponents.isEmpty()) {
			shape.deactivated = true;
		}

		return (PropertyShape) shape;
	}

	@Override
	public void populate(ShaclProperties properties, ShapeSource connection,
			Cache cache, ShaclSail shaclSail) {
		super.populate(properties, connection, cache, shaclSail);

		this.path = Path.buildPath(connection, properties.getPath());

		if (this.path == null) {
			throw new IllegalStateException(properties.getId() + " is a sh:PropertyShape without a sh:path!");
		}

		constraintComponents = getConstraintComponents(properties, connection, cache, shaclSail
		);
	}

	@Override
	protected Shape shallowClone() {
		return new PropertyShape(this);
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {

		super.toModel(subject, predicate, model, cycleDetection);
		model.add(getId(), RDF.TYPE, SHACL.PROPERTY_SHAPE);

		if (subject != null) {
			if (predicate == null) {
				model.add(subject, SHACL.PROPERTY, getId());
			} else {
				model.add(subject, predicate, getId());
			}
		}

		model.add(getId(), SHACL.PATH, path.getId());
		path.toModel(path.getId(), null, model, cycleDetection);

		if (cycleDetection.contains(getId())) {
			return;
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

		ValidationQuery validationQuery = constraintComponents.stream()
				.map(c -> {
					ValidationQuery validationQuery1 = c.generateSparqlValidationQuery(connectionsGroup,
							validationSettings, negatePlan,
							negateChildren, Scope.propertyShape);
					if (!(c instanceof PropertyShape)) {
						return validationQuery1.withConstraintComponent(c.getConstraintComponent());
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

		PlanNode union = EmptyNode.getInstance();

//		if (negatePlan) {
//			assert overrideTargetNode == null : "Negated property shape with override target is not supported at the moment!";
//
//			PlanNode ret = EmptyNode.getInstance();
//
//			for (ConstraintComponent constraintComponent : constraintComponents) {
//				PlanNode planNode = constraintComponent.generateTransactionalValidationPlan(connectionsGroup,
//						logValidationPlans, () -> getAllLocalTargetsPlan(connectionsGroup, negatePlan), negateChildren,
//						false, Scope.propertyShape);
//
//				PlanNode allTargetsPlan = getAllLocalTargetsPlan(connectionsGroup, negatePlan);
//
//				Unique invalid = Unique.getInstance(planNode);
//
//				PlanNode discardedLeft = new InnerJoin(allTargetsPlan, invalid)
//						.getDiscardedLeft(BufferedPlanNode.class);
//
//				ret = UnionNode.getInstance(ret, discardedLeft);
//
//			}
//
//			return ret;
//
//		}

		for (ConstraintComponent constraintComponent : constraintComponents) {
			if (!getPath().isSupported()) {
				logger.error("Unsupported path detected. Shape ignored! \n" + this);
				continue;
			}

			PlanNode validationPlanNode = constraintComponent
					.generateTransactionalValidationPlan(connectionsGroup, validationSettings, overrideTargetNode,
							Scope.propertyShape);

			if (!(constraintComponent instanceof PropertyShape)) {
				validationPlanNode = new ValidationReportNode(validationPlanNode, t -> {
					return new ValidationResult(t.getActiveTarget(), t.getValue(), this,
							constraintComponent.getConstraintComponent(), getSeverity(), t.getScope(), t.getContexts(),
							getContexts());
				});
			}

			if (scope == Scope.propertyShape) {
				validationPlanNode = Unique.getInstance(new TargetChainPopper(validationPlanNode), true);
			} else {
				validationPlanNode = Unique.getInstance(new ShiftToNodeShape(validationPlanNode), true);
			}

			union = UnionNode.getInstance(union, validationPlanNode);
		}

		return union;
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Resource[] dataGraph, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		PlanNode planNode = constraintComponents.stream()
				.map(c -> c.getAllTargetsPlan(connectionsGroup, dataGraph, Scope.propertyShape,
						new StatementMatcher.StableRandomVariableProvider()))
				.distinct()
				.reduce(UnionNode::getInstanceDedupe)
				.orElse(EmptyNode.getInstance());

		if (connectionsGroup.getStats().hasRemoved()) {
			PlanNode planNodeEffectiveTarget = getTargetChain()
					.getEffectiveTarget(Scope.propertyShape, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.getPlanNode(connectionsGroup, dataGraph, Scope.propertyShape, true, null);

			planNode = UnionNode.getInstanceDedupe(planNode, planNodeEffectiveTarget);
		}

		if (scope == Scope.propertyShape) {
			planNode = Unique.getInstance(new TargetChainPopper(planNode), true);
		} else {
			planNode = new ShiftToNodeShape(planNode);
		}

		planNode = Unique.getInstance(planNode, false);

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
	public ConstraintComponent deepClone() {
		PropertyShape nodeShape = new PropertyShape(this);

		nodeShape.constraintComponents = constraintComponents.stream()
				.map(ConstraintComponent::deepClone)
				.collect(Collectors.toList());

		return nodeShape;
	}

	@Override
	public SparqlFragment buildSparqlValidNodes_rsx_targetShape(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
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

}
