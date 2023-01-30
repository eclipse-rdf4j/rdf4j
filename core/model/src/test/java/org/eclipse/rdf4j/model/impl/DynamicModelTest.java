/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.model.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.junit.jupiter.api.Test;

public class DynamicModelTest {

	@Test
	public void testAddNamespace() {
		SimpleNamespace ns = new SimpleNamespace("ex", "example:namespace");

		Model model = new DynamicModelFactory().createEmptyModel();
		model.setNamespace(ns);

		Model filtered = model.filter(null, null, null);

		assertThat(filtered.getNamespace(ns.getPrefix()).get())
				.isEqualTo(ns);
	}

	@Test
	public void testAddNamespacePrefixAndName() {
		String prefix = "ex";
		String name = "example:namespace";

		Model model = new DynamicModelFactory().createEmptyModel();
		model.setNamespace(prefix, name);

		Model filtered = model.filter(null, null, null);

		assertThat(filtered.getNamespace(prefix).isPresent())
				.isTrue();
		assertThat(filtered.getNamespace(prefix).get().getName())
				.isEqualTo(name);
	}

	@Test
	public void testRemoveNamespace() {
		Namespace ns = new SimpleNamespace("ex", "example:namespace");

		Model model = new DynamicModelFactory().createEmptyModel();
		model.setNamespace(ns);

		Model filtered = model.filter(null, null, null);

		filtered.removeNamespace(ns.getPrefix());

		assertThat(filtered.getNamespace(ns.getPrefix()).isPresent())
				.isFalse();
		assertThat(model.getNamespace(ns.getPrefix()).isPresent())
				.isFalse();
	}

}
