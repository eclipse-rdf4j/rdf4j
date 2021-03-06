/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model;

import static org.eclipse.rdf4j.model.Path.format;

import java.util.List;
import java.util.Set;

/**
 * Default variable length property path implementations.
 *
 * @author Alessandro Bollini
 * @since 3.7.0
 */
abstract class PathDefault<T> implements Path {

	private final T value;

	private final boolean inverse;
	private final boolean optional;
	private final boolean repeatable;

	PathDefault(T value, boolean inverse, boolean optional, boolean repeatable) {

		this.value = value;

		this.inverse = inverse;
		this.optional = optional;
		this.repeatable = repeatable;
	}

	protected T getValue() {
		return value;
	}

	@Override
	public boolean isInverse() {
		return inverse;
	}

	@Override
	public boolean isOptional() {
		return optional;
	}

	@Override
	public boolean isRepeatable() {
		return repeatable;
	}

	@Override
	public boolean equals(Object object) {
		return this == object || getClass().isInstance(object)
				&& value.equals(((PathDefault<?>) object).value)
				&& inverse == ((PathDefault<?>) object).inverse
				&& optional == ((PathDefault<?>) object).optional
				&& repeatable == ((PathDefault<?>) object).repeatable;
	}

	@Override
	public int hashCode() {
		return value.hashCode()
				^ Boolean.hashCode(inverse)
				^ Boolean.hashCode(optional)
				^ Boolean.hashCode(repeatable);
	}

	@Override
	public String toString() {
		return format(this);
	}

	static final class Hop extends PathDefault<IRI> implements Path.Hop {

		Hop(IRI value, boolean inverse, boolean optional, boolean repeatable) {

			super(value, inverse, optional, repeatable);

			if (!inverse && !optional && !repeatable) {
				throw new AssertionError("plain direct predicate paths should be represented with an IRI");
			}

		}

		@Override
		public IRI getIRI() {
			return getValue();
		}

		@Override
		public <V> V accept(Visitor<V> visitor) {

			if (visitor == null) {
				throw new NullPointerException("null visitor");
			}

			return visitor.visit(this);
		}

	}

	static final class Seq extends PathDefault<List<Path>> implements Path.Seq {

		Seq(List<Path> value, boolean inverse, boolean optional, boolean repeatable) {
			super(value, inverse, optional, repeatable);
		}

		@Override
		public List<Path> getPaths() {
			return getValue();
		}

		@Override
		public <V> V accept(Visitor<V> visitor) {

			if (visitor == null) {
				throw new NullPointerException("null visitor");
			}

			return visitor.visit(this);
		}

	}

	static final class Alt extends PathDefault<Set<Path>> implements Path.Alt {

		Alt(Set<Path> value, boolean inverse, boolean optional, boolean repeatable) {
			super(value, inverse, optional, repeatable);
		}

		@Override
		public Set<Path> getPaths() {
			return getValue();
		}

		@Override
		public <V> V accept(Visitor<V> visitor) {

			if (visitor == null) {
				throw new NullPointerException("null visitor");
			}

			return visitor.visit(this);
		}

	}

	static final class Not extends PathDefault<Set<Step>> implements Path.Not {

		Not(Set<Step> value, boolean inverse, boolean optional, boolean repeatable) {
			super(value, inverse, optional, repeatable);
		}

		@Override
		public Set<Step> getSteps() {
			return getValue();
		}

		@Override
		public <V> V accept(Visitor<V> visitor) {

			if (visitor == null) {
				throw new NullPointerException("null visitor");
			}

			return visitor.visit(this);
		}

	}

}
