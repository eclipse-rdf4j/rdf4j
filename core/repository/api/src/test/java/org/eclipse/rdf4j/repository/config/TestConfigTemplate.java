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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

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

	@Test
	public final void testInvalidDelimiterThrowsException() {
		assertThrows(IllegalArgumentException.class, () -> ConfigTemplate.escapeMultilineQuotes("'", "any value"));
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

	@Test
	public final void testInlineHintsShouldBeHiddenFromVariableNames() {
		ConfigTemplate temp = new ConfigTemplate("{%Triple DB size[len=16]|1099511627776%}");

		assertTrue(temp.getVariableMap().containsKey("Triple DB size"));
		assertFalse(temp.getVariableMap().containsKey("Triple DB size[len=16]"));
		assertEquals("1099511627776", temp.getVariableMap().get("Triple DB size").get(0));
	}

	@Test
	public final void testInlineHintsShouldNotChangeRenderKeys() {
		ConfigTemplate temp = new ConfigTemplate("{%Triple DB size[len=16]|1099511627776%}");
		Map<String, String> map = new LinkedHashMap<>();
		map.put("Triple DB size", "20971520");

		assertEquals("20971520", temp.render(map));
	}

	@Test
	public final void testTokenDefaultsMayContainCurlyBraces() {
		ConfigTemplate temp = new ConfigTemplate(
				"{%Rule query[rows=8 cols=80]|CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o FILTER(false) }%}");

		assertEquals("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o FILTER(false) }",
				temp.getVariableMap().get("Rule query").get(0));
		assertEquals("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o FILTER(false) }", temp.render(Map.of()));
	}

	@Test
	public final void testInlineDefaultHintShouldProvideTemplateDefault() {
		ConfigTemplate temp = new ConfigTemplate(
				"{%Rule query[rows=8 cols=80 default=\"CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o FILTER(false) }\"]|%}");

		assertEquals("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o FILTER(false) }",
				temp.getVariableMap().get("Rule query").get(0));
		assertEquals("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o FILTER(false) }", temp.render(Map.of()));
	}
}
