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
package org.eclipse.rdf4j.sail.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.DistinctIteration;
import org.eclipse.rdf4j.common.iteration.DualUnionIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.common.iteration.UnionIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A view of an {@link SailSource} that is derived from a backing {@link SailDataset}.
 *
 * @author James Leigh
 */
class SailDatasetImpl implements SailDataset {

	private static final EmptyIteration<Triple, SailException> TRIPLE_EMPTY_ITERATION = new EmptyIteration<>();
	private static final EmptyIteration<Namespace, SailException> NAMESPACES_EMPTY_ITERATION = new EmptyIteration<>();
	private static final EmptyIteration<Statement, SailException> STATEMENT_EMPTY_ITERATION = new EmptyIteration<>();

	/**
	 * {@link SailDataset} of the backing {@link SailSource}.
	 */
	private final SailDataset derivedFrom;

	/**
	 * Changes that have not yet been {@link SailSource#flush()}ed to the backing {@link SailDataset}.
	 */
	private final Changeset changes;

	/**
	 * Create a derivative dataset that applies the given changeset. The life cycle of this and the given
	 * {@link SailDataset} are bound.
	 *
	 * @param derivedFrom will be released when this object is released
	 * @param changes     changeset to be observed with the given dataset
	 */
	public SailDatasetImpl(SailDataset derivedFrom, Changeset changes) {
		this.derivedFrom = derivedFrom;
		this.changes = changes;
		changes.addRefback(this);
	}

	@Override
	public String toString() {
		return changes + "\n" + derivedFrom;
	}

	@Override
	public void close() throws SailException {
		changes.removeRefback(this);
		derivedFrom.close();
	}

	@Override
	public String getNamespace(String prefix) throws SailException {
		Map<String, String> addedNamespaces = changes.getAddedNamespaces();
		if (addedNamespaces != null && addedNamespaces.containsKey(prefix)) {
			return addedNamespaces.get(prefix);
		}
		Set<String> removedPrefixes = changes.getRemovedPrefixes();
		if (removedPrefixes != null && removedPrefixes.contains(prefix) || changes.isNamespaceCleared()) {
			return null;
		}
		return derivedFrom.getNamespace(prefix);
	}

	@Override
	public CloseableIteration<? extends Namespace, SailException> getNamespaces() throws SailException {
		final CloseableIteration<? extends Namespace, SailException> namespaces;
		if (changes.isNamespaceCleared()) {
			namespaces = NAMESPACES_EMPTY_ITERATION;
		} else {
			namespaces = derivedFrom.getNamespaces();
		}
		Iterator<Map.Entry<String, String>> added = null;
		Set<String> removed;
		synchronized (this) {
			Map<String, String> addedNamespaces = changes.getAddedNamespaces();
			if (addedNamespaces != null) {
				added = addedNamespaces.entrySet().iterator();
			}
			removed = changes.getRemovedPrefixes();
		}
		if (added == null && removed == null) {
			return namespaces;
		}
		final Iterator<Map.Entry<String, String>> addedIter = added;
		final Set<String> removedSet = removed;
		return new AbstractCloseableIteration<>() {

			volatile Namespace next;

			@Override
			public boolean hasNext() throws SailException {
				if (isClosed()) {
					return false;
				}
				if (addedIter != null && addedIter.hasNext()) {
					return true;
				}
				Namespace toCheckNext = next;
				while (toCheckNext == null && namespaces.hasNext()) {
					toCheckNext = next = namespaces.next();
					if (removedSet != null && removedSet.contains(toCheckNext.getPrefix())) {
						toCheckNext = next = null;
					}
				}
				return toCheckNext != null;
			}

			@Override
			public Namespace next() throws SailException {
				if (isClosed()) {
					throw new NoSuchElementException("The iteration has been closed.");
				}
				if (addedIter != null && addedIter.hasNext()) {
					Entry<String, String> e = addedIter.next();
					return new SimpleNamespace(e.getKey(), e.getValue());
				}
				try {
					if (hasNext()) {
						Namespace toCheckNext = next;
						if (toCheckNext != null) {
							return toCheckNext;
						}
					}
					close();
					throw new NoSuchElementException("The iteration has been closed.");
				} finally {
					next = null;
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void handleClose() throws SailException {
				try {
					super.handleClose();
				} finally {
					namespaces.close();
				}
			}
		};
	}

	@Override
	public CloseableIteration<? extends Resource, SailException> getContextIDs() throws SailException {
		final CloseableIteration<? extends Resource, SailException> contextIDs;
		contextIDs = derivedFrom.getContextIDs();
		Iterator<Resource> added = null;
		Set<Resource> removed = null;
		synchronized (this) {
			Set<Resource> approvedContexts = changes.getApprovedContexts();
			if (approvedContexts != null) {
				added = approvedContexts.iterator();
			}
			Set<Resource> deprecatedContexts = changes.getDeprecatedContexts();
			if (deprecatedContexts != null) {
				removed = deprecatedContexts;
			}
		}
		if (added == null && removed == null) {
			return contextIDs;
		}
		final Iterator<Resource> addedIter = added;
		final Set<Resource> removedSet = removed;

		return new AbstractCloseableIteration<>() {

			volatile Resource next;

			@Override
			public boolean hasNext() throws SailException {
				if (isClosed()) {
					return false;
				}
				if (addedIter != null && addedIter.hasNext()) {
					return true;
				}
				Resource toCheckNext = next;
				while (toCheckNext == null && contextIDs.hasNext()) {
					toCheckNext = next = contextIDs.next();
					if (removedSet != null && removedSet.contains(toCheckNext)) {
						toCheckNext = next = null;
					}
				}
				return toCheckNext != null;
			}

			@Override
			public Resource next() throws SailException {
				if (isClosed()) {
					throw new NoSuchElementException("The iteration has been closed.");
				}
				if (addedIter != null && addedIter.hasNext()) {
					return addedIter.next();
				}
				try {
					if (hasNext()) {
						Resource toCheckNext = next;
						if (toCheckNext != null) {
							return toCheckNext;
						}
					}
					close();
					throw new NoSuchElementException("The iteration has been closed.");
				} finally {
					next = null;
				}
			}

			@Override
			public void remove() throws SailException {
				throw new UnsupportedOperationException();
			}

			@Override
			public void handleClose() throws SailException {
				try {
					super.handleClose();
				} finally {
					contextIDs.close();
				}
			}
		};
	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred, Value obj,
			Resource... contexts) throws SailException {
		Set<Resource> deprecatedContexts = changes.getDeprecatedContexts();
		CloseableIteration<? extends Statement, SailException> iter;
		if (changes.isStatementCleared()
				|| contexts == null && deprecatedContexts != null && deprecatedContexts.contains(null)
				|| contexts != null && contexts.length > 0 && deprecatedContexts != null
						&& deprecatedContexts.containsAll(Arrays.asList(contexts))) {
			iter = null;
		} else if (contexts != null && contexts.length > 0 && deprecatedContexts != null) {
			List<Resource> remaining = new ArrayList<>(Arrays.asList(contexts));
			remaining.removeAll(deprecatedContexts);
			iter = derivedFrom.getStatements(subj, pred, obj, remaining.toArray(new Resource[0]));
		} else {
			iter = derivedFrom.getStatements(subj, pred, obj, contexts);
		}
		if (changes.hasDeprecated() && iter != null) {
			iter = difference(iter, changes::hasDeprecated);
		}

		if (changes.hasApproved() && iter != null) {

			return new DistinctModelReducingUnionIteration(
					iter,
					changes::removeApproved,
					() -> changes.getApprovedStatements(subj, pred, obj, contexts));

		} else if (changes.hasApproved()) {
			Iterator<Statement> i = changes.getApprovedStatements(subj, pred, obj, contexts).iterator();
			return new CloseableIteratorIteration<>(i);
		} else if (iter != null) {
			return iter;
		} else {
			return STATEMENT_EMPTY_ITERATION;
		}
	}

	@Override
	public CloseableIteration<? extends Triple, SailException> getTriples(Resource subj, IRI pred, Value obj)
			throws SailException {

		CloseableIteration<? extends Triple, SailException> iter;
		if (changes.isStatementCleared()) {
			// nothing in the backing source is relevant, but we may still need to return approved data
			// from the changeset
			iter = null;
		} else {
			iter = derivedFrom.getTriples(subj, pred, obj);
		}

		if (changes.hasDeprecated() && iter != null) {
			iter = triplesDifference(iter, triple -> isDeprecated(triple, changes.getDeprecatedStatements()));
		}

		if (changes.hasApproved()) {
			if (iter != null) {
				CloseableIteratorIteration<? extends Triple, SailException> tripleExceptionCloseableIteratorIteration = new CloseableIteratorIteration<>(
						changes.getApprovedTriples(subj, pred, obj).iterator());

				// merge newly approved triples in the changeset with data from the backing source
				return new DistinctIteration<>(
						DualUnionIteration.getWildcardInstance(iter, tripleExceptionCloseableIteratorIteration));
			}

			// nothing relevant in the backing source, just return all matching approved triples from the changeset
			return new CloseableIteratorIteration<>(changes.getApprovedTriples(subj, pred, obj).iterator());
		} else if (iter != null) {
			return iter;
		} else {
			return TRIPLE_EMPTY_ITERATION;
		}
	}

	private CloseableIteration<? extends Statement, SailException> difference(
			CloseableIteration<? extends Statement, SailException> result, Function<Statement, Boolean> excluded) {
		return new FilterIteration<Statement, SailException>(result) {

			@Override
			protected boolean accept(Statement stmt) {
				return !excluded.apply(stmt);
			}
		};
	}

	private CloseableIteration<? extends Triple, SailException> triplesDifference(
			CloseableIteration<? extends Triple, SailException> result, Function<Triple, Boolean> excluded) {
		return new FilterIteration<Triple, SailException>(result) {

			@Override
			protected boolean accept(Triple stmt) {
				return !excluded.apply(stmt);
			}
		};
	}

	private boolean isDeprecated(Triple triple, List<Statement> deprecatedStatements) {
		// the triple is deprecated if the changeset deprecates all existing statements in the backing dataset that
		// involve this triple.
		try (CloseableIteration<? extends Statement, SailException> subjectStatements = derivedFrom
				.getStatements(triple, null, null)) {
			while (subjectStatements.hasNext()) {
				Statement st = subjectStatements.next();
				if (!deprecatedStatements.contains(st)) {
					return false;
				}
			}
		}
		try (CloseableIteration<? extends Statement, SailException> objectStatements = derivedFrom
				.getStatements(null, null, triple)) {
			while (objectStatements.hasNext()) {
				Statement st = objectStatements.next();
				if (!deprecatedStatements.contains(st)) {
					return false;
				}
			}
		}
		return true;
	}
}
