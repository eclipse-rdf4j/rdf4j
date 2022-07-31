/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.common.logging.base;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.common.logging.LogLevel;
import org.eclipse.rdf4j.common.logging.LogRecord;

/**
 * Simple log record, containing only the basics
 */
public class SimpleLogRecord implements LogRecord {

	private LogLevel level;
	private String message;
	private List<String> stackTrace;
	private String threadName;
	private Date time;

	@Override
	public LogLevel getLevel() {
		return level;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public List<String> getStackTrace() {
		return stackTrace;
	}

	@Override
	public String getThreadName() {
		return threadName;
	}

	@Override
	public Date getTime() {
		return time;
	}

	/**
	 * Set log level
	 *
	 * @param level
	 */
	public void setLevel(LogLevel level) {
		this.level = level;
	}

	/**
	 * Set message
	 *
	 * @param message text
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Set stack trace as list of strings
	 *
	 * @param stackTrace list of strings
	 */
	public void setStackTrace(List<String> stackTrace) {
		this.stackTrace = stackTrace;
	}

	/**
	 * Set thread name
	 *
	 * @param threadName
	 */
	public void setThreadName(String threadName) {
		this.threadName = threadName;
	}

	/**
	 * Set date time
	 *
	 * @param time time
	 */
	public void setTime(Date time) {
		this.time = time;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(level);
		result.append(" ");
		result.append(LogRecord.ISO8601_TIMESTAMP_FORMAT.format(time));
		result.append(" (");
		result.append(threadName);
		result.append("): ");
		result.append(message);
		Iterator<String> tracerator = stackTrace.iterator();
		if (tracerator.hasNext()) {
			result.append("\n\t");
			result.append(tracerator.next());
			if (tracerator.hasNext()) {
				result.append("\n\t");
				result.append(tracerator.next());
				result.append("\n\t... " + (stackTrace.size() - 2) + " more lines");
			}
		}
		return result.toString();
	}
}
