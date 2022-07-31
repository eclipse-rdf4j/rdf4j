/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.monitoring;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.federated.FedXConfig;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience class which writes the query backlog a logger with the name "QueryLog". The logger is configured using
 * the available logging framework.
 * <p>
 * Requires monitoring to be enabled:
 * </p>
 *
 * <ul>
 * <li>{@link FedXConfig#isEnableMonitoring()}</li>
 * <li>{@link FedXConfig#isLogQueries()}</li>
 * </ul>
 *
 * @author Andreas Schwarte
 *
 */
public class QueryLog {
	private static final Logger log = LoggerFactory.getLogger(QueryLog.class);

	private final AtomicBoolean active = new AtomicBoolean(false);
	private Logger queryLog;

	public QueryLog() throws IOException {
		log.info("Initializing logging of queries");
		initQueryLog();
	}

	private void initQueryLog() throws IOException {

		queryLog = LoggerFactory.getLogger("QueryLog");

		// activate the given logger
		active.set(true);
	}

	public void logQuery(QueryInfo query) {
		if (active.get()) {
			queryLog.info(query.getQuery().replace("\r\n", " ").replace("\n", " "));
		}
		if (log.isTraceEnabled()) {
			log.trace("#Query: " + query.getQuery());
		}
	}

}
