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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.util.Namespaces;
import org.junit.Test;

public class BasicParserSettingsTest {
	@Test
	public void testAdditionalPrefixes() {
		assertTrue("No additional prefixes", Namespaces.DEFAULT_RDF4J.size() > Namespaces.DEFAULT_RDFA11.size());
	}

	@Test
	public void testImmutable() {
		try {
			Namespaces.DEFAULT_RDF4J.add(new SimpleNamespace("ex", "http://www.example.com"));
			fail("Not immutable");
		} catch (UnsupportedOperationException use) {
			// ok
		}
	}
}
