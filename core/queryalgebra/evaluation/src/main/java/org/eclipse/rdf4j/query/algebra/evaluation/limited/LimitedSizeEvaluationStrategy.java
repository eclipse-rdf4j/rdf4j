/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.limited;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;

/**
 * @author Jerven Bolleman, SIB Swiss Institute of Bioinformatics
 */
public class LimitedSizeEvaluationStrategy extends StrictEvaluationStrategy {

	private final AtomicLong used = new AtomicLong();

	private final long maxSize;

	/**
	 * @param tripleSource
	 */
	public LimitedSizeEvaluationStrategy(TripleSource tripleSource, long maxSize,
			FederatedServiceResolver serviceManager) {
		super(tripleSource, serviceManager);
		this.maxSize = maxSize;
	}

	/**
	 * @param tripleSource
	 * @param dataset
	 * @param maxCollectionsSize
	 */
	public LimitedSizeEvaluationStrategy(TripleSource tripleSource, Dataset dataset, int maxCollectionsSize,
			FederatedServiceResolver serviceManager) {
		super(tripleSource, dataset, serviceManager);
		this.maxSize = maxCollectionsSize;
	}

	@Override
	public <B> Set<B> makeSet() {
		Set<B> wrapped = super.makeSet();
		return new Set<>() {

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

		};
	}
}
