/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import static junit.framework.TestCase.assertEquals;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;
import org.junit.Test;

/**
 * @author Håvard Ottestad
 */
public class TruncatedValidationReportTest {

	@Test
	public void testTotal() throws IOException {
		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shaclDatatypeAndMinCount.ttl", true);

		ShaclSail sail = (ShaclSail) shaclRepository.getSail();
		sail.setValidationResultTruncationTotalSize(10);

		ValidationReport validationReport = getValidationReport(shaclRepository);
		shaclRepository.shutDown();

		Map<SourceConstraintComponent, Long> collect = validationReport.getValidationResult()
				.stream()
				.collect(Collectors.groupingBy(ValidationResult::getSourceConstraintComponent, Collectors.counting()));
		long total = collect.values().stream().mapToLong(l -> l).sum();

		assertEquals(10, total);

	}

	@Test
	public void testPerConstraint() throws IOException {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shaclDatatypeAndMinCount.ttl", true);

		ShaclSail sail = (ShaclSail) shaclRepository.getSail();
		sail.setValidationResultTruncationPerConstraintSize(10);

		ValidationReport validationReport = getValidationReport(shaclRepository);
		shaclRepository.shutDown();

		Map<SourceConstraintComponent, Long> collect = validationReport.getValidationResult()
				.stream()
				.collect(Collectors.groupingBy(ValidationResult::getSourceConstraintComponent, Collectors.counting()));
		long total = collect.values().stream().mapToLong(l -> l).sum();

		assertEquals(10, collect.get(SourceConstraintComponent.MinCountConstraintComponent).longValue());
		assertEquals(10, collect.get(SourceConstraintComponent.DatatypeConstraintComponent).longValue());
		assertEquals(20, total);

	}

	@Test
	public void testTotalAndPerConstraint() throws IOException {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shaclDatatypeAndMinCount.ttl", true);

		ShaclSail sail = (ShaclSail) shaclRepository.getSail();
		sail.setValidationResultTruncationTotalSize(20);
		sail.setValidationResultTruncationPerConstraintSize(5);

		ValidationReport validationReport = getValidationReport(shaclRepository);
		shaclRepository.shutDown();

		Map<SourceConstraintComponent, Long> collect = validationReport.getValidationResult()
				.stream()
				.collect(Collectors.groupingBy(ValidationResult::getSourceConstraintComponent, Collectors.counting()));
		long total = collect.values().stream().mapToLong(l -> l).sum();

		assertEquals(5, collect.get(SourceConstraintComponent.MinCountConstraintComponent).longValue());
		assertEquals(5, collect.get(SourceConstraintComponent.DatatypeConstraintComponent).longValue());
		assertEquals(10, total);

	}

	@Test
	public void testTotalAndPerConstraint2() throws IOException {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shaclDatatypeAndMinCount.ttl", true);

		ShaclSail sail = (ShaclSail) shaclRepository.getSail();
		sail.setValidationResultTruncationTotalSize(20);
		sail.setValidationResultTruncationPerConstraintSize(15);

		ValidationReport validationReport = getValidationReport(shaclRepository);
		shaclRepository.shutDown();

		Map<SourceConstraintComponent, Long> collect = validationReport.getValidationResult()
				.stream()
				.collect(Collectors.groupingBy(ValidationResult::getSourceConstraintComponent, Collectors.counting()));
		long total = collect.values().stream().mapToLong(l -> l).sum();

		assertEquals(20, total);

	}

	@Test
	public void testNoLimit() throws IOException {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shaclDatatypeAndMinCount.ttl", true);

		ValidationReport validationReport = getValidationReport(shaclRepository);
		shaclRepository.shutDown();

		Map<SourceConstraintComponent, Long> collect = validationReport.getValidationResult()
				.stream()
				.collect(Collectors.groupingBy(ValidationResult::getSourceConstraintComponent, Collectors.counting()));
		long total = collect.values().stream().mapToLong(l -> l).sum();

		assertEquals(5000, collect.get(SourceConstraintComponent.MinCountConstraintComponent).longValue());
		assertEquals(5000, collect.get(SourceConstraintComponent.DatatypeConstraintComponent).longValue());
		assertEquals(10000, total);

	}

	@Test
	public void testRevalidate() throws IOException {
		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shaclDatatypeAndMinCount.ttl", true);

		ShaclSail sail = (ShaclSail) shaclRepository.getSail();
		sail.setValidationResultTruncationPerConstraintSize(15);
		sail.setValidationResultTruncationTotalSize(-1);

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

		assertEquals(15, collect.get(SourceConstraintComponent.MinCountConstraintComponent).longValue());
		assertEquals(15, collect.get(SourceConstraintComponent.DatatypeConstraintComponent).longValue());

		assertEquals(30, total);
	}

	private ValidationReport getValidationReport(SailRepository shaclRepository) {
		ValidationReport validationReport = null;
		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			for (int i = 0; i < 5000; i++) {
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
