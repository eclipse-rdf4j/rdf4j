/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Log record interface
 */
public interface LogRecord {

	public static final SimpleDateFormat ISO8601_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

	/**
	 * Get log level
	 *
	 * @return log level enumeration
	 */
	public LogLevel getLevel();

	/**
	 * Get date time
	 *
	 * @return date
	 */
	public Date getTime();

	/**
	 * Get thread name
	 *
	 * @return thread name
	 */
	public String getThreadName();

	/**
	 * Get message
	 *
	 * @return text
	 */
	public String getMessage();

	/**
	 * Get stack trace as list of strings
	 *
	 * @return list of strings
	 */
	public List<String> getStackTrace();
}
