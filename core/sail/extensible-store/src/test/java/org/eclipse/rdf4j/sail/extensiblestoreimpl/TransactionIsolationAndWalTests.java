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
import org.junit.Ignore;
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
	@Ignore
	public void testReadCommittedLargeTransaction() throws InterruptedException {
		SailRepository repository = new SailRepository(new ExtensibleStoreImplForTests());

		int count = 100000;

		AtomicBoolean failure = new AtomicBoolean(false);

		Runnable runnable = () -> {

			try (SailRepositoryConnection connection = repository.getConnection()) {
				while (true) {
					connection.begin(IsolationLevels.READ_COMMITTED);
					long size = connection.size();
					connection.commit();
					if (size != 0) {
						if (size != count) {
							System.out.println("Size was " + size + ". Expected " + count);
							failure.set(true);
						}
						break;
					}
					Thread.yield();
				}
			}
		};

		Thread thread = new Thread(runnable);
		thread.start();

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.READ_COMMITTED);
			for (int i = 0; i < count; i++) {
				connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral(i));
			}
			connection.commit();

			assertEquals(count, connection.size());

		}

		thread.join();

		assertFalse(failure.get());

	}

}
