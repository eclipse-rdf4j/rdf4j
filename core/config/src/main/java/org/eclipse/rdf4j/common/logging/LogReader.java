/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.logging;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import ch.qos.logback.core.Appender;

public interface LogReader extends Iterator<LogRecord> {

	/**
	 * Initialize the log reader.
	 * 
	 * @throws Exception
	 */
	public void init()
		throws Exception;

	/**
	 * Destroy the log reader and release all used resources.
	 * 
	 * @throws Exception
	 */
	public void destroy()
		throws Exception;

	/**
	 * Logging appender associated with this reader.
	 * 
	 * @param appender
	 *        logging appender associated with this reader
	 */
	public void setAppender(Appender<?> appender);

	/**
	 * Logging appender associated with this reader.
	 * 
	 * @return logging appender associated with this reader.
	 */
	public Appender<?> getAppender();

	/**
	 * Max. number of records returned by this log reader. Zero value (default)
	 * indicates no limit.
	 * 
	 * @param limit
	 *        max. number of records returned by this log reader.
	 */
	public void setLimit(int limit);

	/**
	 * Max. number of records returned by this log reader.
	 * 
	 * @return max. number of records returned by this log reader or zero value
	 *         if no limit has been set.
	 */
	public int getLimit();

	/**
	 * Check if more records are available after limit is reached.
	 * 
	 * @return true if more records are available
	 */
	public boolean isMoreAvailable();

	/**
	 * Index of the first record returned by this log reader.
	 * 
	 * @param offset
	 *        index of the first record returned by this log reader.
	 */
	public void setOffset(int offset);

	/**
	 * Index of the first record returned by this log reader.
	 * 
	 * @return index of the first record returned by this log reader.
	 */
	public int getOffset();

	/**
	 * Test if this LogReader implementation supports level-based records
	 * filtering.
	 * 
	 * @return true if level filtering is supported.
	 */
	public boolean supportsLevelFilter();

	/**
	 * Level of the log records returned by this log reader.
	 */
	public void setLevel(LogLevel level);

	/**
	 * Level of the log records returned by this log reader.
	 * 
	 * @return Level of the log records returned by this log reader or 'null' if
	 *         no level filter has been set.
	 */
	public LogLevel getLevel();

	/**
	 * Test if this LogReader implementation supports thread-based records
	 * filtering.
	 * 
	 * @return true if thread filtering is supported.
	 */
	public boolean supportsThreadFilter();

	/**
	 * Thread name of the log records returned by this log reader.
	 */
	public void setThread(String threadname);

	/**
	 * Thread name of the log records returned by this log reader.
	 * 
	 * @return thread name of the log records returned by this log reader or
	 *         'null' if no thread filter has been set.
	 */
	public String getThread();

	/**
	 * All available thread names of the log records.
	 * 
	 * @return a List of thread names of the log records.
	 */
	public List<String> getThreadNames();

	/**
	 * Test if this LogReader implementation supports date-based records
	 * filtering.
	 * 
	 * @return true if date filtering is supported
	 */
	public boolean supportsDateRanges();

	/**
	 * Start (earliest) date of the log records returned by this log reader.
	 */
	public void setStartDate(Date date);

	/**
	 * Start (earliest) date of the log records returned by this log reader.
	 * 
	 * @return Start date of the log records or 'null' if no start date has been
	 *         set
	 */
	public Date getStartDate();

	/**
	 * End (latest) date of of the log records returned by this log reader.
	 */
	public void setEndDate(Date date);

	/**
	 * End (latest) date of of the log records returned by this log reader.
	 * 
	 * @return End date of the log records or 'null' if no end date has been set
	 */
	public Date getEndDate();

	/**
	 * Min (earliest) available date of the log records.
	 */
	public Date getMinDate();

	/**
	 * Max (latest) available date of the log records.
	 */
	public Date getMaxDate();

}
