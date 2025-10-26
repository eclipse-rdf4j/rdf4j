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
package org.eclipse.rdf4j.sail.lmdb.join;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.sail.lmdb.IdAccessor;
import org.eclipse.rdf4j.sail.lmdb.IdBindingInfo;
import org.eclipse.rdf4j.sail.lmdb.LmdbDatasetContext;
import org.eclipse.rdf4j.sail.lmdb.LmdbEvaluationDataset;
import org.eclipse.rdf4j.sail.lmdb.LmdbEvaluationStrategy;
import org.eclipse.rdf4j.sail.lmdb.RecordIterator;
import org.eclipse.rdf4j.sail.lmdb.ValueStore;
import org.eclipse.rdf4j.sail.lmdb.join.LmdbIdFinalBindingSetIteration;

/**
 * Builds a left-deep chain of ID-only join iterators for an entire BGP and materializes bindings only once.
 */
public final class LmdbIdBGPQueryEvaluationStep implements QueryEvaluationStep {

	private static final String ID_JOIN_ALGORITHM = LmdbIdJoinIterator.class.getSimpleName();

	private final List<StatementPattern> patterns;
	private final QueryEvaluationContext context;
	private final LmdbDatasetContext datasetContext;
	private final Join root;

	public LmdbIdBGPQueryEvaluationStep(Join root, List<StatementPattern> patterns, QueryEvaluationContext context) {
		if (!(context instanceof LmdbDatasetContext)) {
			throw new IllegalArgumentException("LMDB ID BGP join requires LMDB query evaluation context");
		}
		this.root = root;
		this.patterns = patterns;
		this.context = context;
		this.datasetContext = (LmdbDatasetContext) context;
		markJoinTreeWithIdAlgorithm(root);
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet bindings) {
		try {
			LmdbEvaluationDataset dataset = resolveDataset();
			ValueStore valueStore = dataset.getValueStore();

			// Leftmost iterator uses initial bindings (outer input bindings may have Values)
			StatementPattern first = patterns.get(0);
			RecordIterator left = dataset.getRecordIterator(first, bindings);
			LmdbIdJoinIterator.PatternInfo leftInfoPI = LmdbIdJoinIterator.PatternInfo.create(first);
			IdBindingInfo leftInfo = IdBindingInfo.fromFirstPattern(leftInfoPI);

			for (int i = 1; i < patterns.size(); i++) {
				StatementPattern rightPat = patterns.get(i);
				LmdbIdJoinIterator.PatternInfo rightInfo = LmdbIdJoinIterator.PatternInfo.create(rightPat);
				Set<String> shared = sharedVars(leftInfo.getVariableNames(), rightInfo.getVariableNames());
				IdBindingInfo outInfo = IdBindingInfo.combine(leftInfo, rightInfo);

				IdAccessor leftAccessor = (i == 1) ? leftInfoPI : leftInfo;
				final IdAccessor leftAccessorForLambda = leftAccessor;
				final IdBindingInfo outputInfo = outInfo;
				final LmdbIdJoinIterator.PatternInfo rightPatternInfo = rightInfo;
				final CopyPlan leftCopyPlan = CopyPlan.forAccessor(leftAccessor, outInfo);
				final CopyPlan rightCopyPlan = CopyPlan.forRightExclusive(rightPatternInfo, shared, outInfo);

				IdJoinRecordIterator.RightFactory rightFactory = leftRecord -> {
					long[] leftSnapshot = Arrays.copyOf(leftRecord, leftRecord.length);
					RecordIterator rightIter = dataset.getRecordIterator(rightPat,
							varName -> leftAccessorForLambda.getId(leftSnapshot, varName));
					if (rightIter == null) {
						rightIter = LmdbIdJoinIterator.emptyRecordIterator();
					}
					long[] leftSeed = seedLeftContribution(leftSnapshot, leftCopyPlan, outputInfo.size());
					RecordIterator finalRightIter = rightIter;
					return new RecordIterator() {
						@Override
						public long[] next() {
							long[] rightRecord;
							while ((rightRecord = finalRightIter.next()) != null) {
								long[] out = Arrays.copyOf(leftSeed, leftSeed.length);
								appendRightExclusive(out, rightRecord, rightCopyPlan);
								return out;
							}
							return null;
						}

						@Override
						public void close() {
							finalRightIter.close();
						}
					};
				};

				left = new IdJoinRecordIterator(left, rightFactory);
				leftInfo = outInfo;
			}

			return new LmdbIdFinalBindingSetIteration(left, leftInfo, context, bindings, valueStore);
		} catch (QueryEvaluationException e) {
			throw e;
		}
	}

	private Set<String> sharedVars(Set<String> a, Set<String> b) {
		LinkedHashSet<String> s = new LinkedHashSet<>(a);
		s.retainAll(new HashSet<>(b));
		return s;
	}

	private static long[] seedLeftContribution(long[] leftRecord, CopyPlan copyPlan, int outputSize) {
		long[] seed = new long[outputSize];
		copyPlan.copy(leftRecord, seed);
		return seed;
	}

	private static void appendRightExclusive(long[] target, long[] rightRecord, CopyPlan copyPlan) {
		copyPlan.copy(rightRecord, target);
	}

	private static final class CopyPlan {
		private final int[] sourceIndexes;
		private final int[] targetIndexes;

		private CopyPlan(int[] sourceIndexes, int[] targetIndexes) {
			this.sourceIndexes = sourceIndexes;
			this.targetIndexes = targetIndexes;
		}

		static CopyPlan forAccessor(IdAccessor accessor, IdBindingInfo outputInfo) {
			List<Integer> sources = new ArrayList<>();
			List<Integer> targets = new ArrayList<>();
			for (String name : accessor.getVariableNames()) {
				int sourceIdx = accessor.getRecordIndex(name);
				int targetIdx = outputInfo.getIndex(name);
				if (sourceIdx >= 0 && targetIdx >= 0) {
					sources.add(sourceIdx);
					targets.add(targetIdx);
				}
			}
			return new CopyPlan(toIntArray(sources), toIntArray(targets));
		}

		static CopyPlan forRightExclusive(LmdbIdJoinIterator.PatternInfo rightInfo, Set<String> sharedVars,
				IdBindingInfo outputInfo) {
			List<Integer> sources = new ArrayList<>();
			List<Integer> targets = new ArrayList<>();
			for (String name : rightInfo.getVariableNames()) {
				if (sharedVars.contains(name)) {
					continue;
				}
				int sourceIdx = rightInfo.getRecordIndex(name);
				int targetIdx = outputInfo.getIndex(name);
				if (sourceIdx >= 0 && targetIdx >= 0) {
					sources.add(sourceIdx);
					targets.add(targetIdx);
				}
			}
			return new CopyPlan(toIntArray(sources), toIntArray(targets));
		}

		void copy(long[] source, long[] target) {
			for (int i = 0; i < sourceIndexes.length; i++) {
				target[targetIndexes[i]] = source[sourceIndexes[i]];
			}
		}

		private static int[] toIntArray(List<Integer> values) {
			int[] out = new int[values.size()];
			for (int i = 0; i < values.size(); i++) {
				out[i] = values.get(i);
			}
			return out;
		}
	}

	private LmdbEvaluationDataset resolveDataset() {
		// Prefer the dataset supplied by the evaluation context (may be an overlay honoring txn writes)
		java.util.Optional<LmdbEvaluationDataset> fromContext = datasetContext.getLmdbDataset();
		if (fromContext.isPresent()) {
			return fromContext.get();
		}
		// Fall back to the thread-local dataset if the context did not provide one
		return LmdbEvaluationStrategy.getCurrentDataset()
				.orElseThrow(() -> new IllegalStateException("No active LMDB dataset available for join evaluation"));
	}

	public static boolean flattenBGP(TupleExpr expr, List<StatementPattern> out) {
		if (expr instanceof StatementPattern) {
			out.add((StatementPattern) expr);
			return true;
		}
		if (expr instanceof Join) {
			Join j = (Join) expr;
//			// merge joins only; we avoid mergeJoin or other special joins
//			if (j.isMergeJoin()) {
//				return false;
//			}
			return flattenBGP(j.getLeftArg(), out) && flattenBGP(j.getRightArg(), out);
		}
		return false;
	}

	private static void markJoinTreeWithIdAlgorithm(TupleExpr expr) {
		if (expr instanceof Join) {
			Join join = (Join) expr;
			join.setAlgorithm(ID_JOIN_ALGORITHM);
			markJoinTreeWithIdAlgorithm(join.getLeftArg());
			markJoinTreeWithIdAlgorithm(join.getRightArg());
		}
	}
}
