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

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;

/**
 * An iterator that allows to peek at the next element without consuming it. It also allows to mark the current position
 * and reset to that position. Reset can be called multiple times, as long as the underlying iterator has not been
 * advanced beyond peek.
 */
@Experimental
public class PeekMarkIterator implements CloseableIteration<BindingSet> {

	private final CloseableIteration<BindingSet> iterator;
	private boolean mark;
	private ArrayList<BindingSet> buffer;
	private Iterator<BindingSet> bufferIterator = Collections.emptyIterator();
	private BindingSet next;

	// -1: reset not possible; 0: reset possible, but next must be saved; 1: reset possible
	private int resetPossible;

	private PeekMarkIterator(CloseableIteration<BindingSet> iterator) {
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
		calculateNext();
		return next != null;
	}

	@Override
	public BindingSet next() {
		calculateNext();
		BindingSet result = next;
		next = null;
		if (!mark && resetPossible == 0) {
			resetPossible--;
		}
		return result;
	}

	public BindingSet peek() {
		calculateNext();
		return next;
	}

	public void mark() {
		mark = true;
		resetPossible = 1;
		buffer = new ArrayList<>();
		if (next != null) {
			buffer.add(next);
		}
	}

	public void reset() {
		if (buffer == null) {
			throw new IllegalStateException("Mark never set");
		}
		mark = false;
		if (resetPossible < 0) {
			throw new IllegalStateException("Reset not possible");
		} else if (resetPossible == 0) {
			buffer.add(next);
			next = null;
			bufferIterator = buffer.iterator();
		} else if (resetPossible == 1) {
			bufferIterator = buffer.iterator();
		}

		resetPossible = 1;
	}

	@Override
	public void close() {
		iterator.close();
	}
}
