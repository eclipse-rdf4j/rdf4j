/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import java.io.IOException;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryLockedException;
import org.eclipse.rdf4j.sail.LockManager;
import org.eclipse.rdf4j.sail.SailLockedException;
import org.eclipse.rdf4j.sail.helpers.DirectoryLockManager;

/**
 *
 * @author DAle Visser
 */
public class LockRemover {
	
	private final ConsoleIO consoleIO;

	LockRemover(ConsoleIO consoleIO) {
		this.consoleIO = consoleIO;
	}
	
	protected boolean tryToRemoveLock(final Repository repo)
			throws IOException, RepositoryException
		{
			boolean lockRemoved = false;
			final LockManager lockManager = new DirectoryLockManager(repo.getDataDir());
			if (lockManager.isLocked()
					&& consoleIO.askProceed(
							"WARNING: The lock from another process on this repository needs to be removed", true))
			{
				repo.shutDown();
				lockRemoved = lockManager.revokeLock();
				repo.initialize();
			}
			return lockRemoved;
		}

		protected boolean tryToRemoveLock(final RepositoryLockedException rle)
			throws IOException
		{
			boolean lockRemoved = false;
			if (rle.getCause() instanceof SailLockedException) {
				final SailLockedException sle = (SailLockedException)rle.getCause();
				final LockManager lockManager = sle.getLockManager();
				if (lockManager != null
						&& lockManager.isLocked()
						&& consoleIO.askProceed("WARNING: The lock from process '" + sle.getLockedBy()
								+ "' on this repository needs to be removed", true))
				{
					lockRemoved = lockManager.revokeLock();
				}
			}
			return lockRemoved;
		}
}
