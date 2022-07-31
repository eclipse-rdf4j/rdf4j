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
package org.eclipse.rdf4j.federated.performance;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.federated.SPARQLBaseTest;
import org.eclipse.rdf4j.federated.generator.DataGenerator;
import org.eclipse.rdf4j.federated.generator.ResultGenerator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Simple manual performance test for FedX.
 * <p>
 * Can be applied against a scenario created with {@link DataGenerator} and {@link ResultGenerator}. An example scenario
 * is on the classpath in "build/test/fedxPerformanceScenario.jar"
 * </p>
 *
 * <p>
 * Note that the performance scenario does include the correctness check, i.e. it does not only evaluate the pure query
 * duration, but also checks the query results against a prepared result.
 * </p>
 *
 * <p>
 * Example performance run in local environment:
 * </p>
 *
 * <pre>
 * query01: avg=916, min=858, max=984
 * query02: avg=5, min=5, max=6
 * query03: avg=17, min=14, max=24
 * query04: avg=902, min=847, max=972
 * query05: avg=145, min=121, max=239
 * query06: avg=57, min=43, max=79
 * query07: avg=276, min=246, max=346
 * query08: avg=2640, min=2083, max=3747
 * query09: avg=72, min=66, max=94
 * query10: avg=785, min=664, max=1006
 * query11: avg=2465, min=2249, max=3707
 * </pre>
 *
 *
 * @author Andreas Schwarte
 *
 */
public class FedXPerformanceTest extends SPARQLBaseTest {

	static final String[] queries = new String[] {
			"query01", "query02", "query03", "query04", "query05", "query06", "query07", "query08", "query09",
			"query10", "query11" /* , "query12" */
	};

	@Test
	@Disabled
	public void testPerformance() throws Throwable {
		String basePackage = "/tests/performance/";

		/* prepare endpoints */
		prepareTest(Arrays.asList(basePackage + "data1.ttl", basePackage + "data2.ttl", basePackage + "data3.ttl",
				basePackage + "data4.ttl"));

		// warm-up
		for (String query : queries) {
			long start = System.currentTimeMillis();
			execute(basePackage + query + ".rq", basePackage + query + ".srx", false, true);
			long duration = System.currentTimeMillis() - start;
			System.out.println("Warmup " + query + " (Duration: " + duration + ")");
		}

		final int maxRuns = 10;
		List<Run> runs = Lists.newArrayList();
		for (int i = 1; i <= maxRuns; i++) {
			System.out.println("Run " + i);
			long runStart = System.currentTimeMillis();
			Run run = new Run(i);
			runs.add(run);
			for (String query : queries) {
				SingleQueryRun queryRun = new SingleQueryRun(query);
				run.addRun(queryRun);
				long start = System.currentTimeMillis();
				execute(basePackage + query + ".rq", basePackage + query + ".srx", false, true);
				long duration = System.currentTimeMillis() - start;
				queryRun.duration = duration;
			}
			System.out.println("Run " + i + " duration: " + (System.currentTimeMillis() - runStart));
		}

		for (String query : queries) {
			long totalDuration = 0;
			long minDuration = Long.MAX_VALUE;
			long maxDuration = Long.MIN_VALUE;
			for (Run run : runs) {
				SingleQueryRun qRun = run.queryRuns.get(query);
				totalDuration += qRun.duration;
				minDuration = qRun.duration < minDuration ? qRun.duration : minDuration;
				maxDuration = qRun.duration > maxDuration ? qRun.duration : maxDuration;
			}
			long avg = totalDuration / runs.size();

			System.out.println(query + ": avg=" + avg + ", min=" + minDuration + ", max=" + maxDuration);

		}

		log.info("Done");
	}

	@Test
	@Disabled // FIXME currently stack overflow error in result comparison due to implementation issue in RDF4J
	public void testRun() throws Exception {
		String basePackage = "/tests/performance/";

		/* prepare endpoints */
		prepareTest(Arrays.asList(basePackage + "data1.ttl", basePackage + "data2.ttl", basePackage + "data3.ttl",
				basePackage + "data4.ttl"));

		String query = "query12";
		execute(basePackage + query + ".rq", basePackage + query + ".srx", false, true);
	}

	static class Run {

		final int runId;

		protected Map<String, SingleQueryRun> queryRuns = Maps.newLinkedHashMap();

		public Run(int runId) {
			super();
			this.runId = runId;
		}

		public void addRun(SingleQueryRun queryRun) {
			queryRuns.put(queryRun.query, queryRun);
		}
	}

	static class SingleQueryRun {

		private final String query;

		private long duration;

		public SingleQueryRun(String query) {
			super();
			this.query = query;
		}
	}

}
