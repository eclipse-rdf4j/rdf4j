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
public class MemStatementIterator<X extends Exception> extends LookAheadIteration<MemStatement, X> {

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
	 * The context of statements to return, or null if any context is OK.
	 */
	private final MemResource[] contexts;

	/**
	 * Flag indicating whether this iterator should only return explicitly added statements or only return inferred
	 * statements.
	 *
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
	private int statementIdx;

	/**
	 * True if there are no more elements to retrieve.
	 */
	private boolean exhausted;

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
			Boolean explicit, int snapshot, MemResource... contexts) {
		this.statementList = statementList;
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
		this.statementIdx = 0;
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
		while (!exhausted) {
			// First getting the size to check if we are out-of-bounds is more expensive (cache wise) than having a
			// method in MemStatementList that does this for us.
			MemStatement statement = statementList.getIfExists(statementIdx++);
			if (statement == null) {
				exhausted = true;
				break;
			}

			// First check if we match the specified SPO, then check the context, then finally check the
			// explicit/inferred and snapshot.
			// Checking explicit/inferred and snapshot requires reading a volatile field, which is fairly slow and the
			// reason we check this last.

			if ((statement.matchesSPO(subject, predicate, object)) && matchesContext(statement)
					&& matchesExplicitAndSnapshot(statement)) {
				return statement;
			}
		}

		return null;
	}

	private boolean matchesContext(MemStatement statement) {
		if (contexts != null && contexts.length > 0) {
			for (MemResource context : contexts) {
				if (statement.exactSameContext(context)) {
					return true;
				}
			}
			// if we get here there was no matching context
			return false;
		} else {
			// there is no context to check so we can return this statement
			return true;
		}
	}

	private boolean matchesExplicitAndSnapshot(MemStatement st) {
		return (explicitNotSpecified || explicit == st.isExplicit()) &&
				(noIsolation || st.isInSnapshot(snapshot));
	}

}
