/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

public class ShutdownTest {

	@Test
	public void shutdownWhileRunningValidation() throws InterruptedException {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.initialize();

		int activeThreads = Thread.activeCount();

		Future<Object> objectFuture = shaclSail.submitRunnableToExecutorService(() -> {

			for (int i = 0; i < Integer.MAX_VALUE; i++) {
				Thread.sleep(i);
			}

			return null;
		});

		try {
			objectFuture.get(100, TimeUnit.MILLISECONDS);
		} catch (ExecutionException | TimeoutException ignored) {
		}

		shaclSail.shutDown();

		assertEquals(activeThreads, Thread.activeCount());
	}

	@Test
	public void shutdownAndInitializeThreadPool() throws InterruptedException {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		for (int j = 0; j < 3; j++) {
			shaclSail.initialize();

			int activeThreads = Thread.activeCount();

			Future<Object> objectFuture = shaclSail.submitRunnableToExecutorService(() -> {

				for (int i = 0; i < Integer.MAX_VALUE; i++) {
					Thread.sleep(i);
				}

				return null;
			});

			try {
				objectFuture.get(100, TimeUnit.MILLISECONDS);
			} catch (ExecutionException | TimeoutException ignored) {
			}

			shaclSail.shutDown();

			assertEquals(activeThreads, Thread.activeCount());
		}

	}

	@Test(expected = IllegalStateException.class)
	public void threadPoolUninitialized() throws InterruptedException {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());

		shaclSail.submitRunnableToExecutorService(() -> {

			for (int i = 0; i < Integer.MAX_VALUE; i++) {
				Thread.sleep(i);
			}

			return null;
		});

	}

}
