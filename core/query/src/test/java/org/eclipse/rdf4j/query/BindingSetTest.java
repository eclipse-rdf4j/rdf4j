/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for BindingSet implementations.
 *
 * @author jeen
 */
public abstract class BindingSetTest<T extends BindingSet> {

	/**
	 * Verifies that the BindingSet implementation honors the API spec for {@link BindingSet#equals(Object)} and
	 * {@link BindingSet#hashCode()}.
	 */
	@Test
	public void testEqualsHashcode() {

		T[] bindingSets = createTwoEqualReorderedBindingSets();

		T bs1 = bindingSets[0];
		T bs2 = bindingSets[1];

		assertEquals(bs1, bs2);

		assertEquals(bs1.hashCode(), bs2.hashCode());
	}

	/**
	 * Creates two equal, but differently ordered, BindingSet objects.
	 *
	 * @return an array of two equal but differently ordered BindingSets.
	 */
	protected abstract T[] createTwoEqualReorderedBindingSets();
}
