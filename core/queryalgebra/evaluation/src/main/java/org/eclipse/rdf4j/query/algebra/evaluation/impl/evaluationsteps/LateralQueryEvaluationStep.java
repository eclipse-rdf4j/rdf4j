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
import java.util.function.Consumer;
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
import org.eclipse.rdf4j.query.algebra.Projection;
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
	private final QueryEvaluationStep left;
	private final Lateral node;
	private final Map<String, Consumer<BindingSet>> toSubstitute;

	private LateralQueryEvaluationStep(QueryEvaluationStep left, QueryEvaluationStep right, Lateral node,
			QueryEvaluationContext context, EvaluationStrategy strategy,
			Map<String, Consumer<BindingSet>> toSubstitute) {
		this.left = left;
		this.node = node;
		this.right = right;
		this.toSubstitute = toSubstitute;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings) {
		CloseableIteration<BindingSet, QueryEvaluationException> leftIter = left.evaluate(bindings);
		return new LateralIteration(leftIter, node);
	}

	public static QueryEvaluationStep supply(EvaluationStrategy strategy, Lateral node,
			QueryEvaluationContext context) {
		QueryEvaluationStep left = strategy.precompile(node.getLeftArg(), context);
		TupleExpr rightArg = (TupleExpr) node.getRightArg().clone();
		InjectSubstitutableVars visitor = new InjectSubstitutableVars(node.getLeftArg().getBindingNames(), context);
		rightArg.visit(visitor);

		QueryEvaluationStep right = strategy.precompile(rightArg, context);

		return new LateralQueryEvaluationStep(left, right, node, context, strategy, visitor.toSubstitute);
	}

	private class LateralIteration extends LookAheadIteration<BindingSet, QueryEvaluationException> {
		private final CloseableIteration<BindingSet, QueryEvaluationException> leftIter;
		private CloseableIteration<BindingSet, QueryEvaluationException> rightIter = QueryEvaluationStep.EMPTY_ITERATION;
		private final Lateral lateral;

		public LateralIteration(CloseableIteration<BindingSet, QueryEvaluationException> leftIter, Lateral lateral) {
			this.leftIter = leftIter;
			this.lateral = lateral;
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
					substitute(lateral, next, right);
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

	private void substitute(Lateral lateral2, BindingSet next, QueryEvaluationStep right2) {
		toSubstitute.values().forEach(v -> v.accept(next));
	}

	private static final class InjectSubstitutableVars
			extends AbstractSimpleQueryModelVisitor<QueryEvaluationException> {
		private final Set<String> toBind;
		private final QueryEvaluationContext context;
		private final Map<String, VarThatSubsitutes> substitutions;
		private final Map<String, Consumer<BindingSet>> toSubstitute;

		private InjectSubstitutableVars(Set<String> toBind, QueryEvaluationContext context) {
			this.toBind = toBind;
			this.context = context;
			this.substitutions = new HashMap<>();
			this.toSubstitute = new HashMap<>();
		}

		@Override
		public void meet(BindingSetAssignment node) throws QueryEvaluationException {
			// TODO maybe throw an error as this might not be allowed if a var is defined.
			for (String varName : toBind) {
				if (node.getBindingNames().contains(varName)) {
					throw new QueryEvaluationException(varName + " from LATERAL left is redifined in it's right");
				}
			}
			super.meet(node);
		}

		@Override
		public void meet(Projection node) throws QueryEvaluationException {
			// We do not substitute into subqueries so we stop visiting here.
		}

		@Override
		public void meet(Var node) throws QueryEvaluationException {
			final String varName = node.getName();
			if (!node.isConstant() && !node.isAnonymous() && toBind.contains(varName)) {
				if (substitutions.containsKey(varName)) {
					node.replaceWith(substitutions.get(varName));
				} else {
					Predicate<BindingSet> hasValue = context.hasBinding(varName);
					Function<BindingSet, Value> getValue = context.getValue(varName);
					final VarThatSubsitutes replacement = new VarThatSubsitutes(varName, null);
					toSubstitute.put(varName, (bs) -> {
						if (hasValue.test(bs)) {
							replacement.value = getValue.apply(bs);
						}
					});
					node.replaceWith(replacement);
				}
			}
			super.meet(node);
		}
	}

	private static class VarThatSubsitutes extends Var {
		private static final long serialVersionUID = 1L;
		private Value value;

		public VarThatSubsitutes(String name, Value value) {
			super(name);
		}

		@Override
		public boolean hasValue() {
			return value != null;
		}

		@Override
		public Value getValue() {
			return value;
		}

		@Override
		public Var clone() {
			return new VarThatSubsitutes(getName(), value);
		}

	}
}
