/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

/**
 * Print command
 * 
 * @author Dale Visser
 */
class PrintInfo implements Command {
	private final ConsoleState appInfo;
	private final ConsoleIO consoleIO;

	@Override
	public  String getName() {
		return "info";
	}

	@Override
	public String getHelpShort() {
		return "Shows info about the console";
	}
	
	@Override
	public String getHelpLong() {
		return  PrintHelp.USAGE
			+ "info                  Shows information about the console\n";
	}
	
	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 * @param appInfo 
	 */
	PrintInfo(ConsoleIO consoleIO, ConsoleState appInfo) {
		this.consoleIO = consoleIO;
		this.appInfo = appInfo;
	}

	@Override
	public void execute(String... parameters) {
		consoleIO.writeln(appInfo.getApplicationName());
		consoleIO.writeln("Data dir: " + appInfo.getDataDirectory());
		String managerID = appInfo.getManagerID();
		consoleIO.writeln("Connected to: " + (managerID == null ? "-" : managerID));
	}
}
