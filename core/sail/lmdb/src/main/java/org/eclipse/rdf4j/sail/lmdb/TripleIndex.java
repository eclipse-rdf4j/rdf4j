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

import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.openDatabaseWithTxn;
import static org.lwjgl.util.lmdb.LMDB.MDB_CREATE;
import static org.lwjgl.util.lmdb.LMDB.MDB_DUPFIXED;
import static org.lwjgl.util.lmdb.LMDB.MDB_DUPSORT;
import static org.lwjgl.util.lmdb.LMDB.mdb_dbi_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_drop;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.util.GroupMatcher;
import org.eclipse.rdf4j.sail.lmdb.util.IndexKeyReaders;
import org.eclipse.rdf4j.sail.lmdb.util.IndexKeyWriters;

class TripleIndex implements TripleStore.DupIndex {
	static final int MAX_KEY_LENGTH = 4 * 9;

	// triples are represented by 4 varints for subject, predicate, object and context
	static final int SUBJ_IDX = 0;
	static final int PRED_IDX = 1;
	static final int OBJ_IDX = 2;
	static final int CONTEXT_IDX = 3;

	@FunctionalInterface
	interface StatementFieldValueAccessor {
		long get(long[] subj, long[] pred, long[] obj, long[] context, int statementIndex);
	}

	private final char[] fieldSeq;
	private final IndexKeyWriters.KeyWriter keyWriter;
	private final IndexKeyReaders.KeyToQuadReader keyReader;
	private final IndexKeyWriters.MatcherFactory matcherFactory;
	final StatementFieldValueAccessor[] fieldValueAccessors;
	final StatementFieldValueAccessor leadingFieldValueAccessor;
	private final int dbiExplicit, dbiInferred;
	private final boolean dupsortEnabled;
	private final int dbiDupExplicit;
	private final int dbiDupInferred;
	private final int[] indexMap;
	private final PatternScoreFunction patternScoreFunction;
	private final long env;
	private String name;

	TripleIndex(String name, String fieldSeq, boolean createInferredIndex, long env, long writeTxn) throws IOException {
		this(name, fieldSeq, createInferredIndex, env, writeTxn, false);
	}

	TripleIndex(String name, String fieldSeq, boolean createInferredIndex, long env, long writeTxn,
			boolean dupsortEnabled) throws IOException {
		this.name = name;
		this.fieldSeq = fieldSeq.toCharArray();
		this.keyWriter = IndexKeyWriters.forFieldSeq(fieldSeq);
		this.keyReader = IndexKeyReaders.forFieldSeq(fieldSeq);
		this.matcherFactory = IndexKeyWriters.matcherFactory(fieldSeq);
		this.fieldValueAccessors = createFieldValueAccessors(this.fieldSeq);
		this.leadingFieldValueAccessor = this.fieldValueAccessors[0];
		this.indexMap = getIndexes(this.fieldSeq);
		this.patternScoreFunction = PatternScoreFunctions.forFieldSeq(fieldSeq);
		this.env = env;
		// open database and use native sort order without comparator
		dbiExplicit = openDatabaseWithTxn(writeTxn, getName(true), MDB_CREATE);
		if (createInferredIndex) {
			dbiInferred = openDatabaseWithTxn(writeTxn, getName(false), MDB_CREATE);
		} else {
			dbiInferred = -1;
		}
		this.dupsortEnabled = dupsortEnabled;
		if (dupsortEnabled) {
			int flags = MDB_CREATE | MDB_DUPSORT | MDB_DUPFIXED;
			dbiDupExplicit = openDatabaseWithTxn(writeTxn, getDupName(true), flags);
			dbiDupInferred = createInferredIndex ? openDatabaseWithTxn(writeTxn, getDupName(false), flags) : -1;
		} else {
			dbiDupExplicit = -1;
			dbiDupInferred = -1;
		}
	}

	public char[] getFieldSeq() {
		return fieldSeq;
	}

	String getName(boolean explicit) {
		return name + (explicit ? name : name + "-inf");
	}

	private String getDupName(boolean explicit) {
		return getName(explicit) + "-dup";
	}

	int getDB(boolean explicit) {
		return explicit ? dbiExplicit : dbiInferred;
	}

	boolean isDupsortEnabled() {
		return dupsortEnabled;
	}

	public int getDupDB(boolean explicit) {
		return explicit ? dbiDupExplicit : dbiDupInferred;
	}

	private StatementFieldValueAccessor[] createFieldValueAccessors(char[] fieldSeq) {
		StatementFieldValueAccessor[] accessors = new StatementFieldValueAccessor[fieldSeq.length];
		for (int i = 0; i < fieldSeq.length; i++) {
			accessors[i] = getFieldValueAccessor(fieldSeq[i]);
		}
		return accessors;
	}

	/**
	 * Parses a comma/whitespace-separated list of index specifications. Index specifications are required to consists
	 * of 4 characters: 's', 'p', 'o' and 'c'.
	 *
	 * @param indexSpecStr A string like "spoc, pocs, cosp".
	 * @return A Set containing the parsed index specifications.
	 */
	static Set<String> parseIndexSpecList(String indexSpecStr) throws SailException {
		Set<String> indexes = new LinkedHashSet<>();
		if (indexSpecStr != null && !indexSpecStr.isEmpty()) {
			StringTokenizer tok = new StringTokenizer(indexSpecStr, ", \t");
			while (tok.hasMoreTokens()) {
				String index = tok.nextToken().toLowerCase();

				// sanity checks
				if (index.length() != 4 || index.indexOf('s') == -1 || index.indexOf('p') == -1
						|| index.indexOf('o') == -1 || index.indexOf('c') == -1) {
					throw new SailException("invalid value '" + index + "' in index specification: " + indexSpecStr);
				}

				indexes.add(index);
			}
		}
		return indexes;
	}

	private StatementFieldValueAccessor getFieldValueAccessor(char field) {
		switch (field) {
		case 's':
			return (subj, pred, obj, context, statementIndex) -> subj[statementIndex];
		case 'p':
			return (subj, pred, obj, context, statementIndex) -> pred[statementIndex];
		case 'o':
			return (subj, pred, obj, context, statementIndex) -> obj[statementIndex];
		case 'c':
			return (subj, pred, obj, context, statementIndex) -> context[statementIndex];
		default:
			throw new IllegalArgumentException("Unknown index field: " + field);
		}
	}

	protected int[] getIndexes(char[] fieldSeq) {
		int[] indexes = new int[fieldSeq.length];
		for (int i = 0; i < fieldSeq.length; i++) {
			char field = fieldSeq[i];
			int fieldIdx = switch (field) {
			case 's' -> SUBJ_IDX;
			case 'p' -> PRED_IDX;
			case 'o' -> OBJ_IDX;
			case 'c' -> CONTEXT_IDX;
			default -> throw new IllegalArgumentException(
					"invalid character '" + field + "' in field sequence: " + new String(fieldSeq));
			};
			indexes[i] = fieldIdx;
		}
		return indexes;
	}

	/**
	 * Determines the 'score' of this index on the supplied pattern of subject, predicate, object and context IDs. The
	 * higher the score, the better the index is suited for matching the pattern. Lowest score is 0, which means that
	 * the index will perform a sequential scan.
	 */
	public int getPatternScore(long subj, long pred, long obj, long context) {
		return patternScoreFunction.score(subj, pred, obj, context);
	}

	TripleStore.KeyBuilder keyBuilder(long subj, long pred, long obj, long context) {
		return new TripleStore.KeyBuilder() {

			@Override
			public void writeMin(ByteBuffer buffer) {
				getMinKey(buffer, subj, pred, obj, context, TripleStore.NO_PREVIOUS_ID, TripleStore.NO_PREVIOUS_ID,
						TripleStore.NO_PREVIOUS_ID, TripleStore.NO_PREVIOUS_ID);
			}

			@Override
			public void writeMax(ByteBuffer buffer) {
				getMaxKey(buffer, subj, pred, obj, context, -1, -1, -1, -1);
			}
		};
	}

	void getMinKey(ByteBuffer bb, long subj, long pred, long obj, long context) {
		getMinKey(bb, subj, pred, obj, context, TripleStore.NO_PREVIOUS_ID, TripleStore.NO_PREVIOUS_ID,
				TripleStore.NO_PREVIOUS_ID, TripleStore.NO_PREVIOUS_ID);
	}

	void getMinKey(ByteBuffer bb, long subj, long pred, long obj, long context, long prevSubj, long prevPred,
			long prevObj, long prevContext) {
		subj = subj <= 0 ? 0 : subj;
		pred = pred <= 0 ? 0 : pred;
		obj = obj <= 0 ? 0 : obj;
		context = context <= 0 ? 0 : context;
		long prevSubjNorm = prevSubj == TripleStore.NO_PREVIOUS_ID ? TripleStore.NO_PREVIOUS_ID
				: (prevSubj <= 0 ? 0 : prevSubj);
		long prevPredNorm = prevPred == TripleStore.NO_PREVIOUS_ID ? TripleStore.NO_PREVIOUS_ID
				: (prevPred <= 0 ? 0 : prevPred);
		long prevObjNorm = prevObj == TripleStore.NO_PREVIOUS_ID ? TripleStore.NO_PREVIOUS_ID
				: (prevObj <= 0 ? 0 : prevObj);
		long prevContextNorm = prevContext == TripleStore.NO_PREVIOUS_ID ? TripleStore.NO_PREVIOUS_ID
				: (prevContext <= 0 ? 0 : prevContext);
		toKey(bb, subj, pred, obj, context, prevSubjNorm, prevPredNorm, prevObjNorm, prevContextNorm);
	}

	void getMaxKey(ByteBuffer bb, long subj, long pred, long obj, long context) {
		getMaxKey(bb, subj, pred, obj, context, -1, -1, -1, -1);
	}

	void getMaxKey(ByteBuffer bb, long subj, long pred, long obj, long context, long prevSubj, long prevPred,
			long prevObj, long prevContext) {
		subj = subj <= 0 ? Long.MAX_VALUE : subj;
		pred = pred <= 0 ? Long.MAX_VALUE : pred;
		obj = obj <= 0 ? Long.MAX_VALUE : obj;
		context = context < 0 ? Long.MAX_VALUE : context;
		long prevSubjNorm = prevSubj == TripleStore.NO_PREVIOUS_ID ? TripleStore.NO_PREVIOUS_ID
				: (prevSubj <= 0 ? Long.MAX_VALUE : prevSubj);
		long prevPredNorm = prevPred == TripleStore.NO_PREVIOUS_ID ? TripleStore.NO_PREVIOUS_ID
				: (prevPred <= 0 ? Long.MAX_VALUE : prevPred);
		long prevObjNorm = prevObj == TripleStore.NO_PREVIOUS_ID ? TripleStore.NO_PREVIOUS_ID
				: (prevObj <= 0 ? Long.MAX_VALUE : prevObj);
		long prevContextNorm = prevContext == TripleStore.NO_PREVIOUS_ID ? TripleStore.NO_PREVIOUS_ID
				: (prevContext <= 0 ? Long.MAX_VALUE : prevContext);
		toKey(bb, subj, pred, obj, context, prevSubjNorm, prevPredNorm, prevObjNorm, prevContextNorm);
	}

	GroupMatcher createMatcher(long subj, long pred, long obj, long context) {
		int length = getLength(subj, pred, obj, context);

		ByteBuffer bb = ByteBuffer.allocate(length);
		toKey(bb, subj == -1 ? 0 : subj, pred == -1 ? 0 : pred, obj == -1 ? 0 : obj, context == -1 ? 0 : context);
		bb.flip();

		return new GroupMatcher(bb.array(), matcherFactory.create(subj, pred, obj, context));
	}

	private int getLength(long subj, long pred, long obj, long context) {
		int length = 4;
		if (subj > 240) {
			length += 8;
		}
		if (pred > 240) {
			length += 8;

		}
		if (obj > 240) {
			length += 8;

		}
		if (context > 240) {
			length += 8;

		}
		return length;
	}

	public void toDupKeyPrefix(ByteBuffer bb, long subj, long pred, long obj, long context) {
		for (int i = 0; i < 2; i++) {
			switch (fieldSeq[i]) {
			case 's':
				Varint.writeUnsigned(bb, subj);
				break;
			case 'p':
				Varint.writeUnsigned(bb, pred);
				break;
			case 'o':
				Varint.writeUnsigned(bb, obj);
				break;
			case 'c':
				Varint.writeUnsigned(bb, context);
				break;
			default:
				throw new IllegalArgumentException("Unknown index field: " + fieldSeq[i]);
			}
		}
	}

	void toDupValue(ByteBuffer bb, long subj, long pred, long obj, long context) {
		bb.putLong(getFieldValue(fieldSeq[2], subj, pred, obj, context));
		bb.putLong(getFieldValue(fieldSeq[3], subj, pred, obj, context));
	}

	private long getFieldValue(char field, long subj, long pred, long obj, long context) {
		return switch (field) {
		case 's' -> subj;
		case 'p' -> pred;
		case 'o' -> obj;
		case 'c' -> context;
		default -> throw new IllegalArgumentException("Unknown index field: " + field);
		};
	}

	void toKey(ByteBuffer bb, long subj, long pred, long obj, long context) {
		toKey(bb, subj, pred, obj, context, TripleStore.NO_PREVIOUS_ID, TripleStore.NO_PREVIOUS_ID,
				TripleStore.NO_PREVIOUS_ID, TripleStore.NO_PREVIOUS_ID);
	}

	void toKey(ByteBuffer bb, long subj, long pred, long obj, long context, long prevSubj, long prevPred, long prevObj,
			long prevContext) {
		boolean shouldCache = threeOfFourAreZeroOrMax(subj, pred, obj, context);
		if (shouldCache) {
			long sum = subj + pred + obj + context;
			if (sum == 0 && subj == pred && obj == context) {
				bb.put(Varint.ALL_ZERO_QUAD);
				return;
			}

			if (sum < 241) { // keys with sum < 241 only need 4 bytes to write and don't need caching
				shouldCache = false;
			}
		}

		// Pass through to the keyWriter with caching hint
		boolean hasPrev = prevSubj != TripleStore.NO_PREVIOUS_ID;
		keyWriter.write(bb, subj, pred, obj, context, shouldCache, hasPrev, prevSubj, prevPred, prevObj, prevContext);
	}

	void keyToQuad(ByteBuffer key, long[] quad) {
		Varint.readQuadUnsigned(key, indexMap, quad);
	}

	void keyToQuad(ByteBuffer key, long subj, long pred, long obj, long context, long[] quad) {
		keyReader.read(key, subj, pred, obj, context, quad);
	}

	@Override
	public String toString() {
		return new String(getFieldSeq());
	}

	void close() {
		mdb_dbi_close(env, dbiExplicit);
		if (dbiInferred != -1) {
			mdb_dbi_close(env, dbiInferred);
		}
		if (dbiDupExplicit != -1) {
			mdb_dbi_close(env, dbiDupExplicit);
		}
		if (dbiDupInferred != -1) {
			mdb_dbi_close(env, dbiDupInferred);
		}
	}

	void clear(long txn) {
		mdb_drop(txn, dbiExplicit, false);
		if (dbiInferred != -1) {
			mdb_drop(txn, dbiInferred, false);
		}
		if (dbiDupExplicit != -1) {
			mdb_drop(txn, dbiDupExplicit, false);
		}
		if (dbiDupInferred != -1) {
			mdb_drop(txn, dbiDupInferred, false);
		}
	}

	void destroy(long txn) {
		mdb_drop(txn, dbiExplicit, true);
		if (dbiInferred != -1) {
			mdb_drop(txn, dbiInferred, true);
		}
		if (dbiDupExplicit != -1) {
			mdb_drop(txn, dbiDupExplicit, true);
		}
		if (dbiDupInferred != -1) {
			mdb_drop(txn, dbiDupInferred, true);
		}
	}

	static TripleIndex getBestIndex(List<TripleIndex> indexes, long subj, long pred, long obj, long context) {
		int bestScore = -1;
		TripleIndex bestIndex = null;

		for (TripleIndex index : indexes) {
			int score = index.getPatternScore(subj, pred, obj, context);
			if (score > bestScore) {
				bestScore = score;
				bestIndex = index;
			}
		}

		return bestIndex;
	}

	static boolean threeOfFourAreZeroOrMax(long subj, long pred, long obj, long context) {
		// Precompute the 8 equalities once (cheapest operations here)
		boolean zS = subj == 0L, zP = pred == 0L, zO = obj == 0L, zC = context == 0L;
		boolean mS = subj == Long.MAX_VALUE, mP = pred == Long.MAX_VALUE, mO = obj == Long.MAX_VALUE,
				mC = context == Long.MAX_VALUE;

		// ≥3-of-4 ≡ ab(c∨d) ∨ cd(a∨b). Apply once for zeros and once for maxes.
		// Using '&' and '|' (not &&/||) keeps it branchless and predictable.

		return (((zS & zP & (zO | zC)) | (zO & zC & (zS | zP)))// ≥3 zeros
				| ((mS & mP & (mO | mC)) | (mO & mC & (mS | mP))));// ≥3 Long.MAX_VALUE
//				& !(zS & zP & zO & zC)    // not all zeros
//				& !(mS & mP & mO & mC);   // not all max
	}

	static Set<String> orderIndexSpecs(Set<String> indexSpecs) {
		if (indexSpecs.size() < 3) {
			return new LinkedHashSet<>(indexSpecs);
		}

		List<String> orderedSpecs = new ArrayList<>(indexSpecs);
		String mainFieldSeq = orderedSpecs.getFirst();
		List<String> secondarySpecs = new ArrayList<>(orderedSpecs.subList(1, orderedSpecs.size()));
		OrderScore bestSecondaryOrder = findBestSecondaryIndexOrder(mainFieldSeq, secondarySpecs);

		LinkedHashSet<String> reorderedSpecs = new LinkedHashSet<>();
		reorderedSpecs.add(mainFieldSeq);
		reorderedSpecs.addAll(bestSecondaryOrder.indexOrder);

		return reorderedSpecs;
	}

	private static OrderScore findBestSecondaryIndexOrder(String mainFieldSeq, List<String> secondaryIndexSpecs) {
		List<String> sortedSecondarySpecs = new ArrayList<>(secondaryIndexSpecs);
		Collections.sort(sortedSecondarySpecs);

		char[][] fieldSeqs = new char[sortedSecondarySpecs.size() + 1][];
		fieldSeqs[0] = mainFieldSeq.toCharArray();
		for (int i = 0; i < sortedSecondarySpecs.size(); i++) {
			fieldSeqs[i + 1] = sortedSecondarySpecs.get(i).toCharArray();
		}

		boolean[][] reusesCurrentOrder = new boolean[fieldSeqs.length][sortedSecondarySpecs.size()];
		boolean[][] reusesMainOrder = new boolean[fieldSeqs.length][sortedSecondarySpecs.size()];
		for (int currentIndex = 0; currentIndex < fieldSeqs.length; currentIndex++) {
			for (int candidateIndex = 0; candidateIndex < sortedSecondarySpecs.size(); candidateIndex++) {
				char[] candidateFieldSeq = fieldSeqs[candidateIndex + 1];
				reusesCurrentOrder[currentIndex][candidateIndex] = canReuseCurrentOrder(fieldSeqs[currentIndex],
						candidateFieldSeq);
				reusesMainOrder[currentIndex][candidateIndex] = shouldResetToMainIndexOrder(fieldSeqs[0],
						fieldSeqs[currentIndex], candidateFieldSeq);
			}
		}

		long remainingMask = (1L << sortedSecondarySpecs.size()) - 1;
		for (int nonReusableTransitions = 0; nonReusableTransitions <= sortedSecondarySpecs
				.size(); nonReusableTransitions++) {
			Map<Long, Integer> memoizedScores = new HashMap<>();
			int maxMainOrderResets = findMaxMainOrderResets(0, remainingMask, nonReusableTransitions,
					reusesCurrentOrder, reusesMainOrder, memoizedScores);
			if (maxMainOrderResets >= 0) {
				List<String> bestOrder = reconstructBestSecondaryIndexOrder(0, remainingMask, nonReusableTransitions,
						maxMainOrderResets, sortedSecondarySpecs, reusesCurrentOrder, reusesMainOrder, memoizedScores);
				return new OrderScore(bestOrder, sortedSecondarySpecs.size() - nonReusableTransitions,
						maxMainOrderResets);
			}
		}

		return OrderScore.EMPTY;
	}

	private static int findMaxMainOrderResets(int currentIndex, long remainingMask, int nonReusableTransitionsRemaining,
			boolean[][] reusesCurrentOrder, boolean[][] reusesMainOrder, Map<Long, Integer> memoizedScores) {
		if (remainingMask == 0) {
			return 0;
		}

		long memoKey = (remainingMask << 10) | ((long) currentIndex << 5) | nonReusableTransitionsRemaining;
		Integer memoizedScore = memoizedScores.get(memoKey);
		if (memoizedScore != null) {
			return memoizedScore;
		}

		int bestScore = -1;
		for (int candidateIndex = 0; candidateIndex < reusesCurrentOrder[currentIndex].length; candidateIndex++) {
			long candidateMask = 1L << candidateIndex;
			if ((remainingMask & candidateMask) == 0) {
				continue;
			}

			int transitionCost = reusesCurrentOrder[currentIndex][candidateIndex] ? 0 : 1;
			if (transitionCost > nonReusableTransitionsRemaining) {
				continue;
			}

			int tailScore = findMaxMainOrderResets(candidateIndex + 1, remainingMask ^ candidateMask,
					nonReusableTransitionsRemaining - transitionCost, reusesCurrentOrder, reusesMainOrder,
					memoizedScores);
			if (tailScore < 0) {
				continue;
			}

			bestScore = Math.max(bestScore,
					tailScore + (reusesMainOrder[currentIndex][candidateIndex] ? 1 : 0));
		}

		memoizedScores.put(memoKey, bestScore);
		return bestScore;
	}

	private static List<String> reconstructBestSecondaryIndexOrder(int currentIndex, long remainingMask,
			int nonReusableTransitionsRemaining, int mainOrderResetsRemaining, List<String> secondaryIndexSpecs,
			boolean[][] reusesCurrentOrder, boolean[][] reusesMainOrder, Map<Long, Integer> memoizedScores) {
		if (remainingMask == 0) {
			return List.of();
		}

		for (int candidateIndex = 0; candidateIndex < secondaryIndexSpecs.size(); candidateIndex++) {
			long candidateMask = 1L << candidateIndex;
			if ((remainingMask & candidateMask) == 0) {
				continue;
			}

			int transitionCost = reusesCurrentOrder[currentIndex][candidateIndex] ? 0 : 1;
			if (transitionCost > nonReusableTransitionsRemaining) {
				continue;
			}

			int tailScore = findMaxMainOrderResets(candidateIndex + 1, remainingMask ^ candidateMask,
					nonReusableTransitionsRemaining - transitionCost, reusesCurrentOrder, reusesMainOrder,
					memoizedScores);
			if (tailScore < 0) {
				continue;
			}

			int candidateScore = tailScore + (reusesMainOrder[currentIndex][candidateIndex] ? 1 : 0);
			if (candidateScore != mainOrderResetsRemaining) {
				continue;
			}

			List<String> candidateOrder = new ArrayList<>();
			candidateOrder.add(secondaryIndexSpecs.get(candidateIndex));
			candidateOrder.addAll(reconstructBestSecondaryIndexOrder(candidateIndex + 1, remainingMask ^ candidateMask,
					nonReusableTransitionsRemaining - transitionCost, tailScore, secondaryIndexSpecs,
					reusesCurrentOrder, reusesMainOrder, memoizedScores));
			return candidateOrder;
		}

		throw new IllegalStateException("Unable to reconstruct secondary index order");
	}

	static boolean shouldResetToMainIndexOrder(char[] mainFieldSeq, char[] currentFieldSeq, char[] targetFieldSeq) {
		return !canReuseCurrentOrder(currentFieldSeq, targetFieldSeq)
				&& canReuseCurrentOrder(mainFieldSeq, targetFieldSeq);
	}

	private static boolean canReuseCurrentOrder(char[] currentFieldSeq, char[] targetFieldSeq) {
		if (currentFieldSeq.length != targetFieldSeq.length) {
			return false;
		}
		int currentIndex = 0;
		for (int i = 1; i < targetFieldSeq.length; i++) {
			while (currentIndex < currentFieldSeq.length && currentFieldSeq[currentIndex] != targetFieldSeq[i]) {
				if (currentFieldSeq[currentIndex] == targetFieldSeq[0]) {
					currentIndex++;
					continue;
				}
				return false;
			}
			if (currentIndex == currentFieldSeq.length) {
				return false;
			}
			currentIndex++;
		}
		return true;
	}

	record OrderScore(List<String> indexOrder, int reusedTransitions, int mainOrderResets) {

		private static final OrderScore EMPTY = new OrderScore(List.of(), 0, 0);

		@Override
		public String toString() {
			return "OrderScore{" +
					"indexOrder=" + Arrays.toString(indexOrder.toArray()) +
					", reusedTransitions=" + reusedTransitions +
					", mainOrderResets=" + mainOrderResets +
					'}';
		}
	}
}
