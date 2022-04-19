/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail;

/**
 * Indicates that a SAIL cannot be initialised because the configured persisted location is locked.
 *
 * @author James Leigh
 */
public class SailLockedException extends SailException {

	private static final long serialVersionUID = -2465202131214972460L;

	private String lockedBy;

	private final String requestedBy;

	private LockManager manager;

	public SailLockedException(String requestedBy) {
		super("SAIL could not be locked (check permissions)");
		this.requestedBy = requestedBy;
	}

	public SailLockedException(String lockedBy, String requestedBy) {
		super("SAIL is already locked by: " + lockedBy);
		this.lockedBy = lockedBy;
		this.requestedBy = requestedBy;
	}

	public SailLockedException(String lockedBy, String requestedBy, LockManager manager) {
		super("SAIL is already locked by: " + lockedBy + " in " + manager.getLocation());
		this.lockedBy = lockedBy;
		this.requestedBy = requestedBy;
		this.manager = manager;
	}

	/**
	 * Returns the name representing the Java virtual machine that acquired the lock.
	 *
	 * @return the name representing the Java virtual machine that acquired the lock.
	 */
	public String getLockedBy() {
		return lockedBy;
	}

	/**
	 * Returns the name representing the Java virtual machine that requested the lock.
	 *
	 * @return the name representing the Java virtual machine that requested the lock.
	 */
	public String getRequestedBy() {
		return requestedBy;
	}

	/**
	 * @return Returns the lock manager that failed to obtain a lock.
	 */
	public LockManager getLockManager() {
		return manager;
	}
}
