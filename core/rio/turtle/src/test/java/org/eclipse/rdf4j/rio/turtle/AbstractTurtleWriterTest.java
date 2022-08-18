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
package org.eclipse.rdf4j.rio.turtle;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterTest;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.TurtleWriterSettings;

/**
 * @author Jeen Broekstra
 *
 */
public abstract class AbstractTurtleWriterTest extends RDFWriterTest {

	protected AbstractTurtleWriterTest(RDFWriterFactory writerF, RDFParserFactory parserF) {
		super(writerF, parserF);
	}

	@Override
	protected RioSetting<?>[] getExpectedSupportedSettings() {
		return new RioSetting<?>[] {
				BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL,
				BasicWriterSettings.PRETTY_PRINT,
				BasicWriterSettings.INLINE_BLANK_NODES,
				BasicWriterSettings.BASE_DIRECTIVE,
				TurtleWriterSettings.ABBREVIATE_NUMBERS
		};
	}

	protected Model getAbbrevTestModel() {
		Model m = new LinkedHashModel();
		m.add(Values.iri("http://www.example.com/double"), RDF.VALUE, Values.literal("1234567.89", XSD.DOUBLE));
		m.add(Values.iri("http://www.example.com/int"), RDF.VALUE, Values.literal("-2", XSD.INTEGER));
		m.add(Values.iri("http://www.example.com/decimal"), RDF.VALUE, Values.literal("55.66", XSD.DECIMAL));
		return m;
	}
}
