/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.jsonld;

import java.io.StringWriter;

import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.junit.jupiter.api.Test;

public class JSONLDWriterCompatabilityTest {
	@Test
	void testUseLegacySettings() throws Exception {
		final RDFWriter writer = Rio.createWriter(RDFFormat.JSONLD, new StringWriter(), "http://example.org/")
				.set(BasicWriterSettings.INLINE_BLANK_NODES, true)
				.set(org.eclipse.rdf4j.rio.helpers.JSONLDSettings.OPTIMIZE, true)
				.set(org.eclipse.rdf4j.rio.helpers.JSONLDSettings.USE_NATIVE_TYPES, true)
				.set(org.eclipse.rdf4j.rio.helpers.JSONLDSettings.JSONLD_MODE,
						org.eclipse.rdf4j.rio.helpers.JSONLDMode.COMPACT);
		Rio.write(new LinkedHashModel(), writer);
	}

	@Test
	void testUseMovedSettings() throws Exception {
		final RDFWriter writer = Rio.createWriter(RDFFormat.JSONLD, new StringWriter(), "http://example.org/")
				.set(BasicWriterSettings.INLINE_BLANK_NODES, true)
				.set(JSONLDSettings.OPTIMIZE, true)
				.set(JSONLDSettings.USE_NATIVE_TYPES, true)
				.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.COMPACT);
		Rio.write(new LinkedHashModel(), writer);
	}
}
