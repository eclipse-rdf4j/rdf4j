package org.eclipse.rdf4j.query.algebra.evaluation.limited;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.query.QueryEvaluationException;

final class LimitedSizeSet<B> implements Set<B> {
	private final Set<B> wrapped;
	private final AtomicLong used;
	private final long maxSize;

	LimitedSizeSet(Set<B> wrapped, AtomicLong used, final long maxSize) {
		this.wrapped = wrapped;
		this.used = used;
		this.maxSize = maxSize;
	}

	@Override
	public boolean add(B e) {
		final boolean add = wrapped.add(e);
		if (add && used.incrementAndGet() > maxSize) {
			throw new QueryEvaluationException("Set size is to large");
		}
		return add;
	}

	@Override
	public boolean remove(Object o) {

		final boolean removed = wrapped.remove(o);
		if (removed) {
			used.decrementAndGet();
		}
		return removed;
	}

	@Override
	public void clear() {
		int size = size();
		used.getAndAdd(-size);
		wrapped.clear();
	}

	@Override
	public int size() {
		return wrapped.size();
	}

	@Override
	public boolean isEmpty() {
		return wrapped.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return wrapped.contains(o);
	}

	@Override
	public Iterator<B> iterator() {
		return wrapped.iterator();
	}

	@Override
	public Object[] toArray() {
		return wrapped.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return wrapped.toArray(a);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return wrapped.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends B> c) {
		boolean all = true;
		for (B b : c) {
			boolean added = wrapped.add(b);
			if (!added) {
				all = false;
			}
		}
		return all;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		int prevSize = wrapped.size();
		boolean changed = wrapped.retainAll(c);
		if (changed) {
			int newSize = wrapped.size();
			used.addAndGet(-(prevSize - newSize));
		}
		return changed;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		int prevSize = wrapped.size();
		boolean changed = wrapped.removeAll(c);
		if (changed) {
			int newSize = wrapped.size();
			used.addAndGet(-(prevSize - newSize));
		}
		return changed;
	}
}