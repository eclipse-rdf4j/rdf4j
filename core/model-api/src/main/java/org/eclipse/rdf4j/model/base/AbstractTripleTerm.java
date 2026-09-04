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

package org.eclipse.rdf4j.model.base;

import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.TripleTerm;
import org.eclipse.rdf4j.model.Value;

/**
 * Base class for {@link TripleTerm}, offering common functionality.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public abstract class AbstractTripleTerm implements TripleTerm {

	private static final long serialVersionUID = 2661609986803671844L;

	@Override
	public String stringValue() {
		return "<<(" + getSubject() + " " + getPredicate() + " " + getObject() + ")>>";
	}

	@Override
	public boolean equals(Object o) {

		// We check object equality first since it's most likely to be different. In general the number of different
		// predicates in sets of statements are the smallest (and therefore most likely to be identical), so these are
		// checked last.

		return this == o || o instanceof TripleTerm
				&& getObject().equals(((TripleTerm) o).getObject())
				&& getSubject().equals(((TripleTerm) o).getSubject())
				&& getPredicate().equals(((TripleTerm) o).getPredicate());
	}

	@Override
	public int hashCode() { // TODO inline Objects.hash() to avoid array creation?
		return Objects.hash(getSubject(), getPredicate(), getObject());
	}

	@Override
	public String toString() {
		return stringValue();
	}

	static class GenericTripleTerm extends AbstractTripleTerm {

		private static final long serialVersionUID = 7822116805598041700L;

		private final Resource subject;
		private final IRI predicate;
		private final Value object;

		GenericTripleTerm(Resource subject, IRI predicate, Value object) {
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
