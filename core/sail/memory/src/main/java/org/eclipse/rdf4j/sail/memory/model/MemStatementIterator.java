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
package org.eclipse.rdf4j.sail.memory.model;

import java.util.Arrays;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A StatementIterator that can iterate over a list of Statement objects. This iterator compares Resource and Literal
 * objects using the '==' operator, which is possible thanks to the extensive sharing of these objects in the
 * MemoryStore.
 */
public class MemStatementIterator implements CloseableIteration<MemStatement, SailException> {
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
	private MemStatement nextElement;
	/**
	 * Flag indicating whether this iteration has been closed.
	 */
	private boolean closed = false;

	private final MemStatementIteratorCache iteratorCache;

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
			Boolean explicit, int snapshot, MemStatementIteratorCache iteratorCache, MemResource... contexts)
			throws InterruptedException {
		this.statementList = statementList.getStatements();
		this.statementListSize = statementList.getGuaranteedLastIndexInUse() + 1;
		assert this.statementListSize <= this.statementList.length;
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
		this.iteratorCache = iteratorCache;
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

	public static CloseableIteration<MemStatement, SailException> cacheAwareInstance(MemStatementList smallestList,
			MemResource subj, MemIRI pred, MemValue obj, Boolean explicit, int snapshot, MemResource[] memContexts,
			MemStatementIteratorCache iteratorCache) throws InterruptedException {

		if (smallestList.size() > MemStatementIterator.MIN_SIZE_TO_CONSIDER_FOR_CACHE) {
			MemStatementIterator memStatementIterator = null;
			try {
				memStatementIterator = new MemStatementIterator(smallestList, subj, pred, obj, explicit, snapshot,
						iteratorCache, memContexts);
				if (iteratorCache.shouldBeCached(memStatementIterator)) {
					return iteratorCache.getCachedIterator(memStatementIterator);
				} else {
					return memStatementIterator;
				}
			} catch (Throwable t) {
				if (memStatementIterator != null) {
					memStatementIterator.close();
				}
				throw t;
			}
		} else {
			return new MemStatementIterator(smallestList, subj, pred, obj, explicit, snapshot, null, memContexts);
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
	private MemStatement getNextElement() {

		while (statementIndex < statementListSize) {
			// First getting the size to check if we are out-of-bounds is more expensive (cache wise) than having a
			// method in MemStatementList that does this for us.

			MemStatement statement = statementList[statementIndex++];

			// First check if we match the specified SPO, then check the context, then finally check the
			// explicit/inferred and snapshot. Checking explicit/inferred and snapshot requires reading a volatile
			// field, which is fairly slow and the reason we check this last.
			if (statement != null && (statement.matchesSPO(subject, predicate, object))) {
				if (contexts.length > 0) {
					if (statement.matchesContext(contexts)
							&& matchesExplicitAndSnapshot(statement)) {

						matchingStatements++;
						return statement;
					}
				} else {
					if (matchesExplicitAndSnapshot(statement)) {
						matchingStatements++;
						return statement;
					}
				}

			}

		}

		return null;
	}

	boolean matchesExplicitAndSnapshot(MemStatement st) {
		return (explicitNotSpecified || explicit == st.isExplicit()) &&
				(noIsolation || st.isInSnapshot(snapshot));
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
		MemStatementIterator that = (MemStatementIterator) o;
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

	@Override
	public final boolean hasNext() {
		if (closed) {
			return false;
		}

		return lookAhead() != null;
	}

	@Override
	public final MemStatement next() {
		if (closed) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		MemStatement result = lookAhead();

		if (result != null) {
			nextElement = null;
			return result;
		} else {
			throw new NoSuchElementException();
		}
	}

	/**
	 * Fetches the next element if it hasn't been fetched yet and stores it in {@link #nextElement}.
	 *
	 * @return The next element, or null if there are no more results. @ If there is an issue getting the next element
	 *         or closing the iteration.
	 */
	private MemStatement lookAhead() {
		if (nextElement == null) {
			nextElement = getNextElement();

			if (nextElement == null) {
				close();
			}
		}
		return nextElement;
	}

	/**
	 * Throws an {@link UnsupportedOperationException}.
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void close() {
		if (!closed) {
			closed = true;
			statementList = null;
			if (iteratorCache != null && isCandidateForCache()) {
				iteratorCache.incrementIteratorFrequencyMap(this);
			}
		}
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
}
