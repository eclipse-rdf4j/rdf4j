/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import java.io.InputStream;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Håvard Ottestad
 */
public class BulkValidationSettingsTest {

	@BeforeClass
	public static void beforeClass() {
		GlobalValidationExecutionLogging.loggingEnabled = false;
	}

	@Test
	public void testValid() throws Exception {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk, IsolationLevels.NONE);

			try (InputStream shapesData = Utils.class.getClassLoader().getResourceAsStream("shacl.ttl")) {
				connection.add(shapesData, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			}

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.commit();

		} finally {
			repository.shutDown();
		}

	}

	@Test(expected = ShaclSailValidationException.class)
	public void testInvalid() throws Throwable {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk, IsolationLevels.NONE);

			try (InputStream shapesData = Utils.class.getClassLoader().getResourceAsStream("shacl.ttl")) {
				connection.add(shapesData, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			}

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			try {
				connection.commit();
			} catch (RepositoryException e) {
				throw e.getCause();
			}

		} finally {
			repository.shutDown();
		}
	}

	@Test(expected = ShaclSailValidationException.class)
	public void testInvalidSnapshot() throws Throwable {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk, IsolationLevels.SNAPSHOT);

			try (InputStream shapesData = Utils.class.getClassLoader().getResourceAsStream("shacl.ttl")) {
				connection.add(shapesData, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			}

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			try {
				connection.commit();
			} catch (RepositoryException e) {
				throw e.getCause();
			}

		} finally {
			repository.shutDown();
		}

	}

	@Test
	public void testInvalidRollsBackCorrectly() {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk, IsolationLevels.NONE);

			try (InputStream shapesData = Utils.class.getClassLoader().getResourceAsStream("shacl.ttl")) {
				connection.add(shapesData, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			}

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.commit();

		} catch (Exception ignored) {

		}

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(IsolationLevels.NONE);
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();

		} finally {
			repository.shutDown();
		}

	}

	@Test(expected = ShaclSailValidationException.class)
	public void testValidationDisabled() throws Throwable {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Disabled);

			try (InputStream shapesData = Utils.class.getClassLoader().getResourceAsStream("shacl.ttl")) {
				connection.add(shapesData, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			}

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.commit();

			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk);
			try (SailRepositoryConnection connection1 = repository.getConnection()) {

				try {
					connection.commit();
				} catch (RepositoryException e) {
					throw e.getCause();
				}
			}

		} finally {
			repository.shutDown();
		}

	}

	@Test
	public void testValidationDisabledSnapshotSerializableValidation() throws Throwable {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Disabled, IsolationLevels.SNAPSHOT);

			try (InputStream shapesData = Utils.class.getClassLoader().getResourceAsStream("shacl.ttl")) {
				connection.add(shapesData, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			}

			connection.commit();

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.CLASS);

			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Disabled, IsolationLevels.SNAPSHOT);

			try (SailRepositoryConnection connection1 = repository.getConnection()) {

				connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

				connection.commit();

			}

		} finally {
			repository.shutDown();
		}

	}

}
