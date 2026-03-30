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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.ContentReference;

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

	@BeforeEach
	public void setUp() {
		parser = QueryResultIO.createTupleParser(TupleQueryResultFormat.JSON);
		errors = new ParseErrorCollector();
		results = new QueryResultCollector();
		parser.setParseErrorListener(errors);
		parser.setQueryResultHandler(results);
	}

	private void verifyParseResults(String bindingName, Value nextObject) {
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
	public void testSupportedSettings() {
		// 11 supported in AbstractSPARQLJSONParser + 0 from AbstractQueryResultParser
		assertEquals(11, parser.getSupportedSettings().size());
	}

	@Test
	public void testAllowBackslashEscapingAnyCharacterDefault() {
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(BACKSLASH_ESCAPED_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
	}

	@Test
	public void testAllowBackslashEscapingAnyCharacterEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
		parser.parseQueryResult(stringToInputStream(BACKSLASH_ESCAPED_TEST_STRING));
		verifyParseResults(testBindingName, testBindingValueIRI);
	}

	@Test
	public void testAllowBackslashEscapingAnyCharacterDisabled() {
		parser.set(JSONSettings.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, false);
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(BACKSLASH_ESCAPED_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
	}

	@Test
	public void testAllowCommentsDefault() {
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(COMMENTS_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
	}

	@Test
	public void testAllowCommentsEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_COMMENTS, true);
		parser.parseQueryResult(stringToInputStream(COMMENTS_TEST_STRING));
		verifyParseResults(testBindingName, testBindingValueIRI);
	}

	@Test
	public void testAllowCommentsDisabled() {
		parser.set(JSONSettings.ALLOW_COMMENTS, false);
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(COMMENTS_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
	}

	@Test
	public void testAllowNonNumericNumbersDefault() {
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(NON_NUMERIC_NUMBERS_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
	}

	@Test
	public void testAllowNonNumericNumbersEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_NON_NUMERIC_NUMBERS, true);
		parser.parseQueryResult(stringToInputStream(NON_NUMERIC_NUMBERS_TEST_STRING));
		verifyParseResults(testBindingName, testBindingValueNotANumber);
	}

	@Test
	public void testAllowNonNumericNumbersDisabled() {
		parser.set(JSONSettings.ALLOW_NON_NUMERIC_NUMBERS, false);
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(NON_NUMERIC_NUMBERS_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
	}

	@Test
	public void testAllowNumericLeadingZeroesDefault() {
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(NUMERIC_LEADING_ZEROES_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
	}

	@Test
	public void testAllowNumericLeadingZeroesEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_NUMERIC_LEADING_ZEROS, true);
		parser.parseQueryResult(stringToInputStream(NUMERIC_LEADING_ZEROES_TEST_STRING));
		verifyParseResults(testBindingName, testBindingValueLiteralNumber);
	}

	@Test
	public void testAllowNumericLeadingZeroesDisabled() {
		parser.set(JSONSettings.ALLOW_NUMERIC_LEADING_ZEROS, false);
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(NUMERIC_LEADING_ZEROES_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
	}

	@Test
	public void testAllowSingleQuotesDefault() {
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(SINGLE_QUOTES_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
	}

	@Test
	public void testAllowSingleQuotesEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_SINGLE_QUOTES, true);
		parser.parseQueryResult(stringToInputStream(SINGLE_QUOTES_TEST_STRING));
		verifyParseResults(testBindingName, testBindingValueLiteralNumber);
	}

	@Test
	public void testAllowSingleQuotesDisabled() {
		parser.set(JSONSettings.ALLOW_SINGLE_QUOTES, false);
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(SINGLE_QUOTES_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
	}

	@Test
	public void testAllowUnquotedControlCharactersDefault() {
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(UNQUOTED_CONTROL_CHARS_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
	}

	@Test
	public void testAllowUnquotedControlCharactersEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_UNQUOTED_CONTROL_CHARS, true);
		parser.parseQueryResult(stringToInputStream(UNQUOTED_CONTROL_CHARS_TEST_STRING));
		verifyParseResults(testBindingName, testBindingValueLiteralUnquotedControlChar);
	}

	@Test
	public void testAllowUnquotedControlCharactersDisabled() {
		parser.set(JSONSettings.ALLOW_UNQUOTED_CONTROL_CHARS, false);
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(UNQUOTED_CONTROL_CHARS_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
	}

	@Test
	public void testAllowUnquotedFieldNamesDefault() {
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(UNQUOTED_FIELD_NAMES_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
	}

	@Test
	public void testAllowUnquotedFieldNamesEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_UNQUOTED_FIELD_NAMES, true);
		parser.parseQueryResult(stringToInputStream(UNQUOTED_FIELD_NAMES_TEST_STRING));
		verifyParseResults(testBindingName, testBindingValueLiteralNumber);
	}

	@Test
	public void testAllowUnquotedFieldNamesDisabled() {
		parser.set(JSONSettings.ALLOW_UNQUOTED_FIELD_NAMES, false);
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(UNQUOTED_FIELD_NAMES_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
	}

	@Test
	public void testAllowYamlCommentsDefault() {
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(YAML_COMMENTS_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
	}

	@Test
	public void testAllowYamlCommentsEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_YAML_COMMENTS, true);
		parser.parseQueryResult(stringToInputStream(YAML_COMMENTS_TEST_STRING));
		verifyParseResults(testBindingName, testBindingValueIRI);
	}

	@Test
	public void testAllowYamlCommentsDisabled() {
		parser.set(JSONSettings.ALLOW_YAML_COMMENTS, false);
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(YAML_COMMENTS_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
	}

	@Test
	public void testAllowTrailingCommaDefault() {
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(TRAILING_COMMA_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
	}

	@Test
	public void testAllowTrailingCommaEnabled() throws Exception {
		parser.set(JSONSettings.ALLOW_TRAILING_COMMA, true);
		parser.parseQueryResult(stringToInputStream(TRAILING_COMMA_TEST_STRING));
		verifyParseResults(testBindingName, testBindingValueIRI);
	}

	@Test
	public void testAllowTrailingCommaDisabled() {
		parser.set(JSONSettings.ALLOW_TRAILING_COMMA, false);
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(TRAILING_COMMA_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
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
			assertEquals(1, cause.getLocation().getColumnNr());
			assertNotEquals(ContentReference.unknown(), cause.getLocation().contentReference());
			assertEquals(source, cause.getLocation().contentReference().getRawContent());
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
			assertEquals(1, cause.getLocation().getColumnNr());
			assertNotEquals(ContentReference.unknown(), cause.getLocation().contentReference());
			assertEquals(source, cause.getLocation().contentReference().getRawContent());
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
			assertEquals(1, cause.getLocation().getColumnNr());
			assertEquals(ContentReference.unknown(), cause.getLocation().contentReference());
		}
	}

	@Test
	public void testStrictDuplicateDetectionDefault() {
		parser.set(JSONSettings.STRICT_DUPLICATE_DETECTION, false);
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(STRICT_DUPLICATE_DETECTION_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
	}

	@Test
	public void testStrictDuplicateDetectionEnabled() {
		parser.set(JSONSettings.STRICT_DUPLICATE_DETECTION, true);
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(STRICT_DUPLICATE_DETECTION_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
	}

	@Test
	public void testStrictDuplicateDetectionDisabled() {
		parser.set(JSONSettings.STRICT_DUPLICATE_DETECTION, false);
		assertThatThrownBy(() -> parser.parseQueryResult(stringToInputStream(STRICT_DUPLICATE_DETECTION_TEST_STRING)))
				.isInstanceOf(QueryResultParseException.class)
				.hasMessage("Could not parse SPARQL/JSON");
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
