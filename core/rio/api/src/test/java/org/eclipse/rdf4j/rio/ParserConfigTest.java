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
package org.eclipse.rdf4j.rio;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashSet;

import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test for ParserConfig to verify that the core operations succeed and are consistent.
 *
 * @author Peter Ansell
 */
public class ParserConfigTest {

	/**
	 * Test the default constructor does not set any settings, but still returns the default values for basic settings.
	 */
	@Test
	public final void testParserConfig() {
		ParserConfig testConfig = new ParserConfig();

		// check that the basic settings are not set
		assertFalse(testConfig.isSet(BasicParserSettings.PRESERVE_BNODE_IDS));

		// check that the basic settings all return their expected default values
		assertFalse(testConfig.get(BasicParserSettings.PRESERVE_BNODE_IDS));
		assertFalse(testConfig.isPreserveBNodeIDs());

		// then set to check that changes occur
		testConfig.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);

		// check that the basic settings are now explicitly set
		assertTrue(testConfig.isSet(BasicParserSettings.PRESERVE_BNODE_IDS));

		// check that the basic settings all return their set values
		assertTrue(testConfig.get(BasicParserSettings.PRESERVE_BNODE_IDS));
		assertTrue(testConfig.isPreserveBNodeIDs());

		// reset the values
		testConfig.set(BasicParserSettings.PRESERVE_BNODE_IDS, null);

		// check again that the basic settings all return their expected default
		// values
		assertFalse(testConfig.get(BasicParserSettings.PRESERVE_BNODE_IDS));
		assertFalse(testConfig.isPreserveBNodeIDs());
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.ParserConfig#ParserConfig(boolean, boolean, boolean, org.eclipse.rdf4j.rio.RDFParser.DatatypeHandling)}
	 * .
	 */
	@Ignore("TODO: Implement me")
	@Test
	public final void testParserConfigBooleanBooleanBooleanDatatypeHandling() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.ParserConfig#useDefaults()}.
	 */
	@Test
	public final void testUseDefaults() {
		ParserConfig testConfig = new ParserConfig();

		// Test the initial state and add a non-fatal error first
		assertNotNull(testConfig.getNonFatalErrors());
		assertTrue(testConfig.getNonFatalErrors().isEmpty());
		testConfig.addNonFatalError(BasicParserSettings.PRESERVE_BNODE_IDS);
		assertFalse(testConfig.getNonFatalErrors().isEmpty());
		assertTrue(testConfig.isNonFatalError(BasicParserSettings.PRESERVE_BNODE_IDS));

		// Test useDefaults
		testConfig.useDefaults();

		// Verify that the non fatal errors are empty again
		assertTrue(testConfig.getNonFatalErrors().isEmpty());
		assertFalse(testConfig.isNonFatalError(BasicParserSettings.PRESERVE_BNODE_IDS));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.ParserConfig#setNonFatalErrors(java.util.Set)}.
	 */
	@Test
	public final void testSetNonFatalErrors() {
		ParserConfig testConfig = new ParserConfig();

		// Test that the defaults exist and are empty
		assertNotNull(testConfig.getNonFatalErrors());
		assertTrue(testConfig.getNonFatalErrors().isEmpty());

		// Test that we can add to the default before calling setNonFatalErrors
		// (SES-1801)
		testConfig.addNonFatalError(BasicParserSettings.PRESERVE_BNODE_IDS);
		assertFalse(testConfig.getNonFatalErrors().isEmpty());
		assertTrue(testConfig.getNonFatalErrors().contains(BasicParserSettings.PRESERVE_BNODE_IDS));
		assertFalse(testConfig.getNonFatalErrors().contains(BasicParserSettings.VERIFY_DATATYPE_VALUES));

		// Test with a non-empty set that we remove the previous setting
		testConfig.setNonFatalErrors(Collections.<RioSetting<?>>singleton(BasicParserSettings.VERIFY_DATATYPE_VALUES));
		assertNotNull(testConfig.getNonFatalErrors());
		assertFalse(testConfig.getNonFatalErrors().isEmpty());
		assertFalse(testConfig.getNonFatalErrors().contains(BasicParserSettings.PRESERVE_BNODE_IDS));
		assertTrue(testConfig.getNonFatalErrors().contains(BasicParserSettings.VERIFY_DATATYPE_VALUES));

		// Test with an empty set
		testConfig.setNonFatalErrors(new HashSet<>());
		assertNotNull(testConfig.getNonFatalErrors());
		assertTrue(testConfig.getNonFatalErrors().isEmpty());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.ParserConfig#addNonFatalError(org.eclipse.rdf4j.rio.RioSetting)} .
	 */
	@Test
	public final void testAddNonFatalError() {
		ParserConfig testConfig = new ParserConfig();

		assertTrue(testConfig.getNonFatalErrors().isEmpty());
		testConfig.addNonFatalError(BasicParserSettings.PRESERVE_BNODE_IDS);
		assertTrue(testConfig.getNonFatalErrors().contains(BasicParserSettings.PRESERVE_BNODE_IDS));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.ParserConfig#isNonFatalError(org.eclipse.rdf4j.rio.RioSetting)} .
	 */
	@Test
	public final void testIsNonFatalError() {
		ParserConfig testConfig = new ParserConfig();

		assertTrue(testConfig.getNonFatalErrors().isEmpty());

		assertFalse(testConfig.isNonFatalError(BasicParserSettings.PRESERVE_BNODE_IDS));

		testConfig.addNonFatalError(BasicParserSettings.PRESERVE_BNODE_IDS);

		assertTrue(testConfig.isNonFatalError(BasicParserSettings.PRESERVE_BNODE_IDS));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.ParserConfig#getNonFatalErrors()}.
	 */
	@Test
	public final void testGetNonFatalErrors() {
		ParserConfig testConfig = new ParserConfig();

		assertTrue(testConfig.getNonFatalErrors().isEmpty());

		testConfig.addNonFatalError(BasicParserSettings.PRESERVE_BNODE_IDS);

		assertFalse(testConfig.getNonFatalErrors().isEmpty());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.ParserConfig#isPreserveBNodeIDs()}.
	 */
	@Test
	public final void testIsPreserveBNodeIDs() {
		ParserConfig testConfig = new ParserConfig();

		assertFalse(testConfig.isPreserveBNodeIDs());

		testConfig.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);

		assertTrue(testConfig.isPreserveBNodeIDs());
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.ParserConfig#get(org.eclipse.rdf4j.rio.RioSetting)}.
	 */
	@Test
	public final void testGet() {
		ParserConfig testConfig = new ParserConfig();

		assertTrue(testConfig.get(BasicParserSettings.VERIFY_RELATIVE_URIS));

		testConfig.set(BasicParserSettings.VERIFY_RELATIVE_URIS, false);

		assertFalse(testConfig.get(BasicParserSettings.VERIFY_RELATIVE_URIS));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.rio.ParserConfig#set(org.eclipse.rdf4j.rio.RioSetting, java.lang.Object)} .
	 */
	@Test
	public final void testSet() {
		ParserConfig testConfig = new ParserConfig();

		assertFalse(testConfig.isSet(BasicParserSettings.VERIFY_RELATIVE_URIS));

		testConfig.set(BasicParserSettings.VERIFY_RELATIVE_URIS, false);

		assertTrue(testConfig.isSet(BasicParserSettings.VERIFY_RELATIVE_URIS));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.ParserConfig#isSet(org.eclipse.rdf4j.rio.RioSetting)}.
	 */
	@Test
	public final void testIsSet() {
		ParserConfig testConfig = new ParserConfig();

		assertFalse(testConfig.isSet(BasicParserSettings.PRESERVE_BNODE_IDS));

		testConfig.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);

		assertTrue(testConfig.isSet(BasicParserSettings.PRESERVE_BNODE_IDS));
	}
}
