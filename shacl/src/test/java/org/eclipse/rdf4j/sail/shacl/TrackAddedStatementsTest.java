package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;

public class TrackAddedStatementsTest {

	@Test
	public void testCleanup() {

		SailRepository shaclSail = new SailRepository(new ShaclSail(new MemoryStore(), Utils.getSailRepository("empty.ttl")));
		shaclSail.initialize();

		try (SailRepositoryConnection connection = shaclSail.getConnection()) {

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			ShaclSailConnection shaclSailConnection = (ShaclSailConnection) connection.getSailConnection();

			assertNull(shaclSailConnection.addedStatements);
			assertNull(shaclSailConnection.removedStatements);


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

			assertNotNull(shaclSailConnection.addedStatements);
			assertNotNull(shaclSailConnection.removedStatements);

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

			assertNotNull(shaclSailConnection.addedStatements);
			assertNotNull(shaclSailConnection.removedStatements);

			connection.rollback();

			assertNull(shaclSailConnection.addedStatements);
			assertNull(shaclSailConnection.removedStatements);

			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);

			assertNull(shaclSailConnection.addedStatements);
			assertNull(shaclSailConnection.removedStatements);


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

			assertNull(shaclSailConnection.addedStatements);
			assertNull(shaclSailConnection.removedStatements);

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

			try (RepositoryResult<Statement> statements = connection.getStatements(null, null, null)) {
				assertFalse(statements.hasNext());
			}

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

		assertNull(shaclSailConnection.addedStatements);
		assertNull(shaclSailConnection.removedStatements);

		try (SailRepositoryConnection connection2 = shaclSail.getConnection()) {

			try (RepositoryResult<Statement> statements = connection2.getStatements(null, null, null)) {
				assertFalse(statements.hasNext());
			}

		}


	}

}
