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
package org.eclipse.rdf4j.common.app.logging.logback;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.common.app.logging.base.AbstractLogConfiguration;
import org.eclipse.rdf4j.common.app.util.ConfigurationUtil;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.common.logging.LogReader;
import org.eclipse.rdf4j.common.logging.file.logback.FileLogReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class LogbackConfiguration extends AbstractLogConfiguration {

	public static final String LOGGING_DIR_PROPERTY = "org.eclipse.rdf4j.common.logging.dir";

	private static final String LOGBACK_CONFIG_FILE = "logback.xml";

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private File configFile;

	private LogConfigurator configurator = null;

	public LogbackConfiguration() throws IOException {
		super();
		// USE init() FOR FURTHER CONFIGURATION
		// it will be called from the super constructor
	}

	@Override
	public void init() throws IOException {
		configFile = getConfigFile();

		load();

		logger.info("Logback logging API implementation is configured.");
		logger.debug("Log dir: {}", getLoggingDir().getAbsolutePath());

		save();
	}

	@Override
	public void load() throws IOException {
		try {
			if (System.getProperty(LOGGING_DIR_PROPERTY) == null) {
				System.setProperty(LOGGING_DIR_PROPERTY, getLoggingDir().getAbsolutePath());
			}
		} catch (SecurityException e) {
			System.out.println("Not allowed to read or write system property '" + LOGGING_DIR_PROPERTY + "'");
		}

		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		try {
			configurator = new LogConfigurator();
			configurator.setContext(lc);
			lc.reset();
			configurator.doConfigure(configFile);
		} catch (JoranException je) {
			System.out.println("Logback configuration error");
			je.printStackTrace();
			StatusPrinter.print(lc);
		}
	}

	@Override
	public void save() throws IOException {
		// nop
	}

	@Override
	public void destroy() {
		// look up all loggers in the logger context and close
		// all appenders configured for them.
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		lc.reset();
	}

	private File getConfigFile() throws IOException {
		File f = new File(getConfDir(), LOGBACK_CONFIG_FILE);
		if (!f.exists() || !f.canRead()) {
			String content = ConfigurationUtil.loadConfigurationContents(LOGBACK_CONFIG_FILE);
			content = content.replace("${logging.main.file}", LOG_FILE);
			content = content.replace("${logging.event.user.file}", USER_EVENT_LOG_FILE);
			content = content.replace("${logging.event.admin.file}", ADMIN_EVENT_LOG_FILE);
			content = content.replace("${logging.event.user.logger}", USER_EVENT_LOGGER_NAME);
			content = content.replace("${logging.event.admin.logger}", ADMIN_EVENT_LOGGER_NAME);
			if (!f.getParentFile().mkdirs() && !f.getParentFile().canWrite()) {
				throw new IOException("Not allowed to write logging configuration file to " + f.getParent());
			} else {
				IOUtil.writeString(content, f);
			}
		}
		return f;
	}

	@Override
	public LogReader getLogReader(String appender) {
		return this.configurator.getLogReader(appender);
	}

	@Override
	public LogReader getDefaultLogReader() {
		LogReader logReader = this.configurator.getDefaultLogReader();
		if (logReader != null) {
			return logReader;
		}
		return new FileLogReader(new File(getLoggingDir(), LOG_FILE));
	}
}
