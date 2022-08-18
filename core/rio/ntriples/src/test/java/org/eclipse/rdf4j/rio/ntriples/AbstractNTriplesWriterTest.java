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
package org.eclipse.rdf4j.rio.ntriples;

import org.eclipse.rdf4j.rio.RDFParserFactory;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterTest;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.NTriplesWriterSettings;

/**
 * @author Jeen Broekstra
 *
 */
public abstract class AbstractNTriplesWriterTest extends RDFWriterTest {

	protected AbstractNTriplesWriterTest(RDFWriterFactory writerF, RDFParserFactory parserF) {
		super(writerF, parserF);
	}

	@Override
	protected RioSetting<?>[] getExpectedSupportedSettings() {
		return new RioSetting[] {
				BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL,
				NTriplesWriterSettings.ESCAPE_UNICODE
		};
	}

}
