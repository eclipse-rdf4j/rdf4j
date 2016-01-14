/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.DelayedIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.mapdb.DB;
import org.mapdb.DBMaker;

/**
 * Sorts the input and optionally applies limit and distinct.
 * 
 * @author James Leigh
 * @author Arjohn Kampman
 */
public class OrderIterator extends DelayedIteration<BindingSet, QueryEvaluationException> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final CloseableIteration<BindingSet, QueryEvaluationException> iter;

	private final Comparator<BindingSet> comparator;

	private final long limit;

	private final boolean distinct;

	private final File tempFile;

	private final DB db;

	/**
	 * Number of items cached before internal collection is synced to disk. If
	 * set to 0, no disk-syncing is done and all internal caching is kept in
	 * memory.
	 */
	private final long iterationSyncThreshold;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public OrderIterator(CloseableIteration<BindingSet, QueryEvaluationException> iter,
			Comparator<BindingSet> comparator)
	{
		this(iter, comparator, Integer.MAX_VALUE, false);
	}

	public OrderIterator(CloseableIteration<BindingSet, QueryEvaluationException> iter,
			Comparator<BindingSet> comparator, long limit, boolean distinct)
	{
		this(iter, comparator, limit, distinct, 0);
	}

	public OrderIterator(CloseableIteration<BindingSet, QueryEvaluationException> iter,
			Comparator<BindingSet> comparator, long limit, boolean distinct, long iterationSyncThreshold)
	{
		this.iter = iter;
		this.comparator = comparator;
		this.limit = limit;
		this.distinct = distinct;
		this.iterationSyncThreshold = iterationSyncThreshold;

		if (iterationSyncThreshold > 0) {
			try {
				this.tempFile = File.createTempFile("order-eval", null);
			}
			catch (IOException e) {
				throw new IOError(e);
			}
			this.db = DBMaker.newFileDB(tempFile).deleteFilesAfterClose().closeOnJvmShutdown().make();
		}
		else {
			this.tempFile = null;
			this.db = null;
		}
	}

	/*---------*
	 * Methods *
	 *---------*/

	protected NavigableMap<BindingSet, Integer> makeOrderedMap() {
		if (db == null) {
			// no disk-syncing - we use a simple in-memory TreeMap instead.
			return new TreeMap<BindingSet, Integer>(comparator);
		}
		else {
			return db.createTreeMap("iteration").comparator(comparator).makeOrGet();
		}
	}

	protected Iteration<BindingSet, QueryEvaluationException> createIteration()
		throws QueryEvaluationException
	{
		final NavigableMap<BindingSet, Integer> map = makeOrderedMap();
		long size = 0;

		try {
			while (iter.hasNext()) {
				BindingSet next = iter.next();

				// Add this binding set if the limit hasn't been reached yet, or if
				// it is sorted before the current lowest value
				if (size < limit || comparator.compare(next, map.lastKey()) < 0) {

					Integer count = map.get(next);

					if (count == null) {
						put(map, next, 1);
						size++;
					}
					else if (!distinct) {
						put(map, next, ++count);
						size++;
					}

					if (db != null && size % iterationSyncThreshold == 0L) {
						// sync collection to disk every X new entries (where X is a
						// multiple of the cache size)
						db.commit();
					}

					if (size > limit) {
						// Discard binding set that is currently sorted last
						BindingSet lastKey = map.lastKey();

						Integer lastCount = map.get(lastKey);
						if (lastCount > 1) {
							put(map, lastKey, --lastCount);
						}
						else {
							removeLast(map.navigableKeySet());
						}
						size--;
					}
				}
			}
		}
		finally {
			iter.close();
		}

		return new LookAheadIteration<BindingSet, QueryEvaluationException>() {

			private volatile Iterator<BindingSet> iterator = map.keySet().iterator();

			private volatile BindingSet currentBindingSet = null;

			private volatile int count = 0;

			protected BindingSet getNextElement() {

				if (count == 0 && iterator.hasNext()) {
					currentBindingSet = iterator.next();
					count = map.get(currentBindingSet);
				}

				if (count > 0) {
					count--;
					return currentBindingSet;
				}

				return null;
			}
		};
	}

	protected void removeLast(Collection<BindingSet> lastResults) {
		if (lastResults instanceof LinkedList<?>) {
			((LinkedList<BindingSet>)lastResults).removeLast();
		}
		else if (lastResults instanceof List<?>) {
			((List<BindingSet>)lastResults).remove(lastResults.size() - 1);
		}
		else {
			Iterator<BindingSet> iter = lastResults.iterator();
			while (iter.hasNext()) {
				iter.next();
			}
			iter.remove();
		}
	}

	protected boolean add(BindingSet next, Collection<BindingSet> list)
		throws QueryEvaluationException
	{
		return list.add(next);
	}

	protected Integer put(NavigableMap<BindingSet, Integer> map, BindingSet set, int count)
		throws QueryEvaluationException
	{
		return map.put(set, count);
	}

	@Override
	public void remove()
		throws QueryEvaluationException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected void handleClose()
		throws QueryEvaluationException
	{
		iter.close();
		if (db != null) {
			this.db.close();
		}
		super.handleClose();
	}
}
