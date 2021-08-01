package org.eclipse.rdf4j.spring.operationlog.log;

public interface OperationExecutionStatsConsumer {
	void consumeOperationExecutionStats(OperationExecutionStats operationExecutionStats);
}
