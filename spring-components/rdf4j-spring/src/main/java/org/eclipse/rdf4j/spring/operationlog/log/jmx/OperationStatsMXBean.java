package org.eclipse.rdf4j.spring.operationlog.log.jmx;

import java.util.List;

public interface OperationStatsMXBean {
	void reset();

	int getDistinctOperationCount();

	int getDistinctOperationExecutionCount();

	int getTotalOperationExecutionCount();

	int getTotalFailedOperationExecutionCount();

	long getTotalOperationExecutionTime();

	List<AggregatedOperationStats> getAggregatedOperationStats();
}
