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
package org.eclipse.rdf4j.sail.config;

import org.eclipse.rdf4j.sail.Sail;

/**
 * A SailFactory takes care of creating and initializing a specific type of {@link Sail} based on RDF configuration
 * data. SailFactory's are used to create specific Sails and to initialize them based on the configuration data that is
 * supplied to it, for example in a server environment.
 *
 * @author Arjohn Kampman
 */
public interface SailFactory {

	/**
	 * Returns the type of the Sails that this factory creates. Sail types are used for identification and should
	 * uniquely identify specific implementations of the Sail API. This type <em>can</em> be equal to the fully
	 * qualified class name of the Sail, but this is not required.
	 */
	String getSailType();

	SailImplConfig getConfig();

	/**
	 * Returns a Sail instance that has been initialized using the supplied configuration data.
	 *
	 * @param config TODO
	 * @return The created (but un-initialized) Sail.
	 * @throws SailConfigException If no Sail could be created due to invalid or incomplete configuration data.
	 */
	Sail getSail(SailImplConfig config) throws SailConfigException;
}
