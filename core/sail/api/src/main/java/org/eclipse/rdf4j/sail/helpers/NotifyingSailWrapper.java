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

import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailChangedListener;
import org.eclipse.rdf4j.sail.SailException;

/**
 * An implementation of the StackableSail interface that wraps another Sail object and forwards any relevant calls to
 * the wrapped Sail.
 *
 * @author Arjohn Kampman
 */
public class NotifyingSailWrapper extends SailWrapper implements NotifyingSail {

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new SailWrapper. The base Sail for the created SailWrapper can be set later using {@link #setBaseSail}.
	 */
	public NotifyingSailWrapper() {
	}

	/**
	 * Creates a new SailWrapper that wraps the supplied Sail.
	 */
	public NotifyingSailWrapper(NotifyingSail baseSail) {
		setBaseSail(baseSail);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void setBaseSail(Sail baseSail) {
		super.setBaseSail((NotifyingSail) baseSail);
	}

	@Override
	public NotifyingSail getBaseSail() {
		return (NotifyingSail) super.getBaseSail();
	}

	@Override
	public NotifyingSailConnection getConnection() throws SailException {
		return (NotifyingSailConnection) super.getConnection();
	}

	@Override
	public void addSailChangedListener(SailChangedListener listener) {
		verifyBaseSailSet();
		getBaseSail().addSailChangedListener(listener);
	}

	@Override
	public void removeSailChangedListener(SailChangedListener listener) {
		verifyBaseSailSet();
		getBaseSail().removeSailChangedListener(listener);
	}
}
