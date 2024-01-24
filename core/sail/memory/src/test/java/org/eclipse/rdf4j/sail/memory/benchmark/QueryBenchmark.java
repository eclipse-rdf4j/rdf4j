/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.explanation.Explanation;
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

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms1G", "-Xmx1G" })
//@Fork(value = 1, jvmArgs = {"-Xms1G", "-Xmx1G", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=60s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class QueryBenchmark {

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
	private static final String sub_select;
	private static final String multiple_sub_select;

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
			sub_select = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/sub-select.qr"), StandardCharsets.UTF_8);
			multiple_sub_select = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/multiple-sub-select.qr"), StandardCharsets.UTF_8);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws IOException {
//		Options opt = new OptionsBuilder()
//				.include("QueryBenchmark") // adapt to run other benchmark tests
//				// .addProfiler("stack", "lines=20;period=1;top=20")
//				.forks(1)
//				.build();
//
//		new Runner(opt).run();

		long k = 0;

		QueryBenchmark queryBenchmark = new QueryBenchmark();
		queryBenchmark.beforeClass();
		for (int i = 0; i < 100; i++) {
			System.out.println(i);
			long result;
			try (SailRepositoryConnection connection = queryBenchmark.repository.getConnection()) {
				result = count(connection
						.prepareTupleQuery(query1)
						.evaluate());

			}
			k += result;
			long result1;
			try (SailRepositoryConnection connection = queryBenchmark.repository.getConnection()) {
				result1 = count(connection
						.prepareTupleQuery(query4)
						.evaluate());

			}
			k += result1;
			long result2;

			try (SailRepositoryConnection connection = queryBenchmark.repository.getConnection()) {
				result2 = count(connection
						.prepareTupleQuery(query7_pathexpression1)
						.evaluate());

			}
			k += result2;
			long result3;
			try (SailRepositoryConnection connection = queryBenchmark.repository.getConnection()) {
				result3 = count(connection
						.prepareTupleQuery(query8_pathexpression2)
						.evaluate());

			}
			k += result3;
			long result4;
			try (SailRepositoryConnection connection = queryBenchmark.repository.getConnection()) {
				result4 = count(connection
						.prepareTupleQuery(different_datasets_with_similar_distributions)
						.evaluate());

			}
			k += result4;
			long result5;
			try (SailRepositoryConnection connection = queryBenchmark.repository.getConnection()) {
				result5 = count(connection
						.prepareTupleQuery(long_chain)
						.evaluate());

			}
			k += result5;
			long result6;
			try (SailRepositoryConnection connection = queryBenchmark.repository.getConnection()) {
				result6 = count(connection
						.prepareTupleQuery(lots_of_optional)
						.evaluate());

			}
			k += result6;
//            k += queryBenchmark.minus();
			long result7;
			try (SailRepositoryConnection connection = queryBenchmark.repository.getConnection()) {
				result7 = count(connection
						.prepareTupleQuery(nested_optionals)
						.evaluate());

			}
			k += result7;
			long result8;
			try (SailRepositoryConnection connection = queryBenchmark.repository.getConnection()) {
				result8 = count(connection
						.prepareTupleQuery(query_distinct_predicates)
						.evaluate());

			}
			k += result8;
			long result9;
			try (SailRepositoryConnection connection = queryBenchmark.repository.getConnection()) {
				result9 = count(connection
						.prepareTupleQuery(simple_filter_not)
						.evaluate());

			}
			k += result9;
		}
		queryBenchmark.afterClass();
		System.out.println(k);

	}

	@Setup(Level.Trial)
	public void beforeClass() throws IOException {
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
	public long groupByQuery() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return count(connection
					.prepareTupleQuery(query1)
					.evaluate());
		}
	}

	private static long count(TupleQueryResult evaluate) {
		try (Stream<BindingSet> stream = evaluate.stream()) {
			return stream.count();
		}
	}

	@Benchmark
	public long complexQuery() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return count(connection
					.prepareTupleQuery(query4)
					.evaluate()
			);
		}
	}

	@Benchmark
	public long pathExpressionQuery1() {

		try (SailRepositoryConnection connection = repository.getConnection()) {
			return count(connection
					.prepareTupleQuery(query7_pathexpression1)
					.evaluate());

		}
	}

	@Benchmark
	public long pathExpressionQuery2() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return count(connection
					.prepareTupleQuery(query8_pathexpression2)
					.evaluate());
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
			return count(connection
					.prepareTupleQuery(different_datasets_with_similar_distributions)
					.evaluate());
		}
	}

	@Benchmark
	public long long_chain() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return count(connection
					.prepareTupleQuery(long_chain)
					.evaluate());
		}
	}

	@Benchmark
	public long lots_of_optional() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return count(connection
					.prepareTupleQuery(lots_of_optional)
					.evaluate());
		}
	}

	@Benchmark
	public long minus() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return count(connection
					.prepareTupleQuery(minus)
					.evaluate());
		}
	}

	@Benchmark
	public long nested_optionals() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return count(connection
					.prepareTupleQuery(nested_optionals)
					.evaluate());
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
			return count(connection
					.prepareTupleQuery(query_distinct_predicates)
					.evaluate());
		}
	}

	@Benchmark
	public long simple_filter_not() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return count(connection
					.prepareTupleQuery(simple_filter_not)
					.evaluate());
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

	@Benchmark
	public long subSelect() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return count(connection
					.prepareTupleQuery(sub_select)
					.evaluate());
		}
	}

	@Benchmark
	public long multipleSubSelect() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			return count(connection
					.prepareTupleQuery(multiple_sub_select)
					.evaluate());
		}
	}

	private static InputStream getResourceAsStream(String filename) {
		return QueryBenchmark.class.getClassLoader().getResourceAsStream(filename);
	}

}
