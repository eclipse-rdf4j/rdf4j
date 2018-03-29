/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import java.util.Locale;

/**
 * Prints available command and options to the console.
 * 
 * @author Dale Visser
 */
public class PrintHelp implements Command {
	private final ConsoleIO consoleIO;
	private final ConsoleState state;
	
	public static final String USAGE = "Usage:\n";


	protected static final String SPARQL = USAGE
			+ "sparql <query>                       Evaluates the SPARQL query on the currently open repository.\n"
			+ "sparql                               Starts multi-line input for large SPARQL queries.\n"
			+ "select|construct|ask|describe|prefix|base <rest-of-query>\n"
			+ "                                     Evaluates a SPARQL query on the currently open repository.\n";

	protected static final String SERQL = USAGE
			+ "serql <query>                 Evaluates the SeRQL query on the currently open repository\n"
			+ "serql                         Starts multi-line input for large SeRQL queries.\n";


	@Override
	public String getName() {
		return "help";
	}

	@Override
	public String getHelpShort() {
		return "Displays this help message";
	}
	
	/**
	 * Constructor
	 * 
	 * @param consoleIO 
	 */
	PrintHelp(ConsoleIO consoleIO, ConsoleState state) {
		super();
		this.consoleIO = consoleIO;
		this.state = state;
	}

	@Override
	public void execute(String... parameters) {
		if (parameters.length < 2) {
			printCommandOverview();
			return;
		}
		
		final String target = parameters[1].toLowerCase(Locale.ENGLISH);
		Command cmd = state.getCommands().get(target);
		if (cmd != null) {
			consoleIO.writeln(cmd.getHelpLong());
		} else {
			consoleIO.writeln("No additional info available for command " + target);
		}
	}

	/**
	 * Print list of commands
	 */
	private void printCommandOverview() {
		consoleIO.writeln("For more information on a specific command, try 'help <command>'.");
		consoleIO.writeln("List of all commands:");
		
		state.getCommands().forEach((k,v) -> {
			consoleIO.writeln(String.format("%12s %s", k, v.getHelpShort()));
		});

		consoleIO.writeln("sparql      Evaluate a SPARQL query");
		consoleIO.writeln("serql       Evaluate a SeRQL query");
		consoleIO.writeln("exit, quit  Exit the console");
	}

	@Override
	public String getHelpLong() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
}
