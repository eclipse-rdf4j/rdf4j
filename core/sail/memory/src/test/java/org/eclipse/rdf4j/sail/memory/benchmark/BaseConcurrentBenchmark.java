/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.memory.benchmark;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@State(Scope.Benchmark)
public class BaseConcurrentBenchmark {

	Repository repository;
	private ExecutorService executorService;

	static InputStream getResourceAsStream(String filename) {
		return BaseConcurrentBenchmark.class.getClassLoader().getResourceAsStream(filename);
	}

	@Setup(Level.Trial)
	public void setup() throws Exception {
		if (executorService != null) {
			executorService.shutdownNow();
		}
		executorService = Executors.newFixedThreadPool(8);
	}

	@TearDown(Level.Trial)
	public void tearDown() throws Exception {
		if (executorService != null) {
			executorService.shutdownNow();
			executorService = null;
		}
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

	Future<?> submit(Runnable runnable) {
		return executorService.submit(runnable);
	}

	Runnable getRunnable(CountDownLatch startSignal, RepositoryConnection connection,
			IsolationLevel isolationLevel, Consumer<RepositoryConnection> workload) {

		return () -> {
			try {
				startSignal.await();
			} catch (InterruptedException e) {
				throw new IllegalStateException();
			}
			RepositoryConnection localConnection = connection;
			try {
				if (localConnection == null) {
					localConnection = repository.getConnection();
				}

				if (isolationLevel == null) {
					localConnection.begin();
				} else {
					localConnection.begin(isolationLevel);
				}
				workload.accept(localConnection);
				localConnection.commit();

			} finally {
				if (connection == null) {
					assert localConnection != null;
					localConnection.close();
				}
			}
		};
	}

}
