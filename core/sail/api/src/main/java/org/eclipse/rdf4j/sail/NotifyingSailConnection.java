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

/**
 * A connection to an RDF Sail object. A SailConnection is active from the moment it is created until it is closed. Care
 * should be taken to properly close SailConnections as they might block concurrent queries and/or updates on the Sail
 * while active, depending on the Sail-implementation that is being used.
 *
 * @author James Leigh
 */
public interface NotifyingSailConnection extends SailConnection {

	/**
	 * Registers a SailConnection listener with this SailConnection. The listener should be notified of any statements
	 * that are added or removed as part of this SailConnection.
	 *
	 * @param listener A SailConnectionListener.
	 */
	void addConnectionListener(SailConnectionListener listener);

	/**
	 * Deregisters a SailConnection listener with this SailConnection.
	 *
	 * @param listener A SailConnectionListener.
	 */
	void removeConnectionListener(SailConnectionListener listener);

}
