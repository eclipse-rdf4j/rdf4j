/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSailValidationReportHelper;
import org.eclipse.rdf4j.sail.shacl.ShaclValidator;
import org.junit.jupiter.api.Test;

public class ShaclValidatorSparqlMessagesTest {

	@Test
	public void multipleSparqlConstraintsDifferentMessages() throws Exception {

		String shapesTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"\n" +
				"ex:PersonShape a sh:NodeShape ;\n" +
				"  sh:targetClass ex:Person ;\n" +
				"  sh:sparql [\n" +
				"    a sh:SPARQLConstraint ;\n" +
				"    sh:message \"Missing ex:name.\" ;\n" +
				"    sh:select \"\"\"\n" +
				"      PREFIX ex: <http://example.com/ns#>\n" +
				"      SELECT $this WHERE {\n" +
				"        FILTER NOT EXISTS { $this ex:name ?name }\n" +
				"      }\n" +
				"    \"\"\" ;\n" +
				"  ] ;\n" +
				"  sh:sparql [\n" +
				"    a sh:SPARQLConstraint ;\n" +
				"    sh:message \"Invalid ex:age.\" ;\n" +
				"    sh:select \"\"\"\n" +
				"      PREFIX ex: <http://example.com/ns#>\n" +
				"      PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
				"      SELECT $this WHERE {\n" +
				"        $this ex:age ?age .\n" +
				"        FILTER ( datatype(?age) != xsd:integer )\n" +
				"      }\n" +
				"    \"\"\" ;\n" +
				"  ] .\n";

		String dataTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"\n" +
				"ex:alice a ex:Person ;\n" +
				"  ex:age \"twenty\"^^xsd:string .\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesTtl), RDFFormat.TURTLE);
		}

		SailRepository data = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(new StringReader(dataTtl), RDFFormat.TURTLE);
		}

		ValidationReport report = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(report.conforms());
		assertEquals(2, report.getValidationResult().size());

		assertResultMessagesMatchConstraintMessages(report);
	}

	@Test
	public void multipleMessagesPerConstraintAreAllReported() throws Exception {

		String shapesTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"\n" +
				"ex:PersonShape a sh:NodeShape ;\n" +
				"  sh:targetClass ex:Person ;\n" +
				"  sh:sparql [\n" +
				"    a sh:SPARQLConstraint ;\n" +
				"    sh:message \"Name missing\"@en, \"Naam ontbreekt\"@nl ;\n" +
				"    sh:select \"\"\"\n" +
				"      PREFIX ex: <http://example.com/ns#>\n" +
				"      SELECT $this WHERE {\n" +
				"        FILTER NOT EXISTS { $this ex:name ?name }\n" +
				"      }\n" +
				"    \"\"\" ;\n" +
				"  ] ;\n" +
				"  sh:sparql [\n" +
				"    a sh:SPARQLConstraint ;\n" +
				"    sh:message \"Age must be integer\" ;\n" +
				"    sh:select \"\"\"\n" +
				"      PREFIX ex: <http://example.com/ns#>\n" +
				"      PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
				"      SELECT $this WHERE {\n" +
				"        $this ex:age ?age .\n" +
				"        FILTER ( datatype(?age) != xsd:integer )\n" +
				"      }\n" +
				"    \"\"\" ;\n" +
				"  ] .\n";

		String dataTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"\n" +
				"ex:bob a ex:Person ;\n" +
				"  ex:age \"old\"^^xsd:string .\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesTtl), RDFFormat.TURTLE);
		}

		SailRepository data = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(new StringReader(dataTtl), RDFFormat.TURTLE);
		}

		ValidationReport report = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(report.conforms());

		assertResultMessagesMatchConstraintMessages(report);
	}

	@Test
	public void propertyShapeWithMultipleSparqlConstraintsReportsAllMessages() throws Exception {

		String shapesTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"\n" +
				"ex:PersonShape a sh:NodeShape ;\n" +
				"  sh:targetClass ex:Person ;\n" +
				"      sh:message \"test\" ;\n" +
				"  sh:property [\n" +
				"    sh:path ex:knows ;\n" +
				"    sh:sparql [\n" +
				"      a sh:SPARQLConstraint ;\n" +
				"      sh:message \"ex:knows values must be IRIs.\" ;\n" +
				"      sh:select \"\"\"\n" +
				"        PREFIX ex: <http://example.com/ns#>\n" +
				"        SELECT $this ?value WHERE {\n" +
				"          $this ex:knows ?value .\n" +
				"          FILTER ( !isIRI(?value) )\n" +
				"        }\n" +
				"      \"\"\" ;\n" +
				"    ] ;\n" +
				"    sh:sparql [\n" +
				"      a sh:SPARQLConstraint ;\n" +
				"      sh:message \"ex:knows values must be ex:Person.\" ;\n" +
				"      sh:select \"\"\"\n" +
				"        PREFIX ex: <http://example.com/ns#>\n" +
				"        SELECT $this ?value WHERE {\n" +
				"          $this ex:knows ?value .\n" +
				"          FILTER NOT EXISTS { ?value a ex:Person }\n" +
				"        }\n" +
				"      \"\"\" ;\n" +
				"    ]\n" +
				"  ] .\n";

		String dataTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"\n" +
				"ex:alice a ex:Person ;\n" +
				"  ex:knows \"Bob\" , ex:charlie .\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesTtl), RDFFormat.TURTLE);
		}

		SailRepository data = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(new StringReader(dataTtl), RDFFormat.TURTLE);
		}

		ValidationReport report = ShaclValidator.validate(data.getSail(), shapes.getSail());
		System.out.println(report);
		assertFalse(report.conforms());

		assertResultMessagesMatchConstraintMessages(report);
	}

	@Test
	public void personShapePositiveAgeAndNoSelfManageMessagesPerViolation() throws Exception {

		String shapesTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"\n" +
				"ex:PersonShape a sh:NodeShape ;\n" +
				"  sh:targetClass ex:Person ;\n" +
				"    sh:message \"a\", \"b\" ;\n" +
				"  sh:sparql [\n" +
				"    a sh:SPARQLConstraint ;\n" +
				"    sh:message \"Age must be positive.\" ;\n" +
				"    sh:select \"\"\"\n" +
				"      PREFIX ex: <http://example.com/ns#>\n" +
				"      SELECT $this WHERE {\n" +
				"        $this ex:age ?age .\n" +
				"        FILTER ( ?age <= 0 )\n" +
				"      }\n" +
				"    \"\"\" ;\n" +
				"  ] ;\n" +
				"  sh:sparql [\n" +
				"    a sh:SPARQLConstraint ;\n" +
				"    sh:message \"Person must not manage themselves.\" ;\n" +
				"    sh:select \"\"\"\n" +
				"      PREFIX ex: <http://example.com/ns#>\n" +
				"      SELECT $this WHERE {\n" +
				"        $this ex:manage $this .\n" +
				"      }\n" +
				"    \"\"\" ;\n" +
				"  ] .\n";

		String dataTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"\n" +
				"ex:alice a ex:Person ;\n" +
				"  ex:age \"-1\"^^xsd:integer ;\n" +
				"  ex:age \"-2\"^^xsd:integer ;\n" +
				"  ex:manage ex:alice .\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesTtl), RDFFormat.TURTLE);
		}

		SailRepository data = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(new StringReader(dataTtl), RDFFormat.TURTLE);
		}

		ValidationReport report = ShaclValidator.validate(data.getSail(), shapes.getSail());
		ShaclSailValidationReportHelper.printValidationReport(report, System.out);
		assertFalse(report.conforms());
		assertEquals(3, report.getValidationResult().size());

		assertResultMessagesMatchConstraintMessages(report);
	}

	@Test
	public void messageBindingOverridesConstraintMessages() throws Exception {

		String shapesTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"\n" +
				"ex:PersonShape a sh:NodeShape ;\n" +
				"  sh:targetClass ex:Person ;\n" +
				"  sh:sparql [\n" +
				"    a sh:SPARQLConstraint ;\n" +
				"    sh:message \"Fallback message\" ;\n" +
				"    sh:select \"\"\"\n" +
				"      PREFIX ex: <http://example.com/ns#>\n" +
				"      SELECT $this ?message WHERE {\n" +
				"        FILTER NOT EXISTS { $this ex:name ?n }\n" +
				"        BIND(\"Bound message\" AS ?message)\n" +
				"      }\n" +
				"    \"\"\" ;\n" +
				"  ] .\n";

		String dataTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"\n" +
				"ex:alice a ex:Person .\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesTtl), RDFFormat.TURTLE);
		}

		SailRepository data = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(new StringReader(dataTtl), RDFFormat.TURTLE);
		}

		ValidationReport report = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(report.conforms());
		assertEquals(1, report.getValidationResult().size());

		assertAllResultsHaveMessages(report, Set.of("Bound message"));
	}

	@Test
	public void messageBindingUsedWhenNoConstraintMessages() throws Exception {

		String shapesTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"\n" +
				"ex:PersonShape a sh:NodeShape ;\n" +
				"  sh:targetClass ex:Person ;\n" +
				"  sh:sparql [\n" +
				"    a sh:SPARQLConstraint ;\n" +
				"    sh:select \"\"\"\n" +
				"      PREFIX ex: <http://example.com/ns#>\n" +
				"      SELECT $this ?message WHERE {\n" +
				"        FILTER NOT EXISTS { $this ex:name ?n }\n" +
				"        BIND(\"Only bound message\"@en AS ?message)\n" +
				"      }\n" +
				"    \"\"\" ;\n" +
				"  ] .\n";

		String dataTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"\n" +
				"ex:alice a ex:Person .\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesTtl), RDFFormat.TURTLE);
		}

		SailRepository data = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(new StringReader(dataTtl), RDFFormat.TURTLE);
		}

		ValidationReport report = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(report.conforms());
		assertEquals(1, report.getValidationResult().size());

		assertAllResultsHaveMessages(report, Set.of("Only bound message@en"));
	}

	@Test
	public void messageTemplatesSubstituteSelectVariables() throws Exception {

		String shapesTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"\n" +
				"ex:PersonShape a sh:NodeShape ;\n" +
				"  sh:targetClass ex:Person ;\n" +
				"  sh:sparql [\n" +
				"    a sh:SPARQLConstraint ;\n" +
				"    sh:message \"Invalid age {?value} for {$this}\" ;\n" +
				"    sh:select \"\"\"\n" +
				"      PREFIX ex: <http://example.com/ns#>\n" +
				"      PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
				"      SELECT $this ?value WHERE {\n" +
				"        $this ex:age ?value .\n" +
				"        FILTER ( datatype(?value) != xsd:integer )\n" +
				"      }\n" +
				"    \"\"\" ;\n" +
				"  ] .\n";

		String dataTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"\n" +
				"ex:bob a ex:Person ;\n" +
				"  ex:age \"old\"^^xsd:string .\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesTtl), RDFFormat.TURTLE);
		}

		SailRepository data = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(new StringReader(dataTtl), RDFFormat.TURTLE);
		}

		ValidationReport report = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(report.conforms());
		assertEquals(1, report.getValidationResult().size());

		assertAllResultsHaveMessages(report, Set.of("Invalid age old for http://example.com/ns#bob"));
	}

	@Test
	public void noMessagesProducesNoResultMessage() throws Exception {

		String shapesTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"\n" +
				"ex:PersonShape a sh:NodeShape ;\n" +
				"  sh:targetClass ex:Person ;\n" +
				"  sh:sparql [\n" +
				"    a sh:SPARQLConstraint ;\n" +
				"    sh:select \"\"\"\n" +
				"      PREFIX ex: <http://example.com/ns#>\n" +
				"      SELECT $this WHERE {\n" +
				"        FILTER NOT EXISTS { $this ex:name ?n }\n" +
				"      }\n" +
				"    \"\"\" ;\n" +
				"  ] .\n";

		String dataTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"\n" +
				"ex:alice a ex:Person .\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesTtl), RDFFormat.TURTLE);
		}

		SailRepository data = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(new StringReader(dataTtl), RDFFormat.TURTLE);
		}

		ValidationReport report = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(report.conforms());
		assertEquals(1, report.getValidationResult().size());

		Model model = report.asModel();
		for (ValidationResult result : report.getValidationResult()) {
			Set<String> resultMessages = toMessageSet(
					model.filter(result.getId(), SHACL.RESULT_MESSAGE, null).objects());
			assertTrue(resultMessages.isEmpty());
		}
	}

	@Test
	public void multipleTemplateMessagesAreAllSubstitutedAndReported() throws Exception {

		String shapesTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"\n" +
				"ex:PersonShape a sh:NodeShape ;\n" +
				"  sh:targetClass ex:Person ;\n" +
				"  sh:sparql [\n" +
				"    a sh:SPARQLConstraint ;\n" +
				"    sh:message \"Bad age {?value}\"@en , \"Leeftijd ongeldig {?value}\"@nl ;\n" +
				"    sh:select \"\"\"\n" +
				"      PREFIX ex: <http://example.com/ns#>\n" +
				"      PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
				"      SELECT $this ?value WHERE {\n" +
				"        $this ex:age ?value .\n" +
				"        FILTER ( datatype(?value) != xsd:integer )\n" +
				"      }\n" +
				"    \"\"\" ;\n" +
				"  ] .\n";

		String dataTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"\n" +
				"ex:bob a ex:Person ;\n" +
				"  ex:age \"old\"^^xsd:string ;\n" +
				"  ex:age \"ancient\"^^xsd:string .\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesTtl), RDFFormat.TURTLE);
		}

		SailRepository data = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(new StringReader(dataTtl), RDFFormat.TURTLE);
		}

		ValidationReport report = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(report.conforms());
		assertEquals(2, report.getValidationResult().size());

		Model model = report.asModel();
		for (ValidationResult result : report.getValidationResult()) {
			String valueLabel = model.filter(result.getId(), SHACL.VALUE, null)
					.objects()
					.stream()
					.findFirst()
					.map(Value::stringValue)
					.orElseThrow();

			Set<String> expected = Set.of(
					"Bad age " + valueLabel + "@en",
					"Leeftijd ongeldig " + valueLabel + "@nl");

			Set<String> actual = toMessageSet(model.filter(result.getId(), SHACL.RESULT_MESSAGE, null).objects());
			assertEquals(expected, actual);
		}
	}

	@Test
	public void unboundTemplateVariablesRemainUnchanged() throws Exception {

		String shapesTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"\n" +
				"ex:PersonShape a sh:NodeShape ;\n" +
				"  sh:targetClass ex:Person ;\n" +
				"  sh:sparql [\n" +
				"    a sh:SPARQLConstraint ;\n" +
				"    sh:message \"Missing value {?missing} for {$this}\" ;\n" +
				"    sh:select \"\"\"\n" +
				"      PREFIX ex: <http://example.com/ns#>\n" +
				"      SELECT $this WHERE {\n" +
				"        FILTER NOT EXISTS { $this ex:name ?n }\n" +
				"      }\n" +
				"    \"\"\" ;\n" +
				"  ] .\n";

		String dataTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"\n" +
				"ex:alice a ex:Person .\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesTtl), RDFFormat.TURTLE);
		}

		SailRepository data = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(new StringReader(dataTtl), RDFFormat.TURTLE);
		}

		ValidationReport report = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(report.conforms());
		assertEquals(1, report.getValidationResult().size());

		assertAllResultsHaveMessages(report, Set.of("Missing value {?missing} for http://example.com/ns#alice"));
	}

	@Test
	public void repeatedTemplatePlaceholdersAreAllReplaced() throws Exception {

		String shapesTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"\n" +
				"ex:PersonShape a sh:NodeShape ;\n" +
				"  sh:targetClass ex:Person ;\n" +
				"  sh:sparql [\n" +
				"    a sh:SPARQLConstraint ;\n" +
				"    sh:message \"Value {?value} repeats {?value}\" ;\n" +
				"    sh:select \"\"\"\n" +
				"      PREFIX ex: <http://example.com/ns#>\n" +
				"      PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
				"      SELECT $this ?value WHERE {\n" +
				"        $this ex:age ?value .\n" +
				"        FILTER ( datatype(?value) != xsd:integer )\n" +
				"      }\n" +
				"    \"\"\" ;\n" +
				"  ] .\n";

		String dataTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"\n" +
				"ex:bob a ex:Person ;\n" +
				"  ex:age \"old\"^^xsd:string .\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesTtl), RDFFormat.TURTLE);
		}

		SailRepository data = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(new StringReader(dataTtl), RDFFormat.TURTLE);
		}

		ValidationReport report = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(report.conforms());
		assertEquals(1, report.getValidationResult().size());

		assertAllResultsHaveMessages(report, Set.of("Value old repeats old"));
	}

	@Test
	public void messageBindingNonLiteralConvertedToStringLiteral() throws Exception {

		String shapesTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"\n" +
				"ex:PersonShape a sh:NodeShape ;\n" +
				"  sh:targetClass ex:Person ;\n" +
				"  sh:sparql [\n" +
				"    a sh:SPARQLConstraint ;\n" +
				"    sh:message \"Fallback\" ;\n" +
				"    sh:select \"\"\"\n" +
				"      PREFIX ex: <http://example.com/ns#>\n" +
				"      SELECT $this ?message WHERE {\n" +
				"        FILTER NOT EXISTS { $this ex:name ?n }\n" +
				"        BIND(ex:msg1 AS ?message)\n" +
				"      }\n" +
				"    \"\"\" ;\n" +
				"  ] .\n";

		String dataTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"\n" +
				"ex:alice a ex:Person .\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesTtl), RDFFormat.TURTLE);
		}

		SailRepository data = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(new StringReader(dataTtl), RDFFormat.TURTLE);
		}

		ValidationReport report = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(report.conforms());
		assertEquals(1, report.getValidationResult().size());

		assertAllResultsHaveMessages(report, Set.of("http://example.com/ns#msg1"));
	}

	@Test
	public void preboundShapesGraphAvailableInSparqlConstraints() throws Exception {

		String shapesTrig = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"@prefix rdf4j: <http://rdf4j.org/schema/rdf4j#> .\n" +
				"\n" +
				"rdf4j:nil sh:shapesGraph ex:shapesGraph .\n" +
				"\n" +
				"ex:shapesGraph {\n" +
				"  ex:PersonShape a sh:NodeShape ;\n" +
				"    sh:targetClass ex:Person ;\n" +
				"    sh:sparql [\n" +
				"      a sh:SPARQLConstraint ;\n" +
				"      sh:select \"\"\"\n" +
				"        PREFIX ex: <http://example.com/ns#>\n" +
				"        SELECT $this ?message WHERE {\n" +
				"          FILTER NOT EXISTS { $this ex:name ?n }\n" +
				"          BIND(CONCAT(\"sg=\", STR($shapesGraph)) AS ?message)\n" +
				"        }\n" +
				"      \"\"\" ;\n" +
				"    ] .\n" +
				"}\n";

		String dataTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"\n" +
				"ex:alice a ex:Person .\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesTrig), RDFFormat.TRIG);
		}

		SailRepository data = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(new StringReader(dataTtl), RDFFormat.TURTLE);
		}

		ValidationReport report = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(report.conforms());
		assertEquals(1, report.getValidationResult().size());

		assertAllResultsHaveMessages(report, Set.of("sg=http://example.com/ns#shapesGraph"));
	}

	@Test
	public void preboundCurrentShapeAvailableInSparqlConstraints() throws Exception {

		String shapesTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"\n" +
				"ex:PersonShape a sh:NodeShape ;\n" +
				"  sh:targetClass ex:Person ;\n" +
				"  sh:sparql [\n" +
				"    a sh:SPARQLConstraint ;\n" +
				"    sh:select \"\"\"\n" +
				"      PREFIX ex: <http://example.com/ns#>\n" +
				"      SELECT $this ?message WHERE {\n" +
				"        FILTER NOT EXISTS { $this ex:name ?n }\n" +
				"        BIND(CONCAT(\"cs=\", STR($currentShape)) AS ?message)\n" +
				"      }\n" +
				"    \"\"\" ;\n" +
				"  ] .\n";

		String dataTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"\n" +
				"ex:alice a ex:Person .\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesTtl), RDFFormat.TURTLE);
		}

		SailRepository data = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(new StringReader(dataTtl), RDFFormat.TURTLE);
		}

		ValidationReport report = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(report.conforms());
		assertEquals(1, report.getValidationResult().size());

		assertAllResultsHaveMessages(report, Set.of("cs=http://example.com/ns#PersonShape"));
	}

	@Test
	public void messageTemplatesSubstitutePreboundVariablesForNodeShapes() throws Exception {

		String shapesTrig = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"@prefix rdf4j: <http://rdf4j.org/schema/rdf4j#> .\n" +
				"\n" +
				"rdf4j:nil sh:shapesGraph ex:shapesGraph .\n" +
				"\n" +
				"ex:shapesGraph {\n" +
				"  ex:PersonShape a sh:NodeShape ;\n" +
				"    sh:targetClass ex:Person ;\n" +
				"    sh:sparql [\n" +
				"      a sh:SPARQLConstraint ;\n" +
				"      sh:message \"Graph {?shapesGraph} shape {?currentShape} focus {$this}\" ;\n" +
				"      sh:select \"\"\"\n" +
				"        PREFIX ex: <http://example.com/ns#>\n" +
				"        SELECT $this WHERE {\n" +
				"          FILTER NOT EXISTS { $this ex:name ?n }\n" +
				"        }\n" +
				"      \"\"\" ;\n" +
				"    ] .\n" +
				"}\n";

		String dataTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"\n" +
				"ex:alice a ex:Person .\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesTrig), RDFFormat.TRIG);
		}

		SailRepository data = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(new StringReader(dataTtl), RDFFormat.TURTLE);
		}

		ValidationReport report = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(report.conforms());
		assertEquals(1, report.getValidationResult().size());

		assertAllResultsHaveMessages(report,
				Set.of("Graph http://example.com/ns#shapesGraph shape http://example.com/ns#PersonShape focus http://example.com/ns#alice"));
	}

	@Test
	public void messageTemplatesSubstitutePreboundVariablesForPropertyShapes() throws Exception {

		String shapesTrig = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"@prefix rdf4j: <http://rdf4j.org/schema/rdf4j#> .\n" +
				"\n" +
				"rdf4j:nil sh:shapesGraph ex:shapesGraph .\n" +
				"\n" +
				"ex:shapesGraph {\n" +
				"  ex:PersonShape a sh:NodeShape ;\n" +
				"    sh:targetClass ex:Person ;\n" +
				"    sh:property [\n" +
				"      sh:path ex:age ;\n" +
				"      sh:sparql [\n" +
				"        a sh:SPARQLConstraint ;\n" +
				"        sh:message \"Graph {?shapesGraph} shape {?currentShape} focus {$this} value {?value}\" ;\n" +
				"        sh:select \"\"\"\n" +
				"          PREFIX ex: <http://example.com/ns#>\n" +
				"          PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
				"          SELECT $this ?value WHERE {\n" +
				"            $this $PATH ?value .\n" +
				"            FILTER ( ?value <= 0 )\n" +
				"          }\n" +
				"        \"\"\" ;\n" +
				"      ]\n" +
				"    ] .\n" +
				"}\n";

		String dataTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"\n" +
				"ex:alice a ex:Person ;\n" +
				"  ex:age \"-1\"^^xsd:integer .\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesTrig), RDFFormat.TRIG);
		}

		SailRepository data = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(new StringReader(dataTtl), RDFFormat.TURTLE);
		}

		ValidationReport report = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(report.conforms());
		assertEquals(1, report.getValidationResult().size());

		Model model = report.asModel();
		ValidationResult result = report.getValidationResult().iterator().next();
		Set<String> messages = toMessageSet(model.filter(result.getId(), SHACL.RESULT_MESSAGE, null).objects());
		assertEquals(1, messages.size());
		String message = messages.iterator().next();
		assertTrue(message.startsWith("Graph http://example.com/ns#shapesGraph shape "));
		assertTrue(message.contains(" focus http://example.com/ns#alice value -1"));
		assertFalse(message.contains("{?currentShape}"));
	}

	@Test
	public void messageTemplatesSubstitutePathPlaceholderForPropertyShapes() throws Exception {

		String shapesTrig = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"\n" +
				"ex:PersonShape a sh:NodeShape ;\n" +
				"  sh:targetClass ex:Person ;\n" +
				"  sh:property [\n" +
				"    sh:path ex:age ;\n" +
				"    sh:sparql [\n" +
				"      a sh:SPARQLConstraint ;\n" +
				"      sh:message \"Bad path {$PATH} and {?PATH}\" ;\n" +
				"      sh:select \"\"\"\n" +
				"        PREFIX ex: <http://example.com/ns#>\n" +
				"        PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
				"        SELECT $this ?value WHERE {\n" +
				"          $this $PATH ?value .\n" +
				"          FILTER ( ?value <= 0 )\n" +
				"        }\n" +
				"      \"\"\" ;\n" +
				"    ]\n" +
				"  ] .\n";

		String dataTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"\n" +
				"ex:alice a ex:Person ;\n" +
				"  ex:age \"-1\"^^xsd:integer .\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesTrig), RDFFormat.TURTLE);
		}

		SailRepository data = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(new StringReader(dataTtl), RDFFormat.TURTLE);
		}

		ValidationReport report = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(report.conforms());
		assertEquals(1, report.getValidationResult().size());

		assertAllResultsHaveMessages(report,
				Set.of("Bad path <http://example.com/ns#age> and <http://example.com/ns#age>"));
	}

	@Test
	public void pathPlaceholderWorksForInversePaths() throws Exception {

		String shapesTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"\n" +
				"ex:PersonShape a sh:NodeShape ;\n" +
				"  sh:targetClass ex:Person ;\n" +
				"  sh:property [\n" +
				"    sh:path [ sh:inversePath ex:knows ] ;\n" +
				"    sh:sparql [\n" +
				"      a sh:SPARQLConstraint ;\n" +
				"      sh:message \"Inverse knows values must be Person.\" ;\n" +
				"      sh:select \"\"\"\n" +
				"        PREFIX ex: <http://example.com/ns#>\n" +
				"        SELECT $this ?value WHERE {\n" +
				"          $this $PATH ?value .\n" +
				"          FILTER NOT EXISTS { ?value a ex:Person }\n" +
				"        }\n" +
				"      \"\"\" ;\n" +
				"    ]\n" +
				"  ] .\n";

		String dataTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"\n" +
				"ex:alice a ex:Person .\n" +
				"ex:bob ex:knows ex:alice .\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesTtl), RDFFormat.TURTLE);
		}

		SailRepository data = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(new StringReader(dataTtl), RDFFormat.TURTLE);
		}

		ValidationReport report = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(report.conforms());
		assertEquals(1, report.getValidationResult().size());

		assertAllResultsHaveMessages(report, Set.of("Inverse knows values must be Person."));
	}

	@Test
	public void illegalPathPlaceholderUseCausesFailure() throws Exception {

		String shapesTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"\n" +
				"ex:PersonShape a sh:NodeShape ;\n" +
				"  sh:targetClass ex:Person ;\n" +
				"  sh:property [\n" +
				"    sh:path ex:knows ;\n" +
				"    sh:sparql [\n" +
				"      a sh:SPARQLConstraint ;\n" +
				"      sh:select \"\"\"\n" +
				"        PREFIX ex: <http://example.com/ns#>\n" +
				"        SELECT $this WHERE {\n" +
				"          BIND($PATH AS ?p)\n" +
				"          FILTER NOT EXISTS { $this ex:name ?n }\n" +
				"        }\n" +
				"      \"\"\" ;\n" +
				"    ]\n" +
				"  ] .\n";

		String dataTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"\n" +
				"ex:alice a ex:Person .\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesTtl), RDFFormat.TURTLE);
		}

		SailRepository data = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(new StringReader(dataTtl), RDFFormat.TURTLE);
		}

		SailException ex = assertThrows(SailException.class,
				() -> ShaclValidator.validate(data.getSail(), shapes.getSail()));
		Throwable root = ex;
		while (root.getCause() != null) {
			root = root.getCause();
		}
		assertTrue(root instanceof IllegalStateException);
	}

	@Test
	public void nonIriPathBindingIgnoredForPropertyShapes() throws Exception {

		String shapesTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"\n" +
				"ex:PersonShape a sh:NodeShape ;\n" +
				"  sh:targetClass ex:Person ;\n" +
				"  sh:property [\n" +
				"    sh:path ex:knows ;\n" +
				"    sh:sparql [\n" +
				"      a sh:SPARQLConstraint ;\n" +
				"      sh:message \"knows values must be IRIs\" ;\n" +
				"      sh:select \"\"\"\n" +
				"        PREFIX ex: <http://example.com/ns#>\n" +
				"        SELECT $this (\"notAnIRI\" AS ?path) ?value WHERE {\n" +
				"          $this ex:knows ?value .\n" +
				"          FILTER ( !isIRI(?value) )\n" +
				"        }\n" +
				"      \"\"\" ;\n" +
				"    ]\n" +
				"  ] .\n";

		String dataTtl = "@prefix ex: <http://example.com/ns#> .\n" +
				"\n" +
				"ex:alice a ex:Person ;\n" +
				"  ex:knows \"Bob\" .\n";

		SailRepository shapes = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = shapes.getConnection()) {
			connection.add(new StringReader(shapesTtl), RDFFormat.TURTLE);
		}

		SailRepository data = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = data.getConnection()) {
			connection.add(new StringReader(dataTtl), RDFFormat.TURTLE);
		}

		ValidationReport report = ShaclValidator.validate(data.getSail(), shapes.getSail());
		assertFalse(report.conforms());
		assertEquals(1, report.getValidationResult().size());

		Model model = report.asModel();
		for (ValidationResult result : report.getValidationResult()) {
			Set<Value> paths = model.filter(result.getId(), SHACL.RESULT_PATH, null).objects();
			assertEquals(1, paths.size());
			Value path = paths.iterator().next();
			assertTrue(path instanceof IRI, "Expected sh:resultPath to be IRI, got " + path);
			assertEquals("http://example.com/ns#knows", path.stringValue());
		}
	}

	private static void assertResultMessagesMatchConstraintMessages(ValidationReport report) {
		Model model = report.asModel();
		for (ValidationResult result : report.getValidationResult()) {
			Resource resultId = result.getId();

			Set<String> resultMessages = toMessageSet(model.filter(resultId, SHACL.RESULT_MESSAGE, null).objects());

			Set<Resource> sourceConstraints = StreamSupport.stream(
					model.filter(resultId, SHACL.SOURCE_CONSTRAINT, null).objects().spliterator(), false)
					.filter(Value::isResource)
					.map(v -> (Resource) v)
					.collect(Collectors.toSet());

			assertEquals(1, sourceConstraints.size(),
					"Expected exactly one sh:sourceConstraint for result " + resultId);

			Resource sourceConstraint = sourceConstraints.iterator().next();
			Set<String> constraintMessages = toMessageSet(
					model.filter(sourceConstraint, SHACL.MESSAGE, null).objects());

			assertEquals(constraintMessages, resultMessages,
					"Result messages did not match constraint messages for result " + resultId);
		}
	}

	private static void assertAllResultsHaveMessages(ValidationReport report, Set<String> expectedMessages) {
		Model model = report.asModel();
		for (ValidationResult result : report.getValidationResult()) {
			Set<String> resultMessages = toMessageSet(
					model.filter(result.getId(), SHACL.RESULT_MESSAGE, null).objects());
			assertEquals(expectedMessages, resultMessages);
		}
	}

	private static Set<String> toMessageSet(Iterable<Value> values) {
		return StreamSupport.stream(values.spliterator(), false)
				.filter(Value::isLiteral)
				.map(v -> (Literal) v)
				.map(l -> l.getLabel() + l.getLanguage().map(lang -> "@" + lang).orElse(""))
				.collect(Collectors.toSet());
	}

}
