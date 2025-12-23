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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

class VarProviderCloneHookTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	void clonePreservesProviderSpecificState() {
		Var var = Var.of("x", vf.createLiteral("v"), false, false);
		KindAwareVar kinded = assertInstanceOf(KindAwareVar.class, var);

		kinded.setKind("special");

		Var cloned = kinded.clone();
		KindAwareVar clonedKinded = assertInstanceOf(KindAwareVar.class, cloned);

		assertEquals("special", clonedKinded.getKind(), "clone must retain provider-managed state");
		assertEquals(kinded, cloned, "clone should be equal to original when provider state matches");
		assertEquals(kinded.hashCode(), cloned.hashCode(), "hash codes should match when provider state matches");

		Set<Var> vars = new HashSet<>();
		vars.add(kinded);
		vars.add(cloned);
		assertEquals(1, vars.size(), "HashSet should treat clone as duplicate");

		Map<Var, String> map = new HashMap<>();
		map.put(kinded, "payload");
		assertEquals("payload", map.get(cloned), "Map lookup via clone should succeed");
	}

	@Test
	void defaultBehaviorStillUsesNameValueAndFlags() {
		Var first = Var.of("y", vf.createLiteral("v"), false, false);
		Var second = Var.of("y", vf.createLiteral("v"), false, false);

		assertEquals(first, second, "default provider behavior should remain compatible");
		assertEquals(first.hashCode(), second.hashCode(), "hashCode compatibility must remain intact");

		Var cloned = first.clone();
		assertTrue(first.equals(cloned) && cloned.equals(first), "clones must remain equal under default state");
	}
}
