/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Lateral;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.StatementPatternVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.TupleExprs;

/**
 * A query optimizer that re-orders nested Joins.
 *
 * @author Arjohn Kampman
 * @author James Leigh
 */
public class QueryJoinOptimizer implements QueryOptimizer {

	/**
	 * When deciding if merge join is the correct approach we will compare the cardinality of the two join arguments, if
	 * one is bigger than the other by a factor of MERGE_JOIN_CARDINALITY_SIZE_DIFF_MULTIPLIER then we will not use
	 * merge join. As an example, if the limit is 10 and the left cardinality if 50 000 and the right cardinality is 500
	 * 000 then we will use merge join, but if it is 500 001 then we will not.
	 */
	@Experimental
	public static int MERGE_JOIN_CARDINALITY_SIZE_DIFF_MULTIPLIER = 10;

	@Experimental
	public static boolean USE_MERGE_JOIN_FOR_LAST_STATEMENT_PATTERNS_WHEN_CROSS_JOIN = true;

	private static final int FULL_PAIRWISE_START_LIMIT = 6;

	private static final double CARDINALITY_FLOOR = 5.0;

	private static final double MIN_BOUND_VAR_CARDINALITY_EXPONENT = 0.25;

	private static final int MAX_FOREIGN_VAR_FREQ_BONUS = 8;

	protected final EvaluationStatistics statistics;
	private final boolean trackResultSize;
	private final TripleSource tripleSource;

	public QueryJoinOptimizer(EvaluationStatistics statistics) {
		this(statistics, false, new EmptyTripleSource());
	}

	public QueryJoinOptimizer(EvaluationStatistics statistics, TripleSource tripleSource) {
		this(statistics, false, tripleSource);
	}

	public QueryJoinOptimizer(EvaluationStatistics statistics, boolean trackResultSize) {
		this(statistics, trackResultSize, new EmptyTripleSource());
	}

	public QueryJoinOptimizer(EvaluationStatistics statistics, boolean trackResultSize, TripleSource tripleSource) {
		this.statistics = statistics;
		this.trackResultSize = trackResultSize;
		this.tripleSource = tripleSource;
	}

	/**
	 * Applies generally applicable optimizations: path expressions are sorted from more to less specific.
	 *
	 * @param tupleExpr
	 */
	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		tupleExpr.visit(new JoinVisitor());
	}

	/**
	 * This can be extended by subclasses to allow for adjustments to the optimization process.
	 */
	@SuppressWarnings("InnerClassMayBeStatic")
	protected class JoinVisitor extends AbstractSimpleQueryModelVisitor<RuntimeException> {

		private Set<String> boundVars = new HashSet<>();
		private double currentHighestCost = 1;

		protected JoinVisitor() {
			super(trackResultSize);

		}

		@Override
		public void meet(LeftJoin leftJoin) {
			leftJoin.getLeftArg().visit(this);

			Set<String> origBoundVars = boundVars;
			try {
				boundVars = new HashSet<>(boundVars);
				boundVars.addAll(leftJoin.getLeftArg().getBindingNames());

				leftJoin.getRightArg().visit(this);
			} finally {
				boundVars = origBoundVars;
			}
		}

		@Override
		public void meet(Lateral lateral) {
			lateral.getLeftArg().visit(this);

			Set<String> origBoundVars = boundVars;
			try {
				boundVars = new HashSet<>(boundVars);
				boundVars.addAll(lateral.getRightInputBindingNames());

				lateral.getRightArg().visit(this);
			} finally {
				boundVars = origBoundVars;
			}
		}

		@Override
		public void meet(StatementPattern node) throws RuntimeException {
			node.setResultSizeEstimate(Math.max(statistics.getCardinality(node), node.getResultSizeEstimate()));
		}

		private void optimizePriorityJoin(Set<String> origBoundVars, TupleExpr join) {

			Set<String> saveBoundVars = boundVars;
			try {
				boundVars = new HashSet<>(origBoundVars);
				join.visit(this);
			} finally {
				boundVars = saveBoundVars;
			}
		}

		@Override
		public void meet(Join node) {
			if (containsLateral(node)) {
				node.visitChildren(this);
				return;
			}

			Set<String> origBoundVars = boundVars;
			double origCurrentHighestCost = currentHighestCost;
			try {
				boundVars = new HashSet<>(boundVars);
				currentHighestCost = 1;
				Set<String> scopeStartBoundVars = new HashSet<>(boundVars);

				// Recursively get the join arguments
				List<TupleExpr> joinArgs = getJoinArgs(node, new ArrayList<>());

				// get all extensions (BIND clause)
				List<TupleExpr> orderedExtensions = getExtensionTupleExprs(joinArgs);
				optimizeInNewScope(orderedExtensions);
				joinArgs.removeAll(orderedExtensions);

				// get all subselects and order them
				List<TupleExpr> subSelects = getSubSelects(joinArgs);
				optimizeInNewScope(subSelects);
				List<TupleExpr> orderedSubselects = reorderSubselects(subSelects);
				joinArgs.removeAll(orderedSubselects);

				// Reorder the subselects and extensions to a more optimal sequence
				List<TupleExpr> priorityArgs;
				if (orderedExtensions.isEmpty()) {
					priorityArgs = orderedSubselects;
				} else if (orderedSubselects.isEmpty()) {
					priorityArgs = orderedExtensions;
				} else {
					priorityArgs = new ArrayList<>(orderedExtensions.size() + orderedSubselects.size());
					priorityArgs.addAll(orderedExtensions);
					priorityArgs.addAll(orderedSubselects);
				}

				// VALUES clauses are cheap and often selective, but placing all of them first can destroy locality in the
				// chosen join order. Order the real data-access expressions first, then insert each VALUES clause as close
				// as possible to the first expression that can use its bindings.
				List<TupleExpr> bindingSetAssignments = getBindingSetAssignments(joinArgs);
				joinArgs.removeAll(bindingSetAssignments);

				// Reorder the (recursive) join arguments to a more optimal sequence
				Deque<TupleExpr> orderedJoinArgs = new ArrayDeque<>(joinArgs.size());

				// We order all remaining join arguments based on cardinality and
				// variable frequency statistics
				if (!joinArgs.isEmpty()) {
					// Build maps of cardinalities and vars per tuple expression
					Map<TupleExpr, Double> cardinalityMap = Collections.emptyMap();
					Map<TupleExpr, List<Var>> varsMap = new HashMap<>();

					for (TupleExpr tupleExpr : joinArgs) {
						if (tupleExpr instanceof Join) {
							// we can skip calculating the cardinality for instances of Join since we will anyway "meet"
							// these nodes
							continue;
						}

						double cardinality = statistics.getCardinality(tupleExpr);

						tupleExpr.setResultSizeEstimate(Math.max(cardinality, tupleExpr.getResultSizeEstimate()));
						if (!hasCachedCardinality(tupleExpr)) {
							if (cardinalityMap.isEmpty()) {
								cardinalityMap = new HashMap<>();
							}
							cardinalityMap.put(tupleExpr, cardinality);
						}
						if (tupleExpr instanceof ZeroLengthPath) {
							varsMap.put(tupleExpr, ((ZeroLengthPath) tupleExpr).getVarList());
						} else {
							varsMap.put(tupleExpr, getStatementPatternVars(tupleExpr));
						}
					}

					// Build map of var frequences
					Map<Var, Integer> varFreqMap = new HashMap<>((varsMap.size() + 1) * 2);
					for (List<Var> varList : varsMap.values()) {
						fillVarFreqMap(varList, varFreqMap);
					}

					// order all other join arguments based on available statistics
					while (!joinArgs.isEmpty()) {
						TupleExpr tupleExpr = selectNextTupleExpr(joinArgs, cardinalityMap, varsMap, varFreqMap);
						updateCurrentHighestCost(tupleExpr);

						joinArgs.remove(tupleExpr);
						orderedJoinArgs.addLast(tupleExpr);

						// Recursively optimize join arguments
						tupleExpr.visit(this);

						boundVars.addAll(tupleExpr.getBindingNames());
					}
				}

				if (statistics.supportsJoinEstimation() && orderedJoinArgs.size() > 2) {
					orderedJoinArgs = reorderJoinArgs(orderedJoinArgs, scopeStartBoundVars);
				}

				if (!bindingSetAssignments.isEmpty()) {
					priorityArgs = new ArrayList<>(priorityArgs);
					orderedJoinArgs = placeBindingSetAssignments(priorityArgs, orderedJoinArgs, bindingSetAssignments,
							scopeStartBoundVars);
				}

				// Build new join hierarchy
				TupleExpr priorityJoins = null;
				if (!priorityArgs.isEmpty()) {
					priorityJoins = priorityArgs.getFirst();

					for (int i = 1; i < priorityArgs.size(); i++) {
						priorityJoins = new Join(priorityJoins, priorityArgs.get(i));
					}
				}

				if (priorityJoins == null && !orderedJoinArgs.isEmpty()) {

					double cardinality = 0;

					while (orderedJoinArgs.size() > 1) {

						Set<Var> supportedOrders = orderedJoinArgs.peekFirst().getSupportedOrders(tripleSource);
						if (supportedOrders.isEmpty()) {
							break;
						}

						TupleExpr left = orderedJoinArgs.removeFirst();
						TupleExpr right = orderedJoinArgs.removeFirst();

						supportedOrders = new HashSet<>(supportedOrders);
						supportedOrders.retainAll(right.getSupportedOrders(tripleSource));

						if (supportedOrders.isEmpty() || joinOnMultipleVars(left, right) || joinSizeIsTooDifferent(
								Math.max(cardinality, left.getResultSizeEstimate()), right.getResultSizeEstimate())) {

							orderedJoinArgs.addFirst(right);
							orderedJoinArgs.addFirst(left);
							break;

						} else {
							cardinality = Math.max(cardinality, left.getResultSizeEstimate());
							cardinality = Math.max(cardinality, right.getResultSizeEstimate());
							Join join = new Join(left, right);
							join.setOrder((Var) supportedOrders.toArray()[0]);
							join.setMergeJoin(true);
							orderedJoinArgs.addFirst(join);
						}

					}

				}

				if (!orderedJoinArgs.isEmpty()) {
					// Note: generated hierarchy is right-recursive to help the
					// IterativeEvaluationOptimizer to factor out the left-most join
					// argument
					TupleExpr right = orderedJoinArgs.removeLast();
					if (!orderedJoinArgs.isEmpty()) {
						TupleExpr left = orderedJoinArgs.removeLast();

						Set<Var> supportedOrders = new HashSet<>(left.getSupportedOrders(tripleSource));
						supportedOrders.retainAll(right.getSupportedOrders(tripleSource));

						Join join = new Join(left, right);

						if (USE_MERGE_JOIN_FOR_LAST_STATEMENT_PATTERNS_WHEN_CROSS_JOIN) {
							mergeJoinForCrossJoin(orderedJoinArgs, supportedOrders, left, right, join);
						}

						right = join;

					}
					while (!orderedJoinArgs.isEmpty()) {
						right = new Join(orderedJoinArgs.removeLast(), right);
					}

					if (priorityJoins != null) {
						right = new Join(priorityJoins, right);
					}

					// Replace old join hierarchy
					node.replaceWith(right);

					// we optimize after the right call above in case the optimize call below
					// recurses back into this function and we need all the node's parent/child pointers
					// set up correctly for right to work on subsequent calls
					if (priorityJoins != null) {
						optimizePriorityJoin(origBoundVars, priorityJoins);
					}

				} else {
					// only subselect/priority joins involved in this query.
					node.replaceWith(priorityJoins);
				}
			} finally {
				boundVars = origBoundVars;
				currentHighestCost = origCurrentHighestCost;
			}
		}

		private boolean containsLateral(TupleExpr tupleExpr) {
			LateralFinder finder = new LateralFinder();
			tupleExpr.visit(finder);
			return finder.found;
		}

		private class LateralFinder extends AbstractSimpleQueryModelVisitor<RuntimeException> {

			private boolean found;

			private LateralFinder() {
				super(false);
			}

			@Override
			public void meet(Lateral node) {
				found = true;
			}
		}

		/**
		 * This can be used by the upcoming sketch based estimator to reorder joins based on estimated join cost.
		 *
		 * @param orderedJoinArgs
		 * @return
		 */
		private Deque<TupleExpr> reorderJoinArgs(Deque<TupleExpr> orderedJoinArgs, Set<String> scopeStartBoundVars) {
			// Copy input into a mutable list
			List<TupleExpr> tupleExprs = new ArrayList<>(orderedJoinArgs);
			Deque<TupleExpr> ret = new ArrayDeque<>();

			// Memo table: for each (a, b), stores statistics.getCardinality(new Join(a,b))
			Map<TupleExpr, Map<TupleExpr, Double>> cardCache = new HashMap<>();

			// Helper to look up or compute & cache the cardinality of Join(a,b).
			// Avoid mutating the outer cache inside a computeIfAbsent lambda to prevent
			// ConcurrentModificationException on some Map implementations/JDKs.
			BiFunction<TupleExpr, TupleExpr, Double> getCard = (a, b) -> {
				Map<TupleExpr, Double> inner = cardCache.computeIfAbsent(a, k -> new HashMap<>());
				Double cached = inner.get(b);
				if (cached != null) {
					return cached;
				}
				double c = statistics.getCardinality(new Join(a, b));
				inner.put(b, c);
				cardCache.computeIfAbsent(b, k -> new HashMap<>()).put(a, c);
				return c;
			};

			while (!tupleExprs.isEmpty()) {
				if (ret.isEmpty()) {
					TupleExpr bestStart = selectBestStartingExpr(tupleExprs, getCard, scopeStartBoundVars);
					if (bestStart != null) {
						tupleExprs.remove(bestStart);
						ret.addLast(bestStart);
						continue;
					}
				}

				// If ret is empty or next isn’t a StatementPattern, just drain in original order
				if (ret.isEmpty() || !(tupleExprs.getFirst() instanceof StatementPattern)) {
					ret.addLast(tupleExprs.removeFirst());
					continue;
				}

				// Find the tupleExpr in tupleExprs whose join with any in ret has minimal cardinality. Prefer candidates
				// connected to the already chosen prefix; a zero/low estimate for an unrelated cross product should not beat
				// a genuinely connected join.
				boolean connectedCandidateExists = hasSharedStatementPatternWithAny(tupleExprs, ret);
				TupleExpr bestCandidate = null;
				double bestCost = Double.MAX_VALUE;
				for (TupleExpr cand : tupleExprs) {
					if (!statementPatternWithMinimumOneConstant(cand)) {
						continue;
					}

					boolean candConnected = hasSharedVariableWithAny(cand, ret);
					if (connectedCandidateExists && !candConnected) {
						continue;
					}

					// compute the minimum join‐cost between cand and anything in ret
					for (TupleExpr prev : ret) {
						if (!statementPatternWithMinimumOneConstant(prev)) {
							continue;
						}
						if (connectedCandidateExists && !hasSharedVariable(prev, cand)) {
							continue;
						}
						double cost = getCard.apply(prev, cand);
						if (cost < bestCost) {
							bestCost = cost;
							bestCandidate = cand;
						}
					}
				}

				// If we found a cheap StatementPattern, pick it; otherwise just take the head
				if (bestCandidate != null) {
					tupleExprs.remove(bestCandidate);
					ret.addLast(bestCandidate);
				} else {
					ret.addLast(tupleExprs.removeFirst());
				}
			}

			return ret;
		}

		private TupleExpr selectBestStartingExpr(List<TupleExpr> tupleExprs,
		                                         BiFunction<TupleExpr, TupleExpr, Double> getCard, Set<String> scopeStartBoundVars) {
			List<TupleExpr> candidates = new ArrayList<>();
			for (TupleExpr tupleExpr : tupleExprs) {
				if (statementPatternWithMinimumOneConstant(tupleExpr)) {
					candidates.add(tupleExpr);
				}
			}

			List<TupleExpr> runtimeBoundCandidates = getRuntimeBoundTupleExprs(candidates, scopeStartBoundVars);
			boolean restrictedToRuntimeBoundCandidates = !runtimeBoundCandidates.isEmpty();
			if (restrictedToRuntimeBoundCandidates) {
				candidates = runtimeBoundCandidates;
			}

			if (candidates.size() == 1 && restrictedToRuntimeBoundCandidates) {
				return candidates.getFirst();
			}

			if (candidates.size() < 2) {
				// we don't have multiple candidates, so there is nothing to compare against
				return null;
			}

			Map<TupleExpr, Double> singleCard = new HashMap<>(candidates.size());
			for (TupleExpr candidate : candidates) {
				singleCard.put(candidate, statistics.getCardinality(candidate));
			}

			List<TupleExpr> primary = new ArrayList<>(candidates);
			if (primary.size() > FULL_PAIRWISE_START_LIMIT) {
				primary.sort(Comparator.comparingDouble(singleCard::get));
				primary = new ArrayList<>(primary.subList(0, Math.min(3, primary.size())));
			}

			TupleExpr bestA = null;
			TupleExpr bestB = null;
			double bestCost = Double.MAX_VALUE;
			boolean connectedPairExists = hasSharedPair(primary, candidates);

			for (TupleExpr a : primary) {
				for (TupleExpr b : candidates) {
					if (a == b) {
						continue;
					}
					if (connectedPairExists && !hasSharedVariable(a, b)) {
						continue;
					}

					double cost = getCard.apply(a, b);
					if (cost < bestCost) {
						bestCost = cost;
						bestA = a;
						bestB = b;
					}
				}
			}

			if (bestA == null) {
				return null;
			}

			double cardA = singleCard.get(bestA);
			double cardB = singleCard.get(bestB);

			return cardA <= cardB ? bestA : bestB;
		}

		private void optimizeInNewScope(List<TupleExpr> subSelects) {
			for (TupleExpr subSelect : subSelects) {
				subSelect.visit(new JoinVisitor());
			}
		}

		private boolean joinSizeIsTooDifferent(double cardinality, double second) {
			if (cardinality > second && cardinality / MERGE_JOIN_CARDINALITY_SIZE_DIFF_MULTIPLIER > second) {
				return true;
			} else {
				return second > cardinality && second / MERGE_JOIN_CARDINALITY_SIZE_DIFF_MULTIPLIER > cardinality;
			}
		}

		private boolean joinOnMultipleVars(TupleExpr first, TupleExpr second) {
			Set<String> firstBindingNames = first.getBindingNames();
			if (firstBindingNames.size() == 1) {
				return false;
			}
			Set<String> secondBindingNames = second.getBindingNames();
			if (secondBindingNames.size() == 1) {
				return false;
			}
			int overlap = 0;
			for (String firstBindingName : firstBindingNames) {
				if (!firstBindingName.startsWith("_const_") && secondBindingNames.contains(firstBindingName)) {
					overlap++;
				}

				if (overlap > 1) {
					return true;
				}
			}

			return false;

		}

		private boolean hasSharedVariable(TupleExpr first, TupleExpr second) {
			Set<String> secondBindingNames = second.getBindingNames();
			return containsAnyNonConstant(first.getBindingNames(), secondBindingNames);
		}

		private boolean hasSharedVariable(TupleExpr tupleExpr, Set<String> bindingNames) {
			return containsAnyNonConstant(tupleExpr.getBindingNames(), bindingNames);
		}

		private boolean hasSharedVariableWithAny(TupleExpr tupleExpr, Iterable<TupleExpr> tupleExprs) {
			for (TupleExpr other : tupleExprs) {
				if (hasSharedVariable(tupleExpr, other)) {
					return true;
				}
			}
			return false;
		}

		private boolean hasSharedStatementPatternWithAny(List<TupleExpr> candidates, Iterable<TupleExpr> tupleExprs) {
			for (TupleExpr candidate : candidates) {
				if (statementPatternWithMinimumOneConstant(candidate) && hasSharedVariableWithAny(candidate, tupleExprs)) {
					return true;
				}
			}
			return false;
		}

		private boolean hasSharedPair(List<TupleExpr> primary, List<TupleExpr> candidates) {
			for (TupleExpr a : primary) {
				for (TupleExpr b : candidates) {
					if (a != b && hasSharedVariable(a, b)) {
						return true;
					}
				}
			}
			return false;
		}

		private boolean containsAnyNonConstant(Set<String> firstBindingNames, Set<String> secondBindingNames) {
			if (firstBindingNames.isEmpty() || secondBindingNames.isEmpty()) {
				return false;
			}

			for (String firstBindingName : firstBindingNames) {
				if (!firstBindingName.startsWith("_const_") && secondBindingNames.contains(firstBindingName)) {
					return true;
				}
			}
			return false;
		}

		protected <L extends List<TupleExpr>> L getJoinArgs(TupleExpr tupleExpr, L joinArgs) {
			if (tupleExpr instanceof Join join) {
				getJoinArgs(join.getLeftArg(), joinArgs);
				getJoinArgs(join.getRightArg(), joinArgs);
			} else {
				joinArgs.add(tupleExpr);
			}

			return joinArgs;
		}

		protected List<Var> getStatementPatternVars(TupleExpr tupleExpr) {
			if (tupleExpr instanceof StatementPattern) {
				return ((StatementPattern) tupleExpr).getVarList();
			}

			if (tupleExpr instanceof BindingSetAssignment) {
				return List.of();
			}

			return new StatementPatternVarCollector(tupleExpr).getVars();
		}

		protected <M extends Map<Var, Integer>> void fillVarFreqMap(List<Var> varList, M varFreqMap) {
			if (varList.isEmpty()) {
				return;
			}

			for (Var var : varList) {
				varFreqMap.compute(var, (k, v) -> {
					if (v == null) {
						return 1;
					}
					return v + 1;
				});
			}
		}

		private List<TupleExpr> getBindingSetAssignments(List<TupleExpr> expressions) {
			if (expressions.isEmpty()) {
				return List.of();
			}

			List<TupleExpr> bindingSetAssignments = List.of();
			for (TupleExpr expr : expressions) {
				if (expr instanceof BindingSetAssignment) {
					if (bindingSetAssignments.isEmpty()) {
						bindingSetAssignments = List.of(expr);
					} else {
						if (bindingSetAssignments.size() == 1) {
							bindingSetAssignments = new ArrayList<>(bindingSetAssignments);
						}
						bindingSetAssignments.add(expr);
					}
				}
			}
			return bindingSetAssignments;
		}

		private Deque<TupleExpr> placeBindingSetAssignments(List<TupleExpr> priorityArgs,
		                                                    Deque<TupleExpr> orderedJoinArgs, List<TupleExpr> bindingSetAssignments, Set<String> scopeStartBoundVars) {
			List<TupleExpr> originalPriorityArgs = new ArrayList<>(priorityArgs);
			List<TupleExpr> originalOrderedJoinArgs = new ArrayList<>(orderedJoinArgs);

			if (originalPriorityArgs.isEmpty() && originalOrderedJoinArgs.isEmpty()) {
				return new ArrayDeque<>(bindingSetAssignments);
			}

			Map<Integer, List<TupleExpr>> priorityInserts = new HashMap<>();
			Map<Integer, List<TupleExpr>> orderedInserts = new HashMap<>();
			List<TupleExpr> end = List.of();

			for (TupleExpr bindingSetAssignment : bindingSetAssignments) {
				int firstUseIndex = getFirstUseIndex(bindingSetAssignment, originalPriorityArgs, originalOrderedJoinArgs,
						scopeStartBoundVars);
				if (firstUseIndex < 0) {
					if (end.isEmpty()) {
						end = List.of(bindingSetAssignment);
					} else {
						if (end.size() == 1) {
							end = new ArrayList<>(end);
						}
						end.add(bindingSetAssignment);
					}
				} else if (firstUseIndex < originalPriorityArgs.size()) {
					priorityInserts.computeIfAbsent(firstUseIndex, k -> new ArrayList<>()).add(bindingSetAssignment);
				} else {
					int orderedIndex = firstUseIndex - originalPriorityArgs.size();
					orderedInserts.computeIfAbsent(orderedIndex, k -> new ArrayList<>()).add(bindingSetAssignment);
				}
			}

			priorityArgs.clear();
			for (int i = 0; i < originalPriorityArgs.size(); i++) {
				List<TupleExpr> insertBefore = priorityInserts.get(i);
				if (insertBefore != null) {
					priorityArgs.addAll(insertBefore);
				}
				priorityArgs.add(originalPriorityArgs.get(i));
			}

			Deque<TupleExpr> ordered = new ArrayDeque<>();
			for (int i = 0; i < originalOrderedJoinArgs.size(); i++) {
				List<TupleExpr> insertBefore = orderedInserts.get(i);
				if (insertBefore != null) {
					ordered.addAll(insertBefore);
				}
				ordered.addLast(originalOrderedJoinArgs.get(i));
			}
			ordered.addAll(end);

			return ordered;
		}

		private int getFirstUseIndex(TupleExpr bindingSetAssignment, List<TupleExpr> priorityArgs,
		                             List<TupleExpr> orderedJoinArgs, Set<String> scopeStartBoundVars) {
			Set<String> bindingNames = bindingSetAssignment.getBindingNames();
			if (bindingNames.isEmpty()) {
				return -1;
			}

			if (containsAnyNonConstant(bindingNames, scopeStartBoundVars)) {
				return 0;
			}

			int index = 0;
			for (TupleExpr priorityArg : priorityArgs) {
				if (containsAnyNonConstant(bindingNames, priorityArg.getBindingNames())) {
					return index;
				}
				index++;
			}

			for (TupleExpr orderedJoinArg : orderedJoinArgs) {
				if (containsAnyNonConstant(bindingNames, orderedJoinArg.getBindingNames())) {
					return index;
				}
				index++;
			}

			return -1;
		}

		private List<TupleExpr> getExtensionTupleExprs(List<TupleExpr> expressions) {
			if (expressions.isEmpty()) {
				return List.of();
			}

			List<TupleExpr> extensions = List.of();
			for (TupleExpr expr : expressions) {
				if (TupleExprs.containsExtension(expr)) {
					if (extensions.isEmpty()) {
						extensions = List.of(expr);
					} else {
						if (extensions.size() == 1) {
							extensions = new ArrayList<>(extensions);
						}
						extensions.add(expr);
					}
				}
			}
			return extensions;
		}

		/**
		 * This method returns all direct sub-selects in the given list of expressions.
		 * <p>
		 * This method is meant to be possible to override by subclasses.
		 *
		 * @param expressions
		 * @return
		 */
		protected List<TupleExpr> getSubSelects(List<TupleExpr> expressions) {
			if (expressions.isEmpty()) {
				return List.of();
			}

			List<TupleExpr> subselects = List.of();
			for (TupleExpr expr : expressions) {
				if (TupleExprs.containsSubquery(expr)) {
					if (subselects.isEmpty()) {
						subselects = List.of(expr);
					} else {
						if (subselects.size() == 1) {
							subselects = new ArrayList<>(subselects);
						}
						subselects.add(expr);
					}
				}
			}
			return subselects;
		}

		/**
		 * Determines an optimal ordering of subselect join arguments, based on variable bindings. An ordering is
		 * considered optimal if for each consecutive element it holds that first of all its shared variables with all
		 * previous elements is maximized, and second, the union of all its variables with all previous elements is
		 * maximized.
		 * <p>
		 * Example: reordering
		 *
		 * <pre>
		 *   [f] [a b c] [e f] [a d] [b e]
		 * </pre>
		 * <p>
		 * should result in:
		 *
		 * <pre>
		 *   [a b c] [a d] [b e] [e f] [f]
		 * </pre>
		 *
		 * @param subSelects the original ordering of expressions
		 * @return the optimized ordering of expressions
		 */
		protected List<TupleExpr> reorderSubselects(List<TupleExpr> subSelects) {

			if (subSelects.size() == 1) {
				return subSelects;
			}

			List<TupleExpr> result = new ArrayList<>();
			if (subSelects.isEmpty()) {
				return result;
			}

			// Step 1: determine size of join for each pair of arguments
			HashMap<Integer, List<TupleExpr[]>> joinSizes = new HashMap<>();

			int maxJoinSize = 0;
			for (int i = 0; i < subSelects.size(); i++) {
				TupleExpr firstArg = subSelects.get(i);
				for (int j = i + 1; j < subSelects.size(); j++) {
					TupleExpr secondArg = subSelects.get(j);

					int joinSize = getJoinSize(firstArg.getBindingNames(), secondArg.getBindingNames());

					if (joinSize > maxJoinSize) {
						maxJoinSize = joinSize;
					}

					List<TupleExpr[]> l;

					if (joinSizes.containsKey(joinSize)) {
						l = joinSizes.get(joinSize);
					} else {
						l = new ArrayList<>();
					}
					TupleExpr[] tupleTuple = new TupleExpr[] { firstArg, secondArg };
					l.add(tupleTuple);
					joinSizes.put(joinSize, l);
				}
			}

			// Step 2: find the first two elements for the ordered list by
			// selecting the pair with first of all,
			// the highest join size, and second, the highest union size.

			TupleExpr[] maxUnionTupleTuple = null;
			int currentUnionSize = -1;

			// get a list of all argument pairs with the maximum join size
			List<TupleExpr[]> list = joinSizes.get(maxJoinSize);

			// select the pair that has the highest union size.
			for (TupleExpr[] tupleTuple : list) {
				Set<String> names = tupleTuple[0].getBindingNames();
				names.addAll(tupleTuple[1].getBindingNames());
				int unionSize = names.size();

				if (unionSize > currentUnionSize) {
					maxUnionTupleTuple = tupleTuple;
					currentUnionSize = unionSize;
				}
			}

			// add the pair to the result list.
			assert maxUnionTupleTuple != null;
			result.add(maxUnionTupleTuple[0]);
			result.add(maxUnionTupleTuple[1]);

			// Step 3: sort the rest of the list by selecting and adding an element
			// at a time.
			while (result.size() < subSelects.size()) {
				result.add(getNextSubselect(result, subSelects));
			}

			return result;
		}

		private TupleExpr getNextSubselect(List<TupleExpr> currentList, List<TupleExpr> joinArgs) {

			// determine union of names of all elements currently in the list: this
			// corresponds to the projection resulting from joining all these
			// elements.
			Set<String> currentListNames = new HashSet<>();
			for (TupleExpr expr : currentList) {
				currentListNames.addAll(expr.getBindingNames());
			}

			// select the next argument from the list, by checking that it has,
			// first, the highest join size with the current list, and second, the
			// highest union size.
			TupleExpr selected = null;
			int currentUnionSize = -1;
			int currentJoinSize = -1;
			for (TupleExpr candidate : joinArgs) {
				if (!currentList.contains(candidate)) {

					Set<String> names = candidate.getBindingNames();
					int joinSize = getJoinSize(currentListNames, names);

					Set<String> candidateBindingNames = candidate.getBindingNames();
					int unionSize = getUnionSize(currentListNames, candidateBindingNames);

					if (joinSize > currentJoinSize) {
						selected = candidate;
						currentJoinSize = joinSize;
						currentUnionSize = unionSize;
					} else if (joinSize == currentJoinSize) {
						if (unionSize > currentUnionSize) {
							selected = candidate;
							currentUnionSize = unionSize;
						}
					}
				}
			}

			return selected;
		}

		/**
		 * Selects from a list of tuple expressions the next tuple expression that should be evaluated. This method
		 * selects the tuple expression with highest number of bound variables, preferring variables that have been
		 * bound in other tuple expressions over variables with a fixed value.
		 */
		protected TupleExpr selectNextTupleExpr(List<TupleExpr> expressions, Map<TupleExpr, Double> cardinalityMap,
		                                        Map<TupleExpr, List<Var>> varsMap, Map<Var, Integer> varFreqMap) {
			if (expressions.size() == 1) {
				TupleExpr tupleExpr = expressions.getFirst();
				tupleExpr.setCostEstimate(getTupleExprCost(tupleExpr, cardinalityMap, varsMap, varFreqMap));
				return tupleExpr;
			}

			List<TupleExpr> candidateExpressions = getRuntimeBoundTupleExprs(expressions, varsMap);
			if (candidateExpressions.isEmpty()) {
				candidateExpressions = expressions;
			}

			TupleExpr result = null;
			double lowestCost = Double.POSITIVE_INFINITY;

			for (TupleExpr tupleExpr : candidateExpressions) {
				// Calculate a score for this tuple expression
				double cost = getTupleExprCost(tupleExpr, cardinalityMap, varsMap, varFreqMap);

				if (cost < lowestCost || result == null) {
					// More specific path expression found
					lowestCost = cost;
					result = tupleExpr;
					if (cost == 0) {
						break;
					}
				}
			}

			assert result != null;
			result.setCostEstimate(lowestCost);

			return result;
		}

		private List<TupleExpr> getRuntimeBoundTupleExprs(List<TupleExpr> expressions, Map<TupleExpr, List<Var>> varsMap) {
			List<TupleExpr> result = List.of();
			for (TupleExpr expression : expressions) {
				if (hasRuntimeBoundVar(expression, varsMap.get(expression))) {
					if (result.isEmpty()) {
						result = List.of(expression);
					} else {
						if (result.size() == 1) {
							result = new ArrayList<>(result);
						}
						result.add(expression);
					}
				}
			}
			return result;
		}

		private List<TupleExpr> getRuntimeBoundTupleExprs(List<TupleExpr> expressions, Set<String> runtimeBoundVars) {
			List<TupleExpr> result = List.of();
			for (TupleExpr expression : expressions) {
				if (hasSharedVariable(expression, runtimeBoundVars)) {
					if (result.isEmpty()) {
						result = List.of(expression);
					} else {
						if (result.size() == 1) {
							result = new ArrayList<>(result);
						}
						result.add(expression);
					}
				}
			}
			return result;
		}

		private boolean hasRuntimeBoundVar(TupleExpr tupleExpr, List<Var> vars) {
			if (vars != null) {
				for (Var var : vars) {
					if (!var.hasValue() && var.getName() != null && boundVars.contains(var.getName())) {
						return true;
					}
				}
			}

			return hasSharedVariable(tupleExpr, boundVars);
		}

		protected double getTupleExprCost(TupleExpr tupleExpr, Map<TupleExpr, Double> cardinalityMap,
		                                  Map<TupleExpr, List<Var>> varsMap, Map<Var, Integer> varFreqMap) {

			// BindingSetAssignment represents VALUES. It is only forced to cost 0 when it filters/binds a variable that
			// is already available from an outer scope. Otherwise, VALUES placement is handled after join ordering so
			// several VALUES clauses do not all get pulled to the beginning of the query.
			if (tupleExpr instanceof BindingSetAssignment && hasSharedVariable(tupleExpr, boundVars)) {
				return 0;
			}

			double cost = getCardinalityCost(tupleExpr, cardinalityMap);
			List<Var> vars = varsMap.get(tupleExpr);

			if (vars == null || vars.isEmpty()) {
				return cost;
			}

			// Compensate for variables that are bound earlier in the evaluation. Count variables, not positions: repeated
			// occurrences such as ?x :p ?x should not be treated as two independent bound variables.
			List<Var> unboundVars = getUnboundVars(vars);
			List<Var> uniqueUnboundVars = getUniqueVars(unboundVars);
			int nonConstantVarCount = countUniqueNonConstantVars(vars);

			if (nonConstantVarCount > 0) {
				int boundVarCount = nonConstantVarCount - uniqueUnboundVars.size();
				if (boundVarCount == 0) {
					// Cartesian product: make the penalty depend on the current prefix, not just on this pattern.
					cost = cost * currentHighestCost;
				} else {
					// Bound variables make indexed lookups cheaper, but they should not erase the base cardinality entirely.
					// In particular, the old exponent of 0 for fully-bound patterns collapsed every such pattern to cost 1
					// regardless of whether its estimated cardinality was tiny or huge.
					double exp = (double) uniqueUnboundVars.size() / nonConstantVarCount;
					exp = Math.max(MIN_BOUND_VAR_CARDINALITY_EXPONENT, exp);
					cost = Math.pow(cost, exp);
				}
			}

			if (unboundVars.isEmpty()) {
				// Prefer filters/lookups that use more already-bound variables, but only mildly.
				if (nonConstantVarCount > 1) {
					cost /= Math.sqrt(nonConstantVarCount);
				}
			} else {
				// Prefer patterns that bind variables from other tuple expressions. Cap this syntactic bonus so it cannot
				// dominate orders-of-magnitude differences in estimated cardinality.
				int foreignVarFreq = getForeignVarFreq(unboundVars, varFreqMap);
				if (foreignVarFreq > 0) {
					cost /= 1 + Math.min(foreignVarFreq, MAX_FOREIGN_VAR_FREQ_BONUS);
				}
			}

			return Math.max(1, cost);
		}

		private double getCardinalityCost(TupleExpr tupleExpr, Map<TupleExpr, Double> cardinalityMap) {
			double cardinality;
			if (hasCachedCardinality(tupleExpr)) {
				cardinality = ((AbstractQueryModelNode) tupleExpr).getCardinality();
			} else {
				Double cached = cardinalityMap.get(tupleExpr);
				cardinality = cached != null ? cached : statistics.getCardinality(tupleExpr);
			}

			if (!Double.isFinite(cardinality) || cardinality < 0) {
				cardinality = 0;
			}

			// Adding a small floor allows us to order tuple expressions based on bound variables even when statistics
			// returns 0, which can happen for recently-added data or incomplete statistics.
			return cardinality + CARDINALITY_FLOOR;
		}

		private void updateCurrentHighestCost(TupleExpr tupleExpr) {
			double cost = tupleExpr.getCostEstimate();
			double cardinality = tupleExpr.getResultSizeEstimate();

			if (Double.isFinite(cardinality) && cardinality >= 0) {
				cost = Math.max(cost, cardinality + CARDINALITY_FLOOR);
			}

			if (Double.isFinite(cost) && cost > currentHighestCost) {
				currentHighestCost = cost;
			}
		}

		private int countUniqueNonConstantVars(List<Var> vars) {
			if (vars.isEmpty()) {
				return 0;
			}

			Set<Var> uniqueVars = new HashSet<>();
			for (Var var : vars) {
				if (!var.hasValue()) {
					uniqueVars.add(var);
				}
			}
			return uniqueVars.size();
		}

		private List<Var> getUniqueVars(List<Var> vars) {
			if (vars.size() < 2) {
				return vars;
			}

			List<Var> ret = null;
			Set<Var> seen = new HashSet<>();
			for (Var var : vars) {
				if (seen.add(var)) {
					if (ret == null) {
						ret = List.of(var);
					} else {
						if (ret.size() == 1) {
							ret = new ArrayList<>(ret);
						}
						ret.add(var);
					}
				}
			}
			return ret != null ? ret : Collections.emptyList();
		}

		protected List<Var> getUnboundVars(List<Var> vars) {
			int size = vars.size();
			if (size == 0) {
				return List.of();
			}
			if (size == 1) {
				Var var = vars.getFirst();
				if (!var.hasValue() && var.getName() != null && !boundVars.contains(var.getName())) {
					return List.of(var);
				} else {
					return List.of();
				}
			}

			List<Var> ret = null;

			for (Var var : vars) {
				if (!var.hasValue() && var.getName() != null && !boundVars.contains(var.getName())) {
					if (ret == null) {
						ret = List.of(var);
					} else {
						if (ret.size() == 1) {
							ret = new ArrayList<>(ret);
						}
						ret.add(var);
					}
				}
			}

			return ret != null ? ret : Collections.emptyList();
		}

		protected int getForeignVarFreq(List<Var> ownUnboundVars, Map<Var, Integer> varFreqMap) {
			if (ownUnboundVars.isEmpty()) {
				return 0;
			}
			if (ownUnboundVars.size() == 1) {
				return varFreqMap.get(ownUnboundVars.getFirst()) - 1;
			} else {
				int result = -ownUnboundVars.size();
				for (Var var : new HashSet<>(ownUnboundVars)) {
					result += varFreqMap.get(var);
				}
				return result;

			}
		}

		private void mergeJoinForCrossJoin(Deque<TupleExpr> orderedJoinArgs, Set<Var> supportedOrders, TupleExpr left,
		                                   TupleExpr right, Join join) {
			if (!orderedJoinArgs.isEmpty()
					&& !supportedOrders.isEmpty() && !joinOnMultipleVars(left, right)
					&& !joinSizeIsTooDifferent(left.getResultSizeEstimate(), right.getResultSizeEstimate())
					&& left instanceof StatementPattern && right instanceof StatementPattern) {

				HashSet<String> allBindingNamesAbove = new HashSet<>();

				for (TupleExpr orderedJoinArg : orderedJoinArgs) {
					allBindingNamesAbove.addAll(orderedJoinArg.getBindingNames());
				}

				if (!allBindingNamesAbove.isEmpty()) {

					// Check that none of the variables used in the join are used anywhere else, e.g. is this case that
					// join is the right arg of an effective cross join
					Set<String> joinBindingNames = join.getBindingNames();
					boolean crossJoin = true;
					for (String leftBindingName : joinBindingNames) {
						if (!leftBindingName.startsWith("_const_")
								&& allBindingNamesAbove.contains(leftBindingName)) {
							crossJoin = false;
							break;
						}
					}
					if (crossJoin) {
						join.setOrder((Var) supportedOrders.toArray()[0]);
						join.setMergeJoin(true);
						join.setCacheable(true);
					}
				}
			}
		}

		private class StatementPatternVarCollector extends StatementPatternVisitor {

			private final TupleExpr tupleExpr;
			private List<Var> vars;

			public StatementPatternVarCollector(TupleExpr tupleExpr) {
				this.tupleExpr = tupleExpr;
			}

			@Override
			protected void accept(StatementPattern node) {
				if (vars == null) {
					vars = new ArrayList<>(node.getVarList());
				} else {
					vars.addAll(node.getVarList());
				}
			}

			public List<Var> getVars() {
				if (vars == null) {
					try {
						tupleExpr.visit(this);
					} catch (Exception e) {
						if (e instanceof InterruptedException) {
							Thread.currentThread().interrupt();
						}
						throw new IllegalStateException(e);
					}
					if (vars == null) {
						vars = Collections.emptyList();
					}
				}

				return vars;
			}
		}

	}

	private static boolean statementPatternWithMinimumOneConstant(TupleExpr cand) {
		return cand instanceof StatementPattern && ((((StatementPattern) cand).getSubjectVar() != null
				&& ((StatementPattern) cand).getSubjectVar().hasValue())
				|| (((StatementPattern) cand).getPredicateVar() != null
				&& ((StatementPattern) cand).getPredicateVar().hasValue())
				|| (((StatementPattern) cand).getObjectVar() != null
				&& ((StatementPattern) cand).getObjectVar().hasValue())
				|| (((StatementPattern) cand).getContextVar() != null
				&& ((StatementPattern) cand).getContextVar().hasValue()));
	}

	private static int getUnionSize(Set<String> currentListNames, Set<String> candidateBindingNames) {
		int count = 0;
		for (String n : currentListNames) {
			if (!candidateBindingNames.contains(n)) {
				count++;
			}
		}
		return candidateBindingNames.size() + count;
	}

	private static int getJoinSize(Set<String> currentListNames, Set<String> names) {
		int count = 0;
		for (String name : names) {
			if (currentListNames.contains(name)) {
				count++;
			}
		}
		return count;
	}

	private static boolean hasCachedCardinality(TupleExpr tupleExpr) {
		return tupleExpr instanceof AbstractQueryModelNode
				&& ((AbstractQueryModelNode) tupleExpr).isCardinalitySet();
	}

	private static final class EmptyTripleSource implements TripleSource {

		@Override
		public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred, Value obj,
		                                                             Resource... contexts) throws QueryEvaluationException {
			return TripleSource.EMPTY_ITERATION;
		}

		@Override
		public Set<StatementOrder> getSupportedOrders(Resource subj, IRI pred, Value obj, Resource... contexts)
				throws QueryEvaluationException {
			return Set.of();
		}

		@Override
		public ValueFactory getValueFactory() {
			return null;
		}

		@Override
		public Comparator<Value> getComparator() {
			return null;
		}
	}

}
