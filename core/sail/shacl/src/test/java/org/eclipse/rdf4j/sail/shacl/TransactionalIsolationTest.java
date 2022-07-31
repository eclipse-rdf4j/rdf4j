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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.junit.jupiter.api.Test;

public class TransactionalIsolationTest {

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

			connection.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
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

			connection.add(extraShaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);

		} finally {
			sailRepository.shutDown();
		}
	}

	@Test
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

			connection.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
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
				connection.add(extraShaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			} catch (RepositoryException e) {
				assertThat(e.getCause()).isInstanceOf(ShaclSailValidationException.class);
			}
		} finally {
			sailRepository.shutDown();
		}
	}

	@Test
	public void testAddingShapesAfterData() throws Throwable {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		SailRepository sailRepository = new SailRepository(shaclSail);

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

			connection.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);

			try {
				connection.commit();
			} catch (RepositoryException e) {
				assertThat(e.getCause()).isInstanceOf(ShaclSailValidationException.class);
			}

		} finally {
			sailRepository.shutDown();
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

		connection2.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);

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

	@Test
	public void testViolation() throws Throwable {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		SailRepository sailRepository = new SailRepository(shaclSail);
		sailRepository.init();

		try (
				SailRepositoryConnection connection1 = sailRepository.getConnection();
				SailRepositoryConnection connection2 = sailRepository.getConnection()) {

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

			connection2.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			connection2.commit();

			try {
				add(connection1, "ex:steve a ex:Person .");
				fail();
			} catch (RepositoryException e) {
				assertThat(e.getCause()).isInstanceOf(ShaclSailValidationException.class);
			}

		} finally {
			sailRepository.shutDown();

		}

	}

	@Test
	public void testIsolation() throws Throwable {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		shaclSail.setSerializableValidation(true);

		SailRepository sailRepository = new SailRepository(shaclSail);

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

		connection2.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
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
	public void testIsolation_2() throws Throwable {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		shaclSail.setSerializableValidation(true);

		SailRepository sailRepository = new SailRepository(shaclSail);
		sailRepository.init();

		SailRepositoryConnection connection1 = sailRepository.getConnection();
		SailRepositoryConnection connection2 = sailRepository.getConnection();

		connection2.begin(IsolationLevels.SERIALIZABLE);

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

		connection2.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
		connection1.begin(IsolationLevels.SERIALIZABLE);
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
	public void testIsolation2() throws Throwable {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		shaclSail.setSerializableValidation(true);

		SailRepository sailRepository = new SailRepository(shaclSail);
		sailRepository.init();

		SailRepositoryConnection connection1 = sailRepository.getConnection();
		SailRepositoryConnection connection2 = sailRepository.getConnection();

		connection1.begin(IsolationLevels.SNAPSHOT);
		addInTransaction(connection1, "ex:steve a ex:Person .");

		connection2.begin(IsolationLevels.SNAPSHOT);

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

		connection2.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);

		connection1.commit();

		try {
			connection2.commit();
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
	public void testIsolation5() throws Throwable {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		shaclSail.setSerializableValidation(true);

		SailRepository sailRepository = new SailRepository(shaclSail);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
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

			connection.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			add(connection, "ex:steve ex:age 1 .");
		}

		SailRepositoryConnection connection1 = sailRepository.getConnection();
		SailRepositoryConnection connection2 = sailRepository.getConnection();

		connection1.begin(IsolationLevels.SNAPSHOT);
		addInTransaction(connection1, "ex:steve a ex:Person .");

		connection2.begin(IsolationLevels.SNAPSHOT);

		removeInTransaction(connection2, "ex:steve ex:age 1 .");

		connection1.commit();

		try {
			connection2.commit();
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
	public void testIsolation2_1() throws Throwable {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		shaclSail.setSerializableValidation(true);

		SailRepository sailRepository = new SailRepository(shaclSail);
		sailRepository.init();

		SailRepositoryConnection connection1 = sailRepository.getConnection();
		SailRepositoryConnection connection2 = sailRepository.getConnection();

		connection1.begin(IsolationLevels.SNAPSHOT_READ);
		addInTransaction(connection1, "ex:steve a ex:Person .");

		connection2.begin(IsolationLevels.SNAPSHOT_READ);

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

		connection2.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);

		connection1.commit();

		try {
			connection2.commit();
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
	public void testIsolation2_2() throws Throwable {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		shaclSail.setSerializableValidation(true);

		SailRepository sailRepository = new SailRepository(shaclSail);
		sailRepository.init();

		SailRepositoryConnection connection1 = sailRepository.getConnection();
		SailRepositoryConnection connection2 = sailRepository.getConnection();

		connection1.begin(IsolationLevels.SERIALIZABLE);
		addInTransaction(connection1, "ex:steve a ex:Person .");

		connection2.begin(IsolationLevels.SERIALIZABLE);

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

		connection2.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);

		connection1.commit();

		try {
			connection2.commit();
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
	public void testIsolation3() throws Throwable {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		shaclSail.setSerializableValidation(true);

		SailRepository sailRepository = new SailRepository(shaclSail);
		sailRepository.init();

		SailRepositoryConnection connection1 = sailRepository.getConnection();
		SailRepositoryConnection connection2 = sailRepository.getConnection();

		connection1.begin();
		addInTransaction(connection1, "ex:steve a ex:Person .");

		connection2.begin();
		connection1.commit();

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

		connection2.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);

		try {
			connection2.commit();
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
	public void testIsolation4() throws Throwable {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		shaclSail.setSerializableValidation(true);

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

		connection2.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
		connection1.begin();

		addInTransaction(connection1, "ex:steve a ex:Person .");
		connection2.commit();

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

		connection2.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
		connection2.getSailConnection().prepare();
		connection2.rollback();

		add(connection1, "ex:steve a ex:Person .");

		connection2.close();
		connection1.close();

		sailRepository.shutDown();

	}

	@Test()
	public void checkForNoExceptionWithEmptyTransaction() {

		SailRepository sailRepository = new SailRepository(new ShaclSail(new MemoryStore()));
		sailRepository.init();

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			connection.commit();
		}
		sailRepository.shutDown();

	}

	@Test
	public void testRemoveShapes() throws IOException {
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
					"                sh:minCount 1 ;",
					"        ] ."));

			connection.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			connection.begin();
			connection.remove((Resource) null, null, null, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

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
					"                sh:datatype xsd:integer ;",
					"        ] ."));

			connection.add(extraShaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);

			add(connection, "ex:pete a ex:Person .");

		} finally {
			sailRepository.shutDown();
		}
	}

	@Test
	public void testUpdateShapesSparql() throws Throwable {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		SailRepository sailRepository = new SailRepository(shaclSail);

		IRI g1 = Values.iri("http://example.com/g1");

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
					"                sh:minCount 1 ;",
					"        ] ."));

			connection.add(shaclRules, "", RDFFormat.TURTLE, g1);
			connection.commit();

			connection.begin();

			Update update = connection.prepareUpdate(String.join("\n", "",
					"DELETE {",
					"	graph ?g {",
					"		?a ?b ?c .",
					"	}",

					"} WHERE {",
					"	graph ?g {",
					"		?a ?b ?c .",
					"	}",
					"}"));
			update.execute();

			connection.commit();

			add(connection, "ex:pete a ex:Person .");

		} finally {
			sailRepository.shutDown();
		}
	}

	@Test
	public void testReadShapes() throws Throwable {
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
					"        sh:property ex:prop .",
					"",
					"ex:prop    sh:path ex:age ;",
					"                sh:minCount 1 ."));

			connection.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
			connection.commit();

			List<Statement> shapesStatements = Iterations
					.asList(connection.getStatements(null, null, null, false, RDF4J.SHACL_SHAPE_GRAPH));
			List<Statement> shapesStatementsInferred = Iterations
					.asList(connection.getStatements(null, null, null, true, RDF4J.SHACL_SHAPE_GRAPH));
			List<Statement> allStatements = Iterations.asList(connection.getStatements(null, null, null));

			boolean hasShapes = connection.hasStatement(null, null, null, false, RDF4J.SHACL_SHAPE_GRAPH);
			boolean hasAnyStatements = connection.hasStatement(null, null, null, false);

			assertEquals(5, shapesStatements.size());
			assertEquals(5, shapesStatementsInferred.size());
			assertEquals(0, allStatements.size());

			assertTrue(hasShapes);
			assertFalse(hasAnyStatements);
		} finally {
			sailRepository.shutDown();
		}
	}

	@Test
	public void testSerializableValidationAndTransactionalValidationLimit() throws Throwable {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		shaclSail.setSerializableValidation(true);
		shaclSail.setTransactionalValidationLimit(0);

		SailRepository sailRepository = new SailRepository(shaclSail);

		SailRepositoryConnection connection1 = sailRepository.getConnection();
		SailRepositoryConnection connection2 = sailRepository.getConnection();

		connection2.begin(IsolationLevels.SNAPSHOT);

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

		connection2.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
		addInTransaction(connection2, "ex:steve ex:age 1 .");

		connection1.begin(IsolationLevels.SNAPSHOT);

		addInTransaction(connection1, "ex:steve a ex:Person .");

		connection2.commit();
		connection1.commit();

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

	private void add(SailRepositoryConnection connection, String data) throws IOException {
		data = String.join("\n", "",
				"@prefix ex: <http://example.com/ns#> .",
				"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
				data);

		connection.begin();

		StringReader stringReader = new StringReader(data);

		connection.add(stringReader, "", RDFFormat.TRIG);
		connection.commit();
	}

	private void addInTransaction(SailRepositoryConnection connection, String data) {
		data = String.join("\n", "",
				"@prefix ex: <http://example.com/ns#> .",
				"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
				data);

		StringReader stringReader = new StringReader(data);

		try {
			connection.add(stringReader, "", RDFFormat.TRIG);
		} catch (IOException e) {
			throw new IllegalStateException();
		}
	}

	private void removeInTransaction(SailRepositoryConnection connection, String data) {
		data = String.join("\n", "",
				"@prefix ex: <http://example.com/ns#> .",
				"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
				data);

		StringReader stringReader = new StringReader(data);

		try {
			Model parse = Rio.parse(stringReader, RDFFormat.TRIG);
			connection.remove(parse);
		} catch (IOException e) {
			throw new IllegalStateException();
		}
	}

}
