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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;
import org.eclipse.rdf4j.query.algebra.helpers.collectors.VarNameCollector;

public class LeftJoinIterator extends LookAheadIteration<BindingSet> {
	/*-----------*
	 * Variables *
	 *-----------*/

	private final CloseableIteration<BindingSet> leftIter;
	private final QueryEvaluationStep rightEvaluationStep;

	private CloseableIteration<BindingSet> rightIter;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public LeftJoinIterator(EvaluationStrategy strategy, LeftJoin join, BindingSet bindings,
			QueryEvaluationContext context)
			throws QueryEvaluationException {
		Set<String> scopeBindingNames = join.getBindingNames();

		leftIter = strategy.evaluate(join.getLeftArg(), bindings);

		rightIter = null;

		QueryEvaluationStep prepareRightArg = strategy.precompile(join.getRightArg(), context);
		join.setAlgorithm(this);
		var joinCondition = Optional.ofNullable(join.getCondition())
				.map(condition -> strategy.precompile(condition, context));

		rightEvaluationStep = determineRightEvaluationStep(join,
														   prepareRightArg,
														   joinCondition.orElse(null),
														   scopeBindingNames);
	}

	public LeftJoinIterator(QueryEvaluationStep left, QueryEvaluationStep right, QueryValueEvaluationStep joinCondition,
			BindingSet bindings, Set<String> scopeBindingNames, LeftJoin leftJoin)
			throws QueryEvaluationException {
		this(left.evaluate(bindings),
			 determineRightEvaluationStep(leftJoin, right, joinCondition, scopeBindingNames));
	}

	public LeftJoinIterator(CloseableIteration<BindingSet> leftIter,
							QueryEvaluationStep rightEvaluationStep) {
		this.leftIter = leftIter;
		this.rightIter = null;
		this.rightEvaluationStep = rightEvaluationStep;
	}

	public static CloseableIteration<BindingSet> getInstance(QueryEvaluationStep left,
															 QueryEvaluationStep prepareRightArg,
															 QueryValueEvaluationStep joinCondition,
															 BindingSet bindings,
															 Set<String> scopeBindingNames,
															 LeftJoin leftJoin) {

		CloseableIteration<BindingSet> leftIter = left.evaluate(bindings);
		var rightEvaluationStep = determineRightEvaluationStep(leftJoin,
														   prepareRightArg,
														   joinCondition,
														   scopeBindingNames);

		if (leftIter == QueryEvaluationStep.EMPTY_ITERATION) {
			return leftIter;
		} else {
			return new LeftJoinIterator(leftIter, rightEvaluationStep);
		}
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {

		try {
			CloseableIteration<BindingSet> nextRightIter = rightIter;
			while (nextRightIter == null || nextRightIter.hasNext() || leftIter.hasNext()) {
				BindingSet leftBindings = null;

				if (nextRightIter == null) {
					if (leftIter.hasNext()) {
						// Use left arg's bindings in case join fails
						leftBindings = leftIter.next();
						nextRightIter = rightIter = rightEvaluationStep.evaluate(leftBindings);
					} else {
						return null;
					}

				} else if (!nextRightIter.hasNext()) {
					// Use left arg's bindings in case join fails
					leftBindings = leftIter.next();

					nextRightIter.close();
					nextRightIter = rightIter = rightEvaluationStep.evaluate(leftBindings);
				}

				if (nextRightIter == QueryEvaluationStep.EMPTY_ITERATION) {
					rightIter = null;
					return leftBindings;
				}

				if (nextRightIter.hasNext()) {
					return nextRightIter.next();
				}

				if (leftBindings != null) {
					rightIter = null;
					// Join failed, return left arg's bindings
					return leftBindings;
				}
			}
		} catch (NoSuchElementException ignore) {
			// probably, one of the iterations has been closed concurrently in
			// handleClose()
		}

		return null;
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		try {
			leftIter.close();
		} finally {
			if (rightIter != null) {
				rightIter.close();
			}
		}
	}

	static QueryEvaluationStep determineRightEvaluationStep(LeftJoin join,
																	QueryEvaluationStep prepareRightArg,
																	QueryValueEvaluationStep joinCondition,
																	Set<String> scopeBindingNames) {
		if (canEvaluateConditionBasedOnLeftHandSide(join)) {
			return new PreFilterQueryEvaluationStep(prepareRightArg,
													join.getAssuredBindingNames(),
													joinCondition);
		} else {
			return new PostFilterQueryEvaluationStep(prepareRightArg,
													 joinCondition,
													 scopeBindingNames);
		}
	}

	private static boolean canEvaluateConditionBasedOnLeftHandSide(LeftJoin leftJoin) {
		if (leftJoin.hasCondition()) {
			var collector = new VarNameCollector();
			leftJoin.getCondition().visit(collector);

			Set<String> assuredBindingNames = leftJoin.getAssuredBindingNames();
			return assuredBindingNames.containsAll(collector.getVarNames());
		}

		return false;
	}

	static class JoinConditionEvaluator {

		private JoinConditionEvaluator() {
			// Util
		}

		static boolean evaluate(QueryValueEvaluationStep joinCondition, BindingSet bindings) {
			try {
				Value value = joinCondition.evaluate(bindings);
				return QueryEvaluationUtility.getEffectiveBooleanValue(value).orElse(false);
			} catch (ValueExprEvaluationException e) {
				// Ignore, condition not evaluated successfully
				return false;
			}
		}
	}

	static class PreFilterQueryEvaluationStep implements QueryEvaluationStep {

		private final QueryEvaluationStep wrapped;
		private final Set<String> leftAssuredBindingNames;
		private final QueryValueEvaluationStep joinCondition;

        PreFilterQueryEvaluationStep(QueryEvaluationStep wrapped,
									 Set<String> leftAssuredBindingNames,
									 QueryValueEvaluationStep joinCondition) {
            this.wrapped = wrapped;
            this.leftAssuredBindingNames = new HashSet<>(leftAssuredBindingNames);
            this.joinCondition = joinCondition;
        }

        @Override
		public CloseableIteration<BindingSet> evaluate(BindingSet leftBindings) {
			if (shouldEvaluate(leftBindings)) {
				return wrapped.evaluate(leftBindings);
			}

			return QueryEvaluationStep.EMPTY_ITERATION;
		}

		private boolean shouldEvaluate(BindingSet leftBindings) {
			QueryBindingSet scopeBindings = new QueryBindingSet(leftAssuredBindingNames.size());
			for (String scopeBindingName : leftAssuredBindingNames) {
				Binding binding = leftBindings.getBinding(scopeBindingName);
				if (binding != null) {
					scopeBindings.addBinding(binding);
				}
			}

			return JoinConditionEvaluator.evaluate(joinCondition, scopeBindings);
		}
	}

	static class PostFilterQueryEvaluationStep implements QueryEvaluationStep {

		/**
		 * The set of binding names that are "in scope" for the filter. The filter must not include bindings that are (only)
		 * included because of the depth-first evaluation strategy in the evaluation of the constraint.
		 */
		private final Set<String> scopeBindingNames;
		private final QueryEvaluationStep wrapped;
		private final QueryValueEvaluationStep joinCondition;

		PostFilterQueryEvaluationStep(QueryEvaluationStep wrapped,
                                      QueryValueEvaluationStep joinCondition,
									  Set<String> scopeBindingNames) {
			this.wrapped = wrapped;
			this.joinCondition = joinCondition;
            this.scopeBindingNames = scopeBindingNames;
        }

		@Override
		public CloseableIteration<BindingSet> evaluate(BindingSet leftBindings) {
			var rightIteration = wrapped.evaluate(leftBindings);

			if (rightIteration == QueryEvaluationStep.EMPTY_ITERATION) {
				return rightIteration;
			}

			return new FilterIteration<>(rightIteration) {

				@Override
				protected boolean accept(BindingSet bindings) {
					if (joinCondition == null) {
						return true;
					}

					QueryBindingSet scopeBindings = new QueryBindingSet(scopeBindingNames.size());
					for (String scopeBindingName : scopeBindingNames) {
						Binding binding = bindings.getBinding(scopeBindingName);
						if (binding != null) {
							scopeBindings.addBinding(binding);
						}
					}

					return JoinConditionEvaluator.evaluate(joinCondition, scopeBindings);
				}

				@Override
				protected void handleClose() {
					rightIteration.close();
				}
			};
		}
	}
}
