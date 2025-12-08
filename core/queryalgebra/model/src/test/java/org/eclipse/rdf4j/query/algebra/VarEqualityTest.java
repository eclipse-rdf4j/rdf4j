/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

class VarEqualityTest {

	private static final Value VALUE = SimpleValueFactory.getInstance().createLiteral("v");

	static class CustomVar extends Var {
		CustomVar(String name, Value value, boolean anonymous, boolean constant) {
			super(name, value, anonymous, constant);
		}
	}

	@Test
	void equalitySupportsCustomProviderSubclass() {
		Var base = Var.of("x", VALUE, false, false);
		Var subclass = new CustomVar("x", VALUE, false, false);

		assertTrue(base.equals(subclass), "base should equal subclass with same data");
		assertTrue(subclass.equals(base), "subclass should equal base with same data");
		assertEquals(base.hashCode(), subclass.hashCode(), "hashCode must remain compatible");
	}
}
