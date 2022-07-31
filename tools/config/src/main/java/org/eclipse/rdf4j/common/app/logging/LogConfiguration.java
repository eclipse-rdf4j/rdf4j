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
package org.eclipse.rdf4j.common.app.logging;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.common.app.AppConfiguration;
import org.eclipse.rdf4j.common.app.config.Configuration;
import org.eclipse.rdf4j.common.logging.LogReader;

/**
 * Configuration settings for application logging.
 *
 * @author Herko ter Horst
 */
public interface LogConfiguration extends Configuration {

	String LOGGING_DIR = "logs";
	String LOG_FILE = "main.log";

	String USER_EVENT_LOG_FILE = "user-event.log";
	String ADMIN_EVENT_LOG_FILE = "admin-event.log";

	String USER_EVENT_LOGGER_NAME = "event.user";
	String ADMIN_EVENT_LOGGER_NAME = "event.admin";

	/**
	 * Set the base location on the file system for logging configuration and data
	 *
	 * @param baseDir the base location on the file system for logging configuration and data
	 * @throws IOException
	 */
	void setBaseDir(File baseDir) throws IOException;

	/**
	 * The base location on the file system for logging configuration and data
	 *
	 * @return the base location on the file system for logging configuration and data
	 */
	File getBaseDir();

	/**
	 * The location on the file system where logging configuration is stored.
	 *
	 * @return the location on the file system where logging configuration is stored
	 */
	File getConfDir();

	/**
	 * The location on the file system where logging data is stored.
	 *
	 * @return the location on the file system where logging data is stored
	 */
	File getLoggingDir();

	/**
	 * A reader that can read logging information as stored by the specific logger's appender.
	 *
	 * @param appender Name of the appender to which the LogReader is attached
	 * @return a reader that can read logging information as stored by the logger configured through this
	 *         LogConfiguration
	 */
	LogReader getLogReader(String appender);

	/**
	 * Default (fallback) LogReader instance.
	 *
	 * @return default (fallback) LogReader instance.
	 */
	LogReader getDefaultLogReader();

	/**
	 * Is debug logging enabled?
	 *
	 * @return true if debug logging is enabled, false otherwise
	 */
	boolean isDebugLoggingEnabled();

	/**
	 * Enable or disable debug logging.
	 *
	 * @param enabled set to true if debug logging should be enabled, set to false otherwise
	 */
	void setDebugLoggingEnabled(boolean enabled);

	/**
	 * Set application configuration
	 *
	 * @param config application configuration
	 */
	void setAppConfiguration(AppConfiguration config);

	/**
	 * Get application configuration
	 *
	 * @return application configuration
	 */
	AppConfiguration getAppConfiguration();

}
