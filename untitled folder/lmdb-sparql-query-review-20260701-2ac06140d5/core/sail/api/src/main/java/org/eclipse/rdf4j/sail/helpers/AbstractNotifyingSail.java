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
package org.eclipse.rdf4j.sail.helpers;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailChangedEvent;
import org.eclipse.rdf4j.sail.SailChangedListener;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A base {@link NotifyingSail} implementation that takes care of common sail tasks, including proper closing of active
 * connections and a grace period for active connections during shutdown of the store.
 *
 * @author Herko ter Horst
 * @author jeen
 * @author Arjohn Kampman
 */
public abstract class AbstractNotifyingSail extends AbstractSail implements NotifyingSail {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * Objects that should be notified of changes to the data in this Sail.
	 */
	private final Set<SailChangedListener> sailChangedListeners = new HashSet<>(0);

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public NotifyingSailConnection getConnection() throws SailException {
		return (NotifyingSailConnection) super.getConnection();
	}

	@Override
	protected abstract NotifyingSailConnection getConnectionInternal() throws SailException;

	@Override
	public void addSailChangedListener(SailChangedListener listener) {
		synchronized (sailChangedListeners) {
			sailChangedListeners.add(listener);
		}
	}

	@Override
	public void removeSailChangedListener(SailChangedListener listener) {
		synchronized (sailChangedListeners) {
			sailChangedListeners.remove(listener);
		}
	}

	/**
	 * Notifies all registered SailChangedListener's of changes to the contents of this Sail.
	 */
	public void notifySailChanged(SailChangedEvent event) {
		synchronized (sailChangedListeners) {
			for (SailChangedListener l : sailChangedListeners) {
				l.sailChanged(event);
			}
		}
	}
}
