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

import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;

/**
 * @author Arjohn Kampman
 */
public class BadlyDesignedLeftJoinIterator extends LookAheadIteration<BindingSet> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final BindingSet inputBindings;

	private final Set<String> problemVars;

	private final CloseableIteration<BindingSet> leftIter;

	private final QueryEvaluationStep rightEvaluationStep;

	private CloseableIteration<BindingSet> rightIter;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public BadlyDesignedLeftJoinIterator(
			EvaluationStrategy strategy,
			LeftJoin join,
			BindingSet inputBindings,
			Set<String> problemVars,
			QueryEvaluationStep rightEvaluationStep) throws QueryEvaluationException {
		leftIter = strategy.evaluate(join.getLeftArg(), getFilteredBindings(inputBindings, problemVars));
		this.rightEvaluationStep = rightEvaluationStep;
		rightIter = null;
		join.setAlgorithm(this);
		this.inputBindings = inputBindings;
		this.problemVars = problemVars;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public BadlyDesignedLeftJoinIterator(QueryEvaluationStep left,
			BindingSet inputBindings,
			Set<String> problemVars,
			QueryEvaluationStep rightEvaluationStep)
			throws QueryEvaluationException {
		leftIter = left.evaluate(getFilteredBindings(inputBindings, problemVars));
		this.rightEvaluationStep = rightEvaluationStep;
		rightIter = null;
		this.inputBindings = inputBindings;
		this.problemVars = problemVars;
	}

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		BindingSet result = getNextLeftJoinElement();

		// Ignore all results that are not compatible with the input bindings
		while (result != null && !inputBindings.isCompatible(result)) {
			result = getNextLeftJoinElement();
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

	private BindingSet getNextLeftJoinElement() throws QueryEvaluationException {

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

	/*--------------------*
	 * Static util method *
	 *--------------------*/

	private static QueryBindingSet getFilteredBindings(BindingSet bindings, Set<String> problemVars) {
		QueryBindingSet filteredBindings = new QueryBindingSet(bindings);
		filteredBindings.removeAll(problemVars);
		return filteredBindings;
	}
}
