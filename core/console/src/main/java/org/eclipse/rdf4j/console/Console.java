/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.eclipse.rdf4j.Sesame;
import org.eclipse.rdf4j.common.app.AppConfiguration;
import org.eclipse.rdf4j.common.app.AppVersion;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;

/**
 * The RDF4J Console is a command-line application for interacting with RDF4J.
 * It reads commands from standard input and prints feedback to standard output.
 * Available options include loading and querying of data in repositories,
 * repository creation and verification of RDF files.
 * 
 * @author Jeen Broekstra
 * @author Arjohn Kampman
 */
public class Console implements ConsoleState, ConsoleParameters {

	/*------------------*
	 * Static constants *
	 *------------------*/

	private static final AppVersion VERSION = AppVersion.parse(Sesame.getVersion());

	private static final String APP_NAME = "RDF4J Console";

	private static boolean exitOnError;

	/*-----------*
	 * Constants *
	 *-----------*/

	private final AppConfiguration appConfig = new AppConfiguration(APP_NAME, APP_NAME, VERSION);

	/*-----------*
	 * Variables *
	 *-----------*/

	private RepositoryManager manager;

	private String managerID;

	private Repository repository;

	private String repositoryID;

	private final ConsoleIO consoleIO;

	private int consoleWidth = 80;

	private boolean showPrefix = true;

	private boolean queryPrefix = false;

	/*----------------*
	 * Static methods *
	 *----------------*/

	public static void main(final String[] args)
		throws IOException
	{
		final Console console = new Console();
		final Option helpOption = new Option("h", "help", false, "print this help");
		final Option versionOption = new Option("v", "version", false, "print version information");
		final Option serverURLOption = new Option("s", "serverURL", true,
				"URL of Sesame server to connect to, e.g. http://localhost/openrdf-sesame/");
		final Option dirOption = new Option("d", "dataDir", true, "data dir to 'connect' to");
		Option echoOption = new Option("e", "echo", false,
				"echoes input back to stdout, useful for logging script sessions");
		Option quietOption = new Option("q", "quiet", false, "suppresses prompts, useful for scripting");
		Option forceOption = new Option("f", "force", false,
				"always answer yes to (suppressed) confirmation prompts");
		Option cautiousOption = new Option("c", "cautious", false,
				"always answer no to (suppressed) confirmation prompts");
		Option exitOnErrorMode = new Option("x", "exitOnError", false,
				"immediately exit the console on the first error");
		final Options options = new Options();
		OptionGroup cautionGroup = new OptionGroup().addOption(cautiousOption).addOption(forceOption).addOption(
				exitOnErrorMode);
		OptionGroup locationGroup = new OptionGroup().addOption(serverURLOption).addOption(dirOption);
		options.addOptionGroup(locationGroup).addOptionGroup(cautionGroup);
		options.addOption(helpOption).addOption(versionOption).addOption(echoOption).addOption(quietOption);
		CommandLine commandLine = parseCommandLine(args, console, options);
		handleInfoOptions(console, helpOption, versionOption, options, commandLine);
		console.consoleIO.setEcho(commandLine.hasOption(echoOption.getOpt()));
		console.consoleIO.setQuiet(commandLine.hasOption(quietOption.getOpt()));
		exitOnError = commandLine.hasOption(exitOnErrorMode.getOpt());
		String location = handleOptionGroups(console, serverURLOption, dirOption, forceOption, cautiousOption,
				options, cautionGroup, locationGroup, commandLine);
		final String[] otherArgs = commandLine.getArgs();
		if (otherArgs.length > 1) {
			printUsage(console.consoleIO, options);
			System.exit(1);
		}
		connectAndOpen(console, locationGroup.getSelected(), location, otherArgs);
		console.start();
	}

	private static String handleOptionGroups(final Console console, final Option serverURLOption,
			final Option dirOption, Option forceOption, Option cautiousOption, final Options options,
			OptionGroup cautionGroup, OptionGroup locationGroup, CommandLine commandLine)
	{
		String location = null;
		try {
			if (commandLine.hasOption(forceOption.getOpt())) {
				cautionGroup.setSelected(forceOption);
				console.consoleIO.setForce();
			}
			if (commandLine.hasOption(cautiousOption.getOpt())) {
				cautionGroup.setSelected(cautiousOption);
				console.consoleIO.setCautious();
			}
			if (commandLine.hasOption(dirOption.getOpt())) {
				locationGroup.setSelected(dirOption);
				location = commandLine.getOptionValue(dirOption.getOpt());
			}
			if (commandLine.hasOption(serverURLOption.getOpt())) {
				locationGroup.setSelected(serverURLOption);
				location = commandLine.getOptionValue(serverURLOption.getOpt());
			}
		}
		catch (AlreadySelectedException e) {
			printUsage(console.consoleIO, options);
			System.exit(3);
		}
		return location;
	}

	private static CommandLine parseCommandLine(final String[] args, final Console console,
			final Options options)
	{
		CommandLine commandLine = null;
		try {
			commandLine = new PosixParser().parse(options, args);
		}
		catch (ParseException e) {
			console.consoleIO.writeError(e.getMessage());
			System.exit(1);
		}
		return commandLine;
	}

	private static void handleInfoOptions(final Console console, final Option helpOption,
			final Option versionOption, final Options options, final CommandLine commandLine)
	{
		if (commandLine.hasOption(helpOption.getOpt())) {
			printUsage(console.consoleIO, options);
			System.exit(0);
		}
		if (commandLine.hasOption(versionOption.getOpt())) {
			console.consoleIO.writeln(console.appConfig.getFullName());
			System.exit(0);
		}
	}

	private static void connectAndOpen(Console console, String selectedLocationOption, String location,
			String[] otherArgs)
	{
		boolean connected;
		if ("s".equals(selectedLocationOption)) {
			connected = console.connect.connectRemote(location);
		}
		else if ("d".equals(selectedLocationOption)) {
			connected = console.connect.connectLocal(location);
		}
		else {
			connected = console.connect.connectDefault();
		}
		if (!connected) {
			System.exit(2);
		}
		if (otherArgs.length > 0) {
			console.open.openRepository(otherArgs[0]);
		}
	}

	private static void printUsage(ConsoleIO cio, Options options) {
		cio.writeln("Sesame Console, an interactive shell based utility to communicate with Sesame repositories.");
		final HelpFormatter formatter = new HelpFormatter();
		formatter.setWidth(80);
		formatter.printHelp("start-console [OPTION] [repositoryID]", options);
		cio.writeln();
		cio.writeln("For bug reports and suggestions, see http://www.openrdf.org/");
	}

	private final Map<String, Command> commandMap = new HashMap<String, Command>();

	private final Connect connect;

	private final Disconnect disconnect;

	private final Open open;

	private final QueryEvaluator queryEvaluator;

	public Console()
		throws IOException
	{
		appConfig.init();
		consoleIO = new ConsoleIO(new BufferedReader(new InputStreamReader(System.in)), System.out, System.err,
				this);
		commandMap.put("federate", new Federate(consoleIO, this));
		this.queryEvaluator = new QueryEvaluator(consoleIO, this, this);
		LockRemover lockRemover = new LockRemover(consoleIO);
		Close close = new Close(consoleIO, this);
		commandMap.put("close", close);
		this.disconnect = new Disconnect(consoleIO, this, close);
		commandMap.put("help", new PrintHelp(consoleIO));
		commandMap.put("info", new PrintInfo(consoleIO, this));
		this.connect = new Connect(consoleIO, this, disconnect);
		commandMap.put("connect", connect);
		commandMap.put("create", new Create(consoleIO, this, lockRemover));
		commandMap.put("drop", new Drop(consoleIO, this, close, lockRemover));
		this.open = new Open(consoleIO, this, close, lockRemover);
		commandMap.put("open", open);
		commandMap.put("show", new Show(consoleIO, this));
		commandMap.put("load", new Load(consoleIO, this, lockRemover));
		commandMap.put("verify", new Verify(consoleIO));
		commandMap.put("clear", new Clear(consoleIO, this, lockRemover));
		commandMap.put("set", new SetParameters(consoleIO, this));
	}

	public void start()
		throws IOException
	{
		consoleIO.writeln("Sesame Console, an interactive shell to communicate with Sesame repositories.");
		consoleIO.writeln();
		consoleIO.writeln("Type 'help' for help.");
		int exitCode = 0;
		try {
			boolean exitFlag = false;
			while (!exitFlag) {
				final String command = consoleIO.readCommand();
				if (command == null) {
					// EOF
					break;
				}
				exitFlag = executeCommand(command);
				if (exitOnError && consoleIO.wasErrorWritten()) {
					exitCode = 2;
					exitFlag = true;
				}
			}
		}
		finally {
			disconnect.execute(false);
		}
		if (exitCode != 0) {
			System.exit(exitCode);
		}
		consoleIO.writeln("Bye");
	}

	private boolean executeCommand(final String command)
		throws IOException
	{
		boolean exit = false;
		
		// only try to parse the command if non-empty.
		if (0 < command.length()) {
			final String[] tokens = parse(command);
			final String operation = tokens[0].toLowerCase(Locale.ENGLISH);
			exit = "quit".equals(operation) || "exit".equals(operation);
			if (!exit) {
				if (commandMap.containsKey(operation)) {
					commandMap.get(operation).execute(tokens);
				}
				else if ("disconnect".equals(operation)) {
					disconnect.execute(true);
				}
				else {
					queryEvaluator.executeQuery(command, operation);
				}
			}
		}

		return exit;
	}

	private String[] parse(final String command) {
		final Pattern pattern = Pattern.compile("\"([^\"]*)\"|(\\S+)");
		final Matcher matcher = pattern.matcher(command);
		final List<String> tokens = new ArrayList<String>();
		while (matcher.find()) {
			if (matcher.group(1) == null) {
				tokens.add(matcher.group());
			}
			else {
				tokens.add(matcher.group(1));
			}
		}
		return tokens.toArray(new String[tokens.size()]);
	}

	public String getApplicationName() {
		return this.appConfig.getFullName();
	}

	public File getDataDirectory() {
		return this.appConfig.getDataDir();
	}

	public String getManagerID() {
		return this.managerID;
	}

	public String getRepositoryID() {
		return this.repositoryID;
	}

	public RepositoryManager getManager() {
		return this.manager;
	}

	public void setManager(RepositoryManager manager) {
		this.manager = manager;
	}

	public void setManagerID(String managerID) {
		this.managerID = managerID;
	}

	public Repository getRepository() {
		return this.repository;
	}

	public void setRepositoryID(String repositoryID) {
		this.repositoryID = repositoryID;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public int getWidth() {
		return this.consoleWidth;
	}

	public void setWidth(int width) {
		this.consoleWidth = width;
	}

	public boolean isShowPrefix() {
		return this.showPrefix;
	}

	public void setShowPrefix(boolean value) {
		this.showPrefix = value;
	}

	public boolean isQueryPrefix() {
		return this.queryPrefix;
	}

	public void setQueryPrefix(boolean value) {
		this.queryPrefix = value;
	}
}
