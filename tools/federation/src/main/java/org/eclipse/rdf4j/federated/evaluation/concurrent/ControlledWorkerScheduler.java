/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.concurrent;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.evaluation.join.ControlledWorkerBoundJoin;
import org.eclipse.rdf4j.federated.evaluation.join.ControlledWorkerJoin;
import org.eclipse.rdf4j.federated.evaluation.union.ControlledWorkerUnion;
import org.eclipse.rdf4j.federated.exception.ExceptionUtil;
import org.eclipse.rdf4j.federated.exception.FedXRuntimeException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ControlledWorkerScheduler is a task scheduler that uses a FIFO queue for managing its process. Each instance has a
 * pool with a fixed number of worker threads. Once notified a worker picks the next task from the queue and executes
 * it. The results is then returned to the controlling instance retrieved from the task.
 *
 *
 * @author Andreas Schwarte
 *
 * @see ControlledWorkerUnion
 * @see ControlledWorkerJoin
 * @see ControlledWorkerBoundJoin
 */
public class ControlledWorkerScheduler<T> implements Scheduler<T>, TaskWrapperAware {

	private static final Logger log = LoggerFactory.getLogger(ControlledWorkerScheduler.class);

	private final ExecutorService executor;

	private final LinkedBlockingQueue<Runnable> _taskQueue = new LinkedBlockingQueue<>();

	private final int nWorkers;
	private final String name;
	private TaskWrapper taskWrapper;

	/**
	 * Construct a new instance with 20 workers.
	 *
	 * @deprecated use {@link #ControlledWorkerScheduler(int, String)}. Scheduled to be removed in 4.0
	 */
	@Deprecated
	public ControlledWorkerScheduler() {
		this(20, "FedX Worker");
	}

	/**
	 * Construct a new instance with the specified number of workers and the given name.
	 *
	 * @param nWorkers
	 * @param name
	 */
	public ControlledWorkerScheduler(int nWorkers, String name) {
		this.nWorkers = nWorkers;
		this.name = name;
		this.executor = createExecutorService();
	}

	/**
	 * Schedule the specified parallel task.
	 *
	 * @param task the task to schedule
	 */
	@Override
	public void schedule(ParallelTask<T> task) {

		Runnable runnable = new WorkerRunnable(task);

		// Note: for specific use-cases the runnable may be wrapped (e.g. to allow injection of thread-contexts). By
		// default the unmodified runnable is used
		if (taskWrapper != null) {
			runnable = taskWrapper.wrap(runnable);
		}

		try {
			task.getQueryInfo().registerScheduledTask(task);
		} catch (Throwable e) {
			task.cancel();
			throw e;
		}

		Future<?> future = executor.submit(runnable);

		// register the future to the task
		if (task instanceof ParallelTaskBase<?>) {
			((ParallelTaskBase<?>) task).setScheduledFuture(future);
		}

		// TODO rejected execution exception?

	}

	/**
	 * Schedule the given tasks and inform about finish using the same lock, i.e. all tasks are scheduled one after the
	 * other.
	 *
	 * @param tasks
	 * @param control
	 */
	public void scheduleAll(List<ParallelTask<T>> tasks, ParallelExecutor<T> control) {
		for (ParallelTask<T> task : tasks) {
			schedule(task);
		}

	}

	public int getTotalNumberOfWorkers() {
		return nWorkers;
	}

	public int getNumberOfTasks() {
		return _taskQueue.size();
	}

	private ExecutorService createExecutorService() {

		ThreadPoolExecutor executor = new ThreadPoolExecutor(nWorkers, nWorkers, 60L, TimeUnit.SECONDS, _taskQueue,
				new NamingThreadFactory(name));
		executor.allowCoreThreadTimeOut(true);
		return executor;
	}

	@Override
	public void abort() {
		log.info("Aborting workers of " + name + ".");

		executor.shutdownNow();
		try {
			executor.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new FedXRuntimeException(e);
		}
	}

	@Override
	public void done() {
		/* not needed here, implementations call informFinish(control) to notify done status */
	}

	@Override
	public void handleResult(CloseableIteration<T, QueryEvaluationException> res) {
		/* not needed here since the result is passed directly to the control instance */
		throw new RuntimeException("Unsupported Operation for this scheduler.");
	}

	@Override
	public void informFinish() {
		throw new RuntimeException("Unsupported Operation for this scheduler!");
	}

	/**
	 * Inform this scheduler that the specified control instance will no longer submit tasks.
	 *
	 * @param control
	 */
	public void informFinish(ParallelExecutor<T> control) {

		// TODO
	}

	@Override
	public boolean isRunning() {
		/* Note: this scheduler can only determine runtime for a given control instance! */
		throw new RuntimeException("Unsupported Operation for this scheduler.");
	}

	/**
	 * Determine if there are still task running or queued for the specified control.
	 *
	 * @param control
	 *
	 * @return true, if there are unfinished tasks, false otherwise
	 */
	public boolean isRunning(ParallelExecutor<T> control) {
		return true; // TODO
	}

	@Override
	public void toss(Exception e) {
		/* not needed here: exceptions are directly tossed to the controlling instance */
		throw new RuntimeException("Unsupported Operation for this scheduler.");
	}

	class WorkerRunnable implements Runnable {

		private final ParallelTask<T> task;

		private volatile boolean aborted = false;

		public WorkerRunnable(ParallelTask<T> task) {
			super();
			this.task = task;
		}

		@Override
		public void run() {
			if (aborted) {
				return;
			}

			ParallelExecutor<T> taskControl = task.getControl();

			CloseableIteration<T, QueryEvaluationException> res = null;
			try {
				if (log.isTraceEnabled()) {
					log.trace("Performing task " + task.toString() + " in " + Thread.currentThread().getName());
				}
				res = task.performTask();
				taskControl.addResult(res);
				if (aborted) {
					res.close();
				}

				taskControl.done();
			} catch (Throwable t) {

				log.debug("Exception encountered while evaluating task (" + t.getClass().getSimpleName() + "): "
						+ t.getMessage());
				taskControl.toss(ExceptionUtil.toException(t));

				// e.g. interrupted
				if (res != null) {
					res.close();
				}
				task.cancel();

			}

		}

		public void abort() {
			this.aborted = true;
		}
	}

	/**
	 * Structure to maintain the status for a given control instance.
	 *
	 * @author Andreas Schwarte
	 */
	protected class ControlStatus {
		public int waiting;
		public boolean done;

		public ControlStatus(int waiting, boolean done) {
			this.waiting = waiting;
			this.done = done;
		}
	}

	@Override
	public void shutdown() {
		executor.shutdown();
		try {
			executor.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new FedXRuntimeException(e);
		}

	}

	@Override
	public void setTaskWrapper(TaskWrapper taskWrapper) {
		this.taskWrapper = taskWrapper;
	}
}
