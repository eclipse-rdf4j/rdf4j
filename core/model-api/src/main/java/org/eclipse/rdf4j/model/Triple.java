/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model;

import org.eclipse.rdf4j.common.annotation.Experimental;

/**
 * An RDF* embedded triple. Embedded triples have a subject, predicate and object. Unlike {@link Statement}, a triple
 * never has an associated context.
 * <p>
 * Additional utility functionality for working with {@code Triple} objects is available in the
 * {@code org.eclipse.rdf4j.model.util.Statements} utility class.
 *
 * @author Pavel Mihaylov
 *
 * @implNote In order to ensure interoperability of concrete classes implementing this interface,
 *           {@link #equals(Object)} and {@link #hashCode()} methods must be implemented exactly as described in their
 *           specs.
 */
@Experimental
public interface Triple extends Resource {

	@Override
	default boolean isTriple() {
		return true;
	}

	/**
	 * Gets the subject of this triple.
	 *
	 * @return The triple's subject.
	 */
	Resource getSubject();

	/**
	 * Gets the predicate of this triple.
	 *
	 * @return The triple's predicate.
	 */
	IRI getPredicate();

	/**
	 * Gets the object of this triple.
	 *
	 * @return The triple's object.
	 */
	Value getObject();

	/**
	 * Compares this triple to another object.
	 *
	 * @param other the object to compare this triple to
	 *
	 * @return {@code true} if the {@code other} object is an instance of {@code Triple} and if their
	 *         {@linkplain #getSubject() subjects}, {@linkplain #getPredicate() predicates} and {@linkplain #getObject()
	 *         objects} are equal; {@code false} otherwise
	 */
	@Override
	public boolean equals(Object other);

	/**
	 * Computes the hash code of this triple.
	 *
	 * @return a hash code for this triple computed as {@link java.util.Objects#hash Objects.hash}(
	 *         {@link #getSubject()}, {@link #getPredicate()}, {@link #getObject()})
	 */
	@Override
	public int hashCode();

}
