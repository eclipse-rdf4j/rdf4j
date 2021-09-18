/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Håvard Ottestad
 */
public class TruncatedValidationReportTest {

	private static final int NUMBER_OF_FAILURES = 5000;

	@BeforeClass
	public static void beforeClass() {
		GlobalValidationExecutionLogging.loggingEnabled = false;
	}

	@Test
	public void testTotal() throws IOException {
		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shaclDatatypeAndMinCount.ttl");

		ShaclSail sail = (ShaclSail) shaclRepository.getSail();
		sail.setValidationResultsLimitTotal(10);

		ValidationReport validationReport = getValidationReport(shaclRepository);
		shaclRepository.shutDown();

		Map<SourceConstraintComponent, Long> collect = validationReport.getValidationResult()
				.stream()
				.collect(Collectors.groupingBy(ValidationResult::getSourceConstraintComponent, Collectors.counting()));
		long total = collect.values().stream().mapToLong(l -> l).sum();

		assertTrue(validationReport.isTruncated());
		assertEquals(10, total);

	}

	@Test
	public void testPerConstraint() throws IOException {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shaclDatatypeAndMinCount.ttl");

		ShaclSail sail = (ShaclSail) shaclRepository.getSail();
		sail.setValidationResultsLimitPerConstraint(10);

		ValidationReport validationReport = getValidationReport(shaclRepository);
		shaclRepository.shutDown();

		Map<SourceConstraintComponent, Long> collect = validationReport.getValidationResult()
				.stream()
				.collect(Collectors.groupingBy(ValidationResult::getSourceConstraintComponent, Collectors.counting()));
		long total = collect.values().stream().mapToLong(l -> l).sum();

		assertTrue(validationReport.isTruncated());
		assertEquals(10, collect.get(SourceConstraintComponent.MinCountConstraintComponent).longValue());
		assertEquals(10, collect.get(SourceConstraintComponent.DatatypeConstraintComponent).longValue());
		assertEquals(20, total);

	}

	@Test
	public void testPerConstraint2() throws IOException {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shaclDatatypeAndMinCount.ttl");

		ShaclSail sail = (ShaclSail) shaclRepository.getSail();
		sail.setValidationResultsLimitPerConstraint(10);
		sail.setValidationResultsLimitTotal(1);
		sail.setValidationResultsLimitTotal(-1);

		ValidationReport validationReport = getValidationReport(shaclRepository);
		shaclRepository.shutDown();

		Map<SourceConstraintComponent, Long> collect = validationReport.getValidationResult()
				.stream()
				.collect(Collectors.groupingBy(ValidationResult::getSourceConstraintComponent, Collectors.counting()));
		long total = collect.values().stream().mapToLong(l -> l).sum();

		assertTrue(validationReport.isTruncated());
		assertEquals(10, collect.get(SourceConstraintComponent.MinCountConstraintComponent).longValue());
		assertEquals(10, collect.get(SourceConstraintComponent.DatatypeConstraintComponent).longValue());
		assertEquals(20, total);

	}

	@Test
	public void testZeroTotal() throws IOException {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shaclDatatypeAndMinCount.ttl");

		ShaclSail sail = (ShaclSail) shaclRepository.getSail();
		sail.setValidationResultsLimitTotal(0);

		ValidationReport validationReport = getValidationReport(shaclRepository);
		shaclRepository.shutDown();

		Map<SourceConstraintComponent, Long> collect = validationReport.getValidationResult()
				.stream()
				.collect(Collectors.groupingBy(ValidationResult::getSourceConstraintComponent, Collectors.counting()));
		long total = collect.values().stream().mapToLong(l -> l).sum();

		assertTrue(validationReport.isTruncated());
		assertFalse(validationReport.conforms());
		assertEquals(0, total);

	}

	@Test
	public void testZeroPerConstraint() throws IOException {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shaclDatatypeAndMinCount.ttl");

		ShaclSail sail = (ShaclSail) shaclRepository.getSail();
		sail.setValidationResultsLimitPerConstraint(0);

		ValidationReport validationReport = getValidationReport(shaclRepository);
		shaclRepository.shutDown();

		Map<SourceConstraintComponent, Long> collect = validationReport.getValidationResult()
				.stream()
				.collect(Collectors.groupingBy(ValidationResult::getSourceConstraintComponent, Collectors.counting()));
		long total = collect.values().stream().mapToLong(l -> l).sum();

		assertTrue(validationReport.isTruncated());
		assertFalse(validationReport.conforms());
		assertEquals(0, total);

	}

	@Test
	public void testTotalAndPerConstraint() throws IOException {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shaclDatatypeAndMinCount.ttl");

		ShaclSail sail = (ShaclSail) shaclRepository.getSail();
		sail.setValidationResultsLimitTotal(20);
		sail.setValidationResultsLimitPerConstraint(5);

		ValidationReport validationReport = getValidationReport(shaclRepository);
		shaclRepository.shutDown();

		Map<SourceConstraintComponent, Long> collect = validationReport.getValidationResult()
				.stream()
				.collect(Collectors.groupingBy(ValidationResult::getSourceConstraintComponent, Collectors.counting()));
		long total = collect.values().stream().mapToLong(l -> l).sum();

		assertTrue(validationReport.isTruncated());
		assertEquals(5, collect.get(SourceConstraintComponent.MinCountConstraintComponent).longValue());
		assertEquals(5, collect.get(SourceConstraintComponent.DatatypeConstraintComponent).longValue());
		assertEquals(10, total);

	}

	@Test
	public void testTotalAndPerConstraint2() throws IOException {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shaclDatatypeAndMinCount.ttl");

		ShaclSail sail = (ShaclSail) shaclRepository.getSail();
		sail.setValidationResultsLimitTotal(20);
		sail.setValidationResultsLimitPerConstraint(15);

		ValidationReport validationReport = getValidationReport(shaclRepository);
		shaclRepository.shutDown();

		Map<SourceConstraintComponent, Long> collect = validationReport.getValidationResult()
				.stream()
				.collect(Collectors.groupingBy(ValidationResult::getSourceConstraintComponent, Collectors.counting()));
		long total = collect.values().stream().mapToLong(l -> l).sum();

		assertTrue(validationReport.isTruncated());
		assertEquals(20, total);

		assertTrue(validationReport.asModel().contains(null, RDF4J.TRUNCATED, BooleanLiteral.TRUE));
	}

	@Test
	public void testNoLimit() throws IOException {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shaclDatatypeAndMinCount.ttl");

		ValidationReport validationReport = getValidationReport(shaclRepository);
		shaclRepository.shutDown();

		Map<SourceConstraintComponent, Long> collect = validationReport.getValidationResult()
				.stream()
				.collect(Collectors.groupingBy(ValidationResult::getSourceConstraintComponent, Collectors.counting()));
		long total = collect.values().stream().mapToLong(l -> l).sum();

		assertFalse(validationReport.isTruncated());
		assertEquals(NUMBER_OF_FAILURES,
				collect.get(SourceConstraintComponent.MinCountConstraintComponent).longValue());
		assertEquals(NUMBER_OF_FAILURES,
				collect.get(SourceConstraintComponent.DatatypeConstraintComponent).longValue());
		assertEquals(NUMBER_OF_FAILURES * 2, total);

	}

	@Test
	public void testLimitIsEqualToSize() throws IOException {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shaclDatatypeAndMinCount.ttl");

		ShaclSail sail = (ShaclSail) shaclRepository.getSail();
		sail.setValidationResultsLimitTotal(NUMBER_OF_FAILURES * 2);
		sail.setValidationResultsLimitPerConstraint(NUMBER_OF_FAILURES);

		ValidationReport validationReport = getValidationReport(shaclRepository);
		shaclRepository.shutDown();

		Map<SourceConstraintComponent, Long> collect = validationReport.getValidationResult()
				.stream()
				.collect(Collectors.groupingBy(ValidationResult::getSourceConstraintComponent, Collectors.counting()));
		long total = collect.values().stream().mapToLong(l -> l).sum();

		assertFalse(validationReport.isTruncated());
		assertEquals(NUMBER_OF_FAILURES,
				collect.get(SourceConstraintComponent.MinCountConstraintComponent).longValue());
		assertEquals(NUMBER_OF_FAILURES,
				collect.get(SourceConstraintComponent.DatatypeConstraintComponent).longValue());
		assertEquals(NUMBER_OF_FAILURES * 2, total);

	}

	@Test
	public void testRevalidate() throws IOException {
		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shaclDatatypeAndMinCount.ttl");

		ShaclSail sail = (ShaclSail) shaclRepository.getSail();
		sail.setValidationResultsLimitPerConstraint(15);
		sail.setValidationResultsLimitTotal(-1);

		sail.disableValidation();
		getValidationReport(shaclRepository);
		sail.enableValidation();

		ValidationReport validationReport;
		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			validationReport = ((ShaclSailConnection) connection.getSailConnection()).revalidate();
			connection.commit();
		}

		shaclRepository.shutDown();

		Map<SourceConstraintComponent, Long> collect = validationReport.getValidationResult()
				.stream()
				.collect(Collectors.groupingBy(ValidationResult::getSourceConstraintComponent, Collectors.counting()));
		long total = collect.values().stream().mapToLong(l -> l).sum();

		assertTrue(validationReport.isTruncated());
		assertEquals(15, collect.get(SourceConstraintComponent.MinCountConstraintComponent).longValue());
		assertEquals(15, collect.get(SourceConstraintComponent.DatatypeConstraintComponent).longValue());
		assertEquals(30, total);
	}

	private ValidationReport getValidationReport(SailRepository shaclRepository) {
		ValidationReport validationReport = null;
		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			for (int i = 0; i < NUMBER_OF_FAILURES; i++) {
				ValueFactory vf = connection.getValueFactory();

				BNode bNode = vf.createBNode();
				connection.add(bNode, RDF.TYPE, FOAF.PERSON);
				connection.add(bNode, FOAF.AGE, vf.createLiteral("three"));
			}
			try {
				connection.commit();
			} catch (RepositoryException e) {
				validationReport = ((ShaclSailValidationException) e.getCause()).getValidationReport();
			}
		}
		return validationReport;
	}

}
