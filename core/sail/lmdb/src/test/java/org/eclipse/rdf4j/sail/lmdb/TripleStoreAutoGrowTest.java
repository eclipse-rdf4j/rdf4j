/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_mapsize;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * Low-level tests for {@link TripleStore}.
 */
public class TripleStoreAutoGrowTest {

	protected TripleStore tripleStore;
	private File dataDir;

	@BeforeEach
	public void before(@TempDir File dataDir) throws Exception {
		this.dataDir = dataDir;
		var config = new LmdbStoreConfig("spoc,posc");
		config.setTripleDBSize(4096 * 10);
		tripleStore = new TripleStore(dataDir, config, null);
		((Logger) LoggerFactory
				.getLogger(TripleStore.class.getName()))
						.setLevel(ch.qos.logback.classic.Level.DEBUG);
	}

	@Test
	public void testAutoGrowLargeCommits() throws Exception {
		Random rnd = new Random(1337);

		Txn[] readTxns = new Txn[20];
		for (int i = 0; i < readTxns.length; i++) {
			readTxns[i] = tripleStore.getTxnManager().createReadTxn();
		}
		for (int subj = 1; subj < 50; subj++) {
			tripleStore.startTransaction();
			for (int pred = 1; pred < 100; pred++) {
				for (int obj = 1; obj < 100; obj++) {
					tripleStore.storeTriple(1 + rnd.nextInt(1000), 1 + rnd.nextInt(1000),
							1 + rnd.nextInt(1000), 3, true);
				}
			}
			tripleStore.commit();
		}
		for (int i = 0; i < readTxns.length; i++) {
			readTxns[i].close();
		}
	}

	@Test
	public void testAutoGrowSmallCommits() throws Exception {
		Random rnd = new Random(1337);

		Txn[] readTxns = new Txn[20];
		for (int i = 0; i < readTxns.length; i++) {
			readTxns[i] = tripleStore.getTxnManager().createReadTxn();
		}
		for (int subj = 1; subj < 50; subj++) {
			for (int pred = 1; pred < 100; pred++) {
				tripleStore.startTransaction();
				for (int obj = 1; obj < 100; obj++) {
					tripleStore.storeTriple(1 + rnd.nextInt(1000), 1 + rnd.nextInt(1000),
							1 + rnd.nextInt(1000), 3, true);
				}
				tripleStore.commit();
			}
		}
		for (int i = 0; i < readTxns.length; i++) {
			readTxns[i].close();
		}
	}

	@Test
	public void testAutoGrowAlignedBulkWrites() throws Exception {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,posc,ospc,cspo,cpos,cosp");
		config.setTripleDBSize(8L * 1024 * 1024);
		try (TripleStore alignedBulkStore = new TripleStore(new File(dataDir, "aligned-bulk"), config, null)) {
			long targetRemainingCapacity = LmdbUtil.MIN_FREE_SPACE + getPageSize(alignedBulkStore);
			StatementBatch batch = createBatchExceedingRemainingCapacity(dataDir, config, targetRemainingCapacity);
			shrinkMapToRemainingCapacity(alignedBulkStore, targetRemainingCapacity);

			alignedBulkStore.startTransaction();
			assertFalse(requiresResize(alignedBulkStore), "Aligned batch should start before resize threshold");
			assertDoesNotThrow(
					() -> alignedBulkStore.storeTriplesAligned(batch.subj, batch.pred, batch.obj, batch.context,
							batch.subj.length, true));
			assertDoesNotThrow(alignedBulkStore::commit);

			try (Txn txn = alignedBulkStore.getTxnManager().createReadTxn()) {
				assertEquals(batch.subj.length, count(alignedBulkStore.getTriples(txn, -1, -1, -1, -1, true)));
			}
		}
	}

	private static StatementBatch createBatchExceedingRemainingCapacity(File dataDir, LmdbStoreConfig config,
			long targetRemainingCapacity)
			throws Exception {
		for (int statementCount = 256; statementCount <= 4096; statementCount *= 2) {
			StatementBatch batch = createBatch(100_000L, statementCount);
			long batchBytes = measureAlignedBatchBytes(new File(dataDir, "aligned-bulk-measure-" + statementCount),
					config, batch);
			if (batchBytes > targetRemainingCapacity) {
				return batch;
			}
		}
		throw new AssertionError("Aligned batch never exceeded remaining map capacity");
	}

	private static long measureAlignedBatchBytes(File dir, LmdbStoreConfig config, StatementBatch batch)
			throws Exception {
		try (TripleStore store = new TripleStore(dir, config, null)) {
			store.startTransaction();
			try {
				long usedBefore = currentCapacityUsage(store);
				store.storeTriplesAligned(batch.subj, batch.pred, batch.obj, batch.context, batch.subj.length, true);
				long usedAfter = currentCapacityUsage(store);
				assertTrue(usedAfter > usedBefore, "Aligned batch should consume LMDB map space");
				return usedAfter - usedBefore;
			} finally {
				store.rollback();
			}
		}
	}

	private static void shrinkMapToRemainingCapacity(TripleStore store, long targetRemainingCapacity) throws Exception {
		store.startTransaction();
		long usedCapacity;
		try {
			usedCapacity = currentCapacityUsage(store);
		} finally {
			store.rollback();
		}

		long newMapSize = alignToPageSize(usedCapacity + targetRemainingCapacity, getPageSize(store));
		LmdbUtil.E(mdb_env_set_mapsize(getLongField(store, "env"), newMapSize));
		setLongField(store, "mapSize", newMapSize);
	}

	private static long currentCapacityUsage(TripleStore store) throws Exception {
		return LmdbUtil.getNewSize(getPageSize(store), getLongField(store, "writeTxn"), 0);
	}

	private static int getPageSize(TripleStore store) throws Exception {
		Field pageSizeField = TripleStore.class.getDeclaredField("pageSize");
		pageSizeField.setAccessible(true);
		return pageSizeField.getInt(store);
	}

	private static boolean requiresResize(TripleStore store) throws Exception {
		Method requiresResize = TripleStore.class.getDeclaredMethod("requiresResize");
		requiresResize.setAccessible(true);
		return (boolean) requiresResize.invoke(store);
	}

	private static long getLongField(TripleStore store, String fieldName) throws Exception {
		Field field = TripleStore.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.getLong(store);
	}

	private static void setLongField(TripleStore store, String fieldName, long value) throws Exception {
		Field field = TripleStore.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.setLong(store, value);
	}

	private static long alignToPageSize(long value, long pageSize) {
		return value % pageSize == 0 ? value : value + pageSize - (value % pageSize);
	}

	private static StatementBatch createBatch(long baseId, int statementCount) {
		long[] subj = new long[statementCount];
		long[] pred = new long[statementCount];
		long[] obj = new long[statementCount];
		long[] context = new long[statementCount];
		for (int i = 0; i < statementCount; i++) {
			subj[i] = baseId + i;
			pred[i] = (i % 31) + 1L;
			obj[i] = baseId + 100_000L + i;
			context[i] = (i % 7) + 1L;
		}
		return new StatementBatch(subj, pred, obj, context);
	}

	private static final class StatementBatch {
		private final long[] subj;
		private final long[] pred;
		private final long[] obj;
		private final long[] context;

		private StatementBatch(long[] subj, long[] pred, long[] obj, long[] context) {
			this.subj = subj;
			this.pred = pred;
			this.obj = obj;
			this.context = context;
		}
	}

	private static int count(RecordIterator it) {
		int count = 0;
		try (it) {
			while (it.next() != null) {
				count++;
			}
		}
		return count;
	}

	@AfterEach
	public void after() throws Exception {
		try {
			tripleStore.close();
		} finally {
			LmdbTestUtil.deleteDir(dataDir);
		}
	}
}
