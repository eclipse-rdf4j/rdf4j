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

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;

/**
 * Print command
 *
 * @author Dale Visser
 */
public class PrintInfo extends ConsoleCommand {

	@Override
	public String getName() {
		return "info";
	}

	@Override
	public String getHelpShort() {
		return "Shows info about the console";
	}

	@Override
	public String getHelpLong() {
		return PrintHelp.USAGE + "info                  Shows information about the console\n";
	}

	/**
	 * Constructor
	 *
	 * @param consoleIO
	 * @param state
	 */
	public PrintInfo(ConsoleIO consoleIO, ConsoleState state) {
		super(consoleIO, state);
	}

	@Override
	public void execute(String... parameters) {
		writeln(state.getApplicationName());
		writeln("Data dir: " + state.getDataDirectory());
		String managerID = state.getManagerID();
		writeln("Connected to: " + (managerID == null ? "-" : managerID));
	}
}
