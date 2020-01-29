/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.concurrent;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.evaluation.join.JoinExecutorBase;
import org.eclipse.rdf4j.federated.evaluation.union.UnionExecutorBase;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * Interface for any parallel cursor, i.e. result iterations. Implementations can act as control for scheduler
 * implementations, e.g. {@link ControlledWorkerScheduler}. The common use case is to pass results from the scheduler to
 * the controlling result iteration.
 * 
 * @author Andreas Schwarte
 * 
 * @see JoinExecutorBase
 * @see UnionExecutorBase
 */
public interface ParallelExecutor<T> extends Runnable {

	/**
	 * Handle the result appropriately, e.g. add it to the result iteration. Take care for synchronization in a
	 * multithreaded environment
	 * 
	 * @param res
	 */
	public void addResult(CloseableIteration<T, QueryEvaluationException> res);

	/**
	 * Toss some exception to the controlling instance
	 * 
	 * @param e
	 */
	public void toss(Exception e);

	/**
	 * Inform the controlling instance that some job is done from a different thread. In most cases this is a no-op.
	 */
	public void done();

	/**
	 * Return true if this executor is finished or aborted
	 * 
	 * @return whether the execution is finished
	 */
	public boolean isFinished();

	/**
	 * Return the query info of the associated query
	 * 
	 * @return the query info
	 */
	public QueryInfo getQueryInfo();
}
