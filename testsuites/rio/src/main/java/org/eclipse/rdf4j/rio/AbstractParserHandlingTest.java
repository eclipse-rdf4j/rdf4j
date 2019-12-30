/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract tests to confirm consistent behaviour for the datatype and language handling settings.
 * 
 * @author Peter Ansell
 */
public abstract class AbstractParserHandlingTest {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * Base URI for test parsing.
	 */
	private static final String BASE_URI = "urn:test:base:";

	/**
	 * Test value used for testing unknown datatype value handling.
	 */
	private static final String UNKNOWN_DATATYPE_VALUE = "test unknown datatype literal value";

	/**
	 * Test URI used for testing unknown datatype support.
	 */
	private static final IRI UNKNOWN_DATATYPE_URI = SimpleValueFactory.getInstance()
			.createIRI("urn:test:unknowndatatype");

	/**
	 * Test value used for testing unknown datatype value handling.
	 */
	private static final String KNOWN_DATATYPE_VALUE = "31415926";

	/**
	 * Test URI used for testing known datatype support.
	 * <p>
	 * This may be anything, but it must match with the given {@link DatatypeHandler}.
	 */
	private static final IRI KNOWN_DATATYPE_URI = XMLSchema.INTEGER;

	/**
	 * Test value used for testing unknown language support.
	 */
	private static final String UNKNOWN_LANGUAGE_VALUE = "xsdfsawreaewraew";

	/**
	 * Test Language tag used for testing unknown language support.
	 */
	private static final String UNKNOWN_LANGUAGE_TAG = "fakelanguage123";

	/**
	 * Test value used for testing known language value handling.
	 */
	private static final String KNOWN_LANGUAGE_VALUE = "G'day mate";

	/**
	 * Test Language tag used for testing known language support.
	 */
	private static final String KNOWN_LANGUAGE_TAG = "en-AU";

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	private RDFParser testParser;

	private ParseErrorCollector testListener;

	private Model testStatements;

	/**
	 * Returns an {@link InputStream} containing the given RDF statements in a format that is recognised by the
	 * RDFParser returned by {@link #getParser()}.
	 * 
	 * @param unknownDatatypeStatements A {@link Model} containing statements which all contain unknown datatypes.
	 * @return An InputStream based on the given parameters.
	 */
	protected InputStream getUnknownDatatypeStream(Model model) throws Exception {
		return serialize(model);
	}

	/**
	 * Returns an {@link InputStream} containing the given RDF statements in a format that is recognised by the
	 * RDFParser returned by {@link #getParser()}.
	 * 
	 * @param knownDatatypeStatements A {@link Model} containing statements which all contain known datatypes.
	 * @return An InputStream based on the given parameters.
	 */
	protected InputStream getKnownDatatypeStream(Model model) throws Exception {
		return serialize(model);
	}

	/**
	 * Returns an {@link InputStream} containing the given RDF statements in a format that is recognised by the
	 * RDFParser returned by {@link #getParser()}.
	 * 
	 * @param unknownLanguageStatements A {@link Model} containing statements which all contain unknown language tags.
	 * @return An InputStream based on the given parameters.
	 */
	protected InputStream getUnknownLanguageStream(Model model) throws Exception {
		return serialize(model);
	}

	/**
	 * Returns an {@link InputStream} containing the given RDF statements in a format that is recognised by the
	 * RDFParser returned by {@link #getParser()}.
	 * 
	 * @param knownLanguageStatements A {@link Model} containing statements which all contain known language tags.
	 * @return An InputStream based on the given parameters.
	 */
	protected InputStream getKnownLanguageStream(Model model) throws Exception {
		return serialize(model);
	}

	/**
	 * Concrete test classes can override this to return a new instance of the RDFParser that is being tested.
	 * 
	 * @return A new instance of the RDFParser that is being tested.
	 */
	protected abstract RDFParser getParser();

	/**
	 * Helper method to write the given model to and return an InputStream containing the results.
	 *
	 * @param statements
	 * @return An {@link InputStream} containing the results.
	 * @throws RDFHandlerException
	 */
	private InputStream serialize(Model statements) throws RDFHandlerException {
		ByteArrayOutputStream output = new ByteArrayOutputStream(8096);

		RDFWriter writer = createWriter(output);
		writer.startRDF();
		for (Statement nextStatement : statements) {
			writer.handleStatement(nextStatement);
		}
		writer.endRDF();

		return new ByteArrayInputStream(output.toByteArray());
	}

	/**
	 * Creates an {@link RDFWriter} that is capable of producing an InputStream that can be parsed by
	 * {@link #getParser()}.
	 *
	 * @since 2.3
	 */
	protected RDFWriter createWriter(OutputStream output) {
		throw new IllegalStateException("Subclasses must implement createWriter(OutputStream)");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		testParser = getParser();

		testParser.setValueFactory(vf);
		testListener = new ParseErrorCollector();
		testStatements = new LinkedHashModel();

		testParser.setParseErrorListener(testListener);
		testParser.setRDFHandler(new StatementCollector(testStatements));
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		testListener.reset();
		testListener = null;
		testStatements.clear();
		testStatements = null;

		testParser = null;
	}

	/**
	 * Tests whether an unknown datatype with the default settings will both generate no message and not fail.
	 */
	@Test
	public final void testUnknownDatatypeNoMessageNoFailCase1() throws Exception {
		Model expectedModel = getTestModel(UNKNOWN_DATATYPE_VALUE, UNKNOWN_DATATYPE_URI);
		InputStream input = getUnknownDatatypeStream(expectedModel);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an unknown datatype with the default settings (using {@link ParserConfig#useDefaults()}) will both
	 * generate no message and not fail.
	 */
	@Test
	public final void testUnknownDatatypeNoMessageNoFailCase2() throws Exception {
		Model expectedModel = getTestModel(UNKNOWN_DATATYPE_VALUE, UNKNOWN_DATATYPE_URI);
		InputStream input = getUnknownDatatypeStream(expectedModel);

		testParser.getParserConfig().useDefaults();

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an unknown datatype with the correct settings will both generate no message and not fail.
	 */
	@Test
	public final void testUnknownDatatypeNoMessageNoFailCase3() throws Exception {
		Model expectedModel = getTestModel(UNKNOWN_DATATYPE_VALUE, UNKNOWN_DATATYPE_URI);
		InputStream input = getUnknownDatatypeStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, false);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an unknown datatype with the correct settings will both generate no message and not fail when
	 * addNonFatalError is called with the given setting.
	 */
	@Test
	public final void testUnknownDatatypeNoMessageNoFailCase4() throws Exception {
		Model expectedModel = getTestModel(UNKNOWN_DATATYPE_VALUE, UNKNOWN_DATATYPE_URI);
		InputStream input = getUnknownDatatypeStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, false);
		testParser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an unknown datatype with the correct settings will both generate no message and not fail when
	 * setNonFatalError is called with an empty set to reset the fatal errors
	 */
	@Test
	public final void testUnknownDatatypeNoMessageNoFailCase5() throws Exception {
		Model expectedModel = getTestModel(UNKNOWN_DATATYPE_VALUE, UNKNOWN_DATATYPE_URI);
		InputStream input = getUnknownDatatypeStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, false);
		testParser.getParserConfig().setNonFatalErrors(new HashSet<>());

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an unknown datatype with the message no fail.
	 */
	@Test
	public final void testUnknownDatatypeWithMessageNoFailCase1() throws Exception {
		Model expectedModel = getTestModel(UNKNOWN_DATATYPE_VALUE, UNKNOWN_DATATYPE_URI);
		InputStream input = getUnknownDatatypeStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);
		testParser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 1, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an unknown datatype with the message no fail.
	 */
	@Test
	public final void testUnknownDatatypeWithMessageNoFailCase2() throws Exception {
		Model expectedModel = getTestModel(UNKNOWN_DATATYPE_VALUE, UNKNOWN_DATATYPE_URI);
		InputStream input = getUnknownDatatypeStream(expectedModel);

		testParser.getParserConfig().useDefaults();
		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);
		testParser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 1, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an unknown datatype with the message no fail.
	 */
	@Test
	public final void testUnknownDatatypeWithMessageNoFailCase3() throws Exception {
		Model expectedModel = getTestModel(UNKNOWN_DATATYPE_VALUE, UNKNOWN_DATATYPE_URI);
		InputStream input = getUnknownDatatypeStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);
		testParser.getParserConfig()
				.setNonFatalErrors(Collections.<RioSetting<?>>singleton(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES));

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 1, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an unknown datatype with the message and with a failure.
	 */
	@Test
	public final void testUnknownDatatypeWithMessageWithFailCase1() throws Exception {
		Model expectedModel = getTestModel(UNKNOWN_DATATYPE_VALUE, UNKNOWN_DATATYPE_URI);
		InputStream input = getUnknownDatatypeStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);

		try {
			testParser.parse(input, BASE_URI);
			fail("Did not receive expected exception");
		} catch (RDFParseException e) {
			// expected
		}

		assertErrorListener(0, 1, 0);
		assertModel(new LinkedHashModel());
	}

	/**
	 * Tests whether an known datatype with the default settings will both generate no message and not fail.
	 */
	@Test
	public final void testKnownDatatypeNoMessageNoFailCase1() throws Exception {
		Model expectedModel = getTestModel(KNOWN_DATATYPE_VALUE, KNOWN_DATATYPE_URI);
		InputStream input = getKnownDatatypeStream(expectedModel);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an known datatype with the default settings (using {@link ParserConfig#useDefaults()}) will both
	 * generate no message and not fail.
	 */
	@Test
	public final void testKnownDatatypeNoMessageNoFailCase2() throws Exception {
		Model expectedModel = getTestModel(KNOWN_DATATYPE_VALUE, KNOWN_DATATYPE_URI);
		InputStream input = getKnownDatatypeStream(expectedModel);

		testParser.getParserConfig().useDefaults();

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an known datatype with the correct settings will both generate no message and not fail.
	 */
	@Test
	public final void testKnownDatatypeNoMessageNoFailCase3() throws Exception {
		Model expectedModel = getTestModel(KNOWN_DATATYPE_VALUE, KNOWN_DATATYPE_URI);
		InputStream input = getKnownDatatypeStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, false);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an known datatype with the correct settings will both generate no message and not fail when
	 * addNonFatalError is called with the given setting.
	 */
	@Test
	public final void testKnownDatatypeNoMessageNoFailCase4() throws Exception {
		Model expectedModel = getTestModel(KNOWN_DATATYPE_VALUE, KNOWN_DATATYPE_URI);
		InputStream input = getKnownDatatypeStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, false);
		testParser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an known datatype with the correct settings will both generate no message and not fail when
	 * setNonFatalError is called with an empty set to reset the fatal errors
	 */
	@Test
	public final void testKnownDatatypeNoMessageNoFailCase5() throws Exception {
		Model expectedModel = getTestModel(KNOWN_DATATYPE_VALUE, KNOWN_DATATYPE_URI);
		InputStream input = getKnownDatatypeStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, false);
		testParser.getParserConfig().setNonFatalErrors(new HashSet<>());

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an known datatype with the message no fail.
	 */
	@Test
	public final void testKnownDatatypeWithMessageNoFailCase1() throws Exception {
		Model expectedModel = getTestModel(KNOWN_DATATYPE_VALUE, KNOWN_DATATYPE_URI);
		InputStream input = getKnownDatatypeStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);
		testParser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an known datatype with the message no fail.
	 */
	@Test
	public final void testKnownDatatypeWithMessageNoFailCase2() throws Exception {
		Model expectedModel = getTestModel(KNOWN_DATATYPE_VALUE, KNOWN_DATATYPE_URI);
		InputStream input = getKnownDatatypeStream(expectedModel);

		testParser.getParserConfig().useDefaults();
		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);
		testParser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an known datatype with the message no fail.
	 */
	@Test
	public final void testKnownDatatypeWithMessageNoFailCase3() throws Exception {
		Model expectedModel = getTestModel(KNOWN_DATATYPE_VALUE, KNOWN_DATATYPE_URI);
		InputStream input = getKnownDatatypeStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);
		testParser.getParserConfig()
				.setNonFatalErrors(Collections.<RioSetting<?>>singleton(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES));

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an known datatype with the message which generates a failure if the datatype is unknown.
	 */
	@Test
	public final void testKnownDatatypeWithMessageWhereUnknownWouldFailCase1() throws Exception {
		Model expectedModel = getTestModel(KNOWN_DATATYPE_VALUE, KNOWN_DATATYPE_URI);
		InputStream input = getKnownDatatypeStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an unknown language with the default settings will both generate no message and not fail.
	 */
	@Test
	public final void testUnknownLanguageNoMessageNoFailCase1() throws Exception {
		Model expectedModel = getTestModel(UNKNOWN_LANGUAGE_VALUE, UNKNOWN_LANGUAGE_TAG);
		InputStream input = getUnknownLanguageStream(expectedModel);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an unknown language with the default settings (using {@link ParserConfig#useDefaults()}) will both
	 * generate no message and not fail.
	 */
	@Test
	public final void testUnknownLanguageNoMessageNoFailCase2() throws Exception {
		Model expectedModel = getTestModel(UNKNOWN_LANGUAGE_VALUE, UNKNOWN_LANGUAGE_TAG);
		InputStream input = getUnknownLanguageStream(expectedModel);

		testParser.getParserConfig().useDefaults();

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an unknown language with the correct settings will both generate no message and not fail.
	 */
	@Test
	public final void testUnknownLanguageNoMessageNoFailCase3() throws Exception {
		Model expectedModel = getTestModel(UNKNOWN_LANGUAGE_VALUE, UNKNOWN_LANGUAGE_TAG);
		InputStream input = getUnknownLanguageStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, false);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an unknown language with the correct settings will both generate no message and not fail when
	 * addNonFatalError is called with the given setting.
	 */
	@Test
	public final void testUnknownLanguageNoMessageNoFailCase4() throws Exception {
		Model expectedModel = getTestModel(UNKNOWN_LANGUAGE_VALUE, UNKNOWN_LANGUAGE_TAG);
		InputStream input = getUnknownLanguageStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, false);
		testParser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an unknown language with the correct settings will both generate no message and not fail when
	 * setNonFatalError is called with an empty set to reset the fatal errors
	 */
	@Test
	public final void testUnknownLanguageNoMessageNoFailCase5() throws Exception {
		Model expectedModel = getTestModel(UNKNOWN_LANGUAGE_VALUE, UNKNOWN_LANGUAGE_TAG);
		InputStream input = getUnknownLanguageStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, false);
		testParser.getParserConfig().setNonFatalErrors(new HashSet<>());

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an unknown language with the message no fail.
	 */
	@Test
	public final void testUnknownLanguageWithMessageNoFailCase1() throws Exception {
		Model expectedModel = getTestModel(UNKNOWN_LANGUAGE_VALUE, UNKNOWN_LANGUAGE_TAG);
		InputStream input = getUnknownLanguageStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);
		testParser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 1, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an unknown language with the message no fail.
	 */
	@Test
	public final void testUnknownLanguageWithMessageNoFailCase2() throws Exception {
		Model expectedModel = getTestModel(UNKNOWN_LANGUAGE_VALUE, UNKNOWN_LANGUAGE_TAG);
		InputStream input = getUnknownLanguageStream(expectedModel);

		testParser.getParserConfig().useDefaults();
		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);
		testParser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 1, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an unknown language with the message no fail.
	 */
	@Test
	public final void testUnknownLanguageWithMessageNoFailCase3() throws Exception {
		Model expectedModel = getTestModel(UNKNOWN_LANGUAGE_VALUE, UNKNOWN_LANGUAGE_TAG);
		InputStream input = getUnknownLanguageStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);
		testParser.getParserConfig()
				.setNonFatalErrors(Collections.<RioSetting<?>>singleton(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES));

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 1, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an unknown language with the message and with a failure.
	 */
	@Test
	public final void testUnknownLanguageWithMessageWithFailCase1() throws Exception {
		Model expectedModel = getTestModel(UNKNOWN_LANGUAGE_VALUE, UNKNOWN_LANGUAGE_TAG);
		InputStream input = getUnknownLanguageStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);

		try {
			testParser.parse(input, BASE_URI);
			fail("Did not receive expected exception");
		} catch (RDFParseException e) {
			// expected
		}

		assertErrorListener(0, 1, 0);
		assertModel(new LinkedHashModel());
	}

	/**
	 * Tests whether an known language with the default settings will both generate no message and not fail.
	 */
	@Test
	public final void testKnownLanguageNoMessageNoFailCase1() throws Exception {
		Model expectedModel = getTestModel(KNOWN_LANGUAGE_VALUE, KNOWN_LANGUAGE_TAG);
		InputStream input = getKnownLanguageStream(expectedModel);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an known language with the default settings (using {@link ParserConfig#useDefaults()}) will both
	 * generate no message and not fail.
	 */
	@Test
	public final void testKnownLanguageNoMessageNoFailCase2() throws Exception {
		Model expectedModel = getTestModel(KNOWN_LANGUAGE_VALUE, KNOWN_LANGUAGE_TAG);
		InputStream input = getKnownLanguageStream(expectedModel);

		testParser.getParserConfig().useDefaults();

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an known language with the correct settings will both generate no message and not fail.
	 */
	@Test
	public final void testKnownLanguageNoMessageNoFailCase3() throws Exception {
		Model expectedModel = getTestModel(KNOWN_LANGUAGE_VALUE, KNOWN_LANGUAGE_TAG);
		InputStream input = getKnownLanguageStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, false);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an known language with the correct settings will both generate no message and not fail when
	 * addNonFatalError is called with the given setting.
	 */
	@Test
	public final void testKnownLanguageNoMessageNoFailCase4() throws Exception {
		Model expectedModel = getTestModel(KNOWN_LANGUAGE_VALUE, KNOWN_LANGUAGE_TAG);
		InputStream input = getKnownLanguageStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, false);
		testParser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an known language with the correct settings will both generate no message and not fail when
	 * setNonFatalError is called with an empty set to reset the fatal errors
	 */
	@Test
	public final void testKnownLanguageNoMessageNoFailCase5() throws Exception {
		Model expectedModel = getTestModel(KNOWN_LANGUAGE_VALUE, KNOWN_LANGUAGE_TAG);
		InputStream input = getKnownLanguageStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, false);
		testParser.getParserConfig().setNonFatalErrors(new HashSet<>());

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an known language with the message no fail.
	 */
	@Test
	public final void testKnownLanguageWithMessageNoFailCase1() throws Exception {
		Model expectedModel = getTestModel(KNOWN_LANGUAGE_VALUE, KNOWN_LANGUAGE_TAG);
		InputStream input = getKnownLanguageStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);
		testParser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an known language with the message no fail.
	 */
	@Test
	public final void testKnownLanguageWithMessageNoFailCase2() throws Exception {
		Model expectedModel = getTestModel(KNOWN_LANGUAGE_VALUE, KNOWN_LANGUAGE_TAG);
		InputStream input = getKnownLanguageStream(expectedModel);

		testParser.getParserConfig().useDefaults();
		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);
		testParser.getParserConfig().addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an known language with the message no fail.
	 */
	@Test
	public final void testKnownLanguageWithMessageNoFailCase3() throws Exception {
		Model expectedModel = getTestModel(KNOWN_LANGUAGE_VALUE, KNOWN_LANGUAGE_TAG);
		InputStream input = getKnownLanguageStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);
		testParser.getParserConfig()
				.setNonFatalErrors(Collections.<RioSetting<?>>singleton(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES));

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an known language with the message which generates a failure if the language is unknown.
	 */
	@Test
	public final void testKnownLanguageWithMessageWhereUnknownWouldFailCase1() throws Exception {
		Model expectedModel = getTestModel(KNOWN_LANGUAGE_VALUE, KNOWN_LANGUAGE_TAG);
		InputStream input = getKnownLanguageStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an known language with the message which generates a failure if the uppercased version of the
	 * language is unknown.
	 */
	@Test
	public final void testKnownLanguageWithMessageWhereUnknownWouldFailCase2() throws Exception {
		Model expectedModel = getTestModel(KNOWN_LANGUAGE_VALUE, KNOWN_LANGUAGE_TAG.toUpperCase(Locale.ENGLISH));
		InputStream input = getKnownLanguageStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	/**
	 * Tests whether an known language with the message which generates a failure if the lowercased version of the
	 * language is unknown.
	 */
	@Test
	public final void testKnownLanguageWithMessageWhereUnknownWouldFailCase3() throws Exception {
		Model expectedModel = getTestModel(KNOWN_LANGUAGE_VALUE, KNOWN_LANGUAGE_TAG.toLowerCase(Locale.ENGLISH));
		InputStream input = getKnownLanguageStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel);
	}

	@Test
	public final void testSkolemization() throws Exception {
		Model expectedModel = new LinkedHashModel();
		BNode subj = vf.createBNode();
		expectedModel
				.add(vf.createStatement(subj, RDF.VALUE, vf.createLiteral(KNOWN_DATATYPE_VALUE, KNOWN_DATATYPE_URI)));
		expectedModel
				.add(vf.createStatement(subj, RDF.VALUE, vf.createLiteral(KNOWN_LANGUAGE_VALUE, KNOWN_LANGUAGE_TAG)));
		InputStream input = getKnownDatatypeStream(expectedModel);

		testParser.getParserConfig().set(BasicParserSettings.SKOLEMIZE_ORIGIN, "http://example.com");

		testParser.parse(input, BASE_URI);

		assertErrorListener(0, 0, 0);
		assertModel(expectedModel); // isomorphic
		assertNotEquals(new HashSet<>(expectedModel), new HashSet<>(testStatements)); // blank nodes not preserved
		assertTrue(Models.subjectBNodes(testStatements).isEmpty()); // skolemized
	}

	private void assertModel(Model expectedModel) {
		if (logger.isTraceEnabled()) {
			logger.trace("Expected: {}", expectedModel);
			logger.trace("Actual: {}", testStatements);
		}
		assertTrue("Did not find expected statements", Models.isomorphic(expectedModel, testStatements));
	}

	private void assertErrorListener(int expectedWarnings, int expectedErrors, int expectedFatalErrors) {
		assertEquals("Unexpected number of fatal errors", expectedFatalErrors, testListener.getFatalErrors().size());
		assertEquals("Unexpected number of errors", expectedErrors, testListener.getErrors().size());
		assertEquals("Unexpected number of warnings", expectedWarnings, testListener.getWarnings().size());
	}

	private final Model getTestModel(String datatypeValue, IRI datatypeURI) {
		Model result = new LinkedHashModel();
		result.add(vf.createStatement(vf.createBNode(), DC.DESCRIPTION, vf.createLiteral(datatypeValue, datatypeURI)));
		return result;
	}

	private final Model getTestModel(String languageValue, String languageTag) {
		Model result = new LinkedHashModel();
		result.add(vf.createStatement(vf.createBNode(), RDFS.COMMENT, vf.createLiteral(languageValue, languageTag)));
		return result;
	}
}
