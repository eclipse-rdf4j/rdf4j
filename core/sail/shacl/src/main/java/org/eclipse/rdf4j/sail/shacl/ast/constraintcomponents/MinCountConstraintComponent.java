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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationQuery;
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
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			PlanNodeProvider overrideTargetNode, Scope scope) {

		if (minCount <= 0) {
			return EmptyNode.getInstance();
		}

		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		PlanNode target = getTargetChain()
				.getEffectiveTarget("_target", scope, connectionsGroup.getRdfsSubClassOfReasoner())
				.getPlanNode(connectionsGroup, scope, true, null);

		if (overrideTargetNode != null) {
			target = getTargetChain().getEffectiveTarget("_target", scope, connectionsGroup.getRdfsSubClassOfReasoner())
					.extend(overrideTargetNode.getPlanNode(), connectionsGroup, scope, EffectiveTarget.Extend.right,
							false, null);
		} else {
			// we can assume that we are not doing bulk validation, so it is worth checking our added statements before
			// we go to the base sail

			PlanNode addedByPath = getTargetChain().getPath().get().getAdded(connectionsGroup, null);
			LeftOuterJoin leftOuterJoin = new LeftOuterJoin(target, addedByPath);
			target = new GroupByCountFilter(leftOuterJoin, count -> count < minCount);
		}

		PlanNode relevantTargetsWithPath = new BulkedExternalLeftOuterJoin(
				Unique.getInstance(new TrimToTarget(target), false),
				connectionsGroup.getBaseConnection(),
				connectionsGroup.getBaseValueFactory(),
				getTargetChain().getPath()
						.get()
						.getTargetQueryFragment(new StatementMatcher.Variable("a"), new StatementMatcher.Variable("c"),
								connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider),
				false,
				null,
				(b) -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true)
		);

		PlanNode groupByCount = new GroupByCountFilter(relevantTargetsWithPath, count -> count < minCount);

		return Unique.getInstance(new TrimToTarget(groupByCount), false);

	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Scope scope) {
		return EmptyNode.getInstance();
	}

	@Override
	public ConstraintComponent deepClone() {
		return new MinCountConstraintComponent(minCount);
	}

	@Override
	public ValidationQuery generateSparqlValidationQuery(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans, boolean negatePlan, boolean negateChildren, Scope scope) {
		if (minCount <= 0) {
			return ValidationQuery.Deactivated.getInstance();
		}

		String targetVarPrefix = "target_";
		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget effectiveTarget = getTargetChain().getEffectiveTarget(targetVarPrefix, scope,
				connectionsGroup.getRdfsSubClassOfReasoner());
		String query = effectiveTarget.getQuery(false);

		if (minCount == 1) {
			StatementMatcher.Variable value = new StatementMatcher.Variable("value");

			String pathQuery = getTargetChain().getPath()
					.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(), value,
							connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider))
					.orElseThrow(IllegalStateException::new);

			query += "\nFILTER(NOT EXISTS{" + pathQuery + "})";
		} else {

			StringBuilder condition = new StringBuilder();
			String valuePrefix = "value_";

			for (int i = 0; i < minCount; i++) {
				StatementMatcher.Variable value = new StatementMatcher.Variable(valuePrefix + i);

				String pathQuery = getTargetChain().getPath()
						.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(), value,
								connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider))
						.orElseThrow(IllegalStateException::new);

				condition.append(pathQuery).append("\n");
			}

			List<String> collect = LongStream.range(0, minCount)
					.mapToObj(i -> "?" + valuePrefix + i)
					.collect(Collectors.toList());

			Set<String> notEquals = new HashSet<>();

			for (String left : collect) {
				for (String right : collect) {
					if (left == right) {
						continue;
					}
					if (left.compareTo(right) < 0) {
						notEquals.add(left + " != " + right);
					} else {
						notEquals.add(right + " != " + left);

					}
				}
			}

			String innerCondition = String.join(" && ", notEquals);

			query += "\nFILTER(NOT EXISTS{" + condition + "FILTER(" + innerCondition + ")\n})";
		}

		List<StatementMatcher.Variable> allTargetVariables = effectiveTarget.getAllTargetVariables();

		return new ValidationQuery(query, allTargetVariables, null, scope, getConstraintComponent(), null, null);

	}

	@Override
	public ValidationApproach getOptimalBulkValidationApproach() {
		return ValidationApproach.SPARQL;
	}
}
