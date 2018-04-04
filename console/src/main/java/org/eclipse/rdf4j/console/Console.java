/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import org.eclipse.rdf4j.console.command.QueryEvaluator;
import org.eclipse.rdf4j.console.command.ConsoleCommand;
import org.eclipse.rdf4j.console.command.Close;
import org.eclipse.rdf4j.console.command.Show;
import org.eclipse.rdf4j.console.command.Drop;
import org.eclipse.rdf4j.console.command.Create;
import org.eclipse.rdf4j.console.command.Open;
import org.eclipse.rdf4j.console.command.PrintHelp;
import org.eclipse.rdf4j.console.command.SetParameters;
import org.eclipse.rdf4j.console.command.Serql;
import org.eclipse.rdf4j.console.command.PrintInfo;
import org.eclipse.rdf4j.console.command.Sparql;
import org.eclipse.rdf4j.console.command.Disconnect;
import org.eclipse.rdf4j.console.command.Connect;
import org.eclipse.rdf4j.console.command.Load;
import org.eclipse.rdf4j.console.command.Export;
import org.eclipse.rdf4j.console.command.Verify;
import org.eclipse.rdf4j.console.command.Federate;
import org.eclipse.rdf4j.console.command.Clear;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;
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

import org.eclipse.rdf4j.RDF4J;
import org.eclipse.rdf4j.common.app.AppConfiguration;
import org.eclipse.rdf4j.common.app.AppVersion;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;

import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;

/**
 * The RDF4J Console is a command-line application for interacting with RDF4J. It reads commands from standard
 * input and prints feedback to standard output. Available options include loading and querying of data in
 * repositories, repository creation and verification of RDF files.
 *
 * @author Jeen Broekstra
 * @author Arjohn Kampman
 * @author Bart Hanssens
 */
public class Console implements ConsoleState, ConsoleParameters {

	/*------------------*
	 * Static constants *
	 *------------------*/
	private static final AppVersion VERSION = AppVersion.parse(RDF4J.getVersion());
	private static final String APP_NAME = "Console";
	private static boolean exitOnError;

	/*-----------*
	 * Constants *
	 *-----------*/
	private final AppConfiguration appConfig = new AppConfiguration(APP_NAME, VERSION);

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
	private boolean queryPrefix = true;

	private final SortedMap<String,ConsoleCommand> commandMap = new TreeMap<>();

	private final Connect connect;
	private final Disconnect disconnect;
	private final Open open;

	
	@Override
	public String getApplicationName() {
		return this.appConfig.getFullName();
	}

	@Override
	public File getDataDirectory() {
		return this.appConfig.getDataDir();
	}

	@Override
	public String getManagerID() {
		return this.managerID;
	}

	@Override
	public String getRepositoryID() {
		return this.repositoryID;
	}

	@Override
	public RepositoryManager getManager() {
		return this.manager;
	}

	@Override
	public void setManager(RepositoryManager manager) {
		this.manager = manager;
	}

	@Override
	public void setManagerID(String managerID) {
		this.managerID = managerID;
	}

	@Override
	public Repository getRepository() {
		return this.repository;
	}

	@Override
	public void setRepositoryID(String repositoryID) {
		this.repositoryID = repositoryID;
	}

	@Override
	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	@Override
	public int getWidth() {
		return this.consoleWidth;
	}

	@Override
	public void setWidth(int width) {
		this.consoleWidth = width;
	}

	@Override
	public boolean isShowPrefix() {
		return this.showPrefix;
	}

	@Override
	public void setShowPrefix(boolean value) {
		this.showPrefix = value;
	}

	@Override
	public boolean isQueryPrefix() {
		return this.queryPrefix;
	}

	@Override
	public void setQueryPrefix(boolean value) {
		this.queryPrefix = value;
	}
	
	/*----------------*
	 * Static methods *
	 *----------------*/
	public static void main(final String[] args) throws IOException {
		final Console console = new Console();

		Option helpOption = new Option("h", "help", false, 
				"print this help");
		Option versionOption = new Option("v", "version", false, 
				"print version information");
		Option serverURLOption = new Option("s", "serverURL", true,
				"URL of RDF4J Server to connect to, e.g. http://localhost:8080/rdf4j-server/");
		Option dirOption = new Option("d", "dataDir", true, 
				"data dir to 'connect' to");
		Option echoOption = new Option("e", "echo", false,
				"echoes input back to stdout, useful for logging script sessions");
		Option quietOption = new Option("q", "quiet", false, 
				"suppresses prompts, useful for scripting");
		Option forceOption = new Option("f", "force", false,
				"always answer yes to (suppressed) confirmation prompts");
		Option cautiousOption = new Option("c", "cautious", false,
				"always answer no to (suppressed) confirmation prompts");
		Option exitOnErrorMode = new Option("x", "exitOnError", false,
				"immediately exit the console on the first error");
		
		final Options options = new Options();
		
		OptionGroup cautionGroup = new OptionGroup().addOption(cautiousOption)
								.addOption(forceOption)
								.addOption(exitOnErrorMode);
		OptionGroup locationGroup = new OptionGroup().addOption(serverURLOption)
								.addOption(dirOption);
		
		options.addOptionGroup(locationGroup)
			.addOptionGroup(cautionGroup);
		
		options.addOption(helpOption)
			.addOption(versionOption)
			.addOption(echoOption)
			.addOption(quietOption);
	
		CommandLine commandLine = parseCommandLine(args, console, options);
		handleInfoOptions(console, helpOption, versionOption, options, commandLine);
		
		console.consoleIO.setEcho(commandLine.hasOption(echoOption.getOpt()));
		console.consoleIO.setQuiet(commandLine.hasOption(quietOption.getOpt()));
		exitOnError = commandLine.hasOption(exitOnErrorMode.getOpt());

		String location = handleLocationGroup(console, serverURLOption, dirOption, 
							options, locationGroup, commandLine);
		handleCautionGroup(console, forceOption, cautiousOption, 
							options, cautionGroup, commandLine);
		
		final String[] otherArgs = commandLine.getArgs();
		if (otherArgs.length > 1) {
			printUsage(console.consoleIO, options);
			System.exit(1);
		}
		connectAndOpen(console, locationGroup.getSelected(), location, otherArgs);
		console.start();
	}

	/**
	 * Handle command line option group
	 * 
	 * @param console
	 * @param serverURLOption
	 * @param dirOption
	 * @param options
	 * @param locationGroup
	 * @param commandLine
	 * @return location of the (remote or local) repository 
	 */
	private static String handleLocationGroup(Console console, Option serverURLOption, Option dirOption, 
					Options options, OptionGroup locationGroup, CommandLine commandLine) {
		String location = null;
		
		try {
			if (commandLine.hasOption(dirOption.getOpt())) {
				locationGroup.setSelected(dirOption);
				location = commandLine.getOptionValue(dirOption.getOpt());
			}
			if (commandLine.hasOption(serverURLOption.getOpt())) {
				locationGroup.setSelected(serverURLOption);
				location = commandLine.getOptionValue(serverURLOption.getOpt());
			}
		} catch (AlreadySelectedException e) {
			printUsage(console.consoleIO, options);
			System.exit(3);
		}
		return location;
	}

	/**
	 * Handle caution option group
	 * 
	 * @param console
	 * @param forceOption
	 * @param cautiousOption
	 * @param options
	 * @param cautionGroup
	 * @param commandLine
	 */
	private static void handleCautionGroup(Console console, Option forceOption, Option cautiousOption, 
					Options options, OptionGroup cautionGroup, CommandLine commandLine) {
		try {
			if (commandLine.hasOption(forceOption.getOpt())) {
				cautionGroup.setSelected(forceOption);
				console.consoleIO.setForce();
			}
			if (commandLine.hasOption(cautiousOption.getOpt())) {
				cautionGroup.setSelected(cautiousOption);
				console.consoleIO.setCautious();
			}
		} catch (AlreadySelectedException e) {
			printUsage(console.consoleIO, options);
			System.exit(3);
		}
	}

	/**
	 * Handle info options group
	 * 
	 * @param console
	 * @param helpOption
	 * @param versionOption
	 * @param options
	 * @param commandLine 
	 */
	private static void handleInfoOptions(Console console, Option helpOption,
			Option versionOption, Options options, CommandLine commandLine) {
		if (commandLine.hasOption(helpOption.getOpt())) {
			printUsage(console.consoleIO, options);
			System.exit(0);
		}
		if (commandLine.hasOption(versionOption.getOpt())) {
			console.consoleIO.writeln(console.appConfig.getFullName());
			System.exit(0);
		}
	}

	/**
	 * Parse command line, exit when command line cannot be parsed
	 * 
	 * @param args
	 * @param console
	 * @param options
	 * @return parsed command line
	 */
	private static CommandLine parseCommandLine(String[] args, Console console, Options options) {
		CommandLine commandLine = null;
		try {
			commandLine = new PosixParser().parse(options, args);
		} catch (ParseException e) {
			console.consoleIO.writeError(e.getMessage());
			System.exit(1);
		}
		return commandLine;
	}

	/**
	 * Connect to (and open) a repository, exit when connection fails
	 * 
	 * @param console
	 * @param selectedLocationOption s for server, d for local directory 
	 * @param location
	 * @param otherArgs 
	 */
	private static void connectAndOpen(Console console, String selectedLocationOption, String location,
			String[] otherArgs) {
		boolean connected;
		if ("s".equals(selectedLocationOption)) {
			connected = console.connect.connectRemote(location);
		} else if ("d".equals(selectedLocationOption)) {
			connected = console.connect.connectLocal(location);
		} else {
			connected = console.connect.connectDefault();
		}
		if (!connected) {
			System.exit(2);
		}
		if (otherArgs.length > 0) {
			console.open.openRepository(otherArgs[0]);
		}
	}

	/**
	 * Print usage / available options to console
	 * 
	 * @param cio
	 * @param options 
	 */
	private static void printUsage(ConsoleIO cio, Options options) {
		cio.writeln("RDF4J Console, an interactive command shell for RDF4J repositories.");
		final HelpFormatter formatter = new HelpFormatter();
		formatter.setWidth(80);
		formatter.printHelp("start-console [OPTION] [repositoryID]", options);
		cio.writeln();
		cio.writeln("For bug reports and suggestions, see http://www.rdf4j.org/");
	}

	/**
	 * Add command to register of known commands
	 * 
	 * @param cmd command to be added
	 */
	public final void register(ConsoleCommand cmd) {
		commandMap.put(cmd.getName(), cmd);
	}
	
	/**
	 * Constructor
	 * 
	 * @throws IOException 
	 */
	public Console() throws IOException {
		appConfig.init();
		consoleIO = new ConsoleIO(this);

		Close close = new Close(consoleIO, this);
		this.disconnect = new Disconnect(consoleIO, this, close);
		this.connect = new Connect(consoleIO, this, disconnect);
		this.open = new Open(consoleIO, this, close);
		
		register(new Federate(consoleIO, this));
		register(new Sparql(consoleIO, this, this));
		register(new Serql(consoleIO, this, this));
		register(close);
		register(new PrintHelp(consoleIO, commandMap));
		register(new PrintInfo(consoleIO, this));
		register(connect);
		register(new Create(consoleIO, this));
		register(new Drop(consoleIO, this, close));
		register(open);
		register(new Show(consoleIO, this));
		register(new Load(consoleIO, this));
		register(new Export(consoleIO, this));
		register(new Verify(consoleIO));
		register(new Clear(consoleIO, this));
		register(new SetParameters(consoleIO, this, this));
	}

	/**
	 * Start the interactive console, return error code on exit
	 * 
	 * @throws IOException 
	 */
	public void start() throws IOException {
		consoleIO.writeln(appConfig.getFullName());
		consoleIO.writeln();
		consoleIO.writeln(RDF4J.getVersion());
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
		} catch (UserInterruptException | EndOfFileException e) {
			exitCode = 0;
		} finally {
			disconnect.execute(false);
		}
		if (exitCode != 0) {
			System.exit(exitCode);
		}
		consoleIO.writeln("Bye");
	}

	/**
	 * Execute a command
	 * 
	 * @param command
	 * @return true when exit/quit command is entered
	 * @throws IOException 
	 */
	private boolean executeCommand(String command) throws IOException {
		boolean exit = false;

		// only try to parse the command if non-empty.
		if (0 < command.length()) {
			final String[] tokens = parse(command);
			final String operation = tokens[0].toLowerCase(Locale.ENGLISH);
			
			exit = "quit".equals(operation) || "exit".equals(operation);
			if (!exit) {
				ConsoleCommand cmd = commandMap.getOrDefault(operation, 
									commandMap.get("sparql"));
				if (cmd instanceof QueryEvaluator) {
					((QueryEvaluator) cmd).executeQuery(command, operation);
				} else {
					cmd.execute(tokens);
				}
			}
		}
		return exit;
	}

	/**
	 * Split a command into an array of tokens
	 * 
	 * @param command command to parse
	 * @return array of strings
	 */
	private String[] parse(String command) {
		final Pattern pattern = Pattern.compile("\"([^\"]*)\"|(\\S+)");
		final Matcher matcher = pattern.matcher(command);
		final List<String> tokens = new ArrayList<>();
		
		while (matcher.find()) {
			if (matcher.group(1) == null) {
				tokens.add(matcher.group());
			} else {
				tokens.add(matcher.group(1));
			}
		}
		return tokens.toArray(new String[tokens.size()]);
	}
}
