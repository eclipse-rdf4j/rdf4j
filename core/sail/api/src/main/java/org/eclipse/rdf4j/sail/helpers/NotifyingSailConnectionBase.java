/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.helpers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;

/**
 * Abstract Class offering base functionality for SailConnection
 * implementations.
 * 
 * @author Arjohn Kampman
 * @author jeen
 */
public abstract class NotifyingSailConnectionBase extends AbstractSailConnection implements
		NotifyingSailConnection
{

	/*-----------*
	 * Variables *
	 *-----------*/

	private List<SailConnectionListener> listeners;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public NotifyingSailConnectionBase(AbstractSail sailBase) {
		super(sailBase);
		listeners = new ArrayList<SailConnectionListener>(0);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void addConnectionListener(SailConnectionListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	@Override
	public void removeConnectionListener(SailConnectionListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	protected boolean hasConnectionListeners() {
		synchronized (listeners) {
			return !listeners.isEmpty();
		}
	}

	protected void notifyStatementAdded(Statement st) {
		synchronized (listeners) {
			for (SailConnectionListener listener : listeners) {
				listener.statementAdded(st);
			}
		}
	}

	protected void notifyStatementRemoved(Statement st) {
		synchronized (listeners) {
			for (SailConnectionListener listener : listeners) {
				listener.statementRemoved(st);
			}
		}
	}
}
