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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Measurement(iterations = 3)
@Fork(1)
@State(Scope.Benchmark)
public class ValueStoreWalThroughputBenchmark {

	@Param({ "COMMIT", "INTERVAL", "ALWAYS" })
	public String syncPolicy;

	@Param({ "32", "256" })
	public int payloadBytes;

	@Param({ "0", "1000" })
	public int ackEvery;

	private ValueStoreWalConfig config;
	private ValueStoreWAL wal;
	private String lexical;
	private final AtomicInteger seq = new AtomicInteger();

	@Setup(Level.Trial)
	public void setup() throws IOException {
		Path walDir = Files.createTempDirectory("wal-bench-");
		ValueStoreWalConfig.Builder builder = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString());
		builder.syncPolicy(ValueStoreWalConfig.SyncPolicy.valueOf(syncPolicy));
		config = builder.build();
		wal = ValueStoreWAL.open(config);
		lexical = randomAscii(payloadBytes);
	}

	@TearDown(Level.Trial)
	public void tearDown() throws IOException {
		if (wal != null) {
			wal.close();
		}
	}

	@Benchmark
	@Threads(8)
	public void logMint_literal() throws IOException, InterruptedException {
		int id = seq.incrementAndGet();
		long lsn = wal.logMint(id, ValueStoreWalValueKind.LITERAL, lexical, "", "", 0);
		if (ackEvery > 0) {
			// acknowledge durability occasionally
			if ((id % ackEvery) == 0) {
				wal.awaitDurable(lsn);
			}
		}
	}

	@Benchmark
	@Threads(8)
	public void logMint_iri() throws IOException, InterruptedException {
		int id = seq.incrementAndGet();
		long lsn = wal.logMint(id, ValueStoreWalValueKind.IRI, "http://example.com/" + id, "", "", 0);
		if (ackEvery > 0) {
			if ((id % ackEvery) == 0) {
				wal.awaitDurable(lsn);
			}
		}
	}

	private static String randomAscii(int len) {
		StringBuilder sb = new StringBuilder(len);
		ThreadLocalRandom r = ThreadLocalRandom.current();
		for (int i = 0; i < len; i++) {
			// printable ASCII range 32..126
			char c = (char) r.nextInt(32, 127);
			sb.append(c);
		}
		return sb.toString();
	}
}
