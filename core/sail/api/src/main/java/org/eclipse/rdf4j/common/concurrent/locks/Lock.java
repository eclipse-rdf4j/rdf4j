/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
 
package org.eclipse.rdf4j.common.concurrent.locks;

/**
 * A lock on a specific monitor that can be used for synchronization purposes.
 */
public interface Lock {

	/**
	 * Checks whether the lock is still active.
	 */
	public boolean isActive();

	/**
	 * Release the lock, making it inactive.
	 */
	public void release();
}
