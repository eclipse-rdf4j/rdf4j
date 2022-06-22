/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.targets;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.Targetable;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.AllTargetsPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BindSelect;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ExternalFilterByQuery;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.TupleMapper;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;

public class EffectiveTarget {

	public static final String TARGET_VAR_PREFIX = "target_";
	private final ArrayDeque<EffectiveTargetObject> chain;
	private final EffectiveTargetObject optional;

	public EffectiveTarget(ArrayDeque<Targetable> chain, Targetable optional,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		int index = 0;

		this.chain = new ArrayDeque<>();

		EffectiveTargetObject previous = null;

		for (Targetable targetable : chain) {
			EffectiveTargetObject effectiveTargetObject = new EffectiveTargetObject(
					new StatementMatcher.Variable(TARGET_VAR_PREFIX + String.format("%010d", index++)),
					targetable,
					previous,
					rdfsSubClassOfReasoner,
					stableRandomVariableProvider);
			previous = effectiveTargetObject;
			this.chain.addLast(effectiveTargetObject);
		}

		if (optional != null) {
			this.optional = new EffectiveTargetObject(
					new StatementMatcher.Variable(TARGET_VAR_PREFIX + String.format("%010d", index)),
					optional,
					previous,
					rdfsSubClassOfReasoner,
					stableRandomVariableProvider);
		} else {
			this.optional = null;
		}

	}

	public StatementMatcher.Variable getTargetVar() {
		return chain.getLast().var;
	}

	// Takes a source plan node and for every entry it extends the target chain with all targets that follow.
	// If the target chain is [type foaf:Person / foaf:knows ] and [ex:Peter] is in the source, this will effectively
	// retrieve all "ex:Peter foaf:knows ?extension"
	public PlanNode extend(PlanNode source, ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			ConstraintComponent.Scope scope,
			Extend direction, boolean includePropertyShapeValues, Function<PlanNode, PlanNode> filter) {

		if (source instanceof AllTargetsPlanNode && !includePropertyShapeValues) {
			PlanNode allTargets = getAllTargets(connectionsGroup, dataGraph, scope);
			if (filter != null) {
				allTargets = filter.apply(allTargets);
			}
			return allTargets;
		}

		String query = getQuery(includePropertyShapeValues);
		List<StatementMatcher.Variable> vars = getVars();
		if (includePropertyShapeValues) {
			vars = new ArrayList<>(vars);
			vars.add(optional.var);
		}

		List<String> varNames = vars.stream().map(StatementMatcher.Variable::getName).collect(Collectors.toList());

		if (varNames.size() == 1) {

			PlanNode parent = new TupleMapper(source,
					new ActiveTargetTupleMapper(scope, includePropertyShapeValues, dataGraph));

			if (filter != null) {
				parent = filter.apply(parent);
			}

			return connectionsGroup
					.getCachedNodeFor(getTargetFilter(connectionsGroup, dataGraph, Unique.getInstance(parent, false)));
		} else {

			PlanNode parent = new BindSelect(connectionsGroup.getBaseConnection(), dataGraph, query, vars, source,
					varNames, scope,
					1000, direction, includePropertyShapeValues);

			if (filter != null) {
				parent = connectionsGroup.getCachedNodeFor(parent);
				parent = filter.apply(parent);
				parent = Unique.getInstance(parent, true);
				return parent;
			} else {
				return connectionsGroup.getCachedNodeFor(
						Unique.getInstance(parent, true));
			}

		}
	}

	private List<StatementMatcher.Variable> getVars() {
		return chain.stream().map(t -> t.var).collect(Collectors.toList());
	}

	/**
	 *
	 * @return false if it is 100% sure that this will not match, else returns true
	 */
	public boolean couldMatch(ConnectionsGroup connectionsGroup, Resource[] dataGraph) {

		boolean hasTargetNode = Stream.concat(chain.stream(), getOptionalAsStream())
				.anyMatch(e -> e.target instanceof TargetNode);
		if (hasTargetNode) {
			return true;
		}

		return Stream.concat(chain.stream(), getOptionalAsStream())
				.flatMap(EffectiveTargetObject::getStatementMatcher)
				.anyMatch(
						currentStatementPattern -> connectionsGroup.getAddedStatements()
								.hasStatement(currentStatementPattern.getSubjectValue(),
										currentStatementPattern.getPredicateValue(),
										currentStatementPattern.getObjectValue(), false, dataGraph)
								|| connectionsGroup.getRemovedStatements()
										.hasStatement(currentStatementPattern.getSubjectValue(),
												currentStatementPattern.getPredicateValue(),
												currentStatementPattern.getObjectValue(), false, dataGraph)

				);

	}

	private Stream<EffectiveTargetObject> getOptionalAsStream() {
		Stream<EffectiveTargetObject> optional;
		if (this.optional != null) {
			optional = Stream.of(this.optional);
		} else {
			optional = Stream.empty();
		}
		return optional;
	}

	public PlanNode getAllTargets(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			ConstraintComponent.Scope scope) {
		return new AllTargetsPlanNode(connectionsGroup, dataGraph, chain, getVars(), scope);
	}

	public PlanNode getPlanNode(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			ConstraintComponent.Scope scope,
			boolean includeTargetsAffectedByRemoval, Function<PlanNode, PlanNode> filter) {
		assert !chain.isEmpty();

		includeTargetsAffectedByRemoval = includeTargetsAffectedByRemoval && connectionsGroup.getStats().hasRemoved();

		if (chain.size() == 1 && !(includeTargetsAffectedByRemoval && optional != null)) {
			// simple chain

			EffectiveTargetObject last = chain.getLast();
			if (last.target instanceof Target) {

				if (filter != null) {
					return filter.apply(
							connectionsGroup
									.getCachedNodeFor(((Target) last.target)
											.getAdded(connectionsGroup, dataGraph, scope)));
				} else {
					return connectionsGroup
							.getCachedNodeFor(((Target) last.target)
									.getAdded(connectionsGroup, dataGraph, scope));
				}

			} else {
				throw new ShaclUnsupportedException(
						"Unknown target in chain is type: " + last.getClass().getSimpleName());
			}

		} else {
			// complex chain

			List<StatementMatcher> statementMatchers = chain.stream()
					.flatMap(EffectiveTargetObject::getStatementMatcher)
					.collect(Collectors.toList());

			String query = chain.stream()
					.map(EffectiveTargetObject::getQueryFragment)
					.reduce((a, b) -> a + "\n" + b)
					.orElse("");

			List<StatementMatcher> statementMatchersRemoval = optional != null
					? optional.getStatementMatcher().collect(Collectors.toCollection(ArrayList::new))
					: new ArrayList<>();

			if (chain.getFirst().target instanceof RSXTargetShape) {
				statementMatchersRemoval.addAll(chain.getFirst().getStatementMatcher().collect(Collectors.toList()));
				includeTargetsAffectedByRemoval = true;
			}

			TargetChainRetriever targetChainRetriever;
			if (includeTargetsAffectedByRemoval) {
				targetChainRetriever = new TargetChainRetriever(
						connectionsGroup,
						dataGraph, statementMatchers,
						statementMatchersRemoval,
						query,
						getVars(),
						scope
				);
			} else {
				targetChainRetriever = new TargetChainRetriever(
						connectionsGroup,
						dataGraph, statementMatchers,
						null,
						query,
						getVars(),
						scope
				);
			}

			if (filter != null) {
				return connectionsGroup.getCachedNodeFor(Unique.getInstance(filter.apply(targetChainRetriever), true));
			} else {
				return connectionsGroup.getCachedNodeFor(Unique.getInstance(targetChainRetriever, true));
			}

		}

	}

	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			PlanNode parent) {

		EffectiveTargetObject last = chain.getLast();

		if (chain.size() == 1) {
			// simple chain

			if (last.target instanceof Target) {
				return ((Target) last.target).getTargetFilter(connectionsGroup, dataGraph, parent);
			} else {
				throw new ShaclUnsupportedException(
						"Unknown target in chain is type: " + last.getClass().getSimpleName());
			}
		}

		String query = chain.stream()
				.map(EffectiveTargetObject::getQueryFragment)
				.reduce((a, b) -> a + "\n" + b)
				.orElse("");

		// TODO: this is a slow way to solve this problem! We should use bulk operations.
		return new ExternalFilterByQuery(connectionsGroup.getBaseConnection(), dataGraph, parent, query, last.var,
				validationTuple -> validationTuple.getActiveTarget())
						.getTrueNode(UnBufferedPlanNode.class);
	}

	public String getQuery(boolean includeOptional) {

		ArrayDeque<EffectiveTargetObject> chain;

		if (includeOptional) {
			chain = new ArrayDeque<>(this.chain);
			chain.addLast(optional);
		} else {
			chain = this.chain;
		}

		return chain.stream()
				.map(EffectiveTargetObject::getQueryFragment)
				.reduce((a, b) -> a + "\n" + b)
				.orElse("") + "\n";

	}

	public List<StatementMatcher.Variable> getAllTargetVariables() {
		return chain.stream()
				.map(c -> c.var)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	public enum Extend {
		left,
		right
	}

	public static class EffectiveTargetObject {

		private final StatementMatcher.Variable var;
		private final Targetable target;
		private final EffectiveTargetObject prev;
		private final RdfsSubClassOfReasoner rdfsSubClassOfReasoner;
		private final StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider;

		public EffectiveTargetObject(StatementMatcher.Variable var, Targetable target, EffectiveTargetObject prev,
				RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
				StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
			this.var = var;
			this.target = target;
			this.prev = prev;
			this.rdfsSubClassOfReasoner = rdfsSubClassOfReasoner;
			this.stableRandomVariableProvider = stableRandomVariableProvider;
		}

		public Stream<StatementMatcher> getStatementMatcher() {
			if (prev == null) {
				return target.getStatementMatcher(null, var, rdfsSubClassOfReasoner);
			} else {
				return target.getStatementMatcher(prev.var, var, rdfsSubClassOfReasoner);
			}
		}

		public String getQueryFragment() {
			if (prev == null) {
				return target.getTargetQueryFragment(null, var, rdfsSubClassOfReasoner, stableRandomVariableProvider);
			} else {
				return target.getTargetQueryFragment(prev.var, var, rdfsSubClassOfReasoner,
						stableRandomVariableProvider);
			}
		}
	}

	static class ActiveTargetTupleMapper implements Function<ValidationTuple, ValidationTuple> {
		private final ConstraintComponent.Scope scope;
		private final boolean includePropertyShapeValues;
		private final Resource[] contexts;

		public ActiveTargetTupleMapper(ConstraintComponent.Scope scope, boolean includePropertyShapeValues,
				Resource[] contexts) {
			this.scope = scope;
			this.includePropertyShapeValues = includePropertyShapeValues;
			this.contexts = contexts;
		}

		@Override
		public ValidationTuple apply(ValidationTuple validationTuple) {
			return new ValidationTuple(validationTuple.getActiveTarget(), scope, includePropertyShapeValues, contexts);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ActiveTargetTupleMapper that = (ActiveTargetTupleMapper) o;
			return includePropertyShapeValues == that.includePropertyShapeValues && scope == that.scope;
		}

		@Override
		public int hashCode() {
			return Objects.hash(scope, includePropertyShapeValues);
		}
	}

}
