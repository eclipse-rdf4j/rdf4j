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

import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailChangedEvent;

/**
 * Default implementation of the SailChangedEvent interface.
 */
public class DefaultSailChangedEvent implements SailChangedEvent {

	/*-----------*
	 * Constants *
	 *-----------*/

	private final Sail sail;

	/*-----------*
	 * Variables *
	 *-----------*/

	private boolean statementsAdded;

	private boolean statementsRemoved;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new DefaultSailChangedEvent in which all possible changes are set to false.
	 */
	public DefaultSailChangedEvent(Sail sail) {
		this.sail = sail;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public Sail getSail() {
		return sail;
	}

	@Override
	public boolean statementsAdded() {
		return statementsAdded;
	}

	public void setStatementsAdded(boolean statementsAdded) {
		this.statementsAdded = statementsAdded;
	}

	@Override
	public boolean statementsRemoved() {
		return statementsRemoved;
	}

	public void setStatementsRemoved(boolean statementsRemoved) {
		this.statementsRemoved = statementsRemoved;
	}
}
