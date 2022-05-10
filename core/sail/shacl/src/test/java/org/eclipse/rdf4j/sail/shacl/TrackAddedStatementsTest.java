/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Håvard Ottestad
 */
public class TrackAddedStatementsTest {

	@Test
	public void testCleanup() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("empty.trig");
		addDummyData(shaclRepository);

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();

			Assertions.assertNull(shaclSailConnection.addedStatements);
			Assertions.assertNull(shaclSailConnection.removedStatements);
		} finally {
			shaclRepository.shutDown();
		}

	}

	@Test
	public void testTransactions() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("empty.trig");
		addDummyData(shaclRepository);

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();

			Assertions.assertNotNull(shaclSailConnection.addedStatements);
			Assertions.assertNotNull(shaclSailConnection.removedStatements);

			connection.commit();

		} finally {
			shaclRepository.shutDown();
		}

	}

	@Test
	public void testRollback() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("empty.trig");
		addDummyData(shaclRepository);

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();

			Assertions.assertNotNull(shaclSailConnection.addedStatements);
			Assertions.assertNotNull(shaclSailConnection.removedStatements);

			connection.rollback();

			Assertions.assertNull(shaclSailConnection.addedStatements);
			Assertions.assertNull(shaclSailConnection.removedStatements);

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			Assertions.assertNull(shaclSailConnection.addedStatements);
			Assertions.assertNull(shaclSailConnection.removedStatements);

		} finally {
			shaclRepository.shutDown();
		}

	}

	@Test
	public void testTrandactionRollbackCleanup() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shacl.trig");

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

			Assertions.assertNull(shaclSailConnection.addedStatements);
			Assertions.assertNull(shaclSailConnection.removedStatements);

		} finally {
			shaclRepository.shutDown();
		}

	}

	@Test
	public void testValidationFailedCausesRollback() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shacl.trig");

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.begin();

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			try {
				connection.commit();
				Assertions.fail("commit should have failed");
			} catch (RepositoryException e) {
				// do nothing, expected
			}

		}

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			Assertions.assertEquals(0, size(connection));
		} finally {
			shaclRepository.shutDown();
		}

	}

	@Test
	public void testCleanupOnClose() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shacl.trig");

		SailRepositoryConnection connection = shaclRepository.getConnection();
		connection.begin();

		connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

		connection.close();

		ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();

		Assertions.assertNull(shaclSailConnection.addedStatements);
		Assertions.assertNull(shaclSailConnection.removedStatements);

		Assertions.assertEquals(0, size(shaclRepository));

		shaclRepository.shutDown();

	}

	@Test
	public void testAddRemoveAddRemove() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("empty.trig");
		addDummyData(shaclRepository);

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.begin();

			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();
			try (ConnectionsGroup connectionsGroup = shaclSailConnection.getConnectionsGroup()) {

				Assertions.assertEquals(0, size(connectionsGroup.getAddedStatements()));
				Assertions.assertEquals(1, size(connectionsGroup.getRemovedStatements()));
			}

			connection.commit();

		} finally {
			shaclRepository.shutDown();
		}

	}

	@Test
	public void testAdd() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("empty.trig");
		addDummyData(shaclRepository);

		addDummyData(shaclRepository);

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

				Assertions.assertEquals(1, size(connectionsGroup.getAddedStatements()));
				Assertions.assertEquals(0, size(connectionsGroup.getRemovedStatements()));
			}

			connection.commit();

//			System.out.println(size(connection));

		} finally {
			shaclRepository.shutDown();
		}
	}

	private void addDummyData(SailRepository shaclRepository) {
		try (SailRepositoryConnection connection1 = shaclRepository.getConnection()) {
			connection1.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
		}
	}

	@Test
	public void testAddRemove() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("empty.trig");
		addDummyData(shaclRepository);

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

				Assertions.assertEquals(0, size(connectionsGroup.getAddedStatements()));
				Assertions.assertEquals(0, size(connectionsGroup.getRemovedStatements()));
			}

			connection.commit();

//			System.out.println(size(connection));

		} finally {
			shaclRepository.shutDown();
		}
	}

	@Test
	public void testRemove() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("empty.trig");
		addDummyData(shaclRepository);

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

				Assertions.assertEquals(0, size(connectionsGroup.getAddedStatements()));
				Assertions.assertEquals(1, size(connectionsGroup.getRemovedStatements()));
			}

			connection.commit();

		} finally {
			shaclRepository.shutDown();
		}
	}

	@Test
	public void testRemoveWithoutAdding() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("empty.trig");
		addDummyData(shaclRepository);

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.begin();

			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();
			try (ConnectionsGroup connectionsGroup = shaclSailConnection.getConnectionsGroup()) {

				Assertions.assertEquals(0, size(connectionsGroup.getAddedStatements()));
				Assertions.assertEquals(0, size(connectionsGroup.getRemovedStatements()));
			}

			connection.commit();

		} finally {
			shaclRepository.shutDown();
		}
	}

	@Test
	public void testSingleRemove() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("empty.trig");
		addDummyData(shaclRepository);

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.begin();

			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();
			try (ConnectionsGroup connectionsGroup = shaclSailConnection.getConnectionsGroup()) {

				Assertions.assertEquals(0, size(connectionsGroup.getAddedStatements()));
				Assertions.assertEquals(1, size(connectionsGroup.getRemovedStatements()));
			}

			connection.commit();

		} finally {
			shaclRepository.shutDown();
		}
	}

	@Test
	public void testSingleAdd() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("empty.trig");
		addDummyData(shaclRepository);

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
			connection.begin();

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();
			try (ConnectionsGroup connectionsGroup = shaclSailConnection.getConnectionsGroup()) {

				Assertions.assertEquals(1, size(connectionsGroup.getAddedStatements()));
				Assertions.assertEquals(0, size(connectionsGroup.getRemovedStatements()));
			}

			connection.commit();

		} finally {
			shaclRepository.shutDown();
		}
	}

	private static long size(SailConnection connection) {
		return connection.getStatements(null, null, null, true)
				.stream()
				.map(Object::toString)
//					.peek(logger::info)
				.count();
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
