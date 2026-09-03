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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for duplicate prefix declarations in Turtle parser. Tests include scenarios with @base to show how different
 * placement of the base changes duplicate relative prefix declarations.
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

	// Tests with @base showing different scenarios with relative prefix declarations

	@Test
	public void testDuplicateRelativePrefix_SameBase_ShouldPass() throws Exception {
		String turtle = "@base <http://example.org/> .\n" +
				"@prefix rel: <vocab/> .\n" +
				"@prefix rel: <vocab/> .\n" +
				"\n" +
				"<http://example.org/person> rel:name \"Alice\" .\n";

		// Should not throw an exception when same relative prefix resolves to same absolute namespace
		assertDoesNotThrow(() -> parser.parse(new StringReader(turtle), "http://example.org/"));

		// Should produce the expected statement with resolved absolute namespace
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getPredicate().toString()).isEqualTo("http://example.org/vocab/name");
	}

	@Test
	public void testDuplicateRelativePrefix_BaseChangedBetween_LastWins() throws Exception {
		String turtle = "@base <http://example.org/> .\n" +
				"@prefix rel: <vocab/> .\n" +
				"@base <http://different.org/> .\n" +
				"@prefix rel: <vocab/> .\n" +
				"\n" +
				"<http://example.org/person> rel:name \"Bob\" .\n";

		// Should not throw exception - turtle allows redefinition, last one wins
		assertDoesNotThrow(() -> parser.parse(new StringReader(turtle), "http://example.org/"));

		// The last prefix declaration should win (resolved with the second base)
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getPredicate().toString()).isEqualTo("http://different.org/vocab/name");
	}

	@Test
	public void testDuplicateRelativePrefix_ExternalBaseChange_LastWins() throws Exception {
		String turtle = "@prefix rel: <vocab/> .\n" +
				"@base <http://different.org/> .\n" +
				"@prefix rel: <vocab/> .\n" +
				"\n" +
				"<http://example.org/person> rel:name \"Charlie\" .\n";

		// Should not throw exception - turtle allows redefinition
		assertDoesNotThrow(() -> parser.parse(new StringReader(turtle), "http://example.org/"));

		// First prefix uses external base, second uses internal base
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getPredicate().toString()).isEqualTo("http://different.org/vocab/name");
	}

	@Test
	public void testDuplicateRelativePrefix_MultipleBaseChanges_LastWins() throws Exception {
		String turtle = "@base <http://first.org/> .\n" +
				"@prefix rel: <vocab/> .\n" +
				"@base <http://second.org/> .\n" +
				"@base <http://third.org/> .\n" +
				"@prefix rel: <vocab/> .\n" +
				"\n" +
				"<http://example.org/person> rel:name \"David\" .\n";

		// Should not throw exception
		assertDoesNotThrow(() -> parser.parse(new StringReader(turtle), "http://example.org/"));

		// Last prefix with last base should win
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getPredicate().toString()).isEqualTo("http://third.org/vocab/name");
	}

	@Test
	public void testDuplicateRelativePrefix_BaseAfterAllPrefixes_EarlierBasesUsed() throws Exception {
		String turtle = "@base <http://first.org/> .\n" +
				"@prefix rel: <vocab/> .\n" +
				"@base <http://second.org/> .\n" +
				"@prefix rel: <vocab/> .\n" +
				"@base <http://third.org/> .\n" +
				"\n" +
				"<http://example.org/person> rel:name \"Eve\" .\n";

		// Should not throw exception
		assertDoesNotThrow(() -> parser.parse(new StringReader(turtle), "http://example.org/"));

		// Second prefix with second base should be used (base after prefixes doesn't affect them)
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getPredicate().toString()).isEqualTo("http://second.org/vocab/name");
	}

	@Test
	public void testDuplicateRelativePrefix_SameExternalBase_ShouldPass() throws Exception {
		String turtle = "@prefix rel: <vocab/> .\n" +
				"@prefix rel: <vocab/> .\n" +
				"\n" +
				"<http://example.org/person> rel:name \"Frank\" .\n";

		// Should not throw exception when both relative prefixes resolve to same namespace using external base
		assertDoesNotThrow(() -> parser.parse(new StringReader(turtle), "http://example.org/"));

		// Both prefixes resolve to same namespace using external base
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getPredicate().toString()).isEqualTo("http://example.org/vocab/name");
	}

	@Test
	public void testDuplicateRelativePrefix_SameInternalBase_ShouldPass() throws Exception {
		String turtle = "@base <http://example.org/ns/> .\n" +
				"@prefix rel: <vocab/> .\n" +
				"@prefix rel: <vocab/> .\n" +
				"\n" +
				"<http://example.org/person> rel:name \"Grace\" .\n";

		// Should not throw exception when both relative prefixes resolve to same namespace
		assertDoesNotThrow(() -> parser.parse(new StringReader(turtle), "http://example.org/"));

		// Both prefixes resolve to same namespace using internal base
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getPredicate().toString()).isEqualTo("http://example.org/ns/vocab/name");
	}

	@Test
	public void testDuplicateRelativePrefix_AbsoluteToRelative_LastWins() throws Exception {
		String turtle = "@prefix rel: <http://absolute.org/vocab/> .\n" +
				"@base <http://relative.org/> .\n" +
				"@prefix rel: <vocab/> .\n" +
				"\n" +
				"<http://example.org/person> rel:name \"Henry\" .\n";

		// Should not throw exception
		assertDoesNotThrow(() -> parser.parse(new StringReader(turtle), "http://example.org/"));

		// Second (relative) prefix should win
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getPredicate().toString()).isEqualTo("http://relative.org/vocab/name");
	}

	@Test
	public void testDuplicateRelativePrefix_RelativeToAbsolute_LastWins() throws Exception {
		String turtle = "@base <http://relative.org/> .\n" +
				"@prefix rel: <vocab/> .\n" +
				"@prefix rel: <http://absolute.org/vocab/> .\n" +
				"\n" +
				"<http://example.org/person> rel:name \"Ivy\" .\n";

		// Should not throw exception
		assertDoesNotThrow(() -> parser.parse(new StringReader(turtle), "http://example.org/"));

		// Second (absolute) prefix should win
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getPredicate().toString()).isEqualTo("http://absolute.org/vocab/name");
	}

	@Test
	public void testDuplicateRelativePrefix_ComplexRelativePaths_LastWins() throws Exception {
		String turtle = "@base <http://example.org/path/> .\n" +
				"@prefix rel: <../vocab/> .\n" +
				"@base <http://example.org/different/path/> .\n" +
				"@prefix rel: <../../vocab/> .\n" +
				"\n" +
				"<http://example.org/person> rel:name \"Jack\" .\n";

		// Should not throw exception
		assertDoesNotThrow(() -> parser.parse(new StringReader(turtle), "http://example.org/"));

		// Last prefix with complex relative path should win
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getPredicate().toString()).isEqualTo("http://example.org/vocab/name");
	}

	@Test
	public void testDuplicateRelativePrefix_NoBaseForRelative_ShouldFail() throws Exception {
		String turtle = "@prefix rel: <vocab/> .\n" +
				"@prefix rel: <vocab/> .\n" +
				"\n" +
				"<http://example.org/person> rel:name \"Kate\" .\n";

		// Should throw exception when relative prefix cannot be resolved (no base provided)
		assertThrows(RDFParseException.class, () -> {
			parser.parse(new StringReader(turtle), null);
		});
	}

	@Test
	public void testDuplicateDefaultRelativePrefix_BaseChanges_LastWins() throws Exception {
		String turtle = "@base <http://first.org/> .\n" +
				"@prefix : <vocab/> .\n" +
				"@base <http://second.org/> .\n" +
				"@prefix : <vocab/> .\n" +
				"\n" +
				"<http://example.org/person> :name \"Luna\" .\n";

		// Should not throw exception
		assertDoesNotThrow(() -> parser.parse(new StringReader(turtle), "http://example.org/"));

		// Last default prefix declaration should win
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getPredicate().toString()).isEqualTo("http://second.org/vocab/name");
	}
}