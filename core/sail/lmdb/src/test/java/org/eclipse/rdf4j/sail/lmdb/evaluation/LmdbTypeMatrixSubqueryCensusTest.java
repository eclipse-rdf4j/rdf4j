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
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.lmdb.AdjacencyEngagementTestAccess;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.DirectAdjacencyMode;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

/**
 * The class-linkage census query joins an instance-count subquery with the linkage patterns and groups by the
 * subquery's count as well. The native engine pushes the GROUP BY below that join so that the inner group is the plain
 * type matrix; the result must stay identical to the unrewritten evaluation.
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
public class LmdbTypeMatrixSubqueryCensusTest {

	private static final String EX = "http://example.com/";
	private static final String PREFIX = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n";

	private static final String CENSUS = PREFIX
			+ "SELECT ?sourceClass ?instanceCount ?targetClass (COUNT(*) AS ?edgeCount) WHERE {\n"
			+ "  { SELECT ?sourceClass (COUNT(DISTINCT ?source) AS ?instanceCount) WHERE { ?source rdf:type ?sourceClass . } GROUP BY ?sourceClass }\n"
			+ "  ?source rdf:type ?sourceClass .\n"
			+ "  ?source ?predicate ?target .\n"
			+ "  FILTER(?predicate != rdf:type)\n"
			+ "  ?target rdf:type ?targetClass .\n"
			+ "} GROUP BY ?sourceClass ?instanceCount ?targetClass";

	private static final String CENSUS_ORDERED = CENSUS + " ORDER BY ?sourceClass DESC(?edgeCount)";

	/** The instance-count subquery has two grouping keys, so the rewrite must not apply. */
	private static final String TWO_KEY_SUBQUERY = PREFIX
			+ "SELECT ?sourceClass ?kind ?perKind ?targetClass (COUNT(*) AS ?edgeCount) WHERE {\n"
			+ "  { SELECT ?sourceClass ?kind (COUNT(?source) AS ?perKind) WHERE { ?source rdf:type ?sourceClass . ?source ?kind ?x . } GROUP BY ?sourceClass ?kind }\n"
			+ "  ?source rdf:type ?sourceClass .\n"
			+ "  ?source ?predicate ?target .\n"
			+ "  FILTER(?predicate != rdf:type)\n"
			+ "  ?target rdf:type ?targetClass .\n"
			+ "} GROUP BY ?sourceClass ?kind ?perKind ?targetClass";

	private static final Pattern TYPE_MATRIX_PATH = Pattern.compile(
			"nativeExecutionPath=(typeMatrix|irAggregateTypeMatrix(Interpreted|Parallel|ParallelInterpreted)?)\\b");

	@TempDir
	File nativeDir;

	@TempDir
	File referenceDir;

	private SailRepository nativeRepository;
	private SailRepository referenceRepository;
	private final Map<String, String> previousProperties = new HashMap<>();

	@BeforeEach
	public void pinProperties() {
		for (String property : List.of("rdf4j.lmdb.janinoCodegen.enabled", "rdf4j.lmdb.kernelInterpreter.enabled",
				"rdf4j.lmdb.directAdjacency.nodePredicateProjection.enabled",
				AdjacencyEngagementTestAccess.NODE_PREDICATE_SERVE_PROPERTY)) {
			previousProperties.put(property, System.getProperty(property));
			System.setProperty(property, "false");
		}
		nativeRepository = open(nativeDir, true);
		referenceRepository = open(referenceDir, false);
	}

	@AfterEach
	public void tearDown() {
		try {
			if (nativeRepository != null) {
				nativeRepository.shutDown();
			}
			if (referenceRepository != null) {
				referenceRepository.shutDown();
			}
		} finally {
			previousProperties.forEach((property, value) -> {
				if (value == null) {
					System.clearProperty(property);
				} else {
					System.setProperty(property, value);
				}
			});
		}
	}

	@Test
	public void censusQueryEqualsTheUnrewrittenEvaluation() {
		List<String> expected = rows(referenceRepository, CENSUS, "sourceClass", "instanceCount", "targetClass",
				"edgeCount");
		assertThat(expected).as("fixture must produce linkage rows").hasSizeGreaterThan(5);
		assertThat(rows(nativeRepository, CENSUS, "sourceClass", "instanceCount", "targetClass", "edgeCount"))
				.isEqualTo(expected);
	}

	@Test
	public void censusQueryRunsTheTypeMatrixForTheInnerGroup() {
		long before = LmdbNativeTypeMatrix.RUNS.get();
		try (SailRepositoryConnection connection = nativeRepository.getConnection()) {
			String telemetry = connection.prepareTupleQuery(CENSUS).explain(Explanation.Level.Telemetry).toString();
			assertThat(telemetry).containsPattern(TYPE_MATRIX_PATH);
		}
		assertThat(LmdbNativeTypeMatrix.RUNS.get()).isGreaterThanOrEqualTo(before);
	}

	@Test
	public void orderedCensusQueryEqualsTheUnrewrittenEvaluationAndKeepsItsOrder() {
		List<BindingSet> nativeRows;
		try (SailRepositoryConnection connection = nativeRepository.getConnection()) {
			nativeRows = QueryResults.asList(connection.prepareTupleQuery(CENSUS_ORDERED).evaluate());
		}
		for (int i = 1; i < nativeRows.size(); i++) {
			String previousClass = nativeRows.get(i - 1).getValue("sourceClass").stringValue();
			String currentClass = nativeRows.get(i).getValue("sourceClass").stringValue();
			int byClass = previousClass.compareTo(currentClass);
			assertThat(byClass).as("sourceClass ascending at row " + i).isLessThanOrEqualTo(0);
			if (byClass == 0) {
				long previousCount = ((Literal) nativeRows.get(i - 1).getValue("edgeCount")).longValue();
				long currentCount = ((Literal) nativeRows.get(i).getValue("edgeCount")).longValue();
				assertThat(previousCount).as("edgeCount descending at row " + i).isGreaterThanOrEqualTo(currentCount);
			}
		}
		assertThat(rows(nativeRepository, CENSUS_ORDERED, "sourceClass", "instanceCount", "targetClass", "edgeCount"))
				.isEqualTo(rows(referenceRepository, CENSUS_ORDERED, "sourceClass", "instanceCount", "targetClass",
						"edgeCount"));
		try (SailRepositoryConnection connection = nativeRepository.getConnection()) {
			assertThat(connection.prepareTupleQuery(CENSUS_ORDERED).explain(Explanation.Level.Telemetry).toString())
					.containsPattern(TYPE_MATRIX_PATH);
		}
	}

	@Test
	public void subqueryGroupedByTwoKeysStillReturnsTheUnrewrittenResult() {
		List<String> expected = rows(referenceRepository, TWO_KEY_SUBQUERY, "sourceClass", "kind", "perKind",
				"targetClass", "edgeCount");
		assertThat(expected).isNotEmpty();
		assertThat(rows(nativeRepository, TWO_KEY_SUBQUERY, "sourceClass", "kind", "perKind", "targetClass",
				"edgeCount")).isEqualTo(expected);
	}

	private static List<String> rows(SailRepository repository, String query, String... variables) {
		List<String> rows = new ArrayList<>();
		try (SailRepositoryConnection connection = repository.getConnection()) {
			for (BindingSet bindings : QueryResults.asList(connection.prepareTupleQuery(query).evaluate())) {
				StringBuilder row = new StringBuilder();
				for (String variable : variables) {
					row.append(bindings.getValue(variable)).append('|');
				}
				rows.add(row.toString());
			}
		}
		Collections.sort(rows);
		return rows;
	}

	private static SailRepository open(File dir, boolean nativeEngine) {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,posc,ospc,psoc").setNativeEvaluationEnabled(nativeEngine);
		if (nativeEngine) {
			config.setDirectAdjacencyEnabled(true)
					.setDirectAdjacencyMode(DirectAdjacencyMode.PREFER)
					.setDirectAdjacencyMaxBytes(1L << 30);
		}
		LmdbStore store = new LmdbStore(dir, config);
		SailRepository repository = new SailRepository(store);
		load(repository);
		if (nativeEngine) {
			assertThat(AdjacencyEngagementTestAccess.buildNow(store))
					.as(store.getDirectAdjacencyReadinessDescription())
					.isTrue();
		}
		return repository;
	}

	/** Typed, multi-typed and untyped nodes, literal objects and random edges; identical for both stores. */
	private static void load(SailRepository repository) {
		Set<String> typed = new HashSet<>();
		try (SailRepositoryConnection connection = repository.getConnection()) {
			ValueFactory vf = connection.getValueFactory();
			connection.begin();
			Random random = new Random(23);
			for (int i = 0; i < 120; i++) {
				String node = "n" + i;
				if (random.nextInt(5) != 0) {
					int types = 1 + random.nextInt(2);
					for (int t = 0; t < types; t++) {
						String type = "T" + random.nextInt(6);
						if (typed.add(node + "|" + type)) {
							connection.add(vf.createIRI(EX, node), RDF.TYPE, vf.createIRI(EX, type));
						}
					}
				}
			}
			for (int i = 0; i < 600; i++) {
				IRI source = vf.createIRI(EX, "n" + random.nextInt(120));
				IRI predicate = vf.createIRI(EX, "p" + random.nextInt(4));
				if (random.nextInt(8) == 0) {
					connection.add(source, predicate, vf.createLiteral("v" + random.nextInt(10)));
				} else {
					connection.add(source, predicate, vf.createIRI(EX, "n" + random.nextInt(120)));
				}
			}
			connection.commit();
		}
	}
}
