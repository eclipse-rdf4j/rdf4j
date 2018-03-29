/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import java.util.Objects;

import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableBiMap.Builder;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Set parameters command
 * 
 * @author dale
 */
public class SetParameters extends ConsoleCommand {

	private static final String QUERYPREFIX_COMMAND = "queryprefix";
	private static final String SHOWPREFIX_COMMAND = "showprefix";
	private static final String WIDTH_COMMAND = "width";
	private static final String LOG_COMMAND = "log";

	private static final BiMap<String, Level> LOG_LEVELS;
	
	static {
		Builder<String, Level> logLevels = ImmutableBiMap.<String, Level>builder();

		logLevels.put("none", Level.OFF);
		logLevels.put("error", Level.ERROR);
		logLevels.put("warning", Level.WARN);
		logLevels.put("info", Level.INFO);
		logLevels.put("debug", Level.DEBUG);
		LOG_LEVELS = logLevels.build();
	}

	@Override
	public  String getName() {
		return "set";
	}
	
	@Override
	public String getHelpShort() {
		return "Allows various console parameters to be set";
	}
	
	@Override
	public String getHelpLong() {
		return  PrintHelp.USAGE
			+ "set                            Shows all parameter values\n"
			+ "set width=<number>             Set the width for query result tables\n"
			+ "set log=<level>                Set the logging level (none, error, warning, info or debug)\n"
			+ "set showPrefix=<true|false>    Toggles use of prefixed names in query results\n"
			+ "set queryPrefix=<true|false>   Toggles automatic use of known namespace prefixes in queries\n";

	}
	
	private final ConsoleParameters parameters;

	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 * @param state
	 * @param parameters 
	 */
	SetParameters(ConsoleIO consoleIO, ConsoleState state, ConsoleParameters parameters) {
		super(consoleIO, state);
		this.parameters = parameters;
	}

	@Override
	public void execute(String... tokens) {
		if (tokens.length == 1) {
			showAllParameters();
		} else if (tokens.length == 2) {
			final String param = tokens[1];
			final int eqIdx = param.indexOf('=');
			if (eqIdx < 0) {
				showParameter(param);
			} else {
				final String key = param.substring(0, eqIdx);
				final String value = param.substring(eqIdx + 1);
				setParameter(key, value);
			}
		} else {
			consoleIO.writeln(getHelpLong());
		}
	}

	/**
	 * Show all parameters
	 */
	private void showAllParameters() {
		showLogLevel();
		showWidth();
		showPrefix();
		showQueryPrefix();
	}

	/**
	 * Show parameter
	 * 
	 * @param key parameter key
	 */
	private void showParameter(String key) {
		if (LOG_COMMAND.equalsIgnoreCase(key)) {
			showLogLevel();
		} else if (WIDTH_COMMAND.equalsIgnoreCase(key)) {
			showWidth();
		} else if (SHOWPREFIX_COMMAND.equalsIgnoreCase(key)) {
			showPrefix();
		} else if (QUERYPREFIX_COMMAND.equalsIgnoreCase(key)) {
			showQueryPrefix();
		} else {
			consoleIO.writeError("unknown parameter: " + key);
		}
	}

	/**
	 * Set parameter
	 * 
	 * @param key
	 * @param value 
	 */
	private void setParameter(final String key, final String value) {
		Objects.requireNonNull(key, "parameter key was missing");
		Objects.requireNonNull(value, "parameter value was missing");
		
		if (LOG_COMMAND.equalsIgnoreCase(key)) {
			setLog(value);
		} else if (WIDTH_COMMAND.equalsIgnoreCase(key)) {
			setWidth(value);
		} else if (SHOWPREFIX_COMMAND.equalsIgnoreCase(key)) {
			setShowPrefix(value);
		} else if (QUERYPREFIX_COMMAND.equalsIgnoreCase(key)) {
			setQueryPrefix(value);
		} else {
			consoleIO.writeError("unknown parameter: " + key);
		}
	}

	/**
	 * Show log level
	 */
	private void showLogLevel() {
		Logger logbackRootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		Level currentLevel = logbackRootLogger.getLevel();

		String levelString = LOG_LEVELS.inverse().getOrDefault(currentLevel, currentLevel.levelStr);

		consoleIO.writeln("log: " + levelString);
	}

	/**
	 * Set log level
	 * 
	 * @param value 
	 */
	private void setLog(final String value) {
		// Assume Logback
		Level logLevel = LOG_LEVELS.get(value.toLowerCase());
		if (logLevel != null) {
			Logger logbackRootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
			logbackRootLogger.setLevel(logLevel);
		} else {
			consoleIO.writeError("unknown logging level: " + value);
		}
	}

	/**
	 * Show column width
	 */
	private void showWidth() {
		consoleIO.writeln("width: " + parameters.getWidth());
	}

	/**
	 * Set column width
	 * 
	 * @param value 
	 */
	private void setWidth(final String value) {
		try {
			final int width = Integer.parseInt(value);
			if (width > 0) {
				parameters.setWidth(width);
			} else {
				consoleIO.writeError("Width must be larger than 0");
			}
		} catch (NumberFormatException e) {
			consoleIO.writeError("Width must be a positive number");
		}
	}

	/**
	 * Show prefix
	 */
	private void showPrefix() {
		consoleIO.writeln("showPrefix: " + parameters.isShowPrefix());
	}

	/**
	 * Set prefix
	 * 
	 * @param value 
	 */
	private void setShowPrefix(final String value) {
		parameters.setShowPrefix(Boolean.parseBoolean(value));
	}

	/**
	 * Show query prefix
	 */
	private void showQueryPrefix() {
		consoleIO.writeln("queryPrefix: " + parameters.isQueryPrefix());
	}

	/**
	 * Set query prefix
	 * 
	 * @param value 
	 */
	private void setQueryPrefix(final String value) {
		parameters.setQueryPrefix(Boolean.parseBoolean(value));
	}
}
