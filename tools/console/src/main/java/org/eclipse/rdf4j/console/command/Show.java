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

/**
 * Show command
 *
 * @author Dale Visser
 */
public class Show extends ConsoleCommand {
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
		return PrintHelp.USAGE + "show {r, repositories}   Shows all available repositories\n"
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
				writeError("Unknown target '" + tokens[1] + "'");
			}
		} else {
			writeln(getHelpLong());
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
				writeln("No repositories found");
			} else {
				writeln(OUTPUT_SEPARATOR);
				for (String repID : repIDs) {
					write("|" + repID);

					try {
						final RepositoryInfo repInfo = manager.getRepositoryInfo(repID);
						if (repInfo.getDescription() != null) {
							write(" (\"" + repInfo.getDescription() + "\")");
						}
					} catch (RepositoryException e) {
						write(" [ERROR: " + e.getMessage() + "]");
					}
					writeln("");
				}
				writeln(OUTPUT_SEPARATOR);
			}
		} catch (RepositoryException e) {
			writeError("Failed to get repository list", e);
		}
	}

	/**
	 * Show namespaces
	 */
	private void showNamespaces() {
		Repository repository = state.getRepository();
		if (repository == null) {
			writeUnopenedError();
			return;
		}

		try (RepositoryConnection con = repository.getConnection()) {
			try (CloseableIteration<? extends Namespace, RepositoryException> namespaces = con.getNamespaces()) {
				if (namespaces.hasNext()) {
					writeln(OUTPUT_SEPARATOR);
					while (namespaces.hasNext()) {
						final Namespace namespace = namespaces.next();
						writeln("|" + namespace.getPrefix() + "  " + namespace.getName());
					}
					writeln(OUTPUT_SEPARATOR);
				} else {
					writeln("No namespaces found");
				}
			}
		} catch (RepositoryException e) {
			writeError("Failed to show namespaces", e);
		}
	}

	/**
	 * Show contexts
	 */
	private void showContexts() {
		Repository repository = state.getRepository();
		if (repository == null) {
			writeUnopenedError();
			return;
		}

		try (RepositoryConnection con = repository.getConnection()) {
			try (CloseableIteration<? extends Resource, RepositoryException> contexts = con.getContextIDs()) {
				if (contexts.hasNext()) {
					writeln(OUTPUT_SEPARATOR);
					while (contexts.hasNext()) {
						writeln("|" + contexts.next().toString());
					}
					writeln(OUTPUT_SEPARATOR);
				} else {
					writeln("No contexts found");
				}
			}
		} catch (RepositoryException e) {
			writeError("Failed to show contexts", e);
		}
	}
}
