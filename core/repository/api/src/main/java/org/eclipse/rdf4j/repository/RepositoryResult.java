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
package org.eclipse.rdf4j.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.DistinctIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.iterator.CloseableIterationIterator;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

/**
 * A RepositoryResult is a result collection of objects (for example {@link org.eclipse.rdf4j.model.Statement} ,
 * {@link org.eclipse.rdf4j.model.Namespace}, or {@link Resource} objects) that can be iterated over. It keeps an open
 * connection to the backend for lazy retrieval of individual results. Additionally it has some utility methods to fetch
 * all results and add them to a collection.
 * <p>
 * By default, a RepositoryResult is not necessarily a (mathematical) set: it may contain duplicate objects. Duplicate
 * filtering can be {@link #enableDuplicateFilter() switched on}, but this should not be used lightly as the filtering
 * mechanism is potentially memory-intensive.
 * <p>
 * A RepositoryResult needs to be {@link #close() closed} after use to free up any resources (open connections, read
 * locks, etc.) it has on the underlying repository.
 *
 * @see RepositoryConnection#getStatements(Resource, IRI, Value, boolean, Resource[])
 * @see RepositoryConnection#getNamespaces()
 * @see RepositoryConnection#getContextIDs()
 * @author Jeen Broekstra
 * @author Arjohn Kampman
 */
public class RepositoryResult<T> extends AbstractCloseableIteration<T, RepositoryException> implements Iterable<T> {

	private CloseableIteration<? extends T, RepositoryException> wrappedIter;

	public RepositoryResult(CloseableIteration<? extends T, RepositoryException> iter) {
		assert iter != null;
		wrappedIter = iter;
	}

	@Override
	public boolean hasNext() throws RepositoryException {
		return wrappedIter.hasNext();
	}

	@Override
	public T next() throws RepositoryException {
		return wrappedIter.next();
	}

	@Override
	public void remove() throws RepositoryException {
		wrappedIter.remove();
	}

	@Override
	protected void handleClose() throws RepositoryException {
		try {
			super.handleClose();
		} finally {
			Iterations.closeCloseable(wrappedIter);
		}
	}

	/**
	 * Switches on duplicate filtering while iterating over objects. The RepositoryResult will keep track of the
	 * previously returned objects in a {@link java.util.Set} and on calling next() or hasNext() will ignore any objects
	 * that already occur in this Set.
	 * <P>
	 * Caution: use of this filtering mechanism is potentially memory-intensive.
	 *
	 * @throws RepositoryException if a problem occurred during initialization of the filter.
	 */
	public void enableDuplicateFilter() throws RepositoryException {
		if (wrappedIter instanceof DistinctIteration) {
			return;
		}

		wrappedIter = new DistinctIteration<T, RepositoryException>(wrappedIter);
	}

	/**
	 * Returns a {@link List} containing all objects of this RepositoryResult in order of iteration. The
	 * RepositoryResult is fully consumed and automatically closed by this operation.
	 * <P>
	 * Note: use this method with caution! It pulls the entire RepositoryResult in memory and as such is potentially
	 * very memory-intensive.
	 *
	 * @return a List containing all objects of this RepositoryResult.
	 * @throws RepositoryException if a problem occurred during retrieval of the results.
	 * @see #addTo(Collection)
	 * @deprecated Use {@link Iterations#asList(Iteration)} instead.
	 */
	@Deprecated
	public List<T> asList() throws RepositoryException {
		return addTo(new ArrayList<>());
	}

	/**
	 * Adds all objects of this RepositoryResult to the supplied collection. The RepositoryResult is fully consumed and
	 * automatically closed by this operation.
	 *
	 * @return A reference to the collection that was supplied.
	 * @throws RepositoryException if a problem occurred during retrieval of the results.
	 * @deprecated Use {@link Iterations#addAll(Iteration, Collection)} instead.
	 */
	@Deprecated
	public <C extends Collection<T>> C addTo(C collection) throws RepositoryException {
		try {
			while (hasNext()) {
				collection.add(next());
			}

			return collection;
		} finally {
			close();
		}
	}

	@Override
	public Iterator<T> iterator() {
		return new CloseableIterationIterator<>(this);
	}

}
