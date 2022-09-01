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
package org.eclipse.rdf4j.rio.trig;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;

/**
 * Unit tests for the TriG pretty printing functionality
 *
 * @author Peter Ansell
 */
public class TriGPrettyWriterBackgroundTest extends AbstractTriGWriterTest {

	public TriGPrettyWriterBackgroundTest() {
		super(new TriGWriterFactory(), new TriGParserFactory());
	}

	@Override
	protected void setupWriterConfig(WriterConfig config) {
		config.set(BasicWriterSettings.PRETTY_PRINT, true);
	}

	@Override
	protected Model parse(InputStream reader, String baseURI)
			throws RDFParseException, RDFHandlerException, IOException {
		return QueryResults
				.asModel(QueryResults.parseGraphBackground(reader, baseURI, rdfParserFactory.getRDFFormat(),
						null));
	}

}
