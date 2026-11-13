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
package org.eclipse.rdf4j.rio.jelly;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.junit.jupiter.api.Test;

import eu.neverblink.jelly.convert.rdf4j.rio.JellyParserFactory;

/**
 * Checks that the Jelly parser registered with Rio validates its input by default, like RDF4J's other parsers.
 */
public class JellyParserDefaultsTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@Test
	public void testFactoryChecksByDefault() {
		assertTrue(new JellyParserFactory().isChecking());
	}

	@Test
	public void testRioParserRespectsValidationSettings() throws Exception {
		Model model = new LinkedHashModel();
		model.add(VF.createIRI("http://example.org/s"), VF.createIRI("http://example.org/p"),
				VF.createLiteral("abc", VF.createIRI("http://www.w3.org/2001/XMLSchema#integer")));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Rio.write(model, out, RDFFormat.JELLY);

		RDFParser parser = Rio.createParser(RDFFormat.JELLY);
		parser.getParserConfig().set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
		parser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);

		try {
			parser.parse(new ByteArrayInputStream(out.toByteArray()));
		} catch (RDFParseException e) {
			return;
		}
		throw new AssertionError("Expected an invalid xsd:integer value to be rejected");
	}
}
