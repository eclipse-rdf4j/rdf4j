/*****************************************************************************
 * Copyright (c) ${year} Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class BasicNamespaceTest {

	@Test
	public void compareTo() {

		final Namespace x = new BasicNamespace("com", "http://example.org/x");
		final Namespace y = new BasicNamespace("org", "http://example.org/y");
		final Namespace z = new BasicNamespace("org", "http://example.org/z");

		assertThat(x).isEqualByComparingTo(x);

		assertThat(x).as("nulls first").isGreaterThan(null);

		assertThat(x).as("less by prefix").isLessThan(y);
		assertThat(y).as("greater by prefix").isGreaterThan(x);

		assertThat(y).as("less by name").isLessThan(z);
		assertThat(z).as("greater by name").isGreaterThan(y);

	}

	@Test
	public void testEquals() {

		final Namespace x = new BasicNamespace("com", "http://example.org/x");
		final Namespace y = new BasicNamespace("org", "http://example.org/y");

		assertThat(x).as("same class").isEqualTo(x);

		assertThat(x).as("different class").isEqualTo(new Namespace() {

			@Override
			public String getPrefix() {
				return x.getPrefix();
			}

			@Override
			public String getName() {
				return x.getName();
			}

		});

		assertThat(x).isNotEqualTo(null);
		assertThat(x).isNotEqualTo(new Object());
		assertThat(x).isNotEqualTo(y);
	}

}