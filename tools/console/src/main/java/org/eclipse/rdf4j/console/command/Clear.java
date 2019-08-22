/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.io.IOException;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.console.LockRemover;
import org.eclipse.rdf4j.console.Util;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryReadOnlyException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clear command.
 * 
 * @author Dale Visser
 */
public class Clear extends ConsoleCommand {
	private static final Logger LOGGER = LoggerFactory.getLogger(Clear.class);

	@Override
	public String getName() {
		return "clear";
	}

	@Override
	public String getHelpShort() {
		return "Removes data from a repository";
	}

	@Override
	public String getHelpLong() {
		return PrintHelp.USAGE + "clear                   Clears the entire repository\n"
				+ "clear (<uri>|null)...   Clears the specified context(s)\n";
	}

	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 * @param state
	 * 
	 */
	public Clear(ConsoleIO consoleIO, ConsoleState state) {
		super(consoleIO, state);
	}

	@Override
	public void execute(String... tokens) {
		Repository repository = state.getRepository();

		if (repository == null) {
			consoleIO.writeUnopenedError();
		} else {
			Resource[] contexts;
			try {
				contexts = Util.getContexts(tokens, 1, repository);
			} catch (IllegalArgumentException ioe) {
				consoleIO.writeError(ioe.getMessage());
				return;
			}
			clear(repository, contexts);
		}
	}

	/**
	 * Clear repository, either completely or only triples of specific contexts.
	 * 
	 * @param repository repository to be cleared
	 * @param contexts   array of contexts
	 */
	private void clear(Repository repository, Resource[] contexts) {
		if (contexts.length == 0) {
			consoleIO.writeln("Clearing repository...");
		} else {
			consoleIO.writeln("Removing specified contexts...");
		}
		try {
			try (RepositoryConnection con = repository.getConnection()) {
				con.clear(contexts);
				if (contexts.length == 0) {
					con.clearNamespaces();
				}
			}
		} catch (RepositoryReadOnlyException e) {
			try {
				if (LockRemover.tryToRemoveLock(repository, consoleIO)) {
					this.clear(repository, contexts);
				} else {
					consoleIO.writeError("Failed to clear repository");
					LOGGER.error("Failed to clear repository", e);
				}
			} catch (RepositoryException e1) {
				consoleIO.writeError("Unable to restart repository: " + e1.getMessage());
				LOGGER.error("Unable to restart repository", e1);
			} catch (IOException e1) {
				consoleIO.writeError("Unable to remove lock: " + e1.getMessage());
			}
		} catch (RepositoryException e) {
			consoleIO.writeError("Failed to clear repository: " + e.getMessage());
			LOGGER.error("Failed to clear repository", e);
		}
	}
}
