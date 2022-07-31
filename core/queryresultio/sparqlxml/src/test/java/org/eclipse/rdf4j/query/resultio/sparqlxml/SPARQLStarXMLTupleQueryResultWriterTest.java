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
package org.eclipse.rdf4j.query.resultio.sparqlxml;

import org.eclipse.rdf4j.query.resultio.BasicQueryWriterSettings;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParserFactory;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterFactory;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.XMLWriterSettings;
import org.eclipse.rdf4j.testsuite.query.resultio.AbstractTupleQueryResultWriterTest;

/**
 * @author Jeen Broekstra
 *
 */
public class SPARQLStarXMLTupleQueryResultWriterTest extends AbstractTupleQueryResultWriterTest {

	@Override
	protected TupleQueryResultParserFactory getParserFactory() {
		return new SPARQLStarResultsXMLParserFactory();
	}

	@Override
	protected TupleQueryResultWriterFactory getWriterFactory() {
		return new SPARQLStarResultsXMLWriterFactory();
	}

	@Override
	protected RioSetting<?>[] getExpectedSupportedSettings() {
		return new RioSetting<?>[] {
				BasicQueryWriterSettings.ADD_SESAME_QNAME,
				BasicWriterSettings.PRETTY_PRINT,
				BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL,
				BasicWriterSettings.ENCODE_RDF_STAR,
				XMLWriterSettings.INCLUDE_XML_PI
		};
	}
}
