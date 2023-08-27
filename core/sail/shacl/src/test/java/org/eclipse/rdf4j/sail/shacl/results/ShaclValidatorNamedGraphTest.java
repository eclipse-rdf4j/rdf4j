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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.ShaclValidator;
import org.junit.jupiter.api.Test;

/**
 * @author Håvard Ottestad
 */
public class ShaclValidatorNamedGraphTest {

	private static final String EX = "http://example.com/ns#";
	private static final IRI dataGraph = Values.iri(EX, "dataGraph");

	@Test
	public void testNamedGraph() throws Exception {

		SailRepository shapes = getShapes();

		SailRepository data = new SailRepository(new MemoryStore());

		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(Values.iri(EX, "person1"), RDF.TYPE, RDFS.RESOURCE, dataGraph);
		}

		ValidationReport validate = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(validate.conforms());
	}

	@Test
	public void testRdf4jShaclShapesGraph() throws Exception {

		SailRepository shapes = getShapesRdf4jShaclShapesGraph();

		SailRepository data = new SailRepository(new MemoryStore());

		{
			try (SailRepositoryConnection connection = data.getConnection()) {
				connection.add(Values.iri(EX, "person1"), RDF.TYPE, RDFS.RESOURCE, dataGraph);
			}

			ValidationReport validate = ShaclValidator.validate(data.getSail(), shapes.getSail());
			assertFalse(validate.conforms());
		}

		{
			try (SailRepositoryConnection connection = data.getConnection()) {
				connection.add(Values.iri(EX, "person1"), RDFS.LABEL, Values.literal("label1"));
			}

			ValidationReport validate = ShaclValidator.validate(data.getSail(), shapes.getSail());
			assertTrue(validate.conforms());
		}

		{
			try (SailRepositoryConnection connection = data.getConnection()) {
				connection.add(Values.iri(EX, "person1"), RDFS.LABEL, Values.literal("label2"), dataGraph);
				connection.add(Values.iri(EX, "person1"), RDFS.LABEL, Values.literal("label3"), dataGraph);
			}

			ValidationReport validate = ShaclValidator.validate(data.getSail(), shapes.getSail());
			assertFalse(validate.conforms());
		}

	}

	@Test
	public void testRdf4jShaclShapesGraph2() throws Exception {

		SailRepository shapes = getShapesRdf4jShaclShapesGraph2();

		SailRepository data = new SailRepository(new MemoryStore());

		{
			try (SailRepositoryConnection connection = data.getConnection()) {
				connection.add(Values.iri(EX, "person1"), RDF.TYPE, RDFS.RESOURCE, dataGraph);
			}

			ValidationReport validate = ShaclValidator.validate(data.getSail(), shapes.getSail());
			assertFalse(validate.conforms());
			assertEquals(2, validate.getValidationResult().size());
		}

	}

	private static SailRepository getShapes() throws IOException {
		String shapesString = "@base <http://example.com/ns> .\n" +
				"@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix owl: <http://www.w3.org/2002/07/owl#> .\n" +
				"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"@prefix rdf4j: <http://rdf4j.org/schema/rdf4j#> .\n" +
				"\n" +
				"ex:shapesGraph {\n" +
				"ex:PersonShape\n" +
				"\ta sh:NodeShape  ;\n" +
				"\tsh:targetClass rdfs:Resource ;\n" +
				"\tsh:property ex:PersonShapeProperty  .\n" +
				"\n" +
				"\n" +
				"ex:PersonShapeProperty\n" +
				"        sh:path rdfs:label ;\n" +
				"        sh:minCount 1 .\n" +
				"ex:dataGraph sh:shapesGraph ex:shapesGraph.\n" +
				"}\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesString), RDFFormat.TRIG);
		}
		return shapes;
	}

	private static SailRepository getShapesRdf4jShaclShapesGraph() throws IOException {
		String shapesString = "@base <http://example.com/ns> .\n" +
				"@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix owl: <http://www.w3.org/2002/07/owl#> .\n" +
				"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"@prefix rdf4j: <http://rdf4j.org/schema/rdf4j#> .\n" +
				"\n" +
				"<" + RDF4J.SHACL_SHAPE_GRAPH + "> {\n" +
				"ex:PersonShape\n" +
				"\ta sh:NodeShape  ;\n" +
				"\tsh:targetClass rdfs:Resource ;\n" +
				"\tsh:property ex:PersonShapeProperty  .\n" +
				"\n" +
				"\n" +
				"ex:PersonShapeProperty\n" +
				"        sh:path rdfs:label ;\n" +
				"        sh:minCount 1 .\n" +
				"ex:dataGraph sh:shapesGraph ex:shapesGraph.\n" +
				"}\n" +
				"\n" +
				"ex:shapesGraph {\n" +
				"ex:PersonShape\n" +
				"\ta sh:NodeShape  ;\n" +
				"\tsh:targetClass rdfs:Resource ;\n" +
				"\tsh:property ex:PersonShapeProperty  .\n" +
				"\n" +
				"\n" +
				"ex:PersonShapeProperty\n" +
				"        sh:path rdfs:label ;\n" +
				"        sh:maxCount 1 .\n" +
				"}\n";

		SailRepository shapes = new SailRepository(new ShaclSail(new MemoryStore()));
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesString), RDFFormat.TRIG);
		}
		return shapes;
	}

	private static SailRepository getShapesRdf4jShaclShapesGraph2() throws IOException {
		String shapesString = "@base <http://example.com/ns> .\n" +
				"@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix owl: <http://www.w3.org/2002/07/owl#> .\n" +
				"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"@prefix rdf4j: <http://rdf4j.org/schema/rdf4j#> .\n" +
				"\n" +
				"<" + RDF4J.SHACL_SHAPE_GRAPH + "> {\n" +
				"ex:PersonShape\n" +
				"\ta sh:NodeShape  ;\n" +
				"\tsh:targetClass rdfs:Resource ;\n" +
				"\tsh:property ex:PersonShapeProperty  .\n" +
				"\n" +
				"\n" +
				"ex:PersonShapeProperty\n" +
				"        sh:path rdfs:label ;\n" +
				"        sh:minCount 1 .\n" +
				"ex:dataGraph sh:shapesGraph ex:shapesGraph.\n" +
				"ex:dataGraph sh:shapesGraph <" + RDF4J.SHACL_SHAPE_GRAPH + ">.\n" +
				"}\n" +
				"\n" +
				"ex:shapesGraph {\n" +
				"ex:PersonShape\n" +
				"\ta sh:NodeShape  ;\n" +
				"\tsh:targetClass rdfs:Resource ;\n" +
				"\tsh:property ex:PersonShapeProperty, ex:duplicate  .\n" +
				"\n" +
				"\n" +
				"ex:PersonShapeProperty\n" +
				"        sh:path rdfs:label ;\n" +
				"        sh:maxCount 1 .\n" +
				"ex:duplicate\n" +
				"        sh:path rdfs:label ;\n" +
				"        sh:minCount 1 .\n" +
				"}\n";

		SailRepository shapes = new SailRepository(new ShaclSail(new MemoryStore()));
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			connection.add(new StringReader(shapesString), RDFFormat.TRIG);
			connection.commit();
		}
		return shapes;
	}

}
