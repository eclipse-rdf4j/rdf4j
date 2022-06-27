/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

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
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms64M", "-Xmx512M" })
//@Fork(value = 1, jvmArgs = { "-Xms1G", "-Xmx1G", "-XX:StartFlightRecording=delay=20s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints" })
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ParallelMixedReadWriteBenchmark extends BaseConcurrentBenchmark {

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

	private Model data;

	public static void main(String[] args) throws Exception {
		Options opt = new OptionsBuilder()
				.include("ParallelMixedReadWriteBenchmark.*") // adapt to run other benchmark tests
				.forks(1)
				.build();

		new Runner(opt).run();
	}

	@Setup(Level.Trial)
	public void setup() throws Exception {
		super.setup();
		Logger root = (Logger) LoggerFactory.getLogger("org.eclipse.rdf4j.sail.memory.MemorySailStore");
		root.setLevel(ch.qos.logback.classic.Level.DEBUG);

		repository = new SailRepository(new MemoryStore());
		try (InputStream resourceAsStream = getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl")) {
			data = Rio.parse(resourceAsStream, RDFFormat.TURTLE);
		}
		try (RepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.add(data);
			connection.commit();
		}
	}

	@TearDown(Level.Trial)
	public void tearDown() throws Exception {
		super.tearDown();

		Logger root = (Logger) LoggerFactory.getLogger("org.eclipse.rdf4j.sail.memory.MemorySailStore");
		root.setLevel(ch.qos.logback.classic.Level.WARN);

		if (repository != null) {
			repository.shutDown();
			repository = null;
		}

		System.gc();
		Thread.sleep(100);
		System.gc();
		Thread.sleep(100);
	}

	@TearDown(Level.Invocation)
	public void clearAfterInvocation() throws Exception {
		try (RepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			try (RepositoryResult<Resource> contextIDs = connection.getContextIDs()) {
				for (Resource contextID : contextIDs) {
					if (contextID != null) {
//						System.out.println("Clearing: " + contextID);
						connection.clear(contextID);
					}
				}
			}
			connection.commit();
		}
		System.gc();
		Thread.sleep(100);
		System.gc();
	}

	@Benchmark
	public void mixedQueriesAndReadsAndWrites(Blackhole blackhole) throws InterruptedException {
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
//				System.out.println("Finished query4");

			}));
		}

		for (int i = 0; i < 30; i++) {
			list.add(getRunnable(startSignal, connection, isolationLevel, (localConnection) -> {
				long count = localConnection
						.prepareTupleQuery(query7_pathexpression1)
						.evaluate()
						.stream()
						.count();

				blackhole.consume(count);
//				System.out.println("Finished query7_pathexpression1");

			}));
		}

		for (int i = 0; i < 30; i++) {
			list.add(getRunnable(startSignal, connection, isolationLevel, (localConnection) -> {
				long count = localConnection
						.prepareTupleQuery(query8_pathexpression2)
						.evaluate()
						.stream()
						.count();

				blackhole.consume(count);
//				System.out.println("Finished query8_pathexpression2");

			}));
		}

		for (int i = 0; i < 400; i++) {
			list.add(getRunnable(startSignal, connection, isolationLevel, (localConnection) -> {
				blackhole.consume(localConnection.hasStatement(null, RDF.TYPE, null, false));
//				System.out.println("Finished hasStatement explicit");

			}));
		}

		for (int i = 0; i < 400; i++) {
			list.add(getRunnable(startSignal, connection, isolationLevel, (localConnection) -> {
				blackhole.consume(localConnection.hasStatement(null, RDF.TYPE, null, true));
//				System.out.println("Finished hasStatement inferred");

			}));
		}

		for (int i = 0; i < 20; i++) {
			list.add(getRunnable(startSignal, connection, isolationLevel, (localConnection) -> {
				long count = localConnection
						.prepareTupleQuery(query1)
						.evaluate()
						.stream()
						.count();

				blackhole.consume(count);
//				System.out.println("Finished query1");
			}));
		}

		for (int i = 0; i < 200; i++) {
			list.add(getRunnable(startSignal, connection, isolationLevel, (localConnection) -> {
				for (int j = 0; j < 100; j++) {
					localConnection.add(Values.bnode(), RDFS.LABEL, Values.literal(j),
							Values.iri("http://example.com/g1"));
				}
//				System.out.println("      ### WRITE ### Finished adding data");
			}));
		}

		list.add(getRunnable(startSignal, connection, isolationLevel, (localConnection) -> {
			localConnection.add(data, Values.iri("http://example.com/g2"));
//			System.out.println("      ### WRITE ### Finished loading file");
		}));

		Collections.shuffle(list, new Random(2948234));
		return list;
	}

}
