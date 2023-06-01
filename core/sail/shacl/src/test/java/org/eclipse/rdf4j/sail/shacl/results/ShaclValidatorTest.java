/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.results;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclValidator;
import org.junit.jupiter.api.Test;

/**
 * @author Håvard Ottestad
 */
public class ShaclValidatorTest {

	@Test
	public void testDefaultGraphIsUnionValid() throws Exception {

		SailRepository shapes = getShapes(null);

		SailRepository data = new SailRepository(new MemoryStore());

		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(Values.iri("http://example.org", "person1"), RDF.TYPE, RDFS.RESOURCE);
			connection.add(Values.iri("http://example.org", "person1"), RDFS.LABEL, Values.literal("label"),
					Values.iri("http://example.org/g1"));
		}

		ValidationReport validate = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertTrue(validate.conforms());
	}

	@Test
	public void testDefaultGraphIsUnionFailureDefaultGraph() throws Exception {

		SailRepository shapes = getShapes(null);

		SailRepository data = new SailRepository(new MemoryStore());

		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(Values.iri("http://example.org", "person1"), RDF.TYPE, RDFS.RESOURCE);
		}

		ValidationReport validate = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(validate.conforms());
	}

	@Test
	public void testDefaultGraphIsUnionWarningDefaultGraph() throws Exception {

		SailRepository shapes = getShapes("sh:Warning");

		SailRepository data = new SailRepository(new MemoryStore());

		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(Values.iri("http://example.org", "person1"), RDF.TYPE, RDFS.RESOURCE);
		}

		ValidationReport validate = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(validate.conforms());
		assertEquals(1, validate.getValidationResult().size());
		assertEquals(SHACL.WARNING, validate.getValidationResult().get(0).getSeverity().getIri());

	}

	@Test
	public void testDefaultGraphIsUnionInfoDefaultGraph() throws Exception {

		SailRepository shapes = getShapes("sh:Info");

		SailRepository data = new SailRepository(new MemoryStore());

		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(Values.iri("http://example.org", "person1"), RDF.TYPE, RDFS.RESOURCE);
		}

		ValidationReport validate = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(validate.conforms());
		assertEquals(1, validate.getValidationResult().size());
		assertEquals(SHACL.INFO, validate.getValidationResult().get(0).getSeverity().getIri());
	}

	@Test
	public void testDefaultGraphIsUnionFailureNamedGraph() throws Exception {

		SailRepository shapes = getShapes(null);

		SailRepository data = new SailRepository(new MemoryStore());

		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(Values.iri("http://example.org", "person1"), RDF.TYPE, RDFS.RESOURCE,
					Values.iri("http://example.org/g1"));
		}

		ValidationReport validate = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(validate.conforms());
	}

	private static SailRepository getShapes(String severity) throws IOException {
		String turtleTemplate = "@base <http://example.com/ns> .\n" +
				"@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix owl: <http://www.w3.org/2002/07/owl#> .\n" +
				"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"@prefix rdf4j: <http://rdf4j.org/schema/rdf4j#> .\n" +
				"\n" +
				"ex:PersonShape\n" +
				"\ta sh:NodeShape  ;\n" +
				"\tsh:targetClass rdfs:Resource ;\n" +
				"\tsh:property ex:PersonShapeProperty  .\n" +
				"\n" +
				"\n" +
				"ex:PersonShapeProperty\n" +
				"        sh:path rdfs:label ;\n" +
				"        %s" +
				"        sh:minCount 1 .\n";

		String severityLine = severity != null ? "sh:severity " + severity + " ;\n" : "";
		String turtleString = String.format(turtleTemplate, severityLine);

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(turtleString), RDFFormat.TURTLE);
		}
		return shapes;
	}

}
