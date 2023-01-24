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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.LiteralUtilException;
import org.eclipse.rdf4j.rio.DatatypeHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Abstract test for DatatypeHandler interface.
 *
 * @author Peter Ansell
 */
public abstract class AbstractDatatypeHandlerTest {

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

	@BeforeEach
	public void setUp() throws Exception {
		testHandler = getNewDatatypeHandler();
		vf = getValueFactory();
	}

	@AfterEach
	public void tearDown() throws Exception {
		testHandler = null;
		vf = null;
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.DatatypeHandler#isRecognizedDatatype(org.eclipse.rdf4j.model.IRI)} .
	 */
	@Test
	public void testIsRecognizedDatatypeNull() throws Exception {
		assertThatThrownBy(() -> testHandler.isRecognizedDatatype(null))
				.isInstanceOf(NullPointerException.class);
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
		assertThatThrownBy(() -> testHandler.verifyDatatype(getValueMatchingRecognisedDatatypeUri(), null))
				.isInstanceOf(NullPointerException.class);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.DatatypeHandler#verifyDatatype(java.lang.String, org.eclipse.rdf4j.model.IRI)} .
	 */
	@Test
	public void testVerifyDatatypeNullValueRecognised() throws Exception {
		assertThatThrownBy(() -> testHandler.verifyDatatype(null, getRecognisedDatatypeUri()))
				.isInstanceOf(NullPointerException.class);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.DatatypeHandler#verifyDatatype(java.lang.String, org.eclipse.rdf4j.model.IRI)} .
	 */
	@Test
	public void testVerifyDatatypeNullValueUnrecognised() throws Exception {
		assertThatThrownBy(() -> testHandler.verifyDatatype(null, getUnrecognisedDatatypeUri()))
				.isInstanceOf(LiteralUtilException.class);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.DatatypeHandler#verifyDatatype(java.lang.String, org.eclipse.rdf4j.model.IRI)} .
	 */
	@Test
	public void testVerifyDatatypeUnrecognisedDatatypeUri() throws Exception {
		assertThatThrownBy(
				() -> testHandler.verifyDatatype(getValueMatchingRecognisedDatatypeUri(), getUnrecognisedDatatypeUri()))
				.isInstanceOf(LiteralUtilException.class);
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
		assertThatThrownBy(() -> testHandler.normalizeDatatype(getValueMatchingRecognisedDatatypeUri(), null, vf))
				.isInstanceOf(NullPointerException.class);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.DatatypeHandler#normalizeDatatype(java.lang.String, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.ValueFactory)}
	 * .
	 */
	@Test
	public void testNormalizeDatatypeNullValue() throws Exception {
		assertThatThrownBy(() -> testHandler.normalizeDatatype(null, getRecognisedDatatypeUri(), vf))
				.isInstanceOf(NullPointerException.class);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.DatatypeHandler#normalizeDatatype(java.lang.String, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.ValueFactory)}
	 * .
	 */
	@Test
	public void testNormalizeDatatypeUnrecognisedDatatypeUri() throws Exception {
		assertThatThrownBy(() -> testHandler.normalizeDatatype(getValueMatchingRecognisedDatatypeUri(),
				getUnrecognisedDatatypeUri(), vf))
				.isInstanceOf(LiteralUtilException.class);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.DatatypeHandler#normalizeDatatype(java.lang.String, org.eclipse.rdf4j.model.IRI, org.eclipse.rdf4j.model.ValueFactory)}
	 * .
	 */
	@Test
	public void testNormalizeDatatypeInvalidValue() throws Exception {
		assertThatThrownBy(() -> testHandler.normalizeDatatype(getValueNotMatchingRecognisedDatatypeUri(),
				getRecognisedDatatypeUri(), vf))
				.isInstanceOf(LiteralUtilException.class);
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
