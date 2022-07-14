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
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
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
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;

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
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings,
			PlanNodeProvider overrideTargetNode, Scope scope) {
		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget target = getTargetChain().getEffectiveTarget(scope,
				connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider);

		if (scope == Scope.propertyShape) {
			Path path = getTargetChain().getPath().get();

			PlanNode addedTargets;

			if (overrideTargetNode != null) {
				addedTargets = overrideTargetNode.getPlanNode();
				addedTargets = target.extend(addedTargets, connectionsGroup, validationSettings.getDataGraph(), scope,
						EffectiveTarget.Extend.right,
						false, null);

			} else {
				addedTargets = target.getPlanNode(connectionsGroup, validationSettings.getDataGraph(), scope, false,
						null);
				PlanNode addedByPath = path.getAdded(connectionsGroup, validationSettings.getDataGraph(), null);

				addedByPath = target.getTargetFilter(connectionsGroup,
						validationSettings.getDataGraph(), Unique.getInstance(new TrimToTarget(addedByPath), false));

				addedByPath = target.extend(addedByPath, connectionsGroup, validationSettings.getDataGraph(), scope,
						EffectiveTarget.Extend.left, false,
						null);

				if (connectionsGroup.getStats().hasRemoved()) {
					PlanNode deletedTypes = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, RDF.TYPE,
							clazz, validationSettings.getDataGraph(),
							UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape));

					deletedTypes = getTargetChain()
							.getEffectiveTarget(Scope.nodeShape,
									connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider)
							.extend(deletedTypes, connectionsGroup, validationSettings.getDataGraph(), Scope.nodeShape,
									EffectiveTarget.Extend.left,
									false, null);

					deletedTypes = getTargetChain()
							.getEffectiveTarget(Scope.nodeShape,
									connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider)
							.getTargetFilter(connectionsGroup, validationSettings.getDataGraph(), deletedTypes);

					addedTargets = UnionNode.getInstance(addedTargets,
							new TrimToTarget(new ShiftToPropertyShape(deletedTypes)));
				}

				addedTargets = UnionNode.getInstance(addedByPath, addedTargets);
				addedTargets = Unique.getInstance(addedTargets, false);
			}

			PlanNode joined = new BulkedExternalInnerJoin(
					addedTargets,
					connectionsGroup.getBaseConnection(),
					validationSettings.getDataGraph(),
					path.getTargetQueryFragment(new StatementMatcher.Variable("a"), new StatementMatcher.Variable("c"),
							connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider),
					false,
					null,
					BulkedExternalInnerJoin.getMapper("a", "c", scope, validationSettings.getDataGraph())
			);

			PlanNode falseNode = joined;
			if (connectionsGroup.getAddedStatements() != null) {
				// filter by type against the added statements
				falseNode = new ExternalPredicateObjectFilter(
						connectionsGroup.getAddedStatements(),
						validationSettings.getDataGraph(), RDF.TYPE, Collections.singleton(clazz),
						falseNode, false, ExternalPredicateObjectFilter.FilterOn.value);
			}

			// filter by type against the base sail
			falseNode = new ExternalPredicateObjectFilter(
					connectionsGroup.getBaseConnection(),
					validationSettings.getDataGraph(), RDF.TYPE, Collections.singleton(clazz),
					falseNode, false, ExternalPredicateObjectFilter.FilterOn.value);

			return falseNode;

		} else if (scope == Scope.nodeShape) {

			PlanNode addedTargets;

			if (overrideTargetNode != null) {
				addedTargets = overrideTargetNode.getPlanNode();
				addedTargets = target.extend(addedTargets, connectionsGroup, validationSettings.getDataGraph(), scope,
						EffectiveTarget.Extend.right,
						false, null);
			} else {
				addedTargets = target.getPlanNode(connectionsGroup, validationSettings.getDataGraph(), scope, false,
						null);

				if (connectionsGroup.getStats().hasRemoved()) {
					PlanNode deletedTypes = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, RDF.TYPE,
							clazz, validationSettings.getDataGraph(),
							UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(scope));
					deletedTypes = getTargetChain()
							.getEffectiveTarget(scope, connectionsGroup.getRdfsSubClassOfReasoner(),
									stableRandomVariableProvider)
							.getTargetFilter(connectionsGroup, validationSettings.getDataGraph(), deletedTypes);
					deletedTypes = getTargetChain()
							.getEffectiveTarget(scope, connectionsGroup.getRdfsSubClassOfReasoner(),
									stableRandomVariableProvider)
							.extend(deletedTypes, connectionsGroup, validationSettings.getDataGraph(), scope,
									EffectiveTarget.Extend.left, false, null);
					addedTargets = UnionNode.getInstance(addedTargets, new TrimToTarget(deletedTypes));
				}
			}

			// filter by type against the base sail
			PlanNode falseNode = new ExternalPredicateObjectFilter(
					connectionsGroup.getBaseConnection(),
					validationSettings.getDataGraph(), RDF.TYPE, Collections.singleton(clazz),
					addedTargets, false, ExternalPredicateObjectFilter.FilterOn.value);

			return falseNode;

		} else {
			throw new UnsupportedOperationException("Unknown scope: " + scope);
		}

	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Resource[] dataGraph, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		if (scope == Scope.propertyShape) {
			PlanNode allTargetsPlan = getTargetChain()
					.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.getPlanNode(connectionsGroup, dataGraph, Scope.nodeShape, true, null);

			// removed type statements that match clazz could affect sh:or
			if (connectionsGroup.getStats().hasRemoved()) {
				PlanNode deletedTypes = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, RDF.TYPE,
						clazz, dataGraph, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape));
				deletedTypes = getTargetChain()
						.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
								stableRandomVariableProvider)
						.getTargetFilter(connectionsGroup, dataGraph, deletedTypes);
				deletedTypes = getTargetChain()
						.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
								stableRandomVariableProvider)
						.extend(deletedTypes, connectionsGroup, dataGraph, Scope.nodeShape, EffectiveTarget.Extend.left,
								false,
								null);
				allTargetsPlan = UnionNode.getInstanceDedupe(allTargetsPlan, deletedTypes);
			}

			// added type statements that match clazz could affect sh:not
			if (connectionsGroup.getStats().hasAdded()) {
				PlanNode addedTypes = new UnorderedSelect(connectionsGroup.getAddedStatements(), null, RDF.TYPE,
						clazz, dataGraph, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape));
				addedTypes = getTargetChain()
						.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
								stableRandomVariableProvider)
						.getTargetFilter(connectionsGroup, dataGraph, addedTypes);
				addedTypes = getTargetChain()
						.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
								stableRandomVariableProvider)
						.extend(addedTypes, connectionsGroup, dataGraph, Scope.nodeShape, EffectiveTarget.Extend.left,
								false,
								null);
				allTargetsPlan = UnionNode.getInstanceDedupe(allTargetsPlan, addedTypes);
			}

			return Unique.getInstance(new TrimToTarget(new ShiftToPropertyShape(allTargetsPlan)), false);
		}
		PlanNode allTargetsPlan = EmptyNode.getInstance();

		// removed type statements that match clazz could affect sh:or
		if (connectionsGroup.getStats().hasRemoved()) {
			PlanNode deletedTypes = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null, RDF.TYPE, clazz,
					dataGraph, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape));
			deletedTypes = getTargetChain()
					.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.getTargetFilter(connectionsGroup, dataGraph, deletedTypes);
			deletedTypes = getTargetChain()
					.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.extend(deletedTypes, connectionsGroup, dataGraph, Scope.nodeShape, EffectiveTarget.Extend.left,
							false, null);
			allTargetsPlan = UnionNode.getInstanceDedupe(allTargetsPlan, deletedTypes);

		}

		// added type statements that match clazz could affect sh:not
		if (connectionsGroup.getStats().hasAdded()) {
			PlanNode addedTypes = new UnorderedSelect(connectionsGroup.getAddedStatements(), null, RDF.TYPE, clazz,
					dataGraph, UnorderedSelect.Mapper.SubjectScopedMapper.getFunction(Scope.nodeShape));
			addedTypes = getTargetChain()
					.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.getTargetFilter(connectionsGroup, dataGraph, addedTypes);
			addedTypes = getTargetChain()
					.getEffectiveTarget(Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner(),
							stableRandomVariableProvider)
					.extend(addedTypes, connectionsGroup, dataGraph, Scope.nodeShape, EffectiveTarget.Extend.left,
							false, null);
			allTargetsPlan = UnionNode.getInstanceDedupe(allTargetsPlan, addedTypes);

		}

		return Unique.getInstance(allTargetsPlan, false);
	}

	@Override
	public boolean requiresEvaluation(ConnectionsGroup connectionsGroup, Scope scope, Resource[] dataGraph,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		return super.requiresEvaluation(connectionsGroup, scope, dataGraph, stableRandomVariableProvider)
				|| connectionsGroup.getRemovedStatements().hasStatement(null, RDF.TYPE, clazz, true, dataGraph)
				|| connectionsGroup.getAddedStatements().hasStatement(null, RDF.TYPE, clazz, true, dataGraph);
	}

	@Override
	public ValidationQuery generateSparqlValidationQuery(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, boolean negatePlan, boolean negateChildren, Scope scope) {

		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget effectiveTarget = getTargetChain().getEffectiveTarget(scope,
				connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider);
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
