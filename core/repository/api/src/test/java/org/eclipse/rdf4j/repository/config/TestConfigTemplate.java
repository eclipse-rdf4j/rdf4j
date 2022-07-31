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
package org.eclipse.rdf4j.repository.config;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

/**
 * @author Dale Visser
 */
public class TestConfigTemplate {

	@Test
	public final void testNonEscapeOfAlternateMultilineDelimiter() {
		String value = "I contain a '''multiline\nstring''' that shouldn't be escaped.";
		assertEquals(ConfigTemplate.escapeMultilineQuotes("\"\"\"", value), value);
		value = "I contain a \"\"\"multiline\nstring\"\"\" that shouldn't be escaped.";
		assertEquals(ConfigTemplate.escapeMultilineQuotes("'''", value), value);
	}

	@Test
	public final void testEscapeOfSpecifiedMultilineDelimiter() {
		String value = "I contain a '''multiline\nstring''' that should be escaped.";
		assertEquals(ConfigTemplate.escapeMultilineQuotes("'''", value),
				"I contain a \\'\\'\\'multiline\nstring\\'\\'\\' that should be escaped.");
		value = "I contain a \"\"\"multiline\nstring\"\"\" that should be escaped.";
		assertEquals(ConfigTemplate.escapeMultilineQuotes("\"\"\"", value),
				"I contain a \\\"\\\"\\\"multiline\nstring\\\"\\\"\\\" that should be escaped.");
	}

	@Test
	public final void testNonEscapeOfShorterSequences() {
		String value = "' '' ''' ''''";
		assertEquals(ConfigTemplate.escapeMultilineQuotes("'''", value), "' '' \\'\\'\\' \\'\\'\\''");
		value = "\" \"\" \"\"\" \"\"\"\"";
		assertEquals(ConfigTemplate.escapeMultilineQuotes("\"\"\"", value), "\" \"\" \\\"\\\"\\\" \\\"\\\"\\\"\"");
	}

	@Test(expected = IllegalArgumentException.class)
	public final void testInvalidDelimiterThrowsException() {
		ConfigTemplate.escapeMultilineQuotes("'", "any value");
	}

	@Test
	public final void testSimpleCharacters() {
		ConfigTemplate temp = new ConfigTemplate("{%value%}");
		Map<String, String> map = new LinkedHashMap<>();
		map.put("value", "sob");
		assertEquals("sob", temp.render(map));
	}

	@Test
	public final void testSpecialCharacters() {
		ConfigTemplate temp = new ConfigTemplate("{%value%}");
		Map<String, String> map = new LinkedHashMap<>();
		map.put("value", "$0b");
		assertEquals("$0b", temp.render(map));
	}
}
