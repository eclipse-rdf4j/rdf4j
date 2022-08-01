/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.nativerdf.benchmark;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 0)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms500M", "-Xmx500M", "-XX:+UseParallelGC" })
@Measurement(iterations = 10, batchSize = 1, time = 1, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class OverflowBenchmarkReal {

	@Setup(Level.Trial)
	public void setup() {
		((Logger) (LoggerFactory
				.getLogger("org.eclipse.rdf4j.sail.nativerdf.MemoryOverflowModel")))
						.setLevel(ch.qos.logback.classic.Level.DEBUG);
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("OverflowBenchmarkReal") // adapt to run other benchmark tests
				.build();

		new Runner(opt).run();
	}

	@Benchmark
	public long loadLotsOfData() throws IOException {
		File temporaryFolder = Files.newTemporaryFolder();
		SailRepository sailRepository = null;
		try {
			sailRepository = new SailRepository(new NativeStore(temporaryFolder));

			try (SailRepositoryConnection connection = sailRepository.getConnection()) {

				connection.begin(IsolationLevels.READ_COMMITTED);
				connection.add(
						OverflowBenchmarkReal.class.getClassLoader().getResource("benchmarkFiles/datagovbe-valid.ttl"));
				connection.commit();

				return connection.size();
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
