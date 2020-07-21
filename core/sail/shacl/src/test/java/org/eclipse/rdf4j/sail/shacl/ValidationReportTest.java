/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl;

import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.junit.Test;

public class ValidationReportTest {

	ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	public void simpleFirstTest() throws IOException {
		SailRepository shaclSail = Utils.getInitializedShaclRepository("shacl.ttl", false);

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDF.SUBJECT, RDF.TYPE, RDFS.RESOURCE);

			connection.commit();
			fail();

		} catch (RepositoryException e) {
			ShaclSailValidationException cause = (ShaclSailValidationException) e.getCause();
			Model actual = cause.validationReportAsModel();

			actual.setNamespace(RDF.PREFIX, RDF.NAMESPACE);
			actual.setNamespace(RDFS.PREFIX, RDFS.NAMESPACE);
			actual.setNamespace("ex", "http://example.com/ns#");

			Rio.write(actual, System.out, RDFFormat.TURTLE);

			Model expected = Rio.parse(new StringReader("" + "@prefix ex: <http://example.com/ns#> .\n" +
					"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
					"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" +
					"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
					"\n" +
					"_:node1ebr5j3f8x20 a sh:ValidationReport;\n" +
					"  <http://rdf4j.org/schema/rdf4j#truncated> false;\n" +
					"  sh:conforms false;\n" +
					"  sh:result _:node1ebr5j3f8x21 .\n" +
					"\n" +
					"_:node1ebr5j3f8x21 a sh:ValidationResult;\n" +
					"  sh:focusNode rdf:subject;\n" +
					"  sh:resultPath rdfs:label;\n" +
					"  sh:sourceConstraintComponent sh:MinCountConstraintComponent;\n" +
					"  sh:sourceShape ex:PersonShapeProperty .\n" +
					"\n" +
					"_:node1ebr5j3f8x20 sh:result _:node1ebr5j3f8x22 .\n" +
					"\n" +
					"_:node1ebr5j3f8x22 a sh:ValidationResult;\n" +
					"  sh:focusNode rdfs:Class;\n" +
					"  sh:resultPath rdfs:label;\n" +
					"  sh:sourceConstraintComponent sh:MinCountConstraintComponent;\n" +
					"  sh:sourceShape ex:PersonShapeProperty .\n" + ""), "", RDFFormat.TURTLE);

			assertTrue(Models.isomorphic(expected, actual));

		}
	}

	@Test
	public void withoutPathTest() throws IOException {
		SailRepository shaclSail = Utils.getInitializedShaclRepository("shaclValidateTarget.ttl", false);

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();
			connection.add(vf.createIRI("http://example.com/ns#", "node1"), RDF.TYPE,
					vf.createIRI("http://example.com/ns#", "SecondTarget"));
			connection.commit();

			fail();

		} catch (RepositoryException e) {
			ShaclSailValidationException cause = (ShaclSailValidationException) e.getCause();
			Model actual = cause.validationReportAsModel();

			actual.setNamespace(RDF.PREFIX, RDF.NAMESPACE);
			actual.setNamespace(RDFS.PREFIX, RDFS.NAMESPACE);
			actual.setNamespace("ex", "http://example.com/ns#");

			Rio.write(actual, System.out, RDFFormat.TURTLE);

			Model expected = Rio.parse(new StringReader("" +
					"@prefix ex: <http://example.com/ns#> .\n" +
					"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
					"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
					"\n" +
					"_:node1ecfgke5gx80 a sh:ValidationReport;\n" +
					"  <http://rdf4j.org/schema/rdf4j#truncated> false;\n" +
					"  sh:conforms false;\n" +
					"  sh:result _:node1ecfgke5gx81 .\n" +
					"\n" +
					"_:node1ecfgke5gx81 a sh:ValidationResult;\n" +
					"  sh:focusNode ex:node1;\n" +
					"  sh:sourceConstraintComponent sh:ClassConstraintComponent;\n" +
					"  sh:sourceShape ex:PersonShape;\n" +
					"  sh:value ex:node1 ."
					+ ""), "", RDFFormat.TURTLE);

			assertTrue(Models.isomorphic(expected, actual));

		}
	}

	@Test
	public void nestedLogicalOrSupport() throws IOException {

		SailRepository shaclSail = Utils.getInitializedShaclRepository("test-cases/or/datatype/shacl.ttl", false);

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();
			connection.prepareUpdate(IOUtils.toString(ValidationReportTest.class.getClassLoader()
					.getResourceAsStream("test-cases/or/datatype/invalid/case1/query1.rq"), StandardCharsets.UTF_8))
					.execute();
			connection.commit();
			fail();

		} catch (RepositoryException e) {
			ShaclSailValidationException cause = (ShaclSailValidationException) e.getCause();
			Model actual = cause.validationReportAsModel();

			actual.setNamespace(RDF.PREFIX, RDF.NAMESPACE);
			actual.setNamespace(RDFS.PREFIX, RDFS.NAMESPACE);
			actual.setNamespace("ex", "http://example.com/ns#");

			WriterConfig writerConfig = new WriterConfig();
			writerConfig.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
			writerConfig.set(BasicWriterSettings.PRETTY_PRINT, true);

			Rio.write(actual, System.out, RDFFormat.TURTLE, writerConfig);

			Model expected = Rio.parse(new StringReader(""
					+ "@prefix ex: <http://example.com/ns#> .\n" +
					"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
					"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
					"\n" +
					"[] a sh:ValidationReport;\n" +
					"  <http://rdf4j.org/schema/rdf4j#truncated> false;\n" +
					"  sh:conforms false;\n" +
					"  sh:result [ a sh:ValidationResult;\n" +
					"      sh:detail [ a sh:ValidationResult;\n" +
					"          sh:detail [ a sh:ValidationResult;\n" +
					"              sh:focusNode ex:validPerson1;\n" +
					"              sh:resultPath ex:age;\n" +
					"              sh:sourceConstraintComponent sh:DatatypeConstraintComponent;\n" +
					"              sh:sourceShape ex:personShapeAgeLong;\n" +
					"              sh:value \"abc\"\n" +
					"            ];\n" +
					"          sh:focusNode ex:validPerson1;\n" +
					"          sh:resultPath ex:age;\n" +
					"          sh:sourceConstraintComponent sh:DatatypeConstraintComponent;\n" +
					"          sh:sourceShape ex:personShapeAgeInteger;\n" +
					"          sh:value \"abc\"\n" +
					"        ];\n" +
					"      sh:focusNode ex:validPerson1;\n" +
					"      sh:resultPath ex:age;\n" +
					"      sh:sourceConstraintComponent sh:OrConstraintComponent;\n" +
					"      sh:sourceShape ex:personShapeOr\n" +
					"    ] ." + ""), "", RDFFormat.TURTLE);

			assertTrue(Models.isomorphic(expected, actual));

		}
	}

}
