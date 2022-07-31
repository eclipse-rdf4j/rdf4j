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
package org.eclipse.rdf4j.model.impl;

import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.AbstractStatement;

/**
 * A simple default implementation of the {@link Statement} interface for statements that don't have an associated
 * context. For statements that do have an associated context, {@link ContextStatement} can be used.
 *
 * @see org.eclipse.rdf4j.model.impl.SimpleValueFactory
 * @deprecated Use {@link GenericStatement instead}
 */
@Deprecated(since = "4.1.0", forRemoval = true)
public class SimpleStatement extends AbstractStatement {

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
	 * using {@link org.eclipse.rdf4j.model.impl.SimpleValueFactory#getInstance()}) to create new Statement objects.
	 *
	 * @param subject   The statement's subject, must not be <var>null</var>.
	 * @param predicate The statement's predicate, must not be <var>null</var>.
	 * @param object    The statement's object, must not be <var>null</var>.
	 * @see org.eclipse.rdf4j.model.impl.SimpleValueFactory#createStatement(Resource, IRI, Value)
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

	public boolean exactSameSubject(Resource subject) {
		return subject == this.subject;
	}

	public boolean exactSamePredicate(IRI predicate) {
		return predicate == this.predicate;
	}

	public boolean exactSameObject(Value object) {
		return object == this.object;
	}

	// Implements Statement.getContext()
	@Override
	public Resource getContext() {
		return null;
	}

}
