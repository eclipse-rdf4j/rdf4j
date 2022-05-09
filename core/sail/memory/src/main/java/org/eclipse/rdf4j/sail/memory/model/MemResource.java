/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory.model;

import org.eclipse.rdf4j.model.Resource;

/**
 * A MemoryStore-specific extension of Resource giving it subject statements.
 */
public abstract class MemResource implements MemValue, Resource {

	/**
	 * The list of statements for which this MemURI is the subject.
	 */
	transient final MemStatementList subjectStatements = new MemStatementList();

	/**
	 * The list of statements for which this MemURI represents the context.
	 */
	transient final MemStatementList contextStatements = new MemStatementList();

	public MemStatementList getSubjectStatementList() {
		return subjectStatements;
	}

	public int getSubjectStatementCount() {
		return subjectStatements.size();
	}

	public void addSubjectStatement(MemStatement st) {
		subjectStatements.add(st);
	}

	public void removeSubjectStatement(MemStatement st) {
		subjectStatements.remove(st);
	}

	public void cleanSnapshotsFromSubjectStatements(int currentSnapshot) {
		subjectStatements.cleanSnapshots(currentSnapshot);
	}

	@Override
	public boolean hasSubjectStatements() {
		return !subjectStatements.isEmpty();
	}

//
//	MemStatementList getSubjectStatementList();
//
//	/**
//	 * Gets the number of statements for which this MemResource is the subject.
//	 *
//	 * @return An integer larger than or equal to 0.
//	 */
//	int getSubjectStatementCount();
//
//	/**
//	 * Adds a statement to this MemResource's list of statements for which it is the subject.
//	 *
//	 * @param st
//	 */
//	void addSubjectStatement(MemStatement st);
//
//	/**
//	 * Removes a statement from this MemResource's list of statements for which it is the subject.
//	 *
//	 * @param st
//	 */
//	void removeSubjectStatement(MemStatement st);
//
//	/**
//	 * Removes statements from old snapshots (those that have expired at or before the specified snapshot version) from
//	 * this MemValue's list of statements for which it is the subject.
//	 *
//	 * @param currentSnapshot The current snapshot version.
//	 */
//	void cleanSnapshotsFromSubjectStatements(int currentSnapshot);
//
//	/**
//	 * Gets the list of statements for which this MemResource represents the context.
//	 *
//	 * @return a MemStatementList containing the statements.
//	 */
//	MemStatementList getContextStatementList();
//
//	/**
//	 * Gets the number of statements for which this MemResource represents the context.
//	 *
//	 * @return An integer larger than or equal to 0.
//	 */
//	int getContextStatementCount();
//
//	/**
//	 * Adds a statement to this MemResource's list of statements for which it represents the context.
//	 *
//	 * @param st
//	 */
//	void addContextStatement(MemStatement st);
//
//	/**
//	 * Removes a statement from this MemResource's list of statements for which it represents the context.
//	 *
//	 * @param st
//	 */
//	void removeContextStatement(MemStatement st);
//
//	/**
//	 * Removes statements from old snapshots (those that have expired at or before the specified snapshot version) from
//	 * this MemValue's list of statements for which it is the context.
//	 *
//	 * @param currentSnapshot The current snapshot version.
//	 */
//	void cleanSnapshotsFromContextStatements(int currentSnapshot);

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

	public void addContextStatement(MemStatement st) {
		contextStatements.add(st);
	}

	public void removeContextStatement(MemStatement st) {
		contextStatements.remove(st);
	}

	public void cleanSnapshotsFromContextStatements(int currentSnapshot) {
		contextStatements.cleanSnapshots(currentSnapshot);
	}
}
