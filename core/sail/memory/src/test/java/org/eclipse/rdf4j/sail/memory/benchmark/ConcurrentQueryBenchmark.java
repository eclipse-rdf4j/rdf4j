/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.memory.benchmark;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author Håvard M. Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms1G", "-Xmx1G", })
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ConcurrentQueryBenchmark extends BaseConcurrentBenchmark {

	private SailRepository repository;

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("ConcurrentQueryBenchmark.*") // adapt to run other benchmark tests
				.forks(1)
				.build();

		new Runner(opt).run();
	}

	@Setup(Level.Trial)
	public void setup() throws Exception {
		super.setup();
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
	public void tearDown() throws Exception {
		super.tearDown();
		repository.shutDown();
	}

	@Benchmark
	public void hasStatement(Blackhole blackhole) throws Exception {
		threads(100, () -> {
			try (SailRepositoryConnection connection = repository.getConnection()) {
				for (int i = 0; i < 100; i++) {
					boolean b = connection.hasStatement(null, null, null, true);
					blackhole.consume(b);
				}
			}
		});
	}

	@Benchmark
	public void hasStatementSharedConnection(Blackhole blackhole) throws Exception {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			threads(100, () -> {
				for (int i = 0; i < 100; i++) {
					boolean b = connection.hasStatement(null, null, null, true);
					blackhole.consume(b);
				}
			});
		}
	}

	@Benchmark
	public void getNamespaces(Blackhole blackhole) throws Exception {
		threads(100, () -> {
			try (SailRepositoryConnection connection = repository.getConnection()) {
				for (int i = 0; i < 100; i++) {
					blackhole.consume(connection.getNamespaces().stream().count());
				}
			}
		});
	}

	@Benchmark
	public void getNamespacesSharedConnection(Blackhole blackhole) throws Exception {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			threads(100, () -> {
				for (int i = 0; i < 100; i++) {
					blackhole.consume(connection.getNamespaces().stream().count());
				}
			});
		}
	}

}
