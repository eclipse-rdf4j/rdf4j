/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;

/**
 * Benchmark all 6 single index variants supported by LMDB: spoc, sopc, posc, ospc, opsc, cspo
 */
public class SingleIndexBenchmark {

	private static final String NAMESPACE = "http://example.org/";
	private static final int NUM_STATEMENTS = 1_000_000;

	private ValueFactory vf;
	private Random random;
	private RandomLiteralGenerator literalGenerator;

	private List<IRI> subjects;
	private List<IRI> predicates;
	private List<IRI> types;
	private List<Resource> contexts;

	// All 6 supported single indexes
	private static final String[] INDEXES = { "spoc", "sopc", "posc", "ospc", "opsc", "cspo" };

	public static void main(String[] args) throws Exception {
		SingleIndexBenchmark benchmark = new SingleIndexBenchmark();
		benchmark.run();
	}

	public void run() throws Exception {
		System.out.println("=== LMDB Single Index Benchmark ===");
		System.out.printf("Scale: %,d statements%n%n", NUM_STATEMENTS);

		initTestData();

		// Print header
		System.out.println(
				"====================================================================================================");
		System.out.printf("%-8s | %12s | %12s | %12s | %12s | %12s | %12s%n",
				"Index", "Write", "By Subj", "By Pred", "By Obj", "By Ctx", "Full Scan");
		System.out.printf("%-8s | %12s | %12s | %12s | %12s | %12s | %12s%n",
				"", "(stmts/s)", "(stmts/s)", "(stmts/s)", "(stmts/s)", "(stmts/s)", "(stmts/s)");
		System.out.println(
				"====================================================================================================");

		for (String index : INDEXES) {
			runBenchmark(index);
		}

		System.out.println(
				"====================================================================================================");
	}

	private void initTestData() {
		vf = SimpleValueFactory.getInstance();
		random = new Random(42);
		literalGenerator = new RandomLiteralGenerator(vf, random);

		subjects = new ArrayList<>(10_000);
		predicates = new ArrayList<>(100);
		types = new ArrayList<>(50);
		contexts = new ArrayList<>(20);

		for (int i = 0; i < 10_000; i++) {
			subjects.add(vf.createIRI(NAMESPACE, "subject-" + i));
		}
		for (int i = 0; i < 100; i++) {
			predicates.add(vf.createIRI(NAMESPACE, "predicate-" + i));
		}
		for (int i = 0; i < 50; i++) {
			types.add(vf.createIRI(NAMESPACE, "Type-" + i));
		}
		for (int i = 0; i < 20; i++) {
			contexts.add(vf.createIRI(NAMESPACE, "graph-" + i));
		}
	}

	private void runBenchmark(String index) throws Exception {
		File dataDir = Files.newTemporaryFolder();

		LmdbStoreConfig config = new LmdbStoreConfig(index);
		config.setForceSync(false);
		config.setValueDBSize(2_147_483_648L);
		config.setTripleDBSize(2_147_483_648L);
		config.setValueCacheSize(4096);

		SailRepository repository = new SailRepository(new LmdbStore(dataDir, config));

		long writeRate = 0;
		long readBySubjRate = 0;
		long readByPredRate = 0;
		long readByObjRate = 0;
		long readByCtxRate = 0;
		long fullScanRate = 0;

		try {
			// Write benchmark
			long start = System.currentTimeMillis();
			try (SailRepositoryConnection conn = repository.getConnection()) {
				conn.begin(IsolationLevels.NONE);
				for (int i = 0; i < NUM_STATEMENTS; i++) {
					IRI subject = subjects.get(i % subjects.size());
					IRI predicate = predicates.get(i % predicates.size());
					Resource context = contexts.get(i % contexts.size());

					if (i % 3 == 0) {
						conn.add(subject, RDF.TYPE, types.get(i % types.size()), context);
					} else if (i % 3 == 1) {
						conn.add(subject, RDFS.LABEL, literalGenerator.createRandomLiteral(), context);
					} else {
						IRI object = subjects.get((i + 1) % subjects.size());
						conn.add(subject, predicate, object, context);
					}
				}
				conn.commit();
			}
			long writeTime = System.currentTimeMillis() - start;
			writeRate = (long) (NUM_STATEMENTS * 1000.0 / writeTime);

			// Read by Subject
			start = System.currentTimeMillis();
			long count = 0;
			try (SailRepositoryConnection conn = repository.getConnection()) {
				for (int i = 0; i < 100; i++) {
					IRI subject = subjects.get(random.nextInt(subjects.size()));
					try (CloseableIteration<Statement> iter = conn.getStatements(subject, null, null, false)) {
						while (iter.hasNext()) {
							iter.next();
							count++;
						}
					}
				}
			}
			long readTime = System.currentTimeMillis() - start;
			readBySubjRate = readTime > 0 ? (long) (count * 1000.0 / readTime) : 0;

			// Read by Predicate
			start = System.currentTimeMillis();
			count = 0;
			try (SailRepositoryConnection conn = repository.getConnection()) {
				for (int i = 0; i < 100; i++) {
					IRI predicate = predicates.get(random.nextInt(predicates.size()));
					try (CloseableIteration<Statement> iter = conn.getStatements(null, predicate, null, false)) {
						while (iter.hasNext()) {
							iter.next();
							count++;
						}
					}
				}
			}
			readTime = System.currentTimeMillis() - start;
			readByPredRate = readTime > 0 ? (long) (count * 1000.0 / readTime) : 0;

			// Read by Object
			start = System.currentTimeMillis();
			count = 0;
			try (SailRepositoryConnection conn = repository.getConnection()) {
				for (int i = 0; i < 100; i++) {
					IRI object = types.get(random.nextInt(types.size()));
					try (CloseableIteration<Statement> iter = conn.getStatements(null, null, object, false)) {
						while (iter.hasNext()) {
							iter.next();
							count++;
						}
					}
				}
			}
			readTime = System.currentTimeMillis() - start;
			readByObjRate = readTime > 0 ? (long) (count * 1000.0 / readTime) : 0;

			// Read by Context
			start = System.currentTimeMillis();
			count = 0;
			try (SailRepositoryConnection conn = repository.getConnection()) {
				for (Resource context : contexts) {
					try (CloseableIteration<Statement> iter = conn.getStatements(null, null, null, false, context)) {
						while (iter.hasNext()) {
							iter.next();
							count++;
						}
					}
				}
			}
			readTime = System.currentTimeMillis() - start;
			readByCtxRate = readTime > 0 ? (long) (count * 1000.0 / readTime) : 0;

			// Full Scan
			start = System.currentTimeMillis();
			count = 0;
			try (SailRepositoryConnection conn = repository.getConnection()) {
				try (CloseableIteration<Statement> iter = conn.getStatements(null, null, null, false)) {
					while (iter.hasNext()) {
						iter.next();
						count++;
					}
				}
			}
			readTime = System.currentTimeMillis() - start;
			fullScanRate = readTime > 0 ? (long) (count * 1000.0 / readTime) : 0;

		} finally {
			repository.shutDown();
			FileUtils.deleteDirectory(dataDir);
		}

		System.out.printf("%-8s | %,12d | %,12d | %,12d | %,12d | %,12d | %,12d%n",
				index, writeRate, readBySubjRate, readByPredRate, readByObjRate, readByCtxRate, fullScanRate);
	}
}
