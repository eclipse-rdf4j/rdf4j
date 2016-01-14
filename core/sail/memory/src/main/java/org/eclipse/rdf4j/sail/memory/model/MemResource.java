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
public interface MemResource extends MemValue, Resource {

	/**
	 * Gets the list of statements for which this MemResource is the subject.
	 * @return a MemStatementList containing the statements.
	 */
	public MemStatementList getSubjectStatementList();

	/**
	 * Gets the number of statements for which this MemResource is the subject.
	 * @return An integer larger than or equal to 0.
	 */
	public int getSubjectStatementCount();

	/**
	 * Adds a statement to this MemResource's list of statements for which it
	 * is the subject.
	 */
	public void addSubjectStatement(MemStatement st);

	/**
	 * Removes a statement from this MemResource's list of statements for which
	 * it is the subject.
	 */
	public void removeSubjectStatement(MemStatement st);

	/**
	 * Removes statements from old snapshots (those that have expired at or
	 * before the specified snapshot version) from this MemValue's list of
	 * statements for which it is the subject.
	 * 
	 * @param currentSnapshot
	 *        The current snapshot version.
	 */
	public void cleanSnapshotsFromSubjectStatements(int currentSnapshot);

	/**
	 * Gets the list of statements for which this MemResource represents the
	 * context.
	 * @return a MemStatementList containing the statements.
	 */
	public MemStatementList getContextStatementList();

	/**
	 * Gets the number of statements for which this MemResource represents the
	 * context.
	 * @return An integer larger than or equal to 0.
	 */
	public int getContextStatementCount();

	/**
	 * Adds a statement to this MemResource's list of statements for which
	 * it represents the context.
	 */
	public void addContextStatement(MemStatement st);

	/**
	 * Removes a statement from this MemResource's list of statements for which
	 * it represents the context.
	 */
	public void removeContextStatement(MemStatement st);
	
	/**
	 * Removes statements from old snapshots (those that have expired at or
	 * before the specified snapshot version) from this MemValue's list of
	 * statements for which it is the context.
	 * 
	 * @param currentSnapshot
	 *        The current snapshot version.
	 */
	public void cleanSnapshotsFromContextStatements(int currentSnapshot);
}
