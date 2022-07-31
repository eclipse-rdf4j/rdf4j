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

import java.io.IOException;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;

/**
 * Disconnect command
 *
 * @author Dale Visser
 */
public class Disconnect extends ConsoleCommand {
	private final Close close;

	@Override
	public String getName() {
		return "disconnect";
	}

	@Override
	public String getHelpShort() {
		return "Disconnects from the current set of repositories";
	}

	@Override
	public String getHelpLong() {
		return PrintHelp.USAGE + "disconnect   Disconnects from the current set of repositories or server\n";
	}

	/**
	 * Constructor
	 *
	 * @param consoleIO
	 * @param state
	 * @param close
	 */
	public Disconnect(ConsoleIO consoleIO, ConsoleState state, Close close) {
		super(consoleIO, state);
		this.close = close;
	}

	/**
	 * Execute the command
	 *
	 * @param verbose
	 */
	public void execute(boolean verbose) {
		final RepositoryManager manager = this.state.getManager();
		if (manager == null) {
			if (verbose) {
				writeln("Already disconnected");
			}
		} else {
			close.closeRepository(false);
			writeln("Disconnecting from " + this.state.getManagerID());
			manager.shutDown();
			state.setManager(null);
			state.setManagerID(null);
		}
	}

	@Override
	public void execute(String... parameters) throws IOException {
		execute(true);
	}
}
