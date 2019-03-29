/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.memory;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.Stream;

public class DuplicateStatementTest {

	@Test
	public void testCount() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.init();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {

			connection.begin();

			connection.addStatement(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
			connection.commit();
			connection.begin();
			connection.addStatement(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
			Assert.assertEquals("Statement should appear once", 1, connection.size());
			connection.commit();
		}

	}

	@Test
	public void testGetStatement() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.init();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {

			connection.begin();

			connection.addStatement(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
			connection.commit();
			connection.begin();
			connection.addStatement(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
			try (Stream<? extends Statement> stream = Iterations
					.stream(connection.getStatements(null, null, null, false))) {
				long count = stream.count();
				Assert.assertEquals("Statement should appear once", 1, count);
			}
			connection.commit();
		}
	}

	@Test
	public void testGetStatementAfterCommit() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.init();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {

			connection.begin();

			connection.addStatement(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
			connection.commit();
			connection.begin();
			connection.addStatement(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
			connection.commit();
			try (Stream<? extends Statement> stream = Iterations
					.stream(connection.getStatements(null, null, null, false))) {
				long count = stream.count();
				Assert.assertEquals("Statement should appear once", 1, count);

			}
		}

	}

	@Test
	public void testCountAfterComit() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.init();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {

			connection.begin();

			connection.addStatement(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
			connection.commit();
			connection.begin();
			connection.addStatement(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
			connection.commit();

			Assert.assertEquals("Statement should appear once", 1, connection.size());
		}
	}

}
