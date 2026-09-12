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
import org.eclipse.rdf4j.model.Value;

/**
 * An iterator that allows to peek at the next element without consuming it. It also allows to mark the current position
 * and reset to that position.
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

	public PeekMarkIterator(CloseableIteration<E> iterator) {
		this.iterator = iterator;
	}

	private void calculateNext() {
		if (next != null) {
			return;
		}

		if (bufferIterator.hasNext()) {
			next = bufferIterator.next();
		} else {
			if (!mark && resetPossible > -1) {
				resetPossible--;
			}
			if (iterator.hasNext()) {
				next = iterator.next();
			}
		}

		if (mark && next != null) {
			assert resetPossible > 0;
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
	 * @return the next element without consuming it, or null if there are no more elements
	 */
	public E peek() {
		if (closed) {
			return null;
		}
		calculateNext();
		return next;
	}

	/**
	 * Mark the current position so that the iterator can be reset to the current state. This will cause elements to be
	 * stored in memory until one of {@link #reset()}, {@link #unmark()} or {@link #mark()} is called.
	 */
	public void mark() {
		if (closed) {
			throw new IllegalStateException("The iteration has been closed.");
		}
		mark = true;
		resetPossible = 1;

		if (buffer != null && !bufferIterator.hasNext()) {
			buffer.clear();
			bufferIterator = Collections.emptyIterator();
		} else {
			buffer = new ArrayList<>();
		}

		if (next != null) {
			buffer.add(next);
		}

	}

	/**
	 * Reset the iterator to the marked position. Resetting an iterator multiple times will always reset to the same
	 * position. Resetting an iterator turns off marking. If the iterator was reset previously and the iterator has
	 * advanced beyond the point where reset was initially called, then the iterator can no longer be reset because
	 * there will be elements that were not stored while the iterator was marked and resetting will cause these elements
	 * to be lost.
	 */
	public void reset() {
		if (closed) {
			throw new IllegalStateException("The iteration has been closed.");
		}
		if (buffer == null) {
			throw new IllegalStateException("Mark never set");
		}
		if (resetPossible < 0) {
			throw new IllegalStateException("Reset not possible");
		}

		if (mark && bufferIterator.hasNext()) {
			while (bufferIterator.hasNext()) {
				buffer.add(bufferIterator.next());
			}
		}

		if (resetPossible == 0) {
			assert !mark;
			buffer.add(next);
			next = null;
			bufferIterator = buffer.iterator();
		} else if (resetPossible > 0) {
			next = null;
			bufferIterator = buffer.iterator();
		}

		mark = false;
		resetPossible = 1;
	}

	/**
	 * @return true if the iterator is marked
	 */
	boolean isMarked() {
		return !closed && mark;
	}

	/**
	 * @return true if {@link #reset()} can be called on this iterator
	 */
	boolean isResettable() {
		return !closed && (mark || resetPossible >= 0);
	}

	@Override
	public void close() {
		this.closed = true;
		iterator.close();
		buffer = null;
	}

	/**
	 * Forwards the advisory seek hint to the wrapped iterator, but only while no element could still be owed to the
	 * caller from this iterator's own state: an active mark, a pending reset, buffered elements being replayed, or a
	 * peeked element all suppress forwarding so that mark/reset semantics stay exact.
	 */
	@Override
	public void seek(Value minValue, boolean minInclusive, Value maxValue, boolean maxInclusive) {
		if (closed || mark || resetPossible >= 0 || bufferIterator.hasNext() || next != null) {
			return;
		}
		iterator.seek(minValue, minInclusive, maxValue, maxInclusive);
	}

	@Override
	public boolean supportsSeek() {
		return !closed && iterator.supportsSeek();
	}

	/**
	 * Unmark the iterator. This will cause the iterator to stop buffering elements. If the iterator was recently reset
	 * and there are still elements in the buffer, then these elements will still be returned by next().
	 */
	public void unmark() {
		mark = false;
		resetPossible = -1;
		if (bufferIterator.hasNext()) {
			buffer = null;
		} else if (buffer != null) {

			buffer.clear();
			bufferIterator = Collections.emptyIterator();
		}
	}
}
