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

package org.eclipse.rdf4j.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Objects;

import org.junit.Test;

/**
 * Abstract {@link Statement} test suite.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public abstract class StatementTest {

	/**
	 * Creates a test Statement instance.
	 *
	 * @param subject   the subject of the statement
	 * @param predicate the predicate of the statement
	 * @param object    the predicate of the statement
	 * @param context   the context of the statement; possibly {@code null}
	 *
	 * @return a new instance of the concrete statement class under test
	 */
	protected abstract Statement statement(Resource subject, IRI predicate, Value object, Resource context);

	/**
	 * Creates a test IRI instance.
	 *
	 * @param iri the IRI of the datatype
	 *
	 * @return a new instance of the concrete IRI class under test
	 */
	protected abstract IRI iri(String iri);

	@Test
	public final void testConstructor() {

		final Resource subject = iri("http://example.org/subject");
		final IRI predicate = iri("http://example.org/predicate");
		final Value object = iri("http://example.org/object");
		final Resource context = iri("http://example.org/context");

		final Statement statement = statement(subject, predicate, object, context);

		assertThat(statement.getSubject()).isEqualTo(subject);
		assertThat(statement.getPredicate()).isEqualTo(predicate);
		assertThat(statement.getObject()).isEqualTo(object);
		assertThat(statement.getContext()).isEqualTo(context);

		assertThatNullPointerException().isThrownBy(() -> statement(null, predicate, object, context));
		assertThatNullPointerException().isThrownBy(() -> statement(subject, null, object, context));
		assertThatNullPointerException().isThrownBy(() -> statement(subject, predicate, null, context));

		assertThat(statement(subject, predicate, object, null)).as("accept null context");

	}

	@Test
	public void testEquals() {

		final Resource subject = iri("http://example.org/subject");
		final IRI predicate = iri("http://example.org/predicate");
		final Value object = iri("http://example.org/object");
		final Resource context = iri("http://example.org/context");

		final Statement statement = statement(subject, predicate, object, context);

		assertThat(statement).isEqualTo(statement);
		assertThat(statement).isEqualTo(statement(
				statement.getSubject(), statement.getPredicate(), statement.getObject(), statement.getContext()
		));

		assertThat(statement).isNotEqualTo(null);
		assertThat(statement).isNotEqualTo(new Object());

		final IRI other = iri("http://example.org/other");

		assertThat(statement).isNotEqualTo(statement(
				other, statement.getPredicate(), statement.getObject(), statement.getContext()
		));

		assertThat(statement).isNotEqualTo(statement(
				statement.getSubject(), other, statement.getObject(), statement.getContext()
		));

		assertThat(statement).isNotEqualTo(statement(
				statement.getSubject(), statement.getPredicate(), other, statement.getContext()
		));
		assertThat(statement).isNotEqualTo(statement(
				statement.getSubject(), statement.getPredicate(), statement.getObject(), other
		));

	}

	@Test
	public void testHashCode() {

		final Resource subject = iri("http://example.org/subject");
		final IRI predicate = iri("http://example.org/predicate");
		final Value object = iri("http://example.org/object");
		final Resource context = iri("http://example.org/context");

		final Statement statement = statement(subject, predicate, object, context);

		assertThat(statement.hashCode())
				.as("computed according to contract")
				.isEqualTo(Objects.hash(
						statement.getSubject(),
						statement.getPredicate(),
						statement.getObject(),
						statement.getContext()
				));
	}

}
