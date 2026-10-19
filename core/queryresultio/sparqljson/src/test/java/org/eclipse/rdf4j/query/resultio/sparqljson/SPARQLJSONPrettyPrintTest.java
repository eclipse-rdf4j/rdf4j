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
package org.eclipse.rdf4j.query.resultio.sparqljson;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link BasicWriterSettings#PRETTY_PRINT} is honoured by the SPARQL JSON writers.
 */
public class SPARQLJSONPrettyPrintTest {

	private static final SimpleValueFactory VF = SimpleValueFactory.getInstance();

	// --- tuple writer helpers ------------------------------------------------

	private String writeTupleResult(boolean prettyPrint) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		SPARQLResultsJSONWriter writer = new SPARQLResultsJSONWriter(out);
		writer.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, prettyPrint);

		MapBindingSet bs = new MapBindingSet();
		bs.addBinding("s", VF.createIRI("http://example.org/subject"));
		bs.addBinding("p", RDF.TYPE);
		bs.addBinding("o", VF.createIRI("http://example.org/object"));

		writer.startQueryResult(List.of("s", "p", "o"));
		writer.handleSolution(bs);
		writer.endQueryResult();

		return out.toString(StandardCharsets.UTF_8);
	}

	@Test
	public void tupleResultPrettyPrintEnabled() throws Exception {
		String output = writeTupleResult(true);
		assertThat(output).contains("\n");
	}

	@Test
	public void tupleResultPrettyPrintDisabled() throws Exception {
		String output = writeTupleResult(false);
		assertThat(output).doesNotContain("\n");
	}

	@Test
	public void tupleResultPrettyPrintOutputIsParseable() throws Exception {
		// Sanity-check: pretty-printed output must still parse back to the same bindings.
		String prettyOutput = writeTupleResult(true);
		String compactOutput = writeTupleResult(false);

		assertThat(prettyOutput).isNotEqualTo(compactOutput);

		// Both outputs represent the same logical content: one binding with three variables.
		for (String output : List.of(prettyOutput, compactOutput)) {
			SPARQLResultsJSONParser parser = new SPARQLResultsJSONParser();
			org.eclipse.rdf4j.query.resultio.helpers.QueryResultCollector collector = new org.eclipse.rdf4j.query.resultio.helpers.QueryResultCollector();
			parser.setQueryResultHandler(collector);
			parser.parseQueryResult(
					new java.io.ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)));
			assertThat(collector.getBindingSets()).hasSize(1);
			assertThat(collector.getBindingNames()).containsExactlyInAnyOrder("s", "p", "o");
		}
	}

	// --- boolean writer helpers ----------------------------------------------

	private String writeBooleanResult(boolean prettyPrint) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		SPARQLBooleanJSONWriter writer = new SPARQLBooleanJSONWriter(out);
		writer.getWriterConfig().set(BasicWriterSettings.PRETTY_PRINT, prettyPrint);
		writer.handleBoolean(true);
		return out.toString(StandardCharsets.UTF_8);
	}

	@Test
	public void booleanResultPrettyPrintEnabled() throws Exception {
		String output = writeBooleanResult(true);
		assertThat(output).contains("\n");
	}

	@Test
	public void booleanResultPrettyPrintDisabled() throws Exception {
		String output = writeBooleanResult(false);
		assertThat(output).doesNotContain("\n");
	}

	@Test
	public void booleanResultPrettyPrintOutputIsParseable() throws Exception {
		// Sanity-check: pretty-printed output must still parse to the same boolean value.
		String prettyOutput = writeBooleanResult(true);
		String compactOutput = writeBooleanResult(false);

		assertThat(prettyOutput).isNotEqualTo(compactOutput);

		for (String output : List.of(prettyOutput, compactOutput)) {
			SPARQLBooleanJSONParser parser = new SPARQLBooleanJSONParser(VF);
			org.eclipse.rdf4j.query.resultio.helpers.QueryResultCollector collector = new org.eclipse.rdf4j.query.resultio.helpers.QueryResultCollector();
			parser.setQueryResultHandler(collector);
			parser.parseQueryResult(
					new java.io.ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)));
			assertThat(collector.getBoolean()).isTrue();
		}
	}
}
