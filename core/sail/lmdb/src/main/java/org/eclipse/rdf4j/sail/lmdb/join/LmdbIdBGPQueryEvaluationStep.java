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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
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
	private final IdBindingInfo finalInfo;
	private final QueryEvaluationContext context;
	private final LmdbDatasetContext datasetContext;
	private final Join root;
	private final QueryEvaluationStep fallbackStep;
	private final boolean hasInvalidPattern;
	private final boolean createdDynamicIds;
	private final boolean allowCreateConstantIds;
	private final Map<String, Long> constantBindings;

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

		List<RawPattern> rawPatterns = new ArrayList<>(patterns.size());
		boolean invalid = false;
		boolean created = false;
		Map<String, Long> constants = new HashMap<>();
		for (StatementPattern pattern : patterns) {
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
		for (RawPattern raw : rawPatterns) {
			planList.add(raw.toPlan(finalInfo));
		}
		this.plans = planList;
	}

	public boolean shouldUseFallbackImmediately() {
		return hasInvalidPattern && fallbackStep != null && !allowCreateConstantIds;
	}

	public void applyAlgorithmTag() {
		markJoinTreeWithIdAlgorithm(root);
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

			PatternPlan firstPlan = plans.get(0);
			RecordIterator iter = dataset.getRecordIterator(initialBinding, firstPlan.subjIndex, firstPlan.predIndex,
					firstPlan.objIndex, firstPlan.ctxIndex, firstPlan.patternIds);

			for (int i = 1; i < plans.size(); i++) {
				iter = new BindingJoinRecordIterator(iter, dataset, plans.get(i));
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
		if (expr instanceof StatementPattern) {
			out.add((StatementPattern) expr);
			return true;
		}
		if (expr instanceof Join) {
			Join j = (Join) expr;
			// merge joins only; we avoid mergeJoin or other special joins
			if (j.isMergeJoin()) {
				return false;
			}
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

	private static final class BindingJoinRecordIterator implements RecordIterator {
		private final RecordIterator left;
		private final LmdbEvaluationDataset dataset;
		private final PatternPlan plan;
		private RecordIterator currentRight;

		private BindingJoinRecordIterator(RecordIterator left, LmdbEvaluationDataset dataset, PatternPlan plan) {
			this.left = left;
			this.dataset = dataset;
			this.plan = plan;
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
					currentRight = null;
				}

				long[] leftBinding = left.next();
				if (leftBinding == null) {
					return null;
				}
				currentRight = dataset.getRecordIterator(leftBinding, plan.subjIndex, plan.predIndex, plan.objIndex,
						plan.ctxIndex, plan.patternIds);
			}
		}

		@Override
		public void close() {
			if (currentRight != null) {
				currentRight.close();
				currentRight = null;
			}
			left.close();
		}
	}

	private static final class PatternPlan {
		private final long[] patternIds;
		private final int subjIndex;
		private final int predIndex;
		private final int objIndex;
		private final int ctxIndex;

		private PatternPlan(long[] patternIds, int subjIndex, int predIndex, int objIndex, int ctxIndex) {
			this.patternIds = patternIds;
			this.subjIndex = subjIndex;
			this.predIndex = predIndex;
			this.objIndex = objIndex;
			this.ctxIndex = ctxIndex;
		}
	}

	private static final class RawPattern {
		private final long[] patternIds;
		private final String subjVar;
		private final String predVar;
		private final String objVar;
		private final String ctxVar;
		private final LmdbIdJoinIterator.PatternInfo patternInfo;
		private final boolean invalid;
		private final boolean createdIds;
		private final Map<String, Long> constantIds;

		private RawPattern(long[] patternIds, String subjVar, String predVar, String objVar, String ctxVar,
				LmdbIdJoinIterator.PatternInfo patternInfo, boolean invalid, boolean createdIds,
				Map<String, Long> constantIds) {
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
			return new RawPattern(ids, subjVar, predVar, objVar, ctxVar, info, invalid, createdAny, constantIds);
		}

		PatternPlan toPlan(IdBindingInfo finalInfo) {
			return new PatternPlan(patternIds.clone(), indexFor(subjVar, finalInfo),
					indexFor(predVar, finalInfo), indexFor(objVar, finalInfo), indexFor(ctxVar, finalInfo));
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
}
