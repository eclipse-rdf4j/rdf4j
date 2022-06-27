/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;

import org.junit.Test;

/**
 * Abstract {@link Namespace} test suite.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public abstract class NamespaceTest {

	/**
	 * Creates a test namespace instance.
	 *
	 * @param prefix the prefix of the namespace
	 * @param name   the name IRI of the namespace
	 *
	 * @return a new instance of the concrete namespace class under test
	 */
	protected abstract Namespace namespace(String prefix, String name);

	@Test
	public void compareTo() {

		final Namespace x = namespace("com", "http://example.org/x");
		final Namespace y = namespace("org", "http://example.org/y");
		final Namespace z = namespace("org", "http://example.org/z");

		assertThat(x).isEqualByComparingTo(x);

		assertThat(x).as("nulls first").isGreaterThan(null);

		assertThat(x).as("less by prefix").isLessThan(y);
		assertThat(y).as("greater by prefix").isGreaterThan(x);

		assertThat(y).as("less by name").isLessThan(z);
		assertThat(z).as("greater by name").isGreaterThan(y);

	}

	@Test
	public void testEquals() {

		final Namespace x = namespace("com", "http://example.org/x");
		final Namespace y = namespace("org", "http://example.org/y");

		assertThat(x).as("same object").isEqualTo(x);
		assertThat(x).as("same class").isEqualTo(namespace(x.getPrefix(), x.getName()));

		assertThat(x).isNotEqualTo(null);
		assertThat(x).isNotEqualTo(new Object());
		assertThat(x).isNotEqualTo(y);
	}

	@Test
	public void testHashCode() {

		final Namespace namespace = namespace("com", "http://example.org/x");

		assertThat(namespace.hashCode())
				.as("computed according to contract")
				.isEqualTo(Objects.hash(namespace.getPrefix(), namespace.getName()));
	}

}
