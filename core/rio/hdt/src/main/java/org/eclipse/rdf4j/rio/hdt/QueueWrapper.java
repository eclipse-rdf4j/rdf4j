/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.hdt;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * Wrapper for MapDB Queue, since it doesn't support iterator nor size()
 * 
 * @author Bart Hanssens
 */
class QueueWrapper<E> extends AbstractQueue<E> {
	private final Queue<E> q;
	private int size = 0;

	public QueueWrapper(Queue<E> queue) {
		this.q = queue;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean add(E e) {
		size++;
		return q.add(e);
	}

	@Override
	public E remove() {
		size--;
		return super.remove();
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator() {
			@Override
			public boolean hasNext() {
				return QueueWrapper.this.size > 0;
			}

			@Override
			public Object next() {
				return QueueWrapper.this.remove();
			}

			@Override
			public void remove() {
				QueueWrapper.this.remove();
			}

			@Override
			public void forEachRemaining(Consumer action) {
				while (QueueWrapper.this.size > 0) {
					action.accept(next());
				}
			}
		};
	}

	@Override
	public boolean offer(E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public E poll() {
		return q.poll();
	}

	@Override
	public E element() {
		return q.element();
	}

	@Override
	public E peek() {
		return q.peek();
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public boolean contains(Object o) {
		return q.contains(o);
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray(Object[] a) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		return q.remove(o);
	}

	@Override
	public boolean containsAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		q.clear();
	}
}
