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

import static org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.sail.lmdb.IdBindingInfo;
import org.eclipse.rdf4j.sail.lmdb.LmdbDatasetContext;
import org.eclipse.rdf4j.sail.lmdb.LmdbEvaluationDataset;
import org.eclipse.rdf4j.sail.lmdb.LmdbEvaluationStrategy;
import org.eclipse.rdf4j.sail.lmdb.RecordIterator;
import org.eclipse.rdf4j.sail.lmdb.ValueStore;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;

/**
 * Query evaluation step that wires up the LMDB ID join iterator.
 */
public class LmdbIdJoinQueryEvaluationStep implements QueryEvaluationStep {

	private final StatementPattern leftPattern;
	private final StatementPattern rightPattern;
	private final QueryEvaluationContext context;
	private final LmdbIdJoinIterator.PatternInfo leftInfo;
	private final LmdbIdJoinIterator.PatternInfo rightInfo;
	private final Set<String> sharedVariables;
	private final LmdbDatasetContext datasetContext;
	private final QueryEvaluationStep fallbackStep;
	private final boolean fallbackImmediately;

	public LmdbIdJoinQueryEvaluationStep(EvaluationStrategy strategy, Join join, QueryEvaluationContext context,
			QueryEvaluationStep fallbackStep) {
		if (!(join.getLeftArg() instanceof StatementPattern) || !(join.getRightArg() instanceof StatementPattern)) {
			throw new IllegalArgumentException("LMDB ID join requires StatementPattern operands");
		}
		if (!(context instanceof LmdbDatasetContext)) {
			throw new IllegalArgumentException("LMDB ID join requires LMDB query evaluation context");
		}

		this.datasetContext = (LmdbDatasetContext) context;
		this.leftPattern = (StatementPattern) join.getLeftArg();
		this.rightPattern = (StatementPattern) join.getRightArg();
		this.context = context;

		this.leftInfo = LmdbIdJoinIterator.PatternInfo.create(leftPattern);
		this.rightInfo = LmdbIdJoinIterator.PatternInfo.create(rightPattern);
		this.sharedVariables = computeSharedVariables(leftInfo, rightInfo);
		this.fallbackStep = fallbackStep;

		boolean allowCreate = this.datasetContext.getLmdbDataset()
				.map(LmdbEvaluationDataset::hasTransactionChanges)
				.orElse(LmdbEvaluationStrategy.hasActiveConnectionChanges());
		ValueStore valueStore = this.datasetContext.getValueStore().orElse(null);
		this.fallbackImmediately = valueStore == null
				|| (!allowCreate && (!constantsResolvable(leftPattern, valueStore, allowCreate)
						|| !constantsResolvable(rightPattern, valueStore, allowCreate)));

	}

	private Set<String> computeSharedVariables(LmdbIdJoinIterator.PatternInfo left,
			LmdbIdJoinIterator.PatternInfo right) {
		Set<String> shared = new HashSet<>(left.getVariableNames());
		shared.retainAll(right.getVariableNames());
		return Collections.unmodifiableSet(shared);
	}

	public boolean shouldUseFallbackImmediately() {
		return fallbackImmediately;
	}

	public void applyAlgorithmTag(Join join) {
		join.setAlgorithm(LmdbIdJoinIterator.class.getSimpleName());
	}

	private static boolean constantsResolvable(StatementPattern pattern, ValueStore valueStore, boolean allowCreate) {
		try {
			Var subj = pattern.getSubjectVar();
			if (subj != null && subj.hasValue()) {
				if (!resolveConstantId(valueStore, subj.getValue(), true, false, allowCreate)) {
					return false;
				}
			}
			Var pred = pattern.getPredicateVar();
			if (pred != null && pred.hasValue()) {
				if (!resolveConstantId(valueStore, pred.getValue(), false, true, allowCreate)) {
					return false;
				}
			}
			Var obj = pattern.getObjectVar();
			if (obj != null && obj.hasValue()) {
				if (!resolveConstantId(valueStore, obj.getValue(), false, false, allowCreate)) {
					return false;
				}
			}
			Var ctx = pattern.getContextVar();
			if (ctx != null && ctx.hasValue()) {
				if (!resolveConstantId(valueStore, ctx.getValue(), true, false, allowCreate)) {
					return false;
				}
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	private static boolean resolveConstantId(ValueStore valueStore, Value value, boolean requireResource,
			boolean requireIri, boolean allowCreate) throws IOException {
		if (requireResource && !(value instanceof Resource)) {
			return false;
		}
		if (requireIri && !(value instanceof IRI)) {
			return false;
		}
		if (value instanceof Resource
				&& ((Resource) value).isTriple()) {
			return false;
		}
		if (value instanceof LmdbValue) {
			LmdbValue lmdbValue = (LmdbValue) value;
			if (lmdbValue.getValueStoreRevision().getValueStore() == valueStore) {
				long id = lmdbValue.getInternalID();
				if (id != UNKNOWN_ID) {
					return true;
				}
			}
		}
		long id = valueStore.getId(value);
		if (id == UNKNOWN_ID && !allowCreate) {
			id = valueStore.getId(value);
		}
		if (id == UNKNOWN_ID && allowCreate) {
			id = valueStore.getId(value, true);
		}
		return id != UNKNOWN_ID;
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet bindings) {
		if (fallbackStep != null && LmdbEvaluationStrategy.hasActiveConnectionChanges()) {
			return fallbackStep.evaluate(bindings);
		}
		if (fallbackImmediately && fallbackStep != null) {
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

			if (!"false".equalsIgnoreCase(System.getProperty("rdf4j.lmdb.experimentalTwoPatternArrayJoin", "true"))) {
				ValueStore valueStore = dataset.getValueStore();
				IdBindingInfo bindingInfo = IdBindingInfo.combine(
						IdBindingInfo.fromFirstPattern(leftInfo, context), rightInfo, context);

				int subjIdx = indexFor(rightPattern.getSubjectVar(), bindingInfo);
				int predIdx = indexFor(rightPattern.getPredicateVar(), bindingInfo);
				int objIdx = indexFor(rightPattern.getObjectVar(), bindingInfo);
				int ctxIdx = indexFor(rightPattern.getContextVar(), bindingInfo);
				long[] patternIds = resolvePatternIds(rightPattern, valueStore);

				long[] initialBinding = createInitialBinding(bindingInfo, bindings, valueStore);
				if (initialBinding == null) {
					return new org.eclipse.rdf4j.common.iteration.EmptyIteration<>();
				}

				RecordIterator leftIterator = dataset.getRecordIterator(leftPattern, bindings);
				long[] bindingSnapshot = new long[initialBinding.length];
				long[] rightScratch = new long[initialBinding.length];
				LmdbIdJoinIterator.RecordIteratorFactory rightFactory = leftRecord -> {
					System.arraycopy(initialBinding, 0, bindingSnapshot, 0, initialBinding.length);
					for (String name : leftInfo.getVariableNames()) {
						int pos = bindingInfo.getIndex(name);
						if (pos >= 0) {
							long id = leftInfo.getId(leftRecord, name);
							if (id != org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID) {
								bindingSnapshot[pos] = id;
							}
						}
					}
					return dataset.getRecordIterator(bindingSnapshot, subjIdx, predIdx, objIdx, ctxIdx, patternIds,
							rightScratch);
				};

				return new LmdbIdJoinIterator(leftIterator, rightFactory, leftInfo, bindingInfo, sharedVariables,
						context, bindings, valueStore);
			}

			// Default: materialize right side via BindingSet and use LmdbIdJoinIterator
			ValueStore valueStore = dataset.getValueStore();
			RecordIterator leftIterator = dataset.getRecordIterator(leftPattern, bindings);

			LmdbIdJoinIterator.RecordIteratorFactory rightFactory = leftRecord -> {
				MutableBindingSet bs = context.createBindingSet();
				bindings.forEach(binding -> bs.addBinding(binding.getName(), binding.getValue()));
				if (!leftInfo.applyRecord(leftRecord, bs, valueStore)) {
					return LmdbIdJoinIterator.emptyRecordIterator();
				}
				return dataset.getRecordIterator(rightPattern, bs);
			};

			return new LmdbIdJoinIterator(leftIterator, rightFactory, leftInfo, rightInfo, sharedVariables, context,
					bindings, valueStore);
		} catch (QueryEvaluationException e) {
			throw e;
		}
	}

	private LmdbEvaluationDataset resolveDataset() {
		// Honor the delegated SailDataset provided via the evaluation context first.
		// This preserves transaction-local visibility (e.g., SNAPSHOT/SERIALIZABLE overlays).
		Optional<LmdbEvaluationDataset> fromContext = datasetContext.getLmdbDataset();
		if (fromContext.isPresent()) {
			return fromContext.get();
		}
		// Fall back to the thread-local only if the context did not carry a dataset reference.
		return LmdbEvaluationStrategy.getCurrentDataset()
				.orElseThrow(() -> new IllegalStateException("No active LMDB dataset available for join evaluation"));
	}

	private static int indexFor(Var var, IdBindingInfo info) {
		if (var == null || var.hasValue()) {
			return -1;
		}
		return info.getIndex(var.getName());
	}

	private static long[] resolvePatternIds(StatementPattern pattern, ValueStore valueStore)
			throws QueryEvaluationException {
		long[] ids = new long[4];
		ids[org.eclipse.rdf4j.sail.lmdb.TripleStore.SUBJ_IDX] = resolveIdIfConstant(pattern.getSubjectVar(), valueStore,
				true, false);
		ids[org.eclipse.rdf4j.sail.lmdb.TripleStore.PRED_IDX] = resolveIdIfConstant(pattern.getPredicateVar(),
				valueStore,
				false, true);
		ids[org.eclipse.rdf4j.sail.lmdb.TripleStore.OBJ_IDX] = resolveIdIfConstant(pattern.getObjectVar(), valueStore,
				false, false);
		ids[org.eclipse.rdf4j.sail.lmdb.TripleStore.CONTEXT_IDX] = resolveIdIfConstant(pattern.getContextVar(),
				valueStore, true, false);
		return ids;
	}

	private static long resolveIdIfConstant(Var var, ValueStore valueStore, boolean requireResource, boolean requireIri)
			throws QueryEvaluationException {
		if (var == null || !var.hasValue()) {
			return org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID;
		}
		Value v = var.getValue();
		if (requireResource && !(v instanceof org.eclipse.rdf4j.model.Resource)) {
			return Long.MIN_VALUE;
		}
		if (requireIri && !(v instanceof org.eclipse.rdf4j.model.IRI)) {
			return Long.MIN_VALUE;
		}
		if (v instanceof org.eclipse.rdf4j.model.Resource && ((org.eclipse.rdf4j.model.Resource) v).isTriple()) {
			return Long.MIN_VALUE;
		}
		try {
			long id = valueStore.getId(v);
			if (id == org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID) {
				id = valueStore.getId(v, true);
			}
			return id == org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID ? Long.MIN_VALUE : id;
		} catch (IOException e) {
			throw new QueryEvaluationException(e);
		}
	}

	private static long[] createInitialBinding(IdBindingInfo info, BindingSet bindings, ValueStore valueStore)
			throws QueryEvaluationException {
		long[] binding = new long[info.size()];
		Arrays.fill(binding, org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID);
		if (bindings == null || bindings.isEmpty()) {
			return binding;
		}
		for (String name : info.getVariableNames()) {
			Value value = bindings.getValue(name);
			if (value == null) {
				continue;
			}
			long id = resolveId(valueStore, value);
			if (id == org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID) {
				return null;
			}
			int index = info.getIndex(name);
			if (index >= 0) {
				binding[index] = id;
			}
		}
		return binding;
	}

	private static long resolveId(ValueStore valueStore, Value value) throws QueryEvaluationException {
		if (value == null) {
			return org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID;
		}
		if (value instanceof org.eclipse.rdf4j.sail.lmdb.model.LmdbValue) {
			org.eclipse.rdf4j.sail.lmdb.model.LmdbValue lmdbValue = (org.eclipse.rdf4j.sail.lmdb.model.LmdbValue) value;
			if (lmdbValue.getValueStoreRevision().getValueStore() == valueStore) {
				long id = lmdbValue.getInternalID();
				if (id != org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.UNKNOWN_ID) {
					return id;
				}
			}
		}
		try {
			return valueStore.getId(value);
		} catch (IOException e) {
			throw new QueryEvaluationException(e);
		}
	}
}
