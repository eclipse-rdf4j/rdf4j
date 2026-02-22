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
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.DelayedIteration;
import org.eclipse.rdf4j.common.iteration.LimitIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * Sorts the input and optionally applies limit and distinct.
 *
 * @author James Leigh
 * @author Arjohn Kampman
 */
public class OrderIterator extends DelayedIteration<BindingSet> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final CloseableIteration<BindingSet> iter;

	private final Comparator<BindingSet> comparator;

	private final long limit;

	private final boolean distinct;

	private final List<SerializedQueue<BindingSet>> serialized = new LinkedList<>();

	/**
	 * Number of items cached before internal collection is synced to disk. If set to 0, no disk-syncing is done and all
	 * internal caching is kept in memory.
	 */
	private final long iterationSyncThreshold;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public OrderIterator(CloseableIteration<BindingSet> iter, Comparator<BindingSet> comparator) {
		this(iter, comparator, Long.MAX_VALUE, false);
	}

	public OrderIterator(CloseableIteration<BindingSet> iter, Comparator<BindingSet> comparator, long limit,
			boolean distinct) {
		this(iter, comparator, limit, distinct, Integer.MAX_VALUE);
	}

	public OrderIterator(CloseableIteration<BindingSet> iter, Comparator<BindingSet> comparator, long limit,
			boolean distinct, long iterationSyncThreshold) {
		this.iter = iter;
		this.comparator = comparator;
		this.limit = limit;
		this.distinct = distinct;
		this.iterationSyncThreshold = iterationSyncThreshold > 0 ? iterationSyncThreshold : Integer.MAX_VALUE;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected CloseableIteration<BindingSet> createIteration() throws QueryEvaluationException {
		BindingSet threshold = null;
		List<BindingSet> list = new LinkedList<>();
		long inputRowsRead = 0;
		long spillCount = 0;
		long spillBytes = 0;
		int limit2 = limit >= Integer.MAX_VALUE / 2 ? Integer.MAX_VALUE : (int) limit * 2;
		int syncThreshold = (int) Math.min(iterationSyncThreshold, Integer.MAX_VALUE);
		try {
			while (iter.hasNext()) {
				if (list.size() >= syncThreshold && list.size() < limit) {
					SerializedQueue<BindingSet> queue = new SerializedQueue<>("orderiter");
					sort(list).forEach(queue::add);
					serialized.add(queue);
					spillCount++;
					long bytes = queue.estimatedBytes();
					spillBytes += bytes;
					onSpillToDisk(queue.size(), bytes);
					decrement(list.size() - queue.size());
					list = new ArrayList<>(list.size());
					if (threshold == null && serialized.stream().mapToLong(SerializedQueue::size).sum() >= limit) {
						Stream<BindingSet> stream = serialized.stream().map(SerializedQueue::peekLast);
						threshold = stream.sorted(comparator).skip(serialized.size() - 1).findFirst().orElseThrow();
					}
				} else if (list.size() >= limit2 || !distinct && threshold == null && list.size() >= limit) {
					List<BindingSet> sorted = new ArrayList<>(limit2);
					sort(list).forEach(sorted::add);
					decrement(list.size() - sorted.size());
					list = sorted;
					if (sorted.size() >= limit) {
						threshold = sorted.get(sorted.size() - 1);
					}
				}
				BindingSet next = iter.next();
				inputRowsRead++;
				onInputRowRead(next);
				if (threshold == null || comparator.compare(next, threshold) < 0) {
					list.add(next);
					increment();
				}
			}
		} catch (IOException e) {
			throw new QueryEvaluationException(e);
		} finally {
			iter.close();
		}

		List<Iterator<BindingSet>> iterators = new ArrayList<>(serialized.size() + 1);
		serialized
				.stream()
				.map(SerializedQueue::iterator)
				.forEach(iterators::add);

		iterators.add(sort(list).iterator());

		SortedIterators<BindingSet> iterator = new SortedIterators<>(comparator, distinct, iterators);
		onSortCompleted(inputRowsRead, spillCount, spillBytes);

		return new LimitIteration<>(new CloseableIteratorIteration<>(iterator), limit);
	}

	protected void increment() throws QueryEvaluationException {
		// give subclasses a chance to stop query evaluation
	}

	protected void onInputRowRead(BindingSet next) throws QueryEvaluationException {
		// give subclasses a chance to track consumed input rows
	}

	protected void onSpillToDisk(int spilledRows, long spilledBytes) throws QueryEvaluationException {
		// give subclasses a chance to track spill behavior
	}

	protected void onSortCompleted(long inputRows, long spillCount, long spillBytes) {
		// give subclasses a chance to track final sort statistics
	}

	protected void decrement(int amount) throws QueryEvaluationException {
		// let subclasses know that the expected result size is smaller
	}

	private Stream<BindingSet> sort(Collection<BindingSet> collection) {
		BindingSet[] array = collection.toArray(new BindingSet[collection.size()]);
		Arrays.parallelSort(array, comparator);
		Stream<BindingSet> stream = Stream.of(array);
		if (distinct) {
			stream = stream.distinct();
		}
		if (limit < Integer.MAX_VALUE) {
			stream = stream.limit(limit);
		}
		return stream;
	}

	@Override
	public void remove() throws QueryEvaluationException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		try {
			super.handleClose();
		} finally {
			try {
				iter.close();
			} finally {
				serialized.stream().map(queue -> {
					try {
						queue.close();
						return null;
					} catch (IOException e) {
						return e;
					}
				}).filter(exec -> exec != null).findFirst().ifPresent(exec -> {
					throw new QueryEvaluationException(exec);
				});
			}
		}
	}

	private static class SerializedQueue<E extends Serializable> extends AbstractQueue<E> implements Closeable {

		private final File file;

		private final ObjectOutputStream output;

		private ObjectInputStream input;

		private int size;

		private E next;

		private E last;

		public SerializedQueue(String prefix) throws IOException {
			this(prefix, null);
		}

		public SerializedQueue(String prefix, File directory) throws IOException {
			file = File.createTempFile(prefix, "", directory);
			output = new ObjectOutputStream(new FileOutputStream(file));
		}

		public E peekLast() {
			return last;
		}

		@Override
		public boolean offer(E e) {
			if (output == null) {
				return false;
			}
			try {
				output.writeObject(e);
				last = e;
				size++;
				return true;
			} catch (IOException exc) {
				return false;
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public E poll() {
			try {
				if (next != null) {
					return next;
				} else if (input == null) {
					output.close();
					input = new ObjectInputStream(new FileInputStream(file));
				}
				size--;
				return (E) input.readObject();
			} catch (IOException | ClassNotFoundException exc) {
				return null;
			} finally {
				next = null;
			}
		}

		@Override
		public E peek() {
			if (size <= 0) {
				return null;
			} else if (next != null) {
				return next;
			} else {
				return next = poll();
			}
		}

		@Override
		public Iterator<E> iterator() {
			return new Iterator<E>() {

				@Override
				public boolean hasNext() {
					return peek() != null;
				}

				@Override
				public E next() {
					return poll();
				}
			};
		}

		@Override
		public int size() {
			if (next == null) {
				return size;
			} else {
				return size + 1;
			}
		}

		@Override
		public void close() throws IOException {
			if (output != null) {
				output.close();
			}
			if (input != null) {
				input.close();
			}
			file.delete();
		}

		public long estimatedBytes() {
			try {
				output.flush();
				return file.length();
			} catch (IOException e) {
				return 0L;
			}
		}

	}

	private static class SortedIterators<E> implements Iterator<E> {

		private final List<Iterator<E>> iterators;

		private final TreeMap<E, List<Integer>> head;

		private final boolean distinct;

		private E next;

		public SortedIterators(Comparator<E> comparator, boolean distinct, List<Iterator<E>> iterators) {
			this.iterators = iterators;
			this.distinct = distinct;
			head = new TreeMap<>(comparator);
		}

		@Override
		public boolean hasNext() {
			if (next != null) {
				return true;
			} else {
				next = next();
				return next != null;
			}
		}

		@Override
		public E next() {
			if (next != null) {
				try {
					return next;
				} finally {
					next = null;
				}
			}
			if (head.isEmpty()) {
				for (int i = 0, n = iterators.size(); i < n; i++) {
					advance(i);
				}
			}
			if (head.isEmpty()) {
				return null;
			} else {
				Entry<E, List<Integer>> e = head.firstEntry();
				advance(e.getValue().remove(0));
				if (e.getValue().isEmpty()) {
					head.remove(e.getKey());
				}
				return e.getKey();
			}
		}

		private void advance(int i) {
			while (iterators.get(i).hasNext()) {
				E key = iterators.get(i).next();
				if (!head.containsKey(key)) {
					head.put(key, new LinkedList<>(List.of(i)));
					break;
				} else if (!distinct) {
					head.get(key).add(i);
					break;
				}
			}
		}

	}

}
