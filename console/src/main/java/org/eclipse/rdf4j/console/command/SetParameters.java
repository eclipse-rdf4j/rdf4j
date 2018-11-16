/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.util.Map;
import java.util.Objects;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleParameters;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.console.setting.ConsoleSetting;

/**
 * Set parameters command
 * 
 * @author dale
 */
public class SetParameters extends ConsoleCommand {
	private final Map<String,ConsoleSetting> settings;
	
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
		StringBuilder builder = new StringBuilder(settings.size() * 80);
		for (ConsoleSetting setting: settings.values()) {
			builder.append(setting.getHelpShort()).append("\n");
		}

		return PrintHelp.USAGE
			+ "set                            Shows all parameter values\n"
			+ builder.toString();
	}
	
	
	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 * @param state
	 * @param parameters 
	 */
	@Deprecated
	public SetParameters(ConsoleIO consoleIO, ConsoleState state, ConsoleParameters parameters) {
		super(consoleIO, state);
		this.settings = convertParams(parameters);
	}

	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 * @param state
	 * @param settings 
	 */
	public SetParameters(ConsoleIO consoleIO, ConsoleState state, Map<String,ConsoleSetting> settings) {
		super(consoleIO, state);
		this.settings = settings;
	}
	
	@Override
	public void execute(String... tokens) {
		if (tokens.length == 1) {
			for (String setting: settings.keySet()) {
				showSetting(setting);
			}
		} else if (tokens.length == 2) {
			String param = tokens[1];
			int eqIdx = param.indexOf('=');
			if (eqIdx < 0) {
				showSetting(param);
			} else {
				String key = param.substring(0, eqIdx);
				String value = param.substring(eqIdx + 1);
				setParameter(key, value);
			}
		} else {
			consoleIO.writeln(getHelpLong());
		}
	}

	/**
	 * Show parameter
	 * 
	 * @param key parameter key
	 */
	private void showSetting(String key) {
		String str = key.toLowerCase();
		
		ConsoleSetting setting = settings.get(str);
		if (setting != null) {
			consoleIO.writeln(key + " : " + setting.getAsString());
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
	private void setParameter(String key, String value) {
		Objects.requireNonNull(key, "parameter key was missing");
		Objects.requireNonNull(value, "parameter value was missing");
		
		String str = key.toLowerCase();

		ConsoleSetting setting = settings.get(str);
		if (setting != null) {
			setting.setFromString(value);
		} else {
			consoleIO.writeError("unknown parameter: " + key);
		}
	}
}
