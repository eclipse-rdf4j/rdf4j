/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import org.eclipse.rdf4j.console.setting.CharacterSet;
import org.eclipse.rdf4j.console.setting.ConsoleSetting;
import org.eclipse.rdf4j.console.setting.WorkDir;

import org.eclipse.rdf4j.repository.Repository;

/**
 * Output command
 * 
 * @author Bart Hanssens
 */
public class Output extends ConsoleCommand {

	private final Map<String,ConsoleSetting> settings;
	private final TupleAndGraphQueryEvaluator evaluator;

	@Override
	public String getName() {
		return "output";
	}

	@Override
	public String getHelpShort() {
		return "Saves query or results to output file (by default in working dir).";
	}
	
	@Override
	public String getHelpLong() {
		return PrintHelp.USAGE
			+ "output lastquery  [charset,]<file.qr>   Output last query string to file\n"
			+ "output lastresult [charset,]<file.srx>  Output last result to file\n"
			+ " [charset,]                    Character set to be used (or default)\n"
			+ " <file.qr>                     By default saved in working dir\n";
	}

	/**
	 * Constructor
	 * 
	 * @param evaluator
	 * @param settings 
	 */
	public Output(TupleAndGraphQueryEvaluator evaluator, Map<String,ConsoleSetting> settings) {
		super(evaluator.getConsoleIO(), evaluator.getConsoleState());
		this.evaluator = evaluator;
		this.settings = settings;
	}

	/**
	 * Get working directory.
	 * Use a new working directory setting when not found.
	 * 
	 * @return boolean
	 */
	private Path getWorkDir() {
		return ((WorkDir) settings.getOrDefault(WorkDir.NAME, new WorkDir())).get();
	}
	
	/**
	 * Get character set
	 * Use a new character set setting when not found.
	 * 
	 * @return boolean
	 */
	private Charset getCharSet() {
		return ((CharacterSet) settings.getOrDefault(CharacterSet.NAME, new CharacterSet())).get();
	}
	
	@Override
	public void execute(String... tokens) {
		Repository repository = state.getRepository();

		if (repository == null) {
			consoleIO.writeUnopenedError();
			return;
		}
		if (tokens.length < 3) {
			consoleIO.writeln(getHelpLong());
			return;
		}

		String outType = tokens[1];
		String fileName = tokens[2];
	
		switch(outType) {
			case "lq":
			case "lastquery":
				saveQuery(fileName, evaluator.getLastQuery());
				break;
			case "lr":
			case "lastresult":
				break;
			default:
				break;
		}
	}

	/**
	 * Save last query string to a file
	 * 
	 * @param name file name
	 * @param query query string
	 */
	private void saveQuery(String name, String query) {
		if (query == null || query.isEmpty()) {
			consoleIO.writeln("No query to save");
			return;
		}
		
		Path p = getWorkDir().resolve(name);
		try {
			if (p.toFile().exists()) {
				boolean overwrite = consoleIO.askProceed("File exists", false);
				if (!overwrite) {
					return;
				}
			}
		} catch (IOException e) {
		}

		try (OutputStream os = Files.newOutputStream(p, StandardOpenOption.CREATE, 
														StandardOpenOption.TRUNCATE_EXISTING)) {
			os.write(query.getBytes(getCharSet()));
		} catch (IOException ex) {
			consoleIO.writeError("Could not write string to file: " + ex.getMessage());
		}
		consoleIO.writeError("Query saved to " + p.toAbsolutePath().toString());
	}
}
