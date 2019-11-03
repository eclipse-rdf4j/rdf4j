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

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;

public class ShutdownTest {

	@Test
	public void shutdownWhileRunningValidation() throws InterruptedException {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.initialize();

		Future<Object> objectFuture = shaclSail.submitRunnableToExecutorService(getSleepingCallable());

		try {
			objectFuture.get(100, TimeUnit.MILLISECONDS);
		} catch (ExecutionException | TimeoutException ignored) {
		}

		assertFalse(objectFuture.isDone());
		shaclSail.shutDown();
		Thread.sleep(100);
		assertTrue("The thread should have be stopped after calling shutdown()", objectFuture.isDone());

	}

	@Test
	public void shutdownAndInitializeThreadPool() throws InterruptedException {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		for (int j = 0; j < 3; j++) {
			shaclSail.initialize();

			Future<Object> objectFuture = shaclSail.submitRunnableToExecutorService(getSleepingCallable());

			try {
				objectFuture.get(100, TimeUnit.MILLISECONDS);
			} catch (ExecutionException | TimeoutException ignored) {
			}

			assertFalse(objectFuture.isDone());
			shaclSail.shutDown();
			Thread.sleep(100);
			assertTrue("The thread should have be stopped after calling shutdown()", objectFuture.isDone());

		}

	}

	@Test
	public void testThatGarbadgeCollectionWillShutdownTheThreadPool()
			throws InterruptedException, NoSuchFieldException, IllegalAccessException {

		ExecutorService[] executorServices = startShaclSailAndTask();

		assertFalse(executorServices[0].isShutdown());

		for (int i = 0; i < 100; i++) {
			System.gc();
			if (executorServices[0].isShutdown()) {
				return;
			}
			System.out.println(i);
			Thread.sleep(100);
		}

		fail("Executor service should have been shutdown due to GC");
	}

	private ExecutorService[] startShaclSailAndTask()
			throws InterruptedException, NoSuchFieldException, IllegalAccessException {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.initialize();

		Future<Object> objectFuture = shaclSail.submitRunnableToExecutorService(getSleepingCallable());

		try {
			objectFuture.get(100, TimeUnit.MILLISECONDS);
		} catch (ExecutionException | TimeoutException ignored) {
		}

		Class<?> c = ShaclSail.class;
		Field field = c.getDeclaredField("executorService");
		field.setAccessible(true);

		return (ExecutorService[]) field.get(shaclSail);

	}

	private Callable<Object> getSleepingCallable() {
		return () -> {

			for (int i = 0; i < Integer.MAX_VALUE; i++) {
				Thread.sleep(10);
			}

			return null;
		};
	}

}
