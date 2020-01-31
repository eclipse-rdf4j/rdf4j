/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

/**
 * A simple default implementation of the {@link Statement} interface for statements that don't have an associated
 * context. For statements that do have an associated context, {@link ContextStatement} can be used.
 * 
 * @see {@link SimpleValueFactory}
 */
public class SimpleStatement implements Statement {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = 8707542157460228077L;

	/**
	 * The statement's subject.
	 */
	private final Resource subject;

	/**
	 * The statement's predicate.
	 */
	private final IRI predicate;

	/**
	 * The statement's object.
	 */
	private final Value object;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new Statement with the supplied subject, predicate and object. *
	 * <p>
	 * Note that creating SimpleStatement objects directly via this constructor is not the recommended approach.
	 * Instead, use a {@link org.eclipse.rdf4j.model.ValueFactory ValueFactory} (obtained from your repository or by
	 * using {@link SimpleValueFactory#getInstance()}) to create new Statement objects.
	 * 
	 * @param subject   The statement's subject, must not be <tt>null</tt>.
	 * @param predicate The statement's predicate, must not be <tt>null</tt>.
	 * @param object    The statement's object, must not be <tt>null</tt>.
	 * @see {@link SimpleValueFactory#createStatement(Resource, IRI, Value)
	 */
	protected SimpleStatement(Resource subject, IRI predicate, Value object) {
		this.subject = Objects.requireNonNull(subject, "subject must not be null");
		this.predicate = Objects.requireNonNull(predicate, "predicate must not be null");
		this.object = Objects.requireNonNull(object, "object must not be null");
	}

	/*---------*
	 * Methods *
	 *---------*/

	// Implements Statement.getSubject()
	@Override
	public Resource getSubject() {
		return subject;
	}

	// Implements Statement.getPredicate()
	@Override
	public IRI getPredicate() {
		return predicate;
	}

	// Implements Statement.getObject()
	@Override
	public Value getObject() {
		return object;
	}

	// Implements Statement.getContext()
	@Override
	public Resource getContext() {
		return null;
	}

	// Overrides Object.equals(Object), implements Statement.equals(Object)
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		if (other instanceof Statement) {
			Statement that = (Statement) other;

			/*
			 * We check object equality first since it's most likely to be different. In general the number of different
			 * predicates and contexts in sets of statements are the smallest (and therefore most likely to be
			 * identical), so these are checked last.
			 */
			return object.equals(that.getObject()) && subject.equals(that.getSubject())
					&& predicate.equals(that.getPredicate()) && Objects.equals(getContext(), that.getContext());
		}

		return false;
	}

	// Overrides Object.hashCode(), implements Statement.hashCode()
	@Override
	public int hashCode() {
		return Objects.hash(subject, predicate, object, getContext());
	}

	/**
	 * Gives a String-representation of this Statement that can be used for debugging.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(256);

		sb.append("(");
		sb.append(getSubject());
		sb.append(", ");
		sb.append(getPredicate());
		sb.append(", ");
		sb.append(getObject());
		sb.append(")");

		return sb.toString();
	}
}
