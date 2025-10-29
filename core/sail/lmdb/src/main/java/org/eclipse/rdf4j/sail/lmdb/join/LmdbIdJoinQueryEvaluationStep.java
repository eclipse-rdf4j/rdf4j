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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
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

			if (Boolean.getBoolean("rdf4j.lmdb.experimentalTwoPatternArrayJoin")) {
				// Experimental: use the array-API two-pattern BGP pipeline
				List<StatementPattern> patterns = Arrays
						.asList(leftPattern, rightPattern);
				LmdbIdBGPQueryEvaluationStep bgpStep = new LmdbIdBGPQueryEvaluationStep(
						new Join(leftPattern, rightPattern), patterns, context, fallbackStep);
				return bgpStep.evaluate(bindings);
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
}
