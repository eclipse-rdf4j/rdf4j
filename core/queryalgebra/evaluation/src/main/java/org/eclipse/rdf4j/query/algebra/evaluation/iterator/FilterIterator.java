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

import java.util.Comparator;
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

public class FilterIterator extends FilterIteration<BindingSet> {

	private final QueryValueEvaluationStep condition;
	private final EvaluationStrategy strategy;
	private final Function<BindingSet, BindingSet> retain;

	public static QueryEvaluationStep supply(Filter filter, EvaluationStrategy strategy,
			QueryEvaluationContext context) {
		QueryEvaluationStep arg = strategy.precompile(filter.getArg(), context);
		QueryValueEvaluationStep ves;
		try {
			ves = strategy.precompile(filter.getCondition(), context);
		} catch (QueryEvaluationException e) {
			// If we have a failed compilation we always return false.
			// Which means empty. so let's short circuit that.
			return QueryEvaluationStep.EMPTY;
		}
		Function<BindingSet, BindingSet> retain;
		if (!isPartOfSubQuery(filter)) {
			retain = buildRetainFunction(filter, context);
		} else {
			retain = Function.identity();
		}

		return (bs) -> new FilterIterator(arg.evaluate(bs), ves, strategy, retain);
	}

	/*--------------*
	 * Constructors *
	 *--------------*/

	public FilterIterator(Filter filter, CloseableIteration<BindingSet> iter, QueryValueEvaluationStep condition,
			EvaluationStrategy strategy) throws QueryEvaluationException {
		super(iter);
		this.condition = condition;
		this.strategy = strategy;
		if (!isPartOfSubQuery(filter)) {
			this.retain = (bs) -> {
				QueryBindingSet nbs = new QueryBindingSet(bs);
				nbs.retainAll(filter.getBindingNames());
				return nbs;
			};
		} else {
			this.retain = Function.identity();
		}
	}

	private FilterIterator(CloseableIteration<BindingSet> iter,
			QueryValueEvaluationStep condition, EvaluationStrategy strategy, Function<BindingSet, BindingSet> retain)
			throws QueryEvaluationException {
		super(iter);
		this.condition = condition;
		this.strategy = strategy;
		// FIXME Jeen Boekstra scopeBindingNames should include bindings from superquery
		// if the filter is part of a subquery. This is a workaround: we should fix the
		// settings of scopeBindingNames, rather than skipping the limiting of bindings.
		this.retain = retain;

	}

	private static Function<BindingSet, BindingSet> buildRetainFunction(Filter filter, QueryEvaluationContext context) {
		final Set<String> bindingNames = filter.getBindingNames();
		@SuppressWarnings("unchecked")
		final Predicate<BindingSet>[] hasBinding = new Predicate[bindingNames.size()];
		@SuppressWarnings("unchecked")
		final BiConsumer<BindingSet, MutableBindingSet>[] setBinding = new BiConsumer[bindingNames.size()];
		Iterator<String> bi = bindingNames.iterator();
		for (int i = 0; bi.hasNext(); i++) {
			String bindingName = bi.next();
			hasBinding[i] = context.hasBinding(bindingName);
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

	@Override
	protected void handleClose() {

	}

}
