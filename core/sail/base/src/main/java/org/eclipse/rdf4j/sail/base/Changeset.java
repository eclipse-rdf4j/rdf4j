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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ModelFactory;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Statements;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.SailConflictException;
import org.eclipse.rdf4j.sail.SailException;

/**
 * Set of changes applied to an {@link SailSourceBranch} awaiting to be flushed into its backing {@link SailSource}.
 *
 * @author James Leigh
 */
abstract class Changeset implements SailSink, ModelFactory {

	AdderBasedReadWriteLock readWriteLock = new AdderBasedReadWriteLock();
	AdderBasedReadWriteLock refBacksReadWriteLock = new AdderBasedReadWriteLock();
	Semaphore prependLock = new Semaphore(1);

	/**
	 * Set of {@link SailDataset}s that are currently using this {@link Changeset} to derive the state of the
	 * {@link SailSource}.
	 */
	private List<SailDatasetImpl> refbacks;

	/**
	 * {@link Changeset}s that have been {@link #flush()}ed to the same {@link SailSourceBranch}, since this object was
	 * {@link #flush()}ed.
	 */
	private Set<Changeset> prepend;

	/**
	 * When in {@link IsolationLevels#SERIALIZABLE} this contains all the observed {@link StatementPattern}s that were
	 * observed by {@link ObservingSailDataset}.
	 */
	private Set<SimpleStatementPattern> observed;

	/**
	 * Statements that have been added as part of a transaction, but has not yet been committed.
	 *
	 * DO NOT EXPOSE THE MODEL OUTSIDE OF THIS CLASS BECAUSE IT IS NOT THREAD-SAFE
	 */
	private volatile Model approved;

	/**
	 * Explicit statements that have been removed as part of a transaction, but have not yet been committed.
	 *
	 * DO NOT EXPOSE THE MODEL OUTSIDE OF THIS CLASS BECAUSE IT IS NOT THREAD-SAFE
	 */
	private volatile Model deprecated;

	/**
	 * Set of contexts of the {@link #approved} statements.
	 */
	private Set<Resource> approvedContexts;

	/**
	 * Set of contexts that were passed to {@link #clear(Resource...)}.
	 */
	private volatile Set<Resource> deprecatedContexts;

	/**
	 * Additional namespaces added.
	 */
	private Map<String, String> addedNamespaces;

	/**
	 * Namespace prefixes that were removed.
	 */
	private Set<String> removedPrefixes;

	/**
	 * If all namespaces were removed, other than {@link #addedNamespaces}.
	 */
	private volatile boolean namespaceCleared;

	/**
	 * If all statements were removed, other than {@link #approved}.
	 */
	private volatile boolean statementCleared;

	private boolean closed;

	@Override
	public void close() throws SailException {
		closed = true;
		refbacks = null;
		prepend = null;
		observed = null;
		approved = null;
		deprecated = null;
		approvedContexts = null;
		deprecatedContexts = null;
		addedNamespaces = null;
		removedPrefixes = null;
	}

	public static Changeset simpleClone(Changeset from) {
		assert !from.closed;

		Changeset changeset = new Changeset() {
			@Override
			public void flush() throws SailException {

			}

			@Override
			public Model createEmptyModel() {
				return from.createEmptyModel();
			}
		};

		changeset.setChangeset(from);

		return changeset;
	}

	@Override
	public void prepare() throws SailException {
		if (prepend != null && observed != null) {
			for (SimpleStatementPattern p : observed) {
				Resource subj = p.getSubject();
				IRI pred = p.getPredicate();
				Value obj = p.getObject();
				Resource context = p.getContext();
				Resource[] contexts;
				if (p.isAllContexts()) {
					contexts = new Resource[0];
				} else {
					contexts = new Resource[] { context };
				}
				for (Changeset changeset : prepend) {
					if (changeset.hasApproved(subj, pred, obj, contexts)
							|| (changeset.hasDeprecated(subj, pred, obj, contexts))) {
						throw new SailConflictException("Observed State has Changed");
					}
				}
			}
		}
	}

	boolean hasApproved(Resource subj, IRI pred, Value obj, Resource[] contexts) {
		if (approved == null) {
			return false;
		}

		boolean readLock = readWriteLock.readLock();
		try {
			return approved.contains(subj, pred, obj, contexts);
		} finally {
			readWriteLock.unlockReader(readLock);
		}
	}

	boolean hasDeprecated(Resource subj, IRI pred, Value obj, Resource[] contexts) {
		if (deprecated == null) {
			return false;
		}

		boolean readLock = readWriteLock.readLock();
		try {
			return deprecated.contains(subj, pred, obj, contexts);
		} finally {
			readWriteLock.unlockReader(readLock);
		}
	}

	public void addRefback(SailDatasetImpl dataset) {

		long writeLock = refBacksReadWriteLock.writeLock();
		try {
			if (refbacks == null) {
				refbacks = new ArrayList<>();
			}
			refbacks.add(dataset);
		} finally {
			refBacksReadWriteLock.unlockWriter(writeLock);
		}

	}

	public void removeRefback(SailDatasetImpl dataset) {
		long writeLock = refBacksReadWriteLock.writeLock();
		try {
			if (refbacks != null) {
				refbacks.removeIf(d -> d == dataset);
			}
		} finally {
			refBacksReadWriteLock.unlockWriter(writeLock);
		}

	}

	public boolean isRefback() {
		boolean readLock = refBacksReadWriteLock.readLock();
		try {
			return refbacks != null && !refbacks.isEmpty();

		} finally {
			refBacksReadWriteLock.unlockReader(readLock);
		}

	}

	public void prepend(Changeset changeset) {
		prependLock.acquireUninterruptibly();

		try {
			if (prepend == null) {
				prepend = Collections.newSetFromMap(new IdentityHashMap<>());
			}
			prepend.add(changeset);
		} finally {
			prependLock.release();
		}

	}

	@Override
	public void setNamespace(String prefix, String name) {

		long writeLock = readWriteLock.writeLock();
		try {
			if (removedPrefixes == null) {
				removedPrefixes = new HashSet<>();
			}
			removedPrefixes.add(prefix);
			if (addedNamespaces == null) {
				addedNamespaces = new HashMap<>();
			}
			addedNamespaces.put(prefix, name);
		} finally {
			readWriteLock.unlockWriter(writeLock);
		}

	}

	@Override
	public void removeNamespace(String prefix) {
		long writeLock = readWriteLock.writeLock();
		try {
			if (addedNamespaces != null) {
				addedNamespaces.remove(prefix);
			}
			if (removedPrefixes == null) {
				removedPrefixes = new HashSet<>();
			}
			removedPrefixes.add(prefix);
		} finally {
			readWriteLock.unlockWriter(writeLock);
		}

	}

	@Override
	public void clearNamespaces() {
		namespaceCleared = true;

		long writeLock = readWriteLock.writeLock();
		try {

			if (removedPrefixes != null) {
				removedPrefixes.clear();
			}
			if (addedNamespaces != null) {
				addedNamespaces.clear();
			}
		} finally {
			readWriteLock.unlockWriter(writeLock);
		}

	}

	@Override
	public void observe(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailConflictException {

		long writeLock = readWriteLock.writeLock();
		try {
			if (observed == null) {
				observed = new HashSet<>();
			}
			if (contexts == null) {
				observed.add(new SimpleStatementPattern(subj, pred, obj, null, false));
			} else if (contexts.length == 0) {
				observed.add(new SimpleStatementPattern(subj, pred, obj, null, true));
			} else {
				for (Resource ctx : contexts) {
					observed.add(new SimpleStatementPattern(subj, pred, obj, ctx, false));
				}
			}
		} finally {
			readWriteLock.unlockWriter(writeLock);
		}

	}

	@Override
	public void observe(Resource subj, IRI pred, Value obj, Resource context)
			throws SailConflictException {

		long writeLock = readWriteLock.writeLock();
		try {
			if (observed == null) {
				observed = new HashSet<>();
			}

			observed.add(new SimpleStatementPattern(subj, pred, obj, context, false));

		} finally {
			readWriteLock.unlockWriter(writeLock);
		}

	}

	@Override
	public void clear(Resource... contexts) {
		long writeLock = readWriteLock.writeLock();
		try {
			if (contexts != null && contexts.length == 0) {
				statementCleared = true;

				if (approved != null) {
					approved.clear();
				}
				if (approvedContexts != null) {
					approvedContexts.clear();
				}
			} else {
				if (deprecatedContexts == null) {
					deprecatedContexts = new HashSet<>();
				}
				if (approved != null) {
					approved.remove(null, null, null, contexts);
				}
				if (approvedContexts != null && contexts != null) {
					for (Resource resource : contexts) {
						approvedContexts.remove(resource);
					}
				}
				if (contexts != null) {
					deprecatedContexts.addAll(Arrays.asList(contexts));
				}
			}
		} finally {
			readWriteLock.unlockWriter(writeLock);
		}

	}

	@Override
	public void approve(Statement statement) {

		long writeLock = readWriteLock.writeLock();
		try {

			if (deprecated != null) {
				deprecated.remove(statement);
			}
			if (approved == null) {
				approved = createEmptyModel();
			}
			approved.add(statement);
			if (statement.getContext() != null) {
				if (approvedContexts == null) {
					approvedContexts = new HashSet<>();
				}
				approvedContexts.add(statement.getContext());
			}
		} finally {
			readWriteLock.unlockWriter(writeLock);
		}

	}

	@Override
	public void approve(Resource subj, IRI pred, Value obj, Resource ctx) throws SailException {
		approve(Statements.statement(subj, pred, obj, ctx));
	}

	@Override
	public void deprecate(Statement statement) {
		long writeLock = readWriteLock.writeLock();
		try {
			if (approved != null) {
				approved.remove(statement);
			}
			if (deprecated == null) {
				deprecated = createEmptyModel();
			}
			deprecated.add(statement);
			Resource ctx = statement.getContext();
			if (approvedContexts != null && approvedContexts.contains(ctx)
					&& !approved.contains(null, null, null, ctx)) {
				approvedContexts.remove(ctx);
			}
		} finally {
			readWriteLock.unlockWriter(writeLock);
		}

	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (observed != null) {
			sb.append(observed.size());
			sb.append(" observations, ");
		}
		if (namespaceCleared) {
			sb.append("namespaceCleared, ");
		}
		if (removedPrefixes != null) {
			sb.append(removedPrefixes.size());
			sb.append(" removedPrefixes, ");
		}
		if (addedNamespaces != null) {
			sb.append(addedNamespaces.size());
			sb.append(" addedNamespaces, ");
		}
		if (statementCleared) {
			sb.append("statementCleared, ");
		}
		if (deprecatedContexts != null && !deprecatedContexts.isEmpty()) {
			sb.append(deprecatedContexts.size());
			sb.append(" deprecatedContexts, ");
		}
		if (deprecated != null) {
			sb.append(deprecated.size());
			sb.append(" deprecated, ");
		}
		if (approved != null) {
			sb.append(approved.size());
			sb.append(" approved, ");
		}
		if (sb.length() > 0) {
			return sb.substring(0, sb.length() - 2);
		} else {
			return super.toString();
		}
	}

	protected void setChangeset(Changeset from) {
		this.observed = from.observed;
		this.approved = from.approved;
		this.deprecated = from.deprecated;
		this.approvedContexts = from.approvedContexts;
		this.deprecatedContexts = from.deprecatedContexts;
		this.addedNamespaces = from.addedNamespaces;
		this.removedPrefixes = from.removedPrefixes;
		this.namespaceCleared = from.namespaceCleared;
		this.statementCleared = from.statementCleared;
	}

	public Set<SimpleStatementPattern> getObserved() {
		boolean readLock = readWriteLock.readLock();
		try {

			return observed == null ? null : Collections.unmodifiableSet(observed);
		} finally {
			readWriteLock.unlockReader(readLock);
		}
	}

	/**
	 * @deprecated Use getObserved() instead!
	 */
	@Deprecated
	public Set<StatementPattern> getObservations() {
		boolean readLock = readWriteLock.readLock();
		try {
			if (observed == null) {
				return null;
			}

			return observed.stream()
					.map(simpleStatementPattern -> new StatementPattern(
							new Var("s", simpleStatementPattern.getSubject()),
							new Var("p", simpleStatementPattern.getPredicate()),
							new Var("o", simpleStatementPattern.getObject()),
							simpleStatementPattern.isAllContexts() ? null
									: new Var("c", simpleStatementPattern.getContext())
					)
					)
					.collect(Collectors.toCollection(HashSet::new));
		} finally {
			readWriteLock.unlockReader(readLock);
		}
	}

	public Set<Resource> getApprovedContexts() {

		boolean readLock = readWriteLock.readLock();
		try {
			return cloneSet(approvedContexts);

		} finally {
			readWriteLock.unlockReader(readLock);
		}

	}

	public Set<Resource> getDeprecatedContexts() {
		if (deprecatedContexts == null) {
			return null;
		}

		boolean readLock = readWriteLock.readLock();
		try {
			return cloneSet(deprecatedContexts);
		} finally {
			readWriteLock.unlockReader(readLock);
		}

	}

	public boolean isStatementCleared() {
		return statementCleared;
	}

	public Map<String, String> getAddedNamespaces() {
		boolean readLock = readWriteLock.readLock();
		try {
			return addedNamespaces;

		} finally {
			readWriteLock.unlockReader(readLock);
		}

	}

	public Set<String> getRemovedPrefixes() {
		boolean readLock = readWriteLock.readLock();
		try {
			return cloneSet(removedPrefixes);

		} finally {
			readWriteLock.unlockReader(readLock);
		}

	}

	public boolean isNamespaceCleared() {
		return namespaceCleared;
	}

	public boolean hasDeprecated() {
		return deprecated != null && !deprecated.isEmpty();
	}

	boolean isChanged() {
		return approved != null || deprecated != null || approvedContexts != null
				|| deprecatedContexts != null || addedNamespaces != null
				|| removedPrefixes != null || statementCleared || namespaceCleared
				|| observed != null;
	}

	List<Statement> getDeprecatedStatements() {
		if (deprecated == null) {
			return Collections.emptyList();
		}

		boolean readLock = readWriteLock.readLock();
		try {
			return new ArrayList<>(deprecated);
		} finally {
			readWriteLock.unlockReader(readLock);
		}

	}

	List<Statement> getApprovedStatements() {
		if (approved == null) {
			return Collections.emptyList();
		}

		boolean readLock = readWriteLock.readLock();
		try {
			return new ArrayList<>(approved);
		} finally {
			readWriteLock.unlockReader(readLock);
		}

	}

	boolean hasDeprecated(Statement statement) {
		if (deprecated == null) {
			return false;
		}

		boolean readLock = readWriteLock.readLock();
		try {
			return deprecated.contains(statement);
		} finally {
			readWriteLock.unlockReader(readLock);
		}

	}

	boolean hasApproved() {
		return approved != null && !approved.isEmpty();
	}

	Iterable<Statement> getApprovedStatements(Resource subj, IRI pred, Value obj,
			Resource[] contexts) {
		if (approved == null) {
			return Collections.emptyList();
		}

		boolean readLock = readWriteLock.readLock();
		try {

			Iterable<Statement> statements = approved.getStatements(subj, pred, obj, contexts);

			// This is a synchronized context, users of this method will be allowed to use the results at their leisure.
			// We
			// provide a copy of the data so that there will be no concurrent modification exceptions!
			if (statements instanceof Collection) {
				return new ArrayList<>((Collection<? extends Statement>) statements);
			} else {
				return StreamSupport
						.stream(statements.spliterator(), false)
						.collect(Collectors.toList());
			}
		} finally {
			readWriteLock.unlockReader(readLock);
		}

	}

	Iterable<Triple> getApprovedTriples(Resource subj, IRI pred, Value obj) {
		if (approved == null) {
			return Collections.emptyList();
		}

		boolean readLock = readWriteLock.readLock();
		try {
			// TODO none of this is particularly well thought-out in terms of performance, but we are aiming
			// for functionally complete first.
			Stream<Triple> approvedSubjectTriples = approved.parallelStream()
					.filter(st -> st.getSubject().isTriple())
					.map(st -> (Triple) st.getSubject())
					.filter(t -> {
						if (subj != null && !subj.equals(t.getSubject())) {
							return false;
						}
						if (pred != null && !pred.equals(t.getPredicate())) {
							return false;
						}
						return obj == null || obj.equals(t.getObject());
					});

			Stream<Triple> approvedObjectTriples = approved.parallelStream()
					.filter(st -> st.getObject().isTriple())
					.map(st -> (Triple) st.getObject())
					.filter(t -> {
						if (subj != null && !subj.equals(t.getSubject())) {
							return false;
						}
						if (pred != null && !pred.equals(t.getPredicate())) {
							return false;
						}
						return obj == null || obj.equals(t.getObject());
					});

			return Stream.concat(approvedSubjectTriples, approvedObjectTriples).collect(Collectors.toList());
		} finally {
			readWriteLock.unlockReader(readLock);
		}
	}

	void removeApproved(Statement next) {
		long writeLock = readWriteLock.writeLock();
		try {
			if (approved != null) {
				approved.remove(next);
			}
		} finally {
			readWriteLock.unlockWriter(writeLock);
		}

	}

	private <T> Set<T> cloneSet(Set<T> set) {
		if (set == null) {
			return null;
		}
		return new HashSet<>(set);
	}

	void sinkApproved(SailSink sink) {
		boolean readLock = readWriteLock.readLock();
		try {
			if (approved != null) {
				sink.approveAll(approved, approvedContexts);
			}
		} finally {
			readWriteLock.unlockReader(readLock);
		}
	}

	void sinkDeprecated(SailSink sink) {
		boolean readLock = readWriteLock.readLock();
		try {
			if (deprecated != null) {
				sink.deprecateAll(deprecated);
			}
		} finally {
			readWriteLock.unlockReader(readLock);
		}
	}

	@Override
	public void approveAll(Set<Statement> approved, Set<Resource> approvedContexts) {
		long writeLock = readWriteLock.writeLock();
		try {

			if (deprecated != null) {
				deprecated.removeAll(approved);
			}
			if (this.approved == null) {
				this.approved = createEmptyModel();
			}
			this.approved.addAll(approved);

			if (approvedContexts != null) {
				if (this.approvedContexts == null) {
					this.approvedContexts = new HashSet<>();
				}
				this.approvedContexts.addAll(approvedContexts);
			}

		} finally {
			readWriteLock.unlockWriter(writeLock);
		}
	}

	@Override
	public void deprecateAll(Set<Statement> deprecated) {
		long writeLock = readWriteLock.writeLock();
		try {

			if (approved != null) {
				approved.removeAll(deprecated);
			}
			if (this.deprecated == null) {
				this.deprecated = createEmptyModel();
			}
			this.deprecated.addAll(deprecated);

			for (Statement statement : deprecated) {
				Resource ctx = statement.getContext();
				if (approvedContexts != null && approvedContexts.contains(ctx)
						&& !approved.contains(null, null, null, ctx)) {
					approvedContexts.remove(ctx);
				}
			}

		} finally {
			readWriteLock.unlockWriter(writeLock);
		}
	}

	public static class SimpleStatementPattern {
		final private Resource subject;
		final private IRI predicate;
		final private Value object;
		final private Resource context;

		// true if the context is the union of all contexts
		final private boolean allContexts;

		public SimpleStatementPattern(Resource subject, IRI predicate, Value object, Resource context,
				boolean allContexts) {
			this.subject = subject;
			this.predicate = predicate;
			this.object = object;
			this.context = context;
			this.allContexts = allContexts;
		}

		public Resource getSubject() {
			return subject;
		}

		public IRI getPredicate() {
			return predicate;
		}

		public Value getObject() {
			return object;
		}

		public Resource getContext() {
			return context;
		}

		public boolean isAllContexts() {
			return allContexts;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			SimpleStatementPattern that = (SimpleStatementPattern) o;
			return allContexts == that.allContexts && Objects.equals(subject, that.subject)
					&& Objects.equals(predicate, that.predicate) && Objects.equals(object, that.object)
					&& Objects.equals(context, that.context);
		}

		@Override
		public int hashCode() {
			int result = 1;

			result = 31 * result + (subject == null ? 0 : subject.hashCode());
			result = 31 * result + (predicate == null ? 0 : ((Object) predicate).hashCode());
			result = 31 * result + (object == null ? 0 : object.hashCode());
			result = 31 * result + (context == null ? 0 : context.hashCode());
			result = 31 * result + ((Object) allContexts).hashCode();

			return result;
		}
	}

	private static class AdderBasedReadWriteLock {

		// StampedLock for handling writers.
		private final StampedLock lock = new StampedLock();

		// LongAdder for handling readers. When the count is equal then there are no active readers.
		private final LongAdder readersLocked = new LongAdder();
		private final LongAdder readersUnlocked = new LongAdder();

		public boolean readLock() {
			while (true) {
				readersLocked.increment();
				if (!lock.isWriteLocked()) {
					// Everything is good! We have acquired a read-lock and there are no active writers.
					return true;
				} else {
					// Release our read lock so we don't block any writers.
					readersUnlocked.increment();
					while (lock.isWriteLocked()) {
						Thread.onSpinWait();
					}
				}
			}
		}

		public void unlockReader(boolean locked) {
			if (locked) {
				readersUnlocked.increment();
			} else {
				throw new IllegalMonitorStateException();
			}
		}

		public long writeLock() {
			// Acquire a write-lock.
			long writeStamp = lock.writeLock();

			// Wait for active readers to finish.
			while (true) {
				// The order is important here.
				long unlockedSum = readersUnlocked.sum();
				long lockedSum = readersLocked.sum();
				if (unlockedSum == lockedSum) {
					// No active readers.
					return writeStamp;
				} else {
					Thread.onSpinWait();
				}

			}
		}

		public void unlockWriter(long stamp) {
			if (stamp != 0) {
				lock.unlockWrite(stamp);
			} else {
				throw new IllegalMonitorStateException();
			}
		}

	}

}
