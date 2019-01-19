package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Test;

public class PrepareCommitTest {

	@Test(expected = Throwable.class)
	public void testFailureWhenChangesAfterPrepare() {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore(), Utils.getSailRepository("shacl.ttl"));
		shaclSail.initialize();

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
	public void testMultiplePrepare() {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore(), Utils.getSailRepository("shacl.ttl"));
		shaclSail.initialize();

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
	public void testWithoutPrepare() {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore(), Utils.getSailRepository("shacl.ttl"));
		shaclSail.initialize();

		try (NotifyingSailConnection connection = shaclSail.getConnection()) {
			connection.begin();
			connection.addStatement(RDFS.RESOURCE, RDFS.SUBCLASSOF, RDFS.RESOURCE);
			connection.commit();

		}

		shaclSail.shutDown();
	}


	@Test
	public void testPrepareAfterRollback() {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore(), Utils.getSailRepository("shacl.ttl"));
		shaclSail.initialize();

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

}
