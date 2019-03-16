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

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryReadOnlyException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dale Visser
 */
public class Drop extends ConsoleCommand {
	private static final Logger LOGGER = LoggerFactory.getLogger(Drop.class);

	private final Close close;

	@Override
	public String getName() {
		return "drop";
	}

	@Override
	public String getHelpShort() {
		return "Drops a repository";
	}

	@Override
	public String getHelpLong() {
		return PrintHelp.USAGE + "drop <repositoryID>   Drops the repository with the specified id\n";
	}

	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 * @param state
	 * @param close
	 */
	public Drop(ConsoleIO consoleIO, ConsoleState state, Close close) {
		super(consoleIO, state);
		this.close = close;
	}

	@Override
	public void execute(String... tokens) throws IOException {
		if (tokens.length < 2) {
			consoleIO.writeln(getHelpLong());
		} else {
			final String repoID = tokens[1];
			try {
				dropRepository(repoID);
			} catch (RepositoryConfigException e) {
				consoleIO.writeError("Unable to drop repository '" + repoID + "': " + e.getMessage());
				LOGGER.warn("Unable to drop repository '" + repoID + "'", e);
			} catch (RepositoryReadOnlyException e) {
				try {
					if (LockRemover.tryToRemoveLock(state.getManager().getSystemRepository(), consoleIO)) {
						execute(tokens);
					} else {
						consoleIO.writeError("Failed to drop repository");
						LOGGER.error("Failed to drop repository", e);
					}
				} catch (RepositoryException e2) {
					consoleIO.writeError("Failed to restart system: " + e2.getMessage());
					LOGGER.error("Failed to restart system", e2);
				}
			} catch (RepositoryException e) {
				consoleIO.writeError("Failed to update configuration in system repository: " + e.getMessage());
				LOGGER.warn("Failed to update configuration in system repository", e);
			}
		}
	}

	/**
	 * Try to drop a repository after confirmation from user
	 * 
	 * @param repoID repository ID
	 * @throws IOException
	 * @throws RepositoryException
	 * @throws RepositoryConfigException
	 */
	private void dropRepository(final String repoID)
			throws IOException, RepositoryException, RepositoryConfigException {
		boolean proceed = consoleIO.askProceed("WARNING: you are about to drop repository '" + repoID + "'.", true);
		if (proceed && !state.getManager().isSafeToRemove(repoID)) {
			proceed = consoleIO.askProceed("WARNING: dropping this repository may break another that is proxying it.",
					false);
		}
		if (proceed) {
			if (repoID.equals(state.getRepositoryID())) {
				close.closeRepository(false);
			}
			final boolean isRemoved = state.getManager().removeRepository(repoID);
			if (isRemoved) {
				consoleIO.writeln("Dropped repository '" + repoID + "'");
			} else {
				consoleIO.writeln("Unknown repository '" + repoID + "'");
			}
		} else {
			consoleIO.writeln("Drop aborted");
		}
	}
}
