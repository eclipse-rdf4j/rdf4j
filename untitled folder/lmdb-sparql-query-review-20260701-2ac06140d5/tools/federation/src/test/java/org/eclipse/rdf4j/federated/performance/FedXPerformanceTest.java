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

import java.io.File;
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
 * Can be applied against a scenario created with {@link DataGenerator} and {@link ResultGenerator}. Example data can be
 * produced with {@link #setupData()}.
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
 * query01: avg=280, min=265, max=296
 * query02: avg=6, min=5, max=7
 * query03: avg=11, min=10, max=13
 * query04: avg=259, min=252, max=270
 * query05: avg=85, min=78, max=105
 * query06: avg=44, min=38, max=51
 * query07: avg=118, min=106, max=131
 * query08: avg=2672, min=2598, max=2817
 * query09: avg=49, min=45, max=55
 * query10: avg=96, min=92, max=101
 * query11: avg=3446, min=3381, max=3613
 * </pre>
 *
 *
 * @author Andreas Schwarte
 *
 */
@Disabled("manual performance test implemented as unit test")
public class FedXPerformanceTest extends SPARQLBaseTest {

	static final String[] queries = new String[] {
			"query01", "query02", "query03", "query04", "query05", "query06", "query07", "query08", "query09",
			"query10", "query11" /* , "query12" */
	};

	/**
	 * The package in the test resources classes folder, where data is written to
	 */
	static final String basePackage = "/tests/performance/";

	@Override
	protected void initFedXConfig() {

		// optionally force ASK queries
		// fedxRule.withConfiguration(c -> c.withEnableGroupedSourceSelection(false));
	}

	@Test
	@Disabled("Activate and run for initial one-time setup")
	public void setupData() throws Exception {

		var benchmarkFolder = new File("src/test/resources" + basePackage);

		new DataGenerator().run(benchmarkFolder);
		new ResultGenerator().run(benchmarkFolder);
	}

	@Test
	public void testPerformance() throws Throwable {

		// change this to see the impact of source selection caching
		// default: cached source selection information
		final boolean SOURCE_SELECTION_CACHE = false;

		/* prepare endpoints */
		prepareTest(Arrays.asList(basePackage + "data1.ttl", basePackage + "data2.ttl", basePackage + "data3.ttl",
				basePackage + "data4.ttl"));

		// warm-up
		for (String query : queries) {
			if (!SOURCE_SELECTION_CACHE) {
				fedxRule.getFederationContext().getSourceSelectionCache().invalidate();
			}

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

				if (!SOURCE_SELECTION_CACHE) {
					fedxRule.getFederationContext().getSourceSelectionCache().invalidate();
				}

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
