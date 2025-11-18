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

package org.eclipse.rdf4j.sail.nativerdf.wal;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * Simple harness to run JMH benchmarks from the IDE or via a Java main.
 *
 * System properties (optional): -Djmh.include=regex (default: ".*Wal.*Benchmark.*") -Djmh.threads=N (default: 8)
 * -Djmh.forks=N (default: 1) -Djmh.warmupIterations=N (default: 3) -Djmh.measurementIterations=N (default: 5)
 * -Djmh.warmupTimeSeconds=N (default: 2) -Djmh.measurementTimeSeconds=N (default: 3)
 * -Djmh.mode=THROUGHPUT|SAMPLE_TIME|... (default: THROUGHPUT) -Djmh.result=path (optional)
 * -Djmh.result.format=text|json|csv (default: text if result provided)
 */
public final class JmhRunnerHarness {

	private JmhRunnerHarness() {
	}

	public static void main(String[] args) throws Exception {
		String include = System.getProperty("jmh.include", ".*Wal.*Benchmark.*");
		int threads = Integer.getInteger("jmh.threads", 8);
		int forks = Integer.getInteger("jmh.forks", 1);
		int warmupIterations = Integer.getInteger("jmh.warmupIterations", 3);
		int measurementIterations = Integer.getInteger("jmh.measurementIterations", 5);
		int warmupTimeSec = Integer.getInteger("jmh.warmupTimeSeconds", 2);
		int measurementTimeSec = Integer.getInteger("jmh.measurementTimeSeconds", 3);
		String modeProp = System.getProperty("jmh.mode", "THROUGHPUT").toUpperCase();

		OptionsBuilder builder = new OptionsBuilder();
		builder.include(include)
				.threads(threads)
				.forks(forks)
				.warmupIterations(warmupIterations)
				.measurementIterations(measurementIterations)
				.warmupTime(TimeValue.seconds(warmupTimeSec))
				.measurementTime(TimeValue.seconds(measurementTimeSec));

		try {
			builder.mode(Mode.valueOf(modeProp));
		} catch (IllegalArgumentException ignored) {
			builder.mode(Mode.Throughput);
		}

		String resultPath = System.getProperty("jmh.result", "").trim();
		if (!resultPath.isEmpty()) {
			String fmt = System.getProperty("jmh.result.format", "text").toLowerCase();
			ResultFormatType rft = ResultFormatType.TEXT;
			switch (fmt) {
			case "json":
				rft = ResultFormatType.JSON;
				break;
			case "csv":
				rft = ResultFormatType.CSV;
				break;
			default:
				rft = ResultFormatType.TEXT;
			}
			builder.result(resultPath).resultFormat(rft);
		}

		Options options = builder.build();
		new Runner(options).run();
	}
}
