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
package org.eclipse.rdf4j.testsuite.repository.optimistic;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.UnknownTransactionStateException;
import org.eclipse.rdf4j.testsuite.repository.OptimisticIsolationTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test that the Repository correctly supports claimed isolation levels.
 *
 * @author James Leigh
 */
public class IsolationLevelTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	private final Logger logger = LoggerFactory.getLogger(IsolationLevelTest.class);

	/*-----------*
	 * Variables *
	 *-----------*/

	protected Repository store;

	private String failedMessage;

	private Throwable failed;

	/*---------*
	 * Methods *
	 *---------*/

	@Before
	public void setUp() throws Exception {
		store = OptimisticIsolationTest.getEmptyInitializedRepository(IsolationLevelTest.class);
		failed = null;
	}

	@After
	public void tearDown() throws Exception {
		store.shutDown();
	}

	protected boolean isSupported(IsolationLevels level) throws RepositoryException {
		try (RepositoryConnection con = store.getConnection()) {
			try {
				con.begin(level);
				return true;
			} finally {
				con.rollback();
			}
		} catch (UnknownTransactionStateException e) {
			return false;
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
	}

	@Test
	public void testReadCommitted() throws Exception {
		readCommitted(IsolationLevels.READ_COMMITTED);
		rollbackTriple(IsolationLevels.READ_COMMITTED);
		readPending(IsolationLevels.READ_COMMITTED);
	}

	@Test
	public void testSnapshotRead() throws Exception {
		if (isSupported(IsolationLevels.SNAPSHOT_READ)) {
			snapshotRead(IsolationLevels.SNAPSHOT_READ);
			readCommitted(IsolationLevels.SNAPSHOT_READ);
			rollbackTriple(IsolationLevels.SNAPSHOT_READ);
			readPending(IsolationLevels.SNAPSHOT_READ);
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
		} else {
			logger.warn("{} does not support {}", store, IsolationLevels.SERIALIZABLE);
		}
	}

	/**
	 * Every connection must support reading it own changes
	 */
	private void readPending(IsolationLevel level) throws RepositoryException {
		clear(store);
		try (RepositoryConnection con = store.getConnection()) {
			con.begin(level);
			con.add(RDF.NIL, RDF.TYPE, RDF.LIST);
			assertEquals(1, count(con, RDF.NIL, RDF.TYPE, RDF.LIST, false));
			con.remove(RDF.NIL, RDF.TYPE, RDF.LIST);
			con.commit();
		}
	}

	/**
	 * Supports rolling back added triples
	 */
	private void rollbackTriple(IsolationLevel level) throws RepositoryException {
		clear(store);
		try (RepositoryConnection con = store.getConnection()) {
			con.begin(level);
			con.add(RDF.NIL, RDF.TYPE, RDF.LIST);
			con.rollback();
			assertEquals(0, count(con, RDF.NIL, RDF.TYPE, RDF.LIST, false));
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
			try (RepositoryConnection write = store.getConnection()) {
				start.countDown();
				start.await();
				write.begin(level);
				write.add(RDF.NIL, RDF.TYPE, RDF.LIST);
				begin.countDown();
				uncommitted.await(1, TimeUnit.SECONDS);
				write.rollback();
			} catch (Throwable e) {
				fail("Writer failed", e);
			}
		});
		Thread reader = new Thread(() -> {
			try (RepositoryConnection read = store.getConnection()) {
				start.countDown();
				start.await();
				begin.await();
				read.begin(level);
				// must not read uncommitted changes
				long counted = count(read, RDF.NIL, RDF.TYPE, RDF.LIST, false);
				uncommitted.countDown();
				try {
					read.commit();
				} catch (RepositoryException e) {
					// it is okay to abort after a dirty read
					// e.printStackTrace();
					return;
				}
				// not read if transaction is consistent
				assertEquals(0, counted);
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
			try (RepositoryConnection write = store.getConnection()) {
				start.countDown();
				start.await();
				write.begin(level);
				write.add(RDF.NIL, RDF.TYPE, RDF.LIST);
				write.commit();

				begin.countDown();
				observed.await();

				write.begin(level);
				write.remove(RDF.NIL, RDF.TYPE, RDF.LIST);
				write.commit();
				changed.countDown();
			} catch (Throwable e) {
				fail("Writer failed", e);
			}
		});
		Thread reader = new Thread(() -> {
			try (RepositoryConnection read = store.getConnection()) {
				start.countDown();
				start.await();
				begin.await();
				read.begin(level);
				long first = count(read, RDF.NIL, RDF.TYPE, RDF.LIST, false);
				assertEquals(1, first);
				observed.countDown();
				changed.await(1, TimeUnit.SECONDS);
				// observed statements must continue to exist
				long second = count(read, RDF.NIL, RDF.TYPE, RDF.LIST, false);
				try {
					read.commit();
				} catch (RepositoryException e) {
					// it is okay to abort on inconsistency
					// e.printStackTrace();
					read.rollback();
					return;
				}
				// statement must continue to exist if transaction consistent
				assertEquals(first, second);
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
	private void snapshotRead(IsolationLevel level) throws RepositoryException {
		clear(store);
		try (RepositoryConnection con = store.getConnection()) {
			con.begin(level);
			int size = 1;
			for (int i = 0; i < size; i++) {
				insertTestStatement(con, i);
			}
			int counter = 0;
			try (CloseableIteration<? extends Statement, RepositoryException> stmts = con.getStatements(null, null,
					null, false)) {
				while (stmts.hasNext()) {
					Statement st = stmts.next();
					counter++;
					if (counter < size) {
						// remove observed statement to force new state
						con.remove(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
						insertTestStatement(con, size + counter);
						insertTestStatement(con, size + size + counter);
					}
				}
			}
			try {
				con.commit();
			} catch (RepositoryException e) {
				// it is okay to abort after a dirty read
				e.printStackTrace();
				return;
			}
			assertEquals(size, counter);
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
			try {
				try (RepositoryConnection write = store.getConnection()) {
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
				}
			} catch (Throwable e) {
				fail("Writer failed", e);
			}
		});
		Thread reader = new Thread(() -> {
			try (RepositoryConnection read = store.getConnection()) {
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
				} catch (RepositoryException e) {
					// it is okay to abort on inconsistency
					// e.printStackTrace();
					read.rollback();
					return;
				}
				// store must not change if transaction consistent
				assertEquals(first, second);
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
		try (RepositoryConnection prep = store.getConnection()) {
			prep.begin(level);
			prep.add(subj, pred, vf.createLiteral(1));
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
		try (RepositoryConnection check = store.getConnection()) {
			check.begin(level);
			Literal lit = readLiteral(check, subj, pred);
			int val = lit.intValue();
			// val could be 4 or 6 if one transaction was aborted
			if (val != 4 && val != 6) {
				assertEquals(9, val);
			}
			check.commit();
		}
	}

	protected Thread incrementBy(final CountDownLatch start, final CountDownLatch observed, final IsolationLevels level,
			final ValueFactory vf, final IRI subj, final IRI pred, final int by) {
		return new Thread(() -> {
			try (RepositoryConnection con = store.getConnection()) {
				start.countDown();
				start.await();
				con.begin(level);
				Literal o1 = readLiteral(con, subj, pred);
				observed.countDown();
				observed.await(1, TimeUnit.SECONDS);
				con.remove(subj, pred, o1);
				con.add(subj, pred, vf.createLiteral(o1.intValue() + by));
				try {
					con.commit();
				} catch (RepositoryException e) {
					// it is okay to abort on conflict
					// e.printStackTrace();
					con.rollback();
				}
			} catch (Throwable e) {
				fail("Increment " + by + " failed", e);
			}
		});
	}

	private void clear(Repository store) throws RepositoryException {
		try (RepositoryConnection con = store.getConnection()) {
			con.begin();
			con.clear();
			con.commit();
		}
	}

	protected long count(RepositoryConnection con, Resource subj, IRI pred, Value obj, boolean includeInferred,
			Resource... contexts) throws RepositoryException {
		try (CloseableIteration<Statement, RepositoryException> stmts = con.getStatements(subj, pred, obj,
				includeInferred, contexts)) {
			long counter = 0;
			while (stmts.hasNext()) {
				stmts.next();
				counter++;
			}
			return counter;
		}
	}

	protected Literal readLiteral(RepositoryConnection con, final IRI subj, final IRI pred) throws RepositoryException {
		try (CloseableIteration<? extends Statement, RepositoryException> stmts = con.getStatements(subj, pred, null,
				false)) {
			if (!stmts.hasNext()) {
				return null;
			}
			Value obj = stmts.next().getObject();
			if (stmts.hasNext()) {
				org.junit.Assert.fail("multiple literals: " + obj + " and " + stmts.next());
			}
			return (Literal) obj;
		}
	}

	protected void insertTestStatement(RepositoryConnection connection, int i) throws RepositoryException {
		ValueFactory vf = connection.getValueFactory();
		Literal lit = vf.createLiteral(Integer.toString(i), CoreDatatype.XSD.INTEGER);
		connection.add(vf.createIRI("http://test#s" + i), vf.createIRI("http://test#p"), lit,
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
