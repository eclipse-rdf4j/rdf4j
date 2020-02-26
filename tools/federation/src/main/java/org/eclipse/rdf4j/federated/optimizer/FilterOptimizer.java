/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.optimizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.rdf4j.federated.algebra.EmptyResult;
import org.eclipse.rdf4j.federated.algebra.ExclusiveGroup;
import org.eclipse.rdf4j.federated.algebra.FilterExpr;
import org.eclipse.rdf4j.federated.algebra.FilterTuple;
import org.eclipse.rdf4j.federated.algebra.StatementTupleExpr;
import org.eclipse.rdf4j.federated.exception.OptimizationException;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter optimizer to push down FILTER expressions as far as possible.
 * 
 * @author Andreas Schwarte
 *
 */
public class FilterOptimizer extends AbstractQueryModelVisitor<OptimizationException> implements FedXOptimizer {

	private static final Logger log = LoggerFactory.getLogger(FilterOptimizer.class);

	@Override
	public void optimize(TupleExpr tupleExpr) {
		tupleExpr.visit(this);
	}

	@Override
	public void meet(Filter filter) {

		if (filter.getArg() instanceof EmptyResult) {
			log.debug(
					"Argument of filter expression does not yield results at the provided sources, replacing Filter node.");
			filter.replaceWith(filter.getArg());
			return;
		}

		/*
		 * TODO idea: if we have a FILTER such as ?s='a' OR ?s='b' OR ?s='c' handle this appropriately
		 */

		ValueExpr valueExpr = filter.getCondition();

		/*
		 * TODO transform condition into some normal form, e.g. CNF
		 */

		// determine conjunctive expressions
		List<ValueExpr> conjunctiveExpressions = new ArrayList<>();
		getConjunctiveExpressions(valueExpr, conjunctiveExpressions);

		FilterExprInsertVisitor filterExprVst = new FilterExprInsertVisitor();
		List<ValueExpr> remainingExpr = new ArrayList<>(conjunctiveExpressions.size());

		for (ValueExpr cond : conjunctiveExpressions) {

			/*
			 * Determine if this filter is applicable for optimization. Currently only leaf expressions are applicable,
			 * i.e. not combined expressions.
			 */
			if (isCompatibleExpr(cond)) {

				HashSet<String> exprVars = new VarFinder().findVars(cond);
				FilterExpr filterExpr = new FilterExpr(cond, exprVars);

				filterExprVst.initialize(filterExpr);
				filter.getArg().visit(filterExprVst);

				// if the filter expr. is handled in the stmt we do not have to keep it
				if (filterExprVst.canRemove())
					continue;

				remainingExpr.add(filterExpr.getExpression());

			} else {
				remainingExpr.add(cond);
			}

		}

		if (remainingExpr.isEmpty()) {
			filter.replaceWith(filter.getArg()); // remove the filter
		}

		else if (remainingExpr.size() == 1) {
			filter.setCondition(remainingExpr.get(0)); // just apply the remaining condition
		}

		else {

			// construct conjunctive value expr
			And root = new And();
			root.setLeftArg(remainingExpr.get(0));
			And tmp = root;
			for (int i = 1; i < remainingExpr.size() - 1; i++) {
				And _a = new And();
				_a.setLeftArg(remainingExpr.get(i));
				tmp.setRightArg(_a);
				tmp = _a;
			}
			tmp.setRightArg(remainingExpr.get(remainingExpr.size() - 1));

			filter.setCondition(root);
		}

	}

	@Override
	public void meet(Service node) throws OptimizationException {
		// do not optimize anything within SERVICE
	}

	/**
	 * add the conjunctive expressions to specified list, has recursive step.
	 *
	 * @param expr     the expr, in the best case in CNF
	 * @param conjExpr the list to which expressions will be added
	 */
	protected void getConjunctiveExpressions(ValueExpr expr, List<ValueExpr> conjExpr) {
		if (expr instanceof And) {
			And and = (And) expr;
			getConjunctiveExpressions(and.getLeftArg(), conjExpr);
			getConjunctiveExpressions(and.getRightArg(), conjExpr);
		} else
			conjExpr.add(expr);
	}

	/**
	 * returns true if this filter can be used for optimization. Currently no conjunctive or disjunctive expressions are
	 * supported.
	 * 
	 * @param e
	 * @return whether the expression is compatible
	 */
	protected boolean isCompatibleExpr(ValueExpr e) {

		if (e instanceof And || e instanceof Or) {
			return false;
		}

		if (e instanceof Not) {
			return isCompatibleExpr(((Not) e).getArg());
		}

		return true;
	}

	protected static class VarFinder extends AbstractQueryModelVisitor<OptimizationException> {

		protected HashSet<String> vars;

		public HashSet<String> findVars(ValueExpr expr) {
			vars = new HashSet<>();
			expr.visit(this);
			return vars;
		}

		@Override
		public void meet(Var var) {
			if (var.getValue() == null)
				vars.add(var.getName());
			super.meet(var);
		}
	}

	protected static class FilterExprInsertVisitor extends AbstractQueryModelVisitor<OptimizationException> {

		protected FilterExpr filterExpr = null; // the current filter Expr
		protected boolean isApplied = false;

		public void initialize(FilterExpr filterExpr) {
			this.isApplied = false;
			this.filterExpr = filterExpr;
		}

		public boolean canRemove() {
			// if the filter is applied somewhere, it can be removed
			return isApplied;
		}

		@Override
		public void meetOther(QueryModelNode node) {

			if (node instanceof FilterTuple) {
				if (node instanceof ExclusiveGroup) {
					// for ExclusiveGroup also visit the children to insert
					// filter expressions and bound values
					node.visitChildren(this);
				}
				handleFilter((FilterTuple) node, filterExpr);
			}

			else if (node instanceof StatementTupleExpr) {
				return;
			}

			else {
				super.meetOther(node);
			}
		}

		private void handleFilter(FilterTuple filterTuple, FilterExpr expr) {

			/*
			 * CompareEQ expressions are inserted as bindings if possible
			 * 
			 * if the filtertuple contains all vars of the filterexpr, we can evaluate the filter expr safely on the
			 * filterTuple
			 * 
			 * if there is no intersection of variables, the filter is irrelevant for this expr
			 * 
			 * if there is some intersection, we cannot remove the filter and have to keep it in the query plan for
			 * postfiltering
			 */
			int intersected = 0;
			for (String filterVar : expr.getVars()) {
				if (filterTuple.getFreeVars().contains(filterVar))
					intersected++;
			}

			// filter expression is irrelevant for this expression
			if (intersected == 0) {
				return;
			}

			// push eq comparison into stmt as bindings
			if (expr.isCompareEq()) {

				if (bindCompareInExpression(filterTuple, (Compare) expr.getExpression())) {
					isApplied = true;
					return;
				}
			}

			// filter contains all variables => push filter
			if (intersected == expr.getVars().size()) {
				if (!isApplied) {
					// only push filter if it has not been applied to another statement
					filterTuple.addFilterExpr(expr);
				}
				isApplied = true;
			}
		}

		/**
		 * Bind the given compare filter expression in the tuple expression, i.e. insert the value as binding for the
		 * respective variable.
		 * 
		 * @param filterTuple
		 * @param cmp
		 * @return
		 */
		private boolean bindCompareInExpression(FilterTuple filterTuple, Compare cmp) {

			boolean isVarLeft = cmp.getLeftArg() instanceof Var;
			boolean isVarRight = cmp.getRightArg() instanceof Var;

			// cases
			// 1. both vars: we cannot add binding
			// 2. left var, right value -> add binding
			// 3. right var, left value -> add binding
			//
			// Note: we restrict this optimization to values of type Resource
			// since for instance subj can only be URIs (i.e. literals are
			// not allowed). For other types the Filter remains in place.

			if (isVarLeft && isVarRight)
				return false;

			if (isVarLeft && cmp.getRightArg() instanceof ValueConstant) {
				String varName = ((Var) cmp.getLeftArg()).getName();
				Value value = ((ValueConstant) cmp.getRightArg()).getValue();
				filterTuple.addBoundFilter(varName, value);
				return true;
			}

			if (isVarRight && cmp.getLeftArg() instanceof ValueConstant) {
				String varName = ((Var) cmp.getRightArg()).getName();
				Value value = ((ValueConstant) cmp.getLeftArg()).getValue();
				filterTuple.addBoundFilter(varName, value);
				return true;
			}

			return false; // not added
		}
	}
}
