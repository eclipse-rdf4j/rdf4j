/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.jelly;

import org.eclipse.rdf4j.query.resultio.TupleQueryResultParserFactory;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterFactory;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.testsuite.query.resultio.AbstractTupleQueryResultWriterTest;

import eu.neverblink.jelly.convert.rdf4j.sparql.JellySparqlTupleParserFactory;
import eu.neverblink.jelly.convert.rdf4j.sparql.JellySparqlTupleWriterFactory;
import eu.neverblink.jelly.convert.rdf4j.sparql.JellySparqlWriterSettings;

public class JellySparqlTupleQueryResultWriterTest extends AbstractTupleQueryResultWriterTest {

	@Override
	protected TupleQueryResultParserFactory getParserFactory() {
		return new JellySparqlTupleParserFactory();
	}

	@Override
	protected TupleQueryResultWriterFactory getWriterFactory() {
		return new JellySparqlTupleWriterFactory();
	}

	@Override
	protected RioSetting<?>[] getExpectedSupportedSettings() {
		return new RioSetting<?>[] {
				BasicWriterSettings.ENCODE_TRIPLE_TERMS,
				BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL,
				JellySparqlWriterSettings.MAX_VALUES_PER_FRAME,
				JellySparqlWriterSettings.DELIMITED_OUTPUT,
				JellySparqlWriterSettings.PUNCTUATED,
				JellySparqlWriterSettings.STREAM_NAME,
				JellySparqlWriterSettings.RDF_VERSION,
				JellySparqlWriterSettings.MAX_NAME_TABLE_SIZE,
				JellySparqlWriterSettings.MAX_PREFIX_TABLE_SIZE,
				JellySparqlWriterSettings.MAX_DATATYPE_TABLE_SIZE,
		};
	}
}
