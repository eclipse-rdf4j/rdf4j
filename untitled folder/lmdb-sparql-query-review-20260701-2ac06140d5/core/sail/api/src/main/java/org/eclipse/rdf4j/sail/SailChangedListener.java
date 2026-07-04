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
 * An interface for objects that want to be notified when the data in specific Sail objects change.
 */
public interface SailChangedListener {

	/**
	 * Notifies the listener of a change to the data of a specific Sail.
	 */
	void sailChanged(SailChangedEvent event);
}
