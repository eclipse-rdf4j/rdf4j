package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;

public class CloseablePeakableIteration<E, X extends Exception> implements CloseableIteration<E, X> {

	CloseableIteration<E, X> parent;

	E peek;

	public CloseablePeakableIteration(CloseableIteration<E, X> parent) {
		this.parent = parent;
	}

	@Override
	public void close() throws X {
		parent.close();
	}

	@Override
	public boolean hasNext() throws X {
		if (peek != null) {
			return true;
		}
		return parent.hasNext();
	}

	@Override
	public E next() throws X {
		E next = null;
		if (peek != null) {
			next = peek;
			peek = null;
		} else {
			next = parent.next();
		}

		return next;
	}

	@Override
	public void remove() throws X {
		parent.remove();
	}

	public E peek() throws X {
		if (peek == null) {
			peek = parent.next();
		}

		return peek;
	}
}
