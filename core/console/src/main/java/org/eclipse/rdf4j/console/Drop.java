/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import java.io.IOException;

import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryReadOnlyException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dale Visser
 */
public class Drop implements Command {

	private static final Logger LOGGER = LoggerFactory.getLogger(Drop.class);

	private final ConsoleIO consoleIO;

	private final ConsoleState state;

	private final Close close;

	private final LockRemover lockRemover;

	Drop(ConsoleIO consoleIO, ConsoleState state, Close close, LockRemover lockRemover) {
		this.consoleIO = consoleIO;
		this.state = state;
		this.close = close;
		this.lockRemover = lockRemover;
	}

	public void execute(String... tokens)
		throws IOException
	{
		if (tokens.length < 2) {
			consoleIO.writeln(PrintHelp.DROP);
		}
		else {
			final String repoID = tokens[1];
			try {
				dropRepository(repoID);
			}
			catch (RepositoryConfigException e) {
				consoleIO.writeError("Unable to drop repository '" + repoID + "': " + e.getMessage());
				LOGGER.warn("Unable to drop repository '" + repoID + "'", e);
			}
			catch (RepositoryReadOnlyException e) {
				try {
					if (lockRemover.tryToRemoveLock(state.getManager().getSystemRepository())) {
						execute(tokens);
					}
					else {
						consoleIO.writeError("Failed to drop repository");
						LOGGER.error("Failed to drop repository", e);
					}
				}
				catch (RepositoryException e2) {
					consoleIO.writeError("Failed to restart system: " + e2.getMessage());
					LOGGER.error("Failed to restart system", e2);
				}
			}
			catch (RepositoryException e) {
				consoleIO.writeError("Failed to update configuration in system repository: " + e.getMessage());
				LOGGER.warn("Failed to update configuration in system repository", e);
			}
		}
	}

	private void dropRepository(final String repoID)
		throws IOException, RepositoryException, RepositoryConfigException
	{
		boolean proceed = consoleIO.askProceed("WARNING: you are about to drop repository '" + repoID
				+ "'.", true);
		if (proceed && !state.getManager().isSafeToRemove(repoID)) {
			proceed = consoleIO.askProceed("WARNING: dropping this repository may break another that is proxying it.", false);
		}
		if (proceed) {
			if (repoID.equals(state.getRepositoryID())) {
				close.closeRepository(false);
			}
			final boolean isRemoved = state.getManager().removeRepository(repoID);
			if (isRemoved) {
				consoleIO.writeln("Dropped repository '" + repoID + "'");
			}
			else {
				consoleIO.writeln("Unknown repository '" + repoID + "'");
			}
		}
		else {
			consoleIO.writeln("Drop aborted");
		}
	}

}
