/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory.model;

import java.util.Arrays;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A StatementIterator that can iterate over a list of Statement objects. This iterator compares Resource and Literal
 * objects using the '==' operator, which is possible thanks to the extensive sharing of these objects in the
 * MemoryStore.
 */
public class MemStatementIterator<X extends Exception> extends LookAheadIteration<MemStatement, X> {
	public static final int MIN_SIZE_TO_CONSIDER_FOR_CACHE = 1000;

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The lists of statements over which to iterate, and its size (different from length). Always set the size before
	 * the array. If we get the array first and the size after, then the MemStatementList object could have resized the
	 * array in between so that the size > length.
	 */
	private final int statementListSize;
	private MemStatement[] statementList;

	/**
	 * The subject of statements to return, or null if any subject is OK.
	 */
	private final MemResource subject;

	/**
	 * The predicate of statements to return, or null if any predicate is OK.
	 */
	private final MemIRI predicate;

	/**
	 * The object of statements to return, or null if any object is OK.
	 */
	private final MemValue object;

	/**
	 * The context of statements to return, or null if any context is OK.
	 */
	private final MemResource[] contexts;

	/**
	 * Flag indicating whether this iterator should only return explicitly added statements or only return inferred
	 * statements.
	 * <p>
	 * If this has not been specified (null) and we should return both explicit and inferred statements, then the flag
	 * below will be set to true.
	 */
	private final boolean explicit;
	private final boolean explicitNotSpecified;

	/**
	 * Indicates which snapshot should be iterated over.
	 */
	private final int snapshot;
	private final boolean noIsolation;

	/**
	 * The index of the last statement that has been returned.
	 */
	private int statementIndex;

	/**
	 * The number of returned statements
	 */
	private int matchingStatements;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new MemStatementIterator that will iterate over the statements contained in the supplied
	 * MemStatementList searching for statements that match the specified pattern of subject, predicate, object and
	 * context(s).
	 *
	 * @param statementList the statements over which to iterate.
	 * @param subject       subject of pattern.
	 * @param predicate     predicate of pattern.
	 * @param object        object of pattern.
	 * @param contexts      context(s) of pattern.
	 */
	public MemStatementIterator(MemStatementList statementList, MemResource subject, MemIRI predicate, MemValue object,
			Boolean explicit, int snapshot, MemResource... contexts) throws InterruptedException {
		this.statementList = statementList.getStatements();
		this.statementListSize = statementList.getGuaranteedLastIndexInUse() + 1;
		assert this.statementListSize <= this.statementList.length;
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
		this.contexts = contexts;
		if (explicit == null) {
			this.explicitNotSpecified = true;
			this.explicit = false;
		} else {
			this.explicitNotSpecified = false;
			this.explicit = explicit;
		}
		this.snapshot = snapshot;
		this.noIsolation = snapshot < 0;
		this.statementIndex = 0;
	}

	public static LookAheadIteration<MemStatement, SailException> cacheAwareInstance(MemStatementList smallestList,
			MemResource subj, MemIRI pred, MemValue obj, Boolean explicit, int snapshot, MemResource[] memContexts,
			MemStatementIteratorCache iteratorCache) throws InterruptedException {

		if (smallestList.size() > MemStatementIterator.MIN_SIZE_TO_CONSIDER_FOR_CACHE) {
			return new CacheAwareIteration<>(
					new MemStatementIterator<>(smallestList, subj, pred, obj, explicit, snapshot, memContexts),
					iteratorCache);
		} else {
			return new MemStatementIterator<>(smallestList, subj, pred, obj, explicit, snapshot, memContexts);
		}
	}

	public static boolean cacheAwareHasStatement(MemStatementList smallestList, MemResource subj, MemIRI pred,
			MemValue obj, Boolean explicit, int snapshot, MemResource[] memContexts,
			MemStatementIteratorCache iteratorCache) throws InterruptedException {

		if (smallestList.size() > MemStatementIterator.MIN_SIZE_TO_CONSIDER_FOR_CACHE) {
			try (CacheAwareIteration<SailException> exceptionCacheAwareIteration = new CacheAwareIteration<>(
					new MemStatementIterator<>(smallestList, subj, pred, obj, explicit, snapshot, memContexts),
					iteratorCache)) {
				return exceptionCacheAwareIteration.hasNext();
			}
		} else {
			try (MemStatementIterator<SailException> exceptionMemStatementIterator = new MemStatementIterator<>(
					smallestList, subj, pred, obj, explicit, snapshot, memContexts)) {
				return exceptionMemStatementIterator.hasNext();
			}
		}
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Searches through statementList, starting from index <var>_nextStatementIdx + 1</var>, for statements that match
	 * the constraints that have been set for this iterator. If a matching statement has been found it will be stored in
	 * <var>_nextStatement</var> and <var>_nextStatementIdx</var> points to the index of this statement in
	 * <var>_statementList</var>. Otherwise, <var>_nextStatement</var> will set to <var>null</var>.
	 */
	@Override
	protected MemStatement getNextElement() {

		while (statementIndex < statementListSize) {
			// First getting the size to check if we are out-of-bounds is more expensive (cache wise) than having a
			// method in MemStatementList that does this for us.

			MemStatement statement = statementList[statementIndex++];

			if (statement != null && (statement.matchesSPO(subject, predicate, object))
					&& statement.matchesContext(contexts)
					&& matchesExplicitAndSnapshot(statement)) {
				// First check if we match the specified SPO, then check the context, then finally check the
				// explicit/inferred and snapshot.
				// Checking explicit/inferred and snapshot requires reading a volatile field, which is fairly slow and
				// the
				// reason we check this last.
				matchingStatements++;
				return statement;
			}

		}

		return null;
	}

	boolean matchesExplicitAndSnapshot(MemStatement st) {
		return (explicitNotSpecified || explicit == st.isExplicit()) &&
				(noIsolation || st.isInSnapshot(snapshot));
	}

	@Override
	protected void handleClose() throws X {
		statementList = null;
	}

	/**
	 * Returns true if this iterator was particularly costly and should be considered for caching
	 *
	 * @return true if it should be cached
	 */
	private boolean isCandidateForCache() {
		if (statementIndex == statementListSize) { // we will only consider caching if the iterator has been completely
			// consumed
			if (statementIndex > MIN_SIZE_TO_CONSIDER_FOR_CACHE) { // minimum 1000 statements need to have been checked
				// by the iterator
				if (matchingStatements == 0) { // if the iterator was effectively empty we can always cache it
					return true;
				} else if (matchingStatements < 100) { // we will not cache iterators that returned more than 99
					// statements
					double ratio = (statementIndex + 0.0) / matchingStatements;
					return ratio > 100; // for every returned statement we need to have checked 100 non-matching
					// statements
				}
			}
		}
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof MemStatementIterator)) {
			return false;
		}
		MemStatementIterator<?> that = (MemStatementIterator<?>) o;
		return explicit == that.explicit && explicitNotSpecified == that.explicitNotSpecified
				&& snapshot == that.snapshot && noIsolation == that.noIsolation
				&& subject == that.subject
				&& predicate == that.predicate && object == that.object
				&& Arrays.equals(contexts, that.contexts);
	}

	private int cachedHashCode = 0;

	@Override
	public int hashCode() {
		if (cachedHashCode == 0) {

			// Inlined Objects.hash(subject, predicate, object, explicit, explicitNotSpecified, snapshot, noIsolation)
			// to avoid array creation.
			int cachedHashCode = 1;
			cachedHashCode = 31 * cachedHashCode + (subject == null ? 0 : subject.hashCode());
			cachedHashCode = 31 * cachedHashCode + (predicate == null ? 0 : predicate.hashCode());
			cachedHashCode = 31 * cachedHashCode + (object == null ? 0 : object.hashCode());
			cachedHashCode = 31 * cachedHashCode + Boolean.hashCode(explicit);
			cachedHashCode = 31 * cachedHashCode + Boolean.hashCode(explicitNotSpecified);
			cachedHashCode = 31 * cachedHashCode + snapshot;
			cachedHashCode = 31 * cachedHashCode + Boolean.hashCode(noIsolation);

			if (contexts != null) {
				if (contexts.length == 1) {
					if (contexts[0] == null) {
						cachedHashCode += 23;
					} else {
						cachedHashCode = 29 * cachedHashCode + contexts[0].hashCode();
					}
				} else if (contexts.length > 0) {
					cachedHashCode = 31 * cachedHashCode + Arrays.hashCode(contexts);
				}
			}

			this.cachedHashCode = cachedHashCode;
		}
		return cachedHashCode;
	}

	@Override
	public String toString() {
		return "MemStatementIterator{" +
				"subject=" + subject +
				", predicate=" + predicate +
				", object=" + object +
				", contexts=" + Arrays.toString(contexts) +
				", explicit=" + explicit +
				", explicitNotSpecified=" + explicitNotSpecified +
				", snapshot=" + snapshot +
				", noIsolation=" + noIsolation +
				'}';
	}

	public Stats getStats() {
		return new Stats(statementIndex, matchingStatements);
	}

	static class Stats {
		private final int checkedStatements;
		private final int matchingStatements;

		public Stats(int checkStatements, int matchingStatements) {
			this.checkedStatements = checkStatements;
			this.matchingStatements = matchingStatements;
		}

		@Override
		public String toString() {
			return "Stats{" +
					"checkedStatements=" + checkedStatements +
					", matchingStatements=" + matchingStatements +
					'}';
		}
	}

	/**
	 * A wrapper for a MemStatementIterator that checks if the iterator should be cached and retrieves the cached one if
	 * that is the case.
	 *
	 * @author Håvard M. Ottestad
	 */
	private static class CacheAwareIteration<X extends Exception> extends LookAheadIteration<MemStatement, X> {

		private final MemStatementIteratorCache iteratorCache;
		private final MemStatementIterator<X> memStatementIterator;
		private final CloseableIteration<MemStatement, X> cachedIterator;
		private Exception e;

		private CacheAwareIteration(MemStatementIterator<X> memStatementIterator,
				MemStatementIteratorCache iteratorCache) throws X {
			try {
				if (iteratorCache.shouldBeCached(memStatementIterator)) {
					CloseableIteration<MemStatement, X> cachedIterator = null;
					try {
						cachedIterator = iteratorCache.getCachedIterator(memStatementIterator);
					} catch (Exception e) {
						this.e = e;
					}
					this.cachedIterator = cachedIterator;
					this.memStatementIterator = null;
				} else {
					this.memStatementIterator = memStatementIterator;
					this.cachedIterator = null;
				}

				this.iteratorCache = iteratorCache;
			} catch (Throwable t) {
				memStatementIterator.close();
				if (t instanceof RuntimeException) {
					throw t;
				}
				throw t;
			}

		}

		@Override
		protected MemStatement getNextElement() throws X {
			if (e != null) {
				throw ((X) e);
			}

			if (memStatementIterator != null) {
				if (memStatementIterator.hasNext()) {
					return memStatementIterator.next();
				}
			} else {
				if (cachedIterator.hasNext()) {
					return cachedIterator.next();
				}
			}

			return null;
		}

		@Override
		protected void handleClose() throws X {
			if (memStatementIterator != null) {
				if (memStatementIterator.isCandidateForCache()) {
					iteratorCache.incrementIteratorFrequencyMap(memStatementIterator);
				}
			} else {
				cachedIterator.close();
			}
		}
	}

}
