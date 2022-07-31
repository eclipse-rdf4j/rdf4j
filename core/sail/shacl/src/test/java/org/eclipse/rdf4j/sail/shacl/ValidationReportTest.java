/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl;

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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ValidationReportTest {

	ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	public void simpleFirstTest() throws IOException {
		SailRepository shaclSail = Utils.getInitializedShaclRepository("shacl.trig");

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDF.SUBJECT, RDF.TYPE, RDFS.RESOURCE);

			connection.commit();
			Assertions.fail();

		} catch (RepositoryException e) {
			ShaclSailValidationException cause = (ShaclSailValidationException) e.getCause();
			Model actual = cause.validationReportAsModel();

			actual.setNamespace(RDF.PREFIX, RDF.NAMESPACE);
			actual.setNamespace(RDFS.PREFIX, RDFS.NAMESPACE);
			actual.setNamespace("ex", "http://example.com/ns#");

			ShaclSailValidationReportHelper.printValidationReport(e, System.out);

			Model expected = Rio.parse(new StringReader("" +
					"@prefix rsx: <http://rdf4j.org/shacl-extensions#> .\n" +
					"@prefix rdf4j: <http://rdf4j.org/schema/rdf4j#> .\n" +
					"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
					"\n" +
					"[] a sh:ValidationReport;\n" +
					"  sh:conforms false;\n" +
					"  rdf4j:truncated false;\n" +
					"  sh:result [ a sh:ValidationResult;\n" +
					"      sh:focusNode <http://www.w3.org/2000/01/rdf-schema#Class>;\n" +
					"      rsx:shapesGraph rdf4j:SHACLShapeGraph;\n" +
					"      sh:resultPath <http://www.w3.org/2000/01/rdf-schema#label>;\n" +
					"      sh:sourceConstraintComponent sh:MinCountConstraintComponent;\n" +
					"      sh:resultSeverity sh:Violation;\n" +
					"      sh:sourceShape <http://example.com/ns#PersonShapeProperty>\n" +
					"    ], [ a sh:ValidationResult;\n" +
					"      sh:focusNode <http://www.w3.org/1999/02/22-rdf-syntax-ns#subject>;\n" +
					"      rsx:shapesGraph rdf4j:SHACLShapeGraph;\n" +
					"      sh:resultPath <http://www.w3.org/2000/01/rdf-schema#label>;\n" +
					"      sh:sourceConstraintComponent sh:MinCountConstraintComponent;\n" +
					"      sh:resultSeverity sh:Violation;\n" +
					"      sh:sourceShape <http://example.com/ns#PersonShapeProperty>\n" +
					"    ] .\n" +
					"\n" +
					"<http://example.com/ns#PersonShapeProperty> a sh:PropertyShape;\n" +
					"  sh:path <http://www.w3.org/2000/01/rdf-schema#label>;\n" +
					"  sh:minCount 1 ." +
					""), "", RDFFormat.TURTLE);

			Assertions.assertTrue(Models.isomorphic(expected, actual));

		} finally {
			shaclSail.shutDown();
		}
	}

	@Test
	public void withoutPathTest() throws IOException {
		SailRepository shaclSail = Utils.getInitializedShaclRepository("shaclValidateTarget.trig");

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();
			connection.add(vf.createIRI("http://example.com/ns#", "node1"), RDF.TYPE,
					vf.createIRI("http://example.com/ns#", "SecondTarget"));
			connection.commit();

			Assertions.fail();

		} catch (RepositoryException e) {
			ShaclSailValidationException cause = (ShaclSailValidationException) e.getCause();
			Model actual = cause.validationReportAsModel();

			actual.setNamespace(RDF.PREFIX, RDF.NAMESPACE);
			actual.setNamespace(RDFS.PREFIX, RDFS.NAMESPACE);
			actual.setNamespace("ex", "http://example.com/ns#");

			ShaclSailValidationReportHelper.printValidationReport(e, System.out);

			Model expected = Rio.parse(new StringReader("" +
					"@prefix rsx: <http://rdf4j.org/shacl-extensions#> .\n" +
					"@prefix rdf4j: <http://rdf4j.org/schema/rdf4j#> .\n" +
					"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
					"\n" +
					"[] a sh:ValidationReport;\n" +
					"  sh:conforms false;\n" +
					"  rdf4j:truncated false;\n" +
					"  sh:result [ a sh:ValidationResult;\n" +
					"      sh:focusNode <http://example.com/ns#node1>;\n" +
					"      rsx:shapesGraph rdf4j:SHACLShapeGraph;\n" +
					"      sh:value <http://example.com/ns#node1>;\n" +
					"      sh:sourceConstraintComponent sh:ClassConstraintComponent;\n" +
					"      sh:resultSeverity sh:Violation;\n" +
					"      sh:sourceShape <http://example.com/ns#PersonShape>\n" +
					"    ] .\n" +
					"\n" +
					"<http://example.com/ns#PersonShape> a sh:NodeShape;\n" +
					"  sh:targetClass <http://example.com/ns#Person>, <http://example.com/ns#SecondTarget>;\n" +
					"  sh:class <http://example.com/ns#Person> .\n" +
					""), "", RDFFormat.TRIG);

			Assertions.assertTrue(Models.isomorphic(expected, actual));

		} finally {
			shaclSail.shutDown();
		}
	}

	@Test
	public void nestedLogicalOrSupport() throws IOException {

		SailRepository shaclSail = Utils.getInitializedShaclRepository("test-cases/or/datatype/shacl.trig");

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();
			connection.prepareUpdate(IOUtils.toString(ValidationReportTest.class.getClassLoader()
					.getResourceAsStream("test-cases/or/datatype/invalid/case1/query1.rq"), StandardCharsets.UTF_8))
					.execute();
			connection.commit();
			Assertions.fail();

		} catch (RepositoryException e) {
			ShaclSailValidationException cause = (ShaclSailValidationException) e.getCause();
			Model actual = cause.validationReportAsModel();

			actual.setNamespace(RDF.PREFIX, RDF.NAMESPACE);
			actual.setNamespace(RDFS.PREFIX, RDFS.NAMESPACE);
			actual.setNamespace("ex", "http://example.com/ns#");

			ShaclSailValidationReportHelper.printValidationReport(e, System.out);

			Model expected = Rio.parse(new StringReader("" +
					"@prefix rsx: <http://rdf4j.org/shacl-extensions#> .\n" +
					"@prefix rdf4j: <http://rdf4j.org/schema/rdf4j#> .\n" +
					"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
					"\n" +
					"[] a sh:ValidationReport;\n" +
					"  sh:conforms false;\n" +
					"  rdf4j:truncated false;\n" +
					"  sh:result [ a sh:ValidationResult;\n" +
					"      sh:focusNode <http://example.com/ns#validPerson1>;\n" +
					"      rsx:shapesGraph rdf4j:SHACLShapeGraph;\n" +
					"      sh:value \"abc\";\n" +
					"      sh:resultPath <http://example.com/ns#age>;\n" +
					"      sh:sourceConstraintComponent sh:OrConstraintComponent;\n" +
					"      sh:resultSeverity sh:Violation;\n" +
					"      sh:sourceShape <http://example.com/ns#personShapeOr>\n" +
					"    ] .\n" +
					"\n" +
					"<http://example.com/ns#personShapeOr> a sh:PropertyShape;\n" +
					"  sh:path <http://example.com/ns#age>;\n" +
					"  sh:or (<http://example.com/ns#personShapeAgeInteger> <http://example.com/ns#personShapeAgeLong>) .\n"
					+
					"\n" +
					"<http://example.com/ns#personShapeAgeInteger> a sh:NodeShape;\n" +
					"  sh:datatype <http://www.w3.org/2001/XMLSchema#integer> .\n" +
					"\n" +
					"<http://example.com/ns#personShapeAgeLong> a sh:NodeShape;\n" +
					"  sh:datatype <http://www.w3.org/2001/XMLSchema#long> .\n" +
					""), "", RDFFormat.TURTLE);

			Assertions.assertTrue(Models.isomorphic(expected, actual));

		} finally {
			shaclSail.shutDown();
		}
	}

	@Test
	public void testHasValueIn() throws IOException {

		SailRepository shaclSail = Utils.getInitializedShaclRepository("test-cases/hasValueIn/simple/shacl.trig");

		ShaclSail sail = (ShaclSail) shaclSail.getSail();
		sail.setDashDataShapes(true);

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();
			connection.prepareUpdate(IOUtils.toString(ValidationReportTest.class.getClassLoader()
					.getResourceAsStream("test-cases/hasValueIn/simple/invalid/case1/query1.rq"),
					StandardCharsets.UTF_8))
					.execute();
			connection.commit();
			Assertions.fail();

		} catch (RepositoryException e) {
			ShaclSailValidationException cause = (ShaclSailValidationException) e.getCause();
			Model actual = cause.validationReportAsModel();

			actual.setNamespace(RDF.PREFIX, RDF.NAMESPACE);
			actual.setNamespace(RDFS.PREFIX, RDFS.NAMESPACE);
			actual.setNamespace("ex", "http://example.com/ns#");

			ShaclSailValidationReportHelper.printValidationReport(e, System.out);

			Model expected = Rio.parse(new StringReader("" +
					"@prefix rsx: <http://rdf4j.org/shacl-extensions#> .\n" +
					"@prefix rdf4j: <http://rdf4j.org/schema/rdf4j#> .\n" +
					"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
					"\n" +
					"[] a sh:ValidationReport;\n" +
					"  sh:conforms false;\n" +
					"  rdf4j:truncated false;\n" +
					"  sh:result [ a sh:ValidationResult;\n" +
					"      sh:focusNode <http://example.com/ns#validPerson1>;\n" +
					"      rsx:shapesGraph rdf4j:SHACLShapeGraph;\n" +
					"      sh:resultPath <http://example.com/ns#knows>;\n" +
					"      sh:sourceConstraintComponent sh:HasValueConstraintComponent;\n" +
					"      sh:resultSeverity sh:Violation;\n" +
					"      sh:sourceShape [ a sh:PropertyShape;\n" +
					"          sh:path <http://example.com/ns#knows>;\n" +
					"          <http://datashapes.org/dash#hasValueIn> (<http://example.com/ns#peter> <http://example.com/ns#mary>\n"
					+
					"              <http://example.com/ns#kate>)\n" +
					"        ]\n" +
					"    ] ." +
					""), "", RDFFormat.TRIG);

			Assertions.assertTrue(Models.isomorphic(expected, actual));

		} finally {
			shaclSail.shutDown();
		}
	}

	@Test
	public void testHasValue() throws IOException {

		SailRepository shaclSail = Utils.getInitializedShaclRepository("test-cases/hasValue/simple/shacl.trig");

		ShaclSail sail = (ShaclSail) shaclSail.getSail();
		sail.setDashDataShapes(true);

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();
			connection.prepareUpdate(IOUtils.toString(ValidationReportTest.class.getClassLoader()
					.getResourceAsStream("test-cases/hasValue/simple/invalid/case1/query1.rq"),
					StandardCharsets.UTF_8))
					.execute();
			connection.commit();
			Assertions.fail();

		} catch (RepositoryException e) {
			ShaclSailValidationException cause = (ShaclSailValidationException) e.getCause();
			Model actual = cause.validationReportAsModel();

			actual.setNamespace(RDF.PREFIX, RDF.NAMESPACE);
			actual.setNamespace(RDFS.PREFIX, RDFS.NAMESPACE);
			actual.setNamespace("ex", "http://example.com/ns#");

			ShaclSailValidationReportHelper.printValidationReport(e, System.out);

			Model expected = Rio.parse(new StringReader(""
					+ "@prefix rsx: <http://rdf4j.org/shacl-extensions#> .\n" +
					"@prefix rdf4j: <http://rdf4j.org/schema/rdf4j#> .\n" +
					"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
					"\n" +
					"[] a sh:ValidationReport;\n" +
					"  sh:conforms false;\n" +
					"  rdf4j:truncated false;\n" +
					"  sh:result [ a sh:ValidationResult;\n" +
					"      sh:focusNode <http://example.com/ns#validPerson1>;\n" +
					"      rsx:shapesGraph rdf4j:SHACLShapeGraph;\n" +
					"      sh:resultPath <http://example.com/ns#knows>;\n" +
					"      sh:sourceConstraintComponent sh:HasValueConstraintComponent;\n" +
					"      sh:resultSeverity sh:Violation;\n" +
					"      sh:sourceShape [ a sh:PropertyShape;\n" +
					"          sh:path <http://example.com/ns#knows>;\n" +
					"          sh:hasValue <http://example.com/ns#peter>\n" +
					"        ]\n" +
					"    ] ."), "", RDFFormat.TRIG);

			Assertions.assertTrue(Models.isomorphic(expected, actual));

		} finally {
			shaclSail.shutDown();
		}
	}

}
