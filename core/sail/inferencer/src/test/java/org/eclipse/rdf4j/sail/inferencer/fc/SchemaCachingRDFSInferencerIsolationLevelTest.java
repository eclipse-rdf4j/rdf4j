/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.inferencer.fc;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConflictException;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.SailIsolationLevelTest;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * An extension of {@link SailIsolationLevelTest} for testing the {@link SchemaCachingRDFSInferencer}.
 */
public class SchemaCachingRDFSInferencerIsolationLevelTest extends SailIsolationLevelTest {

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected Sail createSail() throws SailException {
		// TODO we are testing the inferencer, not the store. We should use a mock here instead of a real memory store.
		return new SchemaCachingRDFSInferencer(new MemoryStore());
	}

	/*
	 * Checks that there is no leak between transactions. When one transactions adds a lot of data to the store another
	 * transaction should see either nothing added or everything added. Nothing in between. Also for inferred
	 * statements.
	 */
	@Override
	public void testLargeTransaction(IsolationLevel isolationLevel, int iterations) {

		IntStream.range(0, iterations).forEach(iteration -> {

			if (store != null)
				store.shutDown();
			store = createSail();

			int count = 1000;
			long triplesInEmptyStore;

			try (SailConnection connection = store.getConnection()) {
				connection.begin();
				connection.addStatement(vf.createBNode(), RDF.TYPE, RDFS.RESOURCE);
				connection.commit();
				connection.begin(IsolationLevels.NONE);
				connection.clear();
				connection.commit();

				triplesInEmptyStore = connection.getStatements(null, null, null, true).stream().count();
			}

			AtomicBoolean failure = new AtomicBoolean(false);

			CountDownLatch countDownLatch = new CountDownLatch(1);

			Runnable runnable = () -> {

				try (SailConnection connection = store.getConnection()) {
					while (true) {
						try {

							countDownLatch.await();
							connection.begin(isolationLevel);
							long actualCount = connection.getStatements(null, null, null, true).stream().count();
							connection.commit();
							if (actualCount != triplesInEmptyStore) {
								if (actualCount != count * 2 + triplesInEmptyStore) {
									logger.error("Size was {}. Expected {} or {}", actualCount,
											triplesInEmptyStore,
											count * 2 + triplesInEmptyStore);
									failure.set(true);
								}
								break;

							}
						} catch (SailConflictException ignored) {
							connection.rollback();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						Thread.yield();
					}
				}
			};

			Thread thread = new Thread(runnable);
			thread.start();

			SimpleValueFactory vf = SimpleValueFactory.getInstance();

			try (SailConnection connection = store.getConnection()) {
				connection.begin(isolationLevel);
				for (int i = 0; i < count; i++) {
					connection.addStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral(i));
				}
				countDownLatch.countDown();
				connection.commit();

				assertEquals(count, connection.size());

			}

			logger.debug("Joining thread");
			try {
				thread.join();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			assertFalse(failure.get());
		});
	}

}
