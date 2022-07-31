/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.rdf4j.model.util.Values.namespace;

import java.util.Arrays;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.Test;

public class SPARQLQueriesTest {

	@Test
	public void testGetPrefixClauses() {
		String result = SPARQLQueries.getPrefixClauses(Arrays.asList(RDF.NS, namespace("ex", "http://example.org/")));

		assertThat(result).isEqualTo("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+ "PREFIX ex: <http://example.org/>\n");

	}

	@Test
	public void testUnescape() {
		String escaped = "foo\\nbar\\twith\\\\most of the \\\"actual\\\" chars that need \\'escaping\\'";

		assertThat(SPARQLQueries.unescape(escaped))
				.isEqualTo("foo\nbar\twith\\most of the \"actual\" chars that need 'escaping'");
	}

	@Test
	public void testUnescape_invalid() {
		String escaped = "foo\\ bar";

		assertThatThrownBy(() -> SPARQLQueries.unescape(escaped)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Unescaped backslash in: " + escaped);
	}

	@Test
	public void testEscape() {
		String unescaped = "foo\nbar\twith\\most of the \"actual\" chars that need 'escaping'";
		String escaped = "foo\\nbar\\twith\\\\most of the \\\"actual\\\" chars that need \\'escaping\\'";

		assertThat(SPARQLQueries.escape(unescaped)).isEqualTo(escaped);
	}
}
