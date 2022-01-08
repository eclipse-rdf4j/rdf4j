/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.memory.benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
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
@Warmup(iterations = 3)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms1G", "-Xmx1G" })
//@Fork(value = 1, jvmArgs = {"-Xms1G", "-Xmx1G", "-XX:StartFlightRecording=delay=60s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ParallelQueryBenchmark {

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

	private SailRepository repository;

	@Setup(Level.Trial)
	public void beforeClass() throws IOException, InterruptedException {
		repository = new SailRepository(new MemoryStore());

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			try (InputStream resourceAsStream = getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl")) {
				connection.add(resourceAsStream, RDFFormat.TURTLE);
			}
			connection.commit();
		}
	}

	@TearDown(Level.Trial)
	public void afterClass() {
		repository.shutDown();
	}

	@Benchmark
	public void mixedWorkload(Blackhole blackhole) throws InterruptedException {
		ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		CountDownLatch startSignal = new CountDownLatch(1);

		getMixedWorkload(blackhole, startSignal, null)
				.forEach(executorService::submit);

		startSignal.countDown();

		executorService.shutdown();
		while (!executorService.isTerminated()) {
			executorService.awaitTermination(100, TimeUnit.MILLISECONDS);
		}
	}

	private ArrayList<Runnable> getMixedWorkload(Blackhole blackhole, CountDownLatch startSignal,
			SailRepositoryConnection connection) {
		ArrayList<Runnable> list = new ArrayList<>();

		for (int i = 0; i < 10; i++) {
			list.add(getRunnable(startSignal, connection, (localConnection) -> {
				long count = localConnection
						.prepareTupleQuery(query4)
						.evaluate()
						.stream()
						.count();

				blackhole.consume(count);
			}));
		}

		for (int i = 0; i < 10; i++) {
			list.add(getRunnable(startSignal, connection, (localConnection) -> {
				long count = localConnection
						.prepareTupleQuery(query7_pathexpression1)
						.evaluate()
						.stream()
						.count();

				blackhole.consume(count);
			}));
		}

		for (int i = 0; i < 10; i++) {
			list.add(getRunnable(startSignal, connection, (localConnection) -> {
				long count = localConnection
						.prepareTupleQuery(query8_pathexpression2)
						.evaluate()
						.stream()
						.count();

				blackhole.consume(count);
			}));
		}

		for (int i = 0; i < 100; i++) {
			list.add(getRunnable(startSignal, connection, (localConnection) -> {
				blackhole.consume(localConnection.hasStatement(null, RDF.TYPE, null, false));
			}));
		}

		for (int i = 0; i < 100; i++) {
			list.add(getRunnable(startSignal, connection, (localConnection) -> {
				blackhole.consume(localConnection.hasStatement(null, RDF.TYPE, null, true));
			}));
		}

		for (int i = 0; i < 5; i++) {
			list.add(getRunnable(startSignal, connection, (localConnection) -> {
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

	private Runnable getRunnable(CountDownLatch startSignal, SailRepositoryConnection connection,
			Consumer<SailRepositoryConnection> workload) {

		return () -> {
			try {
				startSignal.await();
			} catch (InterruptedException e) {
				throw new IllegalStateException();
			}
			SailRepositoryConnection localConnection = connection;
			try {
				if (localConnection == null) {
					localConnection = repository.getConnection();
				}

				workload.accept(localConnection);

			} finally {
				if (connection == null) {
					localConnection.close();
				}
			}
		};
	}

	private static InputStream getResourceAsStream(String filename) {
		return ParallelQueryBenchmark.class.getClassLoader().getResourceAsStream(filename);
	}
}
