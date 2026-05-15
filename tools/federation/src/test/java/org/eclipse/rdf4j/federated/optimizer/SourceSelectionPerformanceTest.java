/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.optimizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.SPARQLBaseTest;
import org.eclipse.rdf4j.federated.algebra.HolderNode;
import org.eclipse.rdf4j.federated.algebra.StatementSource;
import org.eclipse.rdf4j.federated.cache.SourceSelectionCache;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.evaluation.FederationEvaluationStrategy;
import org.eclipse.rdf4j.federated.monitoring.MonitoringService;
import org.eclipse.rdf4j.federated.repository.ConfigurableSailRepository;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.federated.structures.QueryType;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * A performance test for source selection
 *
 * <p>
 * Can be executed together with a provided script for better analysis:
 * </p>
 *
 * <p>
 * Notes:
 *
 * requires to remove the "Disabled" annotation
 * </p>
 *
 * <pre>
 * mvn -pl tools/federation test -Dtest=SourceSelectionPerformanceTest 2>&1 | python3 tools/federation/src/test/scripts/summarize_benchmark.py
 * </pre>
 *
 * <p>
 * Example results
 * </p>
 *
 * <pre>
 * === Source-selection benchmark results (5 measured run(s) + 1 warm-up) ===
+---------+--------------+----------+---------------+-------------------------+
| Members | Latency (ms) | Patterns | Avg time (ms) | Total endpoint requests |
+---------+--------------+----------+---------------+-------------------------+
|       5 |            0 |        2 |            13 |                      60 |
|       5 |            0 |        5 |            16 |                      30 |
|       5 |            0 |       10 |            19 |                      30 |
|       5 |            0 |       20 |            19 |                      30 |
|       5 |           20 |        2 |            28 |                      60 |
|       5 |           20 |        5 |            29 |                      30 |
|       5 |           20 |       10 |            31 |                      30 |
|       5 |           20 |       20 |            32 |                      30 |
|      10 |            0 |        2 |            28 |                     120 |
|      10 |            0 |        5 |            28 |                      60 |
|      10 |            0 |       10 |            31 |                      60 |
|      10 |            0 |       20 |            30 |                      60 |
|      10 |           20 |        2 |            28 |                     120 |
|      10 |           20 |        5 |            30 |                      60 |
|      10 |           20 |       10 |            30 |                      60 |
|      10 |           20 |       20 |            35 |                      60 |
|      20 |            0 |        2 |            30 |                     240 |
|      20 |            0 |        5 |            30 |                     120 |
|      20 |            0 |       10 |            30 |                     120 |
|      20 |            0 |       20 |            32 |                     120 |
|      20 |           20 |        2 |            53 |                     240 |
|      20 |           20 |        5 |            30 |                     120 |
|      20 |           20 |       10 |            31 |                     120 |
|      20 |           20 |       20 |            35 |                     120 |
+---------+--------------+----------+---------------+-------------------------+
 * </pre>
 */
@Execution(ExecutionMode.SAME_THREAD)
@Disabled("manual performance test for local execution")
public class SourceSelectionPerformanceTest extends SPARQLBaseTest {

	/** Set to {@code true} to print benchmark results to stdout. */
	private static final boolean PRINT_RESULTS = true;

	/** Number of measured runs for computing the average execution time. */
	private static final int N_BENCHMARK_RUNS = 5;

	private static final String EX_NS = "http://example.org/";

	@Override
	protected void initFedXConfig() {
		fedxRule.withConfiguration(c -> c.withEnableMonitoring(true));

		// optionally force ASK queries
		// fedxRule.withConfiguration(c -> c.withEnableGroupedSourceSelection(false));
	}

	@Override
	protected int getMaxEndpoints() {
		return 20;
	}

	/**
	 * Provides {@code (nMembers, latencyMs, nPatterns)} argument triples for
	 * {@link #benchmarkSourceSelection(int, int, int)}.
	 *
	 * <p>
	 * Each row exercises a different combination of federation size, simulated endpoint latency, and number of
	 * statement patterns. The {@code nMembers} value must not exceed {@link #N_MEMBERS}, which determines the number of
	 * repository slots created on the embedded server at start-up.
	 * </p>
	 */
	static Stream<Arguments> sourceSelectionParameters() {
		return Stream.of(
				Arguments.of(5, 0, 2),
				Arguments.of(5, 0, 5),
				Arguments.of(5, 0, 10),
				Arguments.of(5, 0, 20),
				Arguments.of(5, 20, 2),
				Arguments.of(5, 20, 5),
				Arguments.of(5, 20, 10),
				Arguments.of(5, 20, 20),
				Arguments.of(10, 0, 2),
				Arguments.of(10, 0, 5),
				Arguments.of(10, 0, 10),
				Arguments.of(10, 0, 20),
				Arguments.of(10, 20, 2),
				Arguments.of(10, 20, 5),
				Arguments.of(10, 20, 10),
				Arguments.of(10, 20, 20),
				Arguments.of(20, 0, 2),
				Arguments.of(20, 0, 5),
				Arguments.of(20, 0, 10),
				Arguments.of(20, 0, 20),
				Arguments.of(20, 20, 2),
				Arguments.of(20, 20, 5),
				Arguments.of(20, 20, 10),
				Arguments.of(20, 20, 20)
		);
	}

	/**
	 * Benchmark for {@link SourceSelection#doSourceSelection(List)} over a federation of {@code nMembers} members,
	 * {@code nPatterns} statement patterns, and a simulated per-request latency of {@code latencyMs} ms.
	 *
	 * <p>
	 * Correctness is validated on the warm-up run: each pattern {@code pred_i} must be matched by exactly
	 * {@code max(0, nMembers - i)} sources.
	 * </p>
	 *
	 * @param nMembers  number of federation members to use (must be &le; {@value #N_MEMBERS})
	 * @param latencyMs simulated round-trip latency per endpoint request in milliseconds
	 * @param nPatterns number of statement patterns to include in each source selection call
	 */
	@ParameterizedTest(name = "members={0}, latency={1}ms, patterns={2}")
	@MethodSource("sourceSelectionParameters")
	public void benchmarkSourceSelection(int nMembers, int latencyMs, int nPatterns) throws Exception {

		// only execute for sparql endpoints
		assumeSparqlEndpoint();

		prepareTest(Collections.nCopies(nMembers, "/tests/basic/data_emptyStore.ttl"));

		for (int memberId = 1; memberId <= nMembers; memberId++) {
			ConfigurableSailRepository memberRepo = getRepository(memberId);
			populateMemberRepository(memberRepo, nMembers, memberId);
			if (latencyMs > 0) {
				memberRepo.setLatencySimulator(() -> {
					try {
						Thread.sleep(latencyMs);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new RuntimeException(e);
					}
				});
			} else {
				memberRepo.setLatencySimulator(null);
			}
		}

		FederationContext ctx = federationContext();
		List<Endpoint> members = new ArrayList<>(ctx.getEndpointManager().getAvailableEndpoints());
		SourceSelectionCache cache = ctx.getSourceSelectionCache();

		// Invalidate any cache state left over from a previous parameterized invocation
		cache.invalidate();

		// Warm-up run (not measured): populates cache and validates correctness
		List<StatementPattern> warmupStmts = createStatementPatterns(nPatterns);
		SourceSelection warmupSS = new SourceSelection(members, cache, federationContext(), createQueryInfo(ctx));
		warmupSS.doSourceSelection(warmupStmts);
		validateCorrectness(warmupStmts, warmupSS, nMembers, nPatterns);

		// Measured runs
		long totalMs = 0;
		for (int run = 0; run < N_BENCHMARK_RUNS; run++) {

			cache.invalidate();

			List<StatementPattern> stmts = createStatementPatterns(nPatterns);
			SourceSelection ss = new SourceSelection(members, cache, federationContext(), createQueryInfo(ctx));

			long start = System.currentTimeMillis();
			ss.doSourceSelection(stmts);
			long runMs = System.currentTimeMillis() - start;
			totalMs += runMs;

			if (PRINT_RESULTS) {
				System.out.println(
						"Source selection benchmark [members=" + nMembers + ", latency=" + latencyMs + "ms]"
								+ " - run " + (run + 1) + "/" + N_BENCHMARK_RUNS + ": " + runMs + " ms");
			}
		}

		long avgMs = totalMs / N_BENCHMARK_RUNS;
		if (PRINT_RESULTS) {
			System.out.println("Source selection benchmark (" + nMembers + " members, " + nPatterns + " patterns, "
					+ latencyMs + "ms latency): average execution time over " + N_BENCHMARK_RUNS + " runs = "
					+ avgMs + " ms");
		}

		MonitoringService monitoring = (MonitoringService) ctx.getMonitoringService();
		long totalRequests = monitoring.getAllMonitoringInformation()
				.stream()
				.mapToLong(m -> m.getNumberOfRequests())
				.sum();
		if (PRINT_RESULTS) {
			System.out.println("Source selection benchmark (" + nMembers + " members, " + nPatterns + " patterns, "
					+ latencyMs + "ms latency): total endpoint requests = " + totalRequests);
		}
	}

	/**
	 * Validates source counts: pattern {@code pred_i} (1-indexed) must have exactly {@code max(0, nMembers - i)}
	 * sources. Patterns with zero expected sources may be absent from {@code stmtToSources} (null entry), which is
	 * treated as an empty source list.
	 */
	private void validateCorrectness(List<StatementPattern> stmts, SourceSelection ss, int nMembers, int nPatterns) {
		for (int i = 0; i < nPatterns; i++) {
			int expectedSources = Math.max(0, nMembers - (i + 1));
			List<StatementSource> sources = ss.stmtToSources.get(stmts.get(i));
			int actualSources = sources == null ? 0 : sources.size();
			Assertions.assertEquals(expectedSources, actualSources,
					"Pattern pred_" + (i + 1) + " should have " + expectedSources + " sources");
		}
	}

	/**
	 * Creates {@code nPatterns} statement patterns, one per predicate {@code ex:pred_1..ex:pred_nPatterns}, with
	 * unbound subject and object. Each pattern is wrapped in a {@link HolderNode} to satisfy the parent requirement of
	 * {@link org.eclipse.rdf4j.query.algebra.QueryModelNode#replaceWith}.
	 *
	 * @param nPatterns number of statement patterns to create
	 */
	private List<StatementPattern> createStatementPatterns(int nPatterns) {
		List<StatementPattern> stmts = new ArrayList<>();
		for (int i = 1; i <= nPatterns; i++) {
			Var predicate = Var.of("pred_" + i, Values.iri(EX_NS, "pred_" + i));
			StatementPattern sp = new StatementPattern(Var.of("s"), predicate, Var.of("o"));
			new HolderNode(sp); // artificial parent required by replaceWith()
			stmts.add(sp);
		}
		return stmts;
	}

	/**
	 * Creates a fresh {@link QueryInfo} for the current federation context.
	 */
	private QueryInfo createQueryInfo(FederationContext ctx) {
		FederationEvaluationStrategy strategy = ctx.createStrategy(null);
		return new QueryInfo("SELECT * WHERE { ?s ?p ?o }", null, QueryType.SELECT, 0, false, ctx, strategy, null);
	}

	/**
	 * Builds an in-memory repository for federation member {@code memberId}.
	 *
	 * <p>
	 * The repository holds one triple {@code (ex:subject_j, ex:pred_i, ex:object_j)} for each predicate index
	 * {@code i = 1..(N_MEMBERS - memberId)}.
	 * </p>
	 */
	private void populateMemberRepository(Repository memberRepo, int nMembers, int memberId) {
		int nPredicates = nMembers - memberId;
		try (RepositoryConnection conn = memberRepo.getConnection()) {
			conn.clear();
			for (int i = 1; i <= nPredicates; i++) {
				conn.add(
						Values.iri(EX_NS, "subject_" + memberId),
						Values.iri(EX_NS, "pred_" + i),
						Values.iri(EX_NS, "object_" + memberId));
			}
		}
	}
}
