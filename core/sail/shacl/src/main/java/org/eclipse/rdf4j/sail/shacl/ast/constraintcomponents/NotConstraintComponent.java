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

import java.util.Set;

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
import org.eclipse.rdf4j.sail.shacl.ast.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.ast.Shape;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationQuery;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.NotValuesIn;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetChain;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;

public class NotConstraintComponent extends AbstractConstraintComponent {
	Shape not;

	public NotConstraintComponent(Resource id, ShapeSource shapeSource,
			Cache cache, ShaclSail shaclSail) {
		super(id);

		ShaclProperties p = new ShaclProperties(id, shapeSource);

		if (p.getType() == SHACL.NODE_SHAPE) {
			not = NodeShape.getInstance(p, shapeSource, cache, shaclSail);
		} else if (p.getType() == SHACL.PROPERTY_SHAPE) {
			not = PropertyShape.getInstance(p, shapeSource, cache, shaclSail);
		} else {
			throw new IllegalStateException("Unknown shape type for " + p.getId());
		}

	}

	public NotConstraintComponent(NotConstraintComponent notConstraintComponent) {
		super(notConstraintComponent.getId());
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.NOT, getId());

		not.toModel(null, null, model, cycleDetection);

	}

	@Override
	public void setTargetChain(TargetChain targetChain) {
		super.setTargetChain(targetChain);
		not.setTargetChain(targetChain.setOptimizable(false));
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.NotConstraintComponent;
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

		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		PlanNodeProvider planNodeProvider;
		if (overrideTargetNode != null) {
			planNodeProvider = overrideTargetNode;
		} else {
			planNodeProvider = () -> getAllTargetsPlan(connectionsGroup, validationSettings.getDataGraph(), scope,
					stableRandomVariableProvider);
		}

		PlanNode planNode = not.generateTransactionalValidationPlan(
				connectionsGroup,
				validationSettings,
				planNodeProvider,
				scope
		);

		PlanNode invalid = Unique.getInstance(planNode, false);

		PlanNode allTargetsPlan;
		if (overrideTargetNode != null) {
			if (scope == Scope.propertyShape) {
				allTargetsPlan = getTargetChain()
						.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
								stableRandomVariableProvider)
						.extend(planNodeProvider.getPlanNode(), connectionsGroup, validationSettings.getDataGraph(),
								Scope.nodeShape,
								EffectiveTarget.Extend.right, false, null);
				allTargetsPlan = Unique.getInstance(new ShiftToPropertyShape(allTargetsPlan), true);
			} else {
				allTargetsPlan = getTargetChain()
						.getEffectiveTarget(scope, connectionsGroup.getRdfsSubClassOfReasoner(),
								stableRandomVariableProvider)
						.extend(planNodeProvider.getPlanNode(), connectionsGroup, validationSettings.getDataGraph(),
								scope, EffectiveTarget.Extend.right,
								false, null);
			}

		} else {
			allTargetsPlan = planNodeProvider.getPlanNode();
		}

		invalid = new NotValuesIn(allTargetsPlan, invalid);

		return invalid;

	}

	/*
	 * PlanNodeProvider targetProvider = overrideTargetNode; if (targetProvider == null) { targetProvider = () ->
	 * getAllTargetsPlan(connectionsGroup, negatePlan, scope); }else{ System.out.println(); }
	 *
	 * PlanNode allTargetsPlan = targetProvider.getPlanNode();
	 *
	 * allTargetsPlan = new DebugPlanNode(allTargetsPlan, p -> { System.out.println("HERE!" + p); });
	 *
	 * PlanNode planNode = not.generateTransactionalValidationPlan(connectionsGroup, logValidationPlans, targetProvider,
	 * negateChildren, false, scope);
	 *
	 * PlanNode invalid = Unique.getInstance(planNode);
	 *
	 * PlanNode discardedLeft = new NotValuesIn(allTargetsPlan, invalid);
	 *
	 * discardedLeft = new DebugPlanNode(discardedLeft, p -> { System.out.println(); });
	 *
	 * return discardedLeft;
	 *
	 */

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Resource[] dataGraph, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		PlanNode allTargets;

		if (scope == Scope.propertyShape) {
			PlanNode allTargetsPlan = getTargetChain()
					.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.getPlanNode(connectionsGroup, dataGraph, Scope.nodeShape, true, null);

			allTargets = Unique.getInstance(new ShiftToPropertyShape(allTargetsPlan), true);
		} else {
			allTargets = getTargetChain()
					.getEffectiveTarget(scope, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.getPlanNode(connectionsGroup, dataGraph, scope, true, null);

		}

		PlanNode notTargets = not.getAllTargetsPlan(connectionsGroup, dataGraph, scope,
				new StatementMatcher.StableRandomVariableProvider());

		return Unique.getInstance(UnionNode.getInstanceDedupe(allTargets, notTargets), false);
	}

	@Override
	public ConstraintComponent deepClone() {
		NotConstraintComponent notConstraintComponent = new NotConstraintComponent(this);
		notConstraintComponent.not = (Shape) not.deepClone();
		return notConstraintComponent;
	}

	@Override
	public boolean requiresEvaluation(ConnectionsGroup connectionsGroup, Scope scope, Resource[] dataGraph,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		return not.requiresEvaluation(connectionsGroup, scope, dataGraph, stableRandomVariableProvider);
	}
}
