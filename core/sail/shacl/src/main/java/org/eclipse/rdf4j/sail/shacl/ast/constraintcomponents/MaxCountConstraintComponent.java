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
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationQuery;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.AbstractBulkJoinPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalLeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.GroupByCountFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.TrimToTarget;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaxCountConstraintComponent extends AbstractConstraintComponent {

	private static final Logger logger = LoggerFactory.getLogger(MaxCountConstraintComponent.class);

	// Performance degrades quickly as the maxCount increases when using a SPARQL Validation Approach. The default is 5,
	// but it can be tuned using the system property below.
	private static final String SPARQL_VALIDATION_APPROACH_LIMIT_PROPERTY = "org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.MaxCountConstraintComponent.sparqlValidationApproachLimit";
	public static long SPARQL_VALIDATION_APPROACH_LIMIT = System
			.getProperty(SPARQL_VALIDATION_APPROACH_LIMIT_PROPERTY) == null ? 1
					: Long.parseLong(System.getProperty(SPARQL_VALIDATION_APPROACH_LIMIT_PROPERTY));

	private final long maxCount;

	public MaxCountConstraintComponent(long maxCount) {
		this.maxCount = maxCount;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.MAX_COUNT, literal(BigInteger.valueOf(maxCount)));
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.MaxCountConstraintComponent;
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings,
			PlanNodeProvider overrideTargetNode, Scope scope) {

		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget effectiveTarget = getTargetChain().getEffectiveTarget(scope,
				connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider);
		Optional<Path> path = getTargetChain().getPath();

		PlanNode mergeNode;

		if (overrideTargetNode != null) {
			mergeNode = effectiveTarget.extend(overrideTargetNode.getPlanNode(), connectionsGroup,
					validationSettings.getDataGraph(), scope,
					EffectiveTarget.Extend.right, false, null);
		} else {
			mergeNode = getAllTargetsIncludingThoseAddedByPath(connectionsGroup, validationSettings, scope,
					effectiveTarget, path.get(), false);
		}

		mergeNode = Unique.getInstance(new TrimToTarget(mergeNode, connectionsGroup), false, connectionsGroup);

		PlanNode relevantTargetsWithPath;

		if (maxCount >= 0) {
			relevantTargetsWithPath = new BulkedExternalInnerJoin(
					mergeNode,
					connectionsGroup.getBaseConnection(),
					validationSettings.getDataGraph(), getTargetChain().getPath()
							.get()
							.getTargetQueryFragment(new StatementMatcher.Variable("a"),
									new StatementMatcher.Variable("c"),
									connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider,
									Set.of()),
					false,
					null,
					BulkedExternalInnerJoin.getMapper("a", "c", scope, validationSettings.getDataGraph()),
					connectionsGroup, AbstractBulkJoinPlanNode.DEFAULT_VARS);
		} else {
			relevantTargetsWithPath = new BulkedExternalLeftOuterJoin(
					mergeNode,
					connectionsGroup.getBaseConnection(),
					validationSettings.getDataGraph(), getTargetChain().getPath()
							.get()
							.getTargetQueryFragment(new StatementMatcher.Variable("a"),
									new StatementMatcher.Variable("c"),
									connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider,
									Set.of()),
					(b) -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true,
							validationSettings.getDataGraph()),
					connectionsGroup, AbstractBulkJoinPlanNode.DEFAULT_VARS);
		}

		relevantTargetsWithPath = connectionsGroup.getCachedNodeFor(relevantTargetsWithPath);

		PlanNode groupByCount = new GroupByCountFilter(relevantTargetsWithPath, count -> count > maxCount,
				connectionsGroup);

		return Unique.getInstance(new TrimToTarget(groupByCount, connectionsGroup), false, connectionsGroup);

	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Resource[] dataGraph, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider,
			ValidationSettings validationSettings) {
		if (scope == Scope.propertyShape) {
			PlanNode allTargetsPlan = getTargetChain()
					.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.getPlanNode(connectionsGroup, dataGraph, Scope.nodeShape, true, null);

			return Unique.getInstance(new ShiftToPropertyShape(allTargetsPlan, connectionsGroup), true,
					connectionsGroup);
		}
		return EmptyNode.getInstance();
	}

	@Override
	public ConstraintComponent deepClone() {
		return new MaxCountConstraintComponent(maxCount);
	}

	@Override
	public ValidationQuery generateSparqlValidationQuery(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, boolean negatePlan, boolean negateChildren, Scope scope) {

		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget effectiveTarget = getTargetChain().getEffectiveTarget(scope,
				connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider);
		String query = effectiveTarget.getQuery(false);

		if (maxCount == 0) {
			StatementMatcher.Variable value = StatementMatcher.Variable.VALUE;

			Optional<SparqlFragment> sparqlFragment = getTargetChain().getPath()
					.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(), value,
							connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider, Set.of()));

			String pathQuery = sparqlFragment
					.orElseThrow(IllegalStateException::new)
					.getFragment();

			query += "\n" + pathQuery;

		} else if (maxCount > 0) {

			StringBuilder paths = new StringBuilder();
			ArrayList<StatementMatcher.Variable> valueVariables = new ArrayList<>();

			for (int i = 0; i < maxCount + 1; i++) {
				StatementMatcher.Variable value = stableRandomVariableProvider.next();
				valueVariables.add(value);
				String pathQuery = getTargetChain().getPath()
						.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(), value,
								connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider, Set.of()))
						.orElseThrow(IllegalStateException::new)
						.getFragment();

				paths.append(pathQuery).append("\n");
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

			query += "\n" + paths.toString().trim() + "\n" + "FILTER(" + innerCondition + ")";
		}

		var allTargetVariables = effectiveTarget.getAllTargetVariables();

		return new ValidationQuery(getTargetChain().getNamespaces(), query, allTargetVariables, null, scope, this, null,
				null);

	}

	@Override
	public ValidationApproach getOptimalBulkValidationApproach() {
		if (maxCount > SPARQL_VALIDATION_APPROACH_LIMIT) {
			if (logger.isDebugEnabled()) {
				logger.debug(
						"maxCount is {}, which is greater than the limit of {}, using ValidationApproach.Transactional instead of ValidationApproach.SPARQL for {}",
						maxCount, SPARQL_VALIDATION_APPROACH_LIMIT, stringRepresentationOfValue(getId()));
			}
			return ValidationApproach.Transactional;
		}
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

		MaxCountConstraintComponent that = (MaxCountConstraintComponent) o;

		return maxCount == that.maxCount;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(maxCount) + "MaxCountConstraintComponent".hashCode();
	}
}
