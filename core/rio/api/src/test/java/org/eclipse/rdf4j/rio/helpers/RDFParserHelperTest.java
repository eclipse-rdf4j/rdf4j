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
package org.eclipse.rdf4j.rio.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashSet;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.DatatypeHandler;
import org.eclipse.rdf4j.rio.LanguageHandler;
import org.eclipse.rdf4j.rio.ParseErrorListener;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for {@link RDFParserHelper} methods.
 *
 * @author Peter Ansell
 */
public class RDFParserHelperTest {

	private static final String TEST_MESSAGE_FOR_FAILURE = "Test message for failure.";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private static final String LABEL_TESTA = "test-a";

	private static final String LANG_EN = "en";

	private ParserConfig parserConfig;

	private ParseErrorCollector errListener;

	private ValueFactory valueFactory;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		parserConfig = new ParserConfig();
		// By default we wipe out the SPI loaded datatype and language handlers
		parserConfig.set(BasicParserSettings.DATATYPE_HANDLERS, Collections.<DatatypeHandler>emptyList());
		parserConfig.set(BasicParserSettings.LANGUAGE_HANDLERS, Collections.<LanguageHandler>emptyList());
		// Ensure that the set of non-fatal errors is empty by default
		parserConfig.setNonFatalErrors(new HashSet<>());
		errListener = new ParseErrorCollector();
		valueFactory = SimpleValueFactory.getInstance();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.helpers.RDFParserHelper#createLiteral(java.lang.String, java.lang.String, org.eclipse.rdf4j.model.URI, org.eclipse.rdf4j.rio.ParserConfig, org.eclipse.rdf4j.rio.ParseErrorListener, org.eclipse.rdf4j.model.ValueFactory)}
	 * .
	 */
	@Test
	public final void testCreateLiteralLabelNull() throws Exception {
		thrown.expect(NullPointerException.class);
		thrown.expectMessage("Cannot create a literal using a null label");
		RDFParserHelper.createLiteral(null, null, null, parserConfig, errListener, valueFactory);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.helpers.RDFParserHelper#createLiteral(java.lang.String, java.lang.String, org.eclipse.rdf4j.model.URI, org.eclipse.rdf4j.rio.ParserConfig, org.eclipse.rdf4j.rio.ParseErrorListener, org.eclipse.rdf4j.model.ValueFactory)}
	 * .
	 */
	@Test
	public final void testCreateLiteralLabelOnly() throws Exception {
		Literal literal = RDFParserHelper.createLiteral(LABEL_TESTA, null, null, parserConfig, errListener,
				valueFactory);

		assertEquals(LABEL_TESTA, literal.getLabel());
		assertFalse(literal.getLanguage().isPresent());
		assertEquals(XSD.STRING, literal.getDatatype());
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.helpers.RDFParserHelper#createLiteral(java.lang.String, java.lang.String, org.eclipse.rdf4j.model.URI, org.eclipse.rdf4j.rio.ParserConfig, org.eclipse.rdf4j.rio.ParseErrorListener, org.eclipse.rdf4j.model.ValueFactory)}
	 * .
	 */
	@Test
	public final void testCreateLiteralLabelAndLanguage() throws Exception {
		Literal literal = RDFParserHelper.createLiteral(LABEL_TESTA, LANG_EN, null, parserConfig, errListener,
				valueFactory);

		assertEquals(LABEL_TESTA, literal.getLabel());
		assertEquals(LANG_EN, literal.getLanguage().orElse(null));
		assertEquals(RDF.LANGSTRING, literal.getDatatype());
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.helpers.RDFParserHelper#createLiteral(java.lang.String, java.lang.String, org.eclipse.rdf4j.model.URI, org.eclipse.rdf4j.rio.ParserConfig, org.eclipse.rdf4j.rio.ParseErrorListener, org.eclipse.rdf4j.model.ValueFactory)}
	 * .
	 */
	@Test
	public final void testCreateLiteralLabelAndDatatype() throws Exception {
		Literal literal = RDFParserHelper.createLiteral(LABEL_TESTA, null, XSD.STRING, parserConfig, errListener,
				valueFactory);

		assertEquals(LABEL_TESTA, literal.getLabel());
		assertFalse(literal.getLanguage().isPresent());
		assertEquals(XSD.STRING, literal.getDatatype());
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.helpers.RDFParserHelper#createLiteral(java.lang.String, java.lang.String, org.eclipse.rdf4j.model.URI, org.eclipse.rdf4j.rio.ParserConfig, org.eclipse.rdf4j.rio.ParseErrorListener, org.eclipse.rdf4j.model.ValueFactory)}
	 * .
	 * <p>
	 * SES-1803 : Temporary decision to ensure RDF-1.0 backwards compatibility for Literals created by this method in
	 * cases where {@link RDF#LANGSTRING} is given and there is a language.
	 */
	@Test
	public final void testCreateLiteralLabelAndLanguageWithRDFLangString() throws Exception {
		Literal literal = RDFParserHelper.createLiteral(LABEL_TESTA, LANG_EN, RDF.LANGSTRING, parserConfig, errListener,
				valueFactory);

		assertEquals(LABEL_TESTA, literal.getLabel());
		assertEquals(LANG_EN, literal.getLanguage().orElse(null));
		assertEquals(RDF.LANGSTRING, literal.getDatatype());
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.helpers.RDFParserHelper#createLiteral(java.lang.String, java.lang.String, org.eclipse.rdf4j.model.URI, org.eclipse.rdf4j.rio.ParserConfig, org.eclipse.rdf4j.rio.ParseErrorListener, org.eclipse.rdf4j.model.ValueFactory)}
	 * .
	 * <p>
	 * SES-1803 : Temporary decision to ensure RDF-1.0 backwards compatibility for Literals created by this method in
	 * cases where {@link RDF#LANGSTRING} is given and there is NO given language.
	 * <p>
	 * SES-2203 : This was inconsistent, so has been changed to verify failure.
	 * <p>
	 * GH-2004 : Changed to handle cases when VERIFY_DATATYPE_VALUES is set to false and {@link RDF#LANGSTRING} is given
	 * and there is NO given language.
	 */
	@Test
	public final void testCreateLiteralLabelNoLanguageWithRDFLangStringWithVerify() throws Exception {
		parserConfig.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
		assertTrue(parserConfig.get(BasicParserSettings.VERIFY_DATATYPE_VALUES));
		thrown.expect(RDFParseException.class);
		RDFParserHelper.createLiteral(LABEL_TESTA, null, RDF.LANGSTRING, parserConfig, errListener, valueFactory);
	}

	@Test
	public final void testCreateLiteralLabelNoLanguageWithRDFLangStringWithNoVerify() throws Exception {
		parserConfig.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, false);
		Literal literal = RDFParserHelper.createLiteral(LABEL_TESTA, null, RDF.LANGSTRING, parserConfig, errListener,
				valueFactory);
		assertFalse(literal.getLanguage().isPresent());
		assertEquals(XSD.STRING, literal.getDatatype());
	}

	@Test
	public final void testReportErrorStringFatalActive() throws Exception {
		parserConfig.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
		assertTrue(parserConfig.get(BasicParserSettings.VERIFY_DATATYPE_VALUES));
		thrown.expect(RDFParseException.class);
		thrown.expectMessage(TEST_MESSAGE_FOR_FAILURE);
		try {
			RDFParserHelper.reportError(TEST_MESSAGE_FOR_FAILURE, BasicParserSettings.VERIFY_DATATYPE_VALUES,
					parserConfig, errListener);
		} finally {
			assertErrorListener(0, 1, 0);
		}
	}

	@Test
	public final void testReportErrorStringNonFatalActive() throws Exception {
		parserConfig.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
		assertTrue(parserConfig.get(BasicParserSettings.VERIFY_DATATYPE_VALUES));
		parserConfig.addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
		RDFParserHelper.reportError(TEST_MESSAGE_FOR_FAILURE, BasicParserSettings.VERIFY_DATATYPE_VALUES, parserConfig,
				errListener);
		assertErrorListener(0, 1, 0);
	}

	@Test
	public final void testReportErrorStringFatalInactive() throws Exception {
		assertFalse(parserConfig.get(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES));
		RDFParserHelper.reportError(TEST_MESSAGE_FOR_FAILURE, BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES,
				parserConfig, errListener);
		assertErrorListener(0, 0, 0);
	}

	@Test
	public final void testReportErrorStringNonFatalInactive() throws Exception {
		assertFalse(parserConfig.get(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES));
		parserConfig.addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES);
		RDFParserHelper.reportError(TEST_MESSAGE_FOR_FAILURE, BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES,
				parserConfig, errListener);
		assertErrorListener(0, 0, 0);
	}

	@Test
	public final void testReportErrorStringIntIntFatalActive() throws Exception {
		parserConfig.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
		assertTrue(parserConfig.get(BasicParserSettings.VERIFY_DATATYPE_VALUES));
		thrown.expect(RDFParseException.class);
		thrown.expectMessage(TEST_MESSAGE_FOR_FAILURE);
		try {
			RDFParserHelper.reportError(TEST_MESSAGE_FOR_FAILURE, 1, 1, BasicParserSettings.VERIFY_DATATYPE_VALUES,
					parserConfig, errListener);
		} finally {
			assertErrorListener(0, 1, 0);
		}
	}

	@Test
	public final void testReportErrorStringIntIntNonFatalActive() throws Exception {
		parserConfig.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
		assertTrue(parserConfig.get(BasicParserSettings.VERIFY_DATATYPE_VALUES));
		parserConfig.addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
		RDFParserHelper.reportError(TEST_MESSAGE_FOR_FAILURE, 1, 1, BasicParserSettings.VERIFY_DATATYPE_VALUES,
				parserConfig, errListener);
		assertErrorListener(0, 1, 0);
	}

	@Test
	public final void testReportErrorStringIntIntFatalInactive() throws Exception {
		assertFalse(parserConfig.get(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES));
		RDFParserHelper.reportError(TEST_MESSAGE_FOR_FAILURE, 1, 1, BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES,
				parserConfig, errListener);
		assertErrorListener(0, 0, 0);
	}

	@Test
	public final void testReportErrorStringIntIntNonFatalInactive() throws Exception {
		assertFalse(parserConfig.get(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES));
		parserConfig.addNonFatalError(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES);
		RDFParserHelper.reportError(TEST_MESSAGE_FOR_FAILURE, 1, 1, BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES,
				parserConfig, errListener);
		assertErrorListener(0, 0, 0);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.helpers.RDFParserHelper#reportError(java.lang.Exception, int, int, org.eclipse.rdf4j.rio.RioSetting, org.eclipse.rdf4j.rio.ParserConfig, org.eclipse.rdf4j.rio.ParseErrorListener)}
	 * .
	 */
	@Ignore
	@Test
	public final void testReportErrorExceptionIntInt() throws Exception {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.helpers.RDFParserHelper#reportFatalError(java.lang.String, org.eclipse.rdf4j.rio.ParseErrorListener)}
	 * .
	 */
	@Ignore
	@Test
	public final void testReportFatalErrorString() throws Exception {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.helpers.RDFParserHelper#reportFatalError(java.lang.String, int, int, org.eclipse.rdf4j.rio.ParseErrorListener)}
	 * .
	 */
	@Ignore
	@Test
	public final void testReportFatalErrorStringIntInt() throws Exception {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.helpers.RDFParserHelper#reportFatalError(java.lang.Exception, org.eclipse.rdf4j.rio.ParseErrorListener)}
	 * .
	 */
	@Ignore
	@Test
	public final void testReportFatalErrorException() throws Exception {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.helpers.RDFParserHelper#reportFatalError(java.lang.Exception, int, int, org.eclipse.rdf4j.rio.ParseErrorListener)}
	 * .
	 */
	@Ignore
	@Test
	public final void testReportFatalErrorExceptionIntInt() throws Exception {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Private method for verifying the number of errors that were logged to the {@link ParseErrorListener}.
	 *
	 * @param fatalErrors Expected number of fatal errors logged by error listener.
	 * @param errors      Expected number of errors logged by error listener.
	 * @param warnings    Expected number of warnings logged by error listener.
	 */
	private void assertErrorListener(int fatalErrors, int errors, int warnings) {
		assertEquals(fatalErrors, errListener.getFatalErrors().size());
		assertEquals(errors, errListener.getErrors().size());
		assertEquals(warnings, errListener.getWarnings().size());
	}
}
