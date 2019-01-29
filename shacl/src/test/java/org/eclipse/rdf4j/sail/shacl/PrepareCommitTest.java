/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
 
package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class PrepareCommitTest {

	@Test(expected = IllegalStateException.class)
	public void testFailureWhenChangesAfterPrepare() throws IOException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("shacl.ttl");

		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			connection.begin();
			connection.addStatement(RDFS.RESOURCE, RDFS.SUBCLASSOF, RDFS.RESOURCE);
			connection.prepare();
			connection.removeStatements(RDFS.RESOURCE, RDFS.SUBCLASSOF, RDFS.RESOURCE);
			connection.commit();
		}

		shaclSail.shutDown();
	}


	@Test
	public void testMultiplePrepare() throws IOException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("shacl.ttl");

		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			connection.begin();
			connection.addStatement(RDFS.RESOURCE, RDFS.SUBCLASSOF, RDFS.RESOURCE);
			connection.prepare();
			connection.commit();

			connection.begin();
			connection.removeStatements(RDFS.RESOURCE, RDFS.SUBCLASSOF, RDFS.RESOURCE);
			connection.prepare();
			connection.commit();

		}

		shaclSail.shutDown();
	}


	@Test
	public void testWithoutPrepare() throws IOException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("shacl.ttl");

		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			connection.begin();
			connection.addStatement(RDFS.RESOURCE, RDFS.SUBCLASSOF, RDFS.RESOURCE);
			connection.commit();

		}

		shaclSail.shutDown();
	}


	@Test
	public void testPrepareAfterRollback() throws IOException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("shacl.ttl");

		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			connection.begin();
			connection.addStatement(RDFS.RESOURCE, RDFS.SUBCLASSOF, RDFS.RESOURCE);
			connection.prepare();
			connection.prepare();
			connection.rollback();
			connection.rollback();

			connection.begin();
			connection.addStatement(RDFS.SUBCLASSOF, RDFS.SUBPROPERTYOF, RDFS.SUBCLASSOF);
			connection.prepare();
			connection.commit();

		}

		shaclSail.shutDown();
	}

	@Test
	public void testAutomaticRollback() throws IOException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("shacl.ttl");


		BNode bNode = SimpleValueFactory.getInstance().createBNode();

		NotifyingSailConnection connection = null;
		try {
			connection = shaclSail.getConnection();
			connection.begin();
			connection.addStatement(bNode, RDF.TYPE, RDFS.RESOURCE);
			connection.prepare();
			connection.commit();

		} catch (ShaclSailValidationException ignored) {
		} finally {
			if (connection != null) {
				// check that nothing has been rolled back yet
				assertTrue(connection.hasStatement(bNode, RDF.TYPE, RDFS.RESOURCE, false));
				connection.close();
			}
		}

		// check that close() called rollback
		try (NotifyingSailConnection connection1 = shaclSail.getConnection()) {
			assertFalse(connection1.hasStatement(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE, false));
		}

		shaclSail.shutDown();
	}

	@Test
	public void testAutomaticRollback2() throws IOException {
		ShaclSail shaclSail = Utils.getInitializedShaclSail("shacl.ttl");

		boolean exception = false;
		BNode bNode = SimpleValueFactory.getInstance().createBNode();

		NotifyingSailConnection connection = null;
		try {
			connection = shaclSail.getConnection();
			connection.begin();
			connection.addStatement(bNode, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();

		} catch (ShaclSailValidationException ignored) {
			exception = true;
		} finally {
			if (connection != null) {
				// check that nothing has been rolled back yet
				assertTrue(connection.hasStatement(bNode, RDF.TYPE, RDFS.RESOURCE, false));
				connection.close();
			}
		}

		// check that close() called rollback
		try (NotifyingSailConnection connection1 = shaclSail.getConnection()) {
			assertFalse(connection1.hasStatement(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE, false));
		}

		shaclSail.shutDown();

		assertTrue(exception);
	}



	@Test
	public void testAutomaticRollbackRepository() throws IOException {
		SailRepository shaclSail = Utils.getInitializedShaclRepository("shacl.ttl",false);

		boolean exception = false;
		BNode bNode = SimpleValueFactory.getInstance().createBNode();

		SailRepositoryConnection connection = null;
		try {
			connection = shaclSail.getConnection();
			connection.begin();
			connection.add(bNode, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();

		} catch (RepositoryException ignored) {
			exception = true;
		} finally {
			if (connection != null) {
				// check that nothing has been rolled back yet
				assertTrue(connection.hasStatement(bNode, RDF.TYPE, RDFS.RESOURCE, false));
				connection.close();
			}
		}

		// check that close() called rollback
		try (SailRepositoryConnection connection1 = shaclSail.getConnection()) {
			assertFalse(connection1.hasStatement(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE, false));
		}

		shaclSail.shutDown();

		assertTrue(exception);
	}



}
