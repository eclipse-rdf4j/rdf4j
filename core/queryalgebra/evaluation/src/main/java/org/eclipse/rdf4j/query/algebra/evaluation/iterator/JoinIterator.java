/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.helpers.TupleExprs;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;

public class JoinIterator extends LookAheadIteration<BindingSet, QueryEvaluationException> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final EvaluationStrategy strategy;

	private final Join join;

	private final CloseableIteration<BindingSet, QueryEvaluationException> leftIter;

	private volatile CloseableIteration<BindingSet, QueryEvaluationException> rightIter;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public JoinIterator(EvaluationStrategy strategy, Join join, BindingSet bindings) throws QueryEvaluationException {
		this.strategy = strategy;
		this.join = join;

		leftIter = strategy.evaluate(join.getLeftArg(), bindings);

		// Initialize with empty iteration so that var is never null
		rightIter = new EmptyIteration<>();
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		try {
			while (rightIter.hasNext() || leftIter.hasNext()) {
				if (rightIter.hasNext()) {
					return rightIter.next();
				}

				// Right iteration exhausted
				rightIter.close();

				if (leftIter.hasNext()) {
					TupleExpr rightArg = join.getRightArg();
					if (isOutOfScopeForLeftArgBindings(rightArg)) {
						// leftiter bindings are out of scope for the right arg, so we merge afterward.
						BindingSet next = leftIter.next();
						rightIter = new MergeIteration(next, new BindingSetFilterIteration(next,
								strategy.evaluate(rightArg, new EmptyBindingSet())));
					} else {
						rightIter = strategy.evaluate(rightArg, leftIter.next());
					}
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
			super.handleClose();
		} finally {
			try {
				leftIter.close();
			} finally {
				rightIter.close();
			}
		}
	}

	private boolean isOutOfScopeForLeftArgBindings(TupleExpr expr) {
		if (expr instanceof Union) {
			return true;
		}
		return TupleExprs.isGraphPatternGroup(expr) && !(expr instanceof Filter);
	}

	private class MergeIteration extends LookAheadIteration<BindingSet, QueryEvaluationException> {

		private BindingSet bindingSet;
		private CloseableIteration<BindingSet, QueryEvaluationException> iter;

		public MergeIteration(BindingSet mergeBS, CloseableIteration<BindingSet, QueryEvaluationException> iter) {
			this.bindingSet = mergeBS;
			this.iter = iter;

		}

		/**
		 * Merge each sequence from the wrapped iterator with the provided BindingSet
		 */
		@Override
		protected BindingSet getNextElement() throws QueryEvaluationException {
			if (!iter.hasNext()) {
				return null;
			}

			BindingSet bs = iter.next();
			QueryBindingSet result = new QueryBindingSet(bs);
			for (Binding b : bindingSet) {
				if (!result.hasBinding(b.getName())) {
					result.addBinding(b);
				}
			}
			return result;
		}

		@Override
		protected void handleClose() {
			super.handleClose();
			iter.close();
		}

	}

	private class BindingSetFilterIteration extends FilterIteration<BindingSet, QueryEvaluationException> {

		private BindingSet bindingSet;

		public BindingSetFilterIteration(BindingSet bindingSet,
				CloseableIteration<BindingSet, QueryEvaluationException> iteration) {
			super(iteration);
			this.bindingSet = bindingSet;
		}

		/**
		 * Filter out sequences where any bindings conflict with bindings in the provided bindingSet
		 */
		@Override
		protected boolean accept(BindingSet toBeFiltered) throws QueryEvaluationException {
			for (Binding b : bindingSet) {
				Value v = b.getValue();
				String name = b.getName();
				if (toBeFiltered.hasBinding(name)) {
					if (!toBeFiltered.getValue(name).equals(v)) {
						return false;
					}
				}
			}
			return true;
		}
	}
}
