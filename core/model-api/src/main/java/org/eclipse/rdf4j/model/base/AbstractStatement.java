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
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

/**
 * Base class for {@link Statement}, offering common functionality.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public abstract class AbstractStatement implements Statement {

	private static final long serialVersionUID = 2087591563645988076L;

	/**
	 * Creates a new statement.
	 *
	 * @param subject   the subject of the statement
	 * @param predicate the predicate of the statement
	 * @param object    the object of the statement
	 *
	 * @return a new generic statement
	 *
	 * @throws NullPointerException if either {@code subject} or {@code predicate} or {@code object} is {@code null}
	 */
	public static Statement createStatement(Resource subject, IRI predicate, Value object) {

		if (subject == null) {
			throw new NullPointerException("null subject");
		}

		if (predicate == null) {
			throw new NullPointerException("null predicate");
		}

		if (object == null) {
			throw new NullPointerException("null object");
		}

		return new GenericStatement(subject, predicate, object, null);
	}

	/**
	 * Creates a new statement.
	 *
	 * @param subject   the subject of the statement
	 * @param predicate the predicate of the statement
	 * @param object    the object of the statement
	 * @param context   the context of the statement; may be {@code null}
	 *
	 * @return a new generic statement
	 *
	 * @throws NullPointerException if either {@code subject} or {@code predicate} or {@code object} is {@code null}
	 */
	public static Statement createStatement(Resource subject, IRI predicate, Value object, Resource context) {

		if (subject == null) {
			throw new NullPointerException("null subject");
		}

		if (predicate == null) {
			throw new NullPointerException("null predicate");
		}

		if (object == null) {
			throw new NullPointerException("null object");
		}

		return new GenericStatement(subject, predicate, object, context);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public boolean equals(Object o) {

		// We check object equality first since it's most likely to be different. In general the number of different
		// predicates and contexts in sets of statements are the smallest (and therefore most likely to be identical),
		// so these are checked last.

		return this == o || o instanceof Statement
				&& Objects.equals(getObject(), ((Statement) o).getObject())
				&& Objects.equals(getSubject(), ((Statement) o).getSubject())
				&& Objects.equals(getPredicate(), ((Statement) o).getPredicate())
				&& Objects.equals(getContext(), ((Statement) o).getContext());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getSubject(), getPredicate(), getObject(), getContext());
	}

	@Override
	public String toString() {
		return "("
				+ getSubject()
				+ ", " + getPredicate()
				+ ", " + getObject()
				+ (getContext() == null ? "" : ", " + getContext())
				+ ")";
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static class GenericStatement extends AbstractStatement {

		private static final long serialVersionUID = -4116676621136121342L;

		private final Resource subject;
		private final IRI predicate;
		private final Value object;
		private final Resource context;

		GenericStatement(Resource subject, IRI predicate, Value object, Resource context) {
			this.subject = subject;
			this.predicate = predicate;
			this.object = object;
			this.context = context;
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
		public Resource getContext() {
			return context;
		}

	}

}
