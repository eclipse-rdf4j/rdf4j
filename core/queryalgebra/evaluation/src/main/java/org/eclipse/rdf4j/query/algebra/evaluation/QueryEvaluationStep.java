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
package org.eclipse.rdf4j.query.algebra.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow.Processor;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Phaser;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.DelayedIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * A Step that may need to be executed in a EvaluationStrategy. The evaluate method should do the minimal work required
 * to evaluate given the bindings. As much as possible should be pre-computed (e.g. resolving constant values)
 */
@FunctionalInterface
public interface QueryEvaluationStep {
	/**
	 * Utility class that removes code duplication and makes a precompiled QueryEvaluationStep available as an iteration
	 * that may be created and used later.
	 */
	@Deprecated(since = "4.1.0", forRemoval = true)
	class DelayedEvaluationIteration extends DelayedIteration<BindingSet, QueryEvaluationException> {
		private final QueryEvaluationStep arg;
		private final BindingSet bs;

		public DelayedEvaluationIteration(QueryEvaluationStep arg, BindingSet bs) {
			this.arg = arg;
			this.bs = bs;
		}

		@Override
		protected CloseableIteration<? extends BindingSet, ? extends QueryEvaluationException> createIteration()
				throws QueryEvaluationException {
			return arg.evaluate(bs);
		}
	}

	EmptyIteration<BindingSet, QueryEvaluationException> EMPTY_ITERATION = new EmptyIteration<>();
	QueryEvaluationStep EMPTY = bindings -> EMPTY_ITERATION;

	CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings);

	/**
	 * A fall back implementation that wraps a pre-existing evaluate method on a strategy
	 *
	 * @param strategy that can evaluate the tuple expr.
	 * @param expr     that is going to be evaluated
	 * @return a thin wrapper arround the evaluation call.
	 */
	static QueryEvaluationStep minimal(EvaluationStrategy strategy, TupleExpr expr) {
		return bs -> strategy.evaluate(expr, bs);
	}

	static QueryEvaluationStep empty() {
		return EMPTY;
	}

	public default boolean canPublish() {
		return false;
	}

	public default EvaluationStepSubmitter publisher(BindingSet initialValues) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Wrap an QueryEvalationStep: where we apply a function on every evaluation result of the wrapped EvaluationStep.
	 * Useful to add a timing function
	 *
	 * @param qes  an QueryEvaluationStep that needs to return modified evaluation results
	 * @param wrap the function that will do the modification
	 * @return a new evaluation step that executes wrap on the inner qes.
	 */
	static QueryEvaluationStep wrap(QueryEvaluationStep qes,
			Function<CloseableIteration<BindingSet, QueryEvaluationException>, CloseableIteration<BindingSet, QueryEvaluationException>> wrap) {
		return bs -> wrap.apply(qes.evaluate(bs));
	}

	public static class EvaluationStepSubmitter {
		private final CloseableIteration<BindingSet, QueryEvaluationException> iter;
		private final SubmissionPublisher<BindingSet> submitter;

		public EvaluationStepSubmitter(CloseableIteration<BindingSet, QueryEvaluationException> iter) {
			super();
			this.iter = iter;
			this.submitter = new SubmissionPublisher<BindingSet>();
		}

		public void submit() {
			while (iter.hasNext()) {
				submitter.submit(iter.next());
			}
			iter.close();
			submitter.close();
		}

		public SubmissionPublisher<BindingSet> getSubmissionPublisher() {
			return submitter;
		}
	}

	public static class EvaluationStepSubscriberToIterator
			implements CloseableIteration<BindingSet, QueryEvaluationException> {
		private volatile boolean closed;
		private volatile QueryEvaluationException exception;
		private final BlockingQueue<BindingSet> next = new ArrayBlockingQueue<>(2);
		private CompletableFuture<Void> consume;

		public EvaluationStepSubscriberToIterator() {
			super();
		}

		@Override
		public boolean hasNext() throws QueryEvaluationException {
			if (next.peek() != null)
				return true;
			while (!closed && exception == null) {
				if (next.peek() != null) {
					return true;
				} else if (consume.isDone()) {
					if (next.peek() != null) {
						return true;
					} else {
						return false;
					}
				}
				Thread.onSpinWait();
			}
			if (exception != null)
				throw exception;
			return false;
		}

		@Override
		public BindingSet next() throws QueryEvaluationException {
			if (exception != null)
				throw exception;
			while (true) {
				try {
					BindingSet next2 = next.take();
					return next2;
				} catch (InterruptedException e) {
					Thread.interrupted();
				}
			}
		}

		@Override
		public void remove() throws QueryEvaluationException {
			throw new QueryEvaluationException(new UnsupportedOperationException());
		}

		public void setException(QueryEvaluationException exception2) {
			this.exception = exception2;
		}

		public Consumer<? super BindingSet> consumer() {
			return (bs) -> {
				while (!closed) {
					try {
						if (next.offer(bs, 20, TimeUnit.MILLISECONDS)) {
							return;
						}
					} catch (InterruptedException e) {
						Thread.interrupted();
					}
				}
			};
		}

		public void setFuture(CompletableFuture<Void> consume) {
			this.consume = consume;
		}

		@Override
		public void close() throws QueryEvaluationException {
			this.closed = true;
		}
	}
}
