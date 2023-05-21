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
package org.eclipse.rdf4j.rio.rdfjson;

import java.io.InputStream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriterTest;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.RDFJSONWriterSettings;

/**
 * JUnit test for the RDF/JSON streaming writer.
 *
 * @author Tomas Kovachev t.kovachev1996@gmail.com
 */
public class RDFJSONStreamingWriterTest extends RDFWriterTest {

	public RDFJSONStreamingWriterTest() {
		super(new RDFJSONWriterFactory(), new RDFJSONParserFactory());
	}

	@Override
	protected Model parse(InputStream reader, String baseURI)
			throws RDFParseException, RDFHandlerException {
		return QueryResults
				.asModel(QueryResults.parseGraphBackground(reader, baseURI, rdfParserFactory.getRDFFormat(),
						null));
	}

	@Override
	protected RioSetting<?>[] getExpectedSupportedSettings() {
		return new RioSetting[] { BasicWriterSettings.PRETTY_PRINT,
				RDFJSONWriterSettings.ALLOW_MULTIPLE_OBJECT_VALUES };
	}

	@Override
	protected void setupWriterConfig(WriterConfig config) {
		config.set(RDFJSONWriterSettings.ALLOW_MULTIPLE_OBJECT_VALUES, true);
	}
}
