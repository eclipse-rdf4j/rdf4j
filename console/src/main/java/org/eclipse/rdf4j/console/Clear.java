/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import java.io.IOException;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
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

	private final LockRemover lockRemover;

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
		return  PrintHelp.USAGE
			+ "clear                   Clears the entire repository\n"
			+ "clear (<uri>|null)...   Clears the specified context(s)\n";
	}
	
	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 * @param state
	 * @param lockRemover 
	 */
	Clear(ConsoleIO consoleIO, ConsoleState state, LockRemover lockRemover) {
		super(consoleIO, state);
		this.lockRemover = lockRemover;
	}

	@Override
	public void execute(String... tokens) {
		Repository repository = state.getRepository();
		
		if (repository == null) {
			consoleIO.writeUnopenedError();
		} else {
			final ValueFactory valueFactory = repository.getValueFactory();
			Resource[] contexts = new Resource[tokens.length - 1];
			
			for (int i = 1; i < tokens.length; i++) {
				final String contextID = tokens[i];
				if (contextID.equalsIgnoreCase("null")) {
					contexts[i - 1] = null; // NOPMD
				} else if (contextID.startsWith("_:")) {
					contexts[i - 1] = valueFactory.createBNode(contextID.substring(2));
				} else {
					try {
						contexts[i - 1] = valueFactory.createIRI(contextID);
					} catch (IllegalArgumentException e) {
						consoleIO.writeError("illegal URI: " + contextID);
						consoleIO.writeln(getHelpLong());
						return;
					}
				}
			}
			clear(repository, contexts);
		}
	}

	/**
	 * Clear repository, either completely or only triples of specific contexts.
	 * 
	 * @param repository repository to be cleared
	 * @param contexts array of contexts
	 */
	private void clear(Repository repository, Resource[] contexts) {
		if (contexts.length == 0) {
			consoleIO.writeln("Clearing repository...");
		} else {
			consoleIO.writeln("Removing specified contexts...");
		}
		try {
			final RepositoryConnection con = repository.getConnection();
			try {
				con.clear(contexts);
				if (contexts.length == 0) {
					con.clearNamespaces();
				}
			} finally {
				con.close();
			}
		} catch (RepositoryReadOnlyException e) {
			try {
				if (lockRemover.tryToRemoveLock(repository)) {
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
