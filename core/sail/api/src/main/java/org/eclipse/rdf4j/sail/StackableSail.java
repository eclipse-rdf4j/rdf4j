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
 * An interface for Sails that can be stacked on top of other Sails.
 */
public interface StackableSail extends Sail {

	/**
	 * Sets the base Sail that this Sail will work on top of. This method will be called before the initialize() method
	 * is called.
	 */
	void setBaseSail(Sail baseSail);

	/**
	 * Gets the base Sail that this Sail works on top of.
	 */
	Sail getBaseSail();
}
