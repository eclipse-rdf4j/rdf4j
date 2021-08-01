package org.eclipse.rdf4j.spring.support.query;

import java.util.Iterator;
import java.util.function.Consumer;

public class DelegatingIterator<T> implements Iterator<T> {
	private Iterator<T> delegate;

	public DelegatingIterator(Iterator<T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean hasNext() {
		return delegate.hasNext();
	}

	@Override
	public T next() {
		return delegate.next();
	}

	@Override
	public void remove() {
		delegate.remove();
	}

	@Override
	public void forEachRemaining(Consumer<? super T> action) {
		delegate.forEachRemaining(action);
	}
}
