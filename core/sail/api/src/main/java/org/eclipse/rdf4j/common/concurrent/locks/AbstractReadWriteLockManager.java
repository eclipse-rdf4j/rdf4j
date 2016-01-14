/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks;


/**
 * An abstract base implementation of a read/write lock manager.
 * 
 * @author Arjohn Kampman
 * @author James Leigh
 */
public abstract class AbstractReadWriteLockManager implements ReadWriteLockManager {

	/*
	 * ----------- Variables -----------
	 */

	/**
	 * Flag indicating whether a writer is active.
	 */
	private final LockManager activeWriter;

	/**
	 * Counter that keeps track of the numer of active read locks.
	 */
	private final LockManager activeReaders;

	/*
	 * -------------- Constructors --------------
	 */

	/**
	 * Creates a MultiReadSingleWriteLockManager.
	 */
	public AbstractReadWriteLockManager() {
		this(false);
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
	public AbstractReadWriteLockManager(boolean trackLocks) {
		boolean trace = trackLocks || Properties.lockTrackingEnabled();
		activeWriter = new LockManager(trace);
		activeReaders = new LockManager(trace);
	}

	/*
	 * --------- Methods ---------
	 */

	/**
	 * If a writer is active
	 */
	protected boolean isWriterActive() {
		return activeWriter.isActiveLock();
	}

	/**
	 * If one or more readers are active
	 */
	protected boolean isReaderActive() {
		return activeReaders.isActiveLock();
	}

	/**
	 * Blocks current thread until after the writer lock is released (if active).
	 * 
	 * @throws InterruptedException
	 */
	protected void waitForActiveWriter()
		throws InterruptedException
	{
		activeWriter.waitForActiveLocks();
	}

	/**
	 * Blocks current thread until there are no reader locks active.
	 * 
	 * @throws InterruptedException
	 */
	protected void waitForActiveReaders()
		throws InterruptedException
	{
		activeReaders.waitForActiveLocks();
	}

	/**
	 * Creates a new Lock for reading and increments counter for active readers.
	 * The lock is tracked if lock tracking is enabled. This method is not thread
	 * safe itself, the calling method is expected to handle synchronization
	 * issues.
	 * 
	 * @return a read lock.
	 */
	protected Lock createReadLock() {
		return activeReaders.createLock("Read");
	}

	/**
	 * Creates a new Lock for writing. The lock is tracked if lock tracking is
	 * enabled. This method is not thread safe itself for performance reasons,
	 * the calling method is expected to handle synchronization issues.
	 * 
	 * @return a write lock.
	 */
	protected Lock createWriteLock() {
		return activeWriter.createLock("Write");
	}
}
