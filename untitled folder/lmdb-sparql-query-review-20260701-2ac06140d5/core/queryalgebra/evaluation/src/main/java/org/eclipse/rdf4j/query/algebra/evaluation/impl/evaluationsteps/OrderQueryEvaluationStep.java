/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.OrderIterator;
import org.eclipse.rdf4j.query.explanation.TelemetryMetricNames;

public class OrderQueryEvaluationStep implements QueryEvaluationStep {

	private final Order orderNode;
	private final long iterationCacheSyncThreshold;
	private final Comparator<BindingSet> cmp;
	private final long limit;
	private final boolean reduced;
	private final QueryEvaluationStep preparedArg;

	public OrderQueryEvaluationStep(Order orderNode, Comparator<BindingSet> cmp, long limit, boolean reduced,
			QueryEvaluationStep preparedArg, long iterationCacheSyncThreshold) {
		super();
		this.orderNode = orderNode;
		this.cmp = cmp;
		this.limit = limit;
		this.reduced = reduced;
		this.preparedArg = preparedArg;
		this.iterationCacheSyncThreshold = iterationCacheSyncThreshold;
	}

	/**
	 * @deprecated for binary compatibility with versions that did not pass the {@link Order} node. Prefer
	 *             {@link #OrderQueryEvaluationStep(Order, Comparator, long, boolean, QueryEvaluationStep, long)}.
	 */
	@Deprecated(forRemoval = false)
	public OrderQueryEvaluationStep(Comparator<BindingSet> cmp, long limit, boolean reduced,
			QueryEvaluationStep preparedArg, long iterationCacheSyncThreshold) {
		this(null, cmp, limit, reduced, preparedArg, iterationCacheSyncThreshold);
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet bs) {
		if (orderNode == null || !orderNode.isRuntimeTelemetryEnabled()) {
			return new OrderIterator(preparedArg.evaluate(bs), cmp, limit, reduced, iterationCacheSyncThreshold);
		}

		AtomicLong sortComparisons = new AtomicLong();
		Comparator<BindingSet> countingComparator = (left, right) -> {
			sortComparisons.incrementAndGet();
			return cmp.compare(left, right);
		};
		return new OrderIterator(preparedArg.evaluate(bs), countingComparator, limit, reduced,
				iterationCacheSyncThreshold) {
			private long rowsSorted;
			private long spillCount;
			private long spillBytes;

			@Override
			protected void onInputRowRead(BindingSet next) {
				rowsSorted++;
			}

			@Override
			protected void onSpillToDisk(int spilledRows, long spilledBytes) {
				spillCount++;
				spillBytes += Math.max(0L, spilledBytes);
			}

			@Override
			protected void onSortCompleted(long inputRows, long spillCount, long spillBytes) {
				this.rowsSorted = Math.max(this.rowsSorted, inputRows);
				this.spillCount = Math.max(this.spillCount, spillCount);
				this.spillBytes = Math.max(this.spillBytes, spillBytes);
			}

			@Override
			protected void handleClose() {
				try {
					orderNode.setLongMetricActual(TelemetryMetricNames.ROWS_SORTED_ACTUAL,
							Math.max(0L, orderNode.getLongMetricActual(TelemetryMetricNames.ROWS_SORTED_ACTUAL))
									+ rowsSorted);
					orderNode.setLongMetricActual(TelemetryMetricNames.SPILL_COUNT_ACTUAL,
							Math.max(0L, orderNode.getLongMetricActual(TelemetryMetricNames.SPILL_COUNT_ACTUAL))
									+ spillCount);
					orderNode.setLongMetricActual(TelemetryMetricNames.SPILL_BYTES_ACTUAL,
							Math.max(0L, orderNode.getLongMetricActual(TelemetryMetricNames.SPILL_BYTES_ACTUAL))
									+ spillBytes);
					orderNode.setLongMetricActual(TelemetryMetricNames.SORT_COMPARISONS_ACTUAL,
							Math.max(0L, orderNode.getLongMetricActual(TelemetryMetricNames.SORT_COMPARISONS_ACTUAL))
									+ sortComparisons.get());
				} finally {
					super.handleClose();
				}
			}
		};
	}
}
