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

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.http.client.SharedHttpClientSessionManager;
import org.eclipse.rdf4j.repository.RepositoryConnection;
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
		SPARQLRepository repo = new SPARQLRepository("http://example.org/sparql");

		// Ensure dependent client is created
		try (RepositoryConnection conn = repo.getConnection()) {
			// no-op
		}

		SharedHttpClientSessionManager mgr = (SharedHttpClientSessionManager) repo.getHttpClientSessionManager();

		// Access internal executor to verify shutdown state
		Field f = SharedHttpClientSessionManager.class.getDeclaredField("executor");
		f.setAccessible(true);
		ExecutorService exec = (ExecutorService) f.get(mgr);

		// Drop strong reference and encourage GC to trigger Cleaner
		repo = null;

		boolean cleaned = false;
		for (int i = 0; i < 40 && !cleaned; i++) {
			System.gc();
			System.runFinalization();
			TimeUnit.MILLISECONDS.sleep(100);
			cleaned = exec.isShutdown() || exec.isTerminated();
		}

		assertThat(cleaned)
				.as("dependent session manager executor should be shut down by Cleaner")
				.isTrue();
	}
}
