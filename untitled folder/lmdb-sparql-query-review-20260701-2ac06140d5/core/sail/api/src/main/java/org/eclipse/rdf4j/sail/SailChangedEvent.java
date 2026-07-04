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
 * Event object that is send to {@link SailChangedListener}s to indicate that the contents of the Sail that sent the
 * event have changed.
 */
public interface SailChangedEvent {

	/**
	 * The Sail object that sent this event.
	 */
	Sail getSail();

	/**
	 * Indicates if statements were added to the Sail.
	 *
	 * @return <var>true</var> if statements were added during a transaction, <var>false</var> otherwise.
	 */
	boolean statementsAdded();

	/**
	 * Indicates if statements were removed from the Sail.
	 *
	 * @return <var>true</var> if statements were removed during a transaction, <var>false</var> otherwise.
	 */
	boolean statementsRemoved();

}
