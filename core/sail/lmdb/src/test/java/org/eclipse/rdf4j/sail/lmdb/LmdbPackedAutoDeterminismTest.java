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
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * AUTO-mode determinism acceptance, ported from the superseded hardening plan: the selected winner and the search
 * counters must be identical run over run, and a one-millisecond configured timeout must have no effect on AUTO because
 * AUTO plans against a fixed work budget with no wall-clock deadline.
 */
class LmdbPackedAutoDeterminismTest {

	private static final SimpleValueFactory VF = SimpleValueFactory.getInstance();
	private static final Pattern COUNTER = Pattern.compile(
			"optimizer\\.cascades(WorkUnits|Groups|LogicalExpressions|PhysicalExpressions|Winners)=([-0-9.E]+)");

	private static final String QUERY = """
			SELECT ?person ?friend ?name WHERE {
			  ?person <http://example.com/knows> ?friend .
			  ?friend <http://example.com/knows> ?other .
			  ?other <http://example.com/name> ?name .
			  ?person <http://example.com/name> ?personName .
			  FILTER(?name != ?personName)
			}
			""";

	@Test
	void autoModeWinnersAndCountersIgnoreConfiguredTimeout(@TempDir File firstDir, @TempDir File secondDir,
			@TempDir File thirdDir) {
		String originalTimeout = System.getProperty(LmdbCascadesOptimizer.TIMEOUT_MILLIS_PROPERTY);
		try {
			// Each run plans cold against a freshly built store with identical contents, so the comparison
			// exercises the search itself rather than the store-owned plan cache.
			System.clearProperty(LmdbCascadesOptimizer.TIMEOUT_MILLIS_PROPERTY);
			Snapshot defaultRun = coldExplain(firstDir);
			Snapshot repeatRun = coldExplain(secondDir);
			assertEquals(defaultRun.structure(), repeatRun.structure(),
					"AUTO planning must select a byte-identical winner run over run");
			assertEquals(defaultRun.counters(), repeatRun.counters(),
					"AUTO planning must report identical search counters run over run");

			System.setProperty(LmdbCascadesOptimizer.TIMEOUT_MILLIS_PROPERTY, "1");
			Snapshot timeoutRun = coldExplain(thirdDir);
			assertEquals(defaultRun.structure(), timeoutRun.structure(),
					"timeoutMillis=1 must not change the AUTO winner");
			assertEquals(defaultRun.counters(), timeoutRun.counters(),
					"timeoutMillis=1 must not change AUTO search counters");
		} finally {
			if (originalTimeout == null) {
				System.clearProperty(LmdbCascadesOptimizer.TIMEOUT_MILLIS_PROPERTY);
			} else {
				System.setProperty(LmdbCascadesOptimizer.TIMEOUT_MILLIS_PROPERTY, originalTimeout);
			}
		}
	}

	private static Snapshot coldExplain(File dataDir) {
		SailRepository repository = repository(dataDir);
		try {
			loadData(repository);
			return explain(repository);
		} finally {
			repository.shutDown();
		}
	}

	private static void loadData(SailRepository repository) {
		IRI knows = VF.createIRI("http://example.com/knows");
		IRI name = VF.createIRI("http://example.com/name");
		try (RepositoryConnection connection = repository.getConnection()) {
			for (int person = 0; person < 24; person++) {
				IRI subject = VF.createIRI("http://example.com/person/" + person);
				connection.add(subject, name, VF.createLiteral("name-" + person));
				connection.add(subject, knows, VF.createIRI("http://example.com/person/" + (person + 1) % 24));
				connection.add(subject, knows, VF.createIRI("http://example.com/person/" + (person + 7) % 24));
			}
		}
	}

	private static Snapshot explain(SailRepository repository) {
		try (RepositoryConnection connection = repository.getConnection()) {
			Explanation explanation = connection.prepareTupleQuery(QUERY).explain(Explanation.Level.Optimized);
			String rendered = new TupleExprIRRenderer().render((TupleExpr) explanation.tupleExpr());
			StringBuilder counters = new StringBuilder();
			Matcher matcher = COUNTER.matcher(explanation.toString());
			while (matcher.find()) {
				counters.append(matcher.group(1)).append('=').append(matcher.group(2)).append('\n');
			}
			return new Snapshot(rendered, counters.toString());
		}
	}

	private record Snapshot(String structure, String counters) {
	}

	private static SailRepository repository(File dataDir) {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,posc")
				.setOptimizerSamplingEnabled(false)
				.setBackgroundRawSamplingMaxMillisPerCycle(0L);
		SailRepository repository = new SailRepository(new LmdbStore(dataDir, config));
		repository.init();
		return repository;
	}
}
