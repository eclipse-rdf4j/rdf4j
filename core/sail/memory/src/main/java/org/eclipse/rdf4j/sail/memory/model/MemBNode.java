/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory.model;

import org.eclipse.rdf4j.model.BNode;

/**
 * A MemoryStore-specific extension of BNodeImpl giving it node properties.
 */
public class MemBNode extends MemResource implements BNode {

	private static final long serialVersionUID = -887382892580321647L;

	/*------------*
	 * Attributes *
	 *------------*/

	/**
	 * The object that created this MemBNode.
	 */
	transient final private Object creator;

	/**
	 * The list of statements for which this MemBNode is the object.
	 */
	transient private final MemStatementList objectStatements = new MemStatementList();
	/**
	 * The blank node's identifier.
	 */
	private final String id;

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
		this.id = id;
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
	}

	@Override
	public void removeObjectStatement(MemStatement st) throws InterruptedException {
		objectStatements.remove(st);

	}

	@Override
	public void cleanSnapshotsFromObjectStatements(int currentSnapshot) throws InterruptedException {
		objectStatements.cleanSnapshots(currentSnapshot);

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
	public String getID() {
		return id;
	}

	@Override
	public String stringValue() {
		return getID();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof BNode
				&& getID().equals(((BNode) o).getID());
	}

	@Override
	public int hashCode() {
		return getID().hashCode();
	}

	@Override
	public String toString() {
		return "_:" + getID();
	}
}
