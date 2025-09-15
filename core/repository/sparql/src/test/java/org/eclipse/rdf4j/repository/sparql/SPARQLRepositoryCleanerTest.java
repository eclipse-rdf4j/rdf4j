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
package org.eclipse.rdf4j.repository.sparql;

import static org.assertj.core.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.jupiter.api.Test;

/**
 * Verifies that a SPARQLRepository performs internal shutdown when it becomes unreachable.
 *
 * <p>
 * This test intentionally does not call {@code repository.shutDown()}. It expects the repository to arrange for its
 * internal {@code shutDownInternal()} to run when the object is no longer reachable (e.g., by using Java 9 Cleaner).
 * </p>
 */
public class SPARQLRepositoryCleanerTest {

	@Test
	void autoShutdownOnUnreachable() throws Exception {
		CountDownLatch shutdownInvoked = new CountDownLatch(1);

		runRepo(shutdownInvoked);

		// Encourage GC and wait briefly for cleaner to run
		boolean observed = false;
		for (int i = 0; i < 20 && !observed; i++) {
			System.gc();
			System.runFinalization();
			observed = shutdownInvoked.await(250, TimeUnit.MILLISECONDS);
		}

		if (!observed) {
			fail("Expected shutDownInternal() to be invoked when SPARQLRepository became unreachable");
		}
	}

	private static void runRepo(CountDownLatch shutdownInvoked) {
		// Create a repository instance that signals when shutDownInternal() is invoked
		SPARQLRepository repo = new SPARQLRepository("http://example.org/sparql") {
			@Override
			protected void shutDownInternal() throws RepositoryException {
				try {
					super.shutDownInternal();
				} finally {
					shutdownInvoked.countDown();
				}
			}
		};

		// Exercise minimal usage without hitting the network
		try (RepositoryConnection conn = repo.getConnection()) {
			// no-op
		}

		// Drop strong reference and ask GC to collect
		repo = null;
	}
}
