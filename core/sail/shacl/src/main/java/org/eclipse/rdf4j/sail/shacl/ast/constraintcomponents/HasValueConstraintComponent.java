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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationQuery;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalLeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.GroupByFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.TrimToTarget;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValueInFilter;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;

public class HasValueConstraintComponent extends AbstractConstraintComponent {

	Value hasValue;

	public HasValueConstraintComponent(Value hasValue) {
		this.hasValue = hasValue;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		model.add(subject, SHACL.HAS_VALUE, hasValue);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.HasValueConstraintComponent;
	}

	@Override
	public ConstraintComponent deepClone() {
		return new HasValueConstraintComponent(hasValue);
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
				addedTargets = target.getPlanNode(connectionsGroup, validationSettings.getDataGraph(), scope, true,
						null);
				PlanNode addedByPath = path.getAdded(connectionsGroup, validationSettings.getDataGraph(), null);

				addedByPath = target.getTargetFilter(connectionsGroup,
						validationSettings.getDataGraph(), Unique.getInstance(new TrimToTarget(addedByPath), false));
				addedByPath = target.extend(addedByPath, connectionsGroup, validationSettings.getDataGraph(), scope,
						EffectiveTarget.Extend.left, false,
						null);

				addedTargets = UnionNode.getInstance(addedByPath, addedTargets);
				addedTargets = Unique.getInstance(addedTargets, false);
			}

			PlanNode joined = new BulkedExternalLeftOuterJoin(
					addedTargets,
					connectionsGroup.getBaseConnection(),
					validationSettings.getDataGraph(),
					path.getTargetQueryFragment(new StatementMatcher.Variable("a"), new StatementMatcher.Variable("c"),
							connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider),
					(b) -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true,
							validationSettings.getDataGraph())
			);

			PlanNode invalidTargets = new GroupByFilter(joined, group -> {
				return group.stream().map(ValidationTuple::getValue).noneMatch(v -> hasValue.equals(v));
			});

			return Unique.getInstance(new TrimToTarget(invalidTargets), false);

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
			}

			PlanNode falseNode = new ValueInFilter(addedTargets, new HashSet<>(Collections.singletonList(hasValue)))
					.getFalseNode(UnBufferedPlanNode.class);

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

			return Unique.getInstance(new ShiftToPropertyShape(allTargetsPlan), true);
		}
		return EmptyNode.getInstance();
	}

	@Override
	public SparqlFragment buildSparqlValidNodes_rsx_targetShape(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {

		List<StatementMatcher> statementMatchers = Collections.emptyList();

		if (getTargetChain().getPath().isPresent()) {
			Path path = getTargetChain().getPath().get();

			statementMatchers = path
					.getStatementMatcher(subject, new StatementMatcher.Variable(hasValue), rdfsSubClassOfReasoner)
					.collect(Collectors.toList());
		}

		if (scope == Scope.propertyShape) {
			Path path = getTargetChain().getPath().get();

			if (hasValue.isIRI()) {
				return SparqlFragment.bgp("BIND(<" + hasValue + "> as ?" + object.getName() + ")\n"
						+ path.getTargetQueryFragment(subject, object, rdfsSubClassOfReasoner,
								stableRandomVariableProvider),
						statementMatchers);
			}
			if (hasValue.isLiteral()) {
				return SparqlFragment.bgp("BIND(" + hasValue.toString() + " as ?" + object.getName() + ")\n"
						+ path.getTargetQueryFragment(subject, object, rdfsSubClassOfReasoner,
								stableRandomVariableProvider),
						statementMatchers);
			}

			throw new UnsupportedOperationException(
					"value was unsupported type: " + hasValue.getClass().getSimpleName());

		} else {
			if (hasValue.isIRI()) {
				return SparqlFragment.filterCondition("?" + object.getName() + " = <" + hasValue + ">",
						statementMatchers);
			} else if (hasValue.isLiteral()) {
				return SparqlFragment.filterCondition("?" + object.getName() + " = " + hasValue, statementMatchers);
			}
			throw new UnsupportedOperationException(
					"value was unsupported type: " + hasValue.getClass().getSimpleName());

		}
	}

	@Override
	public ValidationQuery generateSparqlValidationQuery(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, boolean negatePlan, boolean negateChildren, Scope scope) {

		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget effectiveTarget = getTargetChain().getEffectiveTarget(scope,
				connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider);
		String query = effectiveTarget.getQuery(false);

		if (scope == Scope.nodeShape) {

			query += "FILTER(?" + effectiveTarget.getTargetVar().getName() + " != "
					+ stringRepresentationOfValue(hasValue) + ")\n";

		} else {
			StatementMatcher.Variable value = StatementMatcher.Variable.VALUE;

			String pathQuery = getTargetChain().getPath()
					.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(), value,
							connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider))
					.orElseThrow(IllegalStateException::new);

			query += "FILTER( " +
					"NOT EXISTS{" +
					"	BIND(" + stringRepresentationOfValue(hasValue) + " as ?" + value.getName() + ")\n" +
					pathQuery +
					"})";

		}

		List<StatementMatcher.Variable> allTargetVariables = effectiveTarget.getAllTargetVariables();

		return new ValidationQuery(query, allTargetVariables, null, scope, getConstraintComponent(), null, null);

	}

	private String stringRepresentationOfValue(Value value) {
		if (value.isIRI()) {
			return "<" + value + ">";
		}
		if (value.isLiteral()) {
			IRI datatype = ((Literal) value).getDatatype();
			if (datatype == null) {
				return "\"" + value.stringValue() + "\"";
			}
			if (((Literal) value).getLanguage().isPresent()) {
				return "\"" + value.stringValue() + "\"@" + ((Literal) value).getLanguage().get();
			}
			return "\"" + value.stringValue() + "\"^^<" + datatype.stringValue() + ">";
		}

		throw new IllegalStateException(value.getClass().getSimpleName());
	}

	@Override
	public ValidationApproach getOptimalBulkValidationApproach() {
		return ValidationApproach.SPARQL;
	}
}
