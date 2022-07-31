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
package org.eclipse.rdf4j.model;

/**
 * An RDF-1.1 blank node (aka <em>bnode</em>, aka <em>anonymous node</em>). A blank node has an identifier to be able to
 * compare it to other blank nodes internally. Please note that, conceptually, blank node equality can only be
 * determined by examining the statements that refer to them.
 *
 * @see <a href="http://www.w3.org/TR/rdf11-concepts/#section-blank-nodes">RDF-1.1 Concepts and Abstract Syntax</a>
 *
 * @implNote In order to ensure interoperability of concrete classes implementing this interface,
 *           {@link #equals(Object)} and {@link #hashCode()} methods must be implemented exactly as described in their
 *           specs.
 */
public interface BNode extends Resource {

	@Override
	default boolean isBNode() {
		return true;
	}

	/**
	 * Retrieves this blank node's identifier.
	 *
	 * @return A blank node identifier.
	 */
	String getID();

	/**
	 * Compares this blank node to another object.
	 *
	 * @param o the object to compare this blank node to
	 *
	 * @return {@code true}, if the other object is an instance of {@code BNode} and their {@linkplain #getID() IDs} are
	 *         equal; {@code false}, otherwise.
	 */
	@Override
	boolean equals(Object o);

	/**
	 * Computes the hash code of this blank node.
	 *
	 * @return a hash code for this blank node computed as {@link #getID()}{@code .hashCode()}
	 */
	@Override
	int hashCode();

}
