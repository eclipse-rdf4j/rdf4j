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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
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

	protected final EvaluationStatistics statistics;
	private final boolean trackResultSize;

	public QueryJoinOptimizer(EvaluationStatistics statistics) {
		this(statistics, false);
	}

	public QueryJoinOptimizer(EvaluationStatistics statistics, boolean trackResultSize) {
		this.statistics = statistics;
		this.trackResultSize = trackResultSize;
	}

	/**
	 * Applies generally applicable optimizations: path expressions are sorted from more to less specific.
	 *
	 * @param tupleExpr
	 */
	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		tupleExpr.visit(new JoinVisitor(statistics, trackResultSize));
	}

	private static class JoinVisitor extends AbstractSimpleQueryModelVisitor<RuntimeException> {

		private final EvaluationStatistics statistics;
		Set<String> boundVars = new HashSet<>();

		protected JoinVisitor(EvaluationStatistics statistics, boolean trackResultSize) {
			super(trackResultSize);
			this.statistics = statistics;
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

			Set<String> origBoundVars = boundVars;
			try {
				boundVars = new HashSet<>(boundVars);

				// Recursively get the join arguments
				List<TupleExpr> joinArgs = getJoinArgs(node, new ArrayList<>());

				// get all extensions (BIND clause)
				List<TupleExpr> orderedExtensions = getExtensionTupleExprs(joinArgs);
				joinArgs.removeAll(orderedExtensions);

				// get all subselects and order them
				List<TupleExpr> orderedSubselects = reorderSubselects(getSubSelects(joinArgs));
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

				// Reorder the (recursive) join arguments to a more optimal sequence
				List<TupleExpr> orderedJoinArgs = new ArrayList<>(joinArgs.size());

				// We order all remaining join arguments based on cardinality and
				// variable frequency statistics
				if (joinArgs.size() > 0) {
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

						joinArgs.remove(tupleExpr);
						orderedJoinArgs.add(tupleExpr);

						// Recursively optimize join arguments
						tupleExpr.visit(this);

						boundVars.addAll(tupleExpr.getBindingNames());
					}
				}

				// Build new join hierarchy
				TupleExpr priorityJoins = null;
				if (priorityArgs.size() > 0) {
					priorityJoins = priorityArgs.get(0);
					for (int i = 1; i < priorityArgs.size(); i++) {
						priorityJoins = new Join(priorityJoins, priorityArgs.get(i));
					}
				}

				if (orderedJoinArgs.size() > 0) {
					// Note: generated hierarchy is right-recursive to help the
					// IterativeEvaluationOptimizer to factor out the left-most join
					// argument
					int i = orderedJoinArgs.size() - 1;
					TupleExpr replacement = orderedJoinArgs.get(i);
					for (i--; i >= 0; i--) {
						replacement = new Join(orderedJoinArgs.get(i), replacement);
					}

					if (priorityJoins != null) {
						replacement = new Join(priorityJoins, replacement);
					}

					// Replace old join hierarchy
					node.replaceWith(replacement);

					// we optimize after the replacement call above in case the optimize call below
					// recurses back into this function and we need all the node's parent/child pointers
					// set up correctly for replacement to work on subsequent calls
					if (priorityJoins != null) {
						optimizePriorityJoin(origBoundVars, priorityJoins);
					}

				} else {
					// only subselect/priority joins involved in this query.
					node.replaceWith(priorityJoins);
				}
			} finally {
				boundVars = origBoundVars;
			}
		}

		protected <L extends List<TupleExpr>> L getJoinArgs(TupleExpr tupleExpr, L joinArgs) {
			if (tupleExpr instanceof Join) {
				Join join = (Join) tupleExpr;
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

		protected List<Extension> getExtensions(List<TupleExpr> expressions) {
			List<Extension> extensions = new ArrayList<>();
			for (TupleExpr expr : expressions) {
				if (expr instanceof Extension) {
					extensions.add((Extension) expr);
				}
			}
			return extensions;
		}

		private List<TupleExpr> getExtensionTupleExprs(List<TupleExpr> expressions) {
			if (expressions.isEmpty())
				return List.of();

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

		protected List<TupleExpr> getSubSelects(List<TupleExpr> expressions) {
			if (expressions.isEmpty())
				return List.of();

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
				TupleExpr tupleExpr = expressions.get(0);
				if (tupleExpr.getCostEstimate() < 0) {
					tupleExpr.setCostEstimate(getTupleExprCost(tupleExpr, cardinalityMap, varsMap, varFreqMap));
				}
				return tupleExpr;
			}

			TupleExpr result = null;
			double lowestCost = Double.POSITIVE_INFINITY;

			for (TupleExpr tupleExpr : expressions) {
				// Calculate a score for this tuple expression
				double cost = getTupleExprCost(tupleExpr, cardinalityMap, varsMap, varFreqMap);

				if (cost < lowestCost || result == null) {
					// More specific path expression found
					lowestCost = cost;
					result = tupleExpr;
					if (cost == 0)
						break;
				}
			}

			assert result != null;
			result.setCostEstimate(lowestCost);

			return result;
		}

		protected double getTupleExprCost(TupleExpr tupleExpr, Map<TupleExpr, Double> cardinalityMap,
				Map<TupleExpr, List<Var>> varsMap, Map<Var, Integer> varFreqMap) {

			// BindingSetAssignment has a typical constant cost. This cost is not based on statistics so is much more
			// reliable. If the BindingSetAssignment binds to any of the other variables in the other tuple expressions
			// to choose from, then the cost of the BindingSetAssignment should be set to 0 since it will always limit
			// the upper bound of any other costs. This way the BindingSetAssignment will be chosen as the left
			// argument.
			if (tupleExpr instanceof BindingSetAssignment) {

				Set<Var> varsUsedInOtherExpressions = varFreqMap.keySet();

				for (String assuredBindingName : tupleExpr.getAssuredBindingNames()) {
					if (varsUsedInOtherExpressions.contains(new Var(assuredBindingName))) {
						return 0;
					}
				}
			}

			double cost;

			if (hasCachedCardinality(tupleExpr)) {
				cost = ((AbstractQueryModelNode) tupleExpr).getCardinality();
			} else {
				cost = cardinalityMap.get(tupleExpr);
			}

			List<Var> vars = varsMap.get(tupleExpr);

			// Compensate for variables that are bound earlier in the evaluation
			List<Var> unboundVars = getUnboundVars(vars);
			int constantVars = countConstantVars(vars);

			int nonConstantVarCount = vars.size() - constantVars;

			if (nonConstantVarCount > 0) {
				double exp = (double) unboundVars.size() / nonConstantVarCount;
				cost = Math.pow(cost, exp);
			}

			if (unboundVars.isEmpty()) {
				// Prefer patterns with more bound vars
				if (nonConstantVarCount > 0) {
					cost /= nonConstantVarCount;
				}
			} else {
				// Prefer patterns that bind variables from other tuple expressions
				int foreignVarFreq = getForeignVarFreq(unboundVars, varFreqMap);
				if (foreignVarFreq > 0) {
					cost /= 1 + foreignVarFreq;
				}
			}

			return cost;
		}

		private int countConstantVars(List<Var> vars) {
			int size = 0;

			for (Var var : vars) {
				if (var.hasValue()) {
					size++;
				}
			}

			return size;
		}

		@Deprecated(forRemoval = true, since = "4.1.0")
		protected List<Var> getUnboundVars(Iterable<Var> vars) {

			List<Var> ret = null;

			for (Var var : vars) {
				if (!var.hasValue() && var.getName() != null && !boundVars.contains(var.getName())) {
					if (ret == null) {
						ret = Collections.singletonList(var);
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
				Var var = vars.get(0);
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
				return varFreqMap.get(ownUnboundVars.get(0)) - 1;
			} else {
				int result = -ownUnboundVars.size();
				for (Var var : new HashSet<>(ownUnboundVars)) {
					result += varFreqMap.get(var);
				}
				return result;

			}
		}

		private static class StatementPatternVarCollector extends StatementPatternVisitor {

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

}
