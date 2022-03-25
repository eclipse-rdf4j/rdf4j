/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.sail;

import java.util.Random;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests thread interrupts on a Sail implementation.
 *
 * @author Arjohn Kampman
 */
public abstract class SailInterruptTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	private Sail store;

	private ValueFactory vf;

	@Before
	public void setUp() throws Exception {
		store = createSail();
		store.init();
		vf = store.getValueFactory();
	}

	protected abstract Sail createSail() throws SailException;

	@After
	public void tearDown() throws Exception {
		store.shutDown();
	}

	@Test
	public void testQueryInterrupt() throws Exception {
		// System.out.println("Preparing data set for query interruption test");
		final Random r = new Random(12345);
		SailConnection con = store.getConnection();
		try {
			con.begin();
			for (int i = 0; i < 1000; i++) {
				insertTestStatement(con, r.nextInt());
			}
			con.commit();
		} catch (Exception e) {
			con.rollback();
			Assert.fail(e.getMessage());
		} finally {
			con.close();
		}

		Runnable queryJob = () -> {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					// System.out.println("query sail...");
					iterateStatements();
				} catch (Throwable t) {
					// t.printStackTrace();
				}
			}
		};

		// System.out.println("Starting query thread...");
		Thread queryThread = new Thread(queryJob);
		queryThread.start();

		queryThread.join(50);

		// System.out.println("Interrupting query thread...");
		queryThread.interrupt();

		// System.out.println("Waiting for query thread to finish...");
		queryThread.join();

		// System.out.println("Verifying that the sail can still be queried...");
		iterateStatements();

		// System.out.println("Done");
	}

	private void insertTestStatement(SailConnection connection, int seed) throws SailException {
		IRI subj = vf.createIRI("http://test#s" + seed % 293);
		IRI pred = vf.createIRI("http://test#p" + seed % 29);
		Literal obj = vf.createLiteral(Integer.toString(seed % 2903));
		connection.addStatement(subj, pred, obj);
	}

	private void iterateStatements() throws SailException {
		try (SailConnection con = store.getConnection();
				CloseableIteration<?, SailException> iter = con.getStatements(null, null, null, true)) {
			while (iter.hasNext()) {
				iter.next();
			}
		}
	}
}
