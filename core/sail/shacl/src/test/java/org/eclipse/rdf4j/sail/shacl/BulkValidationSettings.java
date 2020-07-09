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
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Håvard Ottestad
 */
public class BulkValidationSettings {

	@BeforeClass
	public static void beforeClass() {
		// GlobalValidationExecutionLogging.loggingEnabled = true;
	}

	@AfterClass
	public static void afterClass() {
		GlobalValidationExecutionLogging.loggingEnabled = false;
	}

	@Test
	public void testValid() throws Exception {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(ShaclSail.Settings.Validation.Bulk, IsolationLevels.NONE);

			try (InputStream shapesData = Utils.class.getClassLoader().getResourceAsStream("shacl.ttl")) {
				connection.add(shapesData, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			}

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.commit();

		}

	}

	@Test(expected = ShaclSailValidationException.class)
	public void testInvalid() throws Throwable {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(ShaclSail.Settings.Validation.Bulk, IsolationLevels.NONE);

			try (InputStream shapesData = Utils.class.getClassLoader().getResourceAsStream("shacl.ttl")) {
				connection.add(shapesData, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			}

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			try {
				connection.commit();
			} catch (Exception e) {
				throw e.getCause();
			}

		}

	}

	@Test
	public void testInvalidRollsBackCorrectly() {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(ShaclSail.Settings.Validation.Bulk, IsolationLevels.NONE);

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

		}

	}

	@Test
	public void testValidationDisabled() throws Throwable {

		SailRepository repository = new SailRepository(new ShaclSail(new MemoryStore()));

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin(ShaclSail.Settings.Validation.Disabled, IsolationLevels.NONE);

			try (InputStream shapesData = Utils.class.getClassLoader().getResourceAsStream("shacl.ttl")) {
				connection.add(shapesData, "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			}

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			try {
				connection.commit();
			} catch (Exception e) {
				throw e.getCause();
			}

		}

	}

}
