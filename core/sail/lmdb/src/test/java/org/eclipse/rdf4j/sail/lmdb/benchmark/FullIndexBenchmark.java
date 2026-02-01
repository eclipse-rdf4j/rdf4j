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
 * Benchmark LMDB with all 6 indexes enabled: spoc,sopc,posc,ospc,opsc,cspo
 */
public class FullIndexBenchmark {

	private static final String NAMESPACE = "http://example.org/";
	private static final String INDEX_CONFIG = "spoc,sopc,posc,ospc,opsc,cspo";

	private File dataDir;
	private SailRepository repository;
	private ValueFactory vf;
	private Random random;
	private RandomLiteralGenerator literalGenerator;

	private List<IRI> subjects;
	private List<IRI> predicates;
	private List<IRI> types;
	private List<Resource> contexts;

	public static void main(String[] args) throws Exception {
		FullIndexBenchmark benchmark = new FullIndexBenchmark();
		benchmark.run();
	}

	public void run() throws Exception {
		System.out.println("=== LMDB Full Index Benchmark ===");
		System.out.println("Index Config: " + INDEX_CONFIG);
		System.out.println();

		initTestData();

		// Test at different scales
		int[] scales = { 100_000, 1_000_000, 10_000_000 };

		for (int scale : scales) {
			System.out.println("=".repeat(80));
			System.out.printf("Scale: %,d statements%n", scale);
			System.out.println("=".repeat(80));

			setup(scale);
			try {
				runWriteBenchmark(scale);
				runReadBenchmarks();
			} finally {
				teardown();
			}
			System.out.println();
		}
	}

	private void initTestData() {
		vf = SimpleValueFactory.getInstance();
		random = new Random(42);
		literalGenerator = new RandomLiteralGenerator(vf, random);

		subjects = new ArrayList<>(100_000);
		predicates = new ArrayList<>(1000);
		types = new ArrayList<>(500);
		contexts = new ArrayList<>(100);

		for (int i = 0; i < 100_000; i++) {
			subjects.add(vf.createIRI(NAMESPACE, "subject-" + i));
		}
		for (int i = 0; i < 1000; i++) {
			predicates.add(vf.createIRI(NAMESPACE, "predicate-" + i));
		}
		for (int i = 0; i < 500; i++) {
			types.add(vf.createIRI(NAMESPACE, "Type-" + i));
		}
		for (int i = 0; i < 100; i++) {
			contexts.add(vf.createIRI(NAMESPACE, "graph-" + i));
		}
	}

	private void setup(int scale) throws Exception {
		dataDir = Files.newTemporaryFolder();

		LmdbStoreConfig config = new LmdbStoreConfig(INDEX_CONFIG);
		config.setForceSync(false);
		long dbSize = Math.max(2_147_483_648L, (long) (scale * 500L));
		config.setValueDBSize(dbSize);
		config.setTripleDBSize(dbSize);
		config.setValueCacheSize(8192);

		repository = new SailRepository(new LmdbStore(dataDir, config));
	}

	private void teardown() throws Exception {
		if (repository != null) {
			repository.shutDown();
		}
		FileUtils.deleteDirectory(dataDir);
	}

	private void runWriteBenchmark(int numStatements) throws Exception {
		System.out.println("\n--- WRITE ---");

		long start = System.currentTimeMillis();
		try (SailRepositoryConnection conn = repository.getConnection()) {
			conn.begin(IsolationLevels.NONE);
			for (int i = 0; i < numStatements; i++) {
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

				if (i > 0 && i % 1_000_000 == 0) {
					System.out.printf("  Progress: %,d statements...%n", i);
				}
			}
			conn.commit();
		}
		long elapsed = System.currentTimeMillis() - start;
		double rate = numStatements * 1000.0 / elapsed;

		long storeSize = FileUtils.sizeOfDirectory(dataDir);

		System.out.printf("Write:        %,d stmts in %,d ms = %,.0f stmts/sec%n", numStatements, elapsed, rate);
		System.out.printf("Store size:   %,.2f MB (%.1f bytes/stmt)%n",
				storeSize / (1024.0 * 1024.0), (double) storeSize / numStatements);
	}

	private void runReadBenchmarks() throws Exception {
		System.out.println("\n--- READ ---");

		// Full Scan
		long start = System.currentTimeMillis();
		long count = 0;
		try (SailRepositoryConnection conn = repository.getConnection()) {
			try (CloseableIteration<Statement> iter = conn.getStatements(null, null, null, false)) {
				while (iter.hasNext()) {
					iter.next();
					count++;
				}
			}
		}
		long elapsed = System.currentTimeMillis() - start;
		System.out.printf("Full Scan:    %,d stmts in %,d ms = %,.0f stmts/sec%n",
				count, elapsed, count * 1000.0 / elapsed);

		// By Subject (1000 lookups)
		start = System.currentTimeMillis();
		count = 0;
		try (SailRepositoryConnection conn = repository.getConnection()) {
			for (int i = 0; i < 1000; i++) {
				IRI subject = subjects.get(random.nextInt(subjects.size()));
				try (CloseableIteration<Statement> iter = conn.getStatements(subject, null, null, false)) {
					while (iter.hasNext()) {
						iter.next();
						count++;
					}
				}
			}
		}
		elapsed = System.currentTimeMillis() - start;
		System.out.printf("By Subject:   %,d stmts in %,d ms = %,.0f stmts/sec%n",
				count, elapsed, count * 1000.0 / elapsed);

		// By Predicate (100 lookups)
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
		elapsed = System.currentTimeMillis() - start;
		System.out.printf("By Predicate: %,d stmts in %,d ms = %,.0f stmts/sec%n",
				count, elapsed, count * 1000.0 / elapsed);

		// By Object (100 lookups)
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
		elapsed = System.currentTimeMillis() - start;
		System.out.printf("By Object:    %,d stmts in %,d ms = %,.0f stmts/sec%n",
				count, elapsed, count * 1000.0 / elapsed);

		// By Context (all contexts)
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
		elapsed = System.currentTimeMillis() - start;
		System.out.printf("By Context:   %,d stmts in %,d ms = %,.0f stmts/sec%n",
				count, elapsed, count * 1000.0 / elapsed);
	}
}
