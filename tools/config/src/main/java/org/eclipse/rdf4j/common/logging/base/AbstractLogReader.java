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
import java.util.List;

import org.eclipse.rdf4j.common.logging.LogLevel;
import org.eclipse.rdf4j.common.logging.LogReader;
import org.eclipse.rdf4j.common.logging.LogRecord;

import ch.qos.logback.core.Appender;

/**
 * Abstract log reader
 */
public abstract class AbstractLogReader implements LogReader {

	@Override
	public abstract boolean hasNext();

	@Override
	public abstract LogRecord next();

	private int limit = 0;

	private int offset = 0;

	private Appender<?> appender;

	@Override
	public final void remove() {
		throw new UnsupportedOperationException("Removing log records is not supported.");
	}

	@Override
	public void setAppender(Appender<?> appender) {
		this.appender = appender;
	}

	@Override
	public Appender<?> getAppender() {
		return this.appender;
	}

	@Override
	public Date getEndDate() {
		return null;
	}

	@Override
	public LogLevel getLevel() {
		return null;
	}

	@Override
	public Date getStartDate() {
		return null;
	}

	@Override
	public void setEndDate(Date date) {
		throw new UnsupportedOperationException("Date ranges are not supported by this LogReader implementation!");
	}

	@Override
	public void setLevel(LogLevel level) {
		throw new UnsupportedOperationException("Level filter is not supported by this LogReader implementation!");
	}

	@Override
	public void setStartDate(Date date) {
		throw new UnsupportedOperationException("Date ranges are not supported by this LogReader implementation!");
	}

	@Override
	public boolean supportsDateRanges() {
		return false;
	}

	@Override
	public Date getMaxDate() {
		return null;
	}

	@Override
	public Date getMinDate() {
		return null;
	}

	@Override
	public boolean supportsLevelFilter() {
		return false;
	}

	@Override
	public String getThread() {
		return null;
	}

	@Override
	public void setThread(String threadname) {
		throw new UnsupportedOperationException("Thread filter is not supported by this LogReader implementation!");
	}

	@Override
	public boolean supportsThreadFilter() {
		return false;
	}

	/**
	 * Get the limit.
	 *
	 * @return limit
	 */
	@Override
	public int getLimit() {
		return limit;
	}

	/**
	 * Set the limit
	 *
	 * @param limit The limit to set.
	 */
	@Override
	public void setLimit(int limit) {
		this.limit = limit;
	}

	/**
	 * Get the offset
	 *
	 * @return offset.
	 */
	@Override
	public int getOffset() {
		return offset;
	}

	/**
	 * Set the offset
	 *
	 * @param offset The offset to set.
	 */
	@Override
	public void setOffset(int offset) {
		this.offset = offset;
	}

	@Override
	public List<String> getThreadNames() {
		return null;
	}
}
