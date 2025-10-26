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

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.sail.lmdb.LmdbDatasetContext;
import org.eclipse.rdf4j.sail.lmdb.LmdbEvaluationDataset;
import org.eclipse.rdf4j.sail.lmdb.LmdbEvaluationStrategy;
import org.eclipse.rdf4j.sail.lmdb.RecordIterator;
import org.eclipse.rdf4j.sail.lmdb.ValueStore;

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

	public LmdbIdJoinQueryEvaluationStep(EvaluationStrategy strategy, Join join, QueryEvaluationContext context) {
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

		join.setAlgorithm(LmdbIdJoinIterator.class.getSimpleName());

	}

	private Set<String> computeSharedVariables(LmdbIdJoinIterator.PatternInfo left,
			LmdbIdJoinIterator.PatternInfo right) {
		Set<String> shared = new HashSet<>(left.getVariableNames());
		shared.retainAll(right.getVariableNames());
		return Collections.unmodifiableSet(shared);
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet bindings) {
		try {
			LmdbEvaluationDataset dataset = resolveDataset();
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
