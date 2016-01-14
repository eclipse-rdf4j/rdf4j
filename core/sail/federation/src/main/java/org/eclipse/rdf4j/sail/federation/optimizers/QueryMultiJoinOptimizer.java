/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.optimizers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.StatementPatternCollector;
import org.eclipse.rdf4j.sail.federation.algebra.NaryJoin;

/**
 * A query optimizer that re-orders nested Joins.
 * 
 * @author Arjohn Kampman
 * @author James Leigh
 */
public class QueryMultiJoinOptimizer implements QueryOptimizer {

	protected final EvaluationStatistics statistics;

	public QueryMultiJoinOptimizer() {
		this(new EvaluationStatistics());
	}

	public QueryMultiJoinOptimizer(EvaluationStatistics statistics) {
		this.statistics = statistics;
	}

	/**
	 * Applies generally applicable optimizations: path expressions are sorted
	 * from more to less specific.
	 * 
	 * @throws StoreException
	 */
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		tupleExpr.visit(new JoinVisitor());
	}

	protected class JoinVisitor extends AbstractQueryModelVisitor<RuntimeException> {

		private Set<String> boundVars = new HashSet<String>();

		@Override
		public void meet(LeftJoin leftJoin) {
			leftJoin.getLeftArg().visit(this);

			Set<String> origBoundVars = boundVars;
			try {
				boundVars = new HashSet<String>(boundVars);
				boundVars.addAll(leftJoin.getLeftArg().getBindingNames());

				leftJoin.getRightArg().visit(this);
			}
			finally {
				boundVars = origBoundVars;
			}
		}

		@Override
		public void meetOther(QueryModelNode node)
			throws RuntimeException
		{
			if (node instanceof NaryJoin) {
				meetJoin((NaryJoin)node);
			}
			else {
				super.meetOther(node);
			}
		}

		@Override
		public void meet(Join node)
			throws RuntimeException
		{
			meetJoin(node);
		}

		public void meetJoin(TupleExpr node) {
			Set<String> origBoundVars = boundVars;
			try {
				boundVars = new HashSet<String>(boundVars);

				// Recursively get the join arguments
				List<TupleExpr> joinArgs = getJoinArgs(node, new ArrayList<TupleExpr>());

				// Build maps of cardinalities and vars per tuple expression
				Map<TupleExpr, Double> cardinalityMap = new HashMap<TupleExpr, Double>();
				Map<TupleExpr, List<Var>> varsMap = new HashMap<TupleExpr, List<Var>>();

				for (TupleExpr tupleExpr : joinArgs) {
					cardinalityMap.put(tupleExpr, statistics.getCardinality(tupleExpr));
					varsMap.put(tupleExpr, getStatementPatternVars(tupleExpr));
				}

				// Build map of var frequences
				Map<Var, Integer> varFreqMap = new HashMap<Var, Integer>();
				for (List<Var> varList : varsMap.values()) {
					getVarFreqMap(varList, varFreqMap);
				}

				// Reorder the (recursive) join arguments to a more optimal sequence
				List<TupleExpr> orderedJoinArgs = new ArrayList<TupleExpr>(joinArgs.size());
				while (!joinArgs.isEmpty()) {
					TupleExpr tupleExpr = selectNextTupleExpr(joinArgs, cardinalityMap, varsMap, varFreqMap,
							boundVars);

					joinArgs.remove(tupleExpr);
					orderedJoinArgs.add(tupleExpr);

					// Recursively optimize join arguments
					tupleExpr.visit(this);

					boundVars.addAll(tupleExpr.getBindingNames());
				}

				// Build new join hierarchy
				TupleExpr replacement = new NaryJoin(orderedJoinArgs);

				// Replace old join hierarchy
				node.replaceWith(replacement);
			}
			finally {
				boundVars = origBoundVars;
			}
		}

		protected <L extends List<TupleExpr>> L getJoinArgs(TupleExpr tupleExpr, L joinArgs) {
			if (tupleExpr instanceof NaryJoin) {
				NaryJoin join = (NaryJoin)tupleExpr;
				for (TupleExpr arg : join.getArgs()) {
					getJoinArgs(arg, joinArgs);
				}
			}
			else if (tupleExpr instanceof Join) {
				Join join = (Join)tupleExpr;
				getJoinArgs(join.getLeftArg(), joinArgs);
				getJoinArgs(join.getRightArg(), joinArgs);
			}
			else {
				joinArgs.add(tupleExpr);
			}

			return joinArgs;
		}

		protected List<Var> getStatementPatternVars(TupleExpr tupleExpr) {
			List<StatementPattern> stPatterns = StatementPatternCollector.process(tupleExpr);
			List<Var> varList = new ArrayList<Var>(stPatterns.size() * 4);
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

		/**
		 * Selects from a list of tuple expressions the next tuple expression that
		 * should be evaluated. This method selects the tuple expression with
		 * highest number of bound variables, preferring variables that have been
		 * bound in other tuple expressions over variables with a fixed value.
		 */
		protected TupleExpr selectNextTupleExpr(List<TupleExpr> expressions,
				Map<TupleExpr, Double> cardinalityMap, Map<TupleExpr, List<Var>> varsMap,
				Map<Var, Integer> varFreqMap, Set<String> boundVars)
		{
			double lowestCardinality = Double.MAX_VALUE;
			TupleExpr result = null;

			for (TupleExpr tupleExpr : expressions) {
				// Calculate a score for this tuple expression
				double cardinality = getTupleExprCardinality(tupleExpr, cardinalityMap, varsMap, varFreqMap,
						boundVars);

				if (cardinality < lowestCardinality) {
					// More specific path expression found
					lowestCardinality = cardinality;
					result = tupleExpr;
				}
			}

			return result;
		}

		protected double getTupleExprCardinality(TupleExpr tupleExpr, Map<TupleExpr, Double> cardinalityMap,
				Map<TupleExpr, List<Var>> varsMap, Map<Var, Integer> varFreqMap, Set<String> boundVars)
		{
			double cardinality = cardinalityMap.get(tupleExpr);

			List<Var> vars = varsMap.get(tupleExpr);

			// Compensate for variables that are bound earlier in the evaluation
			List<Var> unboundVars = getUnboundVars(vars);
			List<Var> constantVars = getConstantVars(vars);
			int nonConstantCount = vars.size() - constantVars.size();
			if (nonConstantCount > 0) {
				double exp = (double)unboundVars.size() / nonConstantCount;
				cardinality = Math.pow(cardinality, exp);
			}

			if (unboundVars.isEmpty()) {
				// Prefer patterns with more bound vars
				if (nonConstantCount > 0) {
					cardinality /= nonConstantCount;
				}
			}
			else {
				// Prefer patterns that bind variables from other tuple expressions
				int foreignVarFreq = getForeignVarFreq(unboundVars, varFreqMap);
				if (foreignVarFreq > 0) {
					cardinality /= foreignVarFreq;
				}
			}

			// Prefer patterns that bind more variables
			// List<Var> distinctUnboundVars = getUnboundVars(new
			// HashSet<Var>(vars));
			// if (distinctUnboundVars.size() >= 2) {
			// cardinality /= distinctUnboundVars.size();
			// }

			return cardinality;
		}

		protected List<Var> getConstantVars(Iterable<Var> vars) {
			List<Var> constantVars = new ArrayList<Var>();

			for (Var var : vars) {
				if (var.hasValue()) {
					constantVars.add(var);
				}
			}

			return constantVars;
		}

		protected List<Var> getUnboundVars(Iterable<Var> vars) {
			List<Var> unboundVars = new ArrayList<Var>();

			for (Var var : vars) {
				if (!var.hasValue() && !this.boundVars.contains(var.getName())) {
					unboundVars.add(var);
				}
			}

			return unboundVars;
		}

		protected int getForeignVarFreq(List<Var> ownUnboundVars, Map<Var, Integer> varFreqMap) {
			int result = 0;

			Map<Var, Integer> ownFreqMap = getVarFreqMap(ownUnboundVars, new HashMap<Var, Integer>());

			for (Map.Entry<Var, Integer> entry : ownFreqMap.entrySet()) {
				Var var = entry.getKey();
				int ownFreq = entry.getValue();
				result += varFreqMap.get(var) - ownFreq;
			}

			return result;
		}
	}
}
