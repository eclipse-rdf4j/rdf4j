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
package org.eclipse.rdf4j.repository.config;

import org.eclipse.rdf4j.repository.Repository;

/**
 * A RepositoryFactory takes care of creating and initializing a specific type of {@link Repository}s based on RDF
 * configuration data.
 *
 * @author Arjohn Kampman
 */
public interface RepositoryFactory {

	/**
	 * Returns the type of the repositories that this factory creates. Repository types are used for identification and
	 * should uniquely identify specific implementations of the Repository API. This type <em>can</em> be equal to the
	 * fully qualified class name of the repository, but this is not required.
	 */
	String getRepositoryType();

	RepositoryImplConfig getConfig();

	/**
	 * Returns a Repository instance that has been initialized using the supplied configuration data.
	 *
	 * @param config TODO
	 * @return The created (but un-initialized) repository.
	 * @throws RepositoryConfigException If no repository could be created due to invalid or incomplete configuration
	 *                                   data.
	 */
	Repository getRepository(RepositoryImplConfig config) throws RepositoryConfigException;
}
