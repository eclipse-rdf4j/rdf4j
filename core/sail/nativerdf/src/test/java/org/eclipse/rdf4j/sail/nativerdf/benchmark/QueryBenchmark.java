/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.nativerdf.benchmark;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms1G", "-Xmx1G" })
//@Fork(value = 1, jvmArgs = {"-Xms1G", "-Xmx1G", "-XX:StartFlightRecording=delay=60s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class QueryBenchmark {

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	private SailRepository repository;

	private static final String query1;
	private static final String query4;
	private static final String query7_pathexpression1;
	private static final String query8_pathexpression2;

	private static final String common_themes;
	private static final String different_datasets_with_similar_distributions;
	private static final String long_chain;
	private static final String lots_of_optional;
	private static final String minus;
	private static final String nested_optionals;
	private static final String particularly_large_join_surface;
	private static final String query_distinct_predicates;
	private static final String simple_filter_not;
	private static final String wild_card_chain_with_common_ends;

	static {
		try {
			common_themes = IOUtils.toString(getResourceAsStream("benchmarkFiles/common-themes.qr"),
					StandardCharsets.UTF_8);
			different_datasets_with_similar_distributions = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/different-datasets-with-similar-distributions.qr"),
					StandardCharsets.UTF_8);
			long_chain = IOUtils.toString(getResourceAsStream("benchmarkFiles/long-chain.qr"), StandardCharsets.UTF_8);
			lots_of_optional = IOUtils.toString(getResourceAsStream("benchmarkFiles/lots-of-optional.qr"),
					StandardCharsets.UTF_8);
			minus = IOUtils.toString(getResourceAsStream("benchmarkFiles/minus.qr"), StandardCharsets.UTF_8);
			nested_optionals = IOUtils.toString(getResourceAsStream("benchmarkFiles/nested-optionals.qr"),
					StandardCharsets.UTF_8);
			particularly_large_join_surface = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/particularly-large-join-surface.qr"), StandardCharsets.UTF_8);
			query1 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query1.qr"), StandardCharsets.UTF_8);
			query4 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query4.qr"), StandardCharsets.UTF_8);
			query7_pathexpression1 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query7-pathexpression1.qr"),
					StandardCharsets.UTF_8);
			query8_pathexpression2 = IOUtils.toString(getResourceAsStream("benchmarkFiles/query8-pathexpression2.qr"),
					StandardCharsets.UTF_8);
			query_distinct_predicates = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/query-distinct-predicates.qr"), StandardCharsets.UTF_8);
			simple_filter_not = IOUtils.toString(getResourceAsStream("benchmarkFiles/simple-filter-not.qr"),
					StandardCharsets.UTF_8);
			wild_card_chain_with_common_ends = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/wild-card-chain-with-common-ends.qr"), StandardCharsets.UTF_8);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("QueryBenchmark") // adapt to run other benchmark tests
				.forks(1)
				.build();

		new Runner(opt).run();
	}

	@Setup(Level.Trial)
	public void beforeClass() throws IOException, InterruptedException {
		tempDir.create();
		File file = tempDir.newFolder();

		repository = new SailRepository(new NativeStore(file, "spoc,ospc,psoc"));

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
		tempDir.delete();
	}

	@Benchmark
	public long groupByQuery() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection
					.prepareTupleQuery(query1)
					.evaluate()
					.stream()
					.count();
		}
	}

	@Benchmark
	public long complexQuery() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection
					.prepareTupleQuery(query4)
					.evaluate()
					.stream()
					.count();
		}
	}

	@Benchmark
	public long pathExpressionQuery1() {

		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection
					.prepareTupleQuery(query7_pathexpression1)
					.evaluate()
					.stream()
					.count();

		}
	}

	@Benchmark
	public long pathExpressionQuery2() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection
					.prepareTupleQuery(query8_pathexpression2)
					.evaluate()
					.stream()
					.count();
		}
	}

//	@Benchmark
//	public long common_themes() {
//		try (SailRepositoryConnection connection = repository.getConnection()) {
//			return connection
//				.prepareTupleQuery(common_themes)
//				.evaluate()
//				.stream()
//				.count();
//		}
//	}

	@Benchmark
	public long different_datasets_with_similar_distributions() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection
					.prepareTupleQuery(different_datasets_with_similar_distributions)
					.evaluate()
					.stream()
					.count();
		}
	}

	@Benchmark
	public long long_chain() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection
					.prepareTupleQuery(long_chain)
					.evaluate()
					.stream()
					.count();
		}
	}

	@Benchmark
	public long lots_of_optional() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection
					.prepareTupleQuery(lots_of_optional)
					.evaluate()
					.stream()
					.count();
		}
	}

	@Benchmark
	public long minus() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection
					.prepareTupleQuery(minus)
					.evaluate()
					.stream()
					.count();
		}
	}

	@Benchmark
	public long nested_optionals() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection
					.prepareTupleQuery(nested_optionals)
					.evaluate()
					.stream()
					.count();
		}
	}

//	@Benchmark
//	public long particularly_large_join_surface() {
//		try (SailRepositoryConnection connection = repository.getConnection()) {
//			return connection
//				.prepareTupleQuery(particularly_large_join_surface)
//				.evaluate()
//				.stream()
//				.count();
//		}
//	}

	@Benchmark
	public long query_distinct_predicates() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection
					.prepareTupleQuery(query_distinct_predicates)
					.evaluate()
					.stream()
					.count();
		}
	}

	@Benchmark
	public long simple_filter_not() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return connection
					.prepareTupleQuery(simple_filter_not)
					.evaluate()
					.stream()
					.count();
		}
	}

//	@Benchmark
//	public long wild_card_chain_with_common_ends() {
//		try (SailRepositoryConnection connection = repository.getConnection()) {
//			return connection
//				.prepareTupleQuery(wild_card_chain_with_common_ends)
//				.evaluate()
//				.stream()
//				.count();
//		}
//	}

	private static InputStream getResourceAsStream(String filename) {
		return QueryBenchmark.class.getClassLoader().getResourceAsStream(filename);
	}
}
