/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.io.IOException;
import java.util.Map;

import org.eclipse.rdf4j.console.Help;
import org.eclipse.rdf4j.console.Command;
import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.console.setting.ConsoleSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract command
 * 
 * @author Bart Hanssens
 */
public abstract class ConsoleCommand implements Command, Help {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleCommand.class);

	final ConsoleIO consoleIO;
	final ConsoleState state;

	final Map<String, ConsoleSetting> settings;

	/**
	 * Get console IO
	 * 
	 * @return console IO
	 */
	public ConsoleIO getConsoleIO() {
		return this.consoleIO;
	}

	/**
	 * Get console state
	 * 
	 * @return console state
	 */
	public ConsoleState getConsoleState() {
		return this.state;
	}

	/**
	 * Get console settings map
	 * 
	 * @return map of console settings
	 */
	public Map<String, ConsoleSetting> getConsoleSettings() {
		return this.settings;
	}

	/**
	 * Get short description, small enough to fit on one console row
	 * 
	 * @return
	 */
	@Override
	public String getHelpShort() {
		return "No help available";
	}

	/**
	 * Get long description
	 * 
	 * @return string, can be multiple lines
	 */
	@Override
	public String getHelpLong() {
		return "No additional help available";
	}

	@Override
	public String[] usesSettings() {
		return new String[0];
	}

	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 */
	public ConsoleCommand(ConsoleIO consoleIO) {
		this.consoleIO = consoleIO;
		this.state = null;
		this.settings = null;
	}

	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 * @param state
	 */
	public ConsoleCommand(ConsoleIO consoleIO, ConsoleState state) {
		this.consoleIO = consoleIO;
		this.state = state;
		this.settings = null;
	}

	/**
	 * Constructor
	 * 
	 * @param consoleIO console IO
	 * @param state     console state
	 * @param settings  console settings
	 */
	public ConsoleCommand(ConsoleIO consoleIO, ConsoleState state, Map<String, ConsoleSetting> settings) {
		this.consoleIO = consoleIO;
		this.state = state;
		this.settings = settings;
	}

	@Override
	public void execute(String... parameters) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	protected void writeln(String str) {
		consoleIO.writeln(str);
	}

	protected void writeInfo(String str) {
		consoleIO.writeln(str);
		LOGGER.info(str);
	}

	protected void writeError(String str) {
		consoleIO.writeln(str);
		LOGGER.warn(str);
	}

	protected void writeError(String str, Exception e) {
		consoleIO.writeln(str + ": " + e.getMessage());
		LOGGER.error(str, e);
	}
}
