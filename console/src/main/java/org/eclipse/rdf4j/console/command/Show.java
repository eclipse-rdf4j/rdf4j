/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.util.Locale;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.manager.RepositoryInfo;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Show command
 *
 * @author Dale Visser
 */
public class Show extends ConsoleCommand {

	private static final Logger LOGGER = LoggerFactory.getLogger(Show.class);

	private static final String OUTPUT_SEPARATOR = "+----------";

	@Override
	public String getName() {
		return "show";
	}

	@Override
	public String getHelpShort() {
		return "Displays an overview of various resources";
	}

	@Override
	public String getHelpLong() {
		return PrintHelp.USAGE
			+ "show {r, repositories}   Shows all available repositories\n"
			+ "show {n, namespaces}     Shows all namespaces\n"
			+ "show {c, contexts}       Shows all context identifiers\n";
	}

	/**
	 * Constructor
	 *
	 * @param consoleIO
	 * @param state
	 */
	public Show(ConsoleIO consoleIO, ConsoleState state) {
		super(consoleIO, state);
	}

	@Override
	public void execute(String... tokens) {
		if (tokens.length == 2) {
			final String target = tokens[1].toLowerCase(Locale.ENGLISH);
			if ("repositories".equals(target) || "r".equals(target)) {
				showRepositories();
			} else if ("namespaces".equals(target) || "n".equals(target)) {
				showNamespaces();
			} else if ("contexts".equals(target) || "c".equals(target)) {
				showContexts();
			} else {
				consoleIO.writeError("Unknown target '" + tokens[1] + "'");
			}
		} else {
			consoleIO.writeln(getHelpLong());
		}
	}

	/**
	 * Show available repositories
	 */
	private void showRepositories() {
		try {
			RepositoryManager manager = state.getManager();
			final Set<String> repIDs = manager.getRepositoryIDs();
			if (repIDs.isEmpty()) {
				consoleIO.writeln("--no repositories found--");
			} else {
				consoleIO.writeln(OUTPUT_SEPARATOR);
				for (String repID : repIDs) {
					consoleIO.write("|" + repID);

					try {
						final RepositoryInfo repInfo = manager.getRepositoryInfo(repID);
						if (repInfo.getDescription() != null) {
							consoleIO.write(" (\"" + repInfo.getDescription() + "\")");
						}
					} catch (RepositoryException e) {
						consoleIO.write(" [ERROR: " + e.getMessage() + "]");
					}
					consoleIO.writeln();
				}
				consoleIO.writeln(OUTPUT_SEPARATOR);
			}
		} catch (RepositoryException e) {
			consoleIO.writeError("Failed to get repository list: " + e.getMessage());
			LOGGER.error("Failed to get repository list", e);
		}
	}

	/**
	 * Show namespaces
	 */
	private void showNamespaces() {
		Repository repository = state.getRepository();
		if (repository == null) {
			consoleIO.writeUnopenedError();
			return;
		}

		RepositoryConnection con;
		try {
			con = repository.getConnection();
			try {
				final CloseableIteration<? extends Namespace, RepositoryException> namespaces = con.getNamespaces();
				try {
					if (namespaces.hasNext()) {
						consoleIO.writeln(OUTPUT_SEPARATOR);
						while (namespaces.hasNext()) {
							final Namespace namespace = namespaces.next();
							consoleIO.writeln("|" + namespace.getPrefix() + "  " + namespace.getName());
						}
						consoleIO.writeln(OUTPUT_SEPARATOR);
					} else {
						consoleIO.writeln("--no namespaces found--");
					}
				} finally {
					namespaces.close();
				}
			} finally {
				con.close();
			}
		} catch (RepositoryException e) {
			consoleIO.writeError(e.getMessage());
			LOGGER.error("Failed to show namespaces", e);
		}
	}

	/**
	 * Show contexts
	 */
	private void showContexts() {
		Repository repository = state.getRepository();
		if (repository == null) {
			consoleIO.writeUnopenedError();
			return;
		}

		RepositoryConnection con;
		try {
			con = repository.getConnection();
			try {
				final CloseableIteration<? extends Resource, RepositoryException> contexts = con.getContextIDs();
				try {
					if (contexts.hasNext()) {
						consoleIO.writeln(OUTPUT_SEPARATOR);
						while (contexts.hasNext()) {
							consoleIO.writeln("|" + contexts.next().toString());
						}
						consoleIO.writeln(OUTPUT_SEPARATOR);
					} else {
						consoleIO.writeln("--no contexts found--");
					}
				} finally {
					contexts.close();
				}
			} finally {
				con.close();
			}
		} catch (RepositoryException e) {
			consoleIO.writeError(e.getMessage());
			LOGGER.error("Failed to show contexts", e);
		}
	}
}
