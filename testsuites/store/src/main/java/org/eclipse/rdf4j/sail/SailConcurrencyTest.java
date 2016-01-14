/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail;

import static org.junit.Assert.assertNotNull;

import java.util.Random;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests concurrent read and write access to a Sail implementation.
 * 
 * @author Arjohn Kampman
 */
public abstract class SailConcurrencyTest {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final int MAX_STATEMENT_IDX = 1000;

	private static final long MAX_TEST_TIME = 30 * 1000;

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
	public void setUp()
		throws Exception
	{
		store = createSail();
		store.initialize();
		vf = store.getValueFactory();
	}

	protected abstract Sail createSail()
		throws SailException;

	@After
	public void tearDown()
		throws Exception
	{
		store.shutDown();
	}

	@Test
	public void testGetContextIDs()
		throws Exception
	{
		// Create one thread which writes statements to the repository, on a
		// number of named graphs.
		final Random insertRandomizer = new Random(12345L);
		final Random removeRandomizer = new Random(System.currentTimeMillis());

		Runnable writer = new Runnable() {

			public void run() {
				try {
					SailConnection connection = store.getConnection();
					try {
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
					finally {
						connection.close();
					}
				}
				catch (Throwable t) {
					continueRunning = false;
					fail("Writer failed", t);
				}
			}
		};

		// Create another which constantly calls getContextIDs() on the
		// connection.
		Runnable reader = new Runnable() {

			public void run() {
				try {
					SailConnection connection = store.getConnection();
					try {
						while (continueRunning) {
							CloseableIteration<? extends Resource, SailException> contextIter = connection.getContextIDs();
							try {
								int contextCount = 0;
								while (contextIter.hasNext()) {
									Resource context = contextIter.next();
									assertNotNull(context);
									contextCount++;
								}
//								 System.out.println("Found " + contextCount + " contexts");
							}
							finally {
								contextIter.close();
							}
						}
					}
					finally {
						connection.close();
					}
				}
				catch (Throwable t) {
					continueRunning = false;
					fail("Reader failed", t);
				}
			}
		};

		Thread readerThread1 = new Thread(reader);
		Thread readerThread2 = new Thread(reader);
		Thread writerThread1 = new Thread(writer);
		Thread writerThread2 = new Thread(writer);

		System.out.println("Running concurrency test...");

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
		}
		else {
			System.out.println("Test succeeded");
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

	protected void insertTestStatement(SailConnection connection, int i)
		throws SailException
	{
		// System.out.print("+");
		connection.addStatement(vf.createIRI("http://test#s" + i), vf.createIRI("http://test#p" + i),
				vf.createIRI("http://test#o" + i), vf.createIRI("http://test#context_" + i));
	}

	protected void removeTestStatement(SailConnection connection, int i)
		throws SailException
	{
		// System.out.print("-");
		connection.removeStatements(vf.createIRI("http://test#s" + i), vf.createIRI("http://test#p" + i),
				vf.createIRI("http://test#o" + i), vf.createIRI("http://test#context_" + i));
	}
}
