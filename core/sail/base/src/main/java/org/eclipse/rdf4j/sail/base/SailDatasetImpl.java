/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
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

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.FilterIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A view of an {@link SailSource} that is derived from a backing {@link SailDataset}.
 *
 * @author James Leigh
 */
class SailDatasetImpl implements SailDataset {

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
			namespaces = new EmptyIteration<>();
		} else {
			namespaces = derivedFrom.getNamespaces();
		}
		Iterator<Map.Entry<String, String>> added = null;
		Set<String> removed = null;
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
		return new AbstractCloseableIteration<Namespace, SailException>() {

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
		return new AbstractCloseableIteration<Resource, SailException>() {

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
				|| contexts.length > 0 && deprecatedContexts != null
						&& deprecatedContexts.containsAll(Arrays.asList(contexts))) {
			iter = null;
		} else if (contexts.length > 0 && deprecatedContexts != null) {
			List<Resource> remaining = new ArrayList<>(Arrays.asList(contexts));
			remaining.removeAll(deprecatedContexts);
			iter = derivedFrom.getStatements(subj, pred, obj, remaining.toArray(new Resource[0]));
		} else {
			iter = derivedFrom.getStatements(subj, pred, obj, contexts);
		}
		Model deprecated = changes.getDeprecated();
		if (deprecated != null && iter != null) {
			iter = difference(iter, deprecated.filter(subj, pred, obj, contexts));
		}
		Model approved = changes.getApproved();
		if (approved != null && iter != null) {

			return new DistinctModelReducingUnionIteration(iter, approved, (m) -> m.filter(subj, pred, obj, contexts));

		} else if (approved != null) {
			Iterator<Statement> i = approved.filter(subj, pred, obj, contexts).iterator();
			return new CloseableIteratorIteration<>(i);
		} else if (iter != null) {
			return iter;
		} else {
			return new EmptyIteration<>();
		}
	}

	private CloseableIteration<? extends Statement, SailException> difference(
			CloseableIteration<? extends Statement, SailException> result, final Model excluded) {
		if (excluded.isEmpty()) {
			return result;
		}
		return new FilterIteration<Statement, SailException>(result) {

			@Override
			protected boolean accept(Statement stmt) {
				return !excluded.contains(stmt);
			}
		};
	}

}
