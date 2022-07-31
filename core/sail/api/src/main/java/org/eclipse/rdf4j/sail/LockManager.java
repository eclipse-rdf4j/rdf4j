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
package org.eclipse.rdf4j.sail;

import org.eclipse.rdf4j.common.concurrent.locks.Lock;

/**
 * @author james
 */
public interface LockManager {

	/**
	 * Technical description of where the lock is located, such as a URL.
	 */
	String getLocation();

	/**
	 * Determines if the SAIL is locked.
	 *
	 * @return <code>true</code> if the SAIL is already locked.
	 */
	boolean isLocked();

	/**
	 * Creates a lock in a SAIL if it does not yet exist.
	 *
	 * @return a newly acquired lock or null if the SAIL is already locked.
	 */
	Lock tryLock();

	/**
	 * Creates a lock in a SAIL if it does not yet exist.
	 *
	 * @return a newly acquired lock.
	 * @throws SailLockedException if the directory is already locked.
	 */
	Lock lockOrFail() throws SailLockedException;

	/**
	 * Revokes a lock owned by another process.
	 *
	 * @return <code>true</code> if a lock was successfully revoked.
	 */
	boolean revokeLock();

}
