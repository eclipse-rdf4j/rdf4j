/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Lateral;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;

/**
 * Draft implementation of SEP-006 using an SEP-007 substitute approach.
 *
 * @author jerven bolleman
 */
public class LateralQueryEvaluationStep implements QueryEvaluationStep {

	private final QueryEvaluationStep right;
	private final Map<String, VarThatSubsitutes> toSubstitute;
	private final QueryEvaluationStep left;

	private LateralQueryEvaluationStep(QueryEvaluationStep left, TupleExpr rightExpr, Lateral node,
			QueryEvaluationContext context, EvaluationStrategy strategy) {
		this.left = left;
		this.toSubstitute = new HashMap<String, VarThatSubsitutes>();
		rightExpr = (TupleExpr) rightExpr.clone();
		// TODO should we use assured bindingNames?
		rightExpr.visit(new InjectSubstitutableVars(node.getBindingNames(), context));
		this.right = strategy.precompile(rightExpr, context);
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings) {
		CloseableIteration<BindingSet, QueryEvaluationException> leftIter = left.evaluate(bindings);
		return new LateralIteration(leftIter);
	}

	public static QueryEvaluationStep supply(EvaluationStrategy strategy, Lateral node,
			QueryEvaluationContext context) {
		QueryEvaluationStep left = strategy.precompile(node.getLeftArg(), context);
		return new LateralQueryEvaluationStep(left, node.getRightArg(), node, context, strategy);
	}

	private final class InjectSubstitutableVars extends AbstractSimpleQueryModelVisitor<QueryEvaluationException> {
		private final Set<String> toBind;
		private final QueryEvaluationContext context;

		private InjectSubstitutableVars(Set<String> toBind, QueryEvaluationContext context) {
			this.toBind = toBind;
			this.context = context;
		}

		@Override
		public void meet(BindingSetAssignment node) throws QueryEvaluationException {
			// TODO maybe throw an error as this might not be allowed
			super.meet(node);
		}

		@Override
		public void meet(Var node) throws QueryEvaluationException {
			final String varName = node.getName();
			if (!node.isConstant() && !node.isAnonymous() && toBind.contains(varName)) {
				if (toSubstitute.containsKey(varName)) {
					node.replaceWith(toSubstitute.get(varName));
				} else {
					Predicate<BindingSet> hasValue = context.hasBinding(varName);
					Function<BindingSet, Value> getValue = context.getValue(varName);
					final VarThatSubsitutes replacement = new VarThatSubsitutes(varName, hasValue, getValue,
							context.createBindingSet());
					node.replaceWith(replacement);
				}
			}
		}
	}

	private class VarThatSubsitutes extends Var {
		private static final long serialVersionUID = 1L;
		public BindingSet currentBindingSet;
		final Predicate<BindingSet> hasValue;
		final Function<BindingSet, Value> getValue;

		public VarThatSubsitutes(String name, Predicate<BindingSet> hasValue, Function<BindingSet, Value> getValue,
				MutableBindingSet mutableBindingSet) {
			super(name);
			this.hasValue = hasValue;
			this.getValue = getValue;
			this.currentBindingSet = mutableBindingSet;
		}

		@Override
		public boolean hasValue() {
			return hasValue.test(this.currentBindingSet);
		}

		@Override
		public Value getValue() {
			return getValue.apply(this.currentBindingSet);
		}
	}

	private class LateralIteration extends LookAheadIteration<BindingSet, QueryEvaluationException> {
		private final CloseableIteration<BindingSet, QueryEvaluationException> leftIter;
		private CloseableIteration<BindingSet, QueryEvaluationException> rightIter = QueryEvaluationStep.EMPTY_ITERATION;

		public LateralIteration(CloseableIteration<BindingSet, QueryEvaluationException> leftIter) {
			this.leftIter = leftIter;
		}

		@Override
		protected BindingSet getNextElement() throws QueryEvaluationException {
			while (rightIter.hasNext() || leftIter.hasNext()) {
				if (rightIter.hasNext()) {
					return rightIter.next();
				}

				// Right iteration exhausted
				rightIter.close();

				if (leftIter.hasNext()) {
					final BindingSet next = leftIter.next();
					toSubstitute.values().forEach(v -> v.currentBindingSet = next);
					rightIter = right.evaluate(next);
				}
			}
			return null;
		}

		@Override
		protected void handleClose() throws QueryEvaluationException {
			try {
				super.handleClose();
			} finally {
				try {
					leftIter.close();
				} finally {
					rightIter.close();
				}
			}
		}

	}

}
