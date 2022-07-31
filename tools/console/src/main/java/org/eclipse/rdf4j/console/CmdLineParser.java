/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Helper class for configuring console
 *
 * @author Bart Hanssens
 */
class CmdLineParser {

	private final static Option HELP = new Option("h", "help", false, "print this help");
	private final static Option VERSION = new Option("v", "version", false, "print version information");
	private final static Option SERVER = new Option("s", "serverURL", true,
			"URL of RDF4J Server to connect to, e.g. http://localhost:8080/rdf4j-server/");
	private final static Option DIRECTORY = new Option("d", "dataDir", true, "data dir to 'connect' to");
	private final static Option ECHO = new Option("e", "echo", false,
			"echoes input back to stdout, useful for logging script sessions");
	private final static Option QUIET = new Option("q", "quiet", false, "suppresses prompts, useful for scripting");
	private final static Option FORCE = new Option("f", "force", false,
			"always answer yes to (suppressed) confirmation prompts");
	private final static Option CAUTIOUS = new Option("c", "cautious", false,
			"always answer no to (suppressed) confirmation prompts");
	private final static Option MODE = new Option("x", "exitOnError", false,
			"immediately exit the console on the first error");

	private final static OptionGroup CAUTION_GROUP = new OptionGroup().addOption(CAUTIOUS)
			.addOption(FORCE)
			.addOption(MODE);
	private final static OptionGroup LOCATION_GROUP = new OptionGroup().addOption(SERVER).addOption(DIRECTORY);
	private final static Options OPTIONS = new Options().addOptionGroup(LOCATION_GROUP)
			.addOptionGroup(CAUTION_GROUP)
			.addOption(HELP)
			.addOption(VERSION)
			.addOption(ECHO)
			.addOption(QUIET);

	private final Console console;
	private CommandLine commandLine;

	/**
	 * Parse command line arguments
	 *
	 * @param args arguments
	 * @return parsed command line or null
	 */
	protected CommandLine parse(String[] args) {
		try {
			commandLine = new DefaultParser().parse(OPTIONS, args);
		} catch (ParseException e) {
			commandLine = null;
			console.getConsoleIO().writeError(e.getMessage());
		}
		return commandLine;
	}

	/**
	 * Print usage / available command line OPTIONS to screen
	 */
	protected void printUsage() {
		ConsoleIO io = console.getConsoleIO();

		io.writeln("RDF4J Console, an interactive command shell for RDF4J repositories.");
		final HelpFormatter formatter = new HelpFormatter();
		formatter.setWidth(80);
		formatter.printHelp("start-console [OPTION] [repositoryID]", OPTIONS);
		io.writeln();
		io.writeln("For bug reports and suggestions, see http://www.rdf4j.org/");
	}

	/**
	 * Handle help or version parameter at the command line
	 *
	 * @return false if an information option was given
	 */
	protected boolean handleInfoOptions() {
		if (commandLine.hasOption(HELP.getOpt())) {
			printUsage();
			return false;
		}
		if (commandLine.hasOption(VERSION.getOpt())) {
			console.getConsoleIO().writeln(console.getState().getApplicationName());
			return false;
		}
		return true;
	}

	/**
	 * Handle command line "exit on error" mode
	 */
	protected void handleExitOption() {
		console.setExitOnError(commandLine.hasOption(MODE.getOpt()));
	}

	/**
	 * Handle command line echo options
	 */
	protected void handleEchoOptions() {
		ConsoleIO io = console.getConsoleIO();

		io.setEcho(commandLine.hasOption(ECHO.getOpt()));
		io.setQuiet(commandLine.hasOption(QUIET.getOpt()));
	}

	/**
	 * Handle command line caution group
	 *
	 * @return location of the (remote or local) repository
	 */
	protected boolean handleCautionGroup() {
		ConsoleIO io = console.getConsoleIO();

		try {
			if (commandLine.hasOption(FORCE.getOpt())) {
				CAUTION_GROUP.setSelected(FORCE);
				io.setForce();
			}
			if (commandLine.hasOption(CAUTIOUS.getOpt())) {
				CAUTION_GROUP.setSelected(CAUTIOUS);
				io.setCautious();
			}
		} catch (AlreadySelectedException e) {
			printUsage();
			return false;
		}
		return true;
	}

	/**
	 * Handle command line location group
	 *
	 * @return location of the (remote or local) repository
	 */
	protected String handleLocationGroup() {
		String location = null;

		try {
			if (commandLine.hasOption(DIRECTORY.getOpt())) {
				LOCATION_GROUP.setSelected(DIRECTORY);
				location = commandLine.getOptionValue(DIRECTORY.getOpt());
			}
			if (commandLine.hasOption(SERVER.getOpt())) {
				LOCATION_GROUP.setSelected(SERVER);
				location = commandLine.getOptionValue(SERVER.getOpt());
			}
		} catch (AlreadySelectedException e) {
			printUsage();
		}
		return location;
	}

	/**
	 * Get selected location from location group. Can be d(irectory) or s(erver)
	 *
	 * @return string
	 */
	protected String getSelectedLocation() {
		return LOCATION_GROUP.getSelected();
	}

	/**
	 * Get remaining argument from command line, if any. Returns empty string if there is no argument, null on error
	 *
	 * @return argument as string, or null on error
	 */
	protected String handleOtherArg() {
		String[] otherArgs = commandLine.getArgs();
		if (otherArgs.length > 1) {
			printUsage();
			return null;
		}
		return (otherArgs.length == 1) ? otherArgs[0] : "";
	}

	/**
	 * Constructor
	 *
	 * @param console
	 */
	protected CmdLineParser(Console console) {
		this.console = console;
	}
}
