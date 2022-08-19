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
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationReportNode;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;

public class NodeShape extends Shape {

	public NodeShape() {
	}

	public NodeShape(NodeShape nodeShape) {
		super(nodeShape);
	}

	public static NodeShape getInstance(ShaclProperties properties,
			ShapeSource shapeSource, Cache cache, ShaclSail shaclSail) {

		NodeShape shape = (NodeShape) cache.get(properties.getId());
		if (shape == null) {
			shape = new NodeShape();
			cache.put(properties.getId(), shape);
			shape.populate(properties, shapeSource, cache, shaclSail);
		}

		return shape;
	}

	@Override
	public void populate(ShaclProperties properties, ShapeSource connection,
			Cache cache, ShaclSail shaclSail) {
		super.populate(properties, connection, cache, shaclSail);

		if (properties.getMinCount() != null) {
			throw new IllegalStateException("NodeShapes do not support sh:MinCount in " + getId());
		}
		if (properties.getMaxCount() != null) {
			throw new IllegalStateException("NodeShapes do not support sh:MaxCount in " + getId());
		}
		if (properties.isUniqueLang()) {
			throw new IllegalStateException("NodeShapes do not support sh:uniqueLang in " + getId());
		}
		if (properties.getQualifiedValueShape() != null) {
			throw new IllegalStateException("NodeShapes do not support sh:qualifiedValueShape in " + getId());
		}
		/*
		 * Also not supported here is: - sh:lessThan - sh:lessThanOrEquals - sh:qualifiedValueShape
		 */

		constraintComponents = getConstraintComponents(properties, connection, cache, shaclSail);

	}

	@Override
	protected NodeShape shallowClone() {
		return new NodeShape(this);
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		super.toModel(subject, predicate, model, cycleDetection);
		model.add(getId(), RDF.TYPE, SHACL.NODE_SHAPE);

		if (subject != null) {
			if (predicate == null) {
				model.add(subject, SHACL.NODE, getId());
			} else {
				model.add(subject, predicate, getId());
			}

		}

		if (cycleDetection.contains(getId())) {
			return;
		}
		cycleDetection.add(getId());

		constraintComponents.forEach(c -> c.toModel(getId(), null, model, cycleDetection));

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
							negateChildren, Scope.nodeShape);
					if (!(c instanceof PropertyShape)) {
						return validationQuery1.withConstraintComponent(c.getConstraintComponent());
					}
					return validationQuery1;
				})
				.reduce((a, b) -> ValidationQuery.union(a, b, false))
				.orElseThrow(IllegalStateException::new);

		if (produceValidationReports) {
			assert constraintComponents.size() == 1;
			assert !(constraintComponents.get(0) instanceof PropertyShape);

			validationQuery = validationQuery.withShape(this);
			validationQuery = validationQuery.withSeverity(severity);
			validationQuery.makeCurrentStateValidationReport();
		}

		if (scope == Scope.propertyShape) {
			validationQuery.shiftToPropertyShape();
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

		for (ConstraintComponent constraintComponent : constraintComponents) {
			PlanNode validationPlanNode = constraintComponent
					.generateTransactionalValidationPlan(connectionsGroup, validationSettings, overrideTargetNode,
							Scope.nodeShape);

			if (produceValidationReports) {
				assert !(constraintComponent instanceof PropertyShape);
				assert constraintComponents.size() == 1;

				validationPlanNode = new ValidationReportNode(validationPlanNode, t -> {
					return new ValidationResult(t.getActiveTarget(), t.getActiveTarget(), this,
							constraintComponent.getConstraintComponent(), getSeverity(), t.getScope(), t.getContexts(),
							getContexts());
				});
			}

			if (scope == Scope.propertyShape) {
				validationPlanNode = Unique.getInstance(new ShiftToPropertyShape(validationPlanNode), true);
			}

			union = UnionNode.getInstance(union, validationPlanNode);
		}

		return union;
	}

	@Override
	public ValidationApproach getPreferredValidationApproach(ConnectionsGroup connectionsGroup) {
		return constraintComponents.stream()
				.map(constraintComponent -> constraintComponent.getPreferredValidationApproach(connectionsGroup))
				.reduce(ValidationApproach::reducePreferred)
				.orElse(ValidationApproach.Transactional);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.NodeConstraintComponent;
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Resource[] dataGraph, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {

		PlanNode planNode = constraintComponents.stream()
				.map(c -> c.getAllTargetsPlan(connectionsGroup, dataGraph, Scope.nodeShape,
						new StatementMatcher.StableRandomVariableProvider()))
				.distinct()
				.reduce(UnionNode::getInstanceDedupe)
				.orElse(EmptyNode.getInstance());

		if (connectionsGroup.getStats().hasRemoved()) {
			PlanNode planNodeEffectiveTarget = getTargetChain()
					.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.getPlanNode(connectionsGroup, dataGraph, Scope.nodeShape, true, null);

			planNode = UnionNode.getInstanceDedupe(planNode, planNodeEffectiveTarget);
		}

		if (scope == Scope.propertyShape) {
			planNode = Unique.getInstance(new ShiftToPropertyShape(planNode), true);
		}

		planNode = Unique.getInstance(planNode, false);

		return planNode;
	}

	@Override
	public ConstraintComponent deepClone() {
		NodeShape nodeShape = new NodeShape(this);

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
				.map(shape -> shape.buildSparqlValidNodes_rsx_targetShape(subject, object, rdfsSubClassOfReasoner,
						Scope.nodeShape, stableRandomVariableProvider))
				.collect(Collectors.toList());

		if (SparqlFragment.isFilterCondition(sparqlFragments)) {
			return SparqlFragment.and(sparqlFragments);
		} else {
			return SparqlFragment.join(sparqlFragments);
		}
	}

}
