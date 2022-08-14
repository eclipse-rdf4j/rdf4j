/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MediumConcurrencyTest extends SPARQLBaseTest {

	static final String[] queries = new String[] {
			"query01", "query02", "query03", "query04", "query05", "query06", "query07", "query08", "query09",
			"query10", "query11", "query12"
	};

	private static ExecutorService executor;

	@BeforeAll
	public static void beforeClass() {

		executor = Executors.newFixedThreadPool(10);
	}

	@AfterAll
	public static void afterClass() throws InterruptedException {
		if (executor != null) {
			executor.shutdownNow();
			executor.awaitTermination(30, TimeUnit.SECONDS);
		}
	}

	@Test
	public void queryMix() throws Throwable {

		/* test select query retrieving all persons (2 endpoints) */
		prepareTest(Arrays.asList("/tests/medium/data1.ttl", "/tests/medium/data2.ttl", "/tests/medium/data3.ttl",
				"/tests/medium/data4.ttl"));

		final int MAX_QUERIES = 500;
		final Random rand = new Random(12345);
		final List<Future<String>> futures = new ArrayList<>();

		for (int i = 0; i < MAX_QUERIES; i++) {
			Future<String> f = submit(queries[rand.nextInt(queries.length)], i);
			futures.add(f);
		}

		try {
			final String message = Assertions.assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
				for (Future<String> f : futures) {
					f.get(30, TimeUnit.SECONDS);
				}
				return "OK";
			});
			Assertions.assertEquals("OK", message);
		} catch (Throwable t) {
			futures.forEach(future -> future.cancel(true));
			throw t;
		}

		log.info("Done");
	}

	protected Future<String> submit(final String query, final int queryId) {
		return executor.submit(() -> {
			log.info("Executing query " + queryId + ": " + query);
			execute("/tests/medium/" + query + ".rq", "/tests/medium/" + query + ".srx", false, true);
			// uncomment to simulate canceling case
			// executeReadPartial("/tests/medium/" + query + ".rq");

			return "Ok";
		});
	}

}
