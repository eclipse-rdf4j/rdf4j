/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
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
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.AbstractTriple;

/**
 * A simple default implementation of the {@link Triple} interface.
 *
 * @author Pavel Mihaylov
 * @see SimpleValueFactory
 */
public class SimpleTriple extends AbstractTriple {

	/**
	 * The triple's subject.
	 */
	private final Resource subject;

	/**
	 * The triple's predicate.
	 */
	private final IRI predicate;

	/**
	 * The triple's object.
	 */
	private final Value object;

	/**
	 * Creates a new Triple with the supplied subject, predicate and object.
	 * <p>
	 * Note that creating SimpleStatement objects directly via this constructor is not the recommended approach.
	 * Instead, use an instance of {@link org.eclipse.rdf4j.model.ValueFactory} to create new Triple objects.
	 *
	 * @param subject   The triple's subject, must not be <var>null</var>.
	 * @param predicate The triple's predicate, must not be <var>null</var>.
	 * @param object    The triple's object, must not be <var>null</var>.
	 *
	 * @see SimpleValueFactory#createTriple(Resource, IRI, Value)
	 */
	protected SimpleTriple(Resource subject, IRI predicate, Value object) {
		this.subject = Objects.requireNonNull(subject, "subject must not be null");
		this.predicate = Objects.requireNonNull(predicate, "predicate must not be null");
		this.object = Objects.requireNonNull(object, "object must not be null");
	}

	@Override
	public Resource getSubject() {
		return subject;
	}

	@Override
	public IRI getPredicate() {
		return predicate;
	}

	@Override
	public Value getObject() {
		return object;
	}

	@Override
	public String stringValue() {
		StringBuilder sb = new StringBuilder(256);

		sb.append("<<");
		sb.append(getSubject());
		sb.append(" ");
		sb.append(getPredicate());
		sb.append(" ");
		sb.append(getObject());
		sb.append(">>");

		return sb.toString();
	}

	@Override
	public String toString() {
		return stringValue();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof Triple) {
			Triple that = (Triple) o;
			return Objects.equals(subject, that.getSubject()) && Objects.equals(predicate, that.getPredicate())
					&& Objects.equals(object, that.getObject());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(subject, predicate, object);
	}

}
