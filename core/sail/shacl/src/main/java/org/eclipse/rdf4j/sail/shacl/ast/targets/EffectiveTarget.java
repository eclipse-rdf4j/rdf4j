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

package org.eclipse.rdf4j.sail.shacl.ast.targets;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher.Variable;
import org.eclipse.rdf4j.sail.shacl.ast.Targetable;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.paths.InversePath;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.paths.SimplePath;
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
	public static final String[] TARGET_NAMES = IntStream.range(0, 1000)
			.mapToObj(i -> TARGET_VAR_PREFIX + String.format("%010d", i))
			.toArray(String[]::new);
	private final ArrayDeque<EffectiveTargetFragment> chain;
	private final EffectiveTargetFragment optional;

	public EffectiveTarget(ArrayDeque<Targetable> chain, Targetable optional,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
		int index = 0;

		this.chain = new ArrayDeque<>();

		EffectiveTargetFragment previous = null;

		for (Targetable targetable : chain) {
			EffectiveTargetFragment effectiveTargetFragment = new EffectiveTargetFragment(
					new Variable<>(getTargetVarName(index++)),
					targetable,
					previous,
					rdfsSubClassOfReasoner,
					stableRandomVariableProvider);
			previous = effectiveTargetFragment;
			this.chain.addLast(effectiveTargetFragment);
		}

		if (optional != null) {
			this.optional = new EffectiveTargetFragment(
					new StatementMatcher.Variable(getTargetVarName(index)),
					optional,
					previous,
					rdfsSubClassOfReasoner,
					stableRandomVariableProvider);
		} else {
			this.optional = null;
		}

	}

	private String getTargetVarName(int i) {
		if (i < TARGET_NAMES.length) {
			return TARGET_NAMES[i];
		} else {
			return TARGET_VAR_PREFIX + String.format("%010d", i);
		}
	}

	public Variable<Value> getTargetVar() {
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

		var vars = getVars();
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
			SparqlFragment query = getQueryFragment(includePropertyShapeValues);

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

	private List<Variable<Value>> getVars() {
		return chain.stream().map(t -> t.var).collect(Collectors.toList());
	}

	/**
	 * @return false if it is 100% sure that this will not match, else returns true
	 */
	public boolean couldMatch(ConnectionsGroup connectionsGroup, Resource[] dataGraph) {

		if (optional != null && optional.target instanceof TargetNode) {
			return true;
		}

		for (EffectiveTargetFragment e : chain) {
			if (e.target instanceof TargetNode) {
				return true;
			}
			if (!e.getQueryFragment().supportsIncrementalEvaluation()) {
				return true;
			}
		}

		SailConnection addedStatements = connectionsGroup.getAddedStatements();
		SailConnection removedStatements = connectionsGroup.getRemovedStatements();

		if (optional != null) {
			List<StatementMatcher> statementMatchers = optional.getQueryFragment().getStatementMatchers();
			for (StatementMatcher currentStatementPattern : statementMatchers) {
				boolean match = addedStatements
						.hasStatement(currentStatementPattern.getSubjectValue(),
								currentStatementPattern.getPredicateValue(),
								currentStatementPattern.getObjectValue(), false, dataGraph)
						|| removedStatements
								.hasStatement(currentStatementPattern.getSubjectValue(),
										currentStatementPattern.getPredicateValue(),
										currentStatementPattern.getObjectValue(), false, dataGraph);

				if (match) {
					return true;
				}
			}
		}

		for (EffectiveTargetFragment effectiveTargetFragment : chain) {
			List<StatementMatcher> statementMatchers = effectiveTargetFragment.getQueryFragment()
					.getStatementMatchers();
			for (StatementMatcher currentStatementPattern : statementMatchers) {
				boolean match = addedStatements
						.hasStatement(currentStatementPattern.getSubjectValue(),
								currentStatementPattern.getPredicateValue(),
								currentStatementPattern.getObjectValue(), false, dataGraph)
						|| removedStatements
								.hasStatement(currentStatementPattern.getSubjectValue(),
										currentStatementPattern.getPredicateValue(),
										currentStatementPattern.getObjectValue(), false, dataGraph);

				if (match) {
					return true;
				}

			}
		}

		return false;
	}

	public PlanNode getAllTargets(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			ConstraintComponent.Scope scope) {
		return new AllTargetsPlanNode(connectionsGroup.getBaseConnection(), dataGraph, chain, getVars(), scope);
	}

	public PlanNode getPlanNode(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			ConstraintComponent.Scope scope,
			boolean includeTargetsAffectedByRemoval, Function<PlanNode, PlanNode> filter) {
		assert !chain.isEmpty();

		includeTargetsAffectedByRemoval = includeTargetsAffectedByRemoval && connectionsGroup.getStats().hasRemoved();

		if (chain.size() == 1 && !(includeTargetsAffectedByRemoval && optional != null)) {
			// simple chain

			EffectiveTargetFragment last = chain.getLast();
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

			List<SparqlFragment> sparqlFragments = chain.stream()
					.map(EffectiveTargetFragment::getQueryFragment)
					.collect(Collectors.toList());

			SparqlFragment fragment = SparqlFragment.join(sparqlFragments);

			PlanNode targetsPlanNode;

			if (fragment.supportsIncrementalEvaluation()) {

				List<StatementMatcher> statementMatchers = sparqlFragments.stream()
						.flatMap(sparqlFragment -> sparqlFragment.getStatementMatchers().stream())
						.collect(Collectors.toList());

				List<StatementMatcher> statementMatchersRemoval = optional != null
						? optional.getQueryFragment().getStatementMatchers()
						: new ArrayList<>();

				if (chain.getFirst().target instanceof RSXTargetShape) {
					statementMatchersRemoval.addAll(sparqlFragments.get(0).getStatementMatchers());
					includeTargetsAffectedByRemoval = true;
				}

				if (includeTargetsAffectedByRemoval) {
					targetsPlanNode = new TargetChainRetriever(
							connectionsGroup,
							dataGraph,
							statementMatchers,
							statementMatchersRemoval,
							optional,
							fragment,
							getVars(),
							scope,
							false);
				} else {
					targetsPlanNode = new TargetChainRetriever(
							connectionsGroup,
							dataGraph,
							statementMatchers,
							null,
							null,
							fragment,
							getVars(),
							scope,
							false);
				}
			} else {

				targetsPlanNode = new AllTargetsPlanNode(connectionsGroup.getBaseConnection(), dataGraph, chain,
						getVars(), scope);

			}

			if (filter != null) {
				return connectionsGroup.getCachedNodeFor(Unique.getInstance(filter.apply(targetsPlanNode), true));
			} else {
				return connectionsGroup.getCachedNodeFor(Unique.getInstance(targetsPlanNode, true));
			}

		}

	}

	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			PlanNode parent) {

		EffectiveTargetFragment last = chain.getLast();

		if (chain.size() == 1) {
			// simple chain

			if (last.target instanceof Target) {
				return ((Target) last.target).getTargetFilter(connectionsGroup, dataGraph, parent);
			} else {
				throw new ShaclUnsupportedException(
						"Unknown target in chain is type: " + last.getClass().getSimpleName());
			}
		}

		List<SparqlFragment> collect = chain.stream()
				.map(EffectiveTargetFragment::getQueryFragment)
				.collect(Collectors.toList());

		SparqlFragment sparqlFragment = SparqlFragment.join(collect);

		// TODO: this is a slow way to solve this problem! We should use bulk operations.
		return new ExternalFilterByQuery(connectionsGroup.getBaseConnection(), dataGraph, parent, sparqlFragment,
				last.var,
				ValidationTuple::getActiveTarget)
				.getTrueNode(UnBufferedPlanNode.class);
	}

	public String getQuery(boolean includeOptional) {

		ArrayDeque<EffectiveTargetFragment> chain;

		if (includeOptional) {
			chain = new ArrayDeque<>(this.chain);
			chain.addLast(optional);
		} else {
			chain = this.chain;
		}

		return chain.stream()
				.map(EffectiveTargetFragment::getQueryFragment)
				.map(SparqlFragment::getFragment)
				.reduce((a, b) -> a + "\n" + b)
				.orElse("");

	}

	public SparqlFragment getQueryFragment(boolean includeOptional) {

		ArrayDeque<EffectiveTargetFragment> chain;

		if (includeOptional) {
			chain = new ArrayDeque<>(this.chain);
			chain.addLast(optional);
		} else {
			chain = this.chain;
		}

		if (chain.size() == 1) {
			return chain.getFirst().getQueryFragment();
		}

		List<SparqlFragment> collect = chain.stream()
				.map(EffectiveTargetFragment::getQueryFragment)
				.collect(Collectors.toList());

		return SparqlFragment.join(collect);

	}

	public List<Variable<Value>> getAllTargetVariables() {
		return chain.stream()
				.map(c -> c.var)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	public enum Extend {
		left,
		right
	}

	public static class EffectiveTargetFragment {

		private final Variable<Value> var;
		private final Targetable target;
		private final SparqlFragment queryFragment;
		private final StatementMatcher rootStatementMatcher;
		private final EffectiveTargetFragment prev;

		public EffectiveTargetFragment(Variable<Value> var, Targetable target, EffectiveTargetFragment prev,
				RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
				StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider) {
			this.var = var;
			this.target = target;
			this.prev = prev;
			if (prev == null) {
				this.queryFragment = this.target.getTargetQueryFragment(null, this.var, rdfsSubClassOfReasoner,
						stableRandomVariableProvider, Set.of());
			} else {
				this.queryFragment = this.target.getTargetQueryFragment(prev.var, this.var, rdfsSubClassOfReasoner,
						stableRandomVariableProvider, Set.of());
			}

			List<StatementMatcher> statementMatchers = queryFragment.getStatementMatchers();

			if (statementMatchers.isEmpty()) {
				rootStatementMatcher = null;
			} else {
				rootStatementMatcher = queryFragment.getStatementMatchers().get(0);
				assert rootStatementMatcher.getSubjectName() == var.getName() ||
						rootStatementMatcher.getObjectName() == var.getName() ||
						rootStatementMatcher.getSubjectName() == prev.var.getName() ||
						rootStatementMatcher.getObjectName() == prev.var.getName();
			}
		}

		public SparqlFragment getQueryFragment() {
			return queryFragment;
		}

		public Stream<StatementsAndMatcher> getRoot(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
				StatementMatcher currentStatementMatcher,
				Statement currentStatement) {
			if (currentStatementMatcher == rootStatementMatcher) {
				return null;
			} else {
				assert prev != null;
				if (this.target instanceof SimplePath) {
					assert currentStatementMatcher.hasSubset(rootStatementMatcher) ||
							prev.var.getName().equals(currentStatementMatcher.getSubjectName()) ||
							prev.var.getName().equals(currentStatementMatcher.getObjectName());

					return null;
				}

				if (currentStatementMatcher.hasSubset(rootStatementMatcher)) {
					return null;
				}

				if (prev.var.getName().equals(currentStatementMatcher.getSubjectName())
						|| prev.var.getName().equals(currentStatementMatcher.getObjectName())) {
					return null;
				}

				assert !currentStatementMatcher.covers(rootStatementMatcher);

				if (target instanceof Path) {
					Path path = (Path) target;
					assert !(path instanceof InversePath);
					Stream<StatementsAndMatcher> root = queryFragment.getRoot(connectionsGroup, dataGraph, path,
							currentStatementMatcher,
							List.of(currentStatement));

					List<StatementsAndMatcher> collect = root.collect(Collectors.toList());

					return collect.stream();
				}

				throw new UnsupportedOperationException();
			}
		}
	}

	public static class StatementsAndMatcher {
		private final List<Statement> statements;
		private final StatementMatcher statementMatcher;

		// We should support some sort of stream instead, so that we can scale without keeping all the
		// intermediary statements in memeory! It's very hard to implement though since the list of statements is
		// iterated over several times in different branches, so we can't just pass in an iterator since it would be
		// consumed by one branch and then the other branch would only see an empty iterator.
		public StatementsAndMatcher(List<Statement> statements, StatementMatcher statementMatcher) {
			this.statements = statements;
			this.statementMatcher = statementMatcher;
		}

		public List<Statement> getStatements() {
			return statements;
		}

		public StatementMatcher getStatementMatcher() {
			return statementMatcher;
		}

		public boolean hasStatements() {
			return !statements.isEmpty();
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
