/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model.base;

import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;

/**
 * Base class for {@link Triple}, offering common functionality.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public abstract class AbstractTriple implements Triple {

	private static final long serialVersionUID = 2661609986803671844L;

	/**
	 * Creates a new triple value.
	 *
	 * @param subject   the subject of the triple
	 * @param predicate the predicate of the triple
	 * @param object    the object of the triple
	 *
	 * @return a new generic triple value
	 *
	 * @throws NullPointerException if either {@code subject} or {@code predicate} or {@code object} is {@code null}
	 */
	public static Triple createTriple(Resource subject, IRI predicate, Value object) {

		if (subject == null) {
			throw new NullPointerException("null subject");
		}

		if (predicate == null) {
			throw new NullPointerException("null predicate");
		}

		if (object == null) {
			throw new NullPointerException("null object");
		}

		return new GenericTriple(subject, predicate, object);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public String stringValue() {
		return "<<" + getSubject() + " " + getPredicate() + " " + getObject() + ">>";
	}

	@Override
	public boolean equals(Object o) {

		// We check object equality first since it's most likely to be different. In general the number of different
		// predicates and contexts in sets of statements are the smallest (and therefore most likely to be identical),
		// so these are checked last.

		return this == o || o instanceof Triple
				&& Objects.equals(getObject(), ((Triple) o).getObject())
				&& Objects.equals(getSubject(), ((Triple) o).getSubject())
				&& Objects.equals(getPredicate(), ((Triple) o).getPredicate());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getSubject(), getPredicate(), getObject());
	}

	@Override
	public String toString() {
		return stringValue();
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static class GenericTriple extends AbstractTriple {

		private static final long serialVersionUID = 7822116805598041700L;

		private final Resource subject;
		private final IRI predicate;
		private final Value object;

		GenericTriple(Resource subject, IRI predicate, Value object) {
			this.subject = subject;
			this.predicate = predicate;
			this.object = object;
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
	}

}
