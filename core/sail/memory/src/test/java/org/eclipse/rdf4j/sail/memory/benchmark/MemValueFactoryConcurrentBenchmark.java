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

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.memory.model.MemIRI;
import org.eclipse.rdf4j.sail.memory.model.MemValue;
import org.eclipse.rdf4j.sail.memory.model.MemValueFactory;
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

import com.google.common.collect.Lists;

/**
 * @author Håvard M. Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms1G", "-Xmx1G", })
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class MemValueFactoryConcurrentBenchmark extends BaseConcurrentBenchmark {

	public static final int BUCKET_SIZE = 10000;
	private SailRepository repository;
	private List<List<Value>> values;

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("MemValueFactoryConcurrentBenchmark.*") // adapt to run other benchmark tests
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

			List<Value> collect = connection.getStatements(null, null, null)
					.stream()
					.flatMap(s -> Stream.of(s.getSubject(), s.getPredicate(), s.getObject()))
					.distinct()
					.map(v -> {
						if (v.isIRI()) {
							return Values.iri(v.stringValue());
						} else if (v.isBNode()) {
							return Values.bnode(v.stringValue());
						} else if (v.isLiteral()) {
							Literal literal = (Literal) v;
							if (literal.getLanguage().isPresent()) {
								return Values.literal(literal.stringValue(), literal.getLanguage().get());
							} else {
								return Values.literal(literal.stringValue(), literal.getDatatype());
							}
						}
						throw new IllegalStateException("Could not map '" + v + "'");
					})
					.collect(Collectors.toList());

			Collections.shuffle(collect, new Random(4583295));

			values = Lists.partition(collect, BUCKET_SIZE);

		}

	}

	@TearDown(Level.Trial)
	public void tearDown() throws Exception {
		super.tearDown();
		repository.shutDown();
	}

	@Benchmark
	public void onlyReads(Blackhole blackhole) throws Exception {

		MemValueFactory valueFactory = (MemValueFactory) repository.getValueFactory();

		Random random = new Random(48593);

		threads(100, () -> {

			List<Value> values = this.values.get(random.nextInt(this.values.size()));

			for (Value value : values) {
				MemValue memValue = valueFactory.getMemValue(value);
				blackhole.consume(memValue);
			}

		});

	}

	@Benchmark
	public void readHeavy(Blackhole blackhole) throws Exception {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.init();
		MemValueFactory valueFactory = (MemValueFactory) memoryStore.getValueFactory();

		Random random = new Random(48593);

		threads(100, () -> {
			Random r = new Random(random.nextInt());
			for (int i = 0; i < BUCKET_SIZE; i++) {
				MemIRI orCreateMemURI = valueFactory
						.getOrCreateMemURI(Values.iri("http://example.com", "" + r.nextInt(BUCKET_SIZE / 10)));
				blackhole.consume(orCreateMemURI);
			}
		});

	}

	@Benchmark
	public void onlyWrites(Blackhole blackhole) throws Exception {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.init();
		MemValueFactory valueFactory = (MemValueFactory) memoryStore.getValueFactory();

		AtomicInteger atomicInteger = new AtomicInteger();

		threads(100, () -> {
			int base = atomicInteger.incrementAndGet();
			for (int i = 0; i < BUCKET_SIZE; i++) {
				IRI iri = valueFactory.createIRI("http://example.com", base + "-" + i);
				blackhole.consume(iri);
			}
		});

	}

}
