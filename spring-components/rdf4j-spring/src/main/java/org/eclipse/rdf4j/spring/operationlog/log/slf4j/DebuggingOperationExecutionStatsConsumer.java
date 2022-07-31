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

package org.eclipse.rdf4j.spring.operationlog.log.slf4j;

import java.lang.invoke.MethodHandles;

import org.eclipse.rdf4j.spring.operationlog.log.OperationExecutionStats;
import org.eclipse.rdf4j.spring.operationlog.log.OperationExecutionStatsConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
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
