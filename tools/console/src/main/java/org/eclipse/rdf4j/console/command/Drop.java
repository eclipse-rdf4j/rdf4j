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

import java.io.IOException;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryReadOnlyException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;

/**
 * @author Dale Visser
 */
public class Drop extends ConsoleCommand {
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
			writeln(getHelpLong());
		} else {
			final String repoID = tokens[1];
			try {
				dropRepository(repoID);
			} catch (RepositoryConfigException e) {
				writeError("Unable to drop repository '" + repoID, e);
			} catch (RepositoryReadOnlyException e) {
				try {
					execute(tokens);
				} catch (RepositoryException e2) {
					writeError("Failed to restart system", e2);
				}
			} catch (RepositoryException e) {
				writeError("Failed to update configuration in system repository", e);
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
		boolean proceed = askProceed("WARNING: you are about to drop repository '" + repoID + "'.", true);
		if (proceed && !state.getManager().isSafeToRemove(repoID)) {
			proceed = askProceed("WARNING: dropping this repository may break another that is proxying it.",
					false);
		}
		if (proceed) {
			if (repoID.equals(state.getRepositoryID())) {
				close.closeRepository(false);
			}
			final boolean isRemoved = state.getManager().removeRepository(repoID);
			if (isRemoved) {
				writeInfo("Dropped repository '" + repoID + "'");
			} else {
				writeInfo("Unknown repository '" + repoID + "'");
			}
		} else {
			writeln("Drop aborted");
		}
	}
}
