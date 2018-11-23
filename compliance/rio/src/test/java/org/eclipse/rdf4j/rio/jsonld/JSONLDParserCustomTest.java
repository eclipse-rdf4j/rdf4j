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
import java.util.Collection;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.ParseErrorListener;
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

	private RDFParser parser;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private ParseErrorCollector errors;

	private Model model;

	private final IRI testSubjectIRI = SimpleValueFactory.getInstance().createIRI("http://example.com/Subj1");

	private final IRI testPredicate = SimpleValueFactory.getInstance().createIRI("http://example.com/prop1");

	private final IRI testObjectIRI = SimpleValueFactory.getInstance().createIRI("http://example.com/Obj1");

	private final Literal testObjectLiteralNotANumber = SimpleValueFactory.getInstance().createLiteral("NaN",
			XMLSchema.DOUBLE);

	private final Literal testObjectLiteralNumber = SimpleValueFactory.getInstance().createLiteral("42",
			XMLSchema.INTEGER);

	@Before
	public void setUp()
		throws Exception
	{
		parser = Rio.createParser(RDFFormat.JSONLD);
		errors = new ParseErrorCollector();
		model = new LinkedHashModel();
		parser.setParseErrorListener(errors);
		parser.setRDFHandler(new ContextStatementCollector(model, SimpleValueFactory.getInstance()));
	}

	private void verifyParseResults(Resource nextSubject, IRI nextPredicate, Value nextObject)
		throws Exception
	{
		assertEquals(0, errors.getWarnings().size());
		assertEquals(0, errors.getErrors().size());
		assertEquals(0, errors.getFatalErrors().size());

		assertEquals(1, model.size());
		assertTrue("model was not as expected: " + model.toString(),
				model.contains(nextSubject, nextPredicate, nextObject));
	}

	@Test
	public void testSupportedSettings()
		throws Exception
	{
		// 10 supported in JSONLDParser + 12 from AbstractRDFParser
		assertEquals(22, parser.getSupportedSettings().size());
	}

	@Test
	public void testAllowBackslashEscapingAnyCharacterDefault()
		throws Exception
	{
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Could not parse JSONLD");
		parser.parse(new StringReader(BACKSLASH_ESCAPED_TEST_STRING), "");
	}

	@Test
	public void testAllowBackslashEscapingAnyCharacterEnabled()
		throws Exception
	{
		parser.set(JSONSettings.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
		parser.parse(new StringReader(BACKSLASH_ESCAPED_TEST_STRING), "");
		verifyParseResults(testSubjectIRI, testPredicate, testObjectIRI);
	}

	@Test
	public void testAllowBackslashEscapingAnyCharacterDisabled()
		throws Exception
	{
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Could not parse JSONLD");
		parser.set(JSONSettings.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, false);
		parser.parse(new StringReader(BACKSLASH_ESCAPED_TEST_STRING), "");
	}

	@Test
	public void testAllowCommentsDefault()
		throws Exception
	{
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Could not parse JSONLD");
		parser.parse(new StringReader(COMMENTS_TEST_STRING), "");
	}

	@Test
	public void testAllowCommentsEnabled()
		throws Exception
	{
		parser.set(JSONSettings.ALLOW_COMMENTS, true);
		parser.parse(new StringReader(COMMENTS_TEST_STRING), "");
		verifyParseResults(testSubjectIRI, testPredicate, testObjectIRI);
	}

	@Test
	public void testAllowCommentsDisabled()
		throws Exception
	{
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Could not parse JSONLD");
		parser.set(JSONSettings.ALLOW_COMMENTS, false);
		parser.parse(new StringReader(COMMENTS_TEST_STRING), "");
	}

	@Test
	public void testAllowNonNumericNumbersDefault()
		throws Exception
	{
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Could not parse JSONLD");
		parser.parse(new StringReader(NON_NUMERIC_NUMBERS_TEST_STRING), "");
	}

	@Test
	public void testAllowNonNumericNumbersEnabled()
		throws Exception
	{
		parser.set(JSONSettings.ALLOW_NON_NUMERIC_NUMBERS, true);
		parser.parse(new StringReader(NON_NUMERIC_NUMBERS_TEST_STRING), "");
		// FIXME: The literal being created has the replacement character as its label,
		// indicating it is failing somewhere in the pipeline
		verifyParseResults(testSubjectIRI, testPredicate, testObjectLiteralNotANumber);
	}

	@Test
	public void testAllowNonNumericNumbersDisabled()
		throws Exception
	{
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Could not parse JSONLD");
		parser.set(JSONSettings.ALLOW_NON_NUMERIC_NUMBERS, false);
		parser.parse(new StringReader(NON_NUMERIC_NUMBERS_TEST_STRING), "");
	}

	@Test
	public void testAllowNumericLeadingZeroesDefault()
		throws Exception
	{
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Could not parse JSONLD");
		parser.parse(new StringReader(NUMERIC_LEADING_ZEROES_TEST_STRING), "");
	}

	@Test
	public void testAllowNumericLeadingZeroesEnabled()
		throws Exception
	{
		parser.set(JSONSettings.ALLOW_NUMERIC_LEADING_ZEROS, true);
		parser.parse(new StringReader(NUMERIC_LEADING_ZEROES_TEST_STRING), "");
		verifyParseResults(testSubjectIRI, testPredicate, testObjectLiteralNumber);
	}

	@Test
	public void testAllowNumericLeadingZeroesDisabled()
		throws Exception
	{
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Could not parse JSONLD");
		parser.set(JSONSettings.ALLOW_NUMERIC_LEADING_ZEROS, false);
		parser.parse(new StringReader(NUMERIC_LEADING_ZEROES_TEST_STRING), "");
	}

	@Test
	public void testAllowSingleQuotesDefault()
		throws Exception
	{
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Could not parse JSONLD");
		parser.parse(new StringReader(SINGLE_QUOTES_TEST_STRING), "");
	}

	@Test
	public void testAllowSingleQuotesEnabled()
		throws Exception
	{
		parser.set(JSONSettings.ALLOW_SINGLE_QUOTES, true);
		parser.parse(new StringReader(SINGLE_QUOTES_TEST_STRING), "");
		verifyParseResults(testSubjectIRI, testPredicate, testObjectLiteralNumber);
	}

	@Test
	public void testAllowSingleQuotesDisabled()
		throws Exception
	{
		thrown.expect(RDFParseException.class);
		thrown.expectMessage("Could not parse JSONLD");
		parser.set(JSONSettings.ALLOW_SINGLE_QUOTES, false);
		parser.parse(new StringReader(SINGLE_QUOTES_TEST_STRING), "");
	}
}
