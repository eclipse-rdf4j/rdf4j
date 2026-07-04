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
 * An interface for {@link Sail}s that notify registered {@link SailChangedListener}s of changes in the data in the
 * Sail.
 *
 * @author James Leigh
 */
public interface NotifyingSail extends Sail {

	/**
	 * Opens a connection on the Sail which can be used to query and update data. Depending on how the implementation
	 * handles concurrent access, a call to this method might block when there is another open connection on this Sail.
	 *
	 * @throws SailException If no transaction could be started, for example because the Sail is not writable.
	 */
	@Override
	NotifyingSailConnection getConnection() throws SailException;

	/**
	 * Adds the specified SailChangedListener to receive events when the data in this Sail object changes.
	 */
	void addSailChangedListener(SailChangedListener listener);

	/**
	 * Removes the specified SailChangedListener so that it no longer receives events from this Sail object.
	 */
	void removeSailChangedListener(SailChangedListener listener);
}
