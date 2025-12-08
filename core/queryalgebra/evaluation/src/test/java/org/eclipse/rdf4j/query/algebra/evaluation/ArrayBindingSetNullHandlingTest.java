/**
 * ******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * ******************************************************************************
 */
package org.eclipse.rdf4j.query.algebra.evaluation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.query.Binding;
import org.junit.jupiter.api.Test;

/**
 * Reproduces a NullPointerException when an ArrayBindingSet contains an explicit null (UNDEF) binding value and is
 * copied into a QueryBindingSet. Prior to the fix, iterating the ArrayBindingSet could yield a null Binding, which
 * caused NPE in QueryBindingSet.addBinding.
 */
public class ArrayBindingSetNullHandlingTest {

	@Test
	public void iteratorShouldNotReturnNullBindings() {
		ArrayBindingSet bs = new ArrayBindingSet("myVar", "unbound", "mappingProp", "const");
		// Explicitly set an UNDEF/null binding using the direct setter (stores a sentinel NULL_VALUE)
		bs.getDirectSetBinding("myVar").accept(null, bs);
		// Add a real binding so iteration has at least one valid element
		bs.getDirectSetBinding("mappingProp").accept(OWL.EQUIVALENTCLASS, bs);

		for (Binding b : bs) {
			assertNotNull(b, "iterator must not yield null Binding elements");
		}
	}

	@Test
	public void copyingToQueryBindingSetMustSkipUndefBindings() {
		ArrayBindingSet bs = new ArrayBindingSet("myVar", "unbound", "mappingProp", "const");
		// myVar is explicitly present with UNDEF value
		bs.getDirectSetBinding("myVar").accept(null, bs);
		// mappingProp has a concrete value
		bs.getDirectSetBinding("mappingProp").accept(OWL.EQUIVALENTCLASS, bs);

		// Creating a QueryBindingSet from the ArrayBindingSet should not throw
		QueryBindingSet qbs = assertDoesNotThrow(() -> new QueryBindingSet(bs));

		assertTrue(qbs.hasBinding("mappingProp"));
		assertEquals(OWL.EQUIVALENTCLASS, qbs.getValue("mappingProp"));
		// UNDEF binding must not appear in the resulting set
		assertFalse(qbs.hasBinding("myVar"));
		assertEquals(1, qbs.size());
	}

}
