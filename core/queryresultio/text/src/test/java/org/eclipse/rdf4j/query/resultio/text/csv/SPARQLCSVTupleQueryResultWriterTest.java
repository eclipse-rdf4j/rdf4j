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
package org.eclipse.rdf4j.query.resultio.text.csv;

import org.eclipse.rdf4j.query.resultio.TupleQueryResultParserFactory;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterFactory;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.testsuite.query.resultio.AbstractTupleQueryResultWriterTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Jeen Broekstra
 */
public class SPARQLCSVTupleQueryResultWriterTest extends AbstractTupleQueryResultWriterTest {

	@Override
	protected TupleQueryResultParserFactory getParserFactory() {
		return new SPARQLResultsCSVParserFactory();
	}

	@Override
	protected TupleQueryResultWriterFactory getWriterFactory() {
		return new SPARQLResultsCSVWriterFactory();
	}

	@Override
	@Disabled("pending implementation of RDF-star extensions for the csv format")
	@Test
	public void testRDFStarHandling_NoEncoding() {
	}

	@Override
	@Disabled("pending implementation of RDF-star extensions for the csv format")
	@Test
	public void testRDFStarHandling_DeepNesting() {
	}

	@Override
	protected RioSetting<?>[] getExpectedSupportedSettings() {
		return new RioSetting<?>[] {
				BasicWriterSettings.ENCODE_RDF_STAR,
				BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL
		};
	}
}
