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
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;

public class FilterIterator extends FilterIteration<BindingSet> {

	private final QueryValueEvaluationStep condition;
	private final EvaluationStrategy strategy;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public FilterIterator(CloseableIteration<BindingSet> iter, QueryValueEvaluationStep condition,
			EvaluationStrategy strategy) throws QueryEvaluationException {
		super(iter);
		this.condition = condition;
		this.strategy = strategy;

	}

	@Override
	protected boolean accept(BindingSet bindings) throws QueryEvaluationException {
		try {
			return strategy.isTrue(condition, bindings);
		} catch (ValueExprEvaluationException e) {
			// failed to evaluate condition
			return false;
		}
	}

	@Override
	protected void handleClose() {

	}

	/**
	 * This is used to make sure that no variable is seen by the filter that are not in scope. Historically important in
	 * subquery cases.
	 */
	public static final class RetainedVariableFilteredQueryEvaluationContext implements QueryEvaluationContext {
		private final Filter node;
		private final QueryEvaluationContext context;

		public RetainedVariableFilteredQueryEvaluationContext(Filter node, QueryEvaluationContext contextToFilter) {
			this.node = node;
			this.context = contextToFilter;
		}

		@Override
		public Comparator<Value> getComparator() {
			return null;
		}

		@Override
		public Literal getNow() {
			return context.getNow();
		}

		@Override
		public Dataset getDataset() {
			return context.getDataset();
		}

		@Override
		public Predicate<BindingSet> hasBinding(String variableName) {
			if (isVariableInScope(variableName)) {
				return context.hasBinding(variableName);
			} else {
				return bs -> false;
			}
		}

		boolean isVariableInScope(String variableName) {
			return node.getBindingNames().contains(variableName);
		}

		@Override
		public java.util.function.Function<BindingSet, Binding> getBinding(String variableName) {
			if (isVariableInScope(variableName)) {
				return context.getBinding(variableName);
			} else {
				return bs -> null;
			}
		}

		@Override
		public java.util.function.Function<BindingSet, Value> getValue(String variableName) {
			if (isVariableInScope(variableName)) {
				return context.getValue(variableName);
			} else {
				return bs -> null;
			}
		}

		@Override
		public BiConsumer<Value, MutableBindingSet> setBinding(String variableName) {
			return context.setBinding(variableName);
		}
	}

}
