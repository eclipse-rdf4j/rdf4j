/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationQuery;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ExternalPredicateObjectFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.TrimToTarget;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnorderedSelect;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;

public class ClassConstraintComponent extends AbstractConstraintComponent {

	IRI clazz;

	public ClassConstraintComponent(IRI clazz) {
		this.clazz = clazz;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.CLASS, clazz);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.ClassConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new ClassConstraintComponent(clazz);
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			PlanNodeProvider overrideTargetNode, Scope scope) {
		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget target = getTargetChain().getEffectiveTarget("_target", scope,
				connectionsGroup.getRdfsSubClassOfReasoner());

		if (scope == Scope.propertyShape) {
			Path path = getTargetChain().getPath().get();

			PlanNode addedTargets;

			if (overrideTargetNode != null) {
				addedTargets = overrideTargetNode.getPlanNode();
				addedTargets = target.extend(addedTargets, connectionsGroup, scope, EffectiveTarget.Extend.right,
						false, null);

			} else {
				addedTargets = target.getPlanNode(connectionsGroup, scope, false, null);
				PlanNode addedByPath = path.getAdded(connectionsGroup, null);

				addedByPath = target.getTargetFilter(connectionsGroup,
						Unique.getInstance(new TrimToTarget(addedByPath), false));

				addedByPath = target.extend(addedByPath, connectionsGroup, scope, EffectiveTarget.Extend.left, false,
						null);

				if (connectionsGroup.getStats().hasRemoved()) {
					PlanNode deletedTypes = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, RDF.TYPE,
							clazz, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape));

					deletedTypes = getTargetChain()
							.getEffectiveTarget("target_", Scope.nodeShape,
									connectionsGroup.getRdfsSubClassOfReasoner())
							.extend(deletedTypes, connectionsGroup, Scope.nodeShape, EffectiveTarget.Extend.left,
									false, null);

					deletedTypes = getTargetChain()
							.getEffectiveTarget("target_", Scope.nodeShape,
									connectionsGroup.getRdfsSubClassOfReasoner())
							.getTargetFilter(connectionsGroup, deletedTypes);

					addedTargets = UnionNode.getInstance(addedTargets,
							new TrimToTarget(new ShiftToPropertyShape(deletedTypes)));
				}

				addedTargets = UnionNode.getInstance(addedByPath, addedTargets);
				addedTargets = Unique.getInstance(addedTargets, false);
			}

			PlanNode joined = new BulkedExternalInnerJoin(
					addedTargets,
					connectionsGroup.getBaseConnection(),
					connectionsGroup.getBaseValueFactory(),
					path.getTargetQueryFragment(new StatementMatcher.Variable("a"), new StatementMatcher.Variable("c"),
							connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider),
					false,
					null,
					(b) -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true)
			);

			// filter by type against the base sail
			PlanNode falseNode = new ExternalPredicateObjectFilter(
					connectionsGroup.getBaseConnection(),
					RDF.TYPE, Collections.singleton(clazz),
					joined, false, ExternalPredicateObjectFilter.FilterOn.value);

			return falseNode;

		} else if (scope == Scope.nodeShape) {

			PlanNode addedTargets;

			if (overrideTargetNode != null) {
				addedTargets = overrideTargetNode.getPlanNode();
				addedTargets = target.extend(addedTargets, connectionsGroup, scope, EffectiveTarget.Extend.right,
						false, null);
			} else {
				addedTargets = target.getPlanNode(connectionsGroup, scope, false, null);

				if (connectionsGroup.getStats().hasRemoved()) {
					PlanNode deletedTypes = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, RDF.TYPE,
							clazz, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(scope));
					deletedTypes = getTargetChain()
							.getEffectiveTarget("target_", scope, connectionsGroup.getRdfsSubClassOfReasoner())
							.getTargetFilter(connectionsGroup, deletedTypes);
					deletedTypes = getTargetChain()
							.getEffectiveTarget("target_", scope, connectionsGroup.getRdfsSubClassOfReasoner())
							.extend(deletedTypes, connectionsGroup, scope, EffectiveTarget.Extend.left, false, null);
					addedTargets = UnionNode.getInstance(addedTargets, new TrimToTarget(deletedTypes));
				}
			}

			// filter by type against the base sail
			PlanNode falseNode = new ExternalPredicateObjectFilter(
					connectionsGroup.getBaseConnection(),
					RDF.TYPE, Collections.singleton(clazz),
					addedTargets, false, ExternalPredicateObjectFilter.FilterOn.value);

			return falseNode;

		} else {
			throw new UnsupportedOperationException("Unknown scope: " + scope);
		}

	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Scope scope) {
		if (scope == Scope.propertyShape) {
			PlanNode allTargetsPlan = getTargetChain()
					.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
					.getPlanNode(connectionsGroup, Scope.nodeShape, true, null);

			// removed type statements that match clazz could affect sh:or
			if (connectionsGroup.getStats().hasRemoved()) {
				PlanNode deletedTypes = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, RDF.TYPE,
						clazz, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape));
				deletedTypes = getTargetChain()
						.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
						.getTargetFilter(connectionsGroup, deletedTypes);
				deletedTypes = getTargetChain()
						.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
						.extend(deletedTypes, connectionsGroup, Scope.nodeShape, EffectiveTarget.Extend.left, false,
								null);
				allTargetsPlan = UnionNode.getInstanceDedupe(allTargetsPlan, deletedTypes);
			}

			// added type statements that match clazz could affect sh:not
			if (connectionsGroup.getStats().hasAdded()) {
				PlanNode addedTypes = new UnorderedSelect(connectionsGroup.getAddedStatements(), null, RDF.TYPE,
						clazz, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape));
				addedTypes = getTargetChain()
						.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
						.getTargetFilter(connectionsGroup, addedTypes);
				addedTypes = getTargetChain()
						.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
						.extend(addedTypes, connectionsGroup, Scope.nodeShape, EffectiveTarget.Extend.left, false,
								null);
				allTargetsPlan = UnionNode.getInstanceDedupe(allTargetsPlan, addedTypes);
			}

			return Unique.getInstance(new TrimToTarget(new ShiftToPropertyShape(allTargetsPlan)), false);
		}
		PlanNode allTargetsPlan = EmptyNode.getInstance();

		// removed type statements that match clazz could affect sh:or
		if (connectionsGroup.getStats().hasRemoved()) {
			PlanNode deletedTypes = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, RDF.TYPE, clazz,
					UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape));
			deletedTypes = getTargetChain()
					.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
					.getTargetFilter(connectionsGroup, deletedTypes);
			deletedTypes = getTargetChain()
					.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
					.extend(deletedTypes, connectionsGroup, Scope.nodeShape, EffectiveTarget.Extend.left, false, null);
			allTargetsPlan = UnionNode.getInstanceDedupe(allTargetsPlan, deletedTypes);

		}

		// added type statements that match clazz could affect sh:not
		if (connectionsGroup.getStats().hasAdded()) {
			PlanNode addedTypes = new UnorderedSelect(connectionsGroup.getAddedStatements(), null, RDF.TYPE, clazz,
					UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape));
			addedTypes = getTargetChain()
					.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
					.getTargetFilter(connectionsGroup, addedTypes);
			addedTypes = getTargetChain()
					.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
					.extend(addedTypes, connectionsGroup, Scope.nodeShape, EffectiveTarget.Extend.left, false, null);
			allTargetsPlan = UnionNode.getInstanceDedupe(allTargetsPlan, addedTypes);

		}

		return Unique.getInstance(allTargetsPlan, false);
	}

	@Override
	public boolean requiresEvaluation(ConnectionsGroup connectionsGroup, Scope scope) {
		return super.requiresEvaluation(connectionsGroup, scope)
				|| connectionsGroup.getRemovedStatements().hasStatement(null, RDF.TYPE, clazz, true)
				|| connectionsGroup.getAddedStatements().hasStatement(null, RDF.TYPE, clazz, true);
	}

	@Override
	public ValidationQuery generateSparqlValidationQuery(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans, boolean negatePlan, boolean negateChildren, Scope scope) {

		String targetVarPrefix = "target_";
		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget effectiveTarget = getTargetChain().getEffectiveTarget(targetVarPrefix, scope,
				connectionsGroup.getRdfsSubClassOfReasoner());
		String query = effectiveTarget.getQuery(false);

		StatementMatcher.Variable value;

		if (scope == Scope.nodeShape) {

			value = null;

			StatementMatcher.Variable target = effectiveTarget.getTargetVar();

			query += getFilter(connectionsGroup, target);

		} else {
			value = new StatementMatcher.Variable("value");

			String pathQuery = getTargetChain().getPath()
					.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(), value,
							connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider))
					.orElseThrow(IllegalStateException::new);

			query += pathQuery;
			query += getFilter(connectionsGroup, value);
		}

		List<StatementMatcher.Variable> allTargetVariables = effectiveTarget.getAllTargetVariables();

		return new ValidationQuery(query, allTargetVariables, value, scope, getConstraintComponent(), null, null);

	}

	private String getFilter(ConnectionsGroup connectionsGroup, StatementMatcher.Variable target) {

		RdfsSubClassOfReasoner rdfsSubClassOfReasoner = connectionsGroup.getRdfsSubClassOfReasoner();
		Set<Resource> allClasses;

		if (rdfsSubClassOfReasoner != null) {
			allClasses = rdfsSubClassOfReasoner.backwardsChain(clazz);
		} else {
			allClasses = Collections.singleton(clazz);
		}

		String condition = allClasses.stream()
				.map(c -> "EXISTS{?" + target.getName() + " a <" + c.stringValue() + ">}")
				.reduce((a, b) -> a + " || " + b)
				.orElseThrow(IllegalStateException::new);

		return "\n FILTER(!(" + condition + "))";
	}

	@Override
	public ValidationApproach getOptimalBulkValidationApproach() {
		return ValidationApproach.SPARQL;
	}
}
