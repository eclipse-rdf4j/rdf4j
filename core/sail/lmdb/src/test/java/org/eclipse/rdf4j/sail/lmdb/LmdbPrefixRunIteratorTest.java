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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.sail.lmdb.TripleIndex.CONTEXT_IDX;
import static org.eclipse.rdf4j.sail.lmdb.TripleIndex.OBJ_IDX;
import static org.eclipse.rdf4j.sail.lmdb.TripleIndex.PRED_IDX;
import static org.eclipse.rdf4j.sail.lmdb.TripleIndex.SUBJ_IDX;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.eclipse.rdf4j.sail.lmdb.TxnManager.Txn;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Storage-level tests for prefix-run scans over LMDB statement indexes.
 */
public class LmdbPrefixRunIteratorTest {

	@TempDir
	File dataDir;

	private TripleStore tripleStore;

	@AfterEach
	public void tearDown() throws Exception {
		System.clearProperty(LmdbPrefixRunPlan.ENABLED_PROPERTY);
		if (tripleStore != null) {
			tripleStore.close();
		}
	}

	private void open(String indexes) throws Exception {
		tripleStore = new TripleStore(dataDir, new LmdbStoreConfig(indexes), null);
	}

	/*------------------*
	 * Scan correctness *
	 *------------------*/

	@Test
	public void poscPrefixLengthOneEmitsOnePredicateRun() throws Exception {
		open("spoc,posc");
		storeDefaultRows();

		ScanResult result = scan(new int[] { PRED_IDX }, -1, -1, -1, -1, false);

		assertThat(result.values(PRED_IDX)).containsExactly(10L, 20L, 30L);
		assertThat(result.rows).hasSize(3);
		assertThat(result.plan.index().toString()).isEqualTo("posc");
		assertThat(result.plan.prefixLength()).isEqualTo(1);
	}

	@Test
	public void poscPrefixLengthTwoEmitsPredicateObjectRuns() throws Exception {
		open("spoc,posc");
		storeDefaultRows();

		ScanResult result = scan(new int[] { PRED_IDX, OBJ_IDX }, -1, -1, -1, -1, false);

		assertThat(result.pairs(PRED_IDX, OBJ_IDX)).containsExactly("10/1000", "10/1001", "20/2000", "30/3000");
		assertThat(result.plan.prefixLength()).isEqualTo(2);
	}

	@Test
	public void poscPrefixLengthThreeEmitsPredicateObjectSubjectRuns() throws Exception {
		open("spoc,posc");
		storeDefaultRows();

		ScanResult result = scan(new int[] { PRED_IDX, OBJ_IDX, SUBJ_IDX }, -1, -1, -1, -1, false);

		assertThat(result.triples(PRED_IDX, OBJ_IDX, SUBJ_IDX)).containsExactly("10/1000/100", "10/1000/101",
				"10/1001/102", "20/2000/200", "20/2000/201", "30/3000/300");
	}

	@Test
	public void prefixFieldOrderDoesNotMatter() throws Exception {
		open("spoc,posc");
		storeDefaultRows();

		ScanResult result = scan(new int[] { OBJ_IDX, PRED_IDX }, -1, -1, -1, -1, false);

		assertThat(result.plan.index().toString()).isEqualTo("posc");
		assertThat(result.pairs(PRED_IDX, OBJ_IDX)).containsExactly("10/1000", "10/1001", "20/2000", "30/3000");
	}

	@Test
	public void boundPredicatePrecedingDistinctObject() throws Exception {
		open("spoc,posc");
		storeDefaultRows();
		tripleStore.startTransaction();
		tripleStore.storeTriple(103, 10, 1002, 1, true);
		tripleStore.storeTriple(104, 20, 1002, 1, true);
		tripleStore.commit();

		ScanResult result = scan(new int[] { OBJ_IDX }, -1, 10, -1, -1, false);

		assertThat(result.values(OBJ_IDX)).containsExactly(1000L, 1001L, 1002L);
		assertThat(result.values(PRED_IDX)).containsOnly(10L);
		assertThat(result.plan.index().toString()).isEqualTo("posc");
		assertThat(result.plan.prefixLength()).isEqualTo(2);
	}

	@Test
	public void boundContextAfterPrefixFiltersOtherContexts() throws Exception {
		open("spoc,posc");
		tripleStore.startTransaction();
		tripleStore.storeTriple(100, 10, 1000, 1, true);
		tripleStore.storeTriple(100, 10, 1000, 2, true);
		tripleStore.storeTriple(101, 20, 1000, 2, true);
		tripleStore.storeTriple(102, 30, 1000, 1, true);
		tripleStore.storeTriple(103, 40, 1000, 0, true);
		tripleStore.commit();

		assertThat(scan(new int[] { PRED_IDX }, -1, -1, -1, 1, false).values(PRED_IDX)).containsExactly(10L, 30L);
		assertThat(scan(new int[] { PRED_IDX }, -1, -1, -1, 2, false).values(PRED_IDX)).containsExactly(10L, 20L);
		assertThat(scan(new int[] { PRED_IDX }, -1, -1, -1, 0, false).values(PRED_IDX)).containsExactly(40L);
		assertThat(scan(new int[] { PRED_IDX }, -1, -1, -1, -1, false).values(PRED_IDX)).containsExactly(10L, 20L,
				30L, 40L);
	}

	@Test
	public void longRunsSeekPastTheRunInsteadOfScanningIt() throws Exception {
		open("spoc,posc");
		int rowsPerPredicate = 500;
		tripleStore.startTransaction();
		for (long pred = 1; pred <= 3; pred++) {
			for (long subj = 1; subj <= rowsPerPredicate; subj++) {
				tripleStore.storeTriple(1000 + subj, pred, 5000 + subj % 7, 1, true);
			}
		}
		tripleStore.commit();

		ScanResult result = scan(new int[] { PRED_IDX }, -1, -1, -1, -1, false);

		assertThat(result.values(PRED_IDX)).containsExactly(1L, 2L, 3L);
		// each run costs the representative row plus at most SKIP_MIN_RUN stepped rows before the seek engages
		assertThat(result.rowsScanned).isLessThanOrEqualTo(3L * (LmdbPrefixRunIterator.SKIP_MIN_RUN + 1));
		assertThat(result.rowsScanned).isLessThan(3L * rowsPerPredicate);
	}

	@Test
	public void shortRunsAreSteppedOverWithoutSeeking() throws Exception {
		open("spoc,posc");
		int predicates = 200;
		tripleStore.startTransaction();
		for (long pred = 1; pred <= predicates; pred++) {
			tripleStore.storeTriple(100, pred, 1000, 1, true);
			tripleStore.storeTriple(101, pred, 1000, 1, true);
		}
		tripleStore.commit();

		ScanResult result = scan(new int[] { PRED_IDX }, -1, -1, -1, -1, false);

		assertThat(result.rows).hasSize(predicates);
		assertThat(result.values(PRED_IDX)).isSorted();
		// every row is visited exactly once: stepping is cheaper than a seek for runs of two rows
		assertThat(result.rowsScanned).isEqualTo(2L * predicates);
	}

	@Test
	public void runsLongerThanSkipMinRunResumeCorrectlyAfterSeek() throws Exception {
		open("spoc,posc");
		// runs straddling the skip threshold: 1, SKIP_MIN_RUN, SKIP_MIN_RUN + 1, SKIP_MIN_RUN + 2, 3 rows
		int[] runLengths = { 1, LmdbPrefixRunIterator.SKIP_MIN_RUN, LmdbPrefixRunIterator.SKIP_MIN_RUN + 1,
				LmdbPrefixRunIterator.SKIP_MIN_RUN + 2, 3 };
		tripleStore.startTransaction();
		for (int i = 0; i < runLengths.length; i++) {
			for (int row = 0; row < runLengths[i]; row++) {
				tripleStore.storeTriple(1000 + row, 10 + i, 500 + row, 1, true);
			}
		}
		tripleStore.commit();

		ScanResult result = scan(new int[] { PRED_IDX }, -1, -1, -1, -1, false);

		assertThat(result.values(PRED_IDX)).containsExactly(10L, 11L, 12L, 13L, 14L);
	}

	@Test
	public void prefixSeekCarriesWhenLastPrefixComponentOverflows() throws Exception {
		open("spoc,posc");
		tripleStore.startTransaction();
		tripleStore.storeTriple(100, 10, Long.MAX_VALUE, 1, true);
		tripleStore.storeTriple(101, 11, 1, 1, true);
		tripleStore.commit();

		ScanResult result = scan(new int[] { PRED_IDX, OBJ_IDX }, -1, -1, -1, -1, false);

		assertThat(result.pairs(PRED_IDX, OBJ_IDX)).containsExactly("10/" + Long.MAX_VALUE, "11/1");
	}

	@Test
	public void countingModeReportsRunSizes() throws Exception {
		open("spoc,posc");
		storeDefaultRows();
		tripleStore.startTransaction();
		tripleStore.storeTriple(105, 10, 1000, 2, true);
		tripleStore.commit();

		ScanResult result = scan(new int[] { PRED_IDX }, -1, -1, -1, -1, true);

		assertThat(result.values(PRED_IDX)).containsExactly(10L, 20L, 30L);
		assertThat(result.runRowCounts).containsExactly(4L, 2L, 1L);
		// counting a run visits every one of its rows
		assertThat(result.rowsScanned).isEqualTo(7L);

		ScanResult inContext = scan(new int[] { PRED_IDX }, -1, -1, -1, 1, true);
		assertThat(inContext.runRowCounts).containsExactly(3L, 2L, 1L);
	}

	@Test
	public void randomDataMatchesFullScanForAllEligiblePlans() throws Exception {
		open("spoc,posc,ospc");
		Random random = new Random(42);
		tripleStore.startTransaction();
		for (int i = 0; i < 4000; i++) {
			// skewed distributions produce both very long and very short runs
			long subj = 1 + random.nextInt(random.nextBoolean() ? 5 : 400);
			long pred = 1 + random.nextInt(random.nextBoolean() ? 3 : 60);
			long obj = 1 + random.nextInt(random.nextBoolean() ? 4 : 300);
			long context = random.nextInt(3);
			tripleStore.storeTriple(subj, pred, obj, context, true);
		}
		tripleStore.commit();

		int[][] fieldSets = { { SUBJ_IDX }, { PRED_IDX }, { OBJ_IDX }, { CONTEXT_IDX }, { PRED_IDX, OBJ_IDX },
				{ SUBJ_IDX, PRED_IDX }, { OBJ_IDX, SUBJ_IDX }, { PRED_IDX, OBJ_IDX, SUBJ_IDX },
				{ SUBJ_IDX, PRED_IDX, OBJ_IDX }, { OBJ_IDX, SUBJ_IDX, PRED_IDX } };
		long[][] patterns = { { -1, -1, -1, -1 }, { 2, -1, -1, -1 }, { -1, 2, -1, -1 }, { -1, -1, 2, -1 },
				{ -1, -1, -1, 1 }, { -1, 1, -1, 2 }, { 3, 2, -1, -1 }, { -1, 2, 3, -1 } };
		int eligible = 0;
		for (int[] fields : fieldSets) {
			for (long[] pattern : patterns) {
				LmdbPrefixRunPlan plan = tripleStore.prefixRunPlan(fields, pattern[0], pattern[1], pattern[2],
						pattern[3]);
				if (plan == null) {
					continue;
				}
				eligible++;
				Map<String, Long> expected = fullScan(fields, pattern);
				for (boolean countRunRows : new boolean[] { false, true }) {
					ScanResult result = scan(fields, pattern[0], pattern[1], pattern[2], pattern[3], countRunRows);
					String description = "fields " + Arrays.toString(fields) + " pattern " + Arrays.toString(pattern)
							+ " count " + countRunRows + " plan " + plan;
					assertThat(result.keys(fields)).as(description).containsExactlyElementsOf(expected.keySet());
					if (countRunRows) {
						assertThat(result.runRowCounts).as(description)
								.containsExactlyElementsOf(expected.values());
					}
					for (long[] row : result.rows) {
						for (int field = 0; field < 4; field++) {
							if (pattern[field] != -1) {
								assertThat(row[field]).as(description).isEqualTo(pattern[field]);
							}
						}
					}
				}
			}
		}
		assertThat(eligible).isGreaterThan(20);
	}

	/*-------------*
	 * Eligibility *
	 *-------------*/

	@Test
	public void eligibilityRequiresContiguousIndexPrefixAfterBoundFields() throws Exception {
		open("spoc,posc");

		assertPlan(new int[] { PRED_IDX }, false, false, false, false, "posc", 1);
		assertPlan(new int[] { PRED_IDX, OBJ_IDX }, false, false, false, false, "posc", 2);
		assertPlan(new int[] { OBJ_IDX, PRED_IDX }, false, false, false, false, "posc", 2);
		assertPlan(new int[] { SUBJ_IDX }, false, false, false, false, "spoc", 1);
		assertPlan(new int[] { SUBJ_IDX, PRED_IDX }, false, false, false, false, "spoc", 2);
		assertPlan(new int[] { SUBJ_IDX, PRED_IDX, OBJ_IDX }, false, false, false, false, "spoc", 3);
		// bound fields preceding the prefix
		assertPlan(new int[] { OBJ_IDX }, false, true, false, false, "posc", 2);
		assertPlan(new int[] { OBJ_IDX, SUBJ_IDX }, false, true, false, false, "posc", 3);
		assertPlan(new int[] { PRED_IDX }, true, false, false, false, "spoc", 2);
		assertPlan(new int[] { SUBJ_IDX }, false, true, true, false, "posc", 3);
		// a bound context may follow the prefix
		assertPlan(new int[] { PRED_IDX }, false, false, false, true, "posc", 1);
		assertPlan(new int[] { PRED_IDX, OBJ_IDX }, false, false, false, true, "posc", 2);
		assertPlan(new int[] { OBJ_IDX }, true, true, false, false, "spoc", 3);

		// prefix fields interleaved with unbound non-prefix fields, or bound non-context fields after the prefix
		assertThat(tripleStore.prefixRunPlan(new int[] { OBJ_IDX }, false, false, false, false)).isNull();
		assertThat(tripleStore.prefixRunPlan(new int[] { SUBJ_IDX, OBJ_IDX }, false, false, false, false)).isNull();
		assertThat(tripleStore.prefixRunPlan(new int[] { SUBJ_IDX }, false, true, false, false)).isNull();
		assertThat(tripleStore.prefixRunPlan(new int[] { PRED_IDX }, false, false, true, false)).isNull();
		assertThat(tripleStore.prefixRunPlan(new int[] { CONTEXT_IDX }, false, false, false, false)).isNull();
		assertThat(tripleStore.prefixRunPlan(new int[] { OBJ_IDX }, true, false, false, false)).isNull();
		// a prefix field cannot be bound, and prefixes are limited to three fields
		assertThat(tripleStore.prefixRunPlan(new int[] { PRED_IDX }, false, true, false, false)).isNull();
		assertThat(tripleStore.prefixRunPlan(new int[] { PRED_IDX, PRED_IDX }, false, false, false, false)).isNull();
		assertThat(tripleStore.prefixRunPlan(new int[] { SUBJ_IDX, PRED_IDX, OBJ_IDX, CONTEXT_IDX }, false, false,
				false, false)).isNull();
		assertThat(tripleStore.prefixRunPlan(new int[0], false, false, false, false)).isNull();
		assertThat(tripleStore.prefixRunPlan(null, false, false, false, false)).isNull();
	}

	@Test
	public void contextFirstIndexSupportsDistinctContexts() throws Exception {
		open("spoc,posc,cspo");

		assertPlan(new int[] { CONTEXT_IDX }, false, false, false, false, "cspo", 1);
		assertPlan(new int[] { CONTEXT_IDX, SUBJ_IDX }, false, false, false, false, "cspo", 2);
		// a bound context after the prefix keeps the shorter spoc prefix preferable to the context-first index
		assertPlan(new int[] { SUBJ_IDX }, false, false, false, true, "spoc", 1);
		assertPlan(new int[] { PRED_IDX }, true, false, false, true, "spoc", 2);
	}

	@Test
	public void disabledPropertyDisablesPlanning() throws Exception {
		open("spoc,posc");
		System.setProperty(LmdbPrefixRunPlan.ENABLED_PROPERTY, "false");

		assertThat(tripleStore.prefixRunPlan(new int[] { PRED_IDX }, false, false, false, false)).isNull();
		assertThat(tripleStore.prefixRunPlan(new int[] { PRED_IDX }, -1, -1, -1, -1)).isNull();
	}

	@Test
	public void idBasedPlanningTreatsNonPositiveIdsAsUnbound() throws Exception {
		open("spoc,posc");

		LmdbPrefixRunPlan plan = tripleStore.prefixRunPlan(new int[] { OBJ_IDX }, -1, 7, -1, -1);
		assertThat(plan).isNotNull();
		assertThat(plan.index().toString()).isEqualTo("posc");
		assertThat(plan.prefixLength()).isEqualTo(2);
		assertThat(plan.prefixFields()).containsExactly(OBJ_IDX);
		// the null context (id 0) is a bound context
		assertThat(tripleStore.prefixRunPlan(new int[] { PRED_IDX }, -1, -1, -1, 0).prefixLength()).isEqualTo(1);
		assertThat(tripleStore.prefixRunPlan(new int[] { SUBJ_IDX }, 0, -1, -1, -1)).isNotNull();
	}

	/*-----------------------*
	 * Successor key builder *
	 *-----------------------*/

	@Test
	public void successorIncrementsDeepestUnboundPrefixFieldAndResetsTheRest() {
		char[] posc = "posc".toCharArray();
		long[] target = new long[4];

		// distinct predicate: the successor of every key with predicate 10 is (11, 0, 0, 0) in key order
		long[] quad = quad(100, 10, 1000, 1);
		assertThat(LmdbPrefixRunIterator.successorKey(posc, 1, quad, bound(-1, -1, -1, -1), target)).isTrue();
		assertThat(target).containsExactly(0L, 11L, 0L, 0L);

		// distinct object with bound predicate: (10, 1001, 0, 0)
		assertThat(LmdbPrefixRunIterator.successorKey(posc, 2, quad, bound(-1, 10, -1, -1), target)).isTrue();
		assertThat(target).containsExactly(0L, 10L, 1001L, 0L);

		// distinct (predicate, object) with a bound context: deeper fields reset to the bound context
		assertThat(LmdbPrefixRunIterator.successorKey(posc, 2, quad, bound(-1, -1, -1, 7), target)).isTrue();
		assertThat(target).containsExactly(0L, 10L, 1001L, 7L);

		// three-field prefix increments the subject and keeps predicate and object
		assertThat(LmdbPrefixRunIterator.successorKey(posc, 3, quad, bound(-1, -1, -1, -1), target)).isTrue();
		assertThat(target).containsExactly(101L, 10L, 1000L, 0L);
	}

	@Test
	public void successorCarriesIntoShallowerFieldAtMaximumId() {
		char[] posc = "posc".toCharArray();
		long[] target = new long[4];

		// the object is at the maximum id: carry into the predicate
		long[] quad = quad(100, 10, Long.MAX_VALUE, 1);
		assertThat(LmdbPrefixRunIterator.successorKey(posc, 2, quad, bound(-1, -1, -1, -1), target)).isTrue();
		assertThat(target).containsExactly(0L, 11L, 0L, 0L);

		// the carry skips bound fields
		long[] maxSubject = quad(Long.MAX_VALUE, 10, 1000, 1);
		assertThat(LmdbPrefixRunIterator.successorKey(posc, 3, maxSubject, bound(-1, -1, 1000, -1), target))
				.isTrue();
		assertThat(target).containsExactly(0L, 11L, 1000L, 0L);

		// every unbound prefix field at the maximum: no later run can exist
		long[] allMax = quad(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, 1);
		assertThat(LmdbPrefixRunIterator.successorKey(posc, 2, allMax, bound(-1, -1, -1, -1), target)).isFalse();
		assertThat(LmdbPrefixRunIterator.successorKey(posc, 2, quad, bound(-1, 10, -1, -1), target)).isFalse();
	}

	/*---------*
	 * Helpers *
	 *---------*/

	private void assertPlan(int[] prefixFields, boolean subjBound, boolean predBound, boolean objBound,
			boolean contextBound, String index, int prefixLength) {
		LmdbPrefixRunPlan plan = tripleStore.prefixRunPlan(prefixFields, subjBound, predBound, objBound,
				contextBound);
		assertThat(plan).as("plan for %s", Arrays.toString(prefixFields)).isNotNull();
		assertThat(plan.index().toString()).as("index for %s", Arrays.toString(prefixFields)).isEqualTo(index);
		assertThat(plan.prefixLength()).as("prefix length for %s", Arrays.toString(prefixFields))
				.isEqualTo(prefixLength);
	}

	private static long[] quad(long subj, long pred, long obj, long context) {
		return new long[] { subj, pred, obj, context };
	}

	private static long[] bound(long subj, long pred, long obj, long context) {
		return new long[] { subj, pred, obj, context };
	}

	private void storeDefaultRows() throws Exception {
		tripleStore.startTransaction();
		tripleStore.storeTriple(100, 10, 1000, 1, true);
		tripleStore.storeTriple(101, 10, 1000, 1, true);
		tripleStore.storeTriple(102, 10, 1001, 1, true);
		tripleStore.storeTriple(200, 20, 2000, 1, true);
		tripleStore.storeTriple(201, 20, 2000, 1, true);
		tripleStore.storeTriple(300, 30, 3000, 1, true);
		tripleStore.commit();
	}

	private ScanResult scan(int[] prefixFields, long subj, long pred, long obj, long context, boolean countRunRows)
			throws Exception {
		LmdbPrefixRunPlan plan = tripleStore.prefixRunPlan(prefixFields, subj, pred, obj, context);
		assertThat(plan).as("plan for %s", Arrays.toString(prefixFields)).isNotNull();
		try (Txn txn = tripleStore.getTxnManager().createReadTxn();
				LmdbPrefixRunIterator cursor = tripleStore.getPrefixRuns(txn, plan, subj, pred, obj, context, true,
						countRunRows)) {
			List<long[]> rows = new ArrayList<>();
			List<Long> runRowCounts = new ArrayList<>();
			while (cursor.next()) {
				rows.add(Arrays.copyOf(cursor.quad(), 4));
				runRowCounts.add(cursor.runRowCount());
			}
			assertThat(cursor.next()).isFalse();
			return new ScanResult(plan, rows, runRowCounts, cursor.getSourceRowsScannedActual());
		}
	}

	/** Distinct prefix combinations (in index key order of the plan) and their row counts, computed by a full scan. */
	private Map<String, Long> fullScan(int[] fields, long[] pattern) throws Exception {
		LmdbPrefixRunPlan plan = tripleStore.prefixRunPlan(fields, pattern[0], pattern[1], pattern[2], pattern[3]);
		char[] fieldSeq = plan.index().getFieldSeq();
		Set<Integer> fieldSet = new LinkedHashSet<>();
		for (int field : fields) {
			fieldSet.add(field);
		}
		// key order of the prefix fields
		List<Integer> ordered = new ArrayList<>();
		for (char c : fieldSeq) {
			int field = LmdbPrefixRunIterator.fieldIndex(c);
			if (fieldSet.contains(field)) {
				ordered.add(field);
			}
		}
		int[] orderedFields = ordered.stream().mapToInt(Integer::intValue).toArray();
		Map<String, Long> counts = new LinkedHashMap<>();
		List<long[]> rows = new ArrayList<>();
		try (Txn txn = tripleStore.getTxnManager().createReadTxn();
				RecordIterator records = tripleStore.getTriples(txn, pattern[0], pattern[1], pattern[2], pattern[3],
						true)) {
			long[] row;
			while ((row = records.next()) != null) {
				rows.add(Arrays.copyOf(row, 4));
			}
		}
		rows.sort((a, b) -> {
			for (int field : orderedFields) {
				int compared = Long.compare(a[field], b[field]);
				if (compared != 0) {
					return compared;
				}
			}
			return 0;
		});
		for (long[] row : rows) {
			counts.merge(key(row, orderedFields), 1L, Long::sum);
		}
		return counts;
	}

	private static String key(long[] row, int[] orderedFields) {
		StringBuilder sb = new StringBuilder();
		for (int field : orderedFields) {
			sb.append(row[field]).append('/');
		}
		return sb.toString();
	}

	private final class ScanResult {
		private final LmdbPrefixRunPlan plan;
		private final List<long[]> rows;
		private final List<Long> runRowCounts;
		private final long rowsScanned;

		private ScanResult(LmdbPrefixRunPlan plan, List<long[]> rows, List<Long> runRowCounts, long rowsScanned) {
			this.plan = plan;
			this.rows = rows;
			this.runRowCounts = runRowCounts;
			this.rowsScanned = rowsScanned;
		}

		private List<Long> values(int field) {
			return rows.stream().map(row -> row[field]).toList();
		}

		private List<String> pairs(int first, int second) {
			return rows.stream().map(row -> row[first] + "/" + row[second]).toList();
		}

		private List<String> triples(int first, int second, int third) {
			return rows.stream().map(row -> row[first] + "/" + row[second] + "/" + row[third]).toList();
		}

		/** Prefix keys in index key order, comparable with {@link #fullScan}. */
		private List<String> keys(int[] fields) {
			char[] fieldSeq = plan.index().getFieldSeq();
			List<Integer> ordered = new ArrayList<>();
			for (char c : fieldSeq) {
				int field = LmdbPrefixRunIterator.fieldIndex(c);
				for (int f : fields) {
					if (f == field) {
						ordered.add(field);
					}
				}
			}
			int[] orderedFields = ordered.stream().mapToInt(Integer::intValue).toArray();
			return rows.stream().map(row -> key(row, orderedFields)).toList();
		}
	}
}
