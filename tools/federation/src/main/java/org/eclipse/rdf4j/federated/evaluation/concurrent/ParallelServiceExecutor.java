/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.algebra.FedXService;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.repository.sparql.federation.CollectionIteration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parallel executor for {@link FedXService} nodes, which wrap SERVICE expressions.
 *
 * Uses the union scheduler to execute the task
 *
 * @author Andreas Schwarte
 */
public class ParallelServiceExecutor extends LookAheadIteration<BindingSet, QueryEvaluationException>
		implements ParallelExecutor<BindingSet> {

	/*
	 * IMPLEMENTATION NOTE
	 *
	 * This class explicitly does not extend ParallelServiceExecutor: here the execution of the #run() is non blocking,
	 * i.g. blocking is done a consumption time of the iterator
	 */

	protected static final Logger log = LoggerFactory.getLogger(ParallelServiceExecutor.class);

	protected final FedXService service;
	protected final FederationEvalStrategy strategy;
	protected final BindingSet bindings;
	protected final FederationContext federationContext;

	protected CloseableIteration<BindingSet, QueryEvaluationException> rightIter = null;
	protected boolean finished = false;
	protected Exception error = null;

	private CountDownLatch latch = null;

	/**
	 * @param service
	 * @param strategy
	 * @param bindings
	 * @param federationContext
	 */
	public ParallelServiceExecutor(FedXService service,
			FederationEvalStrategy strategy, BindingSet bindings, FederationContext federationContext) {
		super();
		this.service = service;
		this.strategy = strategy;
		this.bindings = bindings;
		this.federationContext = federationContext;
	}

	@Override
	public void run() {

		latch = new CountDownLatch(1);
		ControlledWorkerScheduler<BindingSet> scheduler = federationContext.getManager().getUnionScheduler();
		scheduler.schedule(new ParallelServiceTask());
	}

	@Override
	public void addResult(CloseableIteration<BindingSet, QueryEvaluationException> res) {

		rightIter = res;
		latch.countDown();
	}

	@Override
	public void toss(Exception e) {

		error = e;
		latch.countDown();
	}

	@Override
	public void done() {
		// no-op
	}

	@Override
	public boolean isFinished() {
		synchronized (this) {
			return finished;
		}
	}

	@Override
	public QueryInfo getQueryInfo() {
		return service.getQueryInfo();
	}

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {

		// error resulting from TOSS
		if (error != null) {
			if (error instanceof QueryEvaluationException) {
				throw (QueryEvaluationException) error;
			}
			throw new QueryEvaluationException(error);
		}

		if (rightIter == null) {
			// block if not evaluated
			try {
				boolean completed = latch.await(getQueryInfo().getMaxRemainingTimeMS(), TimeUnit.MILLISECONDS);
				if (!completed) {
					throw new QueryInterruptedException("Timeout during service evaluation");
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.debug("Error while evaluating service expression. Thread got interrupted.");
				error = e;
			}
		}

		// check again for error
		if (error != null) {
			if (error instanceof QueryEvaluationException) {
				throw (QueryEvaluationException) error;
			}
			throw new QueryEvaluationException(error);
		}

		if (rightIter.hasNext()) {
			return rightIter.next();
		}

		return null;
	}

	/**
	 * Task for evaluating service requests
	 *
	 * @author Andreas Schwarte
	 */
	private class ParallelServiceTask extends ParallelTaskBase<BindingSet> {

		@Override
		protected CloseableIteration<BindingSet, QueryEvaluationException> performTaskInternal() throws Exception {

			// Note: in order two avoid deadlocks we consume the SERVICE result.
			// This is basically required to avoid processing background tuple
			// request (i.e. HTTP slots) in the correct order.
			return new CollectionIteration<>(Iterations.asList(strategy.evaluate(service.getService(), bindings)));
		}

		@Override
		public ParallelExecutor<BindingSet> getControl() {
			return ParallelServiceExecutor.this;
		}

	}
}
