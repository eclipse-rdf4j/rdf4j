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
public class Sparql extends QueryEvaluator {
	@Override
	public String getName() {
		return "sparql";
	}
	
	@Override
	public String getHelpShort() {
		return "Evaluate a SPARQL query";
	}

	@Override
	public String getHelpLong() {
		return PrintHelp.USAGE 
			+ "sparql <query>                       Evaluates the SPARQL query on the currently open repository.\n"
			+ "sparql                               Starts multi-line input for large SPARQL queries.\n"
			+ "select|construct|ask|describe|prefix|base <rest-of-query>\n"
			+ "                                     Evaluates a SPARQL query on the currently open repository.\n";
	}

	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 * @param state
	 * @param params 
	 */
	public Sparql(ConsoleIO consoleIO, ConsoleState state, ConsoleParameters params) {
		super(consoleIO, state, params);
	}

	@Override
	public void execute(String... parameters) throws IOException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
}
