/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestoreimpl;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.extensiblestoreimpl.implementation.ExtensibleStoreImplForTests;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TransactionIsolationAndWalTests {

	/*
	 * Checks that there is no leak between transactions. When one transactions adds a lot of data to the store another
	 * transaction should see either nothing added or everything added. Nothing in between.
	 */
	@Test
	public void testReadCommittedLargeTransaction() throws InterruptedException {
		SailRepository repository = new SailRepository(new ExtensibleStoreImplForTests());

		int count = 100000;

		AtomicBoolean failure1 = new AtomicBoolean(false);
		AtomicBoolean failure2 = new AtomicBoolean(false);

		Thread thread1 = new Thread(() -> {

			try (SailRepositoryConnection connection2 = repository.getConnection()) {
				while (true) {
					connection2.begin(IsolationLevels.READ_COMMITTED);
					long size1 = connection2.size();
					connection2.commit();
					if (size1 != 0) {
						if (size1 != count) {
							System.out.println("Size was " + size1 + ". Expected " + count);
							failure1.set(true);
						}
						break;
					}
					Thread.yield();
				}
			}
		});
		thread1.start();

		Thread thread2 = new Thread(() -> {

			try (SailRepositoryConnection connection1 = repository.getConnection()) {
				while (true) {
					connection1.begin(IsolationLevels.READ_COMMITTED);
					long size = connection1.size();
					connection1.commit();
					if (size != 0) {
						if (size != count) {
							System.out.println("Size was " + size + ". Expected " + count);
							failure2.set(true);
						}
						break;
					}
					Thread.yield();
				}
			}
		});
		thread2.start();

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.READ_COMMITTED);
			for (int i = 0; i < count; i++) {
				connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral(i));
			}
			connection.commit();

			assertEquals(count, connection.size());

		}

		thread1.join();
		thread2.join();

		assertFalse(failure1.get());
		assertFalse(failure2.get());

	}

}
