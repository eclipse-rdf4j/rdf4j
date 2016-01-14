/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.logging.base;

import java.util.Date;
import java.util.List;

import org.eclipse.rdf4j.common.logging.LogLevel;
import org.eclipse.rdf4j.common.logging.LogReader;
import org.eclipse.rdf4j.common.logging.LogRecord;

import ch.qos.logback.core.Appender;

public abstract class AbstractLogReader implements LogReader {
	
	public abstract boolean hasNext();

	public abstract LogRecord next();

	private int limit = 0;
	private int offset = 0;	

	private Appender<?> appender;	
		
	public final void remove() {
		throw new UnsupportedOperationException("Removing log records is not supported.");
	}
	
	public void setAppender(Appender<?> appender) {
		this.appender = appender;
	}
	
	public Appender<?> getAppender() {
		return this.appender;
	}	

	public Date getEndDate() {		
		return null;
	}

	public LogLevel getLevel() {
		return null;
	}

	public Date getStartDate() {
		return null;
	}

	public void setEndDate(Date date) {
		throw new UnsupportedOperationException("Date ranges are not supported by this LogReader implementation!");
	}

	public void setLevel(LogLevel level) {
		throw new UnsupportedOperationException("Level filter is not supported by this LogReader implementation!");
	}

	public void setStartDate(Date date) {
		throw new UnsupportedOperationException("Date ranges are not supported by this LogReader implementation!");
	}

	public boolean supportsDateRanges() {
		return false;
	}
	
	public Date getMaxDate() {		
		return null;
	}

	public Date getMinDate() {
		return null;
	}

	public boolean supportsLevelFilter() {
		return false;
	}

	public String getThread() {
		return null;
	}

	public void setThread(String threadname) {
		throw new UnsupportedOperationException("Thread filter is not supported by this LogReader implementation!");
	}

	public boolean supportsThreadFilter() {
		return false;
	}
	
	/**
	 * @return Returns the limit.
	 */
	public int getLimit() {
		return limit;
	}
	
	/**
	 * @param limit The limit to set.
	 */
	public void setLimit(int limit) {
		this.limit = limit;
	}
	
	/**
	 * @return Returns the offset.
	 */
	public int getOffset() {
		return offset;
	}
	
	/**
	 * @param offset The offset to set.
	 */
	public void setOffset(int offset) {
		this.offset = offset;
	}

	public List<String> getThreadNames() {		
		return null;
	}
	
}
