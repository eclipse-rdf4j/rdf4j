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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Benchmarks loading the datagovbe-valid.ttl dataset in a single transaction across all supported LMDB isolation
 * levels.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1, jvmArgs = { "-Xms2G", "-Xmx2G", "-XX:+UseG1GC" })
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class DatagovLoadIsolationBenchmark {

	private static final String DATA_FILE = "benchmarkFiles/datagovbe-valid.ttl";

	@Param({ "NONE", "READ_COMMITTED", "SNAPSHOT_READ", "SNAPSHOT", "SERIALIZABLE" })
	public IsolationLevels isolationLevel;

	private Model data;

	@Setup(Level.Trial)
	public void setup() throws IOException, InterruptedException {
		try (InputStream resourceAsStream = Objects.requireNonNull(
				DatagovLoadIsolationBenchmark.class.getClassLoader().getResourceAsStream(DATA_FILE),
				"dataset resource not found: " + DATA_FILE)) {
			this.data = Rio.parse(resourceAsStream, "", Rio.getParserFormatForFileName(DATA_FILE).orElseThrow());
		}
		System.gc();
		Thread.sleep(100);
		System.gc();
		Thread.sleep(100);
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(DatagovLoadIsolationBenchmark.class.getSimpleName())
				.forks(0)
				.build();

		new Runner(opt).run();
	}

	@Benchmark
	public boolean loadDatagovFileSingleTransaction() throws IOException {
		return loadOnce();
	}

	boolean loadOnce() throws IOException {
		File temporaryFolder = Files.newTemporaryFolder();
		SailRepository sailRepository = null;
		try {
			sailRepository = new SailRepository(new LmdbStore(temporaryFolder, ConfigUtil.createConfig()));
			try (SailRepositoryConnection connection = sailRepository.getConnection()) {
				connection.begin(isolationLevel);
				connection.add(data);
				connection.commit();
				return connection.hasStatement(null, null, null, true);
			}
		} finally {
			try {
				if (sailRepository != null) {
					sailRepository.shutDown();
				}
			} finally {
				FileUtils.deleteDirectory(temporaryFolder);
			}
		}
	}
}
