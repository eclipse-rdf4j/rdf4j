/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory.model;

import org.eclipse.rdf4j.model.impl.SimpleBNode;

/**
 * A MemoryStore-specific extension of BNodeImpl giving it node properties.
 */
public class MemBNode extends SimpleBNode implements MemResource {

	private static final long serialVersionUID = -887382892580321647L;

	/*------------*
	 * Attributes *
	 *------------*/

	/**
	 * The object that created this MemBNode.
	 */
	transient final private Object creator;

	/**
	 * The list of statements for which this MemBNode is the subject.
	 */
	transient private final MemStatementList subjectStatements = new MemStatementList();

	/**
	 * The list of statements for which this MemBNode is the object.
	 */
	transient private final MemStatementList objectStatements = new MemStatementList();

	/**
	 * The list of statements for which this MemBNode represents the context.
	 */
	transient private final MemStatementList contextStatements = new MemStatementList();

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new MemBNode for a bnode ID.
	 *
	 * @param creator The object that is creating this MemBNode.
	 * @param id      bnode ID.
	 */
	public MemBNode(Object creator, String id) {
		super(id);
		this.creator = creator;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public Object getCreator() {
		return creator;
	}

	@Override
	public boolean hasStatements() {
		return !subjectStatements.isEmpty() || !objectStatements.isEmpty() || !contextStatements.isEmpty();
	}

	@Override
	public MemStatementList getSubjectStatementList() {

		return subjectStatements;

	}

	@Override
	public int getSubjectStatementCount() {

		return subjectStatements.size();

	}

	@Override
	public void addSubjectStatement(MemStatement st) {

		subjectStatements.add(st);
	}

	@Override
	public void removeSubjectStatement(MemStatement st) {
		subjectStatements.remove(st);

	}

	@Override
	public void cleanSnapshotsFromSubjectStatements(int currentSnapshot) {
		subjectStatements.cleanSnapshots(currentSnapshot);

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
	public void addObjectStatement(MemStatement st) {

		objectStatements.add(st);
	}

	@Override
	public void removeObjectStatement(MemStatement st) {
		objectStatements.remove(st);

	}

	@Override
	public void cleanSnapshotsFromObjectStatements(int currentSnapshot) {
		objectStatements.cleanSnapshots(currentSnapshot);

	}

	@Override
	public MemStatementList getContextStatementList() {

		return contextStatements;

	}

	@Override
	public int getContextStatementCount() {

		return contextStatements.size();

	}

	@Override
	public void addContextStatement(MemStatement st) {

		contextStatements.add(st);
	}

	@Override
	public void removeContextStatement(MemStatement st) {
		contextStatements.remove(st);

	}

	@Override
	public void cleanSnapshotsFromContextStatements(int currentSnapshot) {
		contextStatements.cleanSnapshots(currentSnapshot);

	}

	@Override
	public boolean hasSubjectStatements() {
		return !subjectStatements.isEmpty();
	}

	@Override
	public boolean hasPredicateStatements() {
		return false;
	}

	@Override
	public boolean hasObjectStatements() {
		return !objectStatements.isEmpty();
	}

	@Override
	public boolean hasContextStatements() {
		return !contextStatements.isEmpty();
	}
}
