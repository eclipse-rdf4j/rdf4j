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

import java.io.Serializable;

/**
 * An RDF statement, with optional associated context. A statement can have an associated context in specific cases, for
 * example when fetched from a repository.
 * <p>
 * Additional utility functionality for working with {@code Statement} objects is available in the
 * {@code org.eclipse.rdf4j.model.util.Statements} utility class.
 *
 * @implNote In order to ensure interoperability of concrete classes implementing this interface,
 *           {@link #equals(Object)} and {@link #hashCode()} methods must be implemented exactly as described in their
 *           specs.
 */
public interface Statement extends Serializable {

	/**
	 * Gets the subject of this statement.
	 *
	 * @return The statement's subject.
	 */
	Resource getSubject();

	/**
	 * Gets the predicate of this statement.
	 *
	 * @return The statement's predicate.
	 */
	IRI getPredicate();

	/**
	 * Gets the object of this statement.
	 *
	 * @return The statement's object.
	 */
	Value getObject();

	/**
	 * Gets the context of this statement.
	 *
	 * @return The statement's context, or <var>null</var> in case of the null context or if not applicable.
	 */
	Resource getContext();

	/**
	 * Compares this statement to another object.
	 *
	 * @param other the object to compare this statement to
	 *
	 * @return {@code true} if the other object is an instance of {@code Statement} and if their
	 *         {@linkplain #getSubject() subjects}, {@linkplain #getPredicate() predicates}, {@linkplain #getObject()
	 *         objects} and {@linkplain #getContext() contexts} are equal; {@code false} otherwise
	 */
	@Override
	boolean equals(Object other);

	/**
	 * Computes the hash code of this statement.
	 *
	 * @return a hash code for this statement computed as {@link java.util.Objects#hash Objects.hash}(
	 *         {@link #getSubject()}, {@link #getPredicate()}, {@link #getObject()}, {@link #getContext()})
	 */
	@Override
	int hashCode();

}
