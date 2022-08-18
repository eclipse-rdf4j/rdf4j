/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory.model;

import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Triple;

/**
 * An Iteration that can iterate over a list of {@link Triple} objects.
 *
 * @author Jeen Broekstra
 */
public class MemTripleIterator<X extends Exception> extends LookAheadIteration<MemTriple, X> {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The lists of statements over which to iterate.
	 */
	private final MemStatement[] statementList;

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
	private final int statementListSize;

	/**
	 * The index of the last statement that has been returned.
	 */
	private int statementIndex;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new MemTripleIterator that will iterate over the triples contained in the supplied MemStatementList
	 * searching for triples that occur as either subject or object in those statements, and which match the specified
	 * pattern of subject, predicate, object.
	 *
	 * @param statementList the statements over which to iterate.
	 * @param subject       subject of pattern.
	 * @param predicate     predicate of pattern.
	 * @param object        object of pattern.
	 */
	public MemTripleIterator(MemStatementList statementList, MemResource subject, MemIRI predicate, MemValue object,
			int snapshot) throws InterruptedException {
		this.statementList = statementList.getStatements();
		this.statementListSize = statementList.getGuaranteedLastIndexInUse() + 1;
		assert this.statementListSize <= this.statementList.length;

		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
		this.snapshot = snapshot;

		this.statementIndex = -1;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Searches through statementList, starting from index <var>_nextStatementIdx + 1</var>, for triples that match the
	 * constraints that have been set for this iterator.
	 */
	@Override
	protected MemTriple getNextElement() {
		statementIndex++;

		for (; statementIndex < statementListSize; statementIndex++) {
			MemStatement statement = statementList[statementIndex];
			if (statement == null) {
				continue;
			}

			if (isInSnapshot(statement)) {
				if (statement.getSubject().isTriple() && statement.getSubject() instanceof MemTriple) {
					MemTriple triple = (MemTriple) statement.getSubject();
					if (triple.matchesSPO(subject, predicate, object)) {
						return triple;
					}
				} else if (statement.getObject().isTriple() && statement.getObject() instanceof MemTriple) {
					MemTriple triple = (MemTriple) statement.getObject();
					if (triple.matchesSPO(subject, predicate, object)) {
						return triple;
					}
				}
			}
		}

		// No more matching statements.
		return null;
	}

	private boolean isInSnapshot(MemStatement st) {
		return snapshot < 0 || st.isInSnapshot(snapshot);
	}
}
