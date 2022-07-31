/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.console.LockRemover;
import org.eclipse.rdf4j.console.Util;
import org.eclipse.rdf4j.console.setting.ConsoleSetting;
import org.eclipse.rdf4j.console.setting.WorkDir;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryReadOnlyException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

/**
 * Load command
 *
 * @author Dale Visser
 */
public class Load extends ConsoleCommand {
	@Override
	public String getName() {
		return "load";
	}

	@Override
	public String getHelpShort() {
		return "Loads a data file into a repository, takes a file path or URL as argument";
	}

	@Override
	public String getHelpLong() {
		return PrintHelp.USAGE + "load <file-or-url> [from <base-uri>] [into <context-id>]\n"
				+ "  <file-or-url>   The path or URL identifying the data file\n"
				+ "  <base-uri>      The base URI to use for resolving relative references, defaults to <file-or-url>\n"
				+ "  <context-id>    The ID of the context to add the data to, e.g. foo:bar or _:n123\n"
				+ "Loads the specified data file into the current repository\n";
	}

	@Override
	public String[] usesSettings() {
		return new String[] { WorkDir.NAME };
	}

	/**
	 * Constructor
	 *
	 * @param consoleIO
	 * @param state
	 * @param settings
	 */
	public Load(ConsoleIO consoleIO, ConsoleState state, Map<String, ConsoleSetting> settings) {
		super(consoleIO, state, settings);
	}

	@Override
	public void execute(final String... tokens) {
		Repository repository = state.getRepository();
		if (repository == null) {
			writeUnopenedError();
		} else {
			if (tokens.length < 2) {
				writeln(getHelpLong());
			} else {
				String baseURI = null;
				String context = null;

				int index = 2;
				if (tokens.length >= index + 2 && tokens[index].equalsIgnoreCase("from")) {
					baseURI = tokens[index + 1];
					index += 2;
				}
				if (tokens.length >= index + 2 && tokens[index].equalsIgnoreCase("into")) {
					context = tokens[tokens.length - 1];
					index += 2;
				}
				if (index < tokens.length) {
					writeln(getHelpLong());
				} else {
					load(repository, baseURI, context, tokens);
				}
			}
		}
	}

	/**
	 * Get working dir setting.
	 *
	 * @return path of working dir
	 */
	private Path getWorkDir() {
		return ((WorkDir) settings.get(WorkDir.NAME)).get();
	}

	/**
	 * Load data into a repository
	 *
	 * @param repository repository
	 * @param baseURI
	 * @param context
	 * @param tokens
	 */
	private void load(Repository repository, String baseURI, String context, final String... tokens) {
		final String dataPath = tokens[1];
		URL dataURL = null;
		File dataFile = null;

		if (Util.isHttpOrFile(dataPath)) {
			try {
				dataURL = new URL(dataPath);
			} catch (MalformedURLException e) {

			}
		} else {
			// dataPath is a file
			dataFile = Util.getNormalizedPath(getWorkDir(), dataPath).toFile();
		}

		try {
			addData(repository, baseURI, context, dataURL, dataFile);
		} catch (RepositoryReadOnlyException e) {
			handleReadOnlyException(repository, e, tokens);
		} catch (MalformedURLException e) {
			writeError("Malformed URL: " + dataPath);
		} catch (IllegalArgumentException e) {
			// Thrown when context URI is invalid
			writeError(e.getMessage());
		} catch (IOException e) {
			writeError("Failed to load data", e);
		} catch (UnsupportedRDFormatException e) {
			writeError("No parser available for this RDF format");
		} catch (RDFParseException e) {
			writeError("Malformed document", e);
		} catch (RepositoryException e) {
			writeError("Unable to add data to repository", e);
		}
	}

	/**
	 * Handle exceptions when loading data in a read-only repository. If a lock is present and can be removed, the
	 * command will be executed again.
	 *
	 * @param repository repository
	 * @param caught     exception
	 * @param tokens     full command as series of tokens
	 */
	private void handleReadOnlyException(Repository repository, RepositoryReadOnlyException caught,
			final String... tokens) {
		try {
			if (LockRemover.tryToRemoveLock(repository, consoleIO)) {
				execute(tokens);
			} else {
				writeError("Failed to load data", caught);
			}
		} catch (RepositoryException e) {
			writeError("Unable to restart repository", e);
		} catch (IOException e) {
			writeError("Unable to remove lock", e);
		}
	}

	/**
	 * Add data from a URL or local file. If the dataURL is null, then the datafile will be used.
	 *
	 * @param repository repository
	 * @param baseURI    base URI
	 * @param context    context (can be null)
	 * @param dataURL    url of the data
	 * @param dataFile   file containing data
	 * @throws RepositoryException
	 * @throws IOException
	 * @throws RDFParseException
	 */
	private void addData(Repository repository, String baseURI, String context, URL dataURL, File dataFile)
			throws RepositoryException, IOException, RDFParseException {
		Resource[] contexts = getContexts(repository, context);
		writeln("Loading data...");

		final long startTime = System.nanoTime();
		try (RepositoryConnection con = repository.getConnection()) {
			if (dataURL == null) {
				con.add(dataFile, baseURI, null, contexts);
			} else {
				con.add(dataURL, baseURI, null, contexts);
			}
		}
		final long endTime = System.nanoTime();
		writeln("Data has been added to the repository (" + (endTime - startTime) / 1_000_000 + " ms)");
	}

	/**
	 * Get context as resource
	 *
	 * @param repository
	 * @param context
	 * @return array of size one, or null
	 */
	private Resource[] getContexts(Repository repository, String context) {
		Resource[] contexts = new Resource[0];
		if (context != null) {
			Resource contextURI;
			if (context.startsWith("_:")) {
				contextURI = repository.getValueFactory().createBNode(context.substring(2));
			} else {
				contextURI = repository.getValueFactory().createIRI(context);
			}
			contexts = new Resource[] { contextURI };
		}
		return contexts;
	}
}
