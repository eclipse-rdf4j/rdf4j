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

package org.eclipse.rdf4j.sail.lmdb;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author Piotr Sowiński
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 4, jvmArgs = { "-Xms1G", "-Xmx1G" })
//@Fork(value = 1, jvmArgs = {"-Xms1G", "-Xmx1G", "-XX:StartFlightRecording=jdk.CPUTimeSample#enabled=true,filename=profile.jfr,method-profiling=max","-XX:FlightRecorderOptions=stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Threads(value = 8)
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class RecordIteratorBenchmark {

	private File dataDir;
	private TripleStore tripleStore;

	@Setup(Level.Trial)
	public void setup() throws IOException {
		dataDir = Files.newTemporaryFolder();
		tripleStore = new TripleStore(dataDir, new LmdbStoreConfig("spoc,posc"));

		final int statements = 1_000_000;
		tripleStore.startTransaction();
		for (int i = 0; i < statements; i++) {
			tripleStore.storeTriple(i, i + 1, i + 2, 1, true);
		}
		tripleStore.commit();
	}

	@TearDown(Level.Trial)
	public void tearDown() throws IOException {
		tripleStore.close();
		FileUtils.deleteDirectory(dataDir);
	}

	@Benchmark
	public void iterateAll(Blackhole blackhole) throws IOException {
		try (TxnManager.Txn txn = tripleStore.getTxnManager().createReadTxn()) {
			try (RecordIterator it = tripleStore.getTriples(txn, 0, 0, 0, 1, true)) {
				long[] item;
				while ((item = it.next()) != null) {
					blackhole.consume(item);
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		if (args != null && args.length > 0) {
			Main.main(args);
			return;
		}

		Options options = new OptionsBuilder()
				.include(RecordIteratorBenchmark.class.getSimpleName() + ".iterateAll")
				.forks(0)
				.build();

		new Runner(options).run();
	}
}
