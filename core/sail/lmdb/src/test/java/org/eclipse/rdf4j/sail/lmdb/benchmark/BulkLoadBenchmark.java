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
 * Bulk load benchmark for LMDB at scale: 100K, 1M, 10M statements.
 */
public class BulkLoadBenchmark {

	private static final String NAMESPACE = "http://example.org/";

	private File dataDir;
	private ValueFactory vf;
	private Random random;
	private RandomLiteralGenerator literalGenerator;

	private List<IRI> subjects;
	private List<IRI> predicates;
	private List<IRI> types;
	private List<Resource> contexts;

	public static void main(String[] args) throws Exception {
		BulkLoadBenchmark benchmark = new BulkLoadBenchmark();
		benchmark.run();
	}

	public void run() throws Exception {
		System.out.println("=== LMDB Bulk Load Benchmark ===\n");

		initTestData();

		// Test different scales
		int[] scales = { 100_000, 1_000_000, 10_000_000 };

		for (int scale : scales) {
			System.out.println("========================================");
			System.out.printf("Testing scale: %,d statements%n", scale);
			System.out.println("========================================\n");

			runBulkLoadTest(scale);
			runBulkReadTest(scale);

			System.out.println();
		}
	}

	private void initTestData() {
		vf = SimpleValueFactory.getInstance();
		random = new Random(42);
		literalGenerator = new RandomLiteralGenerator(vf, random);

		// Generate larger pools for better distribution
		subjects = new ArrayList<>(100_000);
		predicates = new ArrayList<>(1000);
		types = new ArrayList<>(500);
		contexts = new ArrayList<>(100);

		System.out.println("Generating test data pools...");
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
		System.out.println("Test data ready.\n");
	}

	private void runBulkLoadTest(int numStatements) throws Exception {
		dataDir = Files.newTemporaryFolder();

		// Configure for bulk loading
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,posc,ospc");
		config.setForceSync(false);
		// Size DBs appropriately for the scale
		long dbSize = Math.max(1_073_741_824L, (long) (numStatements * 200L)); // ~200 bytes per statement estimate
		config.setValueDBSize(dbSize);
		config.setTripleDBSize(dbSize);
		config.setValueCacheSize(8192);

		SailRepository repository = new SailRepository(new LmdbStore(dataDir, config));

		try {
			System.out.println("--- BULK WRITE TEST ---");

			// Test 1: Single large transaction
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

					// Progress indicator for large loads
					if (i > 0 && i % 1_000_000 == 0) {
						System.out.printf("  Progress: %,d statements loaded...%n", i);
					}
				}

				conn.commit();
			}
			long singleTxnTime = System.currentTimeMillis() - start;
			double singleTxnRate = numStatements * 1000.0 / singleTxnTime;

			System.out.printf("Single Transaction:     %,10d stmts in %,6d ms = %,.0f stmts/sec%n",
					numStatements, singleTxnTime, singleTxnRate);

			// Get store size
			long storeSize = FileUtils.sizeOfDirectory(dataDir);
			System.out.printf("Store size:             %,.2f MB (%.1f bytes/stmt)%n",
					storeSize / (1024.0 * 1024.0), (double) storeSize / numStatements);

		} finally {
			repository.shutDown();
			FileUtils.deleteDirectory(dataDir);
		}

		// Test 2: Batched transactions (10K per batch)
		dataDir = Files.newTemporaryFolder();
		repository = new SailRepository(new LmdbStore(dataDir, config));

		try {
			long start = System.currentTimeMillis();
			int batchSize = 10_000;

			try (SailRepositoryConnection conn = repository.getConnection()) {
				for (int batch = 0; batch < numStatements; batch += batchSize) {
					conn.begin(IsolationLevels.NONE);

					int end = Math.min(batch + batchSize, numStatements);
					for (int i = batch; i < end; i++) {
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
			}
			long batchedTime = System.currentTimeMillis() - start;
			double batchedRate = numStatements * 1000.0 / batchedTime;

			System.out.printf("Batched (10K/txn):      %,10d stmts in %,6d ms = %,.0f stmts/sec%n",
					numStatements, batchedTime, batchedRate);

		} finally {
			repository.shutDown();
			FileUtils.deleteDirectory(dataDir);
		}

		// Test 3: Batched transactions (100K per batch)
		if (numStatements >= 100_000) {
			dataDir = Files.newTemporaryFolder();
			repository = new SailRepository(new LmdbStore(dataDir, config));

			try {
				long start = System.currentTimeMillis();
				int batchSize = 100_000;

				try (SailRepositoryConnection conn = repository.getConnection()) {
					for (int batch = 0; batch < numStatements; batch += batchSize) {
						conn.begin(IsolationLevels.NONE);

						int end = Math.min(batch + batchSize, numStatements);
						for (int i = batch; i < end; i++) {
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
				}
				long batchedTime = System.currentTimeMillis() - start;
				double batchedRate = numStatements * 1000.0 / batchedTime;

				System.out.printf("Batched (100K/txn):     %,10d stmts in %,6d ms = %,.0f stmts/sec%n",
						numStatements, batchedTime, batchedRate);

			} finally {
				repository.shutDown();
				FileUtils.deleteDirectory(dataDir);
			}
		}
	}

	private void runBulkReadTest(int numStatements) throws Exception {
		dataDir = Files.newTemporaryFolder();

		LmdbStoreConfig config = new LmdbStoreConfig("spoc,posc,ospc");
		config.setForceSync(false);
		long dbSize = Math.max(1_073_741_824L, (long) (numStatements * 200L));
		config.setValueDBSize(dbSize);
		config.setTripleDBSize(dbSize);
		config.setValueCacheSize(8192);

		SailRepository repository = new SailRepository(new LmdbStore(dataDir, config));

		try {
			// First load the data
			System.out.println("\n--- BULK READ TEST (loading data first) ---");
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
				}
				conn.commit();
			}
			System.out.println("Data loaded.");

			// Test 1: Full scan
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
			long fullScanTime = System.currentTimeMillis() - start;
			double fullScanRate = count * 1000.0 / fullScanTime;
			System.out.printf("Full Scan:              %,10d stmts in %,6d ms = %,.0f stmts/sec%n",
					count, fullScanTime, fullScanRate);

			// Test 2: Subject lookups (1000 random subjects)
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
			long subjectLookupTime = System.currentTimeMillis() - start;
			double subjectLookupRate = count * 1000.0 / subjectLookupTime;
			System.out.printf("Subject Lookups (1K):   %,10d stmts in %,6d ms = %,.0f stmts/sec%n",
					count, subjectLookupTime, subjectLookupRate);

			// Test 3: Predicate lookups (100 random predicates)
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
			long predicateLookupTime = System.currentTimeMillis() - start;
			double predicateLookupRate = count * 1000.0 / predicateLookupTime;
			System.out.printf("Predicate Lookups (100): %,9d stmts in %,6d ms = %,.0f stmts/sec%n",
					count, predicateLookupTime, predicateLookupRate);

			// Test 4: Context lookups (all contexts)
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
			long contextLookupTime = System.currentTimeMillis() - start;
			double contextLookupRate = count * 1000.0 / contextLookupTime;
			System.out.printf("Context Lookups (100):  %,10d stmts in %,6d ms = %,.0f stmts/sec%n",
					count, contextLookupTime, contextLookupRate);

		} finally {
			repository.shutDown();
			FileUtils.deleteDirectory(dataDir);
		}
	}
}
