package org.eclipse.rdf4j.spring.operationlog.log.slf4j;

import java.lang.invoke.MethodHandles;

import org.eclipse.rdf4j.spring.operationlog.log.OperationExecutionStats;
import org.eclipse.rdf4j.spring.operationlog.log.OperationExecutionStatsConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebuggingOperationExecutionStatsConsumer implements OperationExecutionStatsConsumer {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public DebuggingOperationExecutionStatsConsumer() {
	}

	@Override
	public void consumeOperationExecutionStats(OperationExecutionStats operationExecutionStats) {
		if (logger.isDebugEnabled()) {
			logger.debug(
					"query duration: {} millis; bindingshash: {}; query: {}",
					operationExecutionStats.getQueryDuration(),
					operationExecutionStats.getBindingsHashCode(),
					operationExecutionStats.getOperation());
		}
	}
}
