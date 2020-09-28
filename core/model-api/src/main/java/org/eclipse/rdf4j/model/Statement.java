/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model;

import java.io.Serializable;

/**
 * An RDF statement, with optional associated context. A statement can have an associated context in specific cases, for
 * example when fetched from a repository.
 * <p>
 * Additional utility functionality for working with {@code Statement} objects is available in the
 * {@code org.eclipse.rdf4j.model.util.Statements} utility class.
 */
public interface Statement extends Serializable {

	/**
	 * Gets the subject of this statement.
	 *
	 * @return The statement's subject.
	 */
	public Resource getSubject();

	/**
	 * Gets the predicate of this statement.
	 *
	 * @return The statement's predicate.
	 */
	public IRI getPredicate();

	/**
	 * Gets the object of this statement.
	 *
	 * @return The statement's object.
	 */
	public Value getObject();

	/**
	 * Gets the context of this statement.
	 *
	 * @return The statement's context, or <tt>null</tt> in case of the null context or if not applicable.
	 */
	public Resource getContext();

	/**
	 * Compares a statement object to another object.
	 *
	 * @param other The object to compare this statement to.
	 * @return <tt>true</tt> if the other object is an instance of {@link Statement} and if their subjects, predicates,
	 * objects and contexts are equal.
	 */
	@Override
	public boolean equals(Object other);

	/**
	 * The hash code of a statement.
	 *
	 * @return A hash code for the statement.
	 */
	@Override
	public int hashCode();
}
