/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.util.Objects;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleParameters;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.console.setting.LogLevel;

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
	
	@Override
	public String getName() {
		return "set";
	}
	
	@Override
	public String getHelpShort() {
		return "Allows various console parameters to be set";
	}
	
	@Override
	public String getHelpLong() {
		return PrintHelp.USAGE
			+ "set                            Shows all parameter values\n"
			+ "set width=<number>             Set the width for query result tables\n"
			+ "set log=<level>                Set the logging level (none, error, warning, info or debug)\n"
			+ "set showPrefix=<true|false>    Toggles use of prefixed names in query results\n"
			+ "set queryPrefix=<true|false>   Toggles automatic use of known namespace prefixes in queries\n";

	}
	
	private final ConsoleParameters parameters;
	private final LogLevel logLevel = new LogLevel();
	
	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 * @param state
	 * @param parameters 
	 */
	public SetParameters(ConsoleIO consoleIO, ConsoleState state, ConsoleParameters parameters) {
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
		String str = key.toLowerCase();
		
		switch(str) {
			case LOG_COMMAND:
				showLogLevel();
				break;
			case WIDTH_COMMAND:
				showWidth();
				break;
			case QUERYPREFIX_COMMAND:
				showQueryPrefix();
				break;
			case SHOWPREFIX_COMMAND:
				showPrefix();
				break;
			default:
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
		
		String str = key.toLowerCase();
		
		switch(str) {
			case LOG_COMMAND:
				setLog(value);
				break;
			case WIDTH_COMMAND:
				setWidth(value);
				break;
			case QUERYPREFIX_COMMAND:
				setQueryPrefix(value);
				break;
			case SHOWPREFIX_COMMAND:
				setShowPrefix(value);
				break;
			default:
				consoleIO.writeError("unknown parameter: " + key);
		}
	}

	/**
	 * Show log level
	 */
	private void showLogLevel() {
		consoleIO.writeln("log: " + logLevel.get());
	}

	/**
	 * Set log level
	 * 
	 * @param value 
	 */
	private void setLog(final String value) {
		try {
			logLevel.set(value);
		} catch (IllegalArgumentException iae) {
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
