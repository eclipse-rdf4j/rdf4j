/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.iterator;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author MJAHale
 */
public class UnionIterator<E> extends LookAheadIterator<E> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final Iterator<? extends Iterable<? extends E>> argIter;

	private Iterator<? extends E> currentIter;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new UnionIterator that returns the bag union of the results of a number of Iterators.
	 *
	 * @param args The Iterators containing the elements to iterate over.
	 */
	public UnionIterator(Iterable<? extends E>... args) {
		this(Arrays.asList(args));
	}

	public UnionIterator(Iterable<? extends Iterable<? extends E>> args) {
		argIter = args.iterator();

		// Initialize with empty iteration so that var is never null
		currentIter = new EmptyIterator<>();
	}

	/*--------------*
	 * Constructors *
	 *--------------*/

	@Override
	protected E getNextElement() {
		if (currentIter.hasNext()) {
			return currentIter.next();
		}

		// Current Iterator exhausted, continue with the next one
		if (currentIter instanceof Closeable) {
			try {
				((Closeable) currentIter).close();
			} catch (IOException ioe) {
				// ignore
			}
		}

		if (argIter.hasNext()) {
			currentIter = argIter.next().iterator();
		} else {
			// All elements have been returned
			return null;
		}

		return getNextElement();
	}

	@Override
	protected void handleClose() throws IOException {
		try {
			// Close this iteration, this will prevent lookAhead() from calling
			// getNextElement() again
			super.handleClose();
		} finally {
			if (currentIter instanceof Closeable) {
				((Closeable) currentIter).close();
			}
		}
	}
}
