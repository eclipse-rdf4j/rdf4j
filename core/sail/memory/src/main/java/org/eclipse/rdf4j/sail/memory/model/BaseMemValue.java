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

import net.agkn.hll.HLL;

/**
 * A MemoryStore-specific extension of Resource giving it subject statements.
 */
public abstract class BaseMemValue implements MemValue {

	/**
	 * The list of statements for which this MemResource is the subject.
	 */
	transient final MemStatementList subjectStatements = new MemStatementList();
	transient public final HLL subjectStatements_predicate = MemValue.getHLL();
	transient public final HLL subjectStatements_object = MemValue.getHLL();

	/**
	 * The list of statements for which this MemURI is the predicate.
	 */
	transient final MemStatementList predicateStatements = new MemStatementList();
	transient public final HLL predicateStatements_subjects = MemValue.getHLL();
	transient public final HLL predicateStatements_objects = MemValue.getHLL();

	/**
	 * The list of statements for which this MemResource is the object.
	 */
	transient final MemStatementList objectStatements = new MemStatementList();
	transient public final HLL objectStatements_subjects = MemValue.getHLL();
	transient public final HLL objectStatements_predicate = MemValue.getHLL();

	/**
	 * The list of statements for which this MemResource represents the context.
	 */
	transient final MemStatementList contextStatements = new MemStatementList();

	public MemStatementList getSubjectStatementList() {
		return subjectStatements;
	}

	public int getSubjectStatementCount() {
		return subjectStatements.size();
	}

	public void addSubjectStatement(MemStatement st) throws InterruptedException {
		subjectStatements.add(st);
		subjectStatements_object.addRaw(MemValue.getHashForHLL(st.getObject()));
		subjectStatements_predicate.addRaw(MemValue.getHashForHLL(st.getPredicate()));
	}

	public void cleanSnapshotsFromSubjectStatements(int currentSnapshot) throws InterruptedException {
		subjectStatements.cleanSnapshots(currentSnapshot);
	}

	@Override
	public boolean hasSubjectStatements() {
		return !subjectStatements.isEmpty();
	}

	@Override
	public boolean hasContextStatements() {
		return !contextStatements.isEmpty();
	}

	public MemStatementList getContextStatementList() {
		return contextStatements;
	}

	public int getContextStatementCount() {
		return contextStatements.size();
	}

	public void addContextStatement(MemStatement st) throws InterruptedException {
		contextStatements.add(st);
	}

	@Override
	public MemStatementList getObjectStatementList() {
		return objectStatements;
	}

	@Override
	public int getObjectStatementCount() {
		return objectStatements.size();
	}

	@Override
	public void addObjectStatement(MemStatement st) throws InterruptedException {
		objectStatements.add(st);
		objectStatements_subjects.addRaw(MemValue.getHashForHLL(st.getSubject()));
		objectStatements_predicate.addRaw(MemValue.getHashForHLL(st.getPredicate()));
	}

	public void cleanSnapshotsFromContextStatements(int currentSnapshot) throws InterruptedException {
		contextStatements.cleanSnapshots(currentSnapshot);
	}

	@Override
	public void cleanSnapshotsFromObjectStatements(int currentSnapshot) throws InterruptedException {
		objectStatements.cleanSnapshots(currentSnapshot);
	}

	@Override
	public boolean hasObjectStatements() {
		return !objectStatements.isEmpty();
	}

	/**
	 * Gets the list of statements for which this MemURI is the predicate.
	 *
	 * @return a MemStatementList containing the statements.
	 */
	public MemStatementList getPredicateStatementList() {
		return predicateStatements;
	}

	/**
	 * Gets the number of Statements for which this MemURI is the predicate.
	 *
	 * @return An integer larger than or equal to 0.
	 */
	public int getPredicateStatementCount() {
		return predicateStatements.size();
	}

	/**
	 * Adds a statement to this MemURI's list of statements for which it is the predicate.
	 */
	public void addPredicateStatement(MemStatement st) throws InterruptedException {
		predicateStatements.add(st);
		predicateStatements_subjects.addRaw(MemValue.getHashForHLL(st.getSubject()));
		predicateStatements_objects.addRaw(MemValue.getHashForHLL(st.getObject()));
	}

	/**
	 * Removes statements from old snapshots (those that have expired at or before the specified snapshot version) from
	 * this MemValue's list of statements for which it is the predicate.
	 *
	 * @param currentSnapshot The current snapshot version.
	 */
	public void cleanSnapshotsFromPredicateStatements(int currentSnapshot) throws InterruptedException {
		predicateStatements.cleanSnapshots(currentSnapshot);
	}

	@Override
	public boolean hasPredicateStatements() {
		return !predicateStatements.isEmpty();
	}

}
