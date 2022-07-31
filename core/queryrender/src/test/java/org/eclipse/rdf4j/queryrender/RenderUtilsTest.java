/*******************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.queryrender;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

public class RenderUtilsTest {
	@Test
	public void toSparqlWithLiteralSerialisesLanguageTag() {
		Value val = SimpleValueFactory.getInstance().createLiteral("test", "en");

		assertEquals("\"\"\"test\"\"\"@en", RenderUtils.toSPARQL(val));
	}
}
