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
package com.fluidops.fedx.evaluation.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.FederationManager;
import com.fluidops.fedx.algebra.FedXService;
import com.fluidops.fedx.evaluation.FederationEvalStrategy;
import com.fluidops.fedx.structures.QueryInfo;


/**
 * Parallel executor for {@link FedXService} nodes, which wrap SERVICE expressions.
 * 
 * Uses the union scheduler to execute the task
 * 
 * @author Andreas Schwarte
 */
public class ParallelServiceExecutor extends LookAheadIteration<BindingSet, QueryEvaluationException> implements ParallelExecutor<BindingSet> {
	
	/*
	 * IMPLEMENTATION NOTE
	 * 
	 * This class explicitly does not extend ParallelServiceExecutor: here the
	 * execution of the #run() is non blocking, i.g. blocking is done a consumption
	 * time of the iterator
	 */

	protected static final Logger log = LoggerFactory.getLogger(ParallelServiceExecutor.class);
	
	protected final FedXService service;
	protected final FederationEvalStrategy strategy;
	protected final BindingSet bindings;
	
	protected CloseableIteration<BindingSet, QueryEvaluationException> rightIter = null;
	protected boolean finished = false;
	protected Exception error = null;
	
	private CountDownLatch latch = null;

	/**
	 * @param service
	 * @param strategy
	 * @param bindings
	 */
	public ParallelServiceExecutor(FedXService service,
			FederationEvalStrategy strategy, BindingSet bindings) {
		super();
		this.service = service;
		this.strategy = strategy;
		this.bindings = bindings;
	}

	
	@Override
	public void run() {

		latch = new CountDownLatch(1);
		ControlledWorkerScheduler<BindingSet> scheduler = FederationManager.getInstance().getUnionScheduler();
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
	public void done()	{
		;	// no-op		
	}

	@Override
	public boolean isFinished()	{
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
		if (error!=null) {
			if (error instanceof QueryEvaluationException)
				throw (QueryEvaluationException)error;
			throw new QueryEvaluationException(error);
		}

		if (rightIter==null) {	
			// block if not evaluated
			try {
				boolean completed = latch.await(getQueryInfo().getMaxRemainingTimeMS(), TimeUnit.MILLISECONDS);
				if (!completed) {
					throw new QueryInterruptedException("Timeout during service evaluation");
				}
			} catch (InterruptedException e) {
				log.debug("Error while evaluating service expression. Thread got interrupted.");
				error = e;
			}
		}
		
		// check again for error
		if (error != null) {
			if (error instanceof QueryEvaluationException)
				throw (QueryEvaluationException) error;
			throw new QueryEvaluationException(error);
		}

		if (rightIter.hasNext())
			return rightIter.next();
		
		return null;
	}


	
	/**
	 * Task for evaluating service requests
	 * 
	 * @author Andreas Schwarte
	 */
	private class ParallelServiceTask extends ParallelTaskBase<BindingSet> {
	

		@Override
		public CloseableIteration<BindingSet, QueryEvaluationException> performTask()
				throws Exception {
			return strategy.evaluate(service.getService(), bindings);
		}

		@Override
		public ParallelExecutor<BindingSet> getControl() {
			return ParallelServiceExecutor.this;
		}
		
	}
}
