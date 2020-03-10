/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.base;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ModelFactory;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An {@link SailSource} that keeps a delta of its state from a backing {@link SailSource}.
 *
 * @author James Leigh
 */
class SailSourceBranch implements SailSource {

	private static final Logger logger = LoggerFactory.getLogger(SailSourceBranch.class);

	/**
	 * Used to prevent changes to this object's field from multiple threads.
	 */
	private final ReentrantLock semaphore = new ReentrantLock();

	/**
	 * The difference between this {@link SailSource} and the backing {@link SailSource}.
	 */
	private final ArrayDeque<Changeset> changes = new ArrayDeque<>();

	/**
	 * {@link SailSink} that have been created, but not yet {@link SailSink#flush()}ed to this {@link SailSource}.
	 */
	private final Collection<Changeset> pending = new ArrayList<>();

	/**
	 * Set of open {@link SailDataset} for this {@link SailSource}.
	 */
	private final Collection<SailDataset> observers = new ArrayList<>();

	/**
	 * The underly {@link SailSource} this {@link SailSource} is derived from.
	 */
	private final SailSource backingSource;

	/**
	 * The {@link Model} instances that should be used to store {@link SailSink#approve(Resource, IRI, Value, Resource)}
	 * and {@link SailSink#deprecate(Resource, IRI, Value, Resource)} statements.
	 */
	private final ModelFactory modelFactory;

	/**
	 * If this {@link SailSource} should be flushed to the backing {@link SailSource} when it is not in use.
	 */
	private final boolean autoFlush;

	/**
	 * Non-null when in {@link IsolationLevels#SNAPSHOT} (or higher) mode.
	 */
	private SailDataset snapshot;

	/**
	 * Non-null when in {@link IsolationLevels#SERIALIZABLE} (or higher) mode.
	 */
	private SailSink serializable;

	/**
	 * Non-null after {@link #prepare()}, but before {@link #flush()}.
	 */
	private SailSink prepared;

	/**
	 * Creates a new in-memory {@link SailSource} derived from the given {@link SailSource}.
	 *
	 * @param backingSource
	 */
	public SailSourceBranch(SailSource backingSource) {
		this(backingSource, new DynamicModelFactory(), false);
	}

	/**
	 * Creates a new {@link SailSource} derived from the given {@link SailSource}.
	 *
	 * @param backingSource
	 * @param modelFactory
	 */
	public SailSourceBranch(SailSource backingSource, ModelFactory modelFactory) {
		this(backingSource, modelFactory, false);
	}

	/**
	 * Creates a new {@link SailSource} derived from the given {@link SailSource} and if <code>autoFlush</code> is true,
	 * will automatically call {@link #flush()} when not in use.
	 *
	 * @param backingSource
	 * @param modelFactory
	 * @param autoFlush
	 */
	public SailSourceBranch(SailSource backingSource, ModelFactory modelFactory, boolean autoFlush) {
		this.backingSource = backingSource;
		this.modelFactory = modelFactory;
		this.autoFlush = autoFlush;
	}

	@Override
	public void close() throws SailException {
		semaphore.lock();
		try {
			try {
				try {
					SailDataset toCloseSnapshot = snapshot;
					snapshot = null;
					if (toCloseSnapshot != null) {
						toCloseSnapshot.close();
					}
				} finally {
					SailSink toCloseSerializable = serializable;
					serializable = null;
					if (toCloseSerializable != null) {
						toCloseSerializable.close();
					}
				}
			} finally {
				SailSink toClosePrepared = prepared;
				prepared = null;
				if (toClosePrepared != null) {
					toClosePrepared.close();
				}

			}
		} finally {
			semaphore.unlock();
		}
	}

	@Override
	public SailSink sink(IsolationLevel level) throws SailException {
		Changeset changeset = new Changeset() {

			private boolean prepared;

			@Override
			public void prepare() throws SailException {
				if (!prepared) {
					preparedChangeset(this);
					prepared = true;
				}
				super.prepare();
			}

			@Override
			public void flush() throws SailException {
				merge(this);
			}

			@Override
			public void close() throws SailException {
				try {
					super.close();
				} finally {
					if (prepared) {
						closeChangeset(this);
						prepared = false;
					}
					autoFlush();
				}
			}

			@Override
			public Model createEmptyModel() {
				return modelFactory.createEmptyModel();
			}
		};
		try {
			semaphore.lock();
			pending.add(changeset);
		} finally {
			semaphore.unlock();
		}
		return changeset;
	}

	@Override
	public SailDataset dataset(IsolationLevel level) throws SailException {
		SailDataset dataset = new DelegatingSailDataset(derivedFromSerializable(level)) {

			@Override
			public void close() throws SailException {
				super.close();
				try {
					semaphore.lock();
					observers.remove(this);
					compressChanges();
					autoFlush();
				} finally {
					semaphore.unlock();
				}
			}
		};
		try {
			semaphore.lock();
			observers.add(dataset);
		} finally {
			semaphore.unlock();
		}
		return dataset;
	}

	@Override
	public SailSource fork() {
		return new SailSourceBranch(this, modelFactory);
	}

	@Override
	public void prepare() throws SailException {
		try {
			semaphore.lock();
			if (!changes.isEmpty()) {
				if (prepared == null && serializable == null) {
					prepared = backingSource.sink(IsolationLevels.NONE);
				} else if (prepared == null) {
					prepared = serializable;
				}
				prepare(prepared);
				prepared.prepare();
			}
		} finally {
			semaphore.unlock();
		}
	}

	@Override
	public void flush() throws SailException {
		try {
			semaphore.lock();
			if (!changes.isEmpty()) {
				if (prepared == null) {
					prepare();
				}
				flush(prepared);
				prepared.flush();
				try {
					if (prepared != serializable) {
						prepared.close();
					}
				} finally {
					prepared = null;
				}
			}
		} finally {
			semaphore.unlock();
		}
	}

	public boolean isChanged() {
		try {
			semaphore.lock();
			return !changes.isEmpty();
		} finally {
			semaphore.unlock();
		}
	}

	@Override
	public String toString() {
		return backingSource.toString() + "\n" + changes.toString();
	}

	void preparedChangeset(Changeset changeset) {
		semaphore.lock();
	}

	void merge(Changeset change) {
		try {
			semaphore.lock();
			pending.remove(change);
			if (isChanged(change)) {
				Changeset merged;
				changes.add(change);
				compressChanges();
				merged = changes.getLast();
				for (Changeset c : pending) {
					c.prepend(merged);
				}
			}
		} finally {
			semaphore.unlock();
		}
	}

	void compressChanges() {
		try {
			semaphore.lock();
			while (changes.size() > 1) {
				Changeset pop = changes.removeLast();
				if (changes.peekLast().isRefback()) {
					changes.addLast(pop);
					break;
				}

				try {
					prepare(pop, changes.getLast());
					flush(pop, changes.getLast());
				} catch (SailException e) {
					// Changeset does not throw SailException
					throw new AssertionError(e);
				}
			}

		} finally {
			semaphore.unlock();
		}
	}

	void closeChangeset(Changeset changeset) {
		semaphore.unlock();
	}

	void autoFlush() throws SailException {
		if (autoFlush && semaphore.tryLock()) {
			try {
				if (observers.isEmpty()) {
					flush();
				}
			} finally {
				semaphore.unlock();
			}
		}
	}

	private boolean isChanged(Changeset change) {
		return change.getApproved() != null || change.getDeprecated() != null || change.getApprovedContexts() != null
				|| change.getDeprecatedContexts() != null || change.getAddedNamespaces() != null
				|| change.getRemovedPrefixes() != null || change.isStatementCleared() || change.isNamespaceCleared()
				|| change.getObservations() != null;
	}

	private SailDataset derivedFromSerializable(IsolationLevel level) throws SailException {
		try {
			semaphore.lock();
			if (serializable == null && level.isCompatibleWith(IsolationLevels.SERIALIZABLE)) {
				serializable = backingSource.sink(level);
			}
			SailDataset derivedFrom = derivedFromSnapshot(level);
			if (serializable == null) {
				return derivedFrom;
			} else {
				return new ObservingSailDataset(derivedFrom, sink(level));
			}
		} finally {
			semaphore.unlock();
		}
	}

	private SailDataset derivedFromSnapshot(IsolationLevel level) throws SailException {
		try {
			semaphore.lock();
			SailDataset derivedFrom;
			if (this.snapshot != null) {
				// this object is already has at least snapshot isolation
				derivedFrom = new DelegatingSailDataset(this.snapshot) {

					@Override
					public void close() throws SailException {
						// don't close snapshot yet
					}
				};
			} else {
				derivedFrom = backingSource.dataset(level);
				if (level.isCompatibleWith(IsolationLevels.SNAPSHOT)) {
					this.snapshot = derivedFrom;
					// don't release snapshot until this SailSource is released
					derivedFrom = new DelegatingSailDataset(derivedFrom) {

						@Override
						public void close() throws SailException {
							// don't close snapshot yet
						}
					};
				}
			}
			Iterator<Changeset> iter = changes.iterator();
			while (iter.hasNext()) {
				derivedFrom = new SailDatasetImpl(derivedFrom, iter.next());
			}
			return derivedFrom;
		} finally {
			semaphore.unlock();
		}
	}

	private void prepare(SailSink sink) throws SailException {
		try {
			semaphore.lock();
			Iterator<Changeset> iter = changes.iterator();
			while (iter.hasNext()) {
				prepare(iter.next(), sink);
			}
		} finally {
			semaphore.unlock();
		}
	}

	private void prepare(Changeset change, SailSink sink) throws SailException {
		Set<StatementPattern> observations = change.getObservations();
		if (observations != null) {
			for (StatementPattern p : observations) {
				Resource subj = (Resource) p.getSubjectVar().getValue();
				IRI pred = (IRI) p.getPredicateVar().getValue();
				Value obj = p.getObjectVar().getValue();
				Var ctxVar = p.getContextVar();
				if (ctxVar == null) {
					sink.observe(subj, pred, obj);
				} else {
					sink.observe(subj, pred, obj, (Resource) ctxVar.getValue());
				}
			}
		}
	}

	private void flush(SailSink sink) throws SailException {
		try {
			semaphore.lock();
			if (changes.size() == 1 && !changes.getFirst().isRefback() && sink instanceof Changeset
					&& !isChanged((Changeset) sink)) {
				// one change to apply that is not in use to an empty Changeset
				Changeset dst = (Changeset) sink;
				dst.setChangeset(changes.pop());
			} else {
				Iterator<Changeset> iter = changes.iterator();
				while (iter.hasNext()) {
					flush(iter.next(), sink);
					iter.remove();
				}
			}
		} finally {
			semaphore.unlock();
		}
	}

	private void flush(Changeset change, SailSink sink) throws SailException {
		prepare(change, sink);
		if (change.isNamespaceCleared()) {
			sink.clearNamespaces();
		}
		Set<String> removedPrefixes = change.getRemovedPrefixes();
		if (removedPrefixes != null) {
			for (String prefix : removedPrefixes) {
				sink.removeNamespace(prefix);
			}
		}
		Map<String, String> addedNamespaces = change.getAddedNamespaces();
		if (addedNamespaces != null) {
			for (Map.Entry<String, String> e : addedNamespaces.entrySet()) {
				sink.setNamespace(e.getKey(), e.getValue());
			}
		}
		if (change.isStatementCleared()) {
			sink.clear();
		}
		Set<Resource> deprecatedContexts = change.getDeprecatedContexts();
		if (deprecatedContexts != null && !deprecatedContexts.isEmpty()) {
			sink.clear(deprecatedContexts.toArray(new Resource[0]));
		}
		Model deprecated = change.getDeprecated();
		if (deprecated != null) {
			for (Statement st : deprecated) {
				sink.deprecate(st);
			}
		}
		Model approved = change.getApproved();
		if (approved != null) {
			for (Statement st : approved) {
				sink.approve(st);
			}
		}
	}

}
