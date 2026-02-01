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

package org.eclipse.rdf4j.sail.lmdb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.lmdb.benchmark.RandomLiteralGenerator;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Low-level benchmarks for LMDB internal components.
 * <p>
 * This benchmark directly tests:
 * <ul>
 * <li>ValueStore: value encoding, ID lookup, cache behavior</li>
 * <li>TripleStore: index operations, record iteration</li>
 * <li>Key encoding: varint performance</li>
 * </ul>
 * <p>
 * Note: This benchmark is in the same package as the LMDB classes to access package-private APIs for low-level testing.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3)
@BenchmarkMode({ Mode.Throughput, Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms2G", "-Xmx2G", "-XX:+UseG1GC" })
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class LmdbInternalsBenchmark {

	private static final String NAMESPACE = "http://example.org/";

	private File dataDir;
	private ValueStore valueStore;
	private TripleStore tripleStore;
	private ValueFactory vf;
	private Random random;
	private RandomLiteralGenerator literalGenerator;

	// Pre-stored value IDs for lookup benchmarks
	private List<Long> storedValueIds;
	private List<Value> storedValues;
	private List<IRI> testIRIs;
	private List<Literal> testLiterals;

	// Pre-stored triple IDs
	private List<long[]> storedTriples;

	@Param({ "512", "2048", "8192" })
	private int valueCacheSize;

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("LmdbInternalsBenchmark\\.")
				.forks(1)
				.build();

		new Runner(opt).run();
	}

	@Setup(Level.Trial)
	public void setupTrial() throws IOException {
		dataDir = Files.newTemporaryFolder();
		vf = SimpleValueFactory.getInstance();
		random = new Random(42);
		literalGenerator = new RandomLiteralGenerator(vf, random);

		// Create config
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,posc,ospc");
		config.setForceSync(false);
		config.setValueDBSize(1_073_741_824L);
		config.setTripleDBSize(1_073_741_824L);
		config.setValueCacheSize(valueCacheSize);

		// Initialize stores directly
		File tripleDir = new File(dataDir, "triples");
		File valueDir = new File(dataDir, "values");
		tripleDir.mkdirs();
		valueDir.mkdirs();

		valueStore = new ValueStore(valueDir, config);
		tripleStore = new TripleStore(tripleDir, config, valueStore);

		// Pre-generate test data
		testIRIs = new ArrayList<>(10000);
		testLiterals = new ArrayList<>(10000);
		storedValueIds = new ArrayList<>(10000);
		storedValues = new ArrayList<>(10000);
		storedTriples = new ArrayList<>(10000);

		for (int i = 0; i < 10000; i++) {
			testIRIs.add(vf.createIRI(NAMESPACE, "resource-" + i));
			testLiterals.add(literalGenerator.createRandomLiteral());
		}

		// Store values and capture IDs
		valueStore.startTransaction(true);
		for (int i = 0; i < 10000; i++) {
			Value value = (i % 2 == 0) ? testIRIs.get(i) : testLiterals.get(i);
			long id = valueStore.storeValue(value);
			storedValueIds.add(id);
			storedValues.add(value);
		}
		valueStore.commit();

		// Store triples
		tripleStore.startTransaction();
		for (int i = 0; i < 10000; i++) {
			long subj = storedValueIds.get(i % storedValueIds.size());
			long pred = storedValueIds.get((i + 1) % storedValueIds.size());
			long obj = storedValueIds.get((i + 2) % storedValueIds.size());
			long ctx = storedValueIds.get((i + 3) % storedValueIds.size());

			tripleStore.storeTriple(subj, pred, obj, ctx, true);
			storedTriples.add(new long[] { subj, pred, obj, ctx });
		}
		tripleStore.commit();
	}

	@TearDown(Level.Trial)
	public void teardownTrial() throws IOException {
		if (tripleStore != null) {
			tripleStore.close();
		}
		if (valueStore != null) {
			valueStore.close();
		}
		FileUtils.deleteDirectory(dataDir);
	}

	// ==================== VALUE STORE BENCHMARKS ====================

	/**
	 * Benchmark value ID lookup (cache hit scenario).
	 */
	@Benchmark
	public long valueIdLookupCacheHit(Blackhole bh) throws IOException {
		// Use a fixed small set to maximize cache hits
		int index = random.nextInt(Math.min(100, storedValues.size()));
		Value value = storedValues.get(index);
		long id = valueStore.getId(value);
		bh.consume(id);
		return id;
	}

	/**
	 * Benchmark value ID lookup (cache miss scenario).
	 */
	@Benchmark
	public long valueIdLookupCacheMiss(Blackhole bh) throws IOException {
		// Use random access to maximize cache misses
		int index = random.nextInt(storedValues.size());
		Value value = storedValues.get(index);
		long id = valueStore.getId(value);
		bh.consume(id);
		return id;
	}

	/**
	 * Benchmark value retrieval by ID (cache hit scenario).
	 */
	@Benchmark
	public Value valueRetrievalCacheHit(Blackhole bh) throws IOException {
		// Use a fixed small set to maximize cache hits
		int index = random.nextInt(Math.min(100, storedValueIds.size()));
		long id = storedValueIds.get(index);
		Value value = valueStore.getValue(id);
		bh.consume(value);
		return value;
	}

	/**
	 * Benchmark value retrieval by ID (cache miss scenario).
	 */
	@Benchmark
	public Value valueRetrievalCacheMiss(Blackhole bh) throws IOException {
		// Use random access to maximize cache misses
		int index = random.nextInt(storedValueIds.size());
		long id = storedValueIds.get(index);
		Value value = valueStore.getValue(id);
		bh.consume(value);
		return value;
	}

	/**
	 * Benchmark storing new values.
	 */
	@Benchmark
	public long storeNewValue(Blackhole bh) throws IOException {
		// Create a unique value each time
		IRI newIRI = vf.createIRI(NAMESPACE, "new-resource-" + random.nextLong());

		valueStore.startTransaction(true);
		long id = valueStore.storeValue(newIRI);
		valueStore.commit();

		bh.consume(id);
		return id;
	}

	/**
	 * Benchmark storing existing values (deduplication).
	 */
	@Benchmark
	public long storeExistingValue(Blackhole bh) throws IOException {
		int index = random.nextInt(storedValues.size());
		Value existingValue = storedValues.get(index);

		valueStore.startTransaction(true);
		long id = valueStore.storeValue(existingValue);
		valueStore.commit();

		bh.consume(id);
		return id;
	}

	// ==================== TRIPLE STORE BENCHMARKS ====================

	/**
	 * Benchmark triple lookup by subject.
	 */
	@Benchmark
	public long tripleLookupBySubject(Blackhole bh) throws IOException {
		long count = 0;
		int index = random.nextInt(storedTriples.size());
		long subj = storedTriples.get(index)[0];

		try (TxnManager.Txn txn = tripleStore.getTxnManager().createReadTxn()) {
			try (RecordIterator iter = tripleStore.getTriples(txn, subj, -1, -1, -1, true)) {
				long[] quad;
				while ((quad = iter.next()) != null) {
					bh.consume(quad[0]); // subject
					count++;
				}
			}
		}
		return count;
	}

	/**
	 * Benchmark triple lookup by predicate.
	 */
	@Benchmark
	public long tripleLookupByPredicate(Blackhole bh) throws IOException {
		long count = 0;
		int index = random.nextInt(storedTriples.size());
		long pred = storedTriples.get(index)[1];

		try (TxnManager.Txn txn = tripleStore.getTxnManager().createReadTxn()) {
			try (RecordIterator iter = tripleStore.getTriples(txn, -1, pred, -1, -1, true)) {
				long[] quad;
				while ((quad = iter.next()) != null) {
					bh.consume(quad[1]); // predicate
					count++;
				}
			}
		}
		return count;
	}

	/**
	 * Benchmark triple lookup by object.
	 */
	@Benchmark
	public long tripleLookupByObject(Blackhole bh) throws IOException {
		long count = 0;
		int index = random.nextInt(storedTriples.size());
		long obj = storedTriples.get(index)[2];

		try (TxnManager.Txn txn = tripleStore.getTxnManager().createReadTxn()) {
			try (RecordIterator iter = tripleStore.getTriples(txn, -1, -1, obj, -1, true)) {
				long[] quad;
				while ((quad = iter.next()) != null) {
					bh.consume(quad[2]); // object
					count++;
				}
			}
		}
		return count;
	}

	/**
	 * Benchmark triple lookup by subject and predicate.
	 */
	@Benchmark
	public long tripleLookupBySubjectPredicate(Blackhole bh) throws IOException {
		long count = 0;
		int index = random.nextInt(storedTriples.size());
		long subj = storedTriples.get(index)[0];
		long pred = storedTriples.get(index)[1];

		try (TxnManager.Txn txn = tripleStore.getTxnManager().createReadTxn()) {
			try (RecordIterator iter = tripleStore.getTriples(txn, subj, pred, -1, -1, true)) {
				long[] quad;
				while ((quad = iter.next()) != null) {
					bh.consume(quad[0]); // subject
					count++;
				}
			}
		}
		return count;
	}

	/**
	 * Benchmark storing new triples.
	 */
	@Benchmark
	public boolean storeNewTriple(Blackhole bh) throws IOException {
		long subj = storedValueIds.get(random.nextInt(storedValueIds.size()));
		long pred = storedValueIds.get(random.nextInt(storedValueIds.size()));
		long obj = storedValueIds.get(random.nextInt(storedValueIds.size()));
		long ctx = storedValueIds.get(random.nextInt(storedValueIds.size()));

		tripleStore.startTransaction();
		boolean added = tripleStore.storeTriple(subj, pred, obj, ctx, true);
		tripleStore.commit();

		bh.consume(added);
		return added;
	}

	/**
	 * Benchmark full index scan (limited).
	 */
	@Benchmark
	public long fullIndexScan(Blackhole bh) throws IOException {
		long count = 0;
		try (TxnManager.Txn txn = tripleStore.getTxnManager().createReadTxn()) {
			try (RecordIterator iter = tripleStore.getTriples(txn, -1, -1, -1, -1, true)) {
				long[] quad;
				while ((quad = iter.next()) != null && count < 1000) {
					bh.consume(quad[0]); // subject
					count++;
				}
			}
		}
		return count;
	}

	// ==================== VARINT ENCODING BENCHMARKS ====================

	/**
	 * Benchmark varint key encoding for small values.
	 */
	@Benchmark
	public void varintEncodeSmall(Blackhole bh) {
		long[] values = { 1L, 100L, 1000L, 10000L };
		for (long v : values) {
			int encoded = Varint.calcLengthUnsigned(v);
			bh.consume(encoded);
		}
	}

	/**
	 * Benchmark varint key encoding for large values.
	 */
	@Benchmark
	public void varintEncodeLarge(Blackhole bh) {
		long[] values = { 1_000_000L, 1_000_000_000L, Long.MAX_VALUE / 2, Long.MAX_VALUE };
		for (long v : values) {
			int encoded = Varint.calcLengthUnsigned(v);
			bh.consume(encoded);
		}
	}
}
