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
import java.util.UUID;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyShape extends Shape implements ConstraintComponent, Identifiable {
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

	public static PropertyShape getInstance(ShaclProperties properties, RepositoryConnection connection, Cache cache,
			ShaclSail shaclSail) {
		Shape shape = cache.get(properties.getId());
		if (shape == null) {
			shape = new PropertyShape();
			cache.put(properties.getId(), shape);
			shape.populate(properties, connection, cache, shaclSail);
		}

		if (shape.constraintComponents.isEmpty()) {
			shape.deactivated = true;
		}

		return (PropertyShape) shape;
	}

	@Override
	public void populate(ShaclProperties properties, RepositoryConnection connection,
			Cache cache, ShaclSail shaclSail) {
		super.populate(properties, connection, cache, shaclSail);

		this.path = Path.buildPath(connection, properties.getPath());

		if (this.path == null) {
			throw new IllegalStateException(properties.getId() + " is a sh:PropertyShape without a sh:path!");
		}

		constraintComponents = getConstraintComponents(properties, connection, cache, shaclSail);
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
	public PlanNode generateSparqlValidationPlan(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans, boolean negatePlan, boolean negateChildren, Scope scope) {
		if (isDeactivated()) {
			return new EmptyNode();
		}

		PlanNode union = new EmptyNode();

		for (ConstraintComponent constraintComponent : constraintComponents) {
			PlanNode validationPlanNode = constraintComponent
					.generateSparqlValidationPlan(connectionsGroup, logValidationPlans, negatePlan, false,
							Scope.propertyShape);

			if (!(constraintComponent instanceof PropertyShape)) {
				validationPlanNode = new ValidationReportNode(validationPlanNode, t -> {
					return new ValidationResult(t.getActiveTarget(), t.getValue(), this,
							constraintComponent.getConstraintComponent(), getSeverity(), t.getScope());
				});
			}

			validationPlanNode = new Unique(new TargetChainPopper(validationPlanNode), true);

			union = new UnionNode(union, validationPlanNode);
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

//		if (negatePlan) {
//			assert overrideTargetNode == null : "Negated property shape with override target is not supported at the moment!";
//
//			PlanNode ret = new EmptyNode();
//
//			for (ConstraintComponent constraintComponent : constraintComponents) {
//				PlanNode planNode = constraintComponent.generateTransactionalValidationPlan(connectionsGroup,
//						logValidationPlans, () -> getAllLocalTargetsPlan(connectionsGroup, negatePlan), negateChildren,
//						false, Scope.propertyShape);
//
//				PlanNode allTargetsPlan = getAllLocalTargetsPlan(connectionsGroup, negatePlan);
//
//				Unique invalid = new Unique(planNode);
//
//				PlanNode discardedLeft = new InnerJoin(allTargetsPlan, invalid)
//						.getDiscardedLeft(BufferedPlanNode.class);
//
//				ret = new UnionNode(ret, discardedLeft);
//
//			}
//
//			return ret;
//
//		}

		for (ConstraintComponent constraintComponent : constraintComponents) {
			if (!getPath().isSupported()) {
				logger.error("Unsupported path detected. Shape ignored! \n" + this.toString());
				continue;
			}

			PlanNode validationPlanNode = constraintComponent
					.generateTransactionalValidationPlan(connectionsGroup, logValidationPlans, overrideTargetNode,
							Scope.propertyShape);

			if (!(constraintComponent instanceof PropertyShape)) {
				validationPlanNode = new ValidationReportNode(validationPlanNode, t -> {
					return new ValidationResult(t.getActiveTarget(), t.getValue(), this,
							constraintComponent.getConstraintComponent(), getSeverity(), t.getScope());
				});
			}

			if (scope == Scope.propertyShape) {
				validationPlanNode = new Unique(new TargetChainPopper(validationPlanNode), true);
			} else {
				validationPlanNode = new Unique(new ShiftToNodeShape(validationPlanNode), true);
			}

			union = new UnionNode(union, validationPlanNode);
		}

		return union;
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Scope scope) {
		PlanNode planNode = constraintComponents.stream()
				.map(c -> c.getAllTargetsPlan(connectionsGroup, Scope.propertyShape))
				.reduce(UnionNode::new)
				.orElse(new EmptyNode());

		planNode = new UnionNode(planNode,
				getTargetChain()
						.getEffectiveTarget("_target", Scope.propertyShape,
								connectionsGroup.getRdfsSubClassOfReasoner())
						.getPlanNode(connectionsGroup, Scope.propertyShape, true));

		if (scope == Scope.propertyShape) {
			planNode = new Unique(new TargetChainPopper(planNode), true);
		} else {
			planNode = new ShiftToNodeShape(planNode);
		}

		planNode = new Unique(planNode, false);

		return planNode;
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
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope) {

		List<SparqlFragment> sparqlFragments = constraintComponents.stream()
				.map(shape -> shape.buildSparqlValidNodes_rsx_targetShape(object,
						StatementMatcher.Variable.getRandomInstance(), rdfsSubClassOfReasoner, Scope.propertyShape))
				.collect(Collectors.toList());

		if (SparqlFragment.isFilterCondition(sparqlFragments)) {
			return SparqlFragment.and(sparqlFragments);
		} else {
			return SparqlFragment.join(sparqlFragments);
		}

	}

}
