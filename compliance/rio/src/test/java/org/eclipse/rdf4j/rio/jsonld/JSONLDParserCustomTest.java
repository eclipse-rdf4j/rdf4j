/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.jsonld;

import static org.junit.Assert.*;

import java.io.StringReader;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.JSONSettings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Custom (non-manifest) tests for JSON-LD parser.
 * 
 * @author Peter Ansell
 */
public class JSONLDParserCustomTest {

	/**
	 * Backslash escaped "h" in "http"
	 */
	private static final String BACKSLASH_ESCAPED_TEST_STRING = "[{\"@id\": \"\\http://example.com/Subj1\",\"http://example.com/prop1\": [{\"@id\": \"http://example.com/Obj1\"}]}]";

	private RDFParser parser;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void setUp() throws Exception {
		parser = Rio.createParser(RDFFormat.JSONLD);
	}

	@Test
	public void testSupportedSettings() throws Exception {
		// 10 supported in JSONLDParser + 12 from AbstractRDFParser
		assertEquals(22, parser.getSupportedSettings().size());
	}

	@Test
	public void testAllowBackslashEscapingAnyCharacterDefault() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Could not parse JSONLD");
		parser.parse(new StringReader(BACKSLASH_ESCAPED_TEST_STRING), "");
	}

	@Test
	public void testAllowBackslashEscapingAnyCharacterEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
		parser.parse(new StringReader(BACKSLASH_ESCAPED_TEST_STRING), "");
	}

	@Test
	public void testAllowBackslashEscapingAnyCharacterDisabled() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Could not parse JSONLD");
		parser.set(JSONSettings.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, false);
		parser.parse(new StringReader(BACKSLASH_ESCAPED_TEST_STRING), "");
	}
}
