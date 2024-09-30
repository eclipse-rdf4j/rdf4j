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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationQuery;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.AbstractBulkJoinPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalLeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.GroupByCountFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.LeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.TrimToTarget;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;

public class MinCountConstraintComponent extends AbstractConstraintComponent {

	long minCount;

	public MinCountConstraintComponent(long minCount) {
		this.minCount = minCount;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.MIN_COUNT, literal(BigInteger.valueOf(minCount)));
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.MinCountConstraintComponent;
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings,
			PlanNodeProvider overrideTargetNode, Scope scope) {

		if (minCount <= 0) {
			return EmptyNode.getInstance();
		}

		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		PlanNode target;
		if (overrideTargetNode != null) {
			target = getTargetChain()
					.getEffectiveTarget(scope, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.extend(overrideTargetNode.getPlanNode(), connectionsGroup, validationSettings.getDataGraph(),
							scope, EffectiveTarget.Extend.right,
							false, null);
			if (connectionsGroup.hasAddedStatements()) {
				PlanNode addedByPath = getTargetChain().getPath()
						.get()
						.getAnyAdded(connectionsGroup, validationSettings.getDataGraph(), null);

				// we don't need to compress here because we are anyway going to trim to target later on
				addedByPath = Unique.getInstance(addedByPath, false, connectionsGroup);

				LeftOuterJoin leftOuterJoin = new LeftOuterJoin(target, addedByPath, connectionsGroup);
				target = new GroupByCountFilter(leftOuterJoin, count -> count < minCount, connectionsGroup);
			}
		} else {
			// we can assume that we are not doing bulk validation, so it is worth checking our added statements before
			// we go to the base sail

			target = getTargetChain()
					.getEffectiveTarget(scope, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.getPlanNode(connectionsGroup, validationSettings.getDataGraph(), scope, true, null);

			PlanNode addedByPath = getTargetChain().getPath()
					.get()
					.getAnyAdded(connectionsGroup, validationSettings.getDataGraph(), null);

			// we don't need to compress here because we are anyway going to trim to target later on
			addedByPath = Unique.getInstance(addedByPath, false, connectionsGroup);

			LeftOuterJoin leftOuterJoin = new LeftOuterJoin(target, addedByPath, connectionsGroup);
			target = new GroupByCountFilter(leftOuterJoin, count -> count < minCount, connectionsGroup);
		}

		PlanNode relevantTargetsWithPath = new BulkedExternalLeftOuterJoin(
				Unique.getInstance(new TrimToTarget(target, connectionsGroup), false, connectionsGroup),
				connectionsGroup.getBaseConnection(),
				validationSettings.getDataGraph(), getTargetChain().getPath()
						.get()
						.getTargetQueryFragment(new StatementMatcher.Variable("a"), new StatementMatcher.Variable("c"),
								connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider, Set.of()),
				(b) -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true,
						validationSettings.getDataGraph()),
				connectionsGroup, AbstractBulkJoinPlanNode.DEFAULT_VARS);

		relevantTargetsWithPath = connectionsGroup.getCachedNodeFor(relevantTargetsWithPath);

		PlanNode groupByCount = new GroupByCountFilter(relevantTargetsWithPath, count -> count < minCount,
				connectionsGroup);

		return Unique.getInstance(new TrimToTarget(groupByCount, connectionsGroup), false, connectionsGroup);

	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Resource[] dataGraph, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider,
			ValidationSettings validationSettings) {
		return EmptyNode.getInstance();
	}

	@Override
	public ConstraintComponent deepClone() {
		return new MinCountConstraintComponent(minCount);
	}

	@Override
	public ValidationQuery generateSparqlValidationQuery(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, boolean negatePlan, boolean negateChildren, Scope scope) {
		if (minCount <= 0) {
			return ValidationQuery.Deactivated.getInstance();
		}

		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget effectiveTarget = getTargetChain().getEffectiveTarget(scope,
				connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider);
		String query = effectiveTarget.getQuery(false);

		if (minCount == 1) {
			StatementMatcher.Variable value = StatementMatcher.Variable.VALUE;

			String pathQuery = getTargetChain().getPath()
					.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(), value,
							connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider, Set.of()))
					.orElseThrow(IllegalStateException::new)
					.getFragment();

			query += "\nFILTER(NOT EXISTS{\n" + pathQuery + "\n})";
		} else {

			StringBuilder condition = new StringBuilder();
			ArrayList<StatementMatcher.Variable> valueVariables = new ArrayList<>();

			for (int i = 0; i < minCount; i++) {
				StatementMatcher.Variable value = stableRandomVariableProvider.next();
				valueVariables.add(value);

				String pathQuery = getTargetChain().getPath()
						.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(), value,
								connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider, Set.of()))
						.orElseThrow(IllegalStateException::new)
						.getFragment();

				condition.append(pathQuery).append("\n");
			}

			Set<String> notEquals = new HashSet<>();

			for (int i = 0; i < valueVariables.size(); i++) {
				for (int j = 0; j < valueVariables.size(); j++) {
					if (i == j) {
						continue;
					}
					if (i > j) {
						notEquals.add(valueVariables.get(i).asSparqlVariable() + " != "
								+ valueVariables.get(j).asSparqlVariable());
					} else {
						notEquals.add(valueVariables.get(j).asSparqlVariable() + " != "
								+ valueVariables.get(i).asSparqlVariable());
					}
				}
			}

			String innerCondition = String.join(" && ", notEquals);

			query += "\nFILTER(NOT EXISTS{\n" + condition.toString().trim() + "\nFILTER(" + innerCondition + ")\n})";
		}

		var allTargetVariables = effectiveTarget.getAllTargetVariables();

		return new ValidationQuery(getTargetChain().getNamespaces(), query, allTargetVariables, null, scope, this, null,
				null);

	}

	@Override
	public ValidationApproach getOptimalBulkValidationApproach() {
		return ValidationApproach.SPARQL;
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

		MinCountConstraintComponent that = (MinCountConstraintComponent) o;

		return minCount == that.minCount;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(minCount) + "MinCountConstraintComponent".hashCode();
	}
}
