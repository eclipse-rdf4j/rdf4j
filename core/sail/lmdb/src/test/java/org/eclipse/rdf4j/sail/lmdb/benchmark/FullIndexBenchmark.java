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
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * Benchmark LMDB with all 6 indexes enabled: spoc,sopc,posc,ospc,opsc,cspo Outputs results in markdown report format.
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

	// Results storage
	private final Map<String, ScaleResult> results = new LinkedHashMap<>();

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
			String scaleLabel = formatScale(scale);
			System.out.println("=".repeat(80));
			System.out.printf("Scale: %s statements%n", scaleLabel);
			System.out.println("=".repeat(80));

			ScaleResult result = new ScaleResult(scale);
			results.put(scaleLabel, result);

			setup(scale);
			try {
				runWriteBenchmark(scale, result);
				runReadBenchmarks(result);
			} finally {
				teardown();
			}
			System.out.println();
		}

		// Generate report
		generateReport();
	}

	private String formatScale(int scale) {
		if (scale >= 1_000_000_000) {
			return (scale / 1_000_000_000) + "B";
		} else if (scale >= 1_000_000) {
			return (scale / 1_000_000) + "M";
		} else if (scale >= 1_000) {
			return (scale / 1_000) + "K";
		}
		return String.valueOf(scale);
	}

	private void initTestData() {
		vf = SimpleValueFactory.getInstance();
		random = new Random(42);
		literalGenerator = new RandomLiteralGenerator(vf, random);

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
		System.out.println("Done.\n");
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

	private void runWriteBenchmark(int numStatements, ScaleResult result) throws Exception {
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

				if (i > 0 && i % 10_000_000 == 0) {
					System.out.printf("  Progress: %,d statements...%n", i);
				}
			}
			conn.commit();
		}
		long elapsed = System.currentTimeMillis() - start;
		double rate = numStatements * 1000.0 / elapsed;

		long storeSize = FileUtils.sizeOfDirectory(dataDir);
		double bytesPerStmt = (double) storeSize / numStatements;

		result.writeTimeMs = elapsed;
		result.writeRate = (long) rate;
		result.storeSizeBytes = storeSize;
		result.bytesPerStatement = bytesPerStmt;

		System.out.printf("Write:        %,d stmts in %,d ms = %,.0f stmts/sec%n", numStatements, elapsed, rate);
		System.out.printf("Store size:   %,.2f MB (%.1f bytes/stmt)%n", storeSize / (1024.0 * 1024.0), bytesPerStmt);
	}

	private void runReadBenchmarks(ScaleResult result) throws Exception {
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
		result.fullScanRate = (long) (count * 1000.0 / elapsed);
		System.out.printf("Full Scan:    %,d stmts in %,d ms = %,.0f stmts/sec%n", count, elapsed,
				(double) result.fullScanRate);

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
		result.bySubjectRate = elapsed > 0 ? (long) (count * 1000.0 / elapsed) : 0;
		System.out.printf("By Subject:   %,d stmts in %,d ms = %,.0f stmts/sec%n", count, elapsed,
				(double) result.bySubjectRate);

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
		result.byPredicateRate = elapsed > 0 ? (long) (count * 1000.0 / elapsed) : 0;
		System.out.printf("By Predicate: %,d stmts in %,d ms = %,.0f stmts/sec%n", count, elapsed,
				(double) result.byPredicateRate);

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
		result.byObjectRate = elapsed > 0 ? (long) (count * 1000.0 / elapsed) : 0;
		System.out.printf("By Object:    %,d stmts in %,d ms = %,.0f stmts/sec%n", count, elapsed,
				(double) result.byObjectRate);

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
		result.byContextRate = elapsed > 0 ? (long) (count * 1000.0 / elapsed) : 0;
		System.out.printf("By Context:   %,d stmts in %,d ms = %,.0f stmts/sec%n", count, elapsed,
				(double) result.byContextRate);
	}

	private void generateReport() throws Exception {
		String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
		String reportFile = "lmdb-benchmark-report-" + timestamp + ".md";

		try (PrintWriter out = new PrintWriter(new FileWriter(reportFile))) {
			out.println("# LMDB Benchmark Report");
			out.println();
			out.println("**Date:** " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			out.println();
			out.println("**Index Configuration:** `" + INDEX_CONFIG + "`");
			out.println();
			out.println("**Java Version:** " + System.getProperty("java.version"));
			out.println();
			out.println("**OS:** " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
			out.println();

			// Write Performance Table
			out.println("## Write Performance");
			out.println();
			out.println("| Scale | Time (ms) | Rate (stmts/sec) | Store Size | Bytes/stmt |");
			out.println("|-------|-----------|------------------|------------|------------|");
			for (Map.Entry<String, ScaleResult> entry : results.entrySet()) {
				ScaleResult r = entry.getValue();
				out.printf("| %s | %,d | %,d | %.2f MB | %.1f |%n",
						entry.getKey(),
						r.writeTimeMs,
						r.writeRate,
						r.storeSizeBytes / (1024.0 * 1024.0),
						r.bytesPerStatement);
			}
			out.println();

			// Read Performance Table
			out.println("## Read Performance (stmts/sec)");
			out.println();
			out.println("| Scale | Full Scan | By Subject | By Predicate | By Object | By Context |");
			out.println("|-------|-----------|------------|--------------|-----------|------------|");
			for (Map.Entry<String, ScaleResult> entry : results.entrySet()) {
				ScaleResult r = entry.getValue();
				out.printf("| %s | %,d | %,d | %,d | %,d | %,d |%n",
						entry.getKey(),
						r.fullScanRate,
						r.bySubjectRate,
						r.byPredicateRate,
						r.byObjectRate,
						r.byContextRate);
			}
			out.println();

			// Summary
			out.println("## Summary");
			out.println();

			ScaleResult largest = null;
			String largestScale = "";
			for (Map.Entry<String, ScaleResult> entry : results.entrySet()) {
				largest = entry.getValue();
				largestScale = entry.getKey();
			}

			if (largest != null) {
				out.println("At **" + largestScale + "** scale:");
				out.println();
				out.printf("- **Write throughput:** %,d statements/sec%n", largest.writeRate);
				out.printf("- **Storage efficiency:** %.1f bytes/statement%n", largest.bytesPerStatement);
				out.printf("- **Full scan:** %,d statements/sec%n", largest.fullScanRate);
				out.printf("- **Indexed lookups:** %,d - %,d statements/sec%n",
						Math.min(Math.min(largest.bySubjectRate, largest.byPredicateRate),
								Math.min(largest.byObjectRate, largest.byContextRate)),
						Math.max(Math.max(largest.bySubjectRate, largest.byPredicateRate),
								Math.max(largest.byObjectRate, largest.byContextRate)));
			}
		}

		System.out.println("\n" + "=".repeat(80));
		System.out.println("Report saved to: " + reportFile);
		System.out.println("=".repeat(80));

		// Also print report to console
		System.out.println();
		java.nio.file.Files.readAllLines(new File(reportFile).toPath()).forEach(System.out::println);
	}

	static class ScaleResult {
		int scale;
		long writeTimeMs;
		long writeRate;
		long storeSizeBytes;
		double bytesPerStatement;
		long fullScanRate;
		long bySubjectRate;
		long byPredicateRate;
		long byObjectRate;
		long byContextRate;

		ScaleResult(int scale) {
			this.scale = scale;
		}
	}
}
