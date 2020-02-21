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
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConflictException;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.SailIsolationLevelTest;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
	public void testLargeTransaction(IsolationLevel isolationLevel, int count) throws InterruptedException {

		long triplesInEmptyStore;

		try (SailConnection connection = store.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.clear();
			connection.commit();

			triplesInEmptyStore = connection.getStatements(null, null, null, true).stream().count();
		}

		AtomicBoolean failure = new AtomicBoolean(false);

		Runnable runnable = () -> {

			try (SailConnection connection = store.getConnection()) {
				while (true) {
					try {

						connection.begin(isolationLevel);
						List<Statement> statements = Iterations
								.asList(connection.getStatements(null, null, null, true));
						connection.commit();
						if (statements.size() != triplesInEmptyStore) {
							if (statements.size() != count * 2 + triplesInEmptyStore) {
								logger.error("Size was {}. Expected {} or {}", statements.size(), triplesInEmptyStore,
										count * 2 + triplesInEmptyStore);
								logger.error("\n[\n\t{}\n]",
										statements.stream()
												.map(Object::toString)
												.reduce((a, b) -> a + " , \n\t" + b)
												.get());

								failure.set(true);
							}
							break;

						}
					} catch (SailConflictException ignored) {
						connection.rollback();
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
			logger.debug("Commit");
			connection.commit();

			assertEquals(count, connection.size());

		}

		logger.debug("Joining thread");
		thread.join();

		assertFalse(failure.get());

	}

}
