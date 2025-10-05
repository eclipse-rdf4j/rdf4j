/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.turtle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for duplicate prefix declarations in Turtle parser.
 */
public class TurtlePrefixDuplicateTest {

	private RDFParser parser;
	private List<Statement> statements;
	private RDFHandler handler;

	@BeforeEach
	public void setUp() {
		parser = new TurtleParser();
		statements = new ArrayList<>();
		handler = new StatementCollector(statements);
		parser.setRDFHandler(handler);
	}

	@Test
	public void testDuplicatePrefixDeclarations_SameNamespace_ShouldPass() throws Exception {
		String turtle = "@prefix foaf: <http://xmlns.com/foaf/0.1/> .\n" +
				"@prefix foaf: <http://xmlns.com/foaf/0.1/> .\n" +
				"\n" +
				"<http://example.org/person> foaf:name \"John Doe\" .\n";

		// Should not throw an exception when same prefix maps to same namespace
		assertDoesNotThrow(() -> parser.parse(new StringReader(turtle), "http://example.org/"));

		// Should produce the expected statement
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getPredicate().toString()).isEqualTo("http://xmlns.com/foaf/0.1/name");
	}

	@Test
	public void testDuplicatePrefixDeclarations_DifferentNamespace_LastOneWins() throws Exception {
		String turtle = "@prefix foaf: <http://xmlns.com/foaf/0.1/> .\n" +
				"@prefix foaf: <http://example.org/different/> .\n" +
				"\n" +
				"<http://example.org/person> foaf:name \"John Doe\" .\n";

		// Should not throw an exception - Turtle parsers typically allow redefinition
		assertDoesNotThrow(() -> parser.parse(new StringReader(turtle), "http://example.org/"));

		// The last prefix declaration should win
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getPredicate().toString()).isEqualTo("http://example.org/different/name");
	}

	@Test
	public void testDuplicateDefaultPrefixDeclarations_SameNamespace_ShouldPass() throws Exception {
		String turtle = "@prefix : <http://example.org/ns#> .\n" +
				"@prefix : <http://example.org/ns#> .\n" +
				"\n" +
				"<http://example.org/person> :name \"John Doe\" .\n";

		// Should not throw an exception when same default prefix maps to same namespace
		assertDoesNotThrow(() -> parser.parse(new StringReader(turtle), "http://example.org/"));

		// Should produce the expected statement
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getPredicate().toString()).isEqualTo("http://example.org/ns#name");
	}
}