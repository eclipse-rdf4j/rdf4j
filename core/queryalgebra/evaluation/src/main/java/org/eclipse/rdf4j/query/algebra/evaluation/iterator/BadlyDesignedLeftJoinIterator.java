/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;

/**
 * @author Arjohn Kampman
 */
public class BadlyDesignedLeftJoinIterator extends LookAheadIteration<BindingSet, QueryEvaluationException> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final BindingSet inputBindings;

	private final Set<String> problemVars;
	/**
	 * The set of binding names that are "in scope" for the filter. The filter must not include bindings that are (only)
	 * included because of the depth-first evaluation strategy in the evaluation of the constraint.
	 */
	private final Set<String> scopeBindingNames;
	private final CloseableIteration<? extends BindingSet, QueryEvaluationException> leftIter;
	private final QueryEvaluationStep prepareRightArg;
	private final QueryValueEvaluationStep joinCondition;
	private CloseableIteration<? extends BindingSet, QueryEvaluationException> rightIter;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public BadlyDesignedLeftJoinIterator(EvaluationStrategy strategy, LeftJoin join, BindingSet inputBindings,
			Set<String> problemVars, QueryEvaluationContext context) throws QueryEvaluationException {
		this.scopeBindingNames = join.getBindingNames();

		this.leftIter = strategy.evaluate(join.getLeftArg(), getFilteredBindings(inputBindings, problemVars));

		// Initialize with empty iteration so that var is never null
		this.rightIter = null;

		this.prepareRightArg = strategy.precompile(join.getRightArg(), context);
		join.setAlgorithm(this);
		final ValueExpr condition = join.getCondition();
		if (condition == null) {
			this.joinCondition = null;
		} else {
			this.joinCondition = strategy.precompile(condition, context);
		}
		this.inputBindings = inputBindings;
		this.problemVars = problemVars;

	}

	/*---------*
	 * Methods *
	 *---------*/

	public BadlyDesignedLeftJoinIterator(QueryEvaluationStep left, QueryEvaluationStep right,
			QueryValueEvaluationStep joinCondition, BindingSet inputBindings, Set<String> problemVars)
			throws QueryEvaluationException {
		this.scopeBindingNames = problemVars;

		this.leftIter = left.evaluate(getFilteredBindings(inputBindings, problemVars));

		// Initialize with empty iteration so that var is never null
		this.rightIter = null;

		this.prepareRightArg = right;
		this.joinCondition = joinCondition;

		this.inputBindings = inputBindings;
		this.problemVars = problemVars;
	}

	/*--------------------*
	 * Static util method *
	 *--------------------*/

	private static QueryBindingSet getFilteredBindings(BindingSet bindings, Set<String> problemVars) {
		QueryBindingSet filteredBindings = new QueryBindingSet(bindings);
		filteredBindings.removeAll(problemVars);
		return filteredBindings;
	}

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		BindingSet result = innerGetNextElement();

		// Ignore all results that are not compatible with the input bindings
		while (result != null && !QueryResults.bindingSetsCompatible(inputBindings, result)) {
			result = innerGetNextElement();
		}

		if (result != null) {
			// Make sure the provided problemVars are part of the returned results
			// (necessary in case of e.g. LeftJoin and Union arguments)
			MutableBindingSet extendedResult = null;

			for (String problemVar : problemVars) {
				if (!result.hasBinding(problemVar)) {
					if (extendedResult == null) {
						extendedResult = new QueryBindingSet(result);
					}
					extendedResult.addBinding(problemVar, inputBindings.getValue(problemVar));
				}
			}

			if (extendedResult != null) {
				result = extendedResult;
			}
		}

		return result;
	}

	protected BindingSet innerGetNextElement() throws QueryEvaluationException {
		try {
			CloseableIteration<? extends BindingSet, QueryEvaluationException> nextRightIter = rightIter;
			while ((nextRightIter != null && nextRightIter.hasNext()) || leftIter.hasNext()) {
				BindingSet leftBindings = null;

				if (nextRightIter == null || !nextRightIter.hasNext()) {
					// Use left arg's bindings in case join fails
					leftBindings = leftIter.next();

					if (nextRightIter != null)
						nextRightIter.close();
					nextRightIter = rightIter = prepareRightArg.evaluate(leftBindings);
				}

				while (nextRightIter != null && nextRightIter.hasNext()) {
					BindingSet rightBindings = nextRightIter.next();

					try {
						if (joinCondition == null) {
							return rightBindings;
						} else {
							// Limit the bindings to the ones that are in scope for
							// this filter
							QueryBindingSet scopeBindings = new QueryBindingSet(rightBindings);
							scopeBindings.retainAll(scopeBindingNames);

							if (isTrue(joinCondition, scopeBindings)) {
								return rightBindings;
							}
						}
					} catch (ValueExprEvaluationException e) {
						// Ignore, condition not evaluated successfully
					}
				}

				if (leftBindings != null) {
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

	private boolean isTrue(QueryValueEvaluationStep expr, QueryBindingSet bindings) {
		Value value = expr.evaluate(bindings);
		return QueryEvaluationUtility.getEffectiveBooleanValue(value).orElse(false);
	}

	@Override
	protected final void handleClose() throws QueryEvaluationException {
		try {
			leftIter.close();
		} finally {
			if (rightIter != null)
				rightIter.close();
		}
	}
}
