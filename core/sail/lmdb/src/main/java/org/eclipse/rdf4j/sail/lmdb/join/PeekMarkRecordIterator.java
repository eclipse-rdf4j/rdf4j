/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.join;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.sail.lmdb.RecordIterator;

/**
 * Peek/mark/reset wrapper for {@link RecordIterator}. Mirrors
 * {@link org.eclipse.rdf4j.query.algebra.evaluation.iterator.PeekMarkIterator} but operates on primitive long[]
 * records.
 */
class PeekMarkRecordIterator {

	private final RecordIterator iterator;
	private boolean mark;
	private List<long[]> buffer;
	private Iterator<long[]> bufferIterator = Collections.emptyIterator();
	private long[] next;

	// -1: reset impossible, 0: available once, >0: buffered and ready
	private int resetPossible;

	private boolean closed;

	PeekMarkRecordIterator(RecordIterator iterator) {
		this.iterator = iterator;
	}

	boolean hasNext() {
		if (closed) {
			return false;
		}
		calculateNext();
		return next != null;
	}

	long[] next() {
		if (closed) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		calculateNext();
		long[] result = next;
		next = null;
		if (!mark && resetPossible == 0) {
			resetPossible--;
		}
		if (result == null) {
			throw new NoSuchElementException();
		}
		return result;
	}

	long[] peek() {
		if (closed) {
			return null;
		}
		calculateNext();
		return next;
	}

	void mark() {
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

	void reset() {
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
			if (next != null) {
				buffer.add(next);
				next = null;
			}
			bufferIterator = buffer.iterator();
		} else if (resetPossible > 0) {
			next = null;
			bufferIterator = buffer.iterator();
		}

		mark = false;
		resetPossible = 1;
	}

	boolean isMarked() {
		return !closed && mark;
	}

	boolean isResettable() {
		return !closed && (mark || resetPossible >= 0);
	}

	void unmark() {
		mark = false;
		resetPossible = -1;
		if (bufferIterator.hasNext()) {
			buffer = null;
			bufferIterator = Collections.emptyIterator();
		} else if (buffer != null) {
			buffer.clear();
			bufferIterator = Collections.emptyIterator();
		}
	}

	void close() {
		if (!closed) {
			closed = true;
			buffer = null;
			bufferIterator = Collections.emptyIterator();
			next = null;
			try {
				iterator.close();
			} catch (Exception ignore) {
				// RecordIterator#close does not declare checked exceptions
			}
		}
	}

	private void calculateNext() {
		if (next != null || closed) {
			return;
		}

		if (bufferIterator.hasNext()) {
			next = bufferIterator.next();
		} else {
			if (!mark && resetPossible > -1) {
				resetPossible--;
			}
			if (iterator != null) {
				long[] candidate = iterator.next();
				if (candidate != null) {
					next = Arrays.copyOf(candidate, candidate.length);
				}
			}
		}

		if (mark && next != null) {
			assert resetPossible > 0;
			buffer.add(next);
		}
	}
}
