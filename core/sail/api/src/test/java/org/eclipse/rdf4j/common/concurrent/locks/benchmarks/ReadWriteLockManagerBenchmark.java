/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks.benchmarks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.common.concurrent.locks.ReadPrefReadWriteLockManager;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

/**
 * @author Håvard M. Ottestad
 */
@Warmup(iterations = 20)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms1G", "-Xmx1G", })
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ReadWriteLockManagerBenchmark extends BaseLockManagerBenchmark {

	@Benchmark
	public void onlyReadLocksNoContention() throws Exception {

		ReadPrefReadWriteLockManager lockManager = new ReadPrefReadWriteLockManager();

		threads(100, () -> {

			readLocks(lockManager, 100);

		});

	}

	@Benchmark
	public void onlyWriteLocks() throws Exception {

		ReadPrefReadWriteLockManager lockManager = new ReadPrefReadWriteLockManager();

		threads(1000, () -> {
			try {
				Lock lock = lockManager.getWriteLock();
				lock.release();

				lock = lockManager.getWriteLock();
				lock.release();

			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		});

	}

	@Benchmark
	public void mixedReadHeavy() throws Exception {

		ReadPrefReadWriteLockManager lockManager = new ReadPrefReadWriteLockManager();

		threads(100, () -> {
			try {
				Lock lock = lockManager.getWriteLock();
				lock.release();

				readLocks(lockManager, 100);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		});

	}

	@Benchmark
	public void mixed() throws Exception {

		ReadPrefReadWriteLockManager lockManager = new ReadPrefReadWriteLockManager();

		threads(1000, () -> {
			try {
				Lock lock = lockManager.getWriteLock();
				lock.release();

				readLocks(lockManager, 3);

				lock = lockManager.getWriteLock();
				lock.release();

				readLocks(lockManager, 2);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		});

	}

	private void readLocks(ReadPrefReadWriteLockManager lockManager, int size) {
		List<Lock> locks = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			try {
				locks.add(lockManager.getReadLock());

			} catch (InterruptedException e) {
				throw new IllegalStateException();
			}
		}

		Collections.shuffle(locks);

		for (Lock lock : locks) {
			lock.release();
		}
	}

}
