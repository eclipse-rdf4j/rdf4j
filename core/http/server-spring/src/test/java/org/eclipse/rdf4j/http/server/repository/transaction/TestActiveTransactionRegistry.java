/**
 * Copyright (c) 2015 Eclipse RDF4J contributors, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.http.server.repository.transaction;

import static org.junit.Assert.fail;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestActiveTransactionRegistry {

	private static final Logger logger = LoggerFactory.getLogger(TestActiveTransactionRegistry.class);

	private ActiveTransactionRegistry registry;

	private RepositoryConnection conn;

	private UUID txnId1;

	private UUID txnId2;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp()
		throws Exception
	{
		System.setProperty(ActiveTransactionRegistry.CACHE_TIMEOUT_PROPERTY, "1");
		registry = ActiveTransactionRegistry.INSTANCE;
		conn = Mockito.mock(RepositoryConnection.class);
		txnId1 = UUID.randomUUID();
		txnId2 = UUID.randomUUID();

	}

	@Test
	public void testTimeoutRepeatedAccess()
		throws Exception
	{
		registry.register(txnId2, conn);

		int count = 0;
		while (count++ < 2) {
			logger.debug("pass {}", count);
			registry.getTransactionConnection(txnId2);
			Thread.sleep(1200);
			registry.returnTransactionConnection(txnId2);
		}

		registry.deregister(txnId2);
		try {
			registry.getTransactionConnection(txnId2);
			fail("should be deregistered");
		}
		catch (RepositoryException e) {
			// fall through, expected
		}
	}

	@Test
	public void testMultithreadedAccess() {

		final CountDownLatch txn1registered = new CountDownLatch(1);

		final CountDownLatch done = new CountDownLatch(2);

		Runnable r1 = new Runnable() {

			@Override
			public void run() {
				registry.register(txnId1, conn);
				txn1registered.countDown();

				try {
					registry.getTransactionConnection(txnId1);
					Thread.sleep(700);
					registry.returnTransactionConnection(txnId1);

					done.countDown();
				}
				catch (RepositoryException | InterruptedException e) {
					e.printStackTrace();
					fail(e.getMessage());
				}
			}
		};

		Runnable r2 = new Runnable() {

			@Override
			public void run() {
				try {
					txn1registered.await();
				}
				catch (InterruptedException e) {
					e.printStackTrace();
					fail(e.getMessage());
				}

				try {
					registry.getTransactionConnection(txnId1);
					Thread.sleep(500);
					registry.returnTransactionConnection(txnId1);

					done.countDown();
				}
				catch (RepositoryException | InterruptedException e) {
					e.printStackTrace();
					fail(e.getMessage());
				}
			}
		};

		Runnable r3 = new Runnable() {

			@Override
			public void run() {
				try {
					done.await();
					registry.deregister(txnId1);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
					fail(e.getMessage());
				}
			}
		};

		Thread t1 = new Thread(r1, "r1");
		Thread t2 = new Thread(r2, "r2");
		Thread t3 = new Thread(r3, "r3");

		t3.start();
		t2.start();
		t1.start();

		try {
			t3.join();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
