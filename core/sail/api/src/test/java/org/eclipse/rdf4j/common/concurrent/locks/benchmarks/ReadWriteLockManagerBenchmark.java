/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.common.concurrent.locks.benchmarks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.concurrent.locks.AbstractReadWriteLockManager;
import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.common.concurrent.locks.ReadPrefReadWriteLockManager;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author Håvard M. Ottestad
 */
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms32M", "-Xmx32M", "-XX:+UseG1GC" })
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ReadWriteLockManagerBenchmark extends BaseLockManagerBenchmark {

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder().include("ReadWriteLockManagerBenchmark.*") // adapt to run other benchmark
				// tests
				.build();

		new Runner(opt).run();
	}

	AbstractReadWriteLockManager getReadWriteLockManager() {
		return new ReadPrefReadWriteLockManager("");
	}

	@Benchmark
	public void onlyReadLocksNoContention(Blackhole blackhole) throws Exception {

		AbstractReadWriteLockManager lockManager = getReadWriteLockManager();

		threads(100, () -> {

			readLocks(lockManager, 100, blackhole);

		});

	}

	@Benchmark
	public void onlyWriteLocks(Blackhole blackhole) throws Exception {

		AbstractReadWriteLockManager lockManager = getReadWriteLockManager();

		threads(1000, () -> {
			try {
				Lock lock = lockManager.getWriteLock();
				lock.release();
				blackhole.consume(lock);

				lock = lockManager.getWriteLock();
				lock.release();
				blackhole.consume(lock);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		});

	}

	@Benchmark
	public void mixedReadHeavy(Blackhole blackhole) throws Exception {

		AbstractReadWriteLockManager lockManager = getReadWriteLockManager();

		threads(100, () -> {
			try {
				Lock lock = lockManager.getWriteLock();
				lock.release();
				blackhole.consume(lock);

				readLocks(lockManager, 100, blackhole);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		});

	}

	@Benchmark
	public void readPriority(Blackhole blackhole) throws Exception {

		AbstractReadWriteLockManager lockManager = getReadWriteLockManager();

		threads(100, () -> {
			try {
				Lock readLock1 = lockManager.getReadLock();

				long before = System.currentTimeMillis();
				readLock1.release();
				blackhole.consume(readLock1);

				for (int i = 0; i < 100; i++) {
					Lock readLock2 = lockManager.getReadLock();
					readLock2.release();
					Thread.onSpinWait();
					blackhole.consume(readLock2);

				}

				Lock readLock3 = lockManager.getReadLock();
				long after = System.currentTimeMillis();
				readLock3.release();
				blackhole.consume(readLock3);

				Lock lock = lockManager.getWriteLock();
				Thread.sleep(after - before);
				lock.release();
				blackhole.consume(lock);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		});

	}

// Very slow!
//	@Benchmark
//	public void writePriority(Blackhole blackhole) throws Exception {
//
//		AbstractReadWriteLockManager lockManager = getReadWriteLockManager();
//
//		threads(100, () -> {
//			try {
//				long before = System.currentTimeMillis();
//				Lock writeLock = lockManager.getWriteLock();
//				long after = System.currentTimeMillis();
//				blackhole.consume(writeLock);
//				Thread.yield();
//				writeLock.release();
//
//				Thread.yield();
//
//				for (int i = 0; i < 100; i++) {
//					Lock readLock = lockManager.getReadLock();
//					blackhole.consume(readLock);
//					Thread.yield();
//					readLock.release();
//				}
//
//				synchronized (lockManager) {
//					Thread.sleep(after - before);
//				}
//
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//
//		});
//
//	}

	@Benchmark
	public void mixed(Blackhole blackhole) throws Exception {

		AbstractReadWriteLockManager lockManager = getReadWriteLockManager();

		threads(1000, () -> {
			try {
				Lock lock = lockManager.getWriteLock();
				lock.release();
				blackhole.consume(lock);

				readLocks(lockManager, 3, blackhole);

				lock = lockManager.getWriteLock();
				lock.release();
				blackhole.consume(lock);

				readLocks(lockManager, 2, blackhole);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		});

	}

	private void readLocks(AbstractReadWriteLockManager lockManager, int size, Blackhole blackhole) {
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

		blackhole.consume(locks);
	}

}
