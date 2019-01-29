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
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

public class LeftJoinIterator extends LookAheadIteration<BindingSet, QueryEvaluationException> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final EvaluationStrategy strategy;

	private final LeftJoin join;

	/**
	 * The set of binding names that are "in scope" for the filter. The filter must not include bindings that
	 * are (only) included because of the depth-first evaluation strategy in the evaluation of the constraint.
	 */
	private final Set<String> scopeBindingNames;

	private final CloseableIteration<BindingSet, QueryEvaluationException> leftIter;

	private volatile CloseableIteration<BindingSet, QueryEvaluationException> rightIter;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public LeftJoinIterator(EvaluationStrategy strategy, LeftJoin join, BindingSet bindings)
		throws QueryEvaluationException
	{
		this.strategy = strategy;
		this.join = join;
		this.scopeBindingNames = join.getBindingNames();

		leftIter = strategy.evaluate(join.getLeftArg(), bindings);

		// Initialize with empty iteration so that var is never null
		rightIter = new EmptyIteration<>();
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected BindingSet getNextElement()
		throws QueryEvaluationException
	{
		try {
			CloseableIteration<BindingSet, QueryEvaluationException> nextRightIter = rightIter;
			while (nextRightIter.hasNext() || leftIter.hasNext()) {
				BindingSet leftBindings = null;

				if (!nextRightIter.hasNext()) {
					// Use left arg's bindings in case join fails
					leftBindings = leftIter.next();

					nextRightIter.close();
					nextRightIter = rightIter = strategy.evaluate(join.getRightArg(), leftBindings);
				}

				while (nextRightIter.hasNext()) {
					BindingSet rightBindings = nextRightIter.next();

					try {
						if (join.getCondition() == null) {
							return rightBindings;
						}
						else {
							// Limit the bindings to the ones that are in scope for
							// this filter
							QueryBindingSet scopeBindings = new QueryBindingSet(rightBindings);
							scopeBindings.retainAll(scopeBindingNames);

							if (strategy.isTrue(join.getCondition(), scopeBindings)) {
								return rightBindings;
							}
						}
					}
					catch (ValueExprEvaluationException e) {
						// Ignore, condition not evaluated successfully
					}
				}

				if (leftBindings != null) {
					// Join failed, return left arg's bindings
					return leftBindings;
				}
			}
		}
		catch (NoSuchElementException ignore) {
			// probably, one of the iterations has been closed concurrently in
			// handleClose()
		}

		return null;
	}

	@Override
	protected void handleClose()
		throws QueryEvaluationException
	{
		try {
			super.handleClose();
		}
		finally {
			try {
				leftIter.close();
			}
			finally {
				rightIter.close();
			}
		}
	}
}
