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

import java.util.HashSet;
import java.util.Set;
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
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.SubQueryValueOperator;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.collectors.VarNameCollector;

@Deprecated(since = "4.1.0")
public class FilterIterator extends FilterIteration<BindingSet, QueryEvaluationException> {

	private final QueryValueEvaluationStep condition;
	private final EvaluationStrategy strategy;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public FilterIterator(Filter filter, CloseableIteration<BindingSet, QueryEvaluationException> iter,
			QueryValueEvaluationStep condition, EvaluationStrategy strategy) throws QueryEvaluationException {
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

	@Deprecated(forRemoval = true, since = "4.2.1")
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

	protected static class VarCollector extends AbstractQueryModelVisitor<QueryEvaluationException> {

		private final Set<String> collectedVars = new HashSet<>();

		@Override
		public void meet(Var var) {
			if (!var.isAnonymous()) {
				collectedVars.add(var.getName());
			}
		}

		@Override
		public void meet(BindingSetAssignment node) throws QueryEvaluationException {
			for (BindingSet bs : node.getBindingSets()) {
				for (String name : bs.getBindingNames()) {
					collectedVars.add(name);
				}
			}
			super.meet(node);
		}

		/**
		 * @return Returns the collectedVars.
		 */
		public Set<String> getCollectedVars() {
			return collectedVars;
		}

	}

	/**
	 * This is used to make sure that no variable is seen by the filter that are not in scope. Historically important in
	 * subquery cases. See also GH-4769
	 */
	public static final class RetainedVariableFilteredQueryEvaluationContext implements QueryEvaluationContext {
		private final QueryEvaluationContext context;
		private final Set<String> inScopeVariableNames;

		public RetainedVariableFilteredQueryEvaluationContext(Filter node, QueryEvaluationContext contextToFilter) {
			this.context = contextToFilter;
			final VarCollector varCollector = new VarCollector();
			node.getArg().visit(varCollector);
			this.inScopeVariableNames = varCollector.getCollectedVars();
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
				return (bs) -> false;
			}
		}

		boolean isVariableInScope(String variableName) {
			return inScopeVariableNames.contains(variableName);
		}

		@Override
		public java.util.function.Function<BindingSet, Binding> getBinding(String variableName) {
			if (isVariableInScope(variableName)) {
				return context.getBinding(variableName);
			} else {
				return (bs) -> null;
			}
		}

		@Override
		public java.util.function.Function<BindingSet, Value> getValue(String variableName) {
			if (isVariableInScope(variableName)) {
				return context.getValue(variableName);
			} else {
				return (bs) -> null;
			}
		}

		@Override
		public BiConsumer<Value, MutableBindingSet> setBinding(String variableName) {
			return context.setBinding(variableName);
		}
	}

}
