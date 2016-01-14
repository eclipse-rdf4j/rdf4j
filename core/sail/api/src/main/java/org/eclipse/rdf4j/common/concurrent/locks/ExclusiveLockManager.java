/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks;


/**
 * A lock manager for exclusive locks.
 * 
 * @author Arjohn Kampman
 * @author James Leigh
 */
public class ExclusiveLockManager {

	/*
	 * ----------- Variables -----------
	 */

	private final LockManager lock;

	/*
	 * -------------- Constructors --------------
	 */

	/**
	 * Creates an ExclusiveLockManager.
	 */
	public ExclusiveLockManager() {
		this(false);
	}

	/**
	 * Creates an ExclusiveLockManager.
	 * 
	 * @param trackLocks
	 *        If create stack traces should be logged
	 */
	public ExclusiveLockManager(boolean trackLocks) {
		this.lock = new LockManager(trackLocks || Properties.lockTrackingEnabled());
	}

	/*
	 * --------- Methods ---------
	 */

	/**
	 * Gets the exclusive lock, if available. This method will return
	 * <tt>null</tt> if the exclusive lock is not immediately available.
	 */
	public Lock tryExclusiveLock() {
		if (lock.isActiveLock()) {
			return null;
		}
		synchronized (this) {
			if (lock.isActiveLock()) {
				return null;
			}
	
			return createLock();
		}
	}

	/**
	 * Gets the exclusive lock. This method blocks when the exclusive lock is
	 * currently in use until it is released.
	 */
	public synchronized Lock getExclusiveLock()
		throws InterruptedException
	{
		while (lock.isActiveLock()) {
			// Someone else currently has the lock
			lock.waitForActiveLocks();
		}

		return createLock();
	}

	private Lock createLock() {
		return lock.createLock("Exclusive");
	}
}
