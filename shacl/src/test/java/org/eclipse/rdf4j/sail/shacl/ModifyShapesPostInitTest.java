/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.SailConflictException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

import static junit.framework.TestCase.assertTrue;

public class ModifyShapesPostInitTest {

	@Test
	public void testUpdatingShapes() throws IOException {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		SailRepository sailRepository = new SailRepository(shaclSail);
		sailRepository.init();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin();

			StringReader shaclRules = new StringReader(String.join("\n", "",
					"@prefix ex: <http://example.com/ns#> .",
					"@prefix sh: <http://www.w3.org/ns/shacl#> .",
					"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

					"ex:PersonShape",
					"        a sh:NodeShape  ;",
					"        sh:targetClass ex:Person ;",
					"        sh:property [",
					"                sh:path ex:age ;",
					"                sh:datatype xsd:integer ;",
					"        ] ."));

			connection.add(shaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			add(connection, "ex:pete a ex:Person .");

			StringReader extraShaclRules = new StringReader(String.join("\n", "",
					"@prefix ex: <http://example.com/ns#> .",
					"@prefix sh: <http://www.w3.org/ns/shacl#> .",
					"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

					"ex:PersonShape",
					"        a sh:NodeShape  ;",
					"        sh:targetClass ex:Person ;",
					"        sh:property [",
					"                sh:path ex:birthyear ;",
					"                sh:datatype xsd:integer ;",
					"        ] ."));

			connection.add(extraShaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);

		}
	}

	@Test(expected = ShaclSailValidationException.class)
	public void testUpdatingShapesViolation() throws Throwable {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		SailRepository sailRepository = new SailRepository(shaclSail);
		sailRepository.init();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			connection.begin();

			StringReader shaclRules = new StringReader(String.join("\n", "",
					"@prefix ex: <http://example.com/ns#> .",
					"@prefix sh: <http://www.w3.org/ns/shacl#> .",
					"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

					"ex:PersonShape",
					"        a sh:NodeShape  ;",
					"        sh:targetClass ex:Person ;",
					"        sh:property [",
					"                sh:path ex:age ;",
					"                sh:datatype xsd:integer ;",
					"        ] ."));

			connection.add(shaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			add(connection, "ex:pete a ex:Person .");

			StringReader extraShaclRules = new StringReader(String.join("\n", "",
					"@prefix ex: <http://example.com/ns#> .",
					"@prefix sh: <http://www.w3.org/ns/shacl#> .",
					"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

					"ex:PersonShape",
					"        a sh:NodeShape  ;",
					"        sh:targetClass ex:Person ;",
					"        sh:property [",
					"                sh:path ex:age ;",
					"                sh:minCount 1 ;",
					"        ] ."));

			try {
				connection.add(extraShaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			} catch (RepositoryException e) {
				throw e.getCause();
			}
		}
	}

	@Test(expected = ShaclSailValidationException.class)
	public void testAddingShapesAfterData() throws Throwable {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		SailRepository sailRepository = new SailRepository(shaclSail);
		sailRepository.init();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {

			add(connection, "ex:pete a ex:Person .");

			connection.begin();

			StringReader shaclRules = new StringReader(String.join("\n", "",
					"@prefix ex: <http://example.com/ns#> .",
					"@prefix sh: <http://www.w3.org/ns/shacl#> .",
					"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

					"ex:PersonShape",
					"        a sh:NodeShape  ;",
					"        sh:targetClass ex:Person ;",
					"        sh:property [",
					"                sh:path ex:age ;",
					"                sh:minCount 1 ;",
					"        ] ."));

			connection.add(shaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);

			try {
				connection.commit();
			} catch (RepositoryException e) {
				throw e.getCause();
			}

		}
	}

	@Test
	public void testAddingShapesThatFails() throws Throwable {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		SailRepository sailRepository = new SailRepository(shaclSail);
		sailRepository.init();

		SailRepositoryConnection connection1 = sailRepository.getConnection();
		SailRepositoryConnection connection2 = sailRepository.getConnection();

		add(connection1, "ex:pete a ex:Person .");

		connection2.begin();

		StringReader shaclRules = new StringReader(String.join("\n", "",
				"@prefix ex: <http://example.com/ns#> .",
				"@prefix sh: <http://www.w3.org/ns/shacl#> .",
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
				"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

				"ex:PersonShape",
				"        a sh:NodeShape  ;",
				"        sh:targetClass ex:Person ;",
				"        sh:property [",
				"                sh:path ex:age ;",
				"                sh:minCount 1 ;",
				"        ] ."));

		connection2.add(shaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);

		try {
			connection2.commit();
		} catch (Throwable ignored) {
		}

		connection2.rollback();
		add(connection1, "ex:steve a ex:Person .");

		connection2.close();
		connection1.close();

		sailRepository.shutDown();

	}

	@Test(expected = ShaclSailValidationException.class)
	public void testViolation() throws Throwable {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		SailRepository sailRepository = new SailRepository(shaclSail);
		sailRepository.init();

		SailRepositoryConnection connection1 = sailRepository.getConnection();
		SailRepositoryConnection connection2 = sailRepository.getConnection();

		connection2.begin();

		StringReader shaclRules = new StringReader(String.join("\n", "",
				"@prefix ex: <http://example.com/ns#> .",
				"@prefix sh: <http://www.w3.org/ns/shacl#> .",
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
				"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

				"ex:PersonShape",
				"        a sh:NodeShape  ;",
				"        sh:targetClass ex:Person ;",
				"        sh:property [",
				"                sh:path ex:age ;",
				"                sh:minCount 1 ;",
				"        ] ."));

		connection2.add(shaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
		connection2.commit();

		try {
			add(connection1, "ex:steve a ex:Person .");

		} catch (Throwable e) {
			throw e.getCause();
		}

		connection2.close();
		connection1.close();

		sailRepository.shutDown();

	}

	@Test
	public void testIsolation() throws Throwable {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		SailRepository sailRepository = new SailRepository(shaclSail);
		sailRepository.init();

		SailRepositoryConnection connection1 = sailRepository.getConnection();
		SailRepositoryConnection connection2 = sailRepository.getConnection();

		connection2.begin();

		StringReader shaclRules = new StringReader(String.join("\n", "",
				"@prefix ex: <http://example.com/ns#> .",
				"@prefix sh: <http://www.w3.org/ns/shacl#> .",
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
				"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

				"ex:PersonShape",
				"        a sh:NodeShape  ;",
				"        sh:targetClass ex:Person ;",
				"        sh:property [",
				"                sh:path ex:age ;",
				"                sh:minCount 1 ;",
				"        ] ."));

		connection2.add(shaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
		connection1.begin();
		connection2.commit();

		addInTransaction(connection1, "ex:steve a ex:Person .");

		try {
			connection1.commit();
		} catch (Throwable ignored) {
		}

		connection2.close();
		connection1.close();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			ValidationReport validationReport = ((ShaclSailConnection) connection.getSailConnection()).revalidate();

			assertTrue(validationReport.conforms());

			connection.commit();
		}

		sailRepository.shutDown();

	}

	@Test
	public void testRollbak() throws Throwable {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		SailRepository sailRepository = new SailRepository(shaclSail);
		sailRepository.init();

		SailRepositoryConnection connection1 = sailRepository.getConnection();
		SailRepositoryConnection connection2 = sailRepository.getConnection();

		connection2.begin();

		StringReader shaclRules = new StringReader(String.join("\n", "",
				"@prefix ex: <http://example.com/ns#> .",
				"@prefix sh: <http://www.w3.org/ns/shacl#> .",
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
				"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

				"ex:PersonShape",
				"        a sh:NodeShape  ;",
				"        sh:targetClass ex:Person ;",
				"        sh:property [",
				"                sh:path ex:age ;",
				"                sh:minCount 1 ;",
				"        ] ."));

		connection2.add(shaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
		connection2.getSailConnection().prepare();
		connection2.rollback();

		add(connection1, "ex:steve a ex:Person .");

		connection2.close();
		connection1.close();

		sailRepository.shutDown();

	}

	@Test(expected = SailConflictException.class)
	public void testDeadlockDetection() throws Throwable {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		SailRepository sailRepository = new SailRepository(shaclSail);
		sailRepository.init();

		SailRepositoryConnection connection1 = sailRepository.getConnection();
		SailRepositoryConnection connection2 = sailRepository.getConnection();

		try {
			connection2.begin();

			StringReader shaclRules = new StringReader(String.join("\n", "",
					"@prefix ex: <http://example.com/ns#> .",
					"@prefix sh: <http://www.w3.org/ns/shacl#> .",
					"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

					"ex:PersonShape",
					"        a sh:NodeShape  ;",
					"        sh:targetClass ex:Person ;",
					"        sh:property [",
					"                sh:path ex:age ;",
					"                sh:minCount 1 ;",
					"        ] ."));

			connection2.add(shaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection1.begin();

			addInTransaction(connection1, "ex:steve a ex:Person .");

			try {
				connection1.commit();
			} catch (RepositoryException e) {
				throw e.getCause();
			}

		} finally {
			connection2.close();
			connection1.close();

			sailRepository.shutDown();
		}

	}

	@Test(expected = SailConflictException.class)
	public void testDeadlockDetectionOnWriteLock() throws Throwable {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		SailRepository sailRepository = new SailRepository(shaclSail);
		sailRepository.init();

		SailRepositoryConnection connection1 = sailRepository.getConnection();
		SailRepositoryConnection connection2 = sailRepository.getConnection();

		try {
			connection2.begin();

			StringReader shaclRules = new StringReader(String.join("\n", "",
					"@prefix ex: <http://example.com/ns#> .",
					"@prefix sh: <http://www.w3.org/ns/shacl#> .",
					"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

					"ex:PersonShape",
					"        a sh:NodeShape  ;",
					"        sh:targetClass ex:Person ;",
					"        sh:property [",
					"                sh:path ex:age ;",
					"                sh:minCount 1 ;",
					"        ] ."));

			connection2.add(shaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			connection1.begin();

			shaclRules = new StringReader(String.join("\n", "",
					"@prefix ex: <http://example.com/ns#> .",
					"@prefix sh: <http://www.w3.org/ns/shacl#> .",
					"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

					"ex:PersonShape",
					"        a sh:NodeShape  ;",
					"        sh:targetClass ex:Person ;",
					"        sh:property [",
					"                sh:path ex:age ;",
					"                sh:maxCount 1 ;",
					"        ] ."));

			try {
				connection1.add(shaclRules, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			} catch (RepositoryException e) {
				throw e.getCause();
			}

		} finally {
			connection2.close();
			connection1.close();

			sailRepository.shutDown();
		}

	}

	@Test()
	public void checkForNoExceptionWithEmptyTransaction() {

		SailRepository sailRepository = new SailRepository(new ShaclSail(new MemoryStore()));
		sailRepository.init();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			connection.commit();
		}

	}

	private void add(SailRepositoryConnection connection, String data) throws IOException {
		data = String.join("\n", "",
				"@prefix ex: <http://example.com/ns#> .",
				"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
				data);

		connection.begin();

		StringReader stringReader = new StringReader(data);

		connection.add(stringReader, "", RDFFormat.TURTLE);
		connection.commit();
	}

	private void addInTransaction(SailRepositoryConnection connection, String data) throws IOException {
		data = String.join("\n", "",
				"@prefix ex: <http://example.com/ns#> .",
				"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
				data);

		StringReader stringReader = new StringReader(data);

		connection.add(stringReader, "", RDFFormat.TURTLE);
	}

}
