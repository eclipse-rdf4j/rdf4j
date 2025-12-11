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
import java.util.concurrent.ConcurrentHashMap;
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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 100, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G" })
//@Fork(value = 1, jvmArgs = {"-Xms1G", "-Xmx1G", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=60s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class QueryBenchmark {

	private static final ConcurrentHashMap<String, String> explanations = new ConcurrentHashMap<>();

	private SailRepository repository;

	private static final String query1;
	private static final String query4;
	private static final String query10;
	private static final String query7_pathexpression1;
	private static final String query8_pathexpression2;

	private static final String common_themes;
	private static final String different_datasets_with_similar_distributions;
	private static final String long_chain;
	private static final String optional_lhs_filter;
	private static final String optional_rhs_filter;
	private static final String lots_of_optional;
	private static final String minus;
	private static final String nested_optionals;
	private static final String particularly_large_join_surface;
	private static final String query_distinct_predicates;
	private static final String simple_filter_not;
	private static final String wild_card_chain_with_common_ends;
	private static final String sub_select;
	private static final String multiple_sub_select;
	private static final String contact_point_path_chase;
	private static final String distribution_media_contrast;
	private static final String top_titles_by_length;
	private static final String language_union_regex;
	private static final String publisher_distribution_aggregation;
	private static final String join_reorder_stress;
	private static final String optional_filter_pushdown;
	private static final String star_path_fanout;
	private static final String union_publisher_dedup;
	private static final String language_group_having;
	private static final String overlapping_optionals_wide;
	private static final String overlapping_optionals_filtered;
	private static final String values_dup_union;

	static {
		try {
			common_themes = IOUtils.toString(getResourceAsStream("benchmarkFiles/common-themes.qr"),
					StandardCharsets.UTF_8);
			different_datasets_with_similar_distributions = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/different-datasets-with-similar-distributions.qr"),
					StandardCharsets.UTF_8);
			long_chain = IOUtils.toString(getResourceAsStream("benchmarkFiles/long-chain.qr"), StandardCharsets.UTF_8);
			optional_lhs_filter = IOUtils.toString(getResourceAsStream("benchmarkFiles/optional-lhs-filter.qr"),
					StandardCharsets.UTF_8);
			optional_rhs_filter = IOUtils.toString(getResourceAsStream("benchmarkFiles/optional-rhs-filter.qr"),
					StandardCharsets.UTF_8);
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
			contact_point_path_chase = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/contact-point-path-chase.qr"), StandardCharsets.UTF_8);
			distribution_media_contrast = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/distribution-media-contrast.qr"), StandardCharsets.UTF_8);
			top_titles_by_length = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/top-titles-by-length.qr"), StandardCharsets.UTF_8);
			language_union_regex = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/language-union-regex.qr"), StandardCharsets.UTF_8);
			publisher_distribution_aggregation = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/publisher-distribution-aggregation.qr"),
					StandardCharsets.UTF_8);
			join_reorder_stress = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/join-reorder-stress.qr"), StandardCharsets.UTF_8);
			optional_filter_pushdown = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/optional-filter-pushdown.qr"), StandardCharsets.UTF_8);
			star_path_fanout = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/star-path-fanout.qr"), StandardCharsets.UTF_8);
			union_publisher_dedup = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/union-publisher-dedup.qr"), StandardCharsets.UTF_8);
			language_group_having = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/language-group-having.qr"), StandardCharsets.UTF_8);
			overlapping_optionals_wide = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/overlapping-optionals-wide.qr"), StandardCharsets.UTF_8);
			overlapping_optionals_filtered = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/overlapping-optionals-filtered.qr"), StandardCharsets.UTF_8);
			values_dup_union = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/values-dup-union.qr"), StandardCharsets.UTF_8);
			query10 = IOUtils.toString(
					getResourceAsStream("benchmarkFiles/query10.qr"), StandardCharsets.UTF_8);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException, RunnerException {

		// REMEMBER TO RUN WITH HIGH ENOUGH MEMORY SETTINGS, E.G., -Xms8G -Xmx8G

		Options opt = new OptionsBuilder()
				.include(QueryBenchmark.class.getName() + "\\.") // adapt to run other benchmark tests
				// .addProfiler("stack", "lines=20;period=1;top=20")
				.forks(0)
				.warmupIterations(1)
				.warmupBatchSize(1)
				.warmupTime(TimeValue.milliseconds(1))
				.measurementIterations(1)
				.measurementBatchSize(1)
				.measurementTime(TimeValue.milliseconds(1))
				.build();

		new Runner(opt).run();

		System.out.println();

		explanations.keySet().stream().sorted().forEach(k -> {
			String explanation = explanations.get(k);
			System.out.println("=== " + k + " ===");
			System.out.println(explanation);
			System.out.println("\n\n");
		});

//		long k = 0;
//
//		QueryBenchmark queryBenchmark = new QueryBenchmark();
//		queryBenchmark.beforeClass();
//
//		long l = queryBenchmark.complexQuery();
//		System.out.println("complexQuery: " + l);
//		queryBenchmark.afterClass();
//		System.out.println(k);

	}

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

		Thread.sleep(5000);
	}

	@TearDown(Level.Trial)
	public void afterClass() {
		repository.shutDown();
//		System.out.println();
//
//		explanations.keySet().stream().sorted().forEach(k ->{
//			String explanation = explanations.get(k);
//			System.out.println("=== " + k + " ===");
//			System.out.println(explanation);
//			System.out.println("\n\n");
//		});
	}

	@Benchmark
	public long groupByQuery() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, query1);

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
//			TupleQuery tupleQuery = connection
//					.prepareTupleQuery(query4);
//			System.out.println(tupleQuery.explain(Explanation.Level.Executed));

//			saveQueryExplanation(connection, query4);

			return count(connection
					.prepareTupleQuery(query4)
					.evaluate()
			);
		}
	}

	private static void saveQueryExplanation(SailRepositoryConnection connection, String query) {
		Explanation explainOptimized = connection.prepareTupleQuery(query).explain(Explanation.Level.Optimized);
		Explanation explainExecuted = connection.prepareTupleQuery(query).explain(Explanation.Level.Executed);

		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		String methodName = stackTrace[2].getMethodName();
		explanations.put(methodName + " - OPTIMIZED", explainOptimized.toString());
		explanations.put(methodName + " - EXECUTED", explainExecuted.toString());
	}

	@Benchmark
	public long query10() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			TupleQuery tupleQuery = connection
//					.prepareTupleQuery(query4);
//			System.out.println(tupleQuery.explain(Explanation.Level.Executed));

//			saveQueryExplanation(connection, query10);

			return count(connection
					.prepareTupleQuery(query10)
					.evaluate()
			);
		}
	}

	@Benchmark
	public long pathExpressionQuery1() {

		try (SailRepositoryConnection connection = repository.getConnection()) {

//			saveQueryExplanation(connection, query7_pathexpression1);

			return count(connection
					.prepareTupleQuery(query7_pathexpression1)
					.evaluate());

		}
	}

	@Benchmark
	public long pathExpressionQuery2() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, query8_pathexpression2);

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
//			saveQueryExplanation(connection, different_datasets_with_similar_distributions);

			return count(connection
					.prepareTupleQuery(different_datasets_with_similar_distributions)
					.evaluate());
		}
	}

	@Benchmark
	public long long_chain() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, long_chain);

			return count(connection
					.prepareTupleQuery(long_chain)
					.evaluate());
		}
	}

	@Benchmark
	public long optional_lhs_filter() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, optional_lhs_filter);

			return count(connection
					.prepareTupleQuery(optional_lhs_filter)
					.evaluate());
		}
	}

	@Benchmark
	public long optional_rhs_filter() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, optional_rhs_filter);

			return count(connection
					.prepareTupleQuery(optional_rhs_filter)
					.evaluate());
		}
	}

	@Benchmark
	public long lots_of_optional() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, lots_of_optional);

			return count(connection
					.prepareTupleQuery(lots_of_optional)
					.evaluate());
		}
	}

	@Benchmark
	public long minus() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, minus);

			return count(connection
					.prepareTupleQuery(minus)
					.evaluate());
		}
	}

	@Benchmark
	public long nested_optionals() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, nested_optionals);

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
//			saveQueryExplanation(connection, query_distinct_predicates);
			return count(connection
					.prepareTupleQuery(query_distinct_predicates)
					.evaluate());
		}
	}

	@Benchmark
	public long simple_filter_not() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, simple_filter_not);

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
//			saveQueryExplanation(connection, sub_select);

			return count(connection
					.prepareTupleQuery(sub_select)
					.evaluate());
		}
	}

	@Benchmark
	public long multipleSubSelect() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, multiple_sub_select);

			return count(connection
					.prepareTupleQuery(multiple_sub_select)
					.evaluate());
		}
	}

	@Benchmark
	public long distributionMediaContrast() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, distribution_media_contrast);
			return count(connection
					.prepareTupleQuery(distribution_media_contrast)
					.evaluate());
		}
	}

	@Benchmark
	public long contactPointPathChase() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, contact_point_path_chase);
			return count(connection
					.prepareTupleQuery(contact_point_path_chase)
					.evaluate());
		}
	}

	@Benchmark
	public long topTitlesByLength() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, top_titles_by_length);
			return count(connection
					.prepareTupleQuery(top_titles_by_length)
					.evaluate());
		}
	}

	@Benchmark
	public long languageUnionRegex() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, language_union_regex);
			return count(connection
					.prepareTupleQuery(language_union_regex)
					.evaluate());
		}
	}

	@Benchmark
	public long publisherDistributionAggregation() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, publisher_distribution_aggregation);
			return count(connection
					.prepareTupleQuery(publisher_distribution_aggregation)
					.evaluate());
		}
	}

	@Benchmark
	public long joinReorderStress() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, join_reorder_stress);
			return count(connection
					.prepareTupleQuery(join_reorder_stress)
					.evaluate());
		}
	}

	@Benchmark
	public long optionalFilterPushdown() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, optional_filter_pushdown);

			return count(connection
					.prepareTupleQuery(optional_filter_pushdown)
					.evaluate());
		}
	}

	@Benchmark
	public long starPathFanout() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, star_path_fanout);

			return count(connection
					.prepareTupleQuery(star_path_fanout)
					.evaluate());
		}
	}

	@Benchmark
	public long unionPublisherDedup() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, union_publisher_dedup);

			return count(connection
					.prepareTupleQuery(union_publisher_dedup)
					.evaluate());
		}
	}

	@Benchmark
	public long languageGroupHaving() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, language_group_having);

			return count(connection
					.prepareTupleQuery(language_group_having)
					.evaluate());
		}
	}

	@Benchmark
	public long overlappingOptionalsWide() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, overlapping_optionals_wide);

			return count(connection
					.prepareTupleQuery(overlapping_optionals_wide)
					.evaluate());
		}
	}

	@Benchmark
	public long overlappingOptionalsFiltered() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, overlapping_optionals_filtered);

			return count(connection
					.prepareTupleQuery(overlapping_optionals_filtered)
					.evaluate());
		}
	}

	@Benchmark
	public long valuesDupUnion() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
//			saveQueryExplanation(connection, values_dup_union);

			return count(connection
					.prepareTupleQuery(values_dup_union)
					.evaluate());
		}
	}

	private static InputStream getResourceAsStream(String filename) {
		return QueryBenchmark.class.getClassLoader().getResourceAsStream(filename);
	}

}
