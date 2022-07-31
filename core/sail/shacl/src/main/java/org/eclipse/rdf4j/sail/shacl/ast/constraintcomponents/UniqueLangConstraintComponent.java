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
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationQuery;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.NonUniqueTargetLang;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.TrimToTarget;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;

public class UniqueLangConstraintComponent extends AbstractConstraintComponent {

	public UniqueLangConstraintComponent() {
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.UNIQUE_LANG, BooleanLiteral.TRUE);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.UniqueLangConstraintComponent;
	}

	@Override
	public ValidationQuery generateSparqlValidationQuery(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, boolean negatePlan, boolean negateChildren, Scope scope) {
		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget effectiveTarget = getTargetChain().getEffectiveTarget(scope,
				connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider);
		String query = effectiveTarget.getQuery(false);

		StatementMatcher.Variable value1 = stableRandomVariableProvider.next();

		String pathQuery1 = getTargetChain().getPath()
				.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(), value1,
						connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider))
				.orElseThrow(IllegalStateException::new);

		query += pathQuery1;

		StatementMatcher.Variable value2 = stableRandomVariableProvider.next();

		String pathQuery2 = getTargetChain().getPath()
				.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(), value2,
						connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider))
				.orElseThrow(IllegalStateException::new);

		query += String.join("\n", "",
				"FILTER(",
				"	EXISTS {",
				"		" + pathQuery2,
				"		FILTER(",
				"			lang(?" + value2.getName() + ") != \"\" && ",
				"			lang(?" + value1.getName() + ") != \"\" && ",
				"			?" + value1.getName() + " != ?" + value2.getName() + " && ",
				"			lang(?" + value1.getName() + ") = lang(?" + value2.getName() + ")",
				"		)",
				"	}",
				")");

		List<StatementMatcher.Variable> allTargetVariables = effectiveTarget.getAllTargetVariables();

		return new ValidationQuery(query, allTargetVariables, null, scope, getConstraintComponent(), null, null);

	}

	@Override
	public ValidationApproach getOptimalBulkValidationApproach() {
		return ValidationApproach.SPARQL;
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, PlanNodeProvider overrideTargetNode, Scope scope) {
//		assert !negateChildren : "There are no subplans!";
//		assert !negatePlan;

		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget effectiveTarget = getTargetChain().getEffectiveTarget(Scope.propertyShape,
				connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider);
		Optional<Path> path = getTargetChain().getPath();

		if (!path.isPresent() || scope != Scope.propertyShape) {
			throw new IllegalStateException("UniqueLang only operates on paths");
		}

		if (overrideTargetNode != null) {

			PlanNode targets = effectiveTarget.extend(overrideTargetNode.getPlanNode(), connectionsGroup,
					validationSettings.getDataGraph(), scope,
					EffectiveTarget.Extend.right, false, null);

			PlanNode relevantTargetsWithPath = new BulkedExternalInnerJoin(
					targets,
					connectionsGroup.getBaseConnection(),
					validationSettings.getDataGraph(), path.get()
							.getTargetQueryFragment(new StatementMatcher.Variable("a"),
									new StatementMatcher.Variable("c"),
									connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider),
					false,
					null,
					BulkedExternalInnerJoin.getMapper("a", "c", scope, validationSettings.getDataGraph())

			);

			PlanNode nonUniqueTargetLang = new NonUniqueTargetLang(relevantTargetsWithPath);
			return Unique.getInstance(new TrimToTarget(nonUniqueTargetLang), false);
		}

		if (connectionsGroup.getStats().wasEmptyBeforeTransaction()) {
			PlanNode addedTargets = effectiveTarget.getPlanNode(connectionsGroup, validationSettings.getDataGraph(),
					scope, false, null);

			PlanNode addedByPath = path.get().getAdded(connectionsGroup, validationSettings.getDataGraph(), null);

			PlanNode innerJoin = new InnerJoin(addedTargets, addedByPath).getJoined(UnBufferedPlanNode.class);

			PlanNode nonUniqueTargetLang = new NonUniqueTargetLang(innerJoin);
			return Unique.getInstance(new TrimToTarget(nonUniqueTargetLang), false);
		}

		PlanNode addedTargets = effectiveTarget.getPlanNode(connectionsGroup, validationSettings.getDataGraph(), scope,
				false, null);

		PlanNode addedByPath = path.get().getAdded(connectionsGroup, validationSettings.getDataGraph(), null);

		addedByPath = effectiveTarget.getTargetFilter(connectionsGroup,
				validationSettings.getDataGraph(), Unique.getInstance(new TrimToTarget(addedByPath), false));

		addedByPath = effectiveTarget.extend(addedByPath, connectionsGroup, validationSettings.getDataGraph(), scope,
				EffectiveTarget.Extend.left, false,
				null);

		PlanNode mergeNode = UnionNode.getInstance(addedTargets, addedByPath);

		mergeNode = new TrimToTarget(mergeNode);

		PlanNode allRelevantTargets = Unique.getInstance(mergeNode, false);

		PlanNode relevantTargetsWithPath = new BulkedExternalInnerJoin(
				allRelevantTargets,
				connectionsGroup.getBaseConnection(),
				validationSettings.getDataGraph(), path.get()
						.getTargetQueryFragment(new StatementMatcher.Variable("a"), new StatementMatcher.Variable("c"),
								connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider),
				false,
				null,
				BulkedExternalInnerJoin.getMapper("a", "c", scope, validationSettings.getDataGraph())

		);

		PlanNode nonUniqueTargetLang = new NonUniqueTargetLang(relevantTargetsWithPath);

		return Unique.getInstance(new TrimToTarget(nonUniqueTargetLang), false);

	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Resource[] dataGraph, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		if (scope == Scope.propertyShape) {
			PlanNode allTargetsPlan = getTargetChain()
					.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.getPlanNode(connectionsGroup, dataGraph, Scope.nodeShape, true, null);

			return Unique.getInstance(new ShiftToPropertyShape(allTargetsPlan), true);
		}
		return EmptyNode.getInstance();
	}

	@Override
	public ConstraintComponent deepClone() {
		return new UniqueLangConstraintComponent();
	}
}
