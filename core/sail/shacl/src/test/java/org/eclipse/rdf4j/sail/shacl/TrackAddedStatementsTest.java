/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.fail;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class TrackAddedStatementsTest {

	private static final Logger logger = LoggerFactory.getLogger(TrackAddedStatementsTest.class);

	@Test
	public void testCleanup() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("empty.ttl", false);
		((ShaclSail) shaclRepository.getSail()).setIgnoreNoShapesLoadedException(true);
		shaclRepository.init();

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();

			assertNull(shaclSailConnection.addedStatements);
			assertNull(shaclSailConnection.removedStatements);
		} finally {
			shaclRepository.shutDown();
		}

	}

	@Test
	public void testTransactions() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("empty.ttl", false);
		((ShaclSail) shaclRepository.getSail()).setIgnoreNoShapesLoadedException(true);
		shaclRepository.init();

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();

			assertNotNull(shaclSailConnection.addedStatements);
			assertNotNull(shaclSailConnection.removedStatements);

			connection.commit();

		} finally {
			shaclRepository.shutDown();
		}

	}

	@Test
	public void testRollback() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("empty.ttl", false);
		((ShaclSail) shaclRepository.getSail()).setIgnoreNoShapesLoadedException(true);
		shaclRepository.init();

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();

			assertNotNull(shaclSailConnection.addedStatements);
			assertNotNull(shaclSailConnection.removedStatements);

			connection.rollback();

			assertNull(shaclSailConnection.addedStatements);
			assertNull(shaclSailConnection.removedStatements);

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			assertNull(shaclSailConnection.addedStatements);
			assertNull(shaclSailConnection.removedStatements);

		} finally {
			shaclRepository.shutDown();
		}

	}

	@Test
	public void testTrandactionRollbackCleanup() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shacl.ttl", false);
		((ShaclSail) shaclRepository.getSail()).setIgnoreNoShapesLoadedException(true);
		shaclRepository.init();

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.begin();

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			try {
				connection.commit();
			} catch (Throwable e) {
				System.out.println(e.getMessage());
			}

			connection.rollback();

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();

			assertNull(shaclSailConnection.addedStatements);
			assertNull(shaclSailConnection.removedStatements);

		} finally {
			shaclRepository.shutDown();
		}

	}

	@Test
	public void testValidationFailedCausesRollback() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shacl.ttl", false);
		((ShaclSail) shaclRepository.getSail()).setIgnoreNoShapesLoadedException(true);
		shaclRepository.init();

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.begin();

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			try {
				connection.commit();
				fail("commit should have failed");
			} catch (RepositoryException e) {
				// do nothing, expected
			}

		}

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			assertEquals(0, size(connection));
		} finally {
			shaclRepository.shutDown();
		}

	}

	@Test
	public void testCleanupOnClose() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shacl.ttl", false);
		((ShaclSail) shaclRepository.getSail()).setIgnoreNoShapesLoadedException(true);
		shaclRepository.init();

		SailRepositoryConnection connection = shaclRepository.getConnection();
		connection.begin();

		connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

		connection.close();

		ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();

		assertNull(shaclSailConnection.addedStatements);
		assertNull(shaclSailConnection.removedStatements);

		assertEquals(0, size(shaclRepository));

		shaclRepository.shutDown();

	}

	@Test
	public void testAddRemoveAddRemove() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("empty.ttl", false);
		((ShaclSail) shaclRepository.getSail()).setIgnoreNoShapesLoadedException(true);
		shaclRepository.init();

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.begin();

			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();
			try (ConnectionsGroup connectionsGroup = shaclSailConnection.getConnectionsGroup()) {

				assertEquals(0, size(connectionsGroup.getAddedStatements()));
				assertEquals(1, size(connectionsGroup.getRemovedStatements()));
			}

			connection.commit();

		} finally {
			shaclRepository.shutDown();
		}

	}

	@Test
	public void testAdd() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("empty.ttl", false);
		((ShaclSail) shaclRepository.getSail()).setIgnoreNoShapesLoadedException(true);
		shaclRepository.init();

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			// System.out.println(size(connection));

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			// System.out.println(size(connection));

			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			// System.out.println(size(connection));

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			// System.out.println(size(connection));

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();
			try (ConnectionsGroup connectionsGroup = shaclSailConnection.getConnectionsGroup()) {

				assertEquals(1, size(connectionsGroup.getAddedStatements()));
				assertEquals(0, size(connectionsGroup.getRemovedStatements()));
			}

			connection.commit();

//			System.out.println(size(connection));

		} finally {
			shaclRepository.shutDown();
		}
	}

	@Test
	public void testAddRemove() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("empty.ttl", false);
		((ShaclSail) shaclRepository.getSail()).setIgnoreNoShapesLoadedException(true);
		shaclRepository.init();

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();
			// System.out.println(size(connection));

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			// System.out.println(size(connection));

			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			// System.out.println(size(connection));

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();
			try (ConnectionsGroup connectionsGroup = shaclSailConnection.getConnectionsGroup()) {

				assertEquals(0, size(connectionsGroup.getAddedStatements()));
				assertEquals(0, size(connectionsGroup.getRemovedStatements()));
			}

			connection.commit();

//			System.out.println(size(connection));

		} finally {
			shaclRepository.shutDown();
		}
	}

	@Test
	public void testRemove() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("empty.ttl", false);
		((ShaclSail) shaclRepository.getSail()).setIgnoreNoShapesLoadedException(true);
		shaclRepository.init();

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.begin();

			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();
			try (ConnectionsGroup connectionsGroup = shaclSailConnection.getConnectionsGroup()) {

				assertEquals(0, size(connectionsGroup.getAddedStatements()));
				assertEquals(1, size(connectionsGroup.getRemovedStatements()));
			}

			connection.commit();

		} finally {
			shaclRepository.shutDown();
		}
	}

	@Test
	public void testRemoveWithoutAdding() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("empty.ttl", false);
		((ShaclSail) shaclRepository.getSail()).setIgnoreNoShapesLoadedException(true);
		shaclRepository.init();

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.begin();

			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();
			try (ConnectionsGroup connectionsGroup = shaclSailConnection.getConnectionsGroup()) {

				assertEquals(0, size(connectionsGroup.getAddedStatements()));
				assertEquals(0, size(connectionsGroup.getRemovedStatements()));
			}

			connection.commit();

		} finally {
			shaclRepository.shutDown();
		}
	}

	@Test
	public void testSingleRemove() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("empty.ttl", false);
		((ShaclSail) shaclRepository.getSail()).setIgnoreNoShapesLoadedException(true);
		shaclRepository.init();

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.begin();

			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();
			try (ConnectionsGroup connectionsGroup = shaclSailConnection.getConnectionsGroup()) {

				assertEquals(0, size(connectionsGroup.getAddedStatements()));
				assertEquals(1, size(connectionsGroup.getRemovedStatements()));
			}

			connection.commit();

		} finally {
			shaclRepository.shutDown();
		}
	}

	@Test
	public void testSingleAdd() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("empty.ttl", false);
		((ShaclSail) shaclRepository.getSail()).setIgnoreNoShapesLoadedException(true);
		shaclRepository.init();

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();
			try (ConnectionsGroup connectionsGroup = shaclSailConnection.getConnectionsGroup()) {

				assertEquals(1, size(connectionsGroup.getAddedStatements()));
				assertEquals(0, size(connectionsGroup.getRemovedStatements()));
			}

			connection.commit();

		} finally {
			shaclRepository.shutDown();
		}
	}

	private static long size(SailConnection connection) {
		try {
			return connection.getStatements(null, null, null, true)
					.stream()
					.map(Object::toString)
//					.peek(logger::info)
					.count();
		} finally {
			connection.close();
		}
	}

	private static long size(RepositoryConnection connection) {
		return connection.getStatements(null, null, null)
				.stream()
				.map(Object::toString)
//				.peek(logger::info)
				.count();
	}

	private static long size(Repository repo) {
		try (RepositoryConnection connection = repo.getConnection()) {
			return connection.getStatements(null, null, null)
					.stream()
					.map(Object::toString)
//					.peek(logger::info)
					.count();
		}
	}

}
