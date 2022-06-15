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

	public void addSubjectStatement(MemStatement st) throws InterruptedException {
		subjectStatements.add(st);
	}

	public void removeSubjectStatement(MemStatement st) throws InterruptedException {
		subjectStatements.remove(st);
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

	public void removeContextStatement(MemStatement st) throws InterruptedException {
		contextStatements.remove(st);
	}

	public void cleanSnapshotsFromContextStatements(int currentSnapshot) throws InterruptedException {
		contextStatements.cleanSnapshots(currentSnapshot);
	}
}
