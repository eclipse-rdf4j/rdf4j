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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.common.transaction.QueryEvaluationMode;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.ArrayBindingBasedQueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;
import org.eclipse.rdf4j.sail.lmdb.join.LmdbIdBGPQueryEvaluationStep;
import org.eclipse.rdf4j.sail.lmdb.join.LmdbIdJoinQueryEvaluationStep;
import org.eclipse.rdf4j.sail.lmdb.join.LmdbIdMergeJoinQueryEvaluationStep;

/**
 * Evaluation strategy that can use LMDB-specific join iterators.
 */
@InternalUseOnly
public class LmdbEvaluationStrategy extends StrictEvaluationStrategy {

	private static final ThreadLocal<LmdbEvaluationDataset> CURRENT_DATASET = new ThreadLocal<>();
	private static final ThreadLocal<ConnectionChangeState> CONNECTION_CHANGES = new ThreadLocal<>();

	LmdbEvaluationStrategy(TripleSource tripleSource, Dataset dataset, FederatedServiceResolver serviceResolver,
			long iterationCacheSyncThreshold, EvaluationStatistics evaluationStatistics, boolean trackResultSize) {
		super(tripleSource, dataset, serviceResolver, iterationCacheSyncThreshold, evaluationStatistics,
				trackResultSize);
		setQueryEvaluationMode(QueryEvaluationMode.STRICT);
	}

	@Override
	public QueryEvaluationStep precompile(TupleExpr expr) {
		LmdbEvaluationDataset datasetRef = CURRENT_DATASET.get();
		ValueStore valueStore = datasetRef != null ? datasetRef.getValueStore() : null;
		LmdbEvaluationDataset effectiveDataset = datasetRef;
		if (connectionHasChanges() && valueStore != null) {
			TripleSource idCapableTs = (tripleSource instanceof LmdbIdTripleSource)
					? tripleSource
					: new LmdbIdTripleSourceAdapter(tripleSource, valueStore);
			effectiveDataset = new LmdbOverlayEvaluationDataset(idCapableTs, valueStore);
		}
		LmdbQueryEvaluationContext baseContext = new LmdbQueryEvaluationContext(dataset, tripleSource.getValueFactory(),
				tripleSource.getComparator(), effectiveDataset, valueStore);
		QueryEvaluationContext context = baseContext;
		if (expr instanceof QueryRoot) {
			String[] allVariables = ArrayBindingBasedQueryEvaluationContext
					.findAllVariablesUsedInQuery((QueryRoot) expr);
			QueryEvaluationContext arrayContext = new ArrayBindingBasedQueryEvaluationContext(baseContext, allVariables,
					tripleSource.getComparator());
			context = new LmdbDelegatingQueryEvaluationContext(arrayContext, effectiveDataset, valueStore);
		}
		return precompile(expr, context);
	}

	@Override
	protected QueryEvaluationStep prepare(Join node, QueryEvaluationContext context) {
		QueryEvaluationStep defaultStep = super.prepare(node, context);
		if (Boolean.getBoolean("rdf4j.lmdb.disableIdJoins")) {
			return defaultStep;
		}
		if (context instanceof LmdbDatasetContext && ((LmdbDatasetContext) context).getLmdbDataset().isPresent()) {
			LmdbEvaluationDataset ds = ((LmdbDatasetContext) context).getLmdbDataset().get();
			Optional<ValueStore> valueStoreOpt = ((LmdbDatasetContext) context).getValueStore();
			// If the active transaction has uncommitted changes, avoid ID-only join shortcuts.
			if (ds.hasTransactionChanges() || connectionHasChanges()) {
				return defaultStep;
			}

			if (node.isMergeJoin() && node.getOrder() != null && node.getLeftArg() instanceof StatementPattern
					&& node.getRightArg() instanceof StatementPattern) {
				return new LmdbIdMergeJoinQueryEvaluationStep(node, context, defaultStep);
			}
			// Try to flatten a full BGP of statement patterns
			List<StatementPattern> patterns = new ArrayList<>();
			if (LmdbIdBGPQueryEvaluationStep.flattenBGP(node, patterns)
					&& !patterns.isEmpty()) {
				LmdbIdBGPQueryEvaluationStep step = new LmdbIdBGPQueryEvaluationStep(node, patterns, context,
						defaultStep);
				if (step.shouldUseFallbackImmediately()) {
					if (valueStoreOpt.isPresent()) {
						TripleSource idCapableTs = (tripleSource instanceof LmdbIdTripleSource)
								? tripleSource
								: new LmdbIdTripleSourceAdapter(tripleSource, valueStoreOpt.get());
						LmdbOverlayEvaluationDataset overlay = new LmdbOverlayEvaluationDataset(idCapableTs,
								valueStoreOpt.get());
						QueryEvaluationContext overlayContext = new LmdbDelegatingQueryEvaluationContext(context,
								overlay,
								valueStoreOpt.get());
						return new LmdbIdBGPQueryEvaluationStep(node, patterns,
								overlayContext, defaultStep);
					}
					return defaultStep;
				}
				boolean hasPreBound = hasPreBoundVariables(patterns);
				if ((requiresSnapshotOverlay(ds) || hasPreBound) && valueStoreOpt.isPresent()) {
					TripleSource idCapableTs = (tripleSource instanceof LmdbIdTripleSource)
							? tripleSource
							: new LmdbIdTripleSourceAdapter(tripleSource, valueStoreOpt.get());
					LmdbOverlayEvaluationDataset overlay = new LmdbOverlayEvaluationDataset(idCapableTs,
							valueStoreOpt.get());
					QueryEvaluationContext overlayContext = new LmdbDelegatingQueryEvaluationContext(context, overlay,
							valueStoreOpt.get());
					LmdbIdBGPQueryEvaluationStep overlayStep = new LmdbIdBGPQueryEvaluationStep(node, patterns,
							overlayContext, defaultStep);
					overlayStep.applyAlgorithmTag();
					return overlayStep;
				}
				step.applyAlgorithmTag();
				return step;
			}
			// Fallback to two-pattern ID join
			if (node.getLeftArg() instanceof StatementPattern && node.getRightArg() instanceof StatementPattern) {
				LmdbIdJoinQueryEvaluationStep step = new LmdbIdJoinQueryEvaluationStep(this, node, context,
						defaultStep);
				if (step.shouldUseFallbackImmediately()) {
					if (valueStoreOpt.isPresent()) {
						TripleSource idCapableTs = (tripleSource instanceof LmdbIdTripleSource)
								? tripleSource
								: new LmdbIdTripleSourceAdapter(tripleSource, valueStoreOpt.get());
						LmdbOverlayEvaluationDataset overlay = new LmdbOverlayEvaluationDataset(idCapableTs,
								valueStoreOpt.get());
						QueryEvaluationContext overlayContext = new LmdbDelegatingQueryEvaluationContext(context,
								overlay,
								valueStoreOpt.get());
						return new LmdbIdJoinQueryEvaluationStep(this, node,
								overlayContext, defaultStep);
					}
					return defaultStep;
				}
				if (requiresSnapshotOverlay(ds) && valueStoreOpt.isPresent()) {
					TripleSource idCapableTs = (tripleSource instanceof LmdbIdTripleSource)
							? tripleSource
							: new LmdbIdTripleSourceAdapter(tripleSource, valueStoreOpt.get());
					LmdbOverlayEvaluationDataset overlay = new LmdbOverlayEvaluationDataset(idCapableTs,
							valueStoreOpt.get());
					QueryEvaluationContext overlayContext = new LmdbDelegatingQueryEvaluationContext(context, overlay,
							valueStoreOpt.get());
					LmdbIdJoinQueryEvaluationStep overlayStep = new LmdbIdJoinQueryEvaluationStep(this, node,
							overlayContext, defaultStep);
					overlayStep.applyAlgorithmTag(node);
					return overlayStep;
				}
				step.applyAlgorithmTag(node);
				return step;
			}
		}
		return defaultStep;
	}

	private boolean hasPreBoundVariables(List<StatementPattern> patterns) {
		for (StatementPattern pattern : patterns) {
			if (isPreBound(pattern.getSubjectVar()) || isPreBound(pattern.getPredicateVar())
					|| isPreBound(pattern.getObjectVar()) || isPreBound(pattern.getContextVar())) {
				return true;
			}
		}
		return false;
	}

	private boolean isPreBound(org.eclipse.rdf4j.query.algebra.Var var) {
		return var != null && var.hasValue() && !var.isConstant();
	}

	private boolean requiresSnapshotOverlay(LmdbEvaluationDataset dataset) {
		IsolationLevel isolation = dataset.getIsolationLevel();
		return isolation == IsolationLevels.SNAPSHOT || isolation == IsolationLevels.SERIALIZABLE
				|| isolation == IsolationLevels.SNAPSHOT_READ;
	}

	static void setCurrentDataset(LmdbEvaluationDataset dataset) {
		CURRENT_DATASET.set(dataset);
	}

	static void clearCurrentDataset() {
		CURRENT_DATASET.remove();
	}

	public static Optional<LmdbEvaluationDataset> getCurrentDataset() {
		return Optional.ofNullable(CURRENT_DATASET.get());
	}

	static void pushConnectionChangesFlag(boolean hasUncommittedChanges) {
		if (!hasUncommittedChanges && CONNECTION_CHANGES.get() == null) {
			return;
		}

		ConnectionChangeState state = CONNECTION_CHANGES.get();
		if (state == null) {
			state = new ConnectionChangeState();
			CONNECTION_CHANGES.set(state);
		}
		state.depth++;
		state.hasChanges |= hasUncommittedChanges;
	}

	static void popConnectionChangesFlag() {
		ConnectionChangeState state = CONNECTION_CHANGES.get();
		if (state == null) {
			return;
		}
		state.depth--;
		if (state.depth <= 0) {
			CONNECTION_CHANGES.remove();
		}
	}

	private static boolean connectionHasChanges() {
		ConnectionChangeState state = CONNECTION_CHANGES.get();
		return state != null && state.hasChanges;
	}

	public static boolean hasActiveConnectionChanges() {
		return connectionHasChanges();
	}

	private static final class ConnectionChangeState {
		private int depth;
		private boolean hasChanges;
	}
}
