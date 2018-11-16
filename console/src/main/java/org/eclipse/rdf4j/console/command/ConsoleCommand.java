/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.console.Help;
import org.eclipse.rdf4j.console.Command;
import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleParameters;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.console.setting.ConsoleSetting;
import org.eclipse.rdf4j.console.setting.ConsoleWidth;
import org.eclipse.rdf4j.console.setting.QueryPrefix;
import org.eclipse.rdf4j.console.setting.ShowPrefix;

/**
 * Abstract command
 * 
 * @author Bart Hanssens
 */
public abstract class ConsoleCommand implements Command, Help {
	final ConsoleIO consoleIO;
	final ConsoleState state;
	
	/**
	 * Convert old console parameters to new way (map) of storing parameters.
	 * 
	 * @param parameters console parameters
	 * @return map of console settings
	 */
	@Deprecated
	public static Map<String,ConsoleSetting> convertParams(ConsoleParameters parameters) {
		Map<String,ConsoleSetting> settings = new HashMap<>();
		
		settings.put(ConsoleWidth.NAME, new ConsoleWidth(parameters.getWidth()));
		settings.put(QueryPrefix.NAME, new QueryPrefix(parameters.isQueryPrefix()));
		settings.put(ShowPrefix.NAME, new ShowPrefix(parameters.isShowPrefix()));
		
		return settings;
	}
	
	/**
	 * Get console IO
	 * 
	 * @return 
	 */
	public ConsoleIO getConsoleIO() {
		return this.consoleIO;
	}
	
	/**
	 * Get console state
	 * 
	 * @return 
	 */
	public ConsoleState getConsoleState() {
		return this.state;
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
	public void execute(String... parameters) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Constructor
	 * 
	 * @param consoleIO 
	 */
	public ConsoleCommand(ConsoleIO consoleIO) {
		this.consoleIO = consoleIO;
		this.state = null;
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
	}

}
