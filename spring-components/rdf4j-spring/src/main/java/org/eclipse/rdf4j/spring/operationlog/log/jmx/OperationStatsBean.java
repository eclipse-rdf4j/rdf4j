/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.spring.operationlog.log.jmx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.spring.operationlog.log.OperationExecutionStats;
import org.eclipse.rdf4j.spring.operationlog.log.OperationExecutionStatsConsumer;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class OperationStatsBean implements OperationStatsMXBean, OperationExecutionStatsConsumer {

	private Map<String, AggregatedOperationStats> stats = new HashMap<>();

	private final ExecutorService executorService = Executors.newSingleThreadExecutor();

	@Override
	public List<AggregatedOperationStats> getAggregatedOperationStats() {
		return stats.values()
				.stream()
				.sorted(
						(l, r) -> {
							int cmp = r.getCount() - l.getCount();
							if (cmp != 0) {
								return cmp;
							}
							return (int) (r.getCumulativeTime() - l.getCumulativeTime());
						})
				.collect(Collectors.toList());
	}

	@Override
	public int getDistinctOperationCount() {
		return stats.size();
	}

	@Override
	public int getDistinctOperationExecutionCount() {
		return stats.values()
				.stream()
				.mapToInt(AggregatedOperationStats::getUniqueBindingsCount)
				.sum();
	}

	@Override
	public int getTotalOperationExecutionCount() {
		return stats.values().stream().mapToInt(AggregatedOperationStats::getCount).sum();
	}

	@Override
	public long getTotalOperationExecutionTime() {
		return stats.values().stream().mapToLong(AggregatedOperationStats::getCumulativeTime).sum();
	}

	@Override
	public int getTotalFailedOperationExecutionCount() {
		return stats.values().stream().mapToInt(AggregatedOperationStats::getFailed).sum();
	}

	@Override
	public void reset() {
		executorService.execute(
				() -> {
					Map<String, AggregatedOperationStats> old = stats;
					stats = new HashMap<>();
					old.clear();
				});
	}

	@Override
	public void consumeOperationExecutionStats(OperationExecutionStats operationExecutionStats) {
		executorService.execute(
				() -> {
					Map<String, AggregatedOperationStats> newStats = new HashMap<>(stats);
					AggregatedOperationStats aggregated = stats.get(operationExecutionStats.getOperation());
					if (aggregated == null) {
						aggregated = AggregatedOperationStats.build(operationExecutionStats);
					} else {
						aggregated = aggregated.buildNext(operationExecutionStats);
					}
					newStats.put(operationExecutionStats.getOperation(), aggregated);
					stats = newStats;
				});
	}
}
