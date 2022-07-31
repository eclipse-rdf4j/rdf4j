/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.console.setting;

import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Console log level setting.
 *
 * @author Bart Hanssens
 */
public class LogLevel extends ConsoleSetting<String> {
	public final static String NAME = "log";

	private static final BiMap<String, Level> LOG_LEVELS;

	static {
		ImmutableBiMap.Builder<String, Level> logLevels = ImmutableBiMap.<String, Level>builder();

		logLevels.put("none", Level.OFF);
		logLevels.put("error", Level.ERROR);
		logLevels.put("warning", Level.WARN);
		logLevels.put("info", Level.INFO);
		logLevels.put("debug", Level.DEBUG);
		LOG_LEVELS = logLevels.build();
	}

	@Override
	public String getHelpLong() {
		return "set log=<level>                Set the logging level (none, error, warning, info or debug)\n";
	}

	/**
	 * Constructor
	 */
	public LogLevel() {
		super("info");
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String get() {
		Logger logbackRootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		Level currentLevel = logbackRootLogger.getLevel();
		return LOG_LEVELS.inverse().getOrDefault(currentLevel, currentLevel.levelStr);
	}

	@Override
	public void set(String value) throws IllegalArgumentException {
		// Assume Logback
		Level logLevel = LOG_LEVELS.get(value.toLowerCase());
		if (logLevel != null) {
			Logger logbackRootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
			logbackRootLogger.setLevel(logLevel);
		} else {
			throw new IllegalArgumentException("unknown logging level: " + value);
		}
	}

	@Override
	public void setFromString(String value) throws IllegalArgumentException {
		set(value);
	}
}
