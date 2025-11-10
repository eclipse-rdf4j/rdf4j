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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
 * Query evaluation step that wires up the LMDB merge join iterator.
 */
public class LmdbIdMergeJoinQueryEvaluationStep implements QueryEvaluationStep {

	private final PatternPlan leftPlan;
	private final PatternPlan rightPlan;
	private final IdBindingInfo bindingInfo;
	private final QueryEvaluationContext context;
	private final LmdbDatasetContext datasetContext;
	private final Join join;
	private final LmdbIdJoinIterator.PatternInfo leftInfo;
	private final LmdbIdJoinIterator.PatternInfo rightInfo;
	private final String mergeVariable;
	private final QueryEvaluationStep fallbackStep;
	private final boolean hasInvalidPattern;
	private final String fallbackAlgorithmName;
	private final Map<StatementPattern, KeyRangeBuffers> patternBuffers = new HashMap<>();

	public LmdbIdMergeJoinQueryEvaluationStep(Join join, QueryEvaluationContext context,
			QueryEvaluationStep fallbackStep) {
		if (!(join.getLeftArg() instanceof StatementPattern) || !(join.getRightArg() instanceof StatementPattern)) {
			throw new IllegalArgumentException("LMDB merge join requires StatementPattern operands");
		}
		if (!(context instanceof LmdbDatasetContext)) {
			throw new IllegalArgumentException("LMDB merge join requires LMDB query evaluation context");
		}
		if (!join.isMergeJoin()) {
			throw new IllegalArgumentException("Merge join flag must be set on the Join node");
		}
		Var orderVar = join.getOrder();
		if (orderVar == null) {
			throw new IllegalArgumentException("Merge join requires join order variable to be set");
		}

		this.context = context;
		this.datasetContext = (LmdbDatasetContext) context;
		this.join = join;

		StatementPattern leftPattern = (StatementPattern) join.getLeftArg();
		StatementPattern rightPattern = (StatementPattern) join.getRightArg();

		this.mergeVariable = orderVar.getName();
		this.leftInfo = LmdbIdJoinIterator.PatternInfo.create(leftPattern);
		this.rightInfo = LmdbIdJoinIterator.PatternInfo.create(rightPattern);
		this.fallbackStep = fallbackStep;
		this.fallbackAlgorithmName = fallbackStep != null ? join.getAlgorithmName() : null;

		if (leftPattern.getStatementOrder() == null) {
			leftPattern.setOrder(orderVar);
		}
		if (rightPattern.getStatementOrder() == null) {
			rightPattern.setOrder(orderVar);
		}

		ValueStore valueStore = this.datasetContext.getValueStore()
				.orElseThrow(() -> new IllegalStateException("LMDB merge join requires ValueStore access"));

		RawPattern leftRaw = RawPattern.create(leftPattern, valueStore);
		RawPattern rightRaw = RawPattern.create(rightPattern, valueStore);
		this.hasInvalidPattern = leftRaw.invalid || rightRaw.invalid;

		IdBindingInfo info = IdBindingInfo.fromFirstPattern(leftInfo, context);
		info = IdBindingInfo.combine(info, rightInfo, context);
		this.bindingInfo = info;

		StatementOrder leftOrder = determineOrder(leftPattern, leftInfo);
		StatementOrder rightOrder = determineOrder(rightPattern, rightInfo);

		this.leftPlan = leftRaw.toPlan(info, leftOrder);
		this.rightPlan = rightRaw.toPlan(info, rightOrder);
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
				return new EmptyIteration<>();
			}
			if (leftPlan.order == null || rightPlan.order == null) {
				return evaluateFallback(bindings);
			}

			ValueStore valueStore = dataset.getValueStore();
			long[] initialBinding = createInitialBinding(bindingInfo, bindings, valueStore);
			if (initialBinding == null) {
				return new EmptyIteration<>();
			}

			KeyRangeBuffers leftBuffers = keyBuffersFor(leftPlan.pattern);
			KeyRangeBuffers rightBuffers = keyBuffersFor(rightPlan.pattern);

			RecordIterator leftIterator = dataset.getOrderedRecordIterator(initialBinding, leftPlan.subjIndex,
					leftPlan.predIndex, leftPlan.objIndex, leftPlan.ctxIndex, leftPlan.patternIds, leftPlan.order,
					leftBuffers, null, null);
			if (leftIterator == null) {
				return evaluateFallback(bindings);
			}

			RecordIterator rightIterator = dataset.getOrderedRecordIterator(initialBinding, rightPlan.subjIndex,
					rightPlan.predIndex, rightPlan.objIndex, rightPlan.ctxIndex, rightPlan.patternIds, rightPlan.order,
					rightBuffers, null, null);
			if (rightIterator == null) {
				try {
					leftIterator.close();
				} catch (Exception ignore) {
				}
				return evaluateFallback(bindings);
			}

			join.setAlgorithm(LmdbIdMergeJoinIterator.class.getSimpleName());
			RecordIterator mergeIterator = new LmdbIdMergeJoinIterator(leftIterator, rightIterator, leftInfo, rightInfo,
					mergeVariable, bindingInfo);
			return new LmdbIdFinalBindingSetIteration(mergeIterator, bindingInfo, context, bindings, valueStore,
					Collections.emptyMap());
		} catch (QueryEvaluationException e) {
			throw e;
		}
	}

	private CloseableIteration<BindingSet> evaluateFallback(BindingSet bindings) {
		if (fallbackAlgorithmName != null) {
			join.setAlgorithm(fallbackAlgorithmName);
		}
		if (fallbackStep != null) {
			return fallbackStep.evaluate(bindings);
		}
		return new EmptyIteration<>();
	}

	private LmdbEvaluationDataset resolveDataset() {
		Optional<LmdbEvaluationDataset> fromContext = datasetContext.getLmdbDataset();
		if (fromContext.isPresent()) {
			return fromContext.get();
		}
		return LmdbEvaluationStrategy.getCurrentDataset()
				.orElseThrow(
						() -> new IllegalStateException("No active LMDB dataset available for merge join evaluation"));
	}

	private static long[] createInitialBinding(IdBindingInfo info, BindingSet bindings, ValueStore valueStore)
			throws QueryEvaluationException {
		long[] binding = new long[info.size()];
		Arrays.fill(binding, LmdbValue.UNKNOWN_ID);
		if (bindings == null || bindings.isEmpty()) {
			return binding;
		}
		for (String name : info.getVariableNames()) {
			Value value = bindings.getValue(name);
			if (value == null) {
				continue;
			}
			long id = resolveId(valueStore, value);
			if (id == LmdbValue.UNKNOWN_ID) {
				return null;
			}
			int index = info.getIndex(name);
			if (index >= 0) {
				binding[index] = id;
			}
		}
		return binding;
	}

	private KeyRangeBuffers keyBuffersFor(StatementPattern pattern) {
		return patternBuffers.computeIfAbsent(pattern, p -> KeyRangeBuffers.acquire());
	}

	private static long resolveId(ValueStore valueStore, Value value) throws QueryEvaluationException {
		if (value == null) {
			return LmdbValue.UNKNOWN_ID;
		}
		if (value instanceof org.eclipse.rdf4j.sail.lmdb.model.LmdbValue) {
			org.eclipse.rdf4j.sail.lmdb.model.LmdbValue lmdbValue = (org.eclipse.rdf4j.sail.lmdb.model.LmdbValue) value;
			if (lmdbValue.getValueStoreRevision().getValueStore() == valueStore) {
				long id = lmdbValue.getInternalID();
				if (id != LmdbValue.UNKNOWN_ID) {
					return id;
				}
			}
		}
		try {
			long id = valueStore.getId(value);
			return id;
		} catch (IOException e) {
			throw new QueryEvaluationException(e);
		}
	}

	private StatementOrder determineOrder(StatementPattern pattern, LmdbIdJoinIterator.PatternInfo info) {
		StatementOrder order = pattern.getStatementOrder();
		if (order != null) {
			return order;
		}

		if (mergeVariable == null) {
			return null;
		}

		int mask = info.getPositionsMask(mergeVariable);
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
		private final long[] patternIds;
		private final String subjVar;
		private final String predVar;
		private final String objVar;
		private final String ctxVar;
		private final boolean invalid;
		private final StatementPattern pattern;

		private RawPattern(long[] patternIds, String subjVar, String predVar, String objVar, String ctxVar,
				boolean invalid, StatementPattern pattern) {
			this.patternIds = patternIds;
			this.subjVar = subjVar;
			this.predVar = predVar;
			this.objVar = objVar;
			this.ctxVar = ctxVar;
			this.invalid = invalid;
			this.pattern = pattern;
		}

		static RawPattern create(StatementPattern pattern, ValueStore valueStore) {
			long[] ids = new long[4];
			Arrays.fill(ids, LmdbValue.UNKNOWN_ID);
			boolean invalid = false;

			Var subj = pattern.getSubjectVar();
			String subjVar = null;
			if (subj != null) {
				if (subj.hasValue()) {
					long id = constantId(valueStore, subj.getValue(), true, false);
					if (id == Long.MIN_VALUE) {
						invalid = true;
					} else {
						ids[TripleStore.SUBJ_IDX] = id;
					}
				} else {
					subjVar = subj.getName();
				}
			}

			Var pred = pattern.getPredicateVar();
			String predVar = null;
			if (pred != null) {
				if (pred.hasValue()) {
					long id = constantId(valueStore, pred.getValue(), false, true);
					if (id == Long.MIN_VALUE) {
						invalid = true;
					} else {
						ids[TripleStore.PRED_IDX] = id;
					}
				} else {
					predVar = pred.getName();
				}
			}

			Var obj = pattern.getObjectVar();
			String objVar = null;
			if (obj != null) {
				if (obj.hasValue()) {
					long id = constantId(valueStore, obj.getValue(), false, false);
					if (id == Long.MIN_VALUE) {
						invalid = true;
					} else {
						ids[TripleStore.OBJ_IDX] = id;
					}
				} else {
					objVar = obj.getName();
				}
			}

			Var ctx = pattern.getContextVar();
			String ctxVar = null;
			if (ctx != null) {
				if (ctx.hasValue()) {
					long id = constantId(valueStore, ctx.getValue(), true, false);
					if (id == Long.MIN_VALUE) {
						invalid = true;
					} else {
						ids[TripleStore.CONTEXT_IDX] = id;
					}
				} else {
					ctxVar = ctx.getName();
				}
			}

			return new RawPattern(ids, subjVar, predVar, objVar, ctxVar, invalid, pattern);
		}

		PatternPlan toPlan(IdBindingInfo bindingInfo, StatementOrder order) {
			return new PatternPlan(patternIds.clone(), indexFor(subjVar, bindingInfo), indexFor(predVar, bindingInfo),
					indexFor(objVar, bindingInfo), indexFor(ctxVar, bindingInfo), order, pattern);
		}

		private static int indexFor(String varName, IdBindingInfo info) {
			return varName == null ? -1 : info.getIndex(varName);
		}

		private static long constantId(ValueStore valueStore, Value value, boolean requireResource,
				boolean requireIri) {
			if (requireResource && !(value instanceof Resource)) {
				return Long.MIN_VALUE;
			}
			if (requireIri && !(value instanceof IRI)) {
				return Long.MIN_VALUE;
			}
			if (value instanceof Resource && ((Resource) value).isTriple()) {
				return Long.MIN_VALUE;
			}
			try {
				if (value instanceof org.eclipse.rdf4j.sail.lmdb.model.LmdbValue) {
					org.eclipse.rdf4j.sail.lmdb.model.LmdbValue lmdbValue = (org.eclipse.rdf4j.sail.lmdb.model.LmdbValue) value;
					if (lmdbValue.getValueStoreRevision().getValueStore() == valueStore) {
						long id = lmdbValue.getInternalID();
						if (id != LmdbValue.UNKNOWN_ID) {
							return id;
						}
					}
				}
				long id = valueStore.getId(value);
				if (id == LmdbValue.UNKNOWN_ID) {
					id = valueStore.getId(value, true);
				}
				return id == LmdbValue.UNKNOWN_ID ? Long.MIN_VALUE : id;
			} catch (IOException e) {
				return Long.MIN_VALUE;
			}
		}
	}
}
