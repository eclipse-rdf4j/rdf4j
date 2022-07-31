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
package org.eclipse.rdf4j.rio.rdfjson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Reader;
import java.io.StringReader;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.ContextStatementCollector;
import org.eclipse.rdf4j.rio.helpers.JSONSettings;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Custom (non-manifest) tests for RDF/JSON parser.
 *
 * @author Peter Ansell
 */
public class RDFJSONParserCustomTest {

	/**
	 * Backslash escaped "h" in "http"
	 */
	private static final String BACKSLASH_ESCAPED_TEST_STRING = "{\"\\http://example.com/Subj1\": { \"http://example.com/prop1\": [{\"value\": \"http://example.com/Obj1\", \"type\": \"uri\"}]}}";

	/**
	 * Java/C++ style comments
	 */
	private static final String COMMENTS_TEST_STRING = "{/*This is a non-standard java/c++ style comment\n*/\"http://example.com/Subj1\": { \"http://example.com/prop1\": [{\"value\": \"http://example.com/Obj1\", \"type\": \"uri\"}]}}";

	/**
	 * Tests for NaN
	 */
	private static final String NON_NUMERIC_NUMBERS_TEST_STRING = "{\"http://example.com/Subj1\": { \"http://example.com/prop1\": [{\"value\": NaN, \"type\": \"literal\", \"datatype\": \"http://www.w3.org/2001/XMLSchema#double\"}]}}";

	/**
	 * Tests for numeric leading zeroes
	 */
	private static final String NUMERIC_LEADING_ZEROES_TEST_STRING = "{\"http://example.com/Subj1\": { \"http://example.com/prop1\": [{\"value\": 000042, \"type\": \"literal\", \"datatype\": \"http://www.w3.org/2001/XMLSchema#integer\"}]}}";

	/**
	 * Tests for single-quotes
	 */
	private static final String SINGLE_QUOTES_TEST_STRING = "{\'http://example.com/Subj1\': { \"http://example.com/prop1\": [{\"value\": \"42\", \'type\': \"literal\", \"datatype\": \"http://www.w3.org/2001/XMLSchema#integer\"}]}}";

	/**
	 * Tests for unquoted control char
	 */
	private static final String UNQUOTED_CONTROL_CHARS_TEST_STRING = "{\"http://example.com/Subj1\": { \"http://example.com/prop1\": [{\"value\": \"42\u0009\", \"type\": \"literal\", \"datatype\": \"http://www.w3.org/2001/XMLSchema#string\"}]}}";

	/**
	 * Tests for unquoted field names
	 */
	private static final String UNQUOTED_FIELD_NAMES_TEST_STRING = "{\"http://example.com/Subj1\": { \"http://example.com/prop1\": [{value: \"42\", type: \"literal\", datatype: \"http://www.w3.org/2001/XMLSchema#integer\"}]}}";

	/**
	 * YAML style comments
	 */
	private static final String YAML_COMMENTS_TEST_STRING = "{\n#This is a non-standard yaml style comment/*\n\"http://example.com/Subj1\": { \"http://example.com/prop1\": [{\"value\": \"http://example.com/Obj1\", \"type\": \"uri\"}]}}";

	/**
	 * Trailing comma
	 */
	private static final String TRAILING_COMMA_TEST_STRING = "{\"http://example.com/Subj1\": { \"http://example.com/prop1\": [{\"value\": \"http://example.com/Obj1\", \"type\": \"uri\",}]}}";

	/**
	 * Strict duplicate detection
	 */
	private static final String STRICT_DUPLICATE_DETECTION_TEST_STRING = "{\"http://example.com/Subj1\": { \"http://example.com/prop1\": [{\"value\": \"http://example.com/Obj1\", \"type\": \"uri\", \"type\": \"uri\"}]}}";

	private RDFParser parser;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private ParseErrorCollector errors;

	private Model model;

	private final IRI testSubjectIRI = SimpleValueFactory.getInstance().createIRI("http://example.com/Subj1");

	private final IRI testPredicate = SimpleValueFactory.getInstance().createIRI("http://example.com/prop1");

	private final IRI testObjectIRI = SimpleValueFactory.getInstance().createIRI("http://example.com/Obj1");

	private final Literal testObjectLiteralNotANumber = SimpleValueFactory.getInstance()
			.createLiteral("NaN", XSD.DOUBLE);

	private final Literal testObjectLiteralNumber = SimpleValueFactory.getInstance()
			.createLiteral("42", XSD.INTEGER);

	private final Literal testObjectLiteralUnquotedControlChar = SimpleValueFactory.getInstance()
			.createLiteral("42\u0009", XSD.STRING);

	@Before
	public void setUp() throws Exception {
		parser = Rio.createParser(RDFFormat.RDFJSON);
		errors = new ParseErrorCollector();
		model = new LinkedHashModel();
		parser.setParseErrorListener(errors);
		parser.setRDFHandler(new ContextStatementCollector(model, SimpleValueFactory.getInstance()));
	}

	private void verifyParseResults(Resource nextSubject, IRI nextPredicate, Value nextObject) throws Exception {
		assertEquals(0, errors.getWarnings().size());
		assertEquals(0, errors.getErrors().size());
		assertEquals(0, errors.getFatalErrors().size());

		assertEquals(1, model.size());
		assertTrue("model was not as expected: " + model.toString(),
				model.contains(nextSubject, nextPredicate, nextObject));
	}

	@Test
	public void testSupportedSettings() throws Exception {
		// 17 supported in RDFJSONParser + 13 from AbstractRDFParser
		assertEquals(30, parser.getSupportedSettings().size());
	}

	@Test
	public void testAllowBackslashEscapingAnyCharacterDefault() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Found IOException during parsing [line 1, column 5]");
		parser.parse(new StringReader(BACKSLASH_ESCAPED_TEST_STRING), "");
	}

	@Test
	public void testAllowBackslashEscapingAnyCharacterEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
		parser.parse(new StringReader(BACKSLASH_ESCAPED_TEST_STRING), "");
		verifyParseResults(testSubjectIRI, testPredicate, testObjectIRI);
	}

	@Test
	public void testAllowBackslashEscapingAnyCharacterDisabled() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Found IOException during parsing [line 1, column 5]");
		parser.set(JSONSettings.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, false);
		parser.parse(new StringReader(BACKSLASH_ESCAPED_TEST_STRING), "");
	}

	@Test
	public void testAllowCommentsDefault() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Found IOException during parsing [line 1, column 3]");
		parser.parse(new StringReader(COMMENTS_TEST_STRING), "");
	}

	@Test
	public void testAllowCommentsEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_COMMENTS, true);
		parser.parse(new StringReader(COMMENTS_TEST_STRING), "");
		verifyParseResults(testSubjectIRI, testPredicate, testObjectIRI);
	}

	@Test
	public void testAllowCommentsDisabled() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Found IOException during parsing [line 1, column 3]");
		parser.set(JSONSettings.ALLOW_COMMENTS, false);
		parser.parse(new StringReader(COMMENTS_TEST_STRING), "");
	}

	@Test
	public void testAllowNonNumericNumbersDefault() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Found IOException during parsing [line 1, column 74]");
		parser.parse(new StringReader(NON_NUMERIC_NUMBERS_TEST_STRING), "");
	}

	@Test
	public void testAllowNonNumericNumbersEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_NON_NUMERIC_NUMBERS, true);
		parser.parse(new StringReader(NON_NUMERIC_NUMBERS_TEST_STRING), "");
		verifyParseResults(testSubjectIRI, testPredicate, testObjectLiteralNotANumber);
	}

	@Test
	public void testAllowNonNumericNumbersDisabled() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Found IOException during parsing [line 1, column 74]");
		parser.set(JSONSettings.ALLOW_NON_NUMERIC_NUMBERS, false);
		parser.parse(new StringReader(NON_NUMERIC_NUMBERS_TEST_STRING), "");
	}

	@Test
	public void testAllowNumericLeadingZeroesDefault() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Found IOException during parsing [line 1, column 72]");
		parser.parse(new StringReader(NUMERIC_LEADING_ZEROES_TEST_STRING), "");
	}

	@Test
	public void testAllowNumericLeadingZeroesEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_NUMERIC_LEADING_ZEROS, true);
		parser.parse(new StringReader(NUMERIC_LEADING_ZEROES_TEST_STRING), "");
		verifyParseResults(testSubjectIRI, testPredicate, testObjectLiteralNumber);
	}

	@Test
	public void testAllowNumericLeadingZeroesDisabled() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Found IOException during parsing [line 1, column 72]");
		parser.set(JSONSettings.ALLOW_NUMERIC_LEADING_ZEROS, false);
		parser.parse(new StringReader(NUMERIC_LEADING_ZEROES_TEST_STRING), "");
	}

	@Test
	public void testAllowSingleQuotesDefault() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Found IOException during parsing [line 1, column 3]");
		parser.parse(new StringReader(SINGLE_QUOTES_TEST_STRING), "");
	}

	@Test
	public void testAllowSingleQuotesEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_SINGLE_QUOTES, true);
		parser.parse(new StringReader(SINGLE_QUOTES_TEST_STRING), "");
		verifyParseResults(testSubjectIRI, testPredicate, testObjectLiteralNumber);
	}

	@Test
	public void testAllowSingleQuotesDisabled() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Found IOException during parsing [line 1, column 3]");
		parser.set(JSONSettings.ALLOW_SINGLE_QUOTES, false);
		parser.parse(new StringReader(SINGLE_QUOTES_TEST_STRING), "");
	}

	@Test
	public void testAllowUnquotedControlCharactersDefault() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Found IOException during parsing [line 1, column 75]");
		parser.parse(new StringReader(UNQUOTED_CONTROL_CHARS_TEST_STRING), "");
	}

	@Test
	public void testAllowUnquotedControlCharactersEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_UNQUOTED_CONTROL_CHARS, true);
		parser.parse(new StringReader(UNQUOTED_CONTROL_CHARS_TEST_STRING), "");
		verifyParseResults(testSubjectIRI, testPredicate, testObjectLiteralUnquotedControlChar);
	}

	@Test
	public void testAllowUnquotedControlCharactersDisabled() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Found IOException during parsing [line 1, column 75]");
		parser.set(JSONSettings.ALLOW_UNQUOTED_CONTROL_CHARS, false);
		parser.parse(new StringReader(UNQUOTED_CONTROL_CHARS_TEST_STRING), "");
	}

	@Test
	public void testAllowUnquotedFieldNamesDefault() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Found IOException during parsing [line 1, column 63]");
		parser.parse(new StringReader(UNQUOTED_FIELD_NAMES_TEST_STRING), "");
	}

	@Test
	public void testAllowUnquotedFieldNamesEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_UNQUOTED_FIELD_NAMES, true);
		parser.parse(new StringReader(UNQUOTED_FIELD_NAMES_TEST_STRING), "");
		verifyParseResults(testSubjectIRI, testPredicate, testObjectLiteralNumber);
	}

	@Test
	public void testAllowUnquotedFieldNamesDisabled() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Found IOException during parsing [line 1, column 63]");
		parser.set(JSONSettings.ALLOW_UNQUOTED_FIELD_NAMES, false);
		parser.parse(new StringReader(UNQUOTED_FIELD_NAMES_TEST_STRING), "");
	}

	@Test
	public void testAllowYamlCommentsDefault() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Found IOException during parsing [line 2, column 2]");
		parser.parse(new StringReader(YAML_COMMENTS_TEST_STRING), "");
	}

	@Test
	public void testAllowYamlCommentsEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_YAML_COMMENTS, true);
		parser.parse(new StringReader(YAML_COMMENTS_TEST_STRING), "");
		verifyParseResults(testSubjectIRI, testPredicate, testObjectIRI);
	}

	@Test
	public void testAllowYamlCommentsDisabled() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Found IOException during parsing [line 2, column 2]");
		parser.set(JSONSettings.ALLOW_YAML_COMMENTS, false);
		parser.parse(new StringReader(YAML_COMMENTS_TEST_STRING), "");
	}

	@Test
	public void testAllowTrailingCommaDefault() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Found IOException during parsing [line 1, column 113]");
		parser.parse(new StringReader(TRAILING_COMMA_TEST_STRING), "");
	}

	@Test
	public void testAllowTrailingCommaEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_TRAILING_COMMA, true);
		parser.parse(new StringReader(TRAILING_COMMA_TEST_STRING), "");
		verifyParseResults(testSubjectIRI, testPredicate, testObjectIRI);
	}

	@Test
	public void testAllowTrailingCommaDisabled() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Found IOException during parsing [line 1, column 113]");
		parser.set(JSONSettings.ALLOW_TRAILING_COMMA, false);
		parser.parse(new StringReader(TRAILING_COMMA_TEST_STRING), "");
	}

	@Test
	public void testIncludeSourceLocationDefault() throws Exception {
		final Reader source = new StringReader(YAML_COMMENTS_TEST_STRING);
		try {
			parser.parse(source, "");
			fail("Expected to find an exception");
		} catch (RDFParseException e) {
			assertNotNull(e.getCause());
			assertTrue(e.getCause() instanceof JsonProcessingException);
			JsonProcessingException cause = (JsonProcessingException) e.getCause();
			assertEquals(2, cause.getLocation().getLineNr());
			assertEquals(2, cause.getLocation().getColumnNr());
			assertNotNull(cause.getLocation().getSourceRef());
			assertEquals(source, cause.getLocation().getSourceRef());
		}
	}

	@Test
	public void testIncludeSourceLocationEnabled() throws Exception {
		final Reader source = new StringReader(YAML_COMMENTS_TEST_STRING);
		try {
			parser.set(JSONSettings.INCLUDE_SOURCE_IN_LOCATION, true);
			parser.parse(source, "");
			fail("Expected to find an exception");
		} catch (RDFParseException e) {
			assertNotNull(e.getCause());
			assertTrue(e.getCause() instanceof JsonProcessingException);
			JsonProcessingException cause = (JsonProcessingException) e.getCause();
			assertEquals(2, cause.getLocation().getLineNr());
			assertEquals(2, cause.getLocation().getColumnNr());
			assertNotNull(cause.getLocation().getSourceRef());
			assertEquals(source, cause.getLocation().getSourceRef());
		}
	}

	@Test
	public void testIncludeSourceLocationDisabled() throws Exception {
		try {
			parser.set(JSONSettings.INCLUDE_SOURCE_IN_LOCATION, false);
			parser.parse(new StringReader(YAML_COMMENTS_TEST_STRING), "");
			fail("Expected to find an exception");
		} catch (RDFParseException e) {
			assertNotNull(e.getCause());
			assertTrue(e.getCause() instanceof JsonProcessingException);
			JsonProcessingException cause = (JsonProcessingException) e.getCause();
			assertEquals(2, cause.getLocation().getLineNr());
			assertEquals(2, cause.getLocation().getColumnNr());
			assertNull(cause.getLocation().getSourceRef());
		}
	}

	@Test
	public void testStrictDuplicateDetectionDefault() throws Exception {
		thrown.expect(RDFParseException.class);
		// Extra checking inbuilt in this case. This verifies it moves past Jackson into RDFJSONParser
		thrown.expectMessage(
				"Multiple types found for a single object: subject=http://example.com/Subj1 predicate=http://example.com/prop1 [line 1, column 122]");
		parser.set(JSONSettings.STRICT_DUPLICATE_DETECTION, false);
		parser.parse(new StringReader(STRICT_DUPLICATE_DETECTION_TEST_STRING), "");
	}

	@Test
	public void testStrictDuplicateDetectionEnabled() throws Exception {
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Found IOException during parsing [line 1, column 119]");
		parser.set(JSONSettings.STRICT_DUPLICATE_DETECTION, true);
		parser.parse(new StringReader(STRICT_DUPLICATE_DETECTION_TEST_STRING), "");
	}

	@Test
	public void testStrictDuplicateDetectionDisabled() throws Exception {
		thrown.expect(RDFParseException.class);
		// Extra checking inbuilt in this case. This verifies it moves past Jackson into RDFJSONParser
		thrown.expectMessage(
				"Multiple types found for a single object: subject=http://example.com/Subj1 predicate=http://example.com/prop1 [line 1, column 122]");
		parser.set(JSONSettings.STRICT_DUPLICATE_DETECTION, false);
		parser.parse(new StringReader(STRICT_DUPLICATE_DETECTION_TEST_STRING), "");
	}
}
