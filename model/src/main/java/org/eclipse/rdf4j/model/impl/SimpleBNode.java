/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import org.eclipse.rdf4j.model.BNode;

/**
 * An simple default implementation of the {@link BNode} interface.
 * 
 * @author Arjohn Kampman
 */
public class SimpleBNode implements BNode {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = 5273570771022125970L;

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The blank node's identifier.
	 */
	private String id;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new, unitialized blank node. This blank node's ID needs to be {@link #setID(String) set} before the
	 * normal methods can be used.
	 */
	protected SimpleBNode() {
	}

	/**
	 * Creates a new blank node with the supplied identifier.
	 * 
	 * @param id The identifier for this blank node, must not be <tt>null</tt>.
	 */
	protected SimpleBNode(String id) {
		this();
		setID(id);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public String getID() {
		return id;
	}

	protected void setID(String id) {
		this.id = id;
	}

	@Override
	public String stringValue() {
		return id;
	}

	// Overrides Object.equals(Object), implements BNode.equals(Object)
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof BNode) {
			BNode otherNode = (BNode) o;
			return this.getID().equals(otherNode.getID());
		}

		return false;
	}

	// Overrides Object.hashCode(), implements BNode.hashCode()
	@Override
	public int hashCode() {
		return id.hashCode();
	}

	// Overrides Object.toString()
	@Override
	public String toString() {
		return "_:" + id;
	}
}
