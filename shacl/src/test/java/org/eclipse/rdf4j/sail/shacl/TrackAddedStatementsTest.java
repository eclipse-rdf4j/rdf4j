/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;

/**
 * @author Håvard Ottestad
 */
public class TrackAddedStatementsTest {

	{
		LoggingNode.loggingEnabled = true;
	}

	@Test
	public void testCleanup() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("empty.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();

			assertNull(shaclSailConnection.getAddedStatements());
			assertNull(shaclSailConnection.getAddedStatements());


		}

	}

	@Test
	public void testTransactions() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("empty.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();

			assertNotNull(shaclSailConnection.getAddedStatements());
			assertNotNull(shaclSailConnection.getRemovedStatements());

			connection.commit();

		}

	}

	@Test
	public void testRollback() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("empty.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();

			assertNotNull(shaclSailConnection.getAddedStatements());
			assertNotNull(shaclSailConnection.getRemovedStatements());

			connection.rollback();

			assertNull(shaclSailConnection.getAddedStatements());
			assertNull(shaclSailConnection.getRemovedStatements());

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			assertNull(shaclSailConnection.getAddedStatements());
			assertNull(shaclSailConnection.getRemovedStatements());


		}

	}

	@Test
	public void testValidationFailedCleanup() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("shacl.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			try {
				connection.commit();
			} catch (Throwable e) {
				System.out.println(e.getMessage());
			}

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();

			assertNull(shaclSailConnection.getAddedStatements());
			assertNull(shaclSailConnection.getRemovedStatements());

		}

	}

	@Test
	public void testValidationFailedCausesRollback() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("shacl.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			try {
				connection.commit();
			} catch (Throwable e) {
				System.out.println(e.getMessage());
			}

			assertEquals(0, size(connection));


		}

	}

	@Test
	public void testCleanupOnClose() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("shacl.ttl")));
		shaclSail.initialize();

		SailRepositoryConnection connection = shaclSail.getConnection();
		connection.begin();

		connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

		connection.close();

		ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();

		assertNull(shaclSailConnection.getAddedStatements());
		assertNull(shaclSailConnection.getRemovedStatements());


		assertEquals(0, size(shaclSail));


	}


	@Test
	public void testAddRemoveAddRemove() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("empty.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.begin();

			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);


			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();

			assertEquals(0, size(shaclSailConnection.getAddedStatements()));
			assertEquals(1, size(shaclSailConnection.getRemovedStatements()));

			connection.commit();

		}


	}

	@Test
	public void testAdd() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("empty.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {
			connection.begin();
			//System.out.println(size(connection));

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
//			System.out.println(size(connection));

			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
//			System.out.println(size(connection));

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
//			System.out.println(size(connection));

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();

			assertEquals(1, size(shaclSailConnection.getAddedStatements()));
			assertEquals(0, size(shaclSailConnection.getRemovedStatements()));


			connection.commit();

			System.out.println(size(connection));


		}
	}

	@Test
	public void testAddRemove() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("empty.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {
			connection.begin();
//			System.out.println(size(connection));

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
//			System.out.println(size(connection));

			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
//			System.out.println(size(connection));


			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();

			assertEquals(0, size(shaclSailConnection.getAddedStatements()));
			assertEquals(0, size(shaclSailConnection.getRemovedStatements()));


			connection.commit();

			System.out.println(size(connection));


		}
	}

	@Test
	public void testRemove() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("empty.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.begin();

			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();

			assertEquals(0, size(shaclSailConnection.getAddedStatements()));
			assertEquals(1, size(shaclSailConnection.getRemovedStatements()));

			connection.commit();

		}
	}

	@Test
	public void testRemoveWithoutAdding() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("empty.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.begin();

			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();

			assertEquals(0, size(shaclSailConnection.getAddedStatements()));
			assertEquals(0, size(shaclSailConnection.getRemovedStatements()));

			connection.commit();

		}
	}

	@Test
	public void testSingleRemove() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("empty.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			connection.begin();

			connection.remove(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();

			assertEquals(0, size(shaclSailConnection.getAddedStatements()));
			assertEquals(1, size(shaclSailConnection.getRemovedStatements()));


			connection.commit();

		}
	}

	@Test
	public void testSingleAdd() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("empty.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {
			connection.begin();

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();
			shaclSailConnection.fillAddedAndRemovedStatementRepositories();

			assertEquals(1, size(shaclSailConnection.getAddedStatements()));
			assertEquals(0, size(shaclSailConnection.getRemovedStatements()));

			connection.commit();

		}
	}


	private static long size(RepositoryConnection connection) {
		return Iterations.stream(connection.getStatements(null, null, null)).peek(System.out::println).count();
	}

	private static long size(Repository repo) {
		try (RepositoryConnection connection = repo.getConnection()) {
			return Iterations.stream(connection.getStatements(null, null, null)).peek(System.out::println).count();
		}
	}

}
