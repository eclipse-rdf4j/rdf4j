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
package org.eclipse.rdf4j.sail.lmdb.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.benchmark.common.ThemeQueryCatalog;
import org.eclipse.rdf4j.benchmark.rio.util.ThemeDataSetGenerator.Theme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * MEDICAL_RECORDS q9 oscillated across identical runs from a clean learned state (220ms - 1644ms - 667ms - ...,
 * 2026-08-04): completed-query feedback flipped a near-tied anti-join choice every run and priced the correlated
 * probe's outer input at ~2 rows. This guards the fixed steady state: repeated runs with learning live stay in one
 * performance class and no plan ever prices the MINUS probe's outer rows at a crushed fraction of its input.
 */
class LmdbMedicalQ9PlanStabilityIT {

	private static final Theme THEME = Theme.MEDICAL_RECORDS;
	private static final int QUERY_INDEX = 9;
	private static final int RUNS = 6;
	private static final int MAX_EXECUTION_TIME_SECONDS = 60;
	// The oscillation produced ~7.5x excursions (220ms vs 1644ms); JIT and page-cache noise stays well below this.
	private static final double MAX_SLOWDOWN_VS_FASTEST = 5.0d;
	private static final long ABSOLUTE_SLACK_NANOS = TimeUnit.MILLISECONDS.toNanos(400);
	private static final double MIN_CORRELATION_OUTER_ROWS = 300.0d;
	private static final Pattern OUTER_ROWS = Pattern.compile("plannedCorrelationOuterRows=([0-9.,]+[KM]?)");
	private static final Path REPORT_PATH = BenchmarkPathSupport.resolveTarget("medical-q9-plan-stability.txt");

	@Test
	@Timeout(value = 20, unit = TimeUnit.MINUTES)
	void q9StaysInOnePerformanceClassAcrossLearningRuns() throws Exception {
		try (var ignored = BenchmarkJoinEstimatorSupport.forceDefaultPackedOptimizerMode()) {
			ThemeQueryBenchmark benchmark = newBenchmark();
			deleteLearnedSidecars();
			benchmark.setup();
			try {
				String query = ThemeQueryCatalog.queryFor(THEME, QUERY_INDEX);
				long expectedCount = ThemeQueryCatalog.expectedCountBindingValueFor(THEME, QUERY_INDEX)
						.orElseThrow();
				long[] elapsedNanos = new long[RUNS];
				StringBuilder report = new StringBuilder("MEDICAL_RECORDS q9 plan stability (" + RUNS + " runs)\n");
				for (int run = 0; run < RUNS; run++) {
					long started = System.nanoTime();
					long count = benchmark.executeCountQuery(query, expectedCount, MAX_EXECUTION_TIME_SECONDS);
					elapsedNanos[run] = System.nanoTime() - started;
					assertEquals(expectedCount, count, "run " + run);
					String plan = benchmark.explainOptimizedPlan(query);
					List<Double> outerRows = correlationOuterRows(plan);
					report.append(String.format(Locale.ROOT, "run %d: %.1f ms, outerRows=%s%n",
							run, elapsedNanos[run] / 1_000_000.0d, outerRows));
					for (double rows : outerRows) {
						assertTrue(rows >= MIN_CORRELATION_OUTER_ROWS,
								() -> "A learned run re-planned the MINUS probe with crushed outer rows (" + rows
										+ ") — the q9 poison shape: \n" + report + "\n" + plan);
					}
				}
				long fastest = Long.MAX_VALUE;
				for (long nanos : elapsedNanos) {
					fastest = Math.min(fastest, nanos);
				}
				writeReport(report.toString());
				System.out.println(report);
				// Run 0 is excluded: it pays JIT and page-cache warmup. From run 1 on, learned feedback is live
				// and every re-plan must stay in the fast class.
				for (int run = 1; run < RUNS; run++) {
					assertTrue(elapsedNanos[run] <= fastest * MAX_SLOWDOWN_VS_FASTEST + ABSOLUTE_SLACK_NANOS,
							"run " + run + " left the fast performance class:\n" + report);
				}
			} finally {
				benchmark.tearDown();
			}
		}
	}

	private static ThemeQueryBenchmark newBenchmark() {
		ThemeQueryBenchmark benchmark = new ThemeQueryBenchmark();
		benchmark.themeName = THEME.name();
		benchmark.z_queryIndex = QUERY_INDEX;
		benchmark.sketchEstimatorEnabled = true;
		benchmark.loadOnlySelectedTheme = true;
		return benchmark;
	}

	/** A clean learned state: the loaded store may exist, but no learned sidecar survives into run 0. */
	private static void deleteLearnedSidecars() throws IOException {
		File storeDirectory = BenchmarkPathSupport.resolveTarget("lmdb-theme-query-benchmark")
				.resolve(THEME.name().toLowerCase(Locale.ROOT) + "-only")
				.toFile();
		if (!storeDirectory.isDirectory()) {
			return;
		}
		for (String sidecar : new String[] { "join-estimator.rjes.operators", "join-estimator.rjes.filters" }) {
			Files.deleteIfExists(storeDirectory.toPath().resolve(sidecar));
		}
	}

	private static List<Double> correlationOuterRows(String plan) {
		List<Double> values = new ArrayList<>();
		Matcher matcher = OUTER_ROWS.matcher(plan);
		while (matcher.find()) {
			values.add(parseAbbreviated(matcher.group(1)));
		}
		return values;
	}

	private static double parseAbbreviated(String value) {
		String cleaned = value.replace(",", "");
		double multiplier = 1.0d;
		if (cleaned.endsWith("K")) {
			multiplier = 1_000.0d;
			cleaned = cleaned.substring(0, cleaned.length() - 1);
		} else if (cleaned.endsWith("M")) {
			multiplier = 1_000_000.0d;
			cleaned = cleaned.substring(0, cleaned.length() - 1);
		}
		return Double.parseDouble(cleaned) * multiplier;
	}

	private static void writeReport(String report) throws IOException {
		Files.createDirectories(REPORT_PATH.getParent());
		Files.writeString(REPORT_PATH, report, StandardCharsets.UTF_8);
	}
}
