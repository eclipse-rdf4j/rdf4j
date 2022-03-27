/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;

/**
 * Some shapes may cause a validation error even if the targets don't exist. These tests check some of those scenarios.
 */
public class TargetNodeMinCountEdgeCaseTests {

	String shaclShapes = String.join("\n", "",
			"@base <http://example.com/ns> .",
			"@prefix ex: <http://example.com/ns#> .",
			"@prefix owl: <http://www.w3.org/2002/07/owl#> .",
			"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .",
			"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .",
			"@prefix sh: <http://www.w3.org/ns/shacl#> .",
			"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
			"",
			"ex:PersonShape",
			"	a sh:NodeShape  ;",
			"	sh:targetNode ex:validPerson1, ex:validPerson2 ;",
			"	sh:property [",
			"		sh:path ex:ssn ;",
			"		sh:minCount 2 ;",
			"	] .",
			"");

	String EX = "http://example.com/ns#";
	ValueFactory vf = SimpleValueFactory.getInstance();
	IRI validPerson1 = vf.createIRI(EX, "validPerson1");
	IRI validPerson2 = vf.createIRI(EX, "validPerson2");
	IRI ssn = vf.createIRI(EX, "ssn");
	Value value1 = vf.createLiteral(1);
	Value value2 = vf.createLiteral(2);

	@Test
	public void testMinCountWithEmptyState() throws Throwable {

		SailRepository sailRepository = new SailRepository(new ShaclSail(new MemoryStore()));

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			connection.add(new StringReader(shaclShapes), "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			assertThrows(ShaclSailValidationException.class, () -> {
				try {
					connection.commit();
				} catch (RepositoryException e) {
					throw e.getCause();
				}
			});
		} finally {
			sailRepository.shutDown();
		}

	}

	@Test
	public void testMinCountWithInvalidInitialDataset() throws Throwable {

		SailRepository sailRepository = new SailRepository(new ShaclSail(new MemoryStore()));

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			connection.add(validPerson1, ssn, value1);
			connection.add(validPerson2, ssn, value2);
			connection.commit();

			connection.begin();
			connection.add(new StringReader(shaclShapes), "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			assertThrows(ShaclSailValidationException.class, () -> {
				try {
					connection.commit();
				} catch (RepositoryException e) {
					throw e.getCause();
				}
			});
		} finally {
			sailRepository.shutDown();
		}

	}

	@Test
	public void testMinCountWithInvalidInitialDataset2() throws Throwable {

		SailRepository sailRepository = new SailRepository(new ShaclSail(new MemoryStore()));

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			connection.add(validPerson1, ssn, value1);
			connection.add(validPerson1, ssn, value2);
			connection.commit();

			connection.begin();
			connection.add(new StringReader(shaclShapes), "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			assertThrows(ShaclSailValidationException.class, () -> {
				try {
					connection.commit();
				} catch (RepositoryException e) {
					throw e.getCause();
				}
			});
		} finally {
			sailRepository.shutDown();
		}

	}

	@Test
	public void testMinCountWithInvalidInitialDataset3() throws Throwable {

		SailRepository sailRepository = new SailRepository(new ShaclSail(new MemoryStore()));

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			connection.add(validPerson1, ssn, value1);
			connection.add(validPerson1, ssn, value2);
			connection.add(new StringReader(shaclShapes), "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			assertThrows(ShaclSailValidationException.class, () -> {
				try {
					connection.commit();
				} catch (RepositoryException e) {
					throw e.getCause();
				}
			});
		} finally {
			sailRepository.shutDown();
		}

	}

	@Test
	public void testMinCountWithValidInitialDataset() throws Throwable {

		SailRepository sailRepository = new SailRepository(new ShaclSail(new MemoryStore()));

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			connection.add(validPerson1, ssn, value1);
			connection.add(validPerson1, ssn, value2);
			connection.add(validPerson2, ssn, value1);
			connection.add(validPerson2, ssn, value2);
			connection.commit();

			connection.begin();
			connection.add(new StringReader(shaclShapes), "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();
		} catch (Exception e) {
			throw e.getCause();
		} finally {
			sailRepository.shutDown();
		}

	}

	@Test
	public void testMinCountWithValidInitialDataset2() throws Throwable {

		SailRepository sailRepository = new SailRepository(new ShaclSail(new MemoryStore()));

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			connection.add(validPerson1, ssn, value1);
			connection.add(validPerson1, ssn, value2);

			connection.commit();

			connection.begin();
			connection.add(new StringReader(shaclShapes), "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			connection.add(validPerson2, ssn, value1);
			connection.add(validPerson2, ssn, value2);
			connection.commit();
		} catch (Exception e) {
			throw e.getCause();
		} finally {
			sailRepository.shutDown();
		}

	}

}
