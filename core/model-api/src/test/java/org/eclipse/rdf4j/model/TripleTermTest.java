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

import org.junit.jupiter.api.Test;

/**
 * Abstract {@link Triple} test suite.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public abstract class TripleTermTest {

	/**
	 * Creates a test TripleTerm instance.
	 *
	 * @param subject   the subject of the triple
	 * @param predicate the predicate of the triple
	 * @param object    the object of the triple
	 * @return a new instance of the concrete triple class under test
	 */
	protected abstract TripleTerm triple(Resource subject, IRI predicate, Value object);

	/**
	 * Creates a test IRI instance.
	 *
	 * @param iri the IRI of the datatype
	 * @return a new instance of the concrete IRI class under test
	 */
	protected abstract IRI iri(String iri);

	@Test
	public final void testConstructor() {

		final Resource subject = iri("http://example.org/subject");
		final IRI predicate = iri("http://example.org/predicate");
		final Value object = iri("http://example.org/object");

		final TripleTerm tripleTerm = triple(subject, predicate, object);

		assertThat(tripleTerm.getSubject()).isEqualTo(subject);
		assertThat(tripleTerm.getPredicate()).isEqualTo(predicate);
		assertThat(tripleTerm.getObject()).isEqualTo(object);

		assertThatNullPointerException().isThrownBy(() -> triple(null, predicate, object));
		assertThatNullPointerException().isThrownBy(() -> triple(subject, null, object));
		assertThatNullPointerException().isThrownBy(() -> triple(subject, predicate, null));
	}

	@Test
	public void testEquals() {

		final Resource subject = iri("http://example.org/subject");
		final IRI predicate = iri("http://example.org/predicate");
		final Value object = iri("http://example.org/object");

		final TripleTerm tripleTerm = triple(subject, predicate, object);

		assertThat(tripleTerm).isEqualTo(tripleTerm);
		assertThat(tripleTerm)
				.isEqualTo(triple(tripleTerm.getSubject(), tripleTerm.getPredicate(), tripleTerm.getObject()));

		assertThat(tripleTerm).isNotEqualTo(null);
		assertThat(tripleTerm).isNotEqualTo(new Object());

		final IRI other = iri("http://example.org/other");

		assertThat(tripleTerm).isNotEqualTo(triple(other, tripleTerm.getPredicate(), tripleTerm.getObject()));
		assertThat(tripleTerm).isNotEqualTo(triple(tripleTerm.getSubject(), other, tripleTerm.getObject()));
		assertThat(tripleTerm).isNotEqualTo(triple(tripleTerm.getSubject(), tripleTerm.getPredicate(), other));

	}

	@Test
	public void testHashCode() {

		final Resource subject = iri("http://example.org/subject");
		final IRI predicate = iri("http://example.org/predicate");
		final Value object = iri("http://example.org/object");

		final TripleTerm tripleTerm = triple(subject, predicate, object);

		assertThat(tripleTerm.hashCode())
				.as("computed according to contract")
				.isEqualTo(Objects.hash(
						tripleTerm.getSubject(),
						tripleTerm.getPredicate(),
						tripleTerm.getObject()
				));
	}
}
