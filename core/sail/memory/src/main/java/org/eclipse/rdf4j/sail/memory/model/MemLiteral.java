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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleLiteral;
import org.eclipse.rdf4j.model.vocabulary.XSD;

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
	transient private final MemStatementList objectStatements = new MemStatementList();

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new Literal which will get the supplied label.
	 *
	 * @param creator The object that is creating this MemLiteral.
	 * @param label   The label for this literal.
	 */
	public MemLiteral(Object creator, String label) {
		super(label, XSD.STRING);
		this.creator = creator;
	}

	/**
	 * Creates a new Literal which will get the supplied label and language code.
	 *
	 * @param creator The object that is creating this MemLiteral.
	 * @param label   The label for this literal.
	 * @param lang    The language code of the supplied label.
	 */
	public MemLiteral(Object creator, String label, String lang) {
		super(label, lang);
		this.creator = creator;
	}

	/**
	 * Creates a new Literal which will get the supplied label and datatype.
	 *
	 * @param creator  The object that is creating this MemLiteral.
	 * @param label    The label for this literal.
	 * @param datatype The datatype of the supplied label.
	 */
	public MemLiteral(Object creator, String label, IRI datatype) {
		super(label, datatype);
		this.creator = creator;
	}

	public MemLiteral(Object creator, String label, IRI datatype, CoreDatatype coreDatatype) {
		super(label, datatype, coreDatatype);
		this.creator = creator;
	}

	public MemLiteral(Object creator, String label, CoreDatatype datatype) {
		super(label, datatype);
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
		return !objectStatements.isEmpty();
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
	public boolean hasSubjectStatements() {
		return false;
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
		return false;
	}
}
