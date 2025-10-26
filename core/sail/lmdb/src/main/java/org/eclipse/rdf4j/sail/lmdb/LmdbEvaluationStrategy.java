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

/**
 * Evaluation strategy that can use LMDB-specific join iterators.
 */
@InternalUseOnly
public class LmdbEvaluationStrategy extends StrictEvaluationStrategy {

	private static final ThreadLocal<LmdbEvaluationDataset> CURRENT_DATASET = new ThreadLocal<>();

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
		// Prefer the active LMDB dataset for ID-join evaluation to avoid premature materialization.
		LmdbEvaluationDataset effectiveDataset = datasetRef != null ? datasetRef : null;
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
		if (context instanceof LmdbDatasetContext && ((LmdbDatasetContext) context).getLmdbDataset().isPresent()) {
			LmdbEvaluationDataset ds = ((LmdbDatasetContext) context).getLmdbDataset().get();
			// If the active transaction has uncommitted changes, avoid ID-only join shortcuts.
			if (ds.hasTransactionChanges()) {
				return super.prepare(node, context);
			}
			// Try to flatten a full BGP of statement patterns
			List<StatementPattern> patterns = new ArrayList<>();
			if (LmdbIdBGPQueryEvaluationStep.flattenBGP(node, patterns)
					&& !patterns.isEmpty()) {
				return new LmdbIdBGPQueryEvaluationStep(node, patterns, context);
			}
			// Fallback to two-pattern ID join
			if (node.getLeftArg() instanceof StatementPattern && node.getRightArg() instanceof StatementPattern) {
				return new LmdbIdJoinQueryEvaluationStep(this, node, context);
			}
		}
		return super.prepare(node, context);
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
}
