/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import java.io.IOException;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;

/**
 * Disconnect command
 * 
 * @author Dale Visser
 */
public class Disconnect implements Command {

	private final ConsoleIO consoleIO;
	private final ConsoleState appInfo;
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
		return PrintHelp.USAGE
			+ "disconnect   Disconnects from the current set of repositories or server\n";
	}

	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 * @param appInfo
	 * @param close 
	 */
	Disconnect(ConsoleIO consoleIO, ConsoleState appInfo, Close close) {
		this.consoleIO = consoleIO;
		this.appInfo = appInfo;
		this.close = close;
	}

	/**
	 * Execute the command
	 * 
	 * @param verbose 
	 */
	public void execute(boolean verbose) {
		final RepositoryManager manager = this.appInfo.getManager();
		if (manager == null) {
			if (verbose) {
				consoleIO.writeln("Already disconnected");
			}
		}
		else {
			close.closeRepository(false);
			consoleIO.writeln("Disconnecting from " + this.appInfo.getManagerID());
			manager.shutDown();
			appInfo.setManager(null);
			appInfo.setManagerID(null);
		}
	}

	@Override
	public void execute(String... parameters) throws IOException {
		execute(true);
	}
}
