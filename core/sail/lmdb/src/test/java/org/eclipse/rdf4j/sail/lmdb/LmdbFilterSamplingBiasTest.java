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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Live filter sampling must not read only the first rows in index order: when the filtered variable correlates with
 * index order, a budget-capped prefix scan produces a systematically wrong pass ratio that is then cached and
 * persisted.
 */
class LmdbFilterSamplingBiasTest {

	// Object values ascend with insertion order, and so does the spoc key order (value-store ids are assigned
	// sequentially): the first rows in index order all carry small values and all FAIL the filter.
	private static final String QUERY = """
			SELECT * WHERE {
				?s <urn:test:ordered-value> ?value .
				FILTER(?value > 8192)
			}
			""";

	private static final int ROWS = 16_384;

	@Test
	void indexOrderCorrelatedFilterSamplesWithoutPrefixBias(@TempDir File dataDir) {
		LmdbStore store = new LmdbStore(dataDir, samplingConfig());
		SailRepository repository = new SailRepository(store);
		repository.init();
		try {
			loadOrderedRows(repository);

			EvaluationStatistics.FilterPassEstimate estimate = store.getBackingStore()
					.getEvaluationStatistics()
					.estimateFilterPass(firstFilter(QUERY));

			assertEquals(EvaluationStatistics.FilterPassEstimate.Source.SAMPLED, estimate.getSource());
			assertTrue(estimate.getPassRatio() >= 0.3d && estimate.getPassRatio() <= 0.7d,
					"The true pass ratio is ~0.5 but an index-order prefix sample sees only failing rows"
							+ " (sampled ratio was " + estimate.getPassRatio() + ")");
		} finally {
			repository.shutDown();
		}
	}

	@Test
	void prefixModeStillSamplesButCarriesReducedEvidence(@TempDir File dataDir) {
		String previous = System.getProperty(LmdbFilterSelectivityStats.FILTER_SAMPLING_MODE_PROPERTY);
		System.setProperty(LmdbFilterSelectivityStats.FILTER_SAMPLING_MODE_PROPERTY, "prefix");
		try {
			LmdbStore store = new LmdbStore(dataDir, samplingConfig());
			SailRepository repository = new SailRepository(store);
			repository.init();
			try {
				loadOrderedRows(repository);

				EvaluationStatistics.FilterPassEstimate estimate = store.getBackingStore()
						.getEvaluationStatistics()
						.estimateFilterPass(firstFilter(QUERY));

				if (estimate.getSource() == EvaluationStatistics.FilterPassEstimate.Source.SAMPLED) {
					// The prefix sample stopped inside the range: its evidence must be discounted so a fresh
					// unbiased source can out-rank it.
					assertTrue(estimate.getEvidenceCount() * 4L <= ROWS,
							"A range-truncated prefix sample must report reduced evidence, but reported "
									+ estimate.getEvidenceCount());
				}
			} finally {
				repository.shutDown();
			}
		} finally {
			if (previous == null) {
				System.clearProperty(LmdbFilterSelectivityStats.FILTER_SAMPLING_MODE_PROPERTY);
			} else {
				System.setProperty(LmdbFilterSelectivityStats.FILTER_SAMPLING_MODE_PROPERTY, previous);
			}
		}
	}

	private static LmdbStoreConfig samplingConfig() {
		return new LmdbStoreConfig()
				.setTripleIndexes("spoc")
				.setSketchEstimatorEnabled(false)
				.setOptimizerSamplingEnabled(true)
				.setBackgroundRawSamplingEnabled(false);
	}

	private static void loadOrderedRows(SailRepository repository) {
		ValueFactory vf = repository.getValueFactory();
		IRI predicate = vf.createIRI("urn:test:ordered-value");
		try (var connection = repository.getConnection()) {
			connection.begin();
			for (int i = 0; i < ROWS; i++) {
				connection.add(vf.createIRI("urn:test:row:" + i), predicate, vf.createLiteral(i));
			}
			connection.commit();
		}
	}

	private static Filter firstFilter(String query) {
		Filter[] found = new Filter[1];
		QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null)
				.getTupleExpr()
				.visit(new AbstractQueryModelVisitor<RuntimeException>() {
					@Override
					public void meet(Filter node) {
						if (found[0] == null) {
							found[0] = node;
						}
						super.meet(node);
					}
				});
		return found[0];
	}
}
