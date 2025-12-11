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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
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
