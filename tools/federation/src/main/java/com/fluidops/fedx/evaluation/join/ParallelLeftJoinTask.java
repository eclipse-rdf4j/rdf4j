/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.evaluation.join;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.evaluation.FederationEvalStrategy;
import com.fluidops.fedx.evaluation.concurrent.ParallelExecutor;
import com.fluidops.fedx.evaluation.concurrent.ParallelTaskBase;

/**
 * A task implementation representing a join, i.e. the provided expression is evaluated 
 * with the given bindings.
 * 
 * @author Andreas Schwarte
 */
public class ParallelLeftJoinTask extends ParallelTaskBase<BindingSet> {
	
	static final Logger log = LoggerFactory.getLogger(ParallelLeftJoinTask.class);

	protected final FederationEvalStrategy strategy;
	protected final LeftJoin join;
	protected final BindingSet leftBindings;
	protected final ParallelExecutor<BindingSet> joinControl;
	
	public ParallelLeftJoinTask(ParallelExecutor<BindingSet> joinControl, FederationEvalStrategy strategy,
			LeftJoin join, BindingSet leftBindings) {
		this.strategy = strategy;
		this.join = join;
		this.leftBindings = leftBindings;
		this.joinControl = joinControl;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> performTask() throws Exception {

		return new FedXLeftJoinIteration(strategy, join, leftBindings);

	}

	@Override
	public ParallelExecutor<BindingSet> getControl() {
		return joinControl;
	}

	static class FedXLeftJoinIteration extends LookAheadIteration<BindingSet, QueryEvaluationException> {

		protected final FederationEvalStrategy strategy;

		private final BindingSet leftBindings;

		private final LeftJoin join;

		/**
		 * The set of binding names that are "in scope" for the filter. The filter must
		 * not include bindings that are (only) included because of the depth-first
		 * evaluation strategy in the evaluation of the constraint.
		 */
		private final Set<String> scopeBindingNames;


		private CloseableIteration<BindingSet, QueryEvaluationException> rightIter;

		private AtomicBoolean exhausted = new AtomicBoolean(false);

		public FedXLeftJoinIteration(FederationEvalStrategy strategy, LeftJoin join, BindingSet leftBindings) {
			super();
			this.strategy = strategy;
			this.join = join;
			this.leftBindings = leftBindings;
			this.scopeBindingNames = join.getBindingNames();

		}

		@Override
		protected BindingSet getNextElement() throws QueryEvaluationException {

			if (rightIter == null) {
				// lazy evaluation
				rightIter = strategy.evaluate(join.getRightArg(), leftBindings);

				if (!rightIter.hasNext() && !exhausted.getAndSet(true)) {
					return leftBindings;
				}
			}

			if (rightIter.hasNext()) {
				while (rightIter.hasNext()) {
					BindingSet rightBindings = rightIter.next();

					try {
						if (join.getCondition() == null) {
							return rightBindings;
						} else {
							// Limit the bindings to the ones that are in scope for
							// this filter
							QueryBindingSet scopeBindings = new QueryBindingSet(rightBindings);
							scopeBindings.retainAll(scopeBindingNames);

							if (strategy.isTrue(join.getCondition(), scopeBindings)) {
								return rightBindings;
							}
						}
					} catch (ValueExprEvaluationException e) {
						// Ignore, condition not evaluated successfully
					}
				}

				// join did not work
				if (leftBindings != null) {
					return leftBindings;
				}
			}

			return null;
		}

		@Override
		protected void handleClose() throws QueryEvaluationException {
			try {
				super.handleClose();
			} finally {
				if (rightIter != null) {
					rightIter.close();
				}
			}
		}

	}

}
