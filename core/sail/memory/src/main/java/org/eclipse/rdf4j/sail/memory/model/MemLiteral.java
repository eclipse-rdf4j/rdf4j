/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory.model;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleLiteral;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

/**
 * A MemoryStore-specific extension of Literal giving it node properties.
 * 
 * @author Arjohn Kampman
 */
public class MemLiteral extends SimpleLiteral implements MemValue {

	private static final long serialVersionUID = 4288477328829845024L;

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The object that created this MemLiteral.
	 */
	transient private final Object creator;

	/**
	 * The list of statements for which this MemLiteral is the object.
	 */
	transient private volatile MemStatementList objectStatements;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new Literal which will get the supplied label.
	 * 
	 * @param creator
	 *        The object that is creating this MemLiteral.
	 * @param label
	 *        The label for this literal.
	 */
	public MemLiteral(Object creator, String label) {
		super(label, XMLSchema.STRING);
		this.creator = creator;
	}

	/**
	 * Creates a new Literal which will get the supplied label and language code.
	 * 
	 * @param creator
	 *        The object that is creating this MemLiteral.
	 * @param label
	 *        The label for this literal.
	 * @param lang
	 *        The language code of the supplied label.
	 */
	public MemLiteral(Object creator, String label, String lang) {
		super(label, lang);
		this.creator = creator;
	}

	/**
	 * Creates a new Literal which will get the supplied label and datatype.
	 * 
	 * @param creator
	 *        The object that is creating this MemLiteral.
	 * @param label
	 *        The label for this literal.
	 * @param datatype
	 *        The datatype of the supplied label.
	 */
	public MemLiteral(Object creator, String label, IRI datatype) {
		super(label, datatype);
		this.creator = creator;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public Object getCreator() {
		return creator;
	}

	public boolean hasStatements() {
		return objectStatements != null;
	}

	public MemStatementList getObjectStatementList() {
		if (objectStatements == null) {
			return EMPTY_LIST;
		}
		else {
			return objectStatements;
		}
	}

	public int getObjectStatementCount() {
		if (objectStatements == null) {
			return 0;
		}
		else {
			return objectStatements.size();
		}
	}

	public void addObjectStatement(MemStatement st) {
		if (objectStatements == null) {
			objectStatements = new MemStatementList(1);
		}

		objectStatements.add(st);
	}

	public void removeObjectStatement(MemStatement st) {
		objectStatements.remove(st);

		if (objectStatements.isEmpty()) {
			objectStatements = null;
		}
	}

	public void cleanSnapshotsFromObjectStatements(int currentSnapshot) {
		if (objectStatements != null) {
			objectStatements.cleanSnapshots(currentSnapshot);

			if (objectStatements.isEmpty()) {
				objectStatements = null;
			}
		}
	}
}
