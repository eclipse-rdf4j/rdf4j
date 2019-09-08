/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.monitoring;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.structures.QueryInfo;

/**
 * Convenience class which writes the query backlog a logger with the name
 * "QueryLog". The logger is configured using the available logging framework.
 * <p>
 * Requires monitoring to be enabled:
 * </p>
 * 
 * <ul>
 * <li>{@link Config#isEnableMonitoring()}</li>
 * <li>{@link Config#isLogQueries()}</li>
 * </ul>
 * 
 * @author Andreas Schwarte
 *
 */
public class QueryLog
{
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
