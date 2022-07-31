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
package org.eclipse.rdf4j.console.command;

import java.util.Map;
import java.util.Objects;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.console.setting.ConsoleSetting;

/**
 * Set parameters command
 *
 * @author dale
 */
public class SetParameters extends ConsoleCommand {

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
		for (ConsoleSetting setting : settings.values()) {
			builder.append(setting.getHelpLong());
		}
		return PrintHelp.USAGE + "set                            Shows all parameter values\n" + builder.toString();
	}

	/**
	 * Constructor
	 *
	 * @param consoleIO
	 * @param state
	 * @param settings
	 */
	public SetParameters(ConsoleIO consoleIO, ConsoleState state, Map<String, ConsoleSetting> settings) {
		super(consoleIO, state, settings);
	}

	@Override
	public void execute(String... tokens) {
		switch (tokens.length) {
		case 0:
			writeln(getHelpLong());
			break;
		case 1:
			for (String setting : settings.keySet()) {
				showSetting(setting);
			}
			break;
		default:
			String param = tokens[1];
			int eqIdx = param.indexOf('=');
			if (eqIdx < 0) {
				showSetting(param);
			} else {
				String key = param.substring(0, eqIdx);
				// FIXME: somewhat ugly, join back together to set parameter which may contain spaces
				String values = String.join(" ", tokens);
				eqIdx = values.indexOf('=');
				setParameter(key, values.substring(eqIdx + 1));
			}
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
			String s = setting.getAsString();
			// quick and dirty wrapping of too long values
			if ((s.length() > 80 - 10) && s.contains(",")) {
				StringBuilder builder = new StringBuilder();
				for (String val : s.split(",")) {
					builder.append("\n    ").append(val);
				}
				s = builder.toString();
			}
			writeln(key + ": " + s);
		} else {
			writeError("Unknown parameter: " + key);
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
			try {
				setting.setFromString(value);
			} catch (IllegalArgumentException iae) {
				writeError(iae.getMessage());
			}
		} else {
			writeError("Unknown parameter: " + key);
		}
	}
}
