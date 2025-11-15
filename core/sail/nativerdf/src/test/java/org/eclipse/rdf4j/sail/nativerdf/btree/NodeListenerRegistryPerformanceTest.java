/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.btree;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NodeListenerRegistryPerformanceTest {

	@Test
	void deregistrationOfLargeListenerSetCompletesQuickly(@TempDir File dataDir) throws IOException {
		try (BTree tree = new BTree(dataDir, "listener", 4096, 64)) {
			Node node = new Node(1, tree);
			int listenerCount = 120_000;
			NodeListener[] listeners = new NodeListener[listenerCount];

			for (int i = 0; i < listenerCount; i++) {
				listeners[i] = new NoOpNodeListener();
				node.register(listeners[i]);
			}

			long started = System.nanoTime();
			for (int i = listenerCount - 1; i >= 0; i--) {
				node.deregister(listeners[i]);
			}
			long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

			Assertions.assertTrue(elapsedMillis < 5_000,
					() -> "deregistering " + listenerCount + " listeners took " + elapsedMillis + "ms");
		}
	}

	@Test
	void concurrentRegistrationsDoNotLeak(@TempDir File dataDir) throws Exception {
		try (BTree tree = new BTree(dataDir, "listener-concurrent", 4096, 64)) {
			Node node = new Node(2, tree);
			int threads = Math.max(4, Runtime.getRuntime().availableProcessors());
			ExecutorService executor = Executors.newFixedThreadPool(threads);
			CountDownLatch latch = new CountDownLatch(1);
			List<Future<?>> futures = new ArrayList<>();
			for (int t = 0; t < threads; t++) {
				futures.add(executor.submit(() -> {
					latch.await();
					for (int i = 0; i < 5_000; i++) {
						NodeListener listener = new NoOpNodeListener();
						NodeListenerHandle handle = node.register(listener);
						if ((i & 1) == 0) {
							handle.remove();
						} else {
							node.deregister(listener);
						}
					}
					return null;
				}));
			}
			latch.countDown();
			for (Future<?> future : futures) {
				future.get();
			}
			executor.shutdown();
			executor.awaitTermination(10, TimeUnit.SECONDS);
			Assertions.assertEquals(0, node.getRegisteredListenerCount());
		}
	}

	private static final class NoOpNodeListener implements NodeListener {

		@Override
		public boolean valueAdded(Node node, int addedIndex) {
			return false;
		}

		@Override
		public boolean valueRemoved(Node node, int removedIndex) {
			return false;
		}

		@Override
		public boolean rotatedLeft(Node node, int valueIndex, Node leftChildNode, Node rightChildNode) {
			return false;
		}

		@Override
		public boolean rotatedRight(Node node, int valueIndex, Node leftChildNode, Node rightChildNode) {
			return false;
		}

		@Override
		public boolean nodeSplit(Node node, Node newNode, int medianIdx) {
			return false;
		}

		@Override
		public boolean nodeMergedWith(Node sourceNode, Node targetNode, int mergeIdx) {
			return false;
		}
	}
}
