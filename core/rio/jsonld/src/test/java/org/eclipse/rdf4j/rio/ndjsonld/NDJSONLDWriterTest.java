/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.ndjsonld;

import java.io.IOException;

import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriterTest;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.junit.Ignore;
import org.junit.Test;

public class NDJSONLDWriterTest extends RDFWriterTest {

	public NDJSONLDWriterTest() {
		super(new NDJSONLDWriterFactory(), new NDJSONLDParserFactory());
	}

	@Test
	@Override
	@Ignore("TODO: See case for JSONLDWriterTest")
	public void testIllegalPrefix() throws RDFHandlerException, RDFParseException, IOException {
	}

	@Override
	protected void setupWriterConfig(WriterConfig config) {
		super.setupWriterConfig(config);
		config.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.COMPACT);
	}

	@Override
	protected void setupParserConfig(ParserConfig config) {
		super.setupParserConfig(config);
		config.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);
		config.set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);
	}

	@Override
	protected RioSetting<?>[] getExpectedSupportedSettings() {
		return new RioSetting[] {
				BasicWriterSettings.BASE_DIRECTIVE,
				JSONLDSettings.COMPACT_ARRAYS,
				JSONLDSettings.HIERARCHICAL_VIEW,
				JSONLDSettings.JSONLD_MODE,
				JSONLDSettings.PRODUCE_GENERALIZED_RDF,
				JSONLDSettings.USE_RDF_TYPE,
				JSONLDSettings.USE_NATIVE_TYPES
		};
	}
}
