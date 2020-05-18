/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQueryResult;

/**
 * An implementation of the {@link TupleQueryResult} interface that stores the complete query result in memory. The
 * query results in a MutableTupleQueryResult can be iterated over multiple times and can also be iterated over in
 * reverse order.
 *
 * @author Arjohn Kampman
 */
public class MutableTupleQueryResult implements TupleQueryResult, Cloneable {

	/*-----------*
	 * Variables *
	 *-----------*/

	private final Set<String> bindingNames = new LinkedHashSet<>();

	private final List<BindingSet> bindingSets = new ArrayList<>();

	/**
	 * The index of the next element that will be returned by a call to {@link #next()}.
	 */
	private volatile int currentIndex = 0;

	/**
	 * The index of the last element that was returned by a call to {@link #next()} or {@link #previous()}. Equal to -1
	 * if there is no such element.
	 */
	private volatile int lastReturned = -1;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public <E extends Exception> MutableTupleQueryResult(Collection<String> bindingNames, BindingSet... bindingSets) {
		this(bindingNames, Arrays.asList(bindingSets));
	}

	/**
	 * Creates a query result table with the supplied binding names. <em>The supplied list of binding names is assumed
	 * to be constant</em>; care should be taken that the contents of this list doesn't change after supplying it to
	 * this solution.
	 *
	 * @param bindingNames The binding names, in order of projection.
	 */
	public MutableTupleQueryResult(Collection<String> bindingNames, Collection<? extends BindingSet> bindingSets) {
		this.bindingNames.addAll(bindingNames);
		this.bindingSets.addAll(bindingSets);
	}

	public <E extends Exception> MutableTupleQueryResult(Collection<String> bindingNames,
			Iteration<? extends BindingSet, E> bindingSetIter) throws E {
		this.bindingNames.addAll(bindingNames);
		Iterations.addAll(bindingSetIter, this.bindingSets);
	}

	public MutableTupleQueryResult(TupleQueryResult tqr) throws QueryEvaluationException {
		this(tqr.getBindingNames(), tqr);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public List<String> getBindingNames() {
		return new ArrayList<>(bindingNames);
	}

	public int size() {
		return bindingSets.size();
	}

	public BindingSet get(int index) {
		return bindingSets.get(index);
	}

	public int getIndex() {
		return currentIndex;
	}

	public void setIndex(int index) {
		if (index < 0 || index > bindingSets.size() + 1) {
			throw new IllegalArgumentException("Index out of range: " + index);
		}

		this.currentIndex = index;
	}

	@Override
	public boolean hasNext() {
		return currentIndex < bindingSets.size();
	}

	@Override
	public BindingSet next() {
		if (hasNext()) {
			BindingSet result = get(currentIndex);
			lastReturned = currentIndex;
			currentIndex++;
			return result;
		}

		throw new NoSuchElementException();
	}

	public boolean hasPrevious() {
		return currentIndex > 0;
	}

	public BindingSet previous() {
		if (hasPrevious()) {
			BindingSet result = bindingSets.get(currentIndex - 1);
			currentIndex--;
			lastReturned = currentIndex;
			return result;
		}

		throw new NoSuchElementException();
	}

	/**
	 * Moves the cursor to the start of the query result, just before the first binding set. After calling this method,
	 * the result can be iterated over from scratch.
	 */
	public void beforeFirst() {
		currentIndex = 0;
	}

	/**
	 * Moves the cursor to the end of the query result, just after the last binding set.
	 */
	public void afterLast() {
		currentIndex = bindingSets.size() + 1;
	}

	/**
	 * Inserts the specified binding set into the list. The binding set is inserted immediately before the next element
	 * that would be returned by {@link #next()}, if any, and after the next element that would be returned by
	 * {@link #previous}, if any. (If the table contains no binding sets, the new element becomes the sole element on
	 * the table.) The new element is inserted before the implicit cursor: a subsequent call to <tt>next()</tt> would be
	 * unaffected, and a subsequent call to <tt>previous()</tt> would return the new binding set.
	 *
	 * @param bindingSet The binding set to insert.
	 */
	public void insert(BindingSet bindingSet) {
		insert(currentIndex, bindingSet);
	}

	public void insert(int index, BindingSet bindingSet) {
		bindingSets.add(index, bindingSet);

		if (currentIndex > index) {
			currentIndex++;
		}

		lastReturned = -1;
	}

	public void append(BindingSet bindingSet) {
		bindingSets.add(bindingSet);
		lastReturned = -1;
	}

	public void set(BindingSet bindingSet) {
		if (lastReturned == -1) {
			throw new IllegalStateException();
		}

		set(lastReturned, bindingSet);
	}

	public BindingSet set(int index, BindingSet bindingSet) {
		return bindingSets.set(index, bindingSet);
	}

	@Override
	public void remove() {
		if (lastReturned == -1) {
			throw new IllegalStateException();
		}

		remove(lastReturned);

		if (currentIndex > lastReturned) {
			currentIndex--;
		}

		lastReturned = -1;
	}

	public BindingSet remove(int index) {
		BindingSet result = bindingSets.remove(index);

		if (currentIndex > index) {
			currentIndex--;
		}

		lastReturned = -1;

		return result;
	}

	public void clear() {
		bindingNames.clear();
		bindingSets.clear();
		currentIndex = 0;
		lastReturned = -1;
	}

	@Override
	public void close() {
		// no-op because we support multiple iterations
	}

	@Override
	public MutableTupleQueryResult clone() throws CloneNotSupportedException {
		MutableTupleQueryResult clone = (MutableTupleQueryResult) super.clone();
		clone.bindingNames.addAll(bindingNames);
		clone.bindingSets.addAll(bindingSets);
		return clone;
	}

}
