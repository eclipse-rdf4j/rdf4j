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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConflictException;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UnknownSailTransactionStateException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple tests to sanity check that Sail correctly supports claimed isolation levels.
 *
 * @author James Leigh
 */
public abstract class SailIsolationLevelTest {

	@BeforeClass
	public static void setUpClass() {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	private final Logger logger = LoggerFactory.getLogger(SailIsolationLevelTest.class);

	/*-----------*
	 * Variables *
	 *-----------*/

	protected Sail store;

	private ValueFactory vf;

	private String failedMessage;

	private Throwable failed;

	/*---------*
	 * Methods *
	 *---------*/

	@Before
	public void setUp() throws Exception {
		store = createSail();
		store.init();
		vf = store.getValueFactory();
		failed = null;
	}

	@After
	public void tearDown() throws Exception {
		store.shutDown();
	}

	protected abstract Sail createSail() throws SailException;

	protected boolean isSupported(IsolationLevels level) throws SailException {
		SailConnection con = store.getConnection();
		try {
			con.begin(level);
			return true;
		} catch (UnknownSailTransactionStateException e) {
			return false;
		} finally {
			con.rollback();
			con.close();
		}
	}

	@Test
	public void testNone() throws Exception {
		readPending(IsolationLevels.NONE);
	}

	@Test
	public void testReadUncommitted() throws Exception {
		rollbackTriple(IsolationLevels.READ_UNCOMMITTED);
		readPending(IsolationLevels.READ_UNCOMMITTED);
		readPendingWhileActive(IsolationLevels.READ_UNCOMMITTED);
	}

	@Test
	public void testReadCommitted() throws Exception {
		readCommitted(IsolationLevels.READ_COMMITTED);
		rollbackTriple(IsolationLevels.READ_COMMITTED);
		readPending(IsolationLevels.READ_COMMITTED);
		readPendingWhileActive(IsolationLevels.READ_COMMITTED);
	}

	@Test
	public void testSnapshotRead() throws Exception {
		if (isSupported(IsolationLevels.SNAPSHOT_READ)) {
			snapshotRead(IsolationLevels.SNAPSHOT_READ);
			readCommitted(IsolationLevels.SNAPSHOT_READ);
			rollbackTriple(IsolationLevels.SNAPSHOT_READ);
			readPending(IsolationLevels.SNAPSHOT_READ);
			readPendingWhileActive(IsolationLevels.SNAPSHOT_READ);
		} else {
			logger.warn("{} does not support {}", store, IsolationLevels.SNAPSHOT_READ);
		}
	}

	@Test
	public void testSnapshot() throws Exception {
		if (isSupported(IsolationLevels.SNAPSHOT)) {
			snapshot(IsolationLevels.SNAPSHOT);
			snapshotRead(IsolationLevels.SNAPSHOT);
			repeatableRead(IsolationLevels.SNAPSHOT);
			readCommitted(IsolationLevels.SNAPSHOT);
			rollbackTriple(IsolationLevels.SNAPSHOT);
			readPending(IsolationLevels.SNAPSHOT);
			readPendingWhileActive(IsolationLevels.SNAPSHOT);
		} else {
			logger.warn("{} does not support {}", store, IsolationLevels.SNAPSHOT);
		}
	}

	@Test
	public void testSerializable() throws Exception {

		if (isSupported(IsolationLevels.SERIALIZABLE)) {
			serializable(IsolationLevels.SERIALIZABLE);
			snapshot(IsolationLevels.SERIALIZABLE);
			snapshotRead(IsolationLevels.SERIALIZABLE);
			repeatableRead(IsolationLevels.SERIALIZABLE);
			readCommitted(IsolationLevels.SERIALIZABLE);
			rollbackTriple(IsolationLevels.SERIALIZABLE);
			readPending(IsolationLevels.SERIALIZABLE);
			readPendingWhileActive(IsolationLevels.SERIALIZABLE);
		} else {
			logger.warn("{} does not support {}", store, IsolationLevels.SERIALIZABLE);
		}
	}

	@Test
	public void testLargeTransactionReadCommitted() throws InterruptedException {
		if (isSupported(IsolationLevels.READ_COMMITTED)) {
			testLargeTransaction(IsolationLevels.READ_COMMITTED, 1000);
		} else {
			logger.warn("Isolation level not supporter.");
		}
	}

	@Test
	public void testLargeTransactionSnapshot() throws InterruptedException {
		if (isSupported(IsolationLevels.SNAPSHOT)) {
			testLargeTransaction(IsolationLevels.SNAPSHOT, 1000);
		} else {
			logger.warn("Isolation level not supporter.");
		}
	}

	@Test
	public void testLargeTransactionSnapshotRead() throws InterruptedException {
		if (isSupported(IsolationLevels.SNAPSHOT_READ)) {
			testLargeTransaction(IsolationLevels.SNAPSHOT_READ, 1000);
		} else {
			logger.warn("Isolation level not supporter.");
		}
	}

	@Test
	public void testLargeTransactionSerializable() throws InterruptedException {
		if (isSupported(IsolationLevels.SERIALIZABLE)) {
			testLargeTransaction(IsolationLevels.SERIALIZABLE, 1000);
		} else {
			logger.warn("Isolation level not supporter.");
		}
	}

	/*
	 * Checks that there is no leak between transactions. When one transactions adds a lot of data to the store another
	 * transaction should see either nothing added or everything added. Nothing in between.
	 */
	public void testLargeTransaction(IsolationLevel isolationLevel, int count) throws InterruptedException {

		try (SailConnection connection = store.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.clear();
			connection.commit();
		}

		AtomicBoolean failure = new AtomicBoolean(false);

		Runnable runnable = () -> {

			try (SailConnection connection = store.getConnection()) {
				while (true) {
					try {
						connection.begin(isolationLevel);
						List<Statement> statements = Iterations
								.asList(connection.getStatements(null, null, null, false));
						connection.commit();
						if (statements.size() != 0) {
							if (statements.size() != count) {
								logger.error("Size was {}. Expected 0 or {}", statements.size(), count);
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
				connection.addStatement(RDFS.RESOURCE, RDFS.LABEL, vf.createLiteral(i));
			}
			logger.debug("Commit");
			connection.commit();

			assertEquals(count, connection.size());

		}

		logger.debug("Joining thread");
		thread.join();

		assertFalse(failure.get());

	}

	/**
	 * Every connection must support reading it own changes
	 */
	private void readPending(IsolationLevel level) throws SailException {
		clear(store);
		try (SailConnection con = store.getConnection()) {
			con.begin(level);
			con.addStatement(RDF.NIL, RDF.TYPE, RDF.LIST);
			Assert.assertEquals(1, count(con, RDF.NIL, RDF.TYPE, RDF.LIST, false));
			con.removeStatements(RDF.NIL, RDF.TYPE, RDF.LIST);
			con.commit();
		}
	}

	/**
	 * Every connection must support reading its own changes while another iteration is active.
	 */
	private void readPendingWhileActive(IsolationLevel level) throws SailException {
		clear(store);
		try (SailConnection con = store.getConnection()) {
			// open an iteration outside the transaction and leave it open while another transaction is begun and
			// committed
			try (CloseableIteration<? extends Statement, SailException> unusedStatements = con.getStatements(null, null,
					null, true)) {
				con.begin(level);
				con.addStatement(RDF.NIL, RDF.TYPE, RDF.LIST);
				Assert.assertEquals(1, count(con, RDF.NIL, RDF.TYPE, RDF.LIST, false));
				con.removeStatements(RDF.NIL, RDF.TYPE, RDF.LIST);
				con.commit();
			}
		}
	}

	/**
	 * Supports rolling back added triples
	 */
	private void rollbackTriple(IsolationLevel level) throws SailException {
		clear(store);

		try (SailConnection con = store.getConnection()) {
			con.begin(level);
			con.addStatement(RDF.NIL, RDF.TYPE, RDF.LIST);
			con.rollback();
			Assert.assertEquals(0, count(con, RDF.NIL, RDF.TYPE, RDF.LIST, false));
		}
	}

	/**
	 * Read operations must not see uncommitted changes
	 */
	private void readCommitted(final IsolationLevel level) throws Exception {
		clear(store);
		final CountDownLatch start = new CountDownLatch(2);
		final CountDownLatch begin = new CountDownLatch(1);
		final CountDownLatch uncommitted = new CountDownLatch(1);
		Thread writer = new Thread(() -> {
			try (SailConnection write = store.getConnection()) {
				start.countDown();
				start.await();
				write.begin(level);
				write.addStatement(RDF.NIL, RDF.TYPE, RDF.LIST);
				begin.countDown();
				uncommitted.await(1, TimeUnit.SECONDS);
				write.rollback();
			} catch (Throwable e) {
				fail("Writer failed", e);
			}
		});
		Thread reader = new Thread(() -> {
			try (SailConnection read = store.getConnection()) {
				start.countDown();
				start.await();
				begin.await();
				read.begin(level);
				// must not read uncommitted changes
				long counted = count(read, RDF.NIL, RDF.TYPE, RDF.LIST, false);
				uncommitted.countDown();
				try {
					read.commit();
				} catch (SailException e) {
					// it is okay to abort after a dirty read
					// e.printStackTrace();
					read.rollback();
					return;
				}
				// not read if transaction is consistent
				Assert.assertEquals(0, counted);
			} catch (Throwable e) {
				fail("Reader failed", e);
			}
		});
		reader.start();
		writer.start();
		reader.join();
		writer.join();
		assertNotFailed();
	}

	/**
	 * Any statement read in a transaction must remain present until the transaction is over
	 */
	private void repeatableRead(final IsolationLevels level) throws Exception {
		clear(store);
		final CountDownLatch start = new CountDownLatch(2);
		final CountDownLatch begin = new CountDownLatch(1);
		final CountDownLatch observed = new CountDownLatch(1);
		final CountDownLatch changed = new CountDownLatch(1);
		Thread writer = new Thread(() -> {
			try (SailConnection write = store.getConnection()) {
				start.countDown();
				start.await();
				write.begin(level);
				write.addStatement(RDF.NIL, RDF.TYPE, RDF.LIST);
				write.commit();

				begin.countDown();
				observed.await(1, TimeUnit.SECONDS);

				write.begin(level);
				write.removeStatements(RDF.NIL, RDF.TYPE, RDF.LIST);
				write.commit();
				changed.countDown();
			} catch (Throwable e) {
				fail("Writer failed", e);
			}
		});
		Thread reader = new Thread(() -> {
			try (SailConnection read = store.getConnection()) {
				start.countDown();
				start.await();
				begin.await();
				read.begin(level);
				long first = count(read, RDF.NIL, RDF.TYPE, RDF.LIST, false);
				Assert.assertEquals(1, first);
				observed.countDown();
				changed.await(1, TimeUnit.SECONDS);
				// observed statements must continue to exist
				long second = count(read, RDF.NIL, RDF.TYPE, RDF.LIST, false);
				try {
					read.commit();
				} catch (SailException e) {
					// it is okay to abort on inconsistency
					// e.printStackTrace();
					read.rollback();
					return;
				}
				// statement must continue to exist if transaction consistent
				Assert.assertEquals(first, second);
			} catch (Throwable e) {
				fail("Reader failed", e);
			}
		});
		reader.start();
		writer.start();
		reader.join();
		writer.join();
		assertNotFailed();
	}

	/**
	 * Query results must not include statements added after the first result is read
	 */
	private void snapshotRead(IsolationLevel level) throws SailException {
		clear(store);
		try (SailConnection con = store.getConnection()) {
			con.begin(level);
			int size = 1000;
			for (int i = 0; i < size; i++) {
				insertTestStatement(con, i);
			}
			int counter = 0;
			try (CloseableIteration<? extends Statement, SailException> stmts = con.getStatements(null, null, null,
					false)) {
				while (stmts.hasNext()) {
					Statement st = stmts.next();
					counter++;
					if (counter < size) {
						// remove observed statement to force new state
						con.removeStatements(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
						insertTestStatement(con, size + counter);
						insertTestStatement(con, size + size + counter);
					}
				}
			}
			try {
				con.commit();
			} catch (SailException e) {
				// it is okay to abort after a dirty read
				e.printStackTrace();
				return;
			}
			Assert.assertEquals(size, counter);
		}
	}

	/**
	 * Reader observes the complete state of the store and ensure that does not change
	 */
	private void snapshot(final IsolationLevels level) throws Exception {
		clear(store);
		final CountDownLatch start = new CountDownLatch(2);
		final CountDownLatch begin = new CountDownLatch(1);
		final CountDownLatch observed = new CountDownLatch(1);
		final CountDownLatch changed = new CountDownLatch(1);
		Thread writer = new Thread(() -> {
			try (SailConnection write = store.getConnection()) {
				start.countDown();
				start.await();
				write.begin(level);
				insertTestStatement(write, 1);
				write.commit();

				begin.countDown();
				observed.await(1, TimeUnit.SECONDS);

				write.begin(level);
				insertTestStatement(write, 2);
				write.commit();
				changed.countDown();
			} catch (Throwable e) {
				fail("Writer failed", e);
			}
		});
		Thread reader = new Thread(() -> {
			try (SailConnection read = store.getConnection()) {
				start.countDown();
				start.await();
				begin.await();
				read.begin(level);
				long first = count(read, null, null, null, false);
				observed.countDown();
				changed.await(1, TimeUnit.SECONDS);
				// new statements must not be observed
				long second = count(read, null, null, null, false);
				try {
					read.commit();
				} catch (SailException e) {
					// it is okay to abort on inconsistency
					// e.printStackTrace();
					read.rollback();
					return;
				}
				// store must not change if transaction consistent
				Assert.assertEquals(first, second);
			} catch (Throwable e) {
				fail("Reader failed", e);
			}
		});
		reader.start();
		writer.start();
		reader.join();
		writer.join();
		assertNotFailed();
	}

	/**
	 * Two transactions read a value and replace it
	 */
	private void serializable(final IsolationLevels level) throws Exception {
		clear(store);
		final ValueFactory vf = store.getValueFactory();
		final IRI subj = vf.createIRI("http://test#s");
		final IRI pred = vf.createIRI("http://test#p");
		try (SailConnection prep = store.getConnection()) {
			prep.begin(level);
			prep.addStatement(subj, pred, vf.createLiteral(1));
			prep.commit();
		}
		final CountDownLatch start = new CountDownLatch(2);
		final CountDownLatch observed = new CountDownLatch(2);
		Thread t1 = incrementBy(start, observed, level, vf, subj, pred, 3);
		Thread t2 = incrementBy(start, observed, level, vf, subj, pred, 5);
		t2.start();
		t1.start();
		t2.join();
		t1.join();
		assertNotFailed();
		try (SailConnection check = store.getConnection()) {
			check.begin(level);
			Literal lit = readLiteral(check, subj, pred);
			int val = lit.intValue();
			// val could be 4 or 6 if one transaction was aborted
			if (val != 4 && val != 6) {
				Assert.assertEquals(9, val);
			}
			check.commit();
		}
	}

	protected Thread incrementBy(final CountDownLatch start, final CountDownLatch observed, final IsolationLevels level,
			final ValueFactory vf, final IRI subj, final IRI pred, final int by) {
		return new Thread(() -> {
			try (SailConnection con = store.getConnection()) {
				start.countDown();
				start.await();
				con.begin(level);
				Literal o1 = readLiteral(con, subj, pred);
				observed.countDown();
				observed.await(1, TimeUnit.SECONDS);
				con.removeStatements(subj, pred, o1);
				con.addStatement(subj, pred, vf.createLiteral(o1.intValue() + by));
				try {
					con.commit();
				} catch (SailException e) {
					// it is okay to abort on conflict
					// e.printStackTrace();
					con.rollback();
				}
			} catch (Throwable e) {
				fail("Increment " + by + " failed", e);
			}
		});
	}

	private void clear(Sail store) throws SailException {
		try (SailConnection con = store.getConnection()) {
			con.begin();
			con.clear();
			con.commit();
		}
	}

	protected long count(SailConnection con, Resource subj, IRI pred, Value obj, boolean includeInferred,
			Resource... contexts) throws SailException {
		try (CloseableIteration<? extends Statement, SailException> stmts = con.getStatements(subj, pred, obj,
				includeInferred, contexts)) {
			long counter = 0;
			while (stmts.hasNext()) {
				stmts.next();
				counter++;
			}
			return counter;
		}
	}

	protected Literal readLiteral(SailConnection con, final IRI subj, final IRI pred) throws SailException {
		try (CloseableIteration<? extends Statement, SailException> stmts = con.getStatements(subj, pred, null,
				false)) {
			if (!stmts.hasNext()) {
				return null;
			}
			Value obj = stmts.next().getObject();
			if (stmts.hasNext()) {
				Assert.fail("multiple literals: " + obj + " and " + stmts.next());
			}
			return (Literal) obj;
		}
	}

	protected void insertTestStatement(SailConnection connection, int i) throws SailException {
		Literal lit = vf.createLiteral(Integer.toString(i), XSD.INTEGER);
		connection.addStatement(vf.createIRI("http://test#s" + i), vf.createIRI("http://test#p"), lit,
				vf.createIRI("http://test#context_" + i));
	}

	protected synchronized void fail(String message, Throwable t) {
		failedMessage = message;
		failed = t;
	}

	protected synchronized void assertNotFailed() {
		if (failed != null) {
			throw (AssertionError) new AssertionError(failedMessage).initCause(failed);
		}
	}

}
