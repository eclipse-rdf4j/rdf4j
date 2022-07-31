/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.sail;

import static org.junit.Assert.assertNotNull;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests concurrent read and write access to a Sail implementation.
 *
 * @author Arjohn Kampman
 */
public abstract class SailConcurrencyTest {

	private static final Logger logger = LoggerFactory.getLogger(SailConcurrencyTest.class);
	/*-----------*
	 * Constants *
	 *-----------*/

	private static final int MAX_STATEMENTS = 150_000;

	private static final int MAX_STATEMENT_IDX = 1_000;

	private static final long MAX_TEST_TIME = 30 * 1_000;

	/*-----------*
	 * Variables *
	 *-----------*/

	private Sail store;

	private ValueFactory vf;

	private boolean m_failed;

	private boolean continueRunning;

	/*---------*
	 * Methods *
	 *---------*/

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

	protected class UploadTransaction implements Runnable {

		private final IRI context;

		private int txnSize;

		private final CountDownLatch completed;

		private final CountDownLatch otherTxnCommitted;

		private final AtomicInteger targetSize = new AtomicInteger(MAX_STATEMENTS);

		private final boolean rollback;

		public UploadTransaction(CountDownLatch completed, CountDownLatch otherTxnCommitted, IRI context,
				boolean rollback) {
			this.completed = completed;
			this.otherTxnCommitted = otherTxnCommitted;
			this.context = context;
			this.rollback = rollback;
		}

		@Override
		public void run() {
			try {
				try (SailConnection conn = store.getConnection()) {
					conn.begin();
					while (txnSize < targetSize.get()) {
						IRI subject = vf.createIRI("urn:instance-" + txnSize);
						conn.addStatement(subject, RDFS.LABEL, vf.createLiteral("li" + txnSize), context);
						conn.addStatement(subject, RDFS.COMMENT, vf.createLiteral("ci" + txnSize), context);
						txnSize += 2;
					}
					logger.info("Uploaded " + txnSize + " statements");
					if (rollback) {
						otherTxnCommitted.await();
						logger.info("Testing rollback of " + txnSize + " statements");
						conn.rollback();
					} else {
						conn.commit();
						otherTxnCommitted.countDown();
					}
				}
			} catch (Throwable t) {
				logger.error("error while executing transactions", t);
			} finally {
				completed.countDown();
			}
		}

		public void stopAt(int target) {
			targetSize.set(target);
		}

		public int getSize() {
			return txnSize;
		}

	}

	/**
	 * Verifies that two large concurrent transactions in separate contexts do not cause inconsistencies or errors. This
	 * test may fail intermittently rather than consistently, given its dependency on multi-threading.
	 *
	 * @see <a href="https://github.com/eclipse/rdf4j/issues/693">https://github.com/eclipse/rdf4j/issues/693</a>
	 */
	@Test
	public void testConcurrentAddLargeTxn() throws Exception {
		logger.info("executing two large concurrent transactions");
		final CountDownLatch runnersDone = new CountDownLatch(2);
		final CountDownLatch otherTxnCommitted = new CountDownLatch(1);

		final IRI context1 = vf.createIRI("urn:context1");
		final IRI context2 = vf.createIRI("urn:context2");
		UploadTransaction runner1 = new UploadTransaction(runnersDone, otherTxnCommitted, context1, false);
		UploadTransaction runner2 = new UploadTransaction(runnersDone, otherTxnCommitted, context2, false);

		final long start = System.currentTimeMillis();
		new Thread(runner1).start();
		new Thread(runner2).start();

		if (!runnersDone.await(MAX_TEST_TIME / 2, TimeUnit.MILLISECONDS)) {
			// time to wrap up
			int targetSize = Math.max(runner1.getSize(), runner2.getSize());
			runner1.stopAt(targetSize);
			runner2.stopAt(targetSize);
		}
		while (!runnersDone.await(5, TimeUnit.MINUTES)) {
			logger.info("Still waiting for transactions to commit");
		}
		final long finish = System.currentTimeMillis();
		logger.info("committed both txns in " + (finish - start) / 1000 + "s");

		try (SailConnection conn = store.getConnection()) {
			long size1 = conn.size(context1);
			long size2 = conn.size(context2);
			logger.debug("size 1 = {}, size 2 = {}", size1, size2);
			Assert.assertEquals("upload into context 1 should have been fully committed", runner1.getSize(), size1);
			Assert.assertEquals("upload into context 2 should have been fully committed", runner2.getSize(), size2);
		}

	}

	/**
	 * Verifies that two large concurrent transactions in separate contexts do not cause inconsistencies or errors when
	 * one of the transactions rolls back at the end.
	 */
	@Test
	public void testConcurrentAddLargeTxnRollback() throws Exception {
		logger.info("executing two large concurrent transactions");
		final CountDownLatch runnersDone = new CountDownLatch(2);
		final CountDownLatch otherTxnCommitted = new CountDownLatch(1);

		final IRI context1 = vf.createIRI("urn:context1");
		final IRI context2 = vf.createIRI("urn:context2");

		// transaction into context 1 will commit
		UploadTransaction runner1 = new UploadTransaction(runnersDone, otherTxnCommitted, context1, false);

		// transaction into context 2 will rollback
		UploadTransaction runner2 = new UploadTransaction(runnersDone, otherTxnCommitted, context2, true);

		final long start = System.currentTimeMillis();
		new Thread(runner1).start();
		new Thread(runner2).start();

		if (!runnersDone.await(MAX_TEST_TIME / 2, TimeUnit.MILLISECONDS)) {
			// time to wrap up
			int targetSize = Math.max(runner1.getSize(), runner2.getSize());
			runner1.stopAt(targetSize);
			runner2.stopAt(targetSize);
		}
		while (!runnersDone.await(5, TimeUnit.MINUTES)) {
			logger.info("Still waiting for transaction to rollback");
		}
		final long finish = System.currentTimeMillis();
		logger.info("completed both txns in " + (finish - start) / 1000 + "s");

		try (SailConnection conn = store.getConnection()) {
			long size1 = conn.size(context1);
			long size2 = conn.size(context2);
			logger.debug("size 1 = {}, size 2 = {}", size1, size2);
			Assert.assertEquals("upload into context 1 should have been fully committed", runner1.getSize(), size1);
			Assert.assertEquals("upload into context 2 should have been rolled back", 0, size2);
		}

	}

	@Test
	@Ignore("This test takes a long time and accomplishes little extra")
	public void testGetContextIDs() throws Exception {
		// Create one thread which writes statements to the repository, on a
		// number of named graphs.
		final Random insertRandomizer = new Random(12345L);
		final Random removeRandomizer = new Random(System.currentTimeMillis());

		Runnable writer = () -> {
			try {
				try (SailConnection connection = store.getConnection()) {
					while (continueRunning) {
						connection.begin();
						for (int i = 0; i < 10; i++) {
							insertTestStatement(connection, insertRandomizer.nextInt() % MAX_STATEMENT_IDX);
							removeTestStatement(connection, removeRandomizer.nextInt() % MAX_STATEMENT_IDX);
						}
						// System.out.print("*");
						connection.commit();
					}
				}
			} catch (Throwable t) {
				continueRunning = false;
				fail("Writer failed", t);
			}
		};

		// Create another which constantly calls getContextIDs() on the
		// connection.
		Runnable reader = () -> {
			try {
				try (SailConnection connection = store.getConnection()) {
					while (continueRunning) {
						try (CloseableIteration<? extends Resource, SailException> contextIter = connection
								.getContextIDs()) {
							while (contextIter.hasNext()) {
								Resource context = contextIter.next();
								assertNotNull(context);
							}
						}
					}
				}
			} catch (Throwable t) {
				continueRunning = false;
				fail("Reader failed", t);
			}
		};

		Thread readerThread1 = new Thread(reader);
		Thread readerThread2 = new Thread(reader);
		Thread writerThread1 = new Thread(writer);
		Thread writerThread2 = new Thread(writer);

		logger.info("Running concurrency test...");

		continueRunning = true;
		readerThread1.start();
		readerThread2.start();
		writerThread1.start();
		writerThread2.start();

		readerThread1.join(MAX_TEST_TIME);

		continueRunning = false;

		readerThread1.join(1000);
		readerThread2.join(1000);
		writerThread1.join(1000);
		writerThread2.join(1000);

		if (hasFailed()) {
			Assert.fail("Test Failed");
		} else {
			logger.info("Test succeeded");
		}
	}

	protected synchronized void fail(String message, Throwable t) {
		System.err.println(message);
		t.printStackTrace();
		m_failed = true;
	}

	protected synchronized boolean hasFailed() {
		return m_failed;
	}

	protected void insertTestStatement(SailConnection connection, int i) throws SailException {
		// System.out.print("+");
		connection.addStatement(vf.createIRI("http://test#s" + i), vf.createIRI("http://test#p" + i),
				vf.createIRI("http://test#o" + i), vf.createIRI("http://test#context_" + i));
	}

	protected void removeTestStatement(SailConnection connection, int i) throws SailException {
		// System.out.print("-");
		connection.removeStatements(vf.createIRI("http://test#s" + i), vf.createIRI("http://test#p" + i),
				vf.createIRI("http://test#o" + i), vf.createIRI("http://test#context_" + i));
	}
}
