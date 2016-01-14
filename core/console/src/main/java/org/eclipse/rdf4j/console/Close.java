/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import org.eclipse.rdf4j.repository.Repository;

/**
 * @author Dale Visser
 */
public class Close implements Command {

	private final ConsoleIO consoleIO;

	private final ConsoleState appInfo;

	Close(ConsoleIO consoleIO, ConsoleState appInfo) {
		this.consoleIO = consoleIO;
		this.appInfo = appInfo;
	}

	public void execute(String... tokens) {
		if (tokens.length == 1) {
			closeRepository(true);
		}
		else {
			consoleIO.writeln(PrintHelp.CLOSE);
		}
	}

	protected void closeRepository(final boolean verbose) {
		final Repository repository = this.appInfo.getRepository();
		if (repository == null) {
			if (verbose) {
				consoleIO.writeln("There are no open repositories that can be closed");
			}
		}
		else {
			consoleIO.writeln("Closing repository '" + this.appInfo.getRepositoryID() + "'...");
			this.appInfo.setRepository(null);
			this.appInfo.setRepositoryID(null);
		}
	}

}
