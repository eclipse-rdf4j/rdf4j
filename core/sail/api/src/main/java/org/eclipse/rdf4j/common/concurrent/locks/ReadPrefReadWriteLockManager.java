/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks;


/**
 * A read/write lock manager with reader preference. This lock manager block any
 * requests for write locks until all read locks have been released.
 * 
 * @author Arjohn Kampman
 * @author James Leigh
 */
public class ReadPrefReadWriteLockManager extends AbstractReadWriteLockManager {

	/*
	 * ----------- Variables -----------
	 */

	/*
	 * -------------- Constructors --------------
	 */

	/**
	 * Creates a MultiReadSingleWriteLockManager.
	 */
	public ReadPrefReadWriteLockManager() {
		super();
	}

	/**
	 * Creates a new MultiReadSingleWriteLockManager, optionally with lock
	 * tracking enabled.
	 * 
	 * @param trackLocks
	 *        Controls whether the lock manager will keep track of active locks.
	 *        Enabling lock tracking will add some overhead, but can be very
	 *        useful for debugging.
	 */
	public ReadPrefReadWriteLockManager(boolean trackLocks) {
		super(trackLocks);
	}

	/*
	 * --------- Methods ---------
	 */

	/**
	 * Gets a read lock, if available. This method will return <tt>null</tt> if
	 * the read lock is not immediately available.
	 */
	public Lock tryReadLock() {
		if (isWriterActive()) {
			return null;
		}
		synchronized (this) {
			if (isWriterActive()) {
				return null;
			}
	
			return createReadLock();
		}
	}

	/**
	 * Gets a read lock. This method blocks when a write lock is in use or has
	 * been requested until the write lock is released.
	 */
	public synchronized Lock getReadLock()
		throws InterruptedException
	{
		// Wait for the writer to finish
		while (isWriterActive()) {
			waitForActiveWriter();
		}

		return createReadLock();
	}

	/**
	 * Gets an exclusive write lock, if available. This method will return
	 * <tt>null</tt> if the write lock is not immediately available.
	 */
	public Lock tryWriteLock() {
		if (isWriterActive() || isReaderActive())
			return null;
		synchronized (this) {
			if (isWriterActive() || isReaderActive()) {
				return null;
			}
	
			return createWriteLock();
		}
	}

	/**
	 * Gets an exclusive write lock. This method blocks when the write lock is in
	 * use or has already been requested until the write lock is released. This
	 * method also block when read locks are active until all of them are
	 * released.
	 */
	public Lock getWriteLock()
		throws InterruptedException
	{
		while (true) {
			Lock lock = tryWriteLock();
			if (lock != null)
				return lock;
			waitForActiveWriter();
			waitForActiveReaders();
		}
	}
}
