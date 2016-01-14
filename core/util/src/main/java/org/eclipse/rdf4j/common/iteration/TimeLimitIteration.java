/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.iteration;

import java.util.NoSuchElementException;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Arjohn Kampman
 */
public abstract class TimeLimitIteration<E, X extends Exception> extends IterationWrapper<E, X> {

	private static Timer timer = null;

	private static synchronized Timer getTimer() {
		if (timer == null) {
			timer = new Timer("TimeLimitIteration", true);
		}
		return timer;
	}

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final InterruptTask<E, X> interruptTask;
	
	private volatile boolean isInterrupted = false;

	public TimeLimitIteration(Iteration<? extends E, ? extends X> iter, long timeLimit) {
		super(iter);

		assert timeLimit > 0 : "time limit must be a positive number, is: " + timeLimit;

		interruptTask = new InterruptTask<E, X>(this);

		getTimer().schedule(interruptTask, timeLimit);
	}

	@Override
	public boolean hasNext()
		throws X
	{
		checkInterrupted();
		try {
			boolean result = super.hasNext();
			checkInterrupted();
			return result;
		}
		catch (NoSuchElementException e) {
			checkInterrupted();
			throw e;
		}
	}

	@Override
	public E next()
		throws X
	{
		checkInterrupted();
		try {
			return super.next();
		}
		catch (NoSuchElementException e) {
			checkInterrupted();
			throw e;
		}
	}

	@Override
	public void remove()
		throws X
	{
		checkInterrupted();
		super.remove();
	}

	@Override
	protected void handleClose()
		throws X
	{
		interruptTask.cancel();
		super.handleClose();
	}

	private final void checkInterrupted()
		throws X
	{
		if (isInterrupted) {
			throwInterruptedException();
		}
	}

	protected abstract void throwInterruptedException()
		throws X;

	void interrupt() {
		isInterrupted = true;
		if (!isClosed()) {
			try {
				close();
			}
			catch (Exception e) {
				logger.warn("Failed to close iteration", e);
			}
		}
	}
}
