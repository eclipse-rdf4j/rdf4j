/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.memory.benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms1G", "-Xmx1G" })
//@Fork(value = 1, jvmArgs = { "-Xms1G", "-Xmx1G", "-XX:StartFlightRecording=delay=20s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints" })
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ParallelQueryBenchmark extends BaseConcurrentBenchmark {

	private static final String query1;
	private static final String query4;
	private static final String query7_pathexpression1;
	private static final String query8_pathexpression2;

	static {
		try {
			query1 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query1.qr"), StandardCharsets.UTF_8);
			query4 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query4.qr"), StandardCharsets.UTF_8);
			query7_pathexpression1 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query7-pathexpression1.qr"),
					StandardCharsets.UTF_8);
			query8_pathexpression2 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query8-pathexpression2.qr"),
					StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Setup(Level.Trial)
	public void setup() throws Exception {
		super.setup();
		repository = new SailRepository(new MemoryStore());

		try (RepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			try (InputStream resourceAsStream = getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl")) {
				connection.add(resourceAsStream, RDFFormat.TURTLE);
			}
			connection.commit();
		}
	}

	@TearDown(Level.Trial)
	public void tearDown() throws Exception {
		repository.shutDown();
		super.tearDown();
	}

	public static void main(String[] args) throws Exception {
		ParallelQueryBenchmark benchmark = new ParallelQueryBenchmark();
		benchmark.setup();
		for (int i = 0; i < 1000; i++) {
			System.out.println(i);
			benchmark.mixedQueriesAndReads(new Blackhole(
					"Today's password is swordfish. I understand instantiating Blackholes directly is dangerous."));
		}
		benchmark.tearDown();

	}

	@Benchmark
	public void mixedQueriesAndReads(Blackhole blackhole) throws InterruptedException {
		CountDownLatch startSignal = new CountDownLatch(1);

		List<Future<?>> collect = getMixedWorkload(blackhole, startSignal, null, null)
				.stream()
				.map(this::submit)
				.collect(Collectors.toList());

		startSignal.countDown();

		for (Future<?> future : collect) {
			try {
				future.get();
			} catch (ExecutionException e) {
				throw new IllegalStateException(e);
			}
		}

	}

	private ArrayList<Runnable> getMixedWorkload(Blackhole blackhole, CountDownLatch startSignal,
			RepositoryConnection connection, IsolationLevel isolationLevel) {
		ArrayList<Runnable> list = new ArrayList<>();

		for (int i = 0; i < 10; i++) {
			list.add(getRunnable(startSignal, connection, isolationLevel, (localConnection) -> {
				long count = localConnection
						.prepareTupleQuery(query4)
						.evaluate()
						.stream()
						.count();

				blackhole.consume(count);
			}));
		}

		for (int i = 0; i < 10; i++) {
			list.add(getRunnable(startSignal, connection, isolationLevel, (localConnection) -> {
				long count = localConnection
						.prepareTupleQuery(query7_pathexpression1)
						.evaluate()
						.stream()
						.count();

				blackhole.consume(count);
			}));
		}

		for (int i = 0; i < 10; i++) {
			list.add(getRunnable(startSignal, connection, isolationLevel, (localConnection) -> {
				long count = localConnection
						.prepareTupleQuery(query8_pathexpression2)
						.evaluate()
						.stream()
						.count();

				blackhole.consume(count);
			}));
		}

		for (int i = 0; i < 100; i++) {
			list.add(getRunnable(startSignal, connection, isolationLevel, (localConnection) -> {
				blackhole.consume(localConnection.hasStatement(null, RDF.TYPE, null, false));
			}));
		}

		for (int i = 0; i < 100; i++) {
			list.add(getRunnable(startSignal, connection, isolationLevel, (localConnection) -> {
				blackhole.consume(localConnection.hasStatement(null, RDF.TYPE, null, true));
			}));
		}

		for (int i = 0; i < 5; i++) {
			list.add(getRunnable(startSignal, connection, isolationLevel, (localConnection) -> {
				long count = localConnection
						.prepareTupleQuery(query1)
						.evaluate()
						.stream()
						.count();

				blackhole.consume(count);
			}));
		}

		Collections.shuffle(list, new Random(2948234));
		return list;
	}

}
