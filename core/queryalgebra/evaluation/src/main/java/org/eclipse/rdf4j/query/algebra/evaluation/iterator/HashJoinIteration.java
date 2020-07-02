/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.common.iterator.UnionIterator;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.util.iterators.EmptyIterator;

/**
 * Generic hash join implementation suitable for use by Sail implementations.
 *
 * @author MJAHale
 */
public class HashJoinIteration extends LookAheadIteration<BindingSet, QueryEvaluationException> {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final CloseableIteration<BindingSet, QueryEvaluationException> leftIter;

	private final CloseableIteration<BindingSet, QueryEvaluationException> rightIter;

	private volatile Iterator<BindingSet> scanList;

	private volatile CloseableIteration<BindingSet, QueryEvaluationException> restIter;

	private volatile Map<BindingSetHashKey, List<BindingSet>> hashTable;

	protected final String[] joinAttributes;

	private volatile BindingSet currentScanElem;

	private volatile Iterator<BindingSet> hashTableValues;

	private final boolean leftJoin;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public HashJoinIteration(EvaluationStrategy strategy, Join join, BindingSet bindings)
			throws QueryEvaluationException {
		this(strategy, join.getLeftArg(), join.getRightArg(), bindings, false);
		join.setAlgorithm(this);
	}

	public HashJoinIteration(EvaluationStrategy strategy, LeftJoin join, BindingSet bindings)
			throws QueryEvaluationException {
		this(strategy, join.getLeftArg(), join.getRightArg(), bindings, true);
		join.setAlgorithm(this);
	}

	public HashJoinIteration(EvaluationStrategy strategy, TupleExpr left, TupleExpr right, BindingSet bindings,
			boolean leftJoin) throws QueryEvaluationException {
		this(strategy, strategy.evaluate(left, bindings), left.getBindingNames(), strategy.evaluate(right, bindings),
				right.getBindingNames(), leftJoin);
	}

	public HashJoinIteration(EvaluationStrategy strategy,
			CloseableIteration<BindingSet, QueryEvaluationException> leftIter, Set<String> leftBindingNames,
			CloseableIteration<BindingSet, QueryEvaluationException> rightIter, Set<String> rightBindingNames,
			boolean leftJoin) throws QueryEvaluationException {
		this.leftIter = leftIter;
		this.rightIter = rightIter;

		Set<String> joinAttributeNames = leftBindingNames;
		joinAttributeNames.retainAll(rightBindingNames);
		joinAttributes = joinAttributeNames.toArray(new String[joinAttributeNames.size()]);

		this.leftJoin = leftJoin;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		Map<BindingSetHashKey, List<BindingSet>> nextHashTable = hashTable;
		if (nextHashTable == null) {
			synchronized (this) {
				nextHashTable = hashTable;
				if (nextHashTable == null) {
					nextHashTable = hashTable = setupHashTable();
				}
			}
		}
		Iterator<BindingSet> nextHashTableValues = hashTableValues;

		while (currentScanElem == null) {
			if (scanList.hasNext()) {
				currentScanElem = nextFromCache(scanList);
			} else {
				disposeCache(scanList); // exhausted so can free

				if (restIter.hasNext()) {
					currentScanElem = restIter.next();
				} else {
					// no more elements available
					return null;
				}
			}

			if (currentScanElem != null) {
				if (currentScanElem instanceof EmptyBindingSet) {
					// the empty bindingset should be merged with all bindingset in the
					// hash table
					Collection<List<BindingSet>> values = nextHashTable.values();
					boolean empty = values.isEmpty() || values.size() == 1 && values.contains(null);
					nextHashTableValues = hashTableValues = empty ? new EmptyIterator<>() : new UnionIterator<>(values);
					if (!nextHashTableValues.hasNext()) {
						currentScanElem = null;
						closeHashValue(nextHashTableValues);
						nextHashTableValues = hashTableValues = null;
					}
				} else {
					BindingSetHashKey key = BindingSetHashKey.create(joinAttributes, currentScanElem);
					List<BindingSet> hashValue = nextHashTable.get(key);
					if (hashValue != null && !hashValue.isEmpty()) {
						nextHashTableValues = hashTableValues = hashValue.iterator();
					} else if (leftJoin) {
						nextHashTableValues = hashTableValues = Collections.singletonList(EmptyBindingSet.getInstance())
								.iterator();
					} else {
						currentScanElem = null;
						closeHashValue(nextHashTableValues);
						nextHashTableValues = hashTableValues = null;
					}
				}
			}
		}

		if (nextHashTableValues != null) {
			BindingSet nextHashTableValue = nextHashTableValues.next();

			QueryBindingSet result = new QueryBindingSet(currentScanElem);

			for (String name : nextHashTableValue.getBindingNames()) {
				if (!result.hasBinding(name)) {
					Value v = nextHashTableValue.getValue(name);
					if (v != null) {
						result.addBinding(name, v);
					}
				}
			}

			if (!nextHashTableValues.hasNext()) {
				// we've exhausted the current scanlist entry
				currentScanElem = null;
				closeHashValue(nextHashTableValues);
				nextHashTableValues = hashTableValues = null;
			}
			return result;
		}

		return EmptyBindingSet.getInstance();
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		try {
			super.handleClose();
		} finally {
			try {
				if (leftIter != null) {
					leftIter.close();
				}
			} finally {
				try {
					if (rightIter != null) {
						rightIter.close();
					}
				} finally {
					try {
						Iterator<BindingSet> toCloseHashTableValues = hashTableValues;
						hashTableValues = null;
						if (toCloseHashTableValues != null) {
							closeHashValue(toCloseHashTableValues);
						}
					} finally {
						try {
							Iterator<BindingSet> toCloseScanList = scanList;
							scanList = null;
							if (toCloseScanList != null) {
								disposeCache(toCloseScanList);
							}
						} finally {
							Map<BindingSetHashKey, List<BindingSet>> toCloseHashTable = hashTable;
							hashTable = null;
							if (toCloseHashTable != null) {
								disposeHashTable(toCloseHashTable);
							}
						}
					}
				}
			}
		}
	}

	private Map<BindingSetHashKey, List<BindingSet>> setupHashTable() throws QueryEvaluationException {

		Collection<BindingSet> leftArgResults;
		Collection<BindingSet> rightArgResults = makeIterationCache(rightIter);
		if (!leftJoin) {
			leftArgResults = makeIterationCache(leftIter);

			while (leftIter.hasNext() && rightIter.hasNext()) {
				add(leftArgResults, leftIter.next());
				add(rightArgResults, rightIter.next());
			}
		} else {
			leftArgResults = Collections.emptyList();

			while (rightIter.hasNext()) {
				add(rightArgResults, rightIter.next());
			}
		}

		Collection<BindingSet> smallestResult = null;

		if (leftJoin || leftIter.hasNext()) { // leftArg is the greater relation
			smallestResult = rightArgResults;
			scanList = leftArgResults.iterator();
			restIter = leftIter;
		} else { // rightArg is the greater relation (or they are equal)
			smallestResult = leftArgResults;
			scanList = rightArgResults.iterator();
			restIter = rightIter;
		}

		// help free memory before allocating the hash table
		leftArgResults = null;
		rightArgResults = null;

		// create the hash table for our join
		// hash table will never be any bigger than smallestResult.size()
		Map<BindingSetHashKey, List<BindingSet>> resultHashTable = makeHashTable(smallestResult.size());
		int maxListSize = 1;
		for (BindingSet b : smallestResult) {
			BindingSetHashKey hashKey = BindingSetHashKey.create(joinAttributes, b);

			List<BindingSet> hashValue = resultHashTable.get(hashKey);
			boolean newEntry = (hashValue == null);
			if (newEntry) {
				hashValue = makeHashValue(maxListSize);
			}
			add(hashValue, b);
			// always do a put() in case the map implementation is not memory-based
			// e.g. it serializes the values
			putHashTableEntry(resultHashTable, hashKey, hashValue, newEntry);

			maxListSize = Math.max(maxListSize, hashValue.size());
		}
		return resultHashTable;
	}

	protected void putHashTableEntry(Map<BindingSetHashKey, List<BindingSet>> nextHashTable, BindingSetHashKey hashKey,
			List<BindingSet> hashValue, boolean newEntry) throws QueryEvaluationException {
		// by default, we use a standard memory hash map
		// so we only need to do the put() if the list is new
		if (newEntry) {
			nextHashTable.put(hashKey, hashValue);
		}
	}

	/**
	 * Utility methods to make it easier to inserted custom store dependent list
	 *
	 * @return list
	 */
	protected Collection<BindingSet> makeIterationCache(CloseableIteration<BindingSet, QueryEvaluationException> iter) {
		return new ArrayList<>();
	}

	/**
	 * Utility methods to make it easier to inserted custom store dependent maps
	 *
	 * @return map
	 */
	protected Map<BindingSetHashKey, List<BindingSet>> makeHashTable(int initialSize) {
		Map<BindingSetHashKey, List<BindingSet>> nextHashTable;
		if (joinAttributes.length > 0) {
			// we should probably adjust for the load factor
			// but we are only one rehash away and this might save a bit of memory
			// when we have more than one value per entry
			nextHashTable = new HashMap<>(initialSize);
		} else {
			List<BindingSet> l = (initialSize > 0) ? new ArrayList<>(initialSize) : null;
			nextHashTable = Collections.<BindingSetHashKey, List<BindingSet>>singletonMap(BindingSetHashKey.EMPTY, l);
		}
		return nextHashTable;
	}

	/**
	 * Utility methods to make it easier to inserted custom store dependent list
	 *
	 * @return list
	 */
	protected List<BindingSet> makeHashValue(int currentMaxListSize) {
		// we pick an initial size that means we may only have to resize once
		// while saving memory in the case that the list doesn't reach max size
		return new ArrayList<>(currentMaxListSize / 2 + 1);
	}

	/**
	 * Utility methods to clear-up in case not using in-memory cache.
	 */
	protected void disposeCache(Iterator<BindingSet> iter) {
	}

	/**
	 * Utility methods to clear-up in case not using in-memory hash table.
	 */
	protected void disposeHashTable(Map<BindingSetHashKey, List<BindingSet>> map) {
	}

	/**
	 * Utility methods to clear-up in case not using in-memory hash table.
	 */
	protected <E> void closeHashValue(Iterator<E> iter) {
	}

	// hooks for LimitedSizeHashJoinIterator

	protected <E> E nextFromCache(Iterator<E> iter) {
		return iter.next();
	}

	protected <E> void add(Collection<E> col, E value) throws QueryEvaluationException {
		col.add(value);
	}

	protected <E> void addAll(Collection<E> col, List<E> values) throws QueryEvaluationException {
		col.addAll(values);
	}
}
