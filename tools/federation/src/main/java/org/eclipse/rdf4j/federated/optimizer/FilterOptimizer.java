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
import java.util.Set;

import org.eclipse.rdf4j.federated.algebra.EmptyResult;
import org.eclipse.rdf4j.federated.algebra.ExclusiveGroup;
import org.eclipse.rdf4j.federated.algebra.FilterExpr;
import org.eclipse.rdf4j.federated.algebra.FilterTuple;
import org.eclipse.rdf4j.federated.algebra.NUnion;
import org.eclipse.rdf4j.federated.algebra.StatementTupleExpr;
import org.eclipse.rdf4j.federated.exception.OptimizationException;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter optimizer to push down FILTER expressions as far as possible.
 *
 * @author Andreas Schwarte
 *
 */
public class FilterOptimizer extends AbstractSimpleQueryModelVisitor<OptimizationException> implements FedXOptimizer {

	private static final Logger log = LoggerFactory.getLogger(FilterOptimizer.class);

	public FilterOptimizer() {
		super(true);
	}

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
				if ((new FilterBindingFinder().isFilterOnAssignedBinding(filter, filterExpr.getVars()))) {
					// make sure the filter remains in the new filter expression
					remainingExpr.add(filterExpr.getExpression());
					continue;
				}
				filterExprVst.initialize(filterExpr);
				filter.getArg().visit(filterExprVst);

				// if the filter expr. is handled in the stmt we do not have to keep it
				if (filterExprVst.canRemove()) {
					continue;
				}

				remainingExpr.add(filterExpr.getExpression());

			} else {
				remainingExpr.add(cond);
			}

		}

		if (remainingExpr.isEmpty()) {
			filter.replaceWith(filter.getArg()); // remove the filter
		} else if (remainingExpr.size() == 1) {
			filter.setCondition(remainingExpr.get(0)); // just apply the remaining condition
		} else {

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
		super.meet(filter);
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
		} else {
			conjExpr.add(expr);
		}
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

	protected static class VarFinder extends AbstractSimpleQueryModelVisitor<OptimizationException> {

		protected HashSet<String> vars;

		protected VarFinder() {
			super(true);
		}

		public HashSet<String> findVars(ValueExpr expr) {
			vars = new HashSet<>();
			expr.visit(this);
			return vars;
		}

		@Override
		public void meet(Var var) {
			if (var.getValue() == null) {
				vars.add(var.getName());
			}
			super.meet(var);
		}
	}

	protected static class FilterBindingFinder extends AbstractSimpleQueryModelVisitor<OptimizationException> {

		protected Set<String> vars;

		protected boolean isFilterOnAssignedBinding;

		protected FilterBindingFinder() {
			super(true);
		}

		public boolean isFilterOnAssignedBinding(TupleExpr expr, Set<String> filterArgs) {
			this.vars = filterArgs;
			expr.visit(this);
			return isFilterOnAssignedBinding;
		}

		@Override
		public void meet(Extension node) {
			for (String var : vars) {
				if (node.getBindingNames().contains(var)) {
					isFilterOnAssignedBinding = true;
					return;
				}
			}
			super.meet(node);
		}

		@Override
		public void meet(BindingSetAssignment node) {
			for (String var : vars) {
				if (node.getBindingNames().contains(var)) {
					isFilterOnAssignedBinding = true;
					return;
				}
			}
			super.meet(node);
		}
	}

	protected static class FilterExprInsertVisitor extends AbstractSimpleQueryModelVisitor<OptimizationException> {

		protected FilterExpr filterExpr = null; // the current filter Expr
		protected int added = 0;
		// determines whether the filter is static i.e. should not be pushed down as it would change
		// the query semantically
		protected boolean isStatic = false;

		protected FilterExprInsertVisitor() {
			super(true);
		}

		public void initialize(FilterExpr filterExpr) {
			this.added = 0;
			this.filterExpr = filterExpr;
			this.isStatic = false;
		}

		public boolean canRemove() {
			return added > 0 && !isStatic;
		}

		@Override
		public void meet(LeftJoin node) {
			isStatic = true;
			super.meet(node);
		}

		@Override
		public void meet(Union node) {
			isStatic = true;
			super.meet(node);
		}

		@Override
		public void meet(Difference node) {
			isStatic = true;
			super.meet(node);
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
			} else if (node instanceof StatementTupleExpr) {
				return;
			} else {
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
				if (filterTuple.getFreeVars().contains(filterVar)) {
					intersected++;
				}
			}

			// filter expression is irrelevant for this expression
			if (intersected == 0) {
				return;
			}

			// push eq comparison into stmt as bindings
			if (expr.isCompareEq()) {

				if (bindCompareInExpression(filterTuple, (Compare) expr.getExpression())) {
					added++;
					return;
				}
			}

			// filter contains all variables => push filter
			if (intersected == expr.getVars().size()) {
				if (shouldAddFilter(filterTuple)) {
					// only push filter if it has not been applied to another statement
					filterTuple.addFilterExpr(expr);
					added++;
				}
			}
		}

		public boolean shouldAddFilter(FilterTuple filterTuple) {
			if (filterTuple.getParentNode() instanceof ExclusiveGroup) {
				return false;
			} else if (hasUnionParent(filterTuple)) {
				return true;
			} else if (added > 0) {
				// only push filter if it has not been applied to another statement
				return false;
			} else {
				return added == 0;
			}
		}

		private boolean hasUnionParent(FilterTuple pattern) {
			QueryModelNode node = pattern.getParentNode();
			while (node != null && node != filterExpr) {
				if (node instanceof Union || node instanceof NUnion) {
					return true;
				}
				node = node.getParentNode();
			}
			return false;
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

			if (isVarLeft && isVarRight) {
				return false;
			}

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
