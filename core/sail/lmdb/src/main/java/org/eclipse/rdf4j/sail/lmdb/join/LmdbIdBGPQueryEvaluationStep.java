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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.sail.lmdb.IdBindingInfo;
import org.eclipse.rdf4j.sail.lmdb.LmdbDatasetContext;
import org.eclipse.rdf4j.sail.lmdb.LmdbEvaluationDataset;
import org.eclipse.rdf4j.sail.lmdb.LmdbEvaluationDataset.KeyRangeBuffers;
import org.eclipse.rdf4j.sail.lmdb.LmdbEvaluationStrategy;
import org.eclipse.rdf4j.sail.lmdb.RecordIterator;
import org.eclipse.rdf4j.sail.lmdb.TripleStore;
import org.eclipse.rdf4j.sail.lmdb.ValueStore;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;

/**
 * Builds a left-deep chain of ID-only join iterators for an entire BGP and materializes bindings only once.
 */
public final class LmdbIdBGPQueryEvaluationStep implements QueryEvaluationStep {

	private static final String ID_JOIN_ALGORITHM = LmdbIdJoinIterator.class.getSimpleName();

	private final List<PatternPlan> plans;
	private final List<RawPattern> rawPatterns;
	private final IdBindingInfo finalInfo;
	private final QueryEvaluationContext context;
	private final LmdbDatasetContext datasetContext;
	private final Join root;
	private final QueryEvaluationStep fallbackStep;
	private final boolean hasInvalidPattern;
	private final boolean createdDynamicIds;
	private final boolean allowCreateConstantIds;
	private final Map<String, Long> constantBindings;
	private final List<MergeSpec> mergeSpecs;
	private final Set<Join> mergeJoinNodes;
	private final Map<StatementPattern, KeyRangeBuffers> patternBuffers = new HashMap<>();

	public LmdbIdBGPQueryEvaluationStep(Join root, List<StatementPattern> patterns, QueryEvaluationContext context,
			QueryEvaluationStep fallbackStep) {
		if (!(context instanceof LmdbDatasetContext)) {
			throw new IllegalArgumentException("LMDB ID BGP join requires LMDB query evaluation context");
		}
		this.root = root;
		this.context = context;
		this.datasetContext = (LmdbDatasetContext) context;
		this.fallbackStep = fallbackStep;

		ValueStore valueStore = this.datasetContext.getValueStore()
				.orElseThrow(() -> new IllegalStateException("LMDB ID BGP join requires ValueStore access"));

		Optional<LmdbEvaluationDataset> datasetOpt = this.datasetContext.getLmdbDataset();
		boolean overlayDataset = datasetOpt
				.map(ds -> LmdbEvaluationStrategy.getCurrentDataset().map(current -> current != ds).orElse(true))
				.orElse(false);
		boolean allowCreate = datasetOpt
				.map(ds -> ds.hasTransactionChanges() || overlayDataset)
				.orElseGet(LmdbEvaluationStrategy::hasActiveConnectionChanges);
		this.allowCreateConstantIds = allowCreate;

		List<MergeSpec> mergeSpecs = new ArrayList<>();
		List<StatementPattern> flattened = new ArrayList<>(patterns.size());
		boolean flattenedOk = flattenBGP(root, flattened, mergeSpecs) && !flattened.isEmpty();
		List<StatementPattern> effectivePatterns = flattenedOk ? flattened : patterns;

		List<RawPattern> rawPatterns = new ArrayList<>(effectivePatterns.size());
		boolean invalid = false;
		boolean created = false;
		Map<String, Long> constants = new HashMap<>();
		for (StatementPattern pattern : effectivePatterns) {
			RawPattern raw = RawPattern.create(pattern, valueStore, allowCreate);
			rawPatterns.add(raw);
			invalid |= raw.invalid;
			created |= raw.createdIds;
			constants.putAll(raw.getConstantIds());
		}
		this.hasInvalidPattern = invalid;
		this.createdDynamicIds = created;
		this.constantBindings = Collections.unmodifiableMap(constants);

		if (rawPatterns.isEmpty()) {
			throw new IllegalArgumentException("Basic graph pattern must contain at least one statement pattern");
		}

		IdBindingInfo info = null;
		for (RawPattern raw : rawPatterns) {
			if (info == null) {
				info = IdBindingInfo.fromFirstPattern(raw.patternInfo, context);
			} else {
				info = IdBindingInfo.combine(info, raw.patternInfo, context);
			}
		}
		this.finalInfo = info;

		List<PatternPlan> planList = new ArrayList<>(rawPatterns.size());
		for (int i = 0; i < rawPatterns.size(); i++) {
			planList.add(rawPatterns.get(i).toPlan(finalInfo));
		}
		this.plans = planList;
		this.rawPatterns = rawPatterns;
		this.mergeSpecs = mergeSpecs;
		this.mergeJoinNodes = new HashSet<>();
		for (MergeSpec spec : mergeSpecs) {
			if (spec.join != null) {
				mergeJoinNodes.add(spec.join);
			}
		}
	}

	public boolean shouldUseFallbackImmediately() {
		return hasInvalidPattern && fallbackStep != null && !allowCreateConstantIds;
	}

	public void applyAlgorithmTag() {
		markJoinTreeWithIdAlgorithm(root, mergeJoinNodes);
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet bindings) {
		if (fallbackStep != null && LmdbEvaluationStrategy.hasActiveConnectionChanges()) {
			return fallbackStep.evaluate(bindings);
		}
		try {
			LmdbEvaluationDataset dataset = resolveDataset();
			if (!dataset.hasTransactionChanges()) {
				dataset.refreshSnapshot();
			}
			if (fallbackStep != null && dataset.hasTransactionChanges()) {
				return fallbackStep.evaluate(bindings);
			}
			if (hasInvalidPattern) {
				return fallbackStep != null ? fallbackStep.evaluate(bindings) : new EmptyIteration<>();
			}
			if (!dataset.hasTransactionChanges() && createdDynamicIds && fallbackStep != null
					&& !allowCreateConstantIds) {
				return fallbackStep.evaluate(bindings);
			}

			ValueStore valueStore = dataset.getValueStore();
			long[] initialBinding = finalInfo.createInitialBinding(bindings, valueStore);
			if (initialBinding == null) {
				return new EmptyIteration<>();
			}

			applyIndexHints(dataset, initialBinding);
			List<Stage> stages = buildStages();
			if (stages.isEmpty()) {
				return new EmptyIteration<>();
			}

			RecordIterator iter = stages.get(0).createInitialIterator(dataset, initialBinding, valueStore);
			for (int i = 1; i < stages.size(); i++) {
				iter = stages.get(i).joinWith(iter, dataset, valueStore);
			}

			return new LmdbIdFinalBindingSetIteration(iter, finalInfo, context, bindings, valueStore, constantBindings);
		} catch (QueryEvaluationException e) {
			throw e;
		}
	}

	private LmdbEvaluationDataset resolveDataset() {
		Optional<LmdbEvaluationDataset> fromContext = datasetContext.getLmdbDataset();
		if (fromContext.isPresent()) {
			return fromContext.get();
		}
		return LmdbEvaluationStrategy.getCurrentDataset()
				.orElseThrow(() -> new IllegalStateException("No active LMDB dataset available for join evaluation"));
	}

	public static boolean flattenBGP(TupleExpr expr, List<StatementPattern> out) {
		return flattenBGP(expr, out, null);
	}

	public static boolean flattenBGP(TupleExpr expr, List<StatementPattern> out, List<MergeSpec> merges) {
		if (expr instanceof StatementPattern) {
			out.add((StatementPattern) expr);
			return true;
		}
		if (expr instanceof Join) {
			Join j = (Join) expr;
			int leftStart = out.size();
			boolean left = flattenBGP(j.getLeftArg(), out, merges);
			int leftEnd = out.size();
			boolean right = flattenBGP(j.getRightArg(), out, merges);
			int rightEnd = out.size();
			if (!left || !right) {
				return false;
			}
			if (j.isMergeJoin() && merges != null) {
				if (rightEnd - leftStart != 2 || leftEnd - leftStart != 1) {
					return false;
				}
				Var orderVar = j.getOrder();
				if (orderVar == null) {
					return false;
				}
				merges.add(new MergeSpec(leftStart, leftEnd, rightEnd, orderVar.getName(), j));
			}
			return true;
		}
		return false;
	}

	private static void markJoinTreeWithIdAlgorithm(TupleExpr expr, Set<Join> mergeJoins) {
		if (expr instanceof Join) {
			Join join = (Join) expr;
			if (mergeJoins != null && mergeJoins.contains(join)) {
				join.setAlgorithm(LmdbIdMergeJoinIterator.class.getSimpleName());
			} else {
				join.setAlgorithm(ID_JOIN_ALGORITHM);
			}
			markJoinTreeWithIdAlgorithm(join.getLeftArg(), mergeJoins);
			markJoinTreeWithIdAlgorithm(join.getRightArg(), mergeJoins);
		}
	}

	private List<Stage> buildStages() {
		List<Stage> stages = new ArrayList<>();
		boolean[] consumed = new boolean[plans.size()];
		for (MergeSpec spec : mergeSpecs) {
			int leftIndex = spec.leftIndex;
			int rightIndex = spec.rightIndex();
			if (leftIndex < 0 || rightIndex >= plans.size()) {
				continue;
			}
			RawPattern leftRaw = rawPatterns.get(leftIndex);
			RawPattern rightRaw = rawPatterns.get(rightIndex);
			StatementOrder leftOrder = determineOrder(leftRaw, spec.mergeVariable);
			StatementOrder rightOrder = determineOrder(rightRaw, spec.mergeVariable);
			PatternPlan leftPlan = leftRaw.toPlan(finalInfo, leftOrder);
			PatternPlan rightPlan = rightRaw.toPlan(finalInfo, rightOrder);
			KeyRangeBuffers leftBuffers = keyBuffersFor(leftPlan.pattern);
			KeyRangeBuffers rightBuffers = keyBuffersFor(rightPlan.pattern);
			stages.add(new MergeStage(leftPlan, rightPlan, leftBuffers, rightBuffers, leftRaw.patternInfo,
					rightRaw.patternInfo, spec.mergeVariable));
			consumed[leftIndex] = true;
			consumed[rightIndex] = true;
		}
		for (int i = 0; i < plans.size(); i++) {
			if (!consumed[i]) {
				PatternPlan plan = plans.get(i);
				stages.add(new PatternStage(plan, keyBuffersFor(plan.pattern)));
			}
		}
		return stages;
	}

	private StatementOrder determineOrder(RawPattern pattern, String mergeVariable) {
		if (pattern.pattern.getStatementOrder() != null) {
			return pattern.pattern.getStatementOrder();
		}
		if (mergeVariable == null) {
			return null;
		}
		int mask = pattern.patternInfo.getPositionsMask(mergeVariable);
		if ((mask & (1 << TripleStore.SUBJ_IDX)) != 0) {
			return StatementOrder.S;
		}
		if ((mask & (1 << TripleStore.PRED_IDX)) != 0) {
			return StatementOrder.P;
		}
		if ((mask & (1 << TripleStore.OBJ_IDX)) != 0) {
			return StatementOrder.O;
		}
		if ((mask & (1 << TripleStore.CONTEXT_IDX)) != 0) {
			return StatementOrder.C;
		}
		return null;
	}

	private void applyIndexHints(LmdbEvaluationDataset dataset) {
		applyIndexHints(dataset, null);
	}

	private void applyIndexHints(LmdbEvaluationDataset dataset, long[] binding) {
		for (int i = 0; i < rawPatterns.size(); i++) {
			RawPattern raw = rawPatterns.get(i);
			StatementPattern pattern = raw.pattern;
			pattern.setIndex(null);
			pattern.setIndexName(null);
			if (dataset == null) {
				continue;
			}
			long subj = resolveComponent(raw.patternIds[TripleStore.SUBJ_IDX], plans.get(i).subjIndex, binding);
			long pred = resolveComponent(raw.patternIds[TripleStore.PRED_IDX], plans.get(i).predIndex, binding);
			long obj = resolveComponent(raw.patternIds[TripleStore.OBJ_IDX], plans.get(i).objIndex, binding);
			long ctx = resolveComponent(raw.patternIds[TripleStore.CONTEXT_IDX], plans.get(i).ctxIndex, binding);
			String fieldSeq = dataset.selectBestIndex(subj, pred, obj, ctx);
			if (fieldSeq == null) {
				continue;
			}
			pattern.setIndexName(fieldSeq);
			String enumKey = fieldSeq.toUpperCase(Locale.ROOT);
			try {
				pattern.setIndex(StatementPattern.Index.valueOf(enumKey));
			} catch (IllegalArgumentException ignore) {
				pattern.setIndex(null);
			}
		}
	}

	private KeyRangeBuffers keyBuffersFor(StatementPattern pattern) {
		return patternBuffers.computeIfAbsent(pattern, p -> KeyRangeBuffers.acquire());
	}

	private static long resolveComponent(long constantId, int bindingIndex, long[] binding) {
		if (constantId != LmdbValue.UNKNOWN_ID) {
			return constantId;
		}
		if (binding != null && bindingIndex >= 0 && bindingIndex < binding.length) {
			long fromBinding = binding[bindingIndex];
			if (fromBinding != LmdbValue.UNKNOWN_ID) {
				return fromBinding;
			}
		}
		return LmdbValue.UNKNOWN_ID;
	}

	private interface Stage {
		RecordIterator createInitialIterator(LmdbEvaluationDataset dataset, long[] initialBinding,
				ValueStore valueStore) throws QueryEvaluationException;

		RecordIterator joinWith(RecordIterator left, LmdbEvaluationDataset dataset, ValueStore valueStore)
				throws QueryEvaluationException;
	}

	private static final class PatternStage implements Stage {
		private final PatternPlan plan;
		private final KeyRangeBuffers keyBuffers;
		private long[] bindingScratch;
		private long[] quadScratch;
		private RecordIterator reusableIterator;

		private PatternStage(PatternPlan plan, KeyRangeBuffers keyBuffers) {
			this.plan = plan;
			this.keyBuffers = keyBuffers;
		}

		@Override
		public RecordIterator createInitialIterator(LmdbEvaluationDataset dataset, long[] initialBinding,
				ValueStore valueStore) throws QueryEvaluationException {
			if (bindingScratch == null || bindingScratch.length < initialBinding.length) {
				bindingScratch = new long[initialBinding.length];
			}
			if (quadScratch == null) {
				quadScratch = new long[4];
			}
			RecordIterator iter = dataset.getRecordIterator(initialBinding, plan.subjIndex, plan.predIndex,
					plan.objIndex, plan.ctxIndex, plan.patternIds, keyBuffers, bindingScratch, quadScratch,
					reusableIterator);
			if (iter != null && iter != LmdbIdJoinIterator.emptyRecordIterator()) {
				reusableIterator = iter;
			} else {
				reusableIterator = null;
			}
			return iter == null ? LmdbIdJoinIterator.emptyRecordIterator() : iter;
		}

		@Override
		public RecordIterator joinWith(RecordIterator left, LmdbEvaluationDataset dataset, ValueStore valueStore)
				throws QueryEvaluationException {
			return new BindingJoinRecordIterator(left, dataset, plan, keyBuffers);
		}
	}

	private final class MergeStage implements Stage {
		private final PatternPlan leftPlan;
		private final PatternPlan rightPlan;
		private final KeyRangeBuffers leftKeyBuffers;
		private final KeyRangeBuffers rightKeyBuffers;
		private final LmdbIdJoinIterator.PatternInfo leftInfo;
		private final LmdbIdJoinIterator.PatternInfo rightInfo;
		private final String mergeVariable;
		private long[] sequentialLeftScratch;
		private long[] orderedLeftScratch;
		private long[] orderedRightScratch;
		private long[] sequentialLeftQuadScratch;
		private long[] orderedLeftQuadScratch;
		private long[] orderedRightQuadScratch;
		private RecordIterator sequentialLeftReusable;
		private RecordIterator orderedLeftReusable;
		private RecordIterator orderedRightReusable;

		private MergeStage(PatternPlan leftPlan, PatternPlan rightPlan, KeyRangeBuffers leftKeyBuffers,
				KeyRangeBuffers rightKeyBuffers, LmdbIdJoinIterator.PatternInfo leftInfo,
				LmdbIdJoinIterator.PatternInfo rightInfo, String mergeVariable) {
			this.leftPlan = leftPlan;
			this.rightPlan = rightPlan;
			this.leftKeyBuffers = leftKeyBuffers;
			this.rightKeyBuffers = rightKeyBuffers;
			this.leftInfo = leftInfo;
			this.rightInfo = rightInfo;
			this.mergeVariable = mergeVariable;
		}

		@Override
		public RecordIterator createInitialIterator(LmdbEvaluationDataset dataset, long[] initialBinding,
				ValueStore valueStore) throws QueryEvaluationException {
			RecordIterator merge = createMergeIterator(dataset, initialBinding, valueStore);
			return merge == null ? LmdbIdJoinIterator.emptyRecordIterator() : merge;
		}

		@Override
		public RecordIterator joinWith(RecordIterator left, LmdbEvaluationDataset dataset, ValueStore valueStore)
				throws QueryEvaluationException {
			return new RecordIterator() {
				private final RecordIterator leftIter = left;
				private RecordIterator currentMerge;
				private long[] mergeScratch;

				@Override
				public long[] next() throws QueryEvaluationException {
					while (true) {
						if (currentMerge != null) {
							long[] merged = currentMerge.next();
							if (merged != null) {
								return merged;
							}
							currentMerge.close();
							currentMerge = null;
						}
						long[] leftBinding = leftIter.next();
						if (leftBinding == null) {
							return null;
						}
						if (mergeScratch == null || mergeScratch.length < leftBinding.length) {
							mergeScratch = new long[leftBinding.length];
						}
						System.arraycopy(leftBinding, 0, mergeScratch, 0, leftBinding.length);
						currentMerge = createMergeIterator(dataset, mergeScratch, valueStore);
					}
				}

				@Override
				public void close() {
					if (currentMerge != null) {
						currentMerge.close();
					}
					leftIter.close();
				}
			};
		}

		private RecordIterator createMergeIterator(LmdbEvaluationDataset dataset, long[] binding, ValueStore valueStore)
				throws QueryEvaluationException {
			if (leftPlan.order == null || rightPlan.order == null) {
				return createSequentialIterator(dataset, binding, valueStore);
			}
			if (orderedLeftScratch == null || orderedLeftScratch.length < binding.length) {
				orderedLeftScratch = new long[binding.length];
			}
			if (orderedLeftQuadScratch == null) {
				orderedLeftQuadScratch = new long[4];
			}
			RecordIterator leftIterator = dataset.getOrderedRecordIterator(binding, leftPlan.subjIndex,
					leftPlan.predIndex, leftPlan.objIndex, leftPlan.ctxIndex, leftPlan.patternIds, leftPlan.order,
					leftKeyBuffers, orderedLeftScratch, orderedLeftQuadScratch, orderedLeftReusable);
			if (leftIterator == null) {
				orderedLeftReusable = null;
				return createSequentialIterator(dataset, binding, valueStore);
			}
			if (leftIterator != LmdbIdJoinIterator.emptyRecordIterator()) {
				orderedLeftReusable = leftIterator;
			} else {
				orderedLeftReusable = null;
			}

			if (orderedRightScratch == null || orderedRightScratch.length < binding.length) {
				orderedRightScratch = new long[binding.length];
			}
			if (orderedRightQuadScratch == null) {
				orderedRightQuadScratch = new long[4];
			}
			RecordIterator rightIterator = dataset.getOrderedRecordIterator(binding, rightPlan.subjIndex,
					rightPlan.predIndex, rightPlan.objIndex, rightPlan.ctxIndex, rightPlan.patternIds, rightPlan.order,
					rightKeyBuffers, orderedRightScratch, orderedRightQuadScratch, orderedRightReusable);
			if (rightIterator == null) {
				leftIterator.close();
				orderedRightReusable = null;
				return createSequentialIterator(dataset, binding, valueStore);
			}
			if (rightIterator != LmdbIdJoinIterator.emptyRecordIterator()) {
				orderedRightReusable = rightIterator;
			} else {
				orderedRightReusable = null;
			}

			return new LmdbIdMergeJoinIterator(leftIterator, rightIterator, leftInfo, rightInfo, mergeVariable,
					finalInfo);
		}

		private RecordIterator createSequentialIterator(LmdbEvaluationDataset dataset, long[] binding,
				ValueStore valueStore) throws QueryEvaluationException {
			if (sequentialLeftScratch == null || sequentialLeftScratch.length < binding.length) {
				sequentialLeftScratch = new long[binding.length];
			}
			if (sequentialLeftQuadScratch == null) {
				sequentialLeftQuadScratch = new long[4];
			}
			RecordIterator leftIterator = dataset.getRecordIterator(binding, leftPlan.subjIndex, leftPlan.predIndex,
					leftPlan.objIndex, leftPlan.ctxIndex, leftPlan.patternIds, leftKeyBuffers, sequentialLeftScratch,
					sequentialLeftQuadScratch, sequentialLeftReusable);
			if (leftIterator != null && leftIterator != LmdbIdJoinIterator.emptyRecordIterator()) {
				sequentialLeftReusable = leftIterator;
			} else {
				sequentialLeftReusable = null;
			}
			if (leftIterator == null) {
				return LmdbIdJoinIterator.emptyRecordIterator();
			}
			return new BindingJoinRecordIterator(leftIterator, dataset, rightPlan, rightKeyBuffers);
		}
	}

	private static final class BindingJoinRecordIterator implements RecordIterator {
		private final RecordIterator left;
		private final LmdbEvaluationDataset dataset;
		private final PatternPlan plan;
		private final KeyRangeBuffers keyBuffers;
		private RecordIterator currentRight;
		private long[] rightScratch;
		private long[] quadScratch;
		private RecordIterator reusableRight;

		private BindingJoinRecordIterator(RecordIterator left, LmdbEvaluationDataset dataset, PatternPlan plan,
				KeyRangeBuffers keyBuffers) {
			this.left = left;
			this.dataset = dataset;
			this.plan = plan;
			this.keyBuffers = keyBuffers;
		}

		@Override
		public long[] next() throws QueryEvaluationException {
			while (true) {
				if (currentRight != null) {
					long[] next = currentRight.next();
					if (next != null) {
						return next;
					}
					currentRight.close();
					if (currentRight != LmdbIdJoinIterator.EMPTY_RECORD_ITERATOR) {
						reusableRight = currentRight;
					}
					currentRight = null;
				}

				long[] leftBinding = left.next();
				if (leftBinding == null) {
					return null;
				}
				if (rightScratch == null || rightScratch.length < leftBinding.length) {
					rightScratch = new long[leftBinding.length];
				}
				if (quadScratch == null) {
					quadScratch = new long[4];
				}
				currentRight = dataset.getRecordIterator(leftBinding, plan.subjIndex, plan.predIndex, plan.objIndex,
						plan.ctxIndex, plan.patternIds, keyBuffers, rightScratch, quadScratch, reusableRight);
				if (currentRight != null && currentRight != LmdbIdJoinIterator.EMPTY_RECORD_ITERATOR) {
					reusableRight = currentRight;
				} else {
					reusableRight = null;
				}
			}
		}

		@Override
		public void close() {
			if (currentRight != null) {
				currentRight.close();
				currentRight = null;
			}
			left.close();
			reusableRight = null;
		}
	}

	private static final class PatternPlan {
		private final long[] patternIds;
		private final int subjIndex;
		private final int predIndex;
		private final int objIndex;
		private final int ctxIndex;
		private final StatementOrder order;
		private final StatementPattern pattern;

		private PatternPlan(long[] patternIds, int subjIndex, int predIndex, int objIndex, int ctxIndex,
				StatementOrder order, StatementPattern pattern) {
			this.patternIds = patternIds;
			this.subjIndex = subjIndex;
			this.predIndex = predIndex;
			this.objIndex = objIndex;
			this.ctxIndex = ctxIndex;
			this.order = order;
			this.pattern = pattern;
		}
	}

	private static final class RawPattern {
		private final StatementPattern pattern;
		private final long[] patternIds;
		private final String subjVar;
		private final String predVar;
		private final String objVar;
		private final String ctxVar;
		private final LmdbIdJoinIterator.PatternInfo patternInfo;
		private final boolean invalid;
		private final boolean createdIds;
		private final Map<String, Long> constantIds;

		private RawPattern(StatementPattern pattern, long[] patternIds, String subjVar, String predVar, String objVar,
				String ctxVar,
				LmdbIdJoinIterator.PatternInfo patternInfo, boolean invalid, boolean createdIds,
				Map<String, Long> constantIds) {
			this.pattern = pattern;
			this.patternIds = patternIds;
			this.subjVar = subjVar;
			this.predVar = predVar;
			this.objVar = objVar;
			this.ctxVar = ctxVar;
			this.patternInfo = patternInfo;
			this.invalid = invalid;
			this.createdIds = createdIds;
			this.constantIds = constantIds;
		}

		Map<String, Long> getConstantIds() {
			return constantIds;
		}

		static RawPattern create(StatementPattern pattern, ValueStore valueStore, boolean allowCreate) {
			long[] ids = new long[4];
			Arrays.fill(ids, LmdbValue.UNKNOWN_ID);

			boolean invalid = false;
			boolean createdAny = false;
			Map<String, Long> constantIds = new HashMap<>();

			Var subj = pattern.getSubjectVar();
			String subjVar = null;
			if (subj != null) {
				if (subj.hasValue()) {
					ConstantIdResult result = constantId(valueStore, subj.getValue(), true, false, allowCreate);
					if (result.isInvalid()) {
						invalid = true;
					} else {
						ids[TripleStore.SUBJ_IDX] = result.id;
						createdAny |= result.created;
						constantIds.put(subj.getName(), result.id);
					}
				} else {
					subjVar = subj.getName();
				}
			}

			Var pred = pattern.getPredicateVar();
			String predVar = null;
			if (pred != null) {
				if (pred.hasValue()) {
					ConstantIdResult result = constantId(valueStore, pred.getValue(), false, true, allowCreate);
					if (result.isInvalid()) {
						invalid = true;
					} else {
						ids[TripleStore.PRED_IDX] = result.id;
						createdAny |= result.created;
						constantIds.put(pred.getName(), result.id);
					}
				} else {
					predVar = pred.getName();
				}
			}

			Var obj = pattern.getObjectVar();
			String objVar = null;
			if (obj != null) {
				if (obj.hasValue()) {
					ConstantIdResult result = constantId(valueStore, obj.getValue(), false, false, allowCreate);
					if (result.isInvalid()) {
						invalid = true;
					} else {
						ids[TripleStore.OBJ_IDX] = result.id;
						createdAny |= result.created;
						constantIds.put(obj.getName(), result.id);
					}
				} else {
					objVar = obj.getName();
				}
			}

			Var ctx = pattern.getContextVar();
			String ctxVar = null;
			if (ctx != null) {
				if (ctx.hasValue()) {
					ConstantIdResult result = constantId(valueStore, ctx.getValue(), true, false, allowCreate);
					if (result.isInvalid()) {
						invalid = true;
					} else {
						ids[TripleStore.CONTEXT_IDX] = result.id;
						createdAny |= result.created;
						constantIds.put(ctx.getName(), result.id);
					}
				} else {
					ctxVar = ctx.getName();
				}
			}

			LmdbIdJoinIterator.PatternInfo info = LmdbIdJoinIterator.PatternInfo.create(pattern);
			return new RawPattern(pattern, ids, subjVar, predVar, objVar, ctxVar, info, invalid, createdAny,
					constantIds);
		}

		PatternPlan toPlan(IdBindingInfo finalInfo) {
			return toPlan(finalInfo, null);
		}

		PatternPlan toPlan(IdBindingInfo finalInfo, StatementOrder order) {
			return new PatternPlan(patternIds.clone(), indexFor(subjVar, finalInfo),
					indexFor(predVar, finalInfo), indexFor(objVar, finalInfo), indexFor(ctxVar, finalInfo), order,
					pattern);
		}

		private static int indexFor(String varName, IdBindingInfo info) {
			return varName == null ? -1 : info.getIndex(varName);
		}

		private static ConstantIdResult constantId(ValueStore valueStore, Value value, boolean requireResource,
				boolean requireIri, boolean allowCreate) {
			if (requireResource && !(value instanceof Resource)) {
				return ConstantIdResult.invalid();
			}
			if (requireIri && !(value instanceof IRI)) {
				return ConstantIdResult.invalid();
			}
			if (value instanceof Resource && ((Resource) value).isTriple()) {
				return ConstantIdResult.invalid();
			}
			try {
				if (value instanceof LmdbValue) {
					LmdbValue lmdbValue = (LmdbValue) value;
					if (lmdbValue.getValueStoreRevision().getValueStore() == valueStore) {
						long id = lmdbValue.getInternalID();
						if (id != LmdbValue.UNKNOWN_ID) {
							return ConstantIdResult.existing(id);
						}
					}
				}
				long id = valueStore.getId(value);
				if (id == LmdbValue.UNKNOWN_ID && !allowCreate) {
					id = valueStore.getId(value);
				}
				if (id == LmdbValue.UNKNOWN_ID) {
					if (!allowCreate) {
						return ConstantIdResult.invalid();
					}
					id = valueStore.getId(value, true);
					if (id == LmdbValue.UNKNOWN_ID) {
						return ConstantIdResult.invalid();
					}
					return ConstantIdResult.created(id);
				}
				return ConstantIdResult.existing(id);
			} catch (IOException e) {
				return ConstantIdResult.invalid();
			}
		}

		private static final class ConstantIdResult {
			private final long id;
			private final boolean created;
			private final boolean valid;

			private ConstantIdResult(long id, boolean created, boolean valid) {
				this.id = id;
				this.created = created;
				this.valid = valid;
			}

			static ConstantIdResult invalid() {
				return new ConstantIdResult(Long.MIN_VALUE, false, false);
			}

			static ConstantIdResult existing(long id) {
				return new ConstantIdResult(id, false, true);
			}

			static ConstantIdResult created(long id) {
				return new ConstantIdResult(id, true, true);
			}

			boolean isInvalid() {
				return !valid;
			}
		}
	}

	private static final class MergeSpec {
		private final int leftIndex;
		private final int leftEnd;
		private final int rightEnd;
		private final String mergeVariable;
		private final Join join;

		private MergeSpec(int leftIndex, int leftEnd, int rightEnd, String mergeVariable, Join join) {
			this.leftIndex = leftIndex;
			this.leftEnd = leftEnd;
			this.rightEnd = rightEnd;
			this.mergeVariable = mergeVariable;
			this.join = join;
		}

		int rightIndex() {
			return rightEnd - 1;
		}
	}
}
