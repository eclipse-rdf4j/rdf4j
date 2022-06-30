/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationQuery;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.AllTargetsPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.FilterPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.ast.targets.TargetChain;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimpleAbstractConstraintComponent extends AbstractConstraintComponent {

	private static final Logger logger = LoggerFactory.getLogger(SimpleAbstractConstraintComponent.class);

	private static final String uuid = UUID.randomUUID().toString().replace("-", "");
	private static final AtomicLong counter = new AtomicLong();

	private Resource id;
	TargetChain targetChain;

	public SimpleAbstractConstraintComponent(Resource id) {
		this.id = id;
	}

	public SimpleAbstractConstraintComponent() {

	}

	public Resource getId() {
		return id;
	}

	@Override
	public TargetChain getTargetChain() {
		return targetChain;
	}

	@Override
	public void setTargetChain(TargetChain targetChain) {
		this.targetChain = targetChain;
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, PlanNodeProvider overrideTargetNode, Scope scope) {

		boolean negatePlan = false;
		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget effectiveTarget = targetChain.getEffectiveTarget(scope,
				connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider);
		Optional<Path> path = targetChain.getPath();

		if (overrideTargetNode != null) {

			PlanNode planNode;

			if (scope == Scope.nodeShape) {
				PlanNode overrideTargetPlanNode = overrideTargetNode.getPlanNode();

				if (overrideTargetPlanNode instanceof AllTargetsPlanNode) {
					PlanNode allTargets = effectiveTarget.getAllTargets(connectionsGroup,
							validationSettings.getDataGraph(), scope);
					allTargets = getFilterAttacherWithNegation(negatePlan, allTargets);

					return Unique.getInstance(allTargets, true);
				} else {
					return effectiveTarget.extend(overrideTargetPlanNode, connectionsGroup,
							validationSettings.getDataGraph(), scope,
							EffectiveTarget.Extend.right,
							false,
							p -> getFilterAttacherWithNegation(negatePlan, p)
					);

				}

			} else {
				PlanNode overrideTargetPlanNode = overrideTargetNode.getPlanNode();

				if (overrideTargetPlanNode instanceof AllTargetsPlanNode) {
					// We are cheating a bit here by retrieving all the targets and values at the same time by
					// pretending to be in node shape scope and then shifting the results back to property shape scope
					PlanNode allTargets = targetChain
							.getEffectiveTarget(Scope.nodeShape,
									connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider)
							.getAllTargets(connectionsGroup, validationSettings.getDataGraph(), Scope.nodeShape);
					allTargets = new ShiftToPropertyShape(allTargets);

					allTargets = getFilterAttacherWithNegation(negatePlan, allTargets);

					return Unique.getInstance(allTargets, true);

				} else {

					overrideTargetPlanNode = effectiveTarget.extend(overrideTargetPlanNode, connectionsGroup,
							validationSettings.getDataGraph(), scope,
							EffectiveTarget.Extend.right, false, null);

					planNode = new BulkedExternalInnerJoin(overrideTargetPlanNode,
							connectionsGroup.getBaseConnection(),
							validationSettings.getDataGraph(), path.get()
									.getTargetQueryFragment(new StatementMatcher.Variable("a"),
											new StatementMatcher.Variable("c"),
											connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider),
							false, null,
							BulkedExternalInnerJoin.getMapper("a", "c", scope, validationSettings.getDataGraph())
					);
				}
			}

			return getFilterAttacherWithNegation(negatePlan, planNode);

		}

		if (scope == Scope.nodeShape) {
			return effectiveTarget.getPlanNode(connectionsGroup, validationSettings.getDataGraph(), scope, false,
					p -> getFilterAttacherWithNegation(negatePlan, p));
		}

		PlanNode invalidValuesDirectOnPath = path.get()
				.getAdded(connectionsGroup, validationSettings.getDataGraph(),
						planNode -> getFilterAttacherWithNegation(negatePlan, planNode));

		InnerJoin innerJoin = new InnerJoin(
				effectiveTarget.getPlanNode(connectionsGroup, validationSettings.getDataGraph(), scope, false, null),
				invalidValuesDirectOnPath);

		if (connectionsGroup.getStats().wasEmptyBeforeTransaction()) {
			return innerJoin.getJoined(UnBufferedPlanNode.class);

		} else {

			PlanNode top = innerJoin.getJoined(BufferedPlanNode.class);

			PlanNode discardedRight = innerJoin.getDiscardedRight(BufferedPlanNode.class);

			PlanNode typeFilterPlan = effectiveTarget.getTargetFilter(connectionsGroup,
					validationSettings.getDataGraph(), discardedRight);

			typeFilterPlan = effectiveTarget.extend(typeFilterPlan, connectionsGroup, validationSettings.getDataGraph(),
					scope,
					EffectiveTarget.Extend.left, true, null);

			top = UnionNode.getInstance(top, typeFilterPlan);

			PlanNode bulkedExternalInnerJoin = new BulkedExternalInnerJoin(
					effectiveTarget.getPlanNode(connectionsGroup, validationSettings.getDataGraph(), scope, false,
							null),
					connectionsGroup.getBaseConnection(),
					validationSettings.getDataGraph(),
					path.get()
							.getTargetQueryFragment(new StatementMatcher.Variable("a"),
									new StatementMatcher.Variable("c"), connectionsGroup.getRdfsSubClassOfReasoner(),
									stableRandomVariableProvider),
					connectionsGroup.hasPreviousStateConnection(),
					connectionsGroup.getPreviousStateConnection(),
					b -> new ValidationTuple(b.getValue("a"), b.getValue("c"), scope, true,
							validationSettings.getDataGraph()));

			top = UnionNode.getInstance(top, bulkedExternalInnerJoin);

			return getFilterAttacherWithNegation(negatePlan, top);

		}
	}

	@Override
	public ValidationQuery generateSparqlValidationQuery(ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings, boolean negatePlan, boolean negateChildren, Scope scope) {

		StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider = new StatementMatcher.StableRandomVariableProvider();

		EffectiveTarget effectiveTarget = targetChain.getEffectiveTarget(scope,
				connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider);
		String query = effectiveTarget.getQuery(false);

		StatementMatcher.Variable value;

		if (scope == Scope.nodeShape) {

			value = null;

			query += getSparqlFilter(negatePlan, effectiveTarget.getTargetVar());

		} else {
			value = StatementMatcher.Variable.VALUE;

			String pathQuery = targetChain.getPath()
					.map(p -> p.getTargetQueryFragment(effectiveTarget.getTargetVar(), value,
							connectionsGroup.getRdfsSubClassOfReasoner(), stableRandomVariableProvider))
					.orElseThrow(IllegalStateException::new);

			query += pathQuery;
			query += getSparqlFilter(negatePlan, value);
		}

		List<StatementMatcher.Variable> allTargetVariables = effectiveTarget.getAllTargetVariables();

		return new ValidationQuery(query, allTargetVariables, value, scope, getConstraintComponent(), null, null);

	}

	private String getSparqlFilter(boolean negatePlan, StatementMatcher.Variable variable) {
		// We use BIND and COALESCE because the filter expression could cause an error and the SHACL spec implicitly
		// says that values that cause errors are in violation of the constraint.

		assert !negatePlan : "This code has not been tested with negated plans! Should be still coalesce to true?";

		String tempVar = "?" + uuid + counter.incrementAndGet();

		return String.join("\n", "",
				"BIND((" + getSparqlFilterExpression(variable.getName(), negatePlan) + ") as " + tempVar + ")",
				"FILTER(COALESCE(" + tempVar + ", true))"
		);
	}

	/**
	 * Simple constraints need only implement this method to support SPARQL based validation. The returned filter body
	 * should evaluate to true for values that fail validation, unless negated==true. If the filter condition throws an
	 * error (a SPARQL runtime error, not Java error) then the error will be caught and coalesced to `true`.
	 *
	 * @param varName
	 * @param negated
	 * @return a string that is the body of a SPARQL filter
	 */
	abstract String getSparqlFilterExpression(String varName, boolean negated);

	private PlanNode getFilterAttacherWithNegation(boolean negatePlan, PlanNode allTargets) {
		if (negatePlan) {
			allTargets = getFilterAttacher().apply(allTargets).getTrueNode(UnBufferedPlanNode.class);
		} else {
			allTargets = getFilterAttacher().apply(allTargets).getFalseNode(UnBufferedPlanNode.class);
		}
		return allTargets;
	}

	@Override
	public ValidationApproach getPreferredValidationApproach(ConnectionsGroup connectionsGroup) {
		return ValidationApproach.Transactional;
	}

	@Override
	public ValidationApproach getOptimalBulkValidationApproach() {
		return ValidationApproach.SPARQL;
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		throw new ShaclUnsupportedException(this.getClass().getSimpleName());
	}

	abstract Function<PlanNode, FilterPlanNode> getFilterAttacher();

	String literalToString(Literal literal) {
		IRI datatype = (literal).getDatatype();
		if (datatype == null) {
			return "\"" + literal.stringValue() + "\"";
		}
		if ((literal).getLanguage().isPresent()) {
			return "\"" + literal.stringValue() + "\"@" + (literal).getLanguage().get();
		}
		return "\"" + literal.stringValue() + "\"^^<" + datatype.stringValue() + ">";

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

}
