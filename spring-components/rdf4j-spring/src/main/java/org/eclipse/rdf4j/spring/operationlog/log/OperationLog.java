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

package org.eclipse.rdf4j.spring.operationlog.log;

import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.spring.operationlog.log.slf4j.DebuggingOperationExecutionStatsConsumer;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class OperationLog {

	private OperationExecutionStatsConsumer statsConsumer;

	public OperationLog(OperationExecutionStatsConsumer statsConsumer) {
		Objects.requireNonNull(statsConsumer);
		this.statsConsumer = statsConsumer;
	}

	@Autowired(required = false)
	public void setStatsConsumer(OperationExecutionStatsConsumer statsConsumer) {
		this.statsConsumer = statsConsumer;
	}

	public OperationLog() {
		this.statsConsumer = new DebuggingOperationExecutionStatsConsumer();
	}

	public void runWithLog(Operation operation, Runnable action) {
		runWithLog(makeStats(operation), action);
	}

	public <T> T runWithLog(Operation operation, Supplier<T> supplier) {
		return runWithLog(makeStats(operation), supplier);
	}

	public void runWithLog(PseudoOperation operation, Runnable action) {
		runWithLog(makeStats(operation), action);
	}

	public <T> T runWithLog(PseudoOperation operation, Supplier<T> supplier) {
		return runWithLog(makeStats(operation), supplier);
	}

	private OperationExecutionStats makeStats(Operation operation) {
		Objects.requireNonNull(operation);
		return OperationExecutionStats.of(operation);
	}

	private OperationExecutionStats makeStats(PseudoOperation operation) {
		Objects.requireNonNull(operation);
		return OperationExecutionStats.of(operation);
	}

	private void runWithLog(OperationExecutionStats stats, Runnable action) {
		Objects.requireNonNull(action);
		try {
			action.run();
			stats.operationSuccessful();
		} catch (Throwable t) {
			stats.operationFailed();
			throw t;
		} finally {
			statsConsumer.consumeOperationExecutionStats(stats);
		}
	}

	private <T> T runWithLog(OperationExecutionStats stats, Supplier<T> supplier) {
		Objects.requireNonNull(supplier);
		try {
			T result = supplier.get();
			stats.operationSuccessful();
			return result;
		} catch (Throwable t) {
			stats.operationFailed();
			throw t;
		} finally {
			statsConsumer.consumeOperationExecutionStats(stats);
		}
	}
}
