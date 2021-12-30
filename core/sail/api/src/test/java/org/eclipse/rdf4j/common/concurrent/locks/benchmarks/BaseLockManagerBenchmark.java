/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks.benchmarks;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@State(Scope.Benchmark)
public class BaseLockManagerBenchmark {

	ExecutorService executorService;

	@Setup(Level.Trial)
	public void setUp() throws InterruptedException {
//		Logger root = (Logger) LoggerFactory.getLogger("");
//		root.setLevel(ch.qos.logback.classic.Level.INFO);

		executorService = Executors.newFixedThreadPool(8);
	}

	@TearDown(Level.Trial)
	public void tearDown() throws InterruptedException {
		executorService.shutdownNow();
	}

	void threads(int threadCount, Runnable runnable) throws InterruptedException {

		CountDownLatch latch = new CountDownLatch(1);
		CountDownLatch latchDone = new CountDownLatch(threadCount);

		for (int i = 0; i < threadCount; i++) {
			executorService.submit(() -> {
				try {
					latch.await();
					runnable.run();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					latchDone.countDown();
				}
			});
		}

		latch.countDown();
		latchDone.await();

	}

}
