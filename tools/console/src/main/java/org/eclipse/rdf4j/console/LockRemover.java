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
package org.eclipse.rdf4j.console;

import java.io.IOException;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryLockedException;
import org.eclipse.rdf4j.sail.LockManager;
import org.eclipse.rdf4j.sail.SailLockedException;
import org.eclipse.rdf4j.sail.helpers.DirectoryLockManager;

/**
 * @author DAle Visser
 */
public class LockRemover {
	/**
	 * Try to remove lock from repository
	 *
	 * @param repo
	 * @param consoleIO
	 * @return true if lock was removed
	 * @throws IOException
	 * @throws RepositoryException
	 */
	public static boolean tryToRemoveLock(Repository repo, ConsoleIO consoleIO)
			throws IOException, RepositoryException {
		boolean lockRemoved = false;

		LockManager lockManager = new DirectoryLockManager(repo.getDataDir());
		if (lockManager.isLocked() && consoleIO
				.askProceed("WARNING: The lock from another process on this repository needs to be removed", true)) {
			repo.shutDown();
			lockRemoved = lockManager.revokeLock();
			repo.init();
		}
		return lockRemoved;
	}

	/**
	 * Try to remove lock when exception was raised
	 *
	 * @param rle
	 * @param consoleIO
	 * @return true if lock was removed
	 * @throws IOException
	 */
	public static boolean tryToRemoveLock(RepositoryLockedException rle, ConsoleIO consoleIO) throws IOException {
		boolean lockRemoved = false;

		if (rle.getCause() instanceof SailLockedException) {
			SailLockedException sle = (SailLockedException) rle.getCause();
			LockManager lockManager = sle.getLockManager();
			if (lockManager != null && lockManager.isLocked() && consoleIO.askProceed(
					"WARNING: The lock from process '" + sle.getLockedBy() + "' on this repository needs to be removed",
					true)) {
				lockRemoved = lockManager.revokeLock();
			}
		}
		return lockRemoved;
	}
}
