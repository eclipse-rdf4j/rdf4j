/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.StatementPatternCollector;
import org.eclipse.rdf4j.query.algebra.helpers.TupleExprs;

/**
 * A query optimizer that re-orders nested Joins.
 *
 * @author Arjohn Kampman
 * @author James Leigh
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class QueryJoinOptimizer implements QueryOptimizer {

	protected final EvaluationStatistics statistics;

	public QueryJoinOptimizer() {
		this(new EvaluationStatistics());
	}

	public QueryJoinOptimizer(EvaluationStatistics statistics) {
		this.statistics = statistics;
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
	 *
	 * @deprecated This class is protected for historic reasons only, and will be made private in a future major
	 *             release.
	 */
	@Deprecated
	protected class JoinVisitor extends AbstractQueryModelVisitor<RuntimeException> {

		Set<String> boundVars = new HashSet<>();

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
			super.meet(node);
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

				// Reorder the (recursive) join arguments to a more optimal sequence
				List<TupleExpr> orderedJoinArgs = new ArrayList<>(joinArgs.size());

				// Reorder the subselects and extensions to a more optimal sequence
				List<TupleExpr> priorityArgs = new ArrayList<>(joinArgs.size());

				// get all extensions (BIND clause)
				List<Extension> orderedExtensions = getExtensions(joinArgs);
				joinArgs.removeAll(orderedExtensions);
				priorityArgs.addAll(orderedExtensions);

				// get all subselects and order them
				List<TupleExpr> orderedSubselects = reorderSubselects(getSubSelects(joinArgs));
				joinArgs.removeAll(orderedSubselects);
				priorityArgs.addAll(orderedSubselects);

				// We order all remaining join arguments based on cardinality and
				// variable frequency statistics
				if (joinArgs.size() > 0) {
					// Build maps of cardinalities and vars per tuple expression
					Map<TupleExpr, Double> cardinalityMap = new HashMap<>();
					Map<TupleExpr, List<Var>> varsMap = new HashMap<>();

					for (TupleExpr tupleExpr : joinArgs) {
						double cardinality = statistics.getCardinality(tupleExpr);
						tupleExpr.setResultSizeEstimate(Math.max(cardinality, tupleExpr.getResultSizeEstimate()));
						cardinalityMap.put(tupleExpr, cardinality);
						if (tupleExpr instanceof ZeroLengthPath) {
							varsMap.put(tupleExpr, ((ZeroLengthPath) tupleExpr).getVarList());
						} else {
							varsMap.put(tupleExpr, getStatementPatternVars(tupleExpr));
						}
					}

					// Build map of var frequences
					Map<Var, Integer> varFreqMap = new HashMap<>();
					for (List<Var> varList : varsMap.values()) {
						getVarFreqMap(varList, varFreqMap);
					}

					// order all other join arguments based on available statistics
					while (!joinArgs.isEmpty()) {
						TupleExpr tupleExpr = selectNextTupleExpr(joinArgs, cardinalityMap, varsMap, varFreqMap,
								boundVars);

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
			List<StatementPattern> stPatterns = StatementPatternCollector.process(tupleExpr);
			List<Var> varList = new ArrayList<>(stPatterns.size() * 4);
			for (StatementPattern sp : stPatterns) {
				sp.getVars(varList);
			}
			return varList;
		}

		protected <M extends Map<Var, Integer>> M getVarFreqMap(List<Var> varList, M varFreqMap) {
			for (Var var : varList) {
				Integer freq = varFreqMap.get(var);
				freq = (freq == null) ? 1 : freq + 1;
				varFreqMap.put(var, freq);
			}
			return varFreqMap;
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

		protected List<TupleExpr> getSubSelects(List<TupleExpr> expressions) {
			List<TupleExpr> subselects = new ArrayList<>();

			for (TupleExpr expr : expressions) {
				if (TupleExprs.containsSubquery(expr)) {
					subselects.add(expr);
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
		 *
		 * should result in:
		 *
		 * <pre>
		 *   [a b c] [a d] [b e] [e f] [f]
		 * </pre>
		 *
		 * @param subselects the original ordering of expressions
		 * @return the optimized ordering of expressions
		 */
		protected List<TupleExpr> reorderSubselects(List<TupleExpr> subselects) {

			if (subselects.size() == 1) {
				return subselects;
			}

			List<TupleExpr> result = new ArrayList<>();
			if (subselects == null || subselects.isEmpty()) {
				return result;
			}

			// Step 1: determine size of join for each pair of arguments
			HashMap<Integer, List<TupleExpr[]>> joinSizes = new HashMap<>();

			int maxJoinSize = 0;
			for (int i = 0; i < subselects.size(); i++) {
				TupleExpr firstArg = subselects.get(i);
				for (int j = i + 1; j < subselects.size(); j++) {
					TupleExpr secondArg = subselects.get(j);

					Set<String> names = firstArg.getBindingNames();
					names.retainAll(secondArg.getBindingNames());

					int joinSize = names.size();
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
			result.add(maxUnionTupleTuple[0]);
			result.add(maxUnionTupleTuple[1]);

			// Step 3: sort the rest of the list by selecting and adding an element
			// at a time.
			while (result.size() < subselects.size()) {
				result.add(getNextSubselect(result, subselects));
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
					names.retainAll(currentListNames);
					int joinSize = names.size();

					names = candidate.getBindingNames();
					names.addAll(currentListNames);
					int unionSize = names.size();

					if (joinSize > currentJoinSize) {
						selected = candidate;
						currentJoinSize = joinSize;
						currentUnionSize = unionSize;
					} else if (joinSize == currentJoinSize) {
						if (unionSize > currentUnionSize) {
							selected = candidate;
							currentJoinSize = joinSize;
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
				Map<TupleExpr, List<Var>> varsMap, Map<Var, Integer> varFreqMap, Set<String> boundVars) {
			TupleExpr result = null;
			double lowestCost = Double.POSITIVE_INFINITY;

			for (TupleExpr tupleExpr : expressions) {
				// Calculate a score for this tuple expression
				double cost = getTupleExprCost(tupleExpr, cardinalityMap, varsMap, varFreqMap,
						boundVars);

				if (cost < lowestCost || result == null) {
					// More specific path expression found
					lowestCost = cost;
					result = tupleExpr;
				}
			}

			result.setCostEstimate(lowestCost);

			return result;
		}

		@Deprecated
		protected double getTupleExprCardinality(TupleExpr tupleExpr, Map<TupleExpr, Double> cardinalityMap,
				Map<TupleExpr, List<Var>> varsMap, Map<Var, Integer> varFreqMap, Set<String> boundVars) {
			return getTupleExprCost(tupleExpr, cardinalityMap, varsMap, varFreqMap, boundVars);
		}

		protected double getTupleExprCost(TupleExpr tupleExpr, Map<TupleExpr, Double> cardinalityMap,
				Map<TupleExpr, List<Var>> varsMap, Map<Var, Integer> varFreqMap, Set<String> boundVars) {

			double cost = cardinalityMap.get(tupleExpr);

			List<Var> vars = varsMap.get(tupleExpr);

			// Compensate for variables that are bound earlier in the evaluation
			List<Var> unboundVars = getUnboundVars(vars);
			List<Var> constantVars = getConstantVars(vars);

			int nonConstantVarCount = vars.size() - constantVars.size();

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

			// BindingSetAssignment has a typical constant cost. This cost is not based on statistics so is much more
			// reliable. If the BindingSetAssignment binds to any of the other variables in the other tuple expressions
			// to choose from, then the cost of the BindingSetAssignment should be set to 0 since it will always limit
			// the upper bound of any other costs. This way the BindingSetAssignment will be chosen as the left
			// argument.
			if (tupleExpr instanceof BindingSetAssignment) {

				Set<Var> varsUsedInOtherExpressions = varFreqMap.keySet();

				for (String assuredBindingName : tupleExpr.getAssuredBindingNames()) {
					if (varsUsedInOtherExpressions.contains(new Var(assuredBindingName))) {
						cost = 0;
						break;
					}
				}
			}

			return cost;
		}

		protected List<Var> getConstantVars(Iterable<Var> vars) {
			List<Var> constantVars = new ArrayList<>();

			for (Var var : vars) {
				if (var.hasValue()) {
					constantVars.add(var);
				}
			}

			return constantVars;
		}

		protected List<Var> getUnboundVars(Iterable<Var> vars) {
			List<Var> unboundVars = new ArrayList<>();

			for (Var var : vars) {
				if (!var.hasValue() && !this.boundVars.contains(var.getName())) {
					unboundVars.add(var);
				}
			}

			return unboundVars;
		}

		protected int getForeignVarFreq(List<Var> ownUnboundVars, Map<Var, Integer> varFreqMap) {
			int result = 0;

			Map<Var, Integer> ownFreqMap = getVarFreqMap(ownUnboundVars, new HashMap<>());

			for (Map.Entry<Var, Integer> entry : ownFreqMap.entrySet()) {
				Var var = entry.getKey();
				int ownFreq = entry.getValue();
				result += varFreqMap.get(var) - ownFreq;
			}

			return result;
		}
	}
}
