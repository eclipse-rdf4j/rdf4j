/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.ast;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationReportNode;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;

public class NodeShape extends Shape implements ConstraintComponent, Identifiable {

	protected boolean produceValidationReports;

	public NodeShape(boolean produceValidationReports) {
		this.produceValidationReports = produceValidationReports;
	}

	public NodeShape(NodeShape nodeShape) {
		super(nodeShape);
		this.produceValidationReports = nodeShape.produceValidationReports;
	}

	public static NodeShape getInstance(ShaclProperties properties,
			RepositoryConnection connection, Cache cache, boolean produceValidationReports, ShaclSail shaclSail) {

		Shape shape = cache.get(properties.getId());
		if (shape == null) {
			shape = new NodeShape(produceValidationReports);
			cache.put(properties.getId(), shape);
			shape.populate(properties, connection, cache, shaclSail);
		}

		return (NodeShape) shape;
	}

	@Override
	public void populate(ShaclProperties properties, RepositoryConnection connection,
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
	public PlanNode generateSparqlValidationPlan(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans, boolean negatePlan, boolean negateChildren, Scope scope) {
		if (isDeactivated()) {
			return new EmptyNode();
		}

		PlanNode union = new EmptyNode();

		for (ConstraintComponent constraintComponent : constraintComponents) {
			PlanNode validationPlanNode = constraintComponent
					.generateSparqlValidationPlan(connectionsGroup, logValidationPlans, negatePlan, false,
							Scope.nodeShape);
			if (!(constraintComponent instanceof PropertyShape) && produceValidationReports) {
				validationPlanNode = new ValidationReportNode(validationPlanNode, t -> {
					return new ValidationResult(t.getActiveTarget(), t.getActiveTarget(), this,
							constraintComponent.getConstraintComponent(), getSeverity(), t.getScope());
				});
			}
			union = new UnionNode(union,
					validationPlanNode);
		}

		return union;

	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans, PlanNodeProvider overrideTargetNode,
			Scope scope) {

		if (isDeactivated()) {
			return new EmptyNode();
		}

		PlanNode union = new EmptyNode();

		for (ConstraintComponent constraintComponent : constraintComponents) {
			PlanNode validationPlanNode = constraintComponent
					.generateTransactionalValidationPlan(connectionsGroup, logValidationPlans, overrideTargetNode,
							Scope.nodeShape);

			if (!(constraintComponent instanceof PropertyShape) && produceValidationReports) {
				validationPlanNode = new ValidationReportNode(validationPlanNode, t -> {
					return new ValidationResult(t.getActiveTarget(), t.getActiveTarget(), this,
							constraintComponent.getConstraintComponent(), getSeverity(), t.getScope());
				});
			}

			if (scope == Scope.propertyShape) {
				validationPlanNode = new Unique(new ShiftToPropertyShape(validationPlanNode), true);
			}

			union = new UnionNode(union,
					validationPlanNode);
		}

		return union;
	}

	@Override
	public ValidationApproach getPreferedValidationApproach() {
		return constraintComponents.stream()
				.map(ConstraintComponent::getPreferedValidationApproach)
				.reduce(ValidationApproach::reduce)
				.orElse(ValidationApproach.Transactional);
	}

	@Override
	public Set<ValidationApproach> getSupportedValidationApproaches() {
		return constraintComponents.stream()
				.map(ConstraintComponent::getSupportedValidationApproaches)
				.flatMap(Set::stream)
				.collect(Collectors.toSet());
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.NodeConstraintComponent;
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Scope scope) {

		PlanNode planNode = constraintComponents.stream()
				.map(c -> c.getAllTargetsPlan(connectionsGroup, Scope.nodeShape))
				.reduce(UnionNode::new)
				.orElse(new EmptyNode());

		planNode = new UnionNode(planNode,
				getTargetChain()
						.getEffectiveTarget("_target", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
						.getPlanNode(connectionsGroup, Scope.nodeShape, true));

		if (scope == Scope.propertyShape) {
			planNode = new Unique(new ShiftToPropertyShape(planNode), true);
		}

		planNode = new Unique(planNode, false);

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
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope) {

		List<SparqlFragment> sparqlFragments = constraintComponents.stream()
				.map(shape -> shape.buildSparqlValidNodes_rsx_targetShape(subject, object, rdfsSubClassOfReasoner,
						Scope.nodeShape))
				.collect(Collectors.toList());

		if (SparqlFragment.isFilterCondition(sparqlFragments)) {
			return SparqlFragment.and(sparqlFragments);
		} else {
			return SparqlFragment.join(sparqlFragments);
		}
	}

}
