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
package org.eclipse.rdf4j.repository;

/**
 * Indicates that a Repository cannot be initialised because the configured persisted location is locked.
 *
 * @author James Leigh
 */
public class RepositoryLockedException extends RepositoryException {

	private static final long serialVersionUID = -1544864578935422866L;

	private final String lockedBy;

	private final String requestedBy;

	public RepositoryLockedException(String lockedBy, String requestedBy, String msg, Throwable t) {
		super(msg, t);
		this.lockedBy = lockedBy;
		this.requestedBy = requestedBy;
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

}
