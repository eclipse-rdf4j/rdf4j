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
package org.eclipse.rdf4j.query.impl;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.eclipse.rdf4j.common.iteration.QueueIteration;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * Makes working with a queue easier by adding the methods {@link #done()} and {@link #toss(Exception)} and
 * automatically converting the exception into a QueryEvaluationException with an appropriate stack trace.
 *
 * @author James Leigh
 */
public class QueueCursor<E> extends QueueIteration<E, QueryEvaluationException> {

	/**
	 * Creates an <var>QueueCursor</var> with the given (fixed) capacity and default access policy.
	 *
	 * @param capacity the capacity of this queue
	 */
	public QueueCursor(int capacity) {
		super(capacity, false);
	}

	/**
	 * Creates an <var>QueueCursor</var> with the given {@link BlockingQueue} as its backing queue.<br>
	 * It may not be threadsafe to modify or access the given {@link BlockingQueue} from other locations. This method
	 * only enables the default {@link ArrayBlockingQueue} to be overridden.
	 *
	 * @param queue A BlockingQueue that is not used in other locations, but will be used as the backing Queue
	 *              implementation for this cursor.
	 * @deprecated WeakReference is no longer supported as a way to automatically close this iteration. The recommended
	 *             approach to automatically closing an iteration on garbage collection is to use a
	 *             {@link java.lang.ref.Cleaner}.
	 */
	public QueueCursor(BlockingQueue<E> queue) {
		super(queue);
	}

	@Override
	protected QueryEvaluationException convert(Exception e) {
		throw new QueryEvaluationException(e);
	}

}
