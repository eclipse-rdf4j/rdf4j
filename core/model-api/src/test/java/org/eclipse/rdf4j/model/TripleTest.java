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
 * Abstract {@link Triple} test suite.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public abstract class TripleTest {

	/**
	 * Creates a test Triple instance.
	 *
	 * @param subject   the subject of the triple
	 * @param predicate the predicate of the triple
	 * @param object    the object of the triple
	 *
	 * @return a new instance of the concrete triple class under test
	 */
	protected abstract Triple triple(Resource subject, IRI predicate, Value object);

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

		final Triple triple = triple(subject, predicate, object);

		assertThat(triple.getSubject()).isEqualTo(subject);
		assertThat(triple.getPredicate()).isEqualTo(predicate);
		assertThat(triple.getObject()).isEqualTo(object);

		assertThatNullPointerException().isThrownBy(() -> triple(null, predicate, object));
		assertThatNullPointerException().isThrownBy(() -> triple(subject, null, object));
		assertThatNullPointerException().isThrownBy(() -> triple(subject, predicate, null));
	}

	@Test
	public void testEquals() {

		final Resource subject = iri("http://example.org/subject");
		final IRI predicate = iri("http://example.org/predicate");
		final Value object = iri("http://example.org/object");

		final Triple triple = triple(subject, predicate, object);

		assertThat(triple).isEqualTo(triple);
		assertThat(triple).isEqualTo(triple(triple.getSubject(), triple.getPredicate(), triple.getObject()));

		assertThat(triple).isNotEqualTo(null);
		assertThat(triple).isNotEqualTo(new Object());

		final IRI other = iri("http://example.org/other");

		assertThat(triple).isNotEqualTo(triple(other, triple.getPredicate(), triple.getObject()));
		assertThat(triple).isNotEqualTo(triple(triple.getSubject(), other, triple.getObject()));
		assertThat(triple).isNotEqualTo(triple(triple.getSubject(), triple.getPredicate(), other));

	}

	@Test
	public void testHashCode() {

		final Resource subject = iri("http://example.org/subject");
		final IRI predicate = iri("http://example.org/predicate");
		final Value object = iri("http://example.org/object");

		final Triple triple = triple(subject, predicate, object);

		assertThat(triple.hashCode())
				.as("computed according to contract")
				.isEqualTo(Objects.hash(
						triple.getSubject(),
						triple.getPredicate(),
						triple.getObject()
				));
	}
}
