/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
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
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationQuery;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalLeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.GroupByCountFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.TrimToTarget;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;

public class MaxCountConstraintComponent extends AbstractConstraintComponent {

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

			PlanNode addedTargets = effectiveTarget.getPlanNode(connectionsGroup, validationSettings.getDataGraph(),
					scope, false, null);

			PlanNode addedByPath = path.get().getAdded(connectionsGroup, validationSettings.getDataGraph(), null);

			addedByPath = effectiveTarget.getTargetFilter(connectionsGroup,
					validationSettings.getDataGraph(), Unique.getInstance(new TrimToTarget(addedByPath), false));

			addedByPath = effectiveTarget.extend(addedByPath, connectionsGroup, validationSettings.getDataGraph(),
					scope, EffectiveTarget.Extend.left,
					false,
					null);

			mergeNode = UnionNode.getInstance(addedTargets, addedByPath);
		}

		mergeNode = Unique.getInstance(new TrimToTarget(mergeNode), false);

		PlanNode relevantTargetsWithPath;

		if (maxCount >= 0) {
			relevantTargetsWithPath = new BulkedExternalInnerJoin(
					mergeNode,
					connectionsGroup.getBaseConnection(),
					validationSettings.getDataGraph(), getTargetChain().getPath()
							.get()
							.getTargetQueryFragment(new StatementMatcher.Variable("a"),
									new StatementMatcher.Variable("c"),
									connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider),
					false,
					null,
					BulkedExternalInnerJoin.getMapper("a", "c", scope, validationSettings.getDataGraph())
			);
		} else {
			relevantTargetsWithPath = new BulkedExternalLeftOuterJoin(
					mergeNode,
					connectionsGroup.getBaseConnection(),
					validationSettings.getDataGraph(), getTargetChain().getPath()
							.get()
							.getTargetQueryFragment(new StatementMatcher.Variable("a"),
									new StatementMatcher.Variable("c"),
									connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider),
					(b) -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true,
							validationSettings.getDataGraph())
			);
		}

		PlanNode groupByCount = new GroupByCountFilter(relevantTargetsWithPath, count -> count > maxCount);

		return Unique.getInstance(new TrimToTarget(groupByCount), false);

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

			String pathQuery = getTargetChain().getPath()
					.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(), value,
							connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider))
					.orElseThrow(IllegalStateException::new);

			query += pathQuery;

		} else if (maxCount > 0) {

			StringBuilder paths = new StringBuilder();
			ArrayList<StatementMatcher.Variable> valueVariables = new ArrayList<>();

			for (int i = 0; i < maxCount + 1; i++) {
				StatementMatcher.Variable value = stableRandomVariableProvider.next();
				valueVariables.add(value);
				String pathQuery = getTargetChain().getPath()
						.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(), value,
								connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider))
						.orElseThrow(IllegalStateException::new);

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

			query += paths + "FILTER(" + innerCondition + ")\n";
		}

		List<StatementMatcher.Variable> allTargetVariables = effectiveTarget.getAllTargetVariables();

		return new ValidationQuery(query, allTargetVariables, null, scope, getConstraintComponent(), null, null);

	}

	@Override
	public ValidationApproach getOptimalBulkValidationApproach() {
		// performance of large maxCount is terrible
		if (maxCount > 5) {
			return ValidationApproach.Transactional;
		}
		return ValidationApproach.SPARQL;
	}
}
