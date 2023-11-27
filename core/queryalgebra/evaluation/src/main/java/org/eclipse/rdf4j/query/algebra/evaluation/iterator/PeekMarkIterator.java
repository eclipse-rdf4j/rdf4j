/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;

/**
 * An iterator that allows to peek at the next element without consuming it. It also allows to mark the current position
 * and reset to that position. Reset can be called multiple times, as long as the underlying iterator has not been
 * advanced beyond peek.
 *
 * @author Håvard M. Ottestad
 */
@Experimental
public class PeekMarkIterator<E> implements CloseableIteration<E> {

	private final CloseableIteration<E> iterator;
	private boolean mark;
	private ArrayList<E> buffer;
	private Iterator<E> bufferIterator = Collections.emptyIterator();
	private E next;

	// -1: reset not possible; 0: reset possible, but next must be saved; 1: reset possible
	private int resetPossible;

	private boolean closed;

	PeekMarkIterator(CloseableIteration<E> iterator) {
		this.iterator = iterator;
	}

	private void calculateNext() {
		if (next != null) {
			return;
		}

		if (bufferIterator.hasNext()) {
			next = bufferIterator.next();
			assert resetPossible == 1;
		} else {
			if (!mark && resetPossible > -1) {
				resetPossible--;
			}
			if (iterator.hasNext()) {
				next = iterator.next();
			}
		}

		if (mark && next != null) {
			assert resetPossible == 1;
			buffer.add(next);
		}

	}

	@Override
	public boolean hasNext() {
		if (closed) {
			return false;
		}
		calculateNext();
		return next != null;
	}

	@Override
	public E next() {
		if (closed) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		calculateNext();
		E result = next;
		next = null;
		if (!mark && resetPossible == 0) {
			resetPossible--;
		}
		if (result == null) {
			throw new NoSuchElementException();
		}

		return result;
	}

	/**
	 *
	 * @return the next element without consuming it, or null if there are no more elements.
	 */
	public E peek() {
		if (closed) {
			return null;
		}
		calculateNext();
		return next;
	}

	public void mark() {
		if (closed) {
			throw new IllegalStateException("The iteration has been closed.");
		}
		mark = true;
		resetPossible = 1;
		buffer = new ArrayList<>();
		if (next != null) {
			buffer.add(next);
		}

	}

	public void reset() {
		if (closed) {
			throw new IllegalStateException("The iteration has been closed.");
		}
		if (buffer == null) {
			throw new IllegalStateException("Mark never set");
		}
		if (mark && bufferIterator.hasNext()) {
			while (bufferIterator.hasNext()) {
				buffer.add(bufferIterator.next());
			}
		}

		mark = false;
		if (resetPossible < 0) {
			throw new IllegalStateException("Reset not possible");
		} else if (resetPossible == 0) {
			buffer.add(next);
			next = null;
			bufferIterator = buffer.iterator();
		} else if (resetPossible == 1) {
			next = null;
			bufferIterator = buffer.iterator();
		}

		resetPossible = 1;
	}

	boolean isMarked() {
		return !closed && mark;
	}

	boolean isResettable() {
		return !closed && (mark || resetPossible >= 0);
	}

	@Override
	public void close() {
		this.closed = true;
		iterator.close();
	}

	// What will happen if we are iterating over the buffer at this point, then unmark is called followed by mark?
	public void unmark() {
		mark = false;
		resetPossible = -1;
	}
}
