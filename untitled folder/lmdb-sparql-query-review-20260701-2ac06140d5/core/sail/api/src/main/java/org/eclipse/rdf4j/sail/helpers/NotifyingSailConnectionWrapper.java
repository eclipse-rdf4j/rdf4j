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

import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;

/**
 * An implementation of the {@link org.eclipse.rdf4j.sail.NotifyingSailConnection} interface that wraps another
 * {@link org.eclipse.rdf4j.sail.NotifyingSailConnection} object and forwards any method calls to the wrapped
 * transaction.
 *
 * @author Jeen Broekstra
 */
public class NotifyingSailConnectionWrapper extends SailConnectionWrapper implements NotifyingSailConnection {

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new {@link NotifyingSailConnectionWrapper} object that wraps the supplied connection.
	 */
	public NotifyingSailConnectionWrapper(NotifyingSailConnection wrappedCon) {
		super(wrappedCon);
	}

	/*-----------------------*
	 * SailConnectionWrapper *
	 *-----------------------*/

	@Override
	public NotifyingSailConnection getWrappedConnection() {
		return (NotifyingSailConnection) super.getWrappedConnection();
	}

	/*-------------------------*
	 * NotifyingSailConnection *
	 *-------------------------*/

	/**
	 * Adds the given listener to the wrapped connection.
	 */
	@Override
	public void addConnectionListener(SailConnectionListener listener) {
		getWrappedConnection().addConnectionListener(listener);
	}

	/**
	 * Removes the given listener from the wrapped connection.
	 */
	@Override
	public void removeConnectionListener(SailConnectionListener listener) {
		getWrappedConnection().removeConnectionListener(listener);
	}
}
