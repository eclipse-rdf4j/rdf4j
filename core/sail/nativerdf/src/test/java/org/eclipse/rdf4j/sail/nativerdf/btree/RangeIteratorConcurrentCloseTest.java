/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.btree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * An iterator that one thread advances with {@link RecordIterator#next()} while another thread closes it (as a query
 * timeout does) must leave a consistent state: the next insert into the tree notifies the registered iterators and must
 * neither fail in {@link RangeIterator#valueAdded(Node, int)} nor find an iterator that is closed but still registered.
 */
public class RangeIteratorConcurrentCloseTest {

	private static final int VALUES = 3000;

	private static final int ROUNDS = 3000;

	@TempDir
	File tmpDir;

	private BTree btree;

	@BeforeEach
	public void setUp() throws Exception {
		// a small node size gives a tree with several levels
		btree = new BTree(tmpDir, "test", 85, 4);
		for (int i = 0; i < VALUES; i += 2) {
			btree.insert(key(i));
		}
	}

	@AfterEach
	public void tearDown() throws Exception {
		btree.delete();
	}

	private static byte[] key(int i) {
		return ByteBuffer.allocate(4).putInt(i).array();
	}

	@Test
	@Timeout(value = 120, unit = TimeUnit.SECONDS)
	public void closeFromAnotherThreadWhileNextLeavesTheTreeUsable() throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			AtomicReference<Throwable> failure = new AtomicReference<>();
			for (int round = 0; round < ROUNDS; round++) {
				RecordIterator iter = btree.iterateAll();
				CountDownLatch go = new CountDownLatch(1);
				Future<?> reader = pool.submit(() -> {
					try {
						go.await();
						while (iter.next() != null) {
							// read on
						}
					} catch (Throwable t) {
						failure.compareAndSet(null, t);
					}
				});
				int delay = round % 50;
				Future<?> closer = pool.submit(() -> {
					try {
						go.await();
						for (int spin = 0; spin < delay * 20; spin++) {
							Thread.onSpinWait();
						}
						iter.close();
					} catch (Throwable t) {
						failure.compareAndSet(null, t);
					}
				});
				go.countDown();
				reader.get();
				closer.get();

				assertNull(failure.get(), "next() or close() failed in round " + round);
				assertNull(iter.next(), "a closed iterator returns nothing, round " + round);

				// every iterator is closed: an insert notifies none and must not fail
				int v = 1 + 2 * round % VALUES;
				if (v % 2 == 1) {
					btree.insert(key(v));
				}
			}
			// insert everything left: nodes split and notify what is still registered
			for (int i = 1; i < VALUES; i += 2) {
				btree.insert(key(i));
			}
			int count = 0;
			RecordIterator all = btree.iterateAll();
			while (all.next() != null) {
				count++;
			}
			all.close();
			assertEquals(VALUES, count);
		} finally {
			pool.shutdownNow();
		}
	}
}
