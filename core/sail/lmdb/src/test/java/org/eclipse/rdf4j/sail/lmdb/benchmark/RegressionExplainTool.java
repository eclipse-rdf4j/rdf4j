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
package org.eclipse.rdf4j.sail.lmdb.benchmark;

import java.io.File;

import org.eclipse.rdf4j.benchmark.common.ThemeQueryCatalog;
import org.eclipse.rdf4j.benchmark.rio.util.ThemeDataSetGenerator.Theme;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;

/**
 * Ad-hoc tool: timed run + optimized plan for specific flagged (theme, queryIndex) pairs against the persistent
 * benchmark store. Args: THEME:IDX pairs, e.g. LIBRARY:4 PHARMA:3
 */
public class RegressionExplainTool {

	public static void main(String[] args) throws Exception {
		File storeDirectory = new File(BenchmarkPathSupport.resolveTarget("lmdb-theme-query-benchmark").toFile(),
				System.getProperty("regressionExplain.store", "complete"));
		var store = new LmdbStore(storeDirectory, ConfigUtil.createConfig());
		var repository = new SailRepository(store);
		try {
			for (String arg : args) {
				String[] parts = arg.split(":");
				Theme theme = Theme.valueOf(parts[0]);
				int idx = Integer.parseInt(parts[1]);
				String query = ThemeQueryCatalog.queryFor(theme, idx);
				System.out.println("############ " + theme + " q" + idx + " ############");
				try (var connection = repository.getConnection()) {
					long best = Long.MAX_VALUE;
					long count = -1;
					for (int i = 0; i < 5; i++) {
						TupleQuery tupleQuery = connection.prepareTupleQuery(query);
						tupleQuery.setMaxExecutionTime(60);
						long start = System.nanoTime();
						try (TupleQueryResult result = tupleQuery.evaluate()) {
							count = result.stream().count();
						} catch (RuntimeException e) {
							System.out.println("evaluation failed: " + e);
							break;
						}
						best = Math.min(best, (System.nanoTime() - start) / 1_000_000);
					}
					System.out.println("rows=" + count + " bestOf5=" + best + " ms");
					TupleQuery tupleQuery = connection.prepareTupleQuery(query);
					tupleQuery.setMaxExecutionTime(60);
					Explanation explain = tupleQuery.explain(Explanation.Level.Optimized);
					System.out.println(explain);
				}
				System.out.println();
			}
		} finally {
			repository.shutDown();
		}
	}
}
