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
package org.eclipse.rdf4j.console;

import java.io.File;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;

/**
 * Console state interface
 *
 * @author Dale Visser
 */
public interface ConsoleState {

	/**
	 * Get application name
	 *
	 * @return application name
	 */
	String getApplicationName();

	/**
	 * Get repository data directory
	 *
	 * @return directory
	 */
	File getDataDirectory();

	/**
	 * Get repository manager ID
	 *
	 * @return repository manager ID
	 */
	String getManagerID();

	/**
	 * Get repository ID
	 *
	 * @return repository ID
	 */
	String getRepositoryID();

	/**
	 * Get repository manager
	 *
	 * @return repository manager
	 */
	RepositoryManager getManager();

	/**
	 * Set repository manager
	 *
	 * @param manager repository manager
	 */
	void setManager(RepositoryManager manager);

	/**
	 * Set repository manager
	 *
	 * @param managerID repository manager ID
	 */
	void setManagerID(String managerID);

	/**
	 * Get repository
	 *
	 * @return repository
	 */
	Repository getRepository();

	/**
	 * Set repository ID
	 *
	 * @param repositoryID repository ID
	 */
	void setRepositoryID(String repositoryID);

	/**
	 * Set repository
	 *
	 * @param repository repository
	 */
	void setRepository(Repository repository);

}
