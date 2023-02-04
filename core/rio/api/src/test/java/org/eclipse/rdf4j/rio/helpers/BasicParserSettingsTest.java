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
package org.eclipse.rdf4j.rio.helpers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.util.Namespaces;
import org.junit.jupiter.api.Test;

public class BasicParserSettingsTest {

	@Test
	public void testAdditionalPrefixes() {
		assertTrue(Namespaces.DEFAULT_RDF4J.size() > Namespaces.DEFAULT_RDFA11.size(), "No additional prefixes");
	}

	@Test
	public void testImmutable() {
		assertThatThrownBy(() -> Namespaces.DEFAULT_RDF4J.add(new SimpleNamespace("ex", "http://www.example.com")))
				.isInstanceOf(UnsupportedOperationException.class)
				.withFailMessage("Not immutable");
	}
}
