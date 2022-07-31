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

package org.eclipse.rdf4j.common.concurrent.locks;

/**
 * A lock manager that manages a multi-read, single-write lock. This lock manager allows multiple read locks to be
 * active at the same time. The write lock is exclusive, meaning that no other read- or write locks may be active at the
 * same time.
 *
 * @author Arjohn Kampman
 */
public interface ReadWriteLockManager {

	/**
	 * Gets a read lock, if available. This method will return <var>null</var> if the read lock is not immediately
	 * available.
	 */
	Lock tryReadLock();

	/**
	 * Gets a read lock. This method blocks until the read lock is available.
	 *
	 * @throws InterruptedException In case the thread requesting the lock was {@link Thread#interrupt() interrupted}.
	 */
	Lock getReadLock() throws InterruptedException;

	/**
	 * Gets an exclusive write lock, if available. This method will return <var>null</var> if the write lock is not
	 * immediately available.
	 */
	Lock tryWriteLock();

	/**
	 * Gets an exclusive write lock. This method blocks until the write lock is available.
	 *
	 * @throws InterruptedException In case the thread requesting the lock was {@link Thread#interrupt() interrupted}.
	 */
	Lock getWriteLock() throws InterruptedException;

	/**
	 * Returns {@code false} if there are no active write locks, otherwise returns {@code true}.
	 */
	boolean isWriterActive();

	/**
	 * Returns {@code false} if there are no active read locks, otherwise returns {@code true}.
	 */
	boolean isReaderActive();

	/**
	 * Blocks until all write locks have been released.
	 *
	 * @throws InterruptedException In case the thread requesting the lock was {@link Thread#interrupt() interrupted}.
	 */
	void waitForActiveWriter() throws InterruptedException;

	/**
	 * Blocks until all read locks have been released.
	 *
	 * @throws InterruptedException In case the thread requesting the lock was {@link Thread#interrupt() interrupted}.
	 */
	void waitForActiveReaders() throws InterruptedException;

}
