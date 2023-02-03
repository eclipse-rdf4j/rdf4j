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
package org.eclipse.rdf4j.rio.turtle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Test for the utility methods in {@link TurtleUtil}.
 *
 * @author Peter Ansell
 */
public class TurtleUtilTest {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#findURISplitIndex(java.lang.String)} .
	 */
	@Disabled("TODO: Implement me")
	@Test
	public final void testFindURISplitIndex() {

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#isWhitespace(int)}.
	 */
	@Test
	public final void testIsWhitespace() {
		assertFalse(TurtleUtil.isWhitespace(';'));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#isPN_CHARS_BASE(int)}.
	 */
	@Disabled("TODO: Implement me")
	@Test
	public final void testIsPN_CHARS_BASE() {

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#isPN_CHARS_U(int)}.
	 */
	@Disabled("TODO: Implement me")
	@Test
	public final void testIsPN_CHARS_U() {

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#isPN_CHARS(int)}.
	 */
	@Disabled("TODO: Implement me")
	@Test
	public final void testIsPN_CHARS() {

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#isPrefixStartChar(int)}.
	 */
	@Disabled("TODO: Implement me")
	@Test
	public final void testIsPrefixStartChar() {

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#isNameStartChar(int)}.
	 */
	@Disabled("TODO: Implement me")
	@Test
	public final void testIsNameStartChar() {

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#isNameChar(int)}.
	 */
	@Test
	public final void testIsNameChar() {
		assertFalse(TurtleUtil.isNameChar(';'));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#isNameEndChar(int)}.
	 */
	@Disabled("TODO: Implement me")
	@Test
	public final void testIsNameEndChar() {

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#isLocalEscapedChar(int)}.
	 */
	@Disabled("TODO: Implement me")
	@Test
	public final void testIsLocalEscapedChar() {

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#isPrefixChar(int)}.
	 */
	@Disabled("TODO: Implement me")
	@Test
	public final void testIsPrefixChar() {

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#isLanguageStartChar(int)}.
	 */
	@Disabled("TODO: Implement me")
	@Test
	public final void testIsLanguageStartChar() {

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#isLanguageChar(int)}.
	 */
	@Disabled("TODO: Implement me")
	@Test
	public final void testIsLanguageChar() {

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#isPN_PREFIX(java.lang.String)}.
	 */
	@Disabled("TODO: Implement me")
	@Test
	public final void testIsPN_PREFIX() {

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#isPLX_START(java.lang.String)}.
	 */
	@Disabled("TODO: Implement me")
	@Test
	public final void testIsPLX_START() {

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#isPERCENT(java.lang.String)}.
	 */
	@Disabled("TODO: Implement me")
	@Test
	public final void testIsPERCENT() {

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#isPLX_INTERNAL(java.lang.String)} .
	 */
	@Disabled("TODO: Implement me")
	@Test
	public final void testIsPLX_INTERNAL() {

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#isPN_LOCAL_ESC(java.lang.String)} .
	 */
	@Disabled("TODO: Implement me")
	@Test
	public final void testIsPN_LOCAL_ESC() {

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#isPN_LOCAL(java.lang.String)}.
	 */
	@Test
	public final void testIsPN_LOCAL() {
		assertTrue(TurtleUtil.isPN_LOCAL("2bar"));
		assertTrue(TurtleUtil.isPN_LOCAL("foobar"));
		assertTrue(TurtleUtil.isPN_LOCAL("_foobar"));
		assertTrue(TurtleUtil.isPN_LOCAL("foo-bar"));
		assertTrue(TurtleUtil.isPN_LOCAL("foo.bar"));
		assertTrue(TurtleUtil.isPN_LOCAL(":foobar"));
		assertTrue(TurtleUtil.isPN_LOCAL(":foobär"));
		assertTrue(TurtleUtil.isPN_LOCAL(""));

		assertFalse(TurtleUtil.isPN_LOCAL(" "));
		assertFalse(TurtleUtil.isPN_LOCAL("foo$bar"));
		assertFalse(TurtleUtil.isPN_LOCAL("$foobar"));
		assertFalse(TurtleUtil.isPN_LOCAL("foo~bar"));
		assertFalse(TurtleUtil.isPN_LOCAL("~foobar"));
		assertFalse(TurtleUtil.isPN_LOCAL("-foobar"));
		assertFalse(TurtleUtil.isPN_LOCAL("[foobar]"));
		assertFalse(TurtleUtil.isPN_LOCAL("foobar]"));
		assertFalse(TurtleUtil.isPN_LOCAL("(foobar)"));
		assertFalse(TurtleUtil.isPN_LOCAL("foobar)"));
		assertFalse(TurtleUtil.isPN_LOCAL("{foobar}"));
		assertFalse(TurtleUtil.isPN_LOCAL("foobar}"));
		assertFalse(TurtleUtil.isPN_LOCAL(".foobar"));
		assertFalse(TurtleUtil.isPN_LOCAL("foo\tbar"));
		assertFalse(TurtleUtil.isPN_LOCAL("foo\rbar"));
		assertFalse(TurtleUtil.isPN_LOCAL("foo\tbar"));
		assertFalse(TurtleUtil.isPN_LOCAL("foo\nbar"));
		assertFalse(TurtleUtil.isPN_LOCAL("*foobar"));
	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#encodeString(java.lang.String)}.
	 */
	@Disabled("TODO: Implement me")
	@Test
	public final void testEncodeString() {

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#encodeLongString(java.lang.String)} .
	 */
	@Disabled("TODO: Implement me")
	@Test
	public final void testEncodeLongString() {

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#encodeURIString(java.lang.String)} .
	 */
	@Disabled("TODO: Implement me")
	@Test
	public final void testEncodeURIString() {

	}

	/**
	 * Test method for {@link org.eclipse.rdf4j.rio.turtle.TurtleUtil#decodeString(java.lang.String)}.
	 */
	@Disabled("TODO: Implement me")
	@Test
	public final void testDecodeString() {

	}

}
