/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
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

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

/**
 * An implementation of the Statement interface with support for Java Generics.
 *
 * @implNote This class is marked as experimental because there may still be changes to the class.
 */
@Experimental
public class GenericStatement<R extends Resource, I extends IRI, V extends Value> implements Statement {

	private static final long serialVersionUID = -4747234187477906748L;

	// Fields for storing the values of the Statement. The fields subject, predicate and object may not be null, context
	// may be null. All are protected, because classes that extend this class should have direct access to the fields.
	protected final R subject;
	protected final I predicate;
	protected final V object;
	protected final R context;

	/**
	 * Creates a new Statement with the supplied subject, predicate and object for the specified associated context.
	 * <p>
	 * Note that creating an objects directly via this constructor is not the recommended approach. Instead, use a
	 * {@link org.eclipse.rdf4j.model.ValueFactory ValueFactory} (obtained from your repository or by using
	 * {@link org.eclipse.rdf4j.model.impl.SimpleValueFactory#getInstance()}) to create new Statement objects.
	 *
	 * @param subject   The statement's subject, must not be <var>null</var>.
	 * @param predicate The statement's predicate, must not be <var>null</var>.
	 * @param object    The statement's object, must not be <var>null</var>.
	 * @param context   The statement's context, <var>null</var> to indicate no context is associated.
	 */
	protected GenericStatement(R subject, I predicate, V object, R context) {
		this.subject = Objects.requireNonNull(subject, "subject must not be null");
		this.predicate = Objects.requireNonNull(predicate, "predicate must not be null");
		this.object = Objects.requireNonNull(object, "object must not be null");
		this.context = context;
	}

	@Override
	public R getSubject() {
		return subject;
	}

	@Override
	public I getPredicate() {
		return predicate;
	}

	@Override
	public V getObject() {
		return object;
	}

	@Override
	public R getContext() {
		return context;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof Statement)) {
			return false;
		}

		Statement that = (Statement) o;

		return subject.equals(that.getSubject()) &&
				predicate.equals(that.getPredicate()) &&
				object.equals(that.getObject()) &&
				Objects.equals(context, that.getContext());
	}

	@Override
	public int hashCode() {
		// Inlined Objects.hash(subject, predicate, object,context) to avoid array creation
		int result = 1;
		result = 31 * result + subject.hashCode();
		result = 31 * result + predicate.hashCode();
		result = 31 * result + object.hashCode();
		result = 31 * result + (context == null ? 0 : context.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "("
				+ subject
				+ ", " + predicate
				+ ", " + object
				+ ") [" + context + "]";
	}
}
