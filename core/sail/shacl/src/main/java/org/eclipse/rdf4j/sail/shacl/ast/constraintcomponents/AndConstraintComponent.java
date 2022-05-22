/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.Cache;
import org.eclipse.rdf4j.sail.shacl.ast.NodeShape;
import org.eclipse.rdf4j.sail.shacl.ast.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclAstLists;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationQuery;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetChain;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;

public class AndConstraintComponent extends LogicalOperatorConstraintComponent {
	List<Shape> and;

	public AndConstraintComponent(Resource id, ShapeSource shapeSource,
			Cache cache, ShaclSail shaclSail) {
		super(id);
		and = ShaclAstLists.toList(shapeSource, id, Resource.class)
				.stream()
				.map(r -> new ShaclProperties(r, shapeSource))
				.map(p -> {
					if (p.getType() == SHACL.NODE_SHAPE) {
						return NodeShape.getInstance(p, shapeSource, cache, shaclSail);
					} else if (p.getType() == SHACL.PROPERTY_SHAPE) {
						return PropertyShape.getInstance(p, shapeSource, cache, shaclSail);
					}
					throw new IllegalStateException("Unknown shape type for " + p.getId());
				})
				.collect(Collectors.toList());

	}

	public AndConstraintComponent(AndConstraintComponent andConstraintComponent) {
		super(andConstraintComponent.getId());
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.AND, getId());

		if (!cycleDetection.contains(getId())) {
			cycleDetection.add(getId());
			and.forEach(o -> o.toModel(null, null, model, cycleDetection));
		}

		if (!model.contains(getId(), null, null)) {
			ShaclAstLists.listToRdf(and.stream().map(Shape::getId).collect(Collectors.toList()), getId(), model);
		}

	}

	@Override
	public void setTargetChain(TargetChain targetChain) {
		super.setTargetChain(targetChain);
		for (Shape shape : and) {
			shape.setTargetChain(targetChain.setOptimizable(false));
		}
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.AndConstraintComponent;
	}

	@Override
	public ValidationQuery generateSparqlValidationQuery(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings,
			boolean negatePlan, boolean negateChildren, Scope scope) {
		throw new ShaclUnsupportedException();
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings,
			PlanNodeProvider overrideTargetNode, Scope scope) {

		PlanNode planNode = and.stream()
				.map(a -> a.generateTransactionalValidationPlan(connectionsGroup, validationSettings,
						overrideTargetNode, scope))
				.reduce(UnionNode::getInstance)
				.orElse(EmptyNode.getInstance());

		return Unique.getInstance(planNode, false);

	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Resource[] dataGraph, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		PlanNode planNode = and.stream()
				.map(c -> c.getAllTargetsPlan(connectionsGroup, dataGraph, scope,
						new StatementMatcher.StableRandomVariableProvider()))
				.distinct()
				.reduce(UnionNode::getInstanceDedupe)
				.orElse(EmptyNode.getInstance());

		planNode = Unique.getInstance(planNode, false);

		return planNode;
	}

	@Override
	public ConstraintComponent deepClone() {

		AndConstraintComponent andConstraintComponent = new AndConstraintComponent(this);
		andConstraintComponent.and = and.stream()
				.map(ConstraintComponent::deepClone)
				.map(a -> ((Shape) a))
				.collect(Collectors.toList());
		return andConstraintComponent;
	}

	@Override
	public boolean requiresEvaluation(ConnectionsGroup connectionsGroup, Scope scope, Resource[] dataGraph,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		return and.stream()
				.anyMatch(c -> c.requiresEvaluation(connectionsGroup, scope, dataGraph, stableRandomVariableProvider));
	}

	@Override
	public SparqlFragment buildSparqlValidNodes_rsx_targetShape(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {

		return buildSparqlValidNodes_rsx_targetShape_inner(subject, object, rdfsSubClassOfReasoner, scope,
				stableRandomVariableProvider, and,
				getTargetChain(),
				SparqlFragment::join, SparqlFragment::and);

	}

}
