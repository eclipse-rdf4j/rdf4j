/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * Makes working with a queue easier by adding the methods {@link #done()} and {@link #toss(Exception)} and
 * automatically converting the exception into a QueryEvaluationException with an appropriate stack trace.
 * 
 * @author James Leigh
 */
public class QueueCursor<E> extends LookAheadIteration<E, QueryEvaluationException> {

	private final AtomicBoolean done = new AtomicBoolean(false);

	private final BlockingQueue<E> queue;

	private final E afterLast = createAfterLast();

	private final Queue<Throwable> exceptions = new ConcurrentLinkedQueue<Throwable>();

	/**
	 * Creates an <tt>QueueCursor</tt> with the given (fixed) capacity and default access policy.
	 * 
	 * @param capacity
	 *        the capacity of this queue
	 */
	public QueueCursor(int capacity) {
		this(capacity, false);
	}

	/**
	 * Creates an <tt>QueueCursor</tt> with the given (fixed) capacity and the specified access policy.
	 * 
	 * @param capacity
	 *        the capacity of this queue
	 * @param fair
	 *        if <tt>true</tt> then queue accesses for threads blocked on insertion or removal, are processed
	 *        in FIFO order; if <tt>false</tt> the access order is unspecified.
	 */
	public QueueCursor(int capacity, boolean fair) {
		super();
		this.queue = new ArrayBlockingQueue<E>(capacity, fair);
	}

	/**
	 * The next time {@link #next()} is called this exception will be thrown. If it is not a
	 * QueryEvaluationException or RuntimeException it will be wrapped in a QueryEvaluationException.
	 */
	public void toss(Exception exception) {
		exceptions.add(exception);
	}

	/**
	 * Adds another item to the queue, blocking while the queue is full.
	 */
	public void put(E item)
		throws InterruptedException
	{
		try {
			while (!isClosed() && !done.get() && !Thread.currentThread().isInterrupted()
					&& !queue.offer(item, 1, TimeUnit.SECONDS))
			{
				// No body, just iterating regularly through the loop conditions to respond to state changes without a full busy-wait loop
			}
			// Proactively close if interruption didn't propagate an exception to the catch clause below
			if (done.get() || Thread.currentThread().isInterrupted()) {
				close();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			close();
			throw e;
		}
	}

	/**
	 * Indicates the method {@link #put(Object)} will not be called in the queue anymore.
	 */
	public void done() {
		// Lazily set here, and then come back in handleClose and use set if necessary
		done.lazySet(true);
		boolean offer = queue.offer(afterLast);
		if (!offer) {
			// TODO: Log inability to add sentinel at debug level
			// The sentinel is forced onto the queue during the close method
		}
	}

	/**
	 * Returns the next item in the queue, which may be <tt>null</tt>, or throws an exception.
	 */
	@Override
	public E getNextElement()
		throws QueryEvaluationException
	{
		if (isClosed()) {
			return null;
		}
		try {
			checkException();
			E take;
			if (done.get()) {
				take = queue.poll();
			}
			else {
				take = queue.take();
				if (done.get()) {
					done(); // in case the queue was full before
				}
			}
			if (isAfterLast(take)) {
				checkException();
				done(); // put afterLast back for others
				return null;
			}
			checkException();
			return take;
		}
		catch (InterruptedException e) {
			checkException();
			close();
			throw new QueryEvaluationException(e);
		}
	}

	@Override
	public void handleClose()
		throws QueryEvaluationException
	{
		try {
			super.handleClose();
		}
		finally {
			done.set(true);
			do {
				queue.clear(); // ensure extra room is available
			}
			while (!queue.offer(afterLast));
			checkException();
		}
	}

	public void checkException()
		throws QueryEvaluationException
	{
		if (!exceptions.isEmpty()) {
			try {
				close();
				throw exceptions.remove();
			}
			catch (RDF4JException e) {
				if (e instanceof QueryEvaluationException) {
					List<StackTraceElement> stack = new ArrayList<StackTraceElement>();
					stack.addAll(Arrays.asList(e.getStackTrace()));
					StackTraceElement[] thisStack = new Throwable().getStackTrace();
					stack.addAll(Arrays.asList(thisStack).subList(1, thisStack.length));
					e.setStackTrace(stack.toArray(new StackTraceElement[stack.size()]));
					throw e;
				}
				else {
					throw new QueryEvaluationException(e);
				}
			}
			catch (RuntimeException e) {
				// any RuntimeException that is not an RDF4JException should be
				// reported as-is
				List<StackTraceElement> stack = new ArrayList<StackTraceElement>();
				stack.addAll(Arrays.asList(e.getStackTrace()));
				StackTraceElement[] thisStack = new Throwable().getStackTrace();
				stack.addAll(Arrays.asList(thisStack));
				e.setStackTrace(stack.toArray(new StackTraceElement[stack.size()]));
				throw e;
			}
			catch (Throwable e) {
				throw new QueryEvaluationException(e);
			}
		}
	}

	private boolean isAfterLast(E take) {
		return take == null || take == afterLast;
	}

	@SuppressWarnings("unchecked")
	private E createAfterLast() {
		return (E)new Object();
	}

}
