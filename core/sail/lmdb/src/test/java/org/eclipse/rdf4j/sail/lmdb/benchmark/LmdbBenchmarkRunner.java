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

package org.eclipse.rdf4j.sail.lmdb.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Main runner for all LMDB benchmarks.
 * <p>
 * This class provides a convenient way to run all LMDB performance benchmarks. You can also run individual benchmarks
 * by specifying their class names via the include pattern.
 * <p>
 * Available benchmarks:
 * <ul>
 * <li>{@link LmdbStoreBenchmark} - Core read/write performance with configurable indexes and cache sizes</li>
 * <li>{@link LmdbStoreConcurrentBenchmark} - Multi-threaded concurrent access patterns</li>
 * <li>{@link org.eclipse.rdf4j.sail.lmdb.LmdbInternalsBenchmark} - Low-level ValueStore/TripleStore operations</li>
 * <li>{@link TransactionsPerSecondBenchmark} - Transaction throughput benchmarks</li>
 * <li>{@link QueryBenchmark} - SPARQL query benchmarks with real-world data</li>
 * </ul>
 * <p>
 * Usage examples:
 *
 * <pre>
 * # Run all LMDB benchmarks
 * mvn test -Dtest=LmdbBenchmarkRunner
 *
 * # Run specific benchmark
 * java -jar target/benchmarks.jar LmdbStoreBenchmark
 *
 * # Run with specific parameters
 * java -jar target/benchmarks.jar -p tripleIndexes=spoc,posc,ospc LmdbStoreBenchmark
 * </pre>
 */
public class LmdbBenchmarkRunner {

	public static void main(String[] args) throws RunnerException {
		// Default: run all LMDB benchmarks
		String includePattern = args.length > 0 ? args[0] : "Lmdb.*Benchmark";

		Options opt = new OptionsBuilder()
				.include(includePattern)
				.warmupIterations(3)
				.measurementIterations(5)
				.forks(1)
				.jvmArgs("-Xms2G", "-Xmx2G", "-XX:+UseG1GC")
				// Output results in JSON for later analysis
				.result("lmdb-benchmark-results.json")
				.resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
				.build();

		new Runner(opt).run();
	}
}
