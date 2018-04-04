/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.io.IOException;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleParameters;
import org.eclipse.rdf4j.console.ConsoleState;

/**
 * Abstract command
 * 
 * @author Bart Hanssens
 */
public class Serql extends QueryEvaluator {
	@Override
	public String getName() {
		return "serql";
	}
	
	@Override
	public String getHelpShort() {
		return "Evaluate a SeRQL query";
	}

	@Override
	public String getHelpLong() {
		return PrintHelp.USAGE 
			+ "serql <query>                 Evaluates the SeRQL query on the currently open repository\n"
			+ "serql                         Starts multi-line input for large SeRQL queries.\n";
	}

	@Override
	public void execute(String... parameters) throws IOException {
	}

	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 * @param state
	 * @param params 
	 */
	public Serql(ConsoleIO consoleIO, ConsoleState state, ConsoleParameters params) {
		super(consoleIO, state, params);
	}
}
