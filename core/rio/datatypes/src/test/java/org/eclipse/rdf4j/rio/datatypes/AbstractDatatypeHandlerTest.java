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
package org.eclipse.rdf4j.rio.datatypes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.LiteralUtilException;
import org.eclipse.rdf4j.rio.DatatypeHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Abstract test for DatatypeHandler interface.
 *
 * @author Peter Ansell
 */
public abstract class AbstractDatatypeHandlerTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	/**
	 * Generates a new instance of the {@link DatatypeHandler} implementation in question and returns it.
	 *
	 * @return A new instance of the {@link DatatypeHandler} implementation being tested.
	 */
	protected abstract DatatypeHandler getNewDatatypeHandler();

	/**
	 * @return A URI that must return true from {@link DatatypeHandler#isRecognizedDatatype(IRI)} and not throw an
	 *         exception if used with a valid value when calling {@link DatatypeHandler#verifyDatatype(String, IRI)} and
	 *         {@link DatatypeHandler#normalizeDatatype(String, IRI, ValueFactory)} .
	 */
	protected abstract IRI getRecognisedDatatypeUri();

	/**
	 * @return A URI that must return false from {@link DatatypeHandler#isRecognizedDatatype(IRI)} and throw an
	 *         exception if used with {@link DatatypeHandler#verifyDatatype(String, IRI)} and
	 *         {@link DatatypeHandler#normalizeDatatype(String, IRI, ValueFactory)} .
	 */
	protected abstract IRI getUnrecognisedDatatypeUri();

	/**
	 * @return A string value that does match the recognised datatype URI, and will succeed when used with both
	 *         {@link DatatypeHandler#verifyDatatype(String, IRI)} and
	 *         {@link DatatypeHandler#normalizeDatatype(String, IRI, ValueFactory)} .
	 */
	protected abstract String getValueMatchingRecognisedDatatypeUri();

	/**
	 * @return A string value that does not match the recognised datatype URI, and will fail when used with both
	 *         {@link DatatypeHandler#verifyDatatype(String, IRI)} and
	 *         {@link DatatypeHandler#normalizeDatatype(String, IRI, ValueFactory)} .
	 */
	protected abstract String getValueNotMatchingRecognisedDatatypeUri();

	/**
	 * @return An instance of {@link Literal} that is equal to the expected output from a successful call to
	 *         {@link DatatypeHandler#normalizeDatatype(String, IRI, org.eclipse.rdf4j.model.ValueFactory)} ;
	 */
	protected abstract Literal getNormalisedLiteralForRecognisedDatatypeAndValue();

	/**
	 * @return An instance of {@link ValueFactory} that can be used to produce a normalised literal.
	 */
	protected abstract ValueFactory getValueFactory();

	/**
	 * @return The key that is expected to be returned for {@link DatatypeHandler#getKey()} to identify the service.
	 */
	protected abstract String getExpectedKey();

	private DatatypeHandler testHandler;

	private ValueFactory vf;

	@Before
	public void setUp() throws Exception {
		testHandler = getNewDatatypeHandler();
		vf = getValueFactory();
	}

	@After
	public void tearDown() throws Exception {
		testHandler = null;
		vf = null;
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.DatatypeHandler#isRecognizedDatatype(org.eclipse.rdf4j.model.IRI)} .
	 */
	@Test
	public void testIsRecognizedDatatypeNull() throws Exception {
		thrown.expect(NullPointerException.class);
		testHandler.isRecognizedDatatype(null);
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.DatatypeHandler#isRecognizedDatatype(org.eclipse.rdf4j.model.IRI)} .
	 */
	@Test
	public void testIsRecognizedDatatypeTrue() throws Exception {
		assertTrue(testHandler.isRecognizedDatatype(getRecognisedDatatypeUri()));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.DatatypeHandler#isRecognizedDatatype(org.eclipse.rdf4j.model.IRI)} .
	 */
	@Test
	public void testIsRecognizedDatatypeFalse() throws Exception {
		assertFalse(testHandler.isRecognizedDatatype(getUnrecognisedDatatypeUri()));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.DatatypeHandler#verifyDatatype(java.lang.String, org.eclipse.rdf4j.model.IRI)} .
	 */
	@Test
	public void testVerifyDatatypeNullDatatypeUri() throws Exception {
		thrown.expect(NullPointerException.class);
		testHandler.verifyDatatype(getValueMatchingRecognisedDatatypeUri(), null);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.DatatypeHandler#verifyDatatype(java.lang.String, org.eclipse.rdf4j.model.IRI)} .
	 */
	@Test
	public void testVerifyDatatypeNullValueRecognised() throws Exception {
		thrown.expect(NullPointerException.class);
		testHandler.verifyDatatype(null, getRecognisedDatatypeUri());
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.DatatypeHandler#verifyDatatype(java.lang.String, org.eclipse.rdf4j.model.IRI)} .
	 */
	@Test
	public void testVerifyDatatypeNullValueUnrecognised() throws Exception {
		thrown.expect(LiteralUtilException.class);
		testHandler.verifyDatatype(null, getUnrecognisedDatatypeUri());
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.DatatypeHandler#verifyDatatype(java.lang.String, org.eclipse.rdf4j.model.IRI)} .
	 */
	@Test
	public void testVerifyDatatypeUnrecognisedDatatypeUri() throws Exception {
		thrown.expect(LiteralUtilException.class);
		testHandler.verifyDatatype(getValueMatchingRecognisedDatatypeUri(), getUnrecognisedDatatypeUri());
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.DatatypeHandler#verifyDatatype(java.lang.String, org.eclipse.rdf4j.model.IRI)} .
	 */
	@Test
	public void testVerifyDatatypeInvalidValue() throws Exception {
		assertFalse(testHandler.verifyDatatype(getValueNotMatchingRecognisedDatatypeUri(), getRecognisedDatatypeUri()));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.DatatypeHandler#verifyDatatype(java.lang.String, org.eclipse.rdf4j.model.IRI)} .
	 */
	@Test
	public void testVerifyDatatypeValidValue() throws Exception {
		assertTrue(testHandler.verifyDatatype(getValueMatchingRecognisedDatatypeUri(), getRecognisedDatatypeUri()));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.DatatypeHandler#normalizeDatatype(java.lang.String, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.ValueFactory)}
	 * .
	 */
	@Test
	public void testNormalizeDatatypeNullDatatypeUri() throws Exception {
		thrown.expect(NullPointerException.class);
		testHandler.normalizeDatatype(getValueMatchingRecognisedDatatypeUri(), null, vf);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.DatatypeHandler#normalizeDatatype(java.lang.String, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.ValueFactory)}
	 * .
	 */
	@Test
	public void testNormalizeDatatypeNullValue() throws Exception {
		thrown.expect(NullPointerException.class);
		testHandler.normalizeDatatype(null, getRecognisedDatatypeUri(), vf);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.DatatypeHandler#normalizeDatatype(java.lang.String, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.ValueFactory)}
	 * .
	 */
	@Test
	public void testNormalizeDatatypeUnrecognisedDatatypeUri() throws Exception {
		thrown.expect(LiteralUtilException.class);
		testHandler.normalizeDatatype(getValueMatchingRecognisedDatatypeUri(), getUnrecognisedDatatypeUri(), vf);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.DatatypeHandler#normalizeDatatype(java.lang.String, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.ValueFactory)}
	 * .
	 */
	@Test
	public void testNormalizeDatatypeInvalidValue() throws Exception {
		thrown.expect(LiteralUtilException.class);
		testHandler.normalizeDatatype(getValueNotMatchingRecognisedDatatypeUri(), getRecognisedDatatypeUri(), vf);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.DatatypeHandler#normalizeDatatype(java.lang.String, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.ValueFactory)}
	 * .
	 */
	@Test
	public void testNormalizeDatatypeValidValue() throws Exception {
		Literal result = testHandler.normalizeDatatype(getValueMatchingRecognisedDatatypeUri(),
				getRecognisedDatatypeUri(), vf);
		Literal expectedResult = getNormalisedLiteralForRecognisedDatatypeAndValue();

		assertNotNull(expectedResult.getDatatype());
		assertNotNull(expectedResult.getLabel());
		assertFalse(expectedResult.getLanguage().isPresent());

		assertEquals(expectedResult.getDatatype(), result.getDatatype());
		assertEquals(expectedResult.getLabel(), result.getLabel());
		assertEquals(expectedResult.getLanguage(), result.getLanguage());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.DatatypeHandler#getKey()}.
	 */
	@Test
	public void testGetKey() throws Exception {
		String result = testHandler.getKey();
		String expectedResult = getExpectedKey();

		assertEquals(expectedResult, result);
	}

}
