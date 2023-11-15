/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.Iterator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.SubQueryValueOperator;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;

@Deprecated(since = "4.1.0")
public class FilterIterator extends FilterIteration<BindingSet, QueryEvaluationException> {

	private final QueryValueEvaluationStep condition;
	private final EvaluationStrategy strategy;
	private final Function<BindingSet, BindingSet> retain;

	public static QueryEvaluationStep supply(Filter node, EvaluationStrategy strategy,
			QueryEvaluationContext context) {
		QueryEvaluationStep arg = strategy.precompile(node.getArg(), context);
		QueryValueEvaluationStep ves;
		try {
//			final QueryEvaluationContext context2 = new FilterIterator.RetainedVariableFilteredQueryEvaluationContext(
//					node, context);
			ves = strategy.precompile(node.getCondition(), context);
		} catch (QueryEvaluationException e) {
			// If we have a failed compilation we always return false.
			// Which means empty. so let's short circuit that.
			return QueryEvaluationStep.EMPTY;
		}
		return new QueryEvaluationStep() {

			@Override
			public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bs) {
				return new FilterIterator(node, arg.evaluate(bs), ves, strategy, context);
			}
		};
	}

	/*--------------*
	 * Constructors *
	 *--------------*/

	public FilterIterator(Filter filter, CloseableIteration<BindingSet, QueryEvaluationException> iter,
			QueryValueEvaluationStep condition, EvaluationStrategy strategy) throws QueryEvaluationException {
		super(iter);
		this.condition = condition;
		this.strategy = strategy;
		if (!isPartOfSubQuery(filter)) {
			this.retain = (bs) -> {
				QueryBindingSet nbs = new QueryBindingSet(bs);
				nbs.retainAll(filter.getBindingNames());
				return bs;
			};
		} else {
			this.retain = Function.identity();
		}
	}

	private FilterIterator(Filter filter, CloseableIteration<BindingSet, QueryEvaluationException> iter,
			QueryValueEvaluationStep condition, EvaluationStrategy strategy, QueryEvaluationContext context)
			throws QueryEvaluationException {
		super(iter);
		this.condition = condition;
		this.strategy = strategy;
		// FIXME Jeen Boekstra scopeBindingNames should include bindings from superquery
		// if the filter is part of a subquery. This is a workaround: we should fix the
		// settings of scopeBindingNames, rather than skipping the limiting of bindings.
		if (!isPartOfSubQuery(filter)) {
//			this.retain = (bs) -> {
//				QueryBindingSet nbs = new QueryBindingSet(bs);
//				nbs.retainAll(filter.getBindingNames());
//				return bs;
//			};
			this.retain = buildRetainFunction(filter, context);
		} else {
			this.retain = Function.identity();
		}
	}

	private Function<BindingSet, BindingSet> buildRetainFunction(Filter filter, QueryEvaluationContext context) {
		final Set<String> bindingNames = filter.getBindingNames();
		@SuppressWarnings("unchecked")
		final Predicate<BindingSet>[] hasBinding = new Predicate[bindingNames.size()];
		@SuppressWarnings("unchecked")
		final BiConsumer<BindingSet, MutableBindingSet>[] setBinding = new BiConsumer[bindingNames.size()];
		Iterator<String> bi = bindingNames.iterator();
		for (int i = 0; bi.hasNext(); i++) {
			String bindingName = bi.next();
			hasBinding[i] = (bs) -> bs.hasBinding(bindingName);
			final Function<BindingSet, Value> getValue = context.getValue(bindingName);
//			Can't use this as there might be bindingNames that the filter expects that are not available.
//			See line the meet(Var) in the ArrayBindingSet.findAllVariablesUsedInQuery(QueryRoot method)
//			final BiConsumer<Value, MutableBindingSet> directSetBinding = context.setBinding(bindingName);
			setBinding[i] = (bs, nbs) -> nbs.setBinding(bindingName, getValue.apply(bs));
		}
		return (bs) -> {
			MutableBindingSet nbs = context.createBindingSet();
			for (int i = 0; i < hasBinding.length; i++) {
				if (hasBinding[i].test(bs)) {
					setBinding[i].accept(bs, nbs);
				}
			}
			return nbs;
		};
	}

	@Override
	protected boolean accept(BindingSet bindings) throws QueryEvaluationException {
		try {

			// Limit the bindings to the ones that are in scope for this filter
			BindingSet scopeBindings = this.retain.apply(bindings);
			return strategy.isTrue(condition, scopeBindings);
		} catch (ValueExprEvaluationException e) {
			// failed to evaluate condition
			return false;
		}
	}

//	@Deprecated(forRemoval = true, since = "4.2.1")
	public static boolean isPartOfSubQuery(QueryModelNode node) {
		if (node instanceof SubQueryValueOperator) {
			return true;
		}

		QueryModelNode parent = node.getParentNode();
		if (parent == null) {
			return false;
		} else {
			return isPartOfSubQuery(parent);
		}
	}

//	protected static class InExistsVarCollector extends AbstractSimpleQueryModelVisitor<QueryEvaluationException> {
//		private final VarCollector vc;
//
//		public InExistsVarCollector(VarCollector vc) {
//			super();
//			this.vc = vc;
//		}
//
//		@Override
//		public void meet(Exists ex) {
//			ex.visit(vc);
//		}
//	}

//	protected static class VarCollector extends AbstractSimpleQueryModelVisitor<QueryEvaluationException> {
//
//		private final Set<String> collectedVars = new HashSet<>();
//
//		public VarCollector() {
//			super(true);
//		}
//
//		@Override
//		public void meet(Var var) {
//			if (!var.isAnonymous()) {
//				collectedVars.add(var.getName());
//			}
//		}
//
//		@Override
//		public void meet(BindingSetAssignment node) throws QueryEvaluationException {
//			for (BindingSet bs : node.getBindingSets()) {
//				for (String name : bs.getBindingNames()) {
//					collectedVars.add(name);
//				}
//			}
//			super.meet(node);
//		}
//
//		/**
//		 * @return Returns the collectedVars.
//		 */
//		public Set<String> getCollectedVars() {
//			return collectedVars;
//		}
//
//		@Override
//		public void meetUnaryTupleOperator(UnaryTupleOperator node) {
//			if (!node.isVariableScopeChange()) {
//				super.meetUnaryTupleOperator(node);
//			}
//		}
//
//		@Override
//		public void meetBinaryTupleOperator(BinaryTupleOperator node) {
//			if (!node.isVariableScopeChange()) {
//				super.meetBinaryTupleOperator(node);
//			}
//		}
//
//		@Override
//		protected void meetBinaryValueOperator(BinaryValueOperator node) throws RuntimeException {
//			if (!node.isVariableScopeChange()) {
//				super.meetBinaryValueOperator(node);
//			}
//		}
//
//		@Override
//		protected void meetNAryValueOperator(NAryValueOperator node) throws RuntimeException {
//			if (!node.isVariableScopeChange()) {
//				super.meetNAryValueOperator(node);
//			}
//		}
//
//		@Override
//		protected void meetSubQueryValueOperator(SubQueryValueOperator node) throws RuntimeException {
//			if (!node.isVariableScopeChange()) {
//				super.meetSubQueryValueOperator(node);
//			}
//		}
//
//		@Override
//		protected void meetUnaryValueOperator(UnaryValueOperator node) throws RuntimeException {
//			if (!node.isVariableScopeChange()) {
//				super.meetUnaryValueOperator(node);
//			}
//		}
//	}

//	/**
//	 * This is used to make sure that no variable is seen by the filter that are not in scope. Historically important in
//	 * subquery cases. See also GH-4769
//	 */
//	public static final class RetainedVariableFilteredQueryEvaluationContext implements QueryEvaluationContext {
//		private final QueryEvaluationContext context;
//		private final Set<String> inScopeVariableNames;
//
////		private final Filter node;
//		public RetainedVariableFilteredQueryEvaluationContext(Filter node, QueryEvaluationContext contextToFilter) {
//			this.context = contextToFilter;
//
////			this.node = node;
//		}
//
//		@Override
//		public Literal getNow() {
//			return context.getNow();
//		}
//
//		@Override
//		public Dataset getDataset() {
//			return context.getDataset();
//		}
//
//		@Override
//		public Predicate<BindingSet> hasBinding(String variableName) {
//			if (isVariableInScope(variableName)) {
//				return context.hasBinding(variableName);
//			} else {
//				return (bs) -> false;
//			}
//		}
//
//		boolean isVariableInScope(String variableName) {
//			return inScopeVariableNames.contains(variableName);
//		}
//
//		@Override
//		public java.util.function.Function<BindingSet, Binding> getBinding(String variableName) {
//			if (isVariableInScope(variableName)) {
//				return context.getBinding(variableName);
//			} else {
//				return (bs) -> null;
//			}
//		}
//
//		@Override
//		public java.util.function.Function<BindingSet, Value> getValue(String variableName) {
//			if (isVariableInScope(variableName)) {
//				return context.getValue(variableName);
//			} else {
//				return (bs) -> null;
//			}
//		}
//
//		@Override
//		public BiConsumer<Value, MutableBindingSet> setBinding(String variableName) {
//			return context.setBinding(variableName);
//		}
//
//		@Override
//		public MutableBindingSet createBindingSet() {
//			return context.createBindingSet();
//		}
//
//		@Override
//		public BiConsumer<Value, MutableBindingSet> addBinding(String variableName) {
//			return context.addBinding(variableName);
//		}
//	}

}
