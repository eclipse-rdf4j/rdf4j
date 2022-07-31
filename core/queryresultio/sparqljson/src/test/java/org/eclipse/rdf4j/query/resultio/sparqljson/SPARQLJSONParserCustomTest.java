/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.sparqljson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.eclipse.rdf4j.query.resultio.QueryResultParser;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.helpers.QueryResultCollector;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.helpers.JSONSettings;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Custom (non-manifest) tests for SPARQL/JSON parser.
 *
 * @author Peter Ansell
 */
public class SPARQLJSONParserCustomTest {

	/**
	 * Backslash escaped "h" in "http"
	 */
	private static final String BACKSLASH_ESCAPED_TEST_STRING = "{\"head\": { \"vars\": [ \"test-binding\" ]  } , \"results\": { \"bindings\": [{ \"test-binding\": {\"value\": \"\\http://example.com/Obj1\", \"type\": \"uri\"}}]}}";

	/**
	 * Java/C++ style comments
	 */
	private static final String COMMENTS_TEST_STRING = "{/*This is a non-standard java/c++ style comment\\n*/\"head\": { \"vars\": [ \"test-binding\" ]  } , \"results\": { \"bindings\": [{ \"test-binding\": {\"value\": \"http://example.com/Obj1\", \"type\": \"uri\"}}]}}";

	/**
	 * Tests for NaN
	 */
	private static final String NON_NUMERIC_NUMBERS_TEST_STRING = "{\"head\": { \"vars\": [ \"test-binding\" ]  } , \"results\": { \"bindings\": [{ \"test-binding\": {\"value\": NaN, \"type\": \"literal\", \"datatype\": \"http://www.w3.org/2001/XMLSchema#double\"}}]}}";

	/**
	 * Tests for numeric leading zeroes
	 */
	private static final String NUMERIC_LEADING_ZEROES_TEST_STRING = "{\"head\": { \"vars\": [ \"test-binding\" ]  } , \"results\": { \"bindings\": [{ \"test-binding\": {\"value\": 000042, \"type\": \"literal\", \"datatype\": \"http://www.w3.org/2001/XMLSchema#integer\"}}]}}";

	/**
	 * Tests for single-quotes
	 */
	private static final String SINGLE_QUOTES_TEST_STRING = "{\"head\": { \"vars\": [ \"test-binding\" ]  } , \"results\": { \"bindings\": [{ \"test-binding\": {\'value\': \"42\", \"type\": \"literal\", \'datatype\': \"http://www.w3.org/2001/XMLSchema#integer\"}}]}}";

	/**
	 * Tests for unquoted control char
	 */
	private static final String UNQUOTED_CONTROL_CHARS_TEST_STRING = "{\"head\": { \"vars\": [ \"test-binding\" ]  } , \"results\": { \"bindings\": [{ \"test-binding\": {\"value\": \"42\u0009\", \"type\": \"literal\"}}]}}";

	/**
	 * Tests for unquoted field names
	 */
	private static final String UNQUOTED_FIELD_NAMES_TEST_STRING = "{\"head\": { \"vars\": [ \"test-binding\" ]  } , results: { \"bindings\": [{ \"test-binding\": {value: \"42\", \"type\": \"literal\", datatype: \"http://www.w3.org/2001/XMLSchema#integer\"}}]}}";

	/**
	 * YAML style comments
	 */
	private static final String YAML_COMMENTS_TEST_STRING = "{\n#This is a non-standard yaml style comment/*\n\"head\": { \"vars\": [ \"test-binding\" ]  } , \"results\": { \"bindings\": [{ \"test-binding\": {\"value\": \"http://example.com/Obj1\", \"type\": \"uri\"}}]}}";

	/**
	 * Trailing comma
	 */
	private static final String TRAILING_COMMA_TEST_STRING = "{\"head\": { \"vars\": [ \"test-binding\", ]  } , \"results\": { \"bindings\": [{ \"test-binding\": {\"value\": \"http://example.com/Obj1\", \"type\": \"uri\",}}]}}";

	/**
	 * Strict duplicate detection
	 */
	private static final String STRICT_DUPLICATE_DETECTION_TEST_STRING = "{\"head\": { \"vars\": [ \"test-binding\", ]  } , \"results\": { \"bindings\": [{ \"test-binding\": {\"value\": \"http://example.com/Obj1\", \"type\": \"uri\", \"type\": \"uri\"}}]}}";

	private QueryResultParser parser;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private QueryResultCollector results;

	private ParseErrorCollector errors;

	private final String testBindingName = "test-binding";

	private final IRI testBindingValueIRI = SimpleValueFactory.getInstance().createIRI("http://example.com/Obj1");

	private final Literal testBindingValueNotANumber = SimpleValueFactory.getInstance()
			.createLiteral("NaN", XSD.DOUBLE);

	private final Literal testBindingValueLiteralNumber = SimpleValueFactory.getInstance()
			.createLiteral("42", XSD.INTEGER);

	private final Literal testBindingValueLiteralUnquotedControlChar = SimpleValueFactory.getInstance()
			.createLiteral("42\u0009", XSD.STRING);

	@Before
	public void setUp() throws Exception {
		parser = QueryResultIO.createTupleParser(TupleQueryResultFormat.JSON);
		errors = new ParseErrorCollector();
		results = new QueryResultCollector();
		parser.setParseErrorListener(errors);
		parser.setQueryResultHandler(results);
	}

	private void verifyParseResults(String bindingName, Value nextObject) throws Exception {
		assertEquals(0, errors.getWarnings().size());
		assertEquals(0, errors.getErrors().size());
		assertEquals(0, errors.getFatalErrors().size());

		assertEquals(1, results.getBindingSets().size());
		BindingSet bindingSet = results.getBindingSets().get(0);
		assertTrue(bindingSet.hasBinding(bindingName));
		assertEquals(nextObject, bindingSet.getValue(bindingName));
	}

	private InputStream stringToInputStream(String input) {
		return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void testSupportedSettings() throws Exception {
		// 11 supported in AbstractSPARQLJSONParser + 0 from AbstractQueryResultParser
		assertEquals(11, parser.getSupportedSettings().size());
	}

	@Test
	public void testAllowBackslashEscapingAnyCharacterDefault() throws Exception {
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.parseQueryResult(stringToInputStream(BACKSLASH_ESCAPED_TEST_STRING));
	}

	@Test
	public void testAllowBackslashEscapingAnyCharacterEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
		parser.parseQueryResult(stringToInputStream(BACKSLASH_ESCAPED_TEST_STRING));
		verifyParseResults(testBindingName, testBindingValueIRI);
	}

	@Test
	public void testAllowBackslashEscapingAnyCharacterDisabled() throws Exception {
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.set(JSONSettings.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, false);
		parser.parseQueryResult(stringToInputStream(BACKSLASH_ESCAPED_TEST_STRING));
	}

	@Test
	public void testAllowCommentsDefault() throws Exception {
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.parseQueryResult(stringToInputStream(COMMENTS_TEST_STRING));
	}

	@Test
	public void testAllowCommentsEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_COMMENTS, true);
		parser.parseQueryResult(stringToInputStream(COMMENTS_TEST_STRING));
		verifyParseResults(testBindingName, testBindingValueIRI);
	}

	@Test
	public void testAllowCommentsDisabled() throws Exception {
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.set(JSONSettings.ALLOW_COMMENTS, false);
		parser.parseQueryResult(stringToInputStream(COMMENTS_TEST_STRING));
	}

	@Test
	public void testAllowNonNumericNumbersDefault() throws Exception {
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.parseQueryResult(stringToInputStream(NON_NUMERIC_NUMBERS_TEST_STRING));
	}

	@Test
	public void testAllowNonNumericNumbersEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_NON_NUMERIC_NUMBERS, true);
		parser.parseQueryResult(stringToInputStream(NON_NUMERIC_NUMBERS_TEST_STRING));
		verifyParseResults(testBindingName, testBindingValueNotANumber);
	}

	@Test
	public void testAllowNonNumericNumbersDisabled() throws Exception {
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.set(JSONSettings.ALLOW_NON_NUMERIC_NUMBERS, false);
		parser.parseQueryResult(stringToInputStream(NON_NUMERIC_NUMBERS_TEST_STRING));
	}

	@Test
	public void testAllowNumericLeadingZeroesDefault() throws Exception {
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.parseQueryResult(stringToInputStream(NUMERIC_LEADING_ZEROES_TEST_STRING));
	}

	@Test
	public void testAllowNumericLeadingZeroesEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_NUMERIC_LEADING_ZEROS, true);
		parser.parseQueryResult(stringToInputStream(NUMERIC_LEADING_ZEROES_TEST_STRING));
		verifyParseResults(testBindingName, testBindingValueLiteralNumber);
	}

	@Test
	public void testAllowNumericLeadingZeroesDisabled() throws Exception {
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.set(JSONSettings.ALLOW_NUMERIC_LEADING_ZEROS, false);
		parser.parseQueryResult(stringToInputStream(NUMERIC_LEADING_ZEROES_TEST_STRING));
	}

	@Test
	public void testAllowSingleQuotesDefault() throws Exception {
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.parseQueryResult(stringToInputStream(SINGLE_QUOTES_TEST_STRING));
	}

	@Test
	public void testAllowSingleQuotesEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_SINGLE_QUOTES, true);
		parser.parseQueryResult(stringToInputStream(SINGLE_QUOTES_TEST_STRING));
		verifyParseResults(testBindingName, testBindingValueLiteralNumber);
	}

	@Test
	public void testAllowSingleQuotesDisabled() throws Exception {
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.set(JSONSettings.ALLOW_SINGLE_QUOTES, false);
		parser.parseQueryResult(stringToInputStream(SINGLE_QUOTES_TEST_STRING));
	}

	@Test
	public void testAllowUnquotedControlCharactersDefault() throws Exception {
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.parseQueryResult(stringToInputStream(UNQUOTED_CONTROL_CHARS_TEST_STRING));
	}

	@Test
	public void testAllowUnquotedControlCharactersEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_UNQUOTED_CONTROL_CHARS, true);
		parser.parseQueryResult(stringToInputStream(UNQUOTED_CONTROL_CHARS_TEST_STRING));
		verifyParseResults(testBindingName, testBindingValueLiteralUnquotedControlChar);
	}

	@Test
	public void testAllowUnquotedControlCharactersDisabled() throws Exception {
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.set(JSONSettings.ALLOW_UNQUOTED_CONTROL_CHARS, false);
		parser.parseQueryResult(stringToInputStream(UNQUOTED_CONTROL_CHARS_TEST_STRING));
	}

	@Test
	public void testAllowUnquotedFieldNamesDefault() throws Exception {
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.parseQueryResult(stringToInputStream(UNQUOTED_FIELD_NAMES_TEST_STRING));
	}

	@Test
	public void testAllowUnquotedFieldNamesEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_UNQUOTED_FIELD_NAMES, true);
		parser.parseQueryResult(stringToInputStream(UNQUOTED_FIELD_NAMES_TEST_STRING));
		verifyParseResults(testBindingName, testBindingValueLiteralNumber);
	}

	@Test
	public void testAllowUnquotedFieldNamesDisabled() throws Exception {
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.set(JSONSettings.ALLOW_UNQUOTED_FIELD_NAMES, false);
		parser.parseQueryResult(stringToInputStream(UNQUOTED_FIELD_NAMES_TEST_STRING));
	}

	@Test
	public void testAllowYamlCommentsDefault() throws Exception {
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.parseQueryResult(stringToInputStream(YAML_COMMENTS_TEST_STRING));
	}

	@Test
	public void testAllowYamlCommentsEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_YAML_COMMENTS, true);
		parser.parseQueryResult(stringToInputStream(YAML_COMMENTS_TEST_STRING));
		verifyParseResults(testBindingName, testBindingValueIRI);
	}

	@Test
	public void testAllowYamlCommentsDisabled() throws Exception {
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.set(JSONSettings.ALLOW_YAML_COMMENTS, false);
		parser.parseQueryResult(stringToInputStream(YAML_COMMENTS_TEST_STRING));
	}

	@Test
	public void testAllowTrailingCommaDefault() throws Exception {
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.parseQueryResult(stringToInputStream(TRAILING_COMMA_TEST_STRING));
	}

	@Test
	public void testAllowTrailingCommaEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_TRAILING_COMMA, true);
		parser.parseQueryResult(stringToInputStream(TRAILING_COMMA_TEST_STRING));
		verifyParseResults(testBindingName, testBindingValueIRI);
	}

	@Test
	public void testAllowTrailingCommaDisabled() throws Exception {
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.set(JSONSettings.ALLOW_TRAILING_COMMA, false);
		parser.parseQueryResult(stringToInputStream(TRAILING_COMMA_TEST_STRING));
	}

	@Test
	public void testIncludeSourceLocationDefault() throws Exception {
		final InputStream source = stringToInputStream(YAML_COMMENTS_TEST_STRING);
		try {
			parser.parseQueryResult(source);
			fail("Expected to find an exception");
		} catch (QueryResultParseException e) {
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
		final InputStream source = stringToInputStream(YAML_COMMENTS_TEST_STRING);
		try {
			parser.set(JSONSettings.INCLUDE_SOURCE_IN_LOCATION, true);
			parser.parseQueryResult(source);
			fail("Expected to find an exception");
		} catch (QueryResultParseException e) {
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
			parser.parseQueryResult(stringToInputStream(YAML_COMMENTS_TEST_STRING));
			fail("Expected to find an exception");
		} catch (QueryResultParseException e) {
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
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.set(JSONSettings.STRICT_DUPLICATE_DETECTION, false);
		parser.parseQueryResult(stringToInputStream(STRICT_DUPLICATE_DETECTION_TEST_STRING));
	}

	@Test
	public void testStrictDuplicateDetectionEnabled() throws Exception {
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.set(JSONSettings.STRICT_DUPLICATE_DETECTION, true);
		parser.parseQueryResult(stringToInputStream(STRICT_DUPLICATE_DETECTION_TEST_STRING));
	}

	@Test
	public void testStrictDuplicateDetectionDisabled() throws Exception {
		thrown.expect(QueryResultParseException.class);
		thrown.expectMessage("Could not parse SPARQL/JSON");
		parser.set(JSONSettings.STRICT_DUPLICATE_DETECTION, false);
		parser.parseQueryResult(stringToInputStream(STRICT_DUPLICATE_DETECTION_TEST_STRING));
	}

	@Test
	public void testLangMissingOnStringLang() throws Exception {
		ParserConfig config = new ParserConfig();
		QueryResultCollector handler = new QueryResultCollector();
		ParseErrorCollector errorCollector = new ParseErrorCollector();
		QueryResultParser aParser = QueryResultIO.createTupleParser(TupleQueryResultFormat.JSON)
				.setQueryResultHandler(handler)
				.setParserConfig(config)
				.setParseErrorListener(errorCollector);

		aParser.parseQueryResult(this.getClass()
				.getResourceAsStream("/sparqljson/dbpedia-stringlang-bug.srj"));

		assertEquals(2, handler.getBindingSets().size());
		assertEquals("Altin Lala", handler.getBindingSets().get(0).getBinding("lc").getValue().stringValue());
		assertEquals("http://de.dbpedia.org/resource/Altin_Lala",
				handler.getBindingSets().get(0).getBinding("subj").getValue().stringValue());
		assertEquals("Hans Lala", handler.getBindingSets().get(1).getBinding("lc").getValue().stringValue());
		assertEquals("http://de.dbpedia.org/resource/Hans_Lala",
				handler.getBindingSets().get(1).getBinding("subj").getValue().stringValue());
	}
}
