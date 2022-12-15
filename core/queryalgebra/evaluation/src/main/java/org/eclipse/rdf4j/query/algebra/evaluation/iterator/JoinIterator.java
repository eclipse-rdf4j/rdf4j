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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Processor;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.SubmissionPublisher;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep.EvaluationStepSubmitter;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep.EvaluationStepSubscriberToIterator;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;

/**
 * Interleaved join iterator.
 * <p>
 * This join iterator produces results by interleaving results from its left argument into its right argument to speed
 * up bindings and produce fail-fast results. Note that this join strategy is only valid in cases where all bindings
 * from the left argument can be considered in scope for the right argument.
 *
 * @author Jeen Broekstra
 */
public class JoinIterator implements CloseableIteration<BindingSet, QueryEvaluationException> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final EvaluationStepSubmitter leftIter;

	private LeftToRightSubscriber subscriber;

	private EvaluationStepSubscriberToIterator evaluationStepSubscriberToIterator;

	private boolean submitted = false;
	/*--------------*
	 * Constructors *
	 *--------------*/

	public JoinIterator(EvaluationStrategy strategy, QueryEvaluationStep preparedLeft,
			QueryEvaluationStep preparedRight, Join join, BindingSet bindings) throws QueryEvaluationException {
		if (preparedLeft.canPublish()) {
			leftIter = preparedLeft.publisher(bindings);
		} else {
			leftIter = new EvaluationStepSubmitter(preparedLeft.evaluate(bindings));
		}

		// Initialize with empty iteration so that var is never null
		join.setAlgorithm(this);
		evaluationStepSubscriberToIterator = new EvaluationStepSubscriberToIterator();
		subscriber = new LeftToRightSubscriber(preparedRight, evaluationStepSubscriberToIterator);
		leftIter.getSubmissionPublisher().subscribe(subscriber);
	}

	public JoinIterator(EvaluationStrategy strategy, Join join, BindingSet bindings, QueryEvaluationContext context)
			throws QueryEvaluationException {
		this(strategy, strategy.precompile(join.getLeftArg(), context),
				strategy.precompile(join.getRightArg(), context), join, bindings);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public boolean hasNext() throws QueryEvaluationException {
		if (!submitted) {
			leftIter.submit();
			submitted = true;
		}
		return evaluationStepSubscriberToIterator.hasNext();
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		if (!submitted) {
			leftIter.submit();
			submitted = true;
		}
		return evaluationStepSubscriberToIterator.next();
	}

	@Override
	public void close() throws QueryEvaluationException {
		if (subscriber != null && subscriber.subscription != null) {
			subscriber.subscription.cancel();
		}
		evaluationStepSubscriberToIterator.close();
	}

	public class LeftToRightSubscriber extends SubmissionPublisher<BindingSet>
			implements Processor<BindingSet, BindingSet> {
		private final QueryEvaluationStep preparedRight;
		private volatile boolean complete;
		private volatile boolean closed;
		private volatile QueryEvaluationException exception;
		private Subscription subscription;
		private final EvaluationStepSubscriberToIterator evaluationStepSubscriberToIterator2;
		private SubmissionPublisher<BindingSet> submitter = new SubmissionPublisher<BindingSet>();

		public LeftToRightSubscriber(QueryEvaluationStep preparedRight,
				EvaluationStepSubscriberToIterator evaluationStepSubscriberToIterator) {
			super();
			this.preparedRight = preparedRight;
			evaluationStepSubscriberToIterator2 = evaluationStepSubscriberToIterator;
			final CompletableFuture<Void> consume = submitter.consume(evaluationStepSubscriberToIterator2.consumer());
			evaluationStepSubscriberToIterator2.setFuture(consume);
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			if (this.subscription != null)
				throw new IllegalStateException();
			this.subscription = subscription;
			this.subscription.request(1);
		}

		@Override
		public void onNext(BindingSet item) {
			try (CloseableIteration<BindingSet, QueryEvaluationException> evaluate = preparedRight.evaluate(item)) {
				while (evaluate.hasNext() && !closed) {
					submitter.submit(evaluate.next());
				}
			}
			this.subscription.request(1);
		}

		@Override
		public void onError(Throwable throwable) {
			if (throwable instanceof QueryEvaluationException) {
				exception = (QueryEvaluationException) throwable;
			} else {
				exception = new QueryEvaluationException(throwable);
			}
			evaluationStepSubscriberToIterator2.setException(exception);
			complete = true;
			if (subscription != null) {
				subscription.cancel();
			}
		}

		@Override
		public void onComplete() {
			submitter.close();
			complete = true;
		}
	}

	@Override
	public void remove() throws QueryEvaluationException {
		throw new UnsupportedOperationException();
	}
}
