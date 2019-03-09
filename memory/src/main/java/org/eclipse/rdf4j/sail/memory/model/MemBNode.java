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
	transient private volatile MemStatementList subjectStatements;

	/**
	 * The list of statements for which this MemBNode is the object.
	 */
	transient private volatile MemStatementList objectStatements;

	/**
	 * The list of statements for which this MemBNode represents the context.
	 */
	transient private volatile MemStatementList contextStatements;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new MemBNode for a bnode ID.
	 * 
	 * @param creator
	 *        The object that is creating this MemBNode.
	 * @param id
	 *        bnode ID.
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
		return subjectStatements != null || objectStatements != null || contextStatements != null;
	}

	@Override
	public MemStatementList getSubjectStatementList() {
		if (subjectStatements == null) {
			return EMPTY_LIST;
		}
		else {
			return subjectStatements;
		}
	}

	@Override
	public int getSubjectStatementCount() {
		if (subjectStatements == null) {
			return 0;
		}
		else {
			return subjectStatements.size();
		}
	}

	@Override
	public void addSubjectStatement(MemStatement st) {
		if (subjectStatements == null) {
			subjectStatements = new MemStatementList(4);
		}

		subjectStatements.add(st);
	}

	@Override
	public void removeSubjectStatement(MemStatement st) {
		subjectStatements.remove(st);

		if (subjectStatements.isEmpty()) {
			subjectStatements = null;
		}
	}

	@Override
	public void cleanSnapshotsFromSubjectStatements(int currentSnapshot) {
		if (subjectStatements != null) {
			subjectStatements.cleanSnapshots(currentSnapshot);

			if (subjectStatements.isEmpty()) {
				subjectStatements = null;
			}
		}
	}

	@Override
	public MemStatementList getObjectStatementList() {
		if (objectStatements == null) {
			return EMPTY_LIST;
		}
		else {
			return objectStatements;
		}
	}

	@Override
	public int getObjectStatementCount() {
		if (objectStatements == null) {
			return 0;
		}
		else {
			return objectStatements.size();
		}
	}

	@Override
	public void addObjectStatement(MemStatement st) {
		if (objectStatements == null) {
			objectStatements = new MemStatementList(4);
		}

		objectStatements.add(st);
	}

	@Override
	public void removeObjectStatement(MemStatement st) {
		objectStatements.remove(st);

		if (objectStatements.isEmpty()) {
			objectStatements = null;
		}
	}

	@Override
	public void cleanSnapshotsFromObjectStatements(int currentSnapshot) {
		if (objectStatements != null) {
			objectStatements.cleanSnapshots(currentSnapshot);

			if (objectStatements.isEmpty()) {
				objectStatements = null;
			}
		}
	}

	@Override
	public MemStatementList getContextStatementList() {
		if (contextStatements == null) {
			return EMPTY_LIST;
		}
		else {
			return contextStatements;
		}
	}

	@Override
	public int getContextStatementCount() {
		if (contextStatements == null) {
			return 0;
		}
		else {
			return contextStatements.size();
		}
	}

	@Override
	public void addContextStatement(MemStatement st) {
		if (contextStatements == null) {
			contextStatements = new MemStatementList(4);
		}

		contextStatements.add(st);
	}

	@Override
	public void removeContextStatement(MemStatement st) {
		contextStatements.remove(st);

		if (contextStatements.isEmpty()) {
			contextStatements = null;
		}
	}

	@Override
	public void cleanSnapshotsFromContextStatements(int currentSnapshot) {
		if (contextStatements != null) {
			contextStatements.cleanSnapshots(currentSnapshot);

			if (contextStatements.isEmpty()) {
				contextStatements = null;
			}
		}
	}
}
