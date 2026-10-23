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
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.algebra.FedXService;
import org.eclipse.rdf4j.federated.evaluation.FederationEvaluationStrategy;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.algebra.Service;
import org.eclipse.rdf4j.query.impl.SpilledBindingSetIteration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parallel executor for {@link FedXService} nodes, which wrap SERVICE expressions.
 *
 * Uses the union scheduler to execute the task
 *
 * @author Andreas Schwarte
 */
public class ParallelServiceExecutor extends LookAheadIteration<BindingSet>
		implements ParallelExecutor<BindingSet> {

	/*
	 * IMPLEMENTATION NOTE
	 *
	 * This class explicitly does not extend ParallelServiceExecutor: here the execution of the #run() is non blocking,
	 * i.g. blocking is done a consumption time of the iterator
	 */

	protected static final Logger log = LoggerFactory.getLogger(ParallelServiceExecutor.class);

	protected final FedXService service;
	protected final FederationEvaluationStrategy strategy;
	protected final BindingSet bindings;
	protected final FederationContext federationContext;

	protected CloseableIteration<BindingSet> rightIter = null;
	protected boolean finished = false;
	protected Exception error = null;

	private CountDownLatch latch = null;
	private volatile ParallelServiceTask scheduledTask;
	private boolean closing;

	/**
	 * @param service
	 * @param strategy
	 * @param bindings
	 * @param federationContext
	 */
	public ParallelServiceExecutor(FedXService service,
			FederationEvaluationStrategy strategy, BindingSet bindings, FederationContext federationContext) {
		super();
		this.service = service;
		this.strategy = strategy;
		this.bindings = bindings;
		this.federationContext = federationContext;
	}

	@Override
	public void run() {

		latch = new CountDownLatch(1);
		ParallelServiceTask task = new ParallelServiceTask();
		scheduledTask = task;
		synchronized (this) {
			if (closing) {
				latch.countDown();
				return;
			}
		}
		ControlledWorkerScheduler<BindingSet> scheduler = federationContext.getManager().getUnionScheduler();
		scheduler.schedule(task);
		boolean closeTask;
		synchronized (this) {
			closeTask = closing;
		}
		if (closeTask) {
			task.cancel();
		}
	}

	@Override
	public void addResult(CloseableIteration<BindingSet> res) {

		boolean closeResult;
		synchronized (this) {
			closeResult = closing;
			if (!closeResult) {
				rightIter = res;
			}
		}
		try {
			if (closeResult) {
				res.close();
			}
		} finally {
			latch.countDown();
		}
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

		CloseableIteration<BindingSet> current;
		synchronized (this) {
			if (closing) {
				return null;
			}
			current = rightIter;
		}
		if (current == null) {
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
			synchronized (this) {
				if (closing) {
					return null;
				}
				current = rightIter;
			}
		}

		// check again for error
		if (error != null) {
			if (error instanceof QueryEvaluationException) {
				throw (QueryEvaluationException) error;
			}
			throw new QueryEvaluationException(error);
		}

		if (current != null && current.hasNext()) {
			synchronized (this) {
				if (closing) {
					return null;
				}
			}
			return current.next();
		}

		return null;
	}

	@Override
	protected void handleClose() {
		CloseableIteration<BindingSet> published;
		synchronized (this) {
			closing = true;
			published = rightIter;
			rightIter = null;
		}
		try {
			ParallelServiceTask task = scheduledTask;
			if (task != null) {
				task.cancel();
			}
		} finally {
			try {
				if (published != null) {
					published.close();
				}
			} finally {
				CountDownLatch ready = latch;
				if (ready != null) {
					ready.countDown();
				}
			}
		}
	}

	/**
	 * Task for evaluating service requests
	 *
	 * @author Andreas Schwarte
	 */
	private class ParallelServiceTask extends ParallelTaskBase<BindingSet> {
		private final Object sourceLock = new Object();
		private boolean closing;
		private CloseableIteration<BindingSet> activeSource;

		@Override
		protected CloseableIteration<BindingSet> performTaskInternal() throws Exception {
			// Fully drain the SERVICE response before publishing it, releasing the remote HTTP slot in order.
			Service service1 = service.getService();
			CloseableIteration<BindingSet> evaluate = strategy.precompile(service1).evaluate(bindings);
			boolean closeSource;
			synchronized (sourceLock) {
				closeSource = closing;
				if (!closeSource) {
					activeSource = evaluate;
				}
			}
			if (closeSource) {
				evaluate.close();
				return new EmptyIteration<>();
			}
			try {
				return SpilledBindingSetIteration.materialize(evaluate);
			} finally {
				synchronized (sourceLock) {
					if (activeSource == evaluate) {
						activeSource = null;
					}
				}
			}
		}

		@Override
		public void close() {
			CloseableIteration<BindingSet> source;
			synchronized (sourceLock) {
				closing = true;
				source = activeSource;
				activeSource = null;
			}
			try {
				if (source != null) {
					source.close();
				}
			} finally {
				super.close();
			}
		}

		@Override
		public ParallelExecutor<BindingSet> getControl() {
			return ParallelServiceExecutor.this;
		}

	}
}
