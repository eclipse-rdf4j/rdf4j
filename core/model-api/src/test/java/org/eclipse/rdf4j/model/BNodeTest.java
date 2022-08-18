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

import org.junit.jupiter.api.Test;

/**
 * Abstract {@link BNode} test suite.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public abstract class BNodeTest {

	/**
	 * Creates a test blank node instance.
	 *
	 * @param id the id of the blank node
	 *
	 * @return a new instance of the concrete blank node class under test
	 */
	protected abstract BNode bnode(String id);

	@Test
	public final void testConstructor() {

		final String id = "id";

		final BNode bnode = bnode(id);

		assertThat(bnode.getID()).isEqualTo(id);

		assertThatNullPointerException().isThrownBy(() -> bnode(null));
	}

	@Test
	public void testStringValue() {

		final String id = "bnode";

		assertThat(bnode(id).getID()).isEqualTo(id);
	}

	@Test
	public void testEquals() {

		final BNode x = bnode("x");
		final BNode y = bnode("y");

		assertThat(x).isEqualTo(x);
		assertThat(x).isEqualTo(bnode(x.getID()));

		assertThat(x).isNotEqualTo(null);
		assertThat(x).isNotEqualTo(new Object());
		assertThat(x).isNotEqualTo(y);

	}

	@Test
	public void testHashCode() {

		final BNode bnode = bnode("bnode");

		assertThat(bnode.hashCode())
				.as("computed according to contract")
				.isEqualTo(bnode.getID().hashCode());
	}

}
