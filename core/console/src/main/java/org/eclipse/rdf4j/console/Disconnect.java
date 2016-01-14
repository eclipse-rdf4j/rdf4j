/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import org.eclipse.rdf4j.repository.manager.RepositoryManager;


/**
 *
 * @author Dale Visser
 */
public class Disconnect {
	
	private final ConsoleIO consoleIO;
	private final ConsoleState appInfo;
	private final Close close;

	Disconnect(ConsoleIO consoleIO, ConsoleState appInfo, Close close){
		this.consoleIO = consoleIO;
		this.appInfo = appInfo;
		this.close = close;
	}

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
}
