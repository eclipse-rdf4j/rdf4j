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
package org.eclipse.rdf4j.common.iteration;

import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Arjohn Kampman
 */
public abstract class TimeLimitIteration<E> extends IterationWrapper<E> {

	private static final Timer timer = new Timer("TimeLimitIteration", true);

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final InterruptTask<E> interruptTask;

	private final AtomicBoolean isInterrupted = new AtomicBoolean(false);

	protected TimeLimitIteration(CloseableIteration<? extends E> iter, long timeLimit) {
		super(iter);

		assert timeLimit > 0 : "time limit must be a positive number, is: " + timeLimit;

		interruptTask = new InterruptTask<>(this);

		timer.schedule(interruptTask, timeLimit);
	}

	@Override
	public boolean hasNext() {
		checkInterrupted();
		if (isClosed()) {
			return false;
		}
		checkInterrupted();
		try {
			boolean result = super.hasNext();
			checkInterrupted();
			return result;
		} catch (NoSuchElementException e) {
			checkInterrupted();
			close();
			throw e;
		}
	}

	@Override
	public E next() {
		checkInterrupted();
		if (isClosed()) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		checkInterrupted();
		try {
			return super.next();
		} catch (NoSuchElementException e) {
			checkInterrupted();
			close();
			throw e;
		}
	}

	@Override
	public void remove() {
		checkInterrupted();
		if (isClosed()) {
			throw new IllegalStateException("The iteration has been closed.");
		}
		checkInterrupted();
		try {
			super.remove();
		} catch (IllegalStateException e) {
			checkInterrupted();
			close();
			throw e;
		}
	}

	@Override
	protected void handleClose() {
		try {
			interruptTask.cancel();
		} finally {
			super.handleClose();
		}
	}

	private void checkInterrupted() {
		if (isInterrupted.get()) {
			try {
				throwInterruptedException();
			} finally {
				try {
					close();
				} catch (Exception e) {
					if (e instanceof InterruptedException) {
						Thread.currentThread().interrupt();
					}
					logger.warn("TimeLimitIteration timed out and failed to close successfully: ", e);
				}
			}
		}
	}

	/**
	 * If the iteration is interrupted by its time limit, this method is called to generate and throw the appropriate
	 * exception.
	 *
	 */
	protected abstract void throwInterruptedException();

	/**
	 * Users of this class must call this method to interrupt the execution at the next available point. It does not
	 * immediately interrupt the running method, but will call close() and set a flag to increase the chances of it
	 * being picked up as soon as possible and to cleanup its resources. <br/>
	 * Note, this method does not generate {@link InterruptedException}s that would occur if {@link Thread#interrupt()}
	 * were called on this thread.
	 */
	void interrupt() {
		isInterrupted.set(true);
		try {
			close();
		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			logger.warn("TimeLimitIteration timed out and failed to close successfully: ", e);
		}
	}
}
