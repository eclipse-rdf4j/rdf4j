/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory.model;

import org.eclipse.rdf4j.common.iteration.LookAheadIteration;

/**
 * A StatementIterator that can iterate over a list of Statement objects. This iterator compares Resource and Literal
 * objects using the '==' operator, which is possible thanks to the extensive sharing of these objects in the
 * MemoryStore.
 */
public class MemTripleIterator<X extends Exception> extends LookAheadIteration<MemTriple, X> {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The lists of statements over which to iterate.
	 */
	private final MemStatementList statementList;

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
	 * Indicates which snapshot should be iterated over.
	 */
	private final int snapshot;

	/**
	 * The index of the last statement that has been returned.
	 */
	private volatile int statementIdx;

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
	 */
	public MemTripleIterator(MemStatementList statementList, MemResource subject, MemIRI predicate, MemValue object,
			int snapshot) {
		this.statementList = statementList;
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
		this.snapshot = snapshot;

		this.statementIdx = -1;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Searches through statementList, starting from index <tt>_nextStatementIdx + 1</tt>, for statements that match the
	 * constraints that have been set for this iterator. If a matching statement has been found it will be stored in
	 * <tt>_nextStatement</tt> and <tt>_nextStatementIdx</tt> points to the index of this statement in
	 * <tt>_statementList</tt>. Otherwise, <tt>_nextStatement</tt> will set to <tt>null</tt>.
	 */
	@Override
	protected MemTriple getNextElement() {
		statementIdx++;

		for (; statementIdx < statementList.size(); statementIdx++) {
			MemStatement st = statementList.get(statementIdx);
			if (isInSnapshot(st)) {
				if (st.getSubject() instanceof MemTriple) {
					MemTriple triple = (MemTriple) st.getSubject();
					if (matchesPattern(triple)) {
						return triple;
					}
				}
				else if (st.getObject() instanceof MemTriple) {
					MemTriple triple = (MemTriple) st.getObject();
					if (matchesPattern(triple)) {
						return triple;
					}
				}
			}
		}

		// No more matching statements.
		return null;
	}

	private boolean matchesPattern(MemTriple triple) {
		if (!(subject == null || subject.equals(triple.getSubject()))) {
			return false;
		}
		if (!(predicate == null || predicate.equals(triple.getPredicate()))) {
			return false;
		}
		if (!(object == null || object.equals(triple.getObject()))) {
			return false;
		}
		return true;
	}

	private boolean isInSnapshot(MemStatement st) {
		return snapshot < 0 || st.isInSnapshot(snapshot);
	}
}
