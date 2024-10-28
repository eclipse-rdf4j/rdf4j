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
package org.eclipse.rdf4j.rio.jsonld;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.eclipse.rdf4j.rio.helpers.JSONLDSettings.SECURE_MODE;
import static org.eclipse.rdf4j.rio.helpers.JSONLDSettings.WHITELIST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.ContextStatementCollector;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import jakarta.json.spi.JsonProvider;
import no.hasmac.jsonld.document.Document;
import no.hasmac.jsonld.document.JsonDocument;

/**
 * Custom (non-manifest) tests for JSON-LD parser.
 *
 * Tests with the old JSONLDSettings class
 *
 * @author Peter Ansell
 */
public class JSONLDParserCustomOldTest {

	/**
	 * Backslash escaped "h" in "http"
	 */
	private static final String BACKSLASH_ESCAPED_TEST_STRING = "[{\"@id\": \"\\http://example.com/Subj1\",\"http://example.com/prop1\": [{\"@id\": \"http://example.com/Obj1\"}]}]";

	/**
	 * Java/C++ style comments
	 */
	private static final String COMMENTS_TEST_STRING = "[{/*This is a non-standard java/c++ style comment\n*/\"@id\": \"http://example.com/Subj1\",\"http://example.com/prop1\": [{\"@id\": \"http://example.com/Obj1\"}]}]";

	/**
	 * Tests for NaN
	 */
	private static final String NON_NUMERIC_NUMBERS_TEST_STRING = "[{\"@id\": \"http://example.com/Subj1\",\"http://example.com/prop1\": NaN}]";

	/**
	 * Tests for numeric leading zeroes
	 */
	private static final String NUMERIC_LEADING_ZEROES_TEST_STRING = "[{\"@id\": \"http://example.com/Subj1\",\"http://example.com/prop1\": 000042}]";

	/**
	 * Tests for single-quotes
	 */
	private static final String SINGLE_QUOTES_TEST_STRING = "[{\'@id\': \"http://example.com/Subj1\",\'http://example.com/prop1\': 42}]";

	/**
	 * Tests for unquoted control char
	 */
	private static final String UNQUOTED_CONTROL_CHARS_TEST_STRING = "[{\"@id\": \"http://example.com/Subj1\",\"http://example.com/prop1\": \"42\u0009\"}]";

	/**
	 * Tests for unquoted field names
	 */
	private static final String UNQUOTED_FIELD_NAMES_TEST_STRING = "[{@id: \"http://example.com/Subj1\",\"http://example.com/prop1\": 42}]";

	/**
	 * YAML style comments
	 */
	private static final String YAML_COMMENTS_TEST_STRING = "[\n{#This is a non-standard yaml style comment/*\n\"@id\": \"http://example.com/Subj1\",\"http://example.com/prop1\": [{\"@id\": \"http://example.com/Obj1\"}]}]";

	/**
	 * Trailing comma
	 */
	private static final String TRAILING_COMMA_TEST_STRING = "[{\"@id\": \"http://example.com/Subj1\",\"http://example.com/prop1\": [{\"@id\": \"http://example.com/Obj1\"},]}]";

	/**
	 * Strict duplicate detection
	 */
	private static final String STRICT_DUPLICATE_DETECTION_TEST_STRING = "[{\"@context\": {}, \"@context\": {}, \"@id\": \"http://example.com/Subj1\",\"http://example.com/prop1\": [{\"@id\": \"http://example.com/Obj1\"}]}]";

	/**
	 * Used for custom document loader
	 */
	private static final String LOADER_CONTEXT = "{ \"@context\": {\"prop\": \"http://example.com/prop1\"} }";
	private static final String LOADER_JSONLD = "{ \"@context\": \"http://example.com/context.jsonld\", \"@id\": \"http://example.com/Subj1\", \"prop\": \"Property\" }";

	private RDFParser parser;

	private ParseErrorCollector errors;

	private Model model;

	private final SimpleValueFactory F = SimpleValueFactory.getInstance();

	private final IRI testSubjectIRI = F.createIRI("http://example.com/Subj1");
	private final IRI testPredicate = F.createIRI("http://example.com/prop1");
	private final IRI testObjectIRI = F.createIRI("http://example.com/Obj1");

	private final Literal testObjectLiteralNotANumber = F.createLiteral("NaN", XSD.DOUBLE);
	private final Literal testObjectLiteralNumber = F.createLiteral("42", XSD.INTEGER);
	private final Literal testObjectLiteralUnquotedControlChar = F.createLiteral("42\u0009", XSD.STRING);

	@BeforeEach
	public void setUp() {
		parser = Rio.createParser(RDFFormat.JSONLD);
		errors = new ParseErrorCollector();
		model = new LinkedHashModel();
		parser.setParseErrorListener(errors);
		parser.setRDFHandler(new ContextStatementCollector(model, F));
	}

	private void verifyParseResults(Resource nextSubject, IRI nextPredicate, Value nextObject) {
		assertEquals(0, errors.getWarnings().size());
		assertEquals(0, errors.getErrors().size());
		assertEquals(0, errors.getFatalErrors().size());

		assertEquals(1, model.size());
		assertTrue(model.contains(nextSubject, nextPredicate, nextObject),
				"model was not as expected: " + model.toString());
	}

	@Test
	public void testSupportedSettings() {
		assertEquals(19, parser.getSupportedSettings().size());
	}

	@Test
	public void testAllowBackslashEscapingAnyCharacterDefault() {
		assertThatThrownBy(() -> parser.parse(new StringReader(BACKSLASH_ESCAPED_TEST_STRING), ""))
				.isInstanceOf(RDFParseException.class)
				.hasMessageContaining("Could not parse JSONLD");
	}

	@Test
	public void testAllowBackslashEscapingAnyCharacterDisabled() {
		assertThatThrownBy(() -> parser.parse(new StringReader(BACKSLASH_ESCAPED_TEST_STRING), ""))
				.isInstanceOf(RDFParseException.class)
				.hasMessageContaining("Could not parse JSONLD");

	}

	@Test
	public void testAllowCommentsDefault() {
		assertThatThrownBy(() -> parser.parse(new StringReader(COMMENTS_TEST_STRING), ""))
				.isInstanceOf(RDFParseException.class)
				.hasMessageContaining("Could not parse JSONLD");
	}

	@Test
	public void testAllowNonNumericNumbersDefault() {
		assertThatThrownBy(() -> parser.parse(new StringReader(NON_NUMERIC_NUMBERS_TEST_STRING), ""))
				.isInstanceOf(RDFParseException.class)
				.hasMessageContaining("Could not parse JSONLD");
	}

	@Test
	public void testAllowNumericLeadingZeroesDefault() {
		assertThatThrownBy(() -> parser.parse(new StringReader(NUMERIC_LEADING_ZEROES_TEST_STRING), ""))
				.isInstanceOf(RDFParseException.class)
				.hasMessageContaining("Could not parse JSONLD");
	}

	@Test
	public void testAllowSingleQuotesDefault() {
		assertThatThrownBy(() -> parser.parse(new StringReader(SINGLE_QUOTES_TEST_STRING), ""))
				.isInstanceOf(RDFParseException.class)
				.hasMessageContaining("Could not parse JSONLD");
	}

	@Test
	public void testAllowUnquotedControlCharactersDefault() {
		assertThatThrownBy(() -> parser.parse(new StringReader(UNQUOTED_CONTROL_CHARS_TEST_STRING), ""))
				.isInstanceOf(RDFParseException.class)
				.hasMessageContaining("Could not parse JSONLD");
	}

	@Test
	public void testAllowUnquotedFieldNamesDefault() {
		assertThatThrownBy(() -> parser.parse(new StringReader(UNQUOTED_FIELD_NAMES_TEST_STRING), ""))
				.isInstanceOf(RDFParseException.class)
				.hasMessageContaining("Could not parse JSONLD");
	}

	@Test
	public void testAllowYamlCommentsDefault() {
		assertThatThrownBy(() -> parser.parse(new StringReader(YAML_COMMENTS_TEST_STRING), ""))
				.isInstanceOf(RDFParseException.class)
				.hasMessageContaining("Could not parse JSONLD");
	}

	@Test
	public void testAllowTrailingCommaDefault() {
		assertThatThrownBy(() -> parser.parse(new StringReader(TRAILING_COMMA_TEST_STRING), ""))
				.isInstanceOf(RDFParseException.class)
				.hasMessageContaining("Could not parse JSONLD");
	}

	@Test
	public void testStrictDuplicateDetectionDefault() throws Exception {
		parser.parse(new StringReader(STRICT_DUPLICATE_DETECTION_TEST_STRING), "");
		verifyParseResults(testSubjectIRI, testPredicate, testObjectIRI);
	}

	@Test
	public void testContext() throws Exception {

		Document jsonDocument = JsonDocument.of(new StringReader(LOADER_CONTEXT));
		jsonDocument.setDocumentUrl(URI.create("http://example.com/context.jsonld"));

		parser.getParserConfig().set(JSONLDSettings.EXPAND_CONTEXT, jsonDocument);
		parser.parse(new StringReader(LOADER_JSONLD), "");
		assertTrue(model.predicates().contains(testPredicate));
	}

	@Test
	public void testLocalFileSecurity() throws Exception {

		String contextUri = JSONLDParserCustomOldTest.class.getClassLoader()
				.getResource("testcases/jsonld/localFileContext/context.jsonld")
				.toString();

		String jsonld = FileUtils
				.readFileToString(new File(JSONLDParserCustomOldTest.class.getClassLoader()
						.getResource("testcases/jsonld/localFileContext/data.jsonld")
						.getFile()), StandardCharsets.UTF_8)
				.replace("file:./context.jsonld", contextUri);

		// expect exception
		RDFParseException rdfParseException = Assertions.assertThrowsExactly(RDFParseException.class, () -> {
			parser.parse(new StringReader(jsonld), "");
		});

		Assertions.assertEquals("Could not load document from " + contextUri
				+ " because it is not whitelisted. See: JSONLDSettings.WHITELIST and JSONLDSettings.SECURE_MODE which can also be set as system properties.",
				rdfParseException.getMessage());
	}

	@Test
	public void testLocalFileSecurityWhiteList() throws Exception {
		String jsonld = FileUtils.readFileToString(new File(JSONLDParserCustomOldTest.class.getClassLoader()
				.getResource("testcases/jsonld/localFileContext/data.jsonld")
				.getFile()), StandardCharsets.UTF_8);
		String contextUri = JSONLDParserCustomOldTest.class.getClassLoader()
				.getResource("testcases/jsonld/localFileContext/context.jsonld")
				.toString();
		jsonld = jsonld.replace("file:./context.jsonld", contextUri);

		parser.getParserConfig().set(WHITELIST, Set.of(contextUri));

		parser.parse(new StringReader(jsonld), "");
		assertTrue(model.objects().contains(FOAF.PERSON));
	}

	@Test
	public void testLocalFileSecurityDisableSecurity() throws Exception {
		String jsonld = FileUtils.readFileToString(new File(JSONLDParserCustomOldTest.class.getClassLoader()
				.getResource("testcases/jsonld/localFileContext/data.jsonld")
				.getFile()), StandardCharsets.UTF_8);
		jsonld = jsonld.replace("file:./context.jsonld",
				JSONLDParserCustomOldTest.class.getClassLoader()
						.getResource("testcases/jsonld/localFileContext/context.jsonld")
						.toString());

		parser.getParserConfig().set(SECURE_MODE, false);

		parser.parse(new StringReader(jsonld), "");
		assertTrue(model.objects().contains(FOAF.PERSON));
	}

	@Test
	public void testLocalFileSecurityDisableSecuritySystemProperty() throws Exception {
		String jsonld = FileUtils.readFileToString(new File(JSONLDParserCustomOldTest.class.getClassLoader()
				.getResource("testcases/jsonld/localFileContext/data.jsonld")
				.getFile()), StandardCharsets.UTF_8);
		jsonld = jsonld.replace("file:./context.jsonld",
				JSONLDParserCustomOldTest.class.getClassLoader()
						.getResource("testcases/jsonld/localFileContext/context.jsonld")
						.toString());

		try {
			System.setProperty(SECURE_MODE.getKey(), "false");
			parser.parse(new StringReader(jsonld), "");
			assertTrue(model.objects().contains(FOAF.PERSON));
		} finally {
			System.clearProperty(SECURE_MODE.getKey());
		}

	}

	@RepeatedTest(10)
	public void testRemoteContext() throws Exception {
		String jsonld = FileUtils.readFileToString(new File(JSONLDParserCustomOldTest.class.getClassLoader()
				.getResource("testcases/jsonld/remoteContext/data.jsonld")
				.getFile()), StandardCharsets.UTF_8);

		parser.getParserConfig().set(WHITELIST, Set.of("https://schema.org"));
		parser.parse(new StringReader(jsonld), "");
		assertEquals(59, model.size());
	}

	@Test
	public void testRemoteContextSystemProperty() throws Exception {
		String jsonld = FileUtils.readFileToString(new File(JSONLDParserCustomOldTest.class.getClassLoader()
				.getResource("testcases/jsonld/remoteContext/data.jsonld")
				.getFile()), StandardCharsets.UTF_8);

		try {
			System.setProperty(WHITELIST.getKey(), "[\"https://schema.org\"]");
			parser.parse(new StringReader(jsonld), "");
			assertEquals(59, model.size());
		} finally {
			System.clearProperty(WHITELIST.getKey());
		}

	}

	@Test
	public void testRemoteContextException() throws Exception {
		String jsonld = FileUtils.readFileToString(new File(JSONLDParserCustomOldTest.class.getClassLoader()
				.getResource("testcases/jsonld/remoteContextException/data.jsonld")
				.getFile()), StandardCharsets.UTF_8);

		parser.getParserConfig().set(WHITELIST, Set.of("https://example.org/context.jsonld"));
		RDFParseException rdfParseException = Assertions.assertThrowsExactly(RDFParseException.class, () -> {
			parser.parse(new StringReader(jsonld), "");
		});

		assertEquals("Could not load document from https://example.org/context.jsonld", rdfParseException.getMessage());
	}

	@Test
	public void testSPI() {
		ServiceLoader<JsonProvider> load = ServiceLoader.load(JsonProvider.class);
		List<String> collect = load.stream()
				.map(ServiceLoader.Provider::get)
				.map(t -> t.getClass().getName())
				.collect(Collectors.toList());
		assertFalse(collect.isEmpty());
		assertEquals("org.glassfish.json.JsonProviderImpl", collect.stream().findFirst().orElse(""));
	}

}
