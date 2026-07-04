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
package org.eclipse.rdf4j.model.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

/**
 * Excludes an Iterator<Statement> based on a given basic graph pattern.
 */
public class PatternIterator<S extends Statement> implements Iterator<S> {

	private final Iterator<S> filteredIter;

	private final Value subj;
	private final Value pred;
	private final Value obj;
	private final Value[] contexts;

	private S nextElement;

	private boolean nextCalled;

	public PatternIterator(Iterator<S> iter, Value subj, Value pred, Value obj, Value... contexts) {
		this.filteredIter = iter;
		this.subj = subj;
		this.pred = pred;
		this.obj = obj;
		this.contexts = notNull(contexts);
	}

	@Override
	public boolean hasNext() {
		findNextElement();

		return nextElement != null;
	}

	@Override
	public S next() {
		findNextElement();

		S result = nextElement;

		if (result != null) {
			nextElement = null;
			nextCalled = true;
			return result;
		} else {
			throw new NoSuchElementException();
		}
	}

	private void findNextElement() {
		while (nextElement == null && filteredIter.hasNext()) {
			S candidate = filteredIter.next();

			if (accept(candidate)) {
				nextElement = candidate;
			}
		}
	}

	@Override
	public void remove() {
		if (!nextCalled) {
			throw new IllegalStateException();
		}
		filteredIter.remove();
	}

	/**
	 * Tests whether or not the specified statement should be returned by this iterator. All objects from the wrapped
	 * iterator pass through this method in the same order as they are coming from the wrapped iterator.
	 *
	 * @param st The statement to be tested.
	 * @return <var>true</var> if the object should be returned, <var>false</var> otherwise.
	 */
	protected boolean accept(S st) {
		if (subj != null && !subj.equals(st.getSubject())) {
			return false;
		}
		if (pred != null && !pred.equals(st.getPredicate())) {
			return false;
		}
		if (obj != null && !obj.equals(st.getObject())) {
			return false;
		}
		Resource stContext = st.getContext();
		if (contexts != null && contexts.length == 0) {
			// Any context matches
			return true;
		} else {
			// Accept if one of the contexts from the pattern matches
			for (Value context : notNull(contexts)) {
				if (context == null && stContext == null) {
					return true;
				}
				if (context != null && context.equals(stContext)) {
					return true;
				}
			}

			return false;
		}
	}

	private Value[] notNull(Value[] contexts) {
		if (contexts == null) {
			return new Resource[] { null };
		}
		return contexts;
	}
}
